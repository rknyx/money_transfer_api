package com.rk.core;

import com.rk.api.Account;
import com.rk.api.Currency;
import com.rk.api.Order;
import com.rk.api.OrderStatus;
import com.rk.db.dao.AccountDAO;
import com.rk.db.dao.ExchangeRateDAO;
import com.rk.db.dao.OrderDAO;
import io.dropwizard.hibernate.UnitOfWork;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * Worker which perform all operations on orders. Status of operation affects order status which can be FAIL or DONE
 * after the end of operation.
 * Typically, calculations are performed with currency conversion. Two conversions are possible in the most complex case:
 * <ul>
 *     <li>First conversion is from order operation currency to sender currency to determine amount to charge</li>
 *     <li>Second conversion is from order operation currency to receiver currency to determine amount to
 *     increase receiver balance</li>
 * </ul>
 * <p>* Exchange rates can change between order submission and processing. Order processing will not do anything with that. Calculations are
 * performed with current exchange rate.</p>
 */
public class OrderProcessingWorker implements Consumer<Order> {
    private static final int DISPLAY_AND_COMPARE_SCALE = 2;
    public static final String SENDER_ACCOUNT_DOES_NOT_EXIST_MSG = "Sender account with id: %s does not exist";
    public static final String RECEIVER_ACCOUNT_DOES_NOT_EXIST_MSG = "Receiver account with id: %s does not exist";
    public static final String INSUFFICIENT_FUNDS_MSG = "Insufficient funds. '%s' is required, '%s' is available";

    private final Logger logger = LoggerFactory.getLogger(getClass().getName());

    private static final NumberFormat currencyFormat = NumberFormat.getCurrencyInstance();

    private final OrderDAO orderDAO;
    private final AccountDAO accountDAO;
    private final ExchangeRateDAO exchangeRateDAO;

    public OrderProcessingWorker(OrderDAO orderDAO, AccountDAO accountDAO, ExchangeRateDAO exchangeRateDAO) {
        this.orderDAO = orderDAO;
        this.accountDAO = accountDAO;
        this.exchangeRateDAO = exchangeRateDAO;
    }

    private class TransferFailException extends Exception {
        private TransferFailException(String message) {
            super(message);
        }
    }

    @Override
    @UnitOfWork
    public void accept(Order order) {
        logger.debug("Start order processing job for order_id: {}", order.getId());
        final CurrencyConverter converter = new CurrencyConverter(exchangeRateDAO);

        try {
            Account sender = null;
            Account receiver = null;
            if (order.getOrderType().isOutgoing()) {
                sender = Optional.ofNullable(accountDAO.findById(order.getSenderAccount()))
                        .orElseThrow(() -> new TransferFailException(
                                String.format(SENDER_ACCOUNT_DOES_NOT_EXIST_MSG, order.getSenderAccount())));
                decreaseSenderBalance(sender, converter, order);
            }

            if (order.getOrderType().isIngoing()) {
                receiver = Optional.ofNullable(accountDAO.findById(order.getReceiverAccount()))
                        .orElseThrow(() -> new TransferFailException(
                                String.format(RECEIVER_ACCOUNT_DOES_NOT_EXIST_MSG, order.getReceiverAccount())));
                increaseReceiverBalance(receiver, converter, order);
            }

            if (sender != null) accountDAO.update(sender);
            if (receiver != null) accountDAO.update(receiver);

            order.setStatus(OrderStatus.DONE);
        } catch (TransferFailException | CurrencyConverter.ConvertingException e) {
            logger.warn("Error during order processing with message: {}", e);
            order.setStatus(OrderStatus.FAILED);
            order.setDescription(e.getMessage());
        } finally {
            logger.debug("Finish order processing job for order_id: {}", order.getId());
            orderDAO.update(order);
        }
    }

    private void decreaseSenderBalance(Account sender, CurrencyConverter converter, Order order)
            throws TransferFailException, CurrencyConverter.ConvertingException {
        BigDecimal requiredFromSender = converter.convert(order.getOperationCurrency(), sender.getCurrency(), order.getAmount());
        assertBalanceIsSufficient(sender, requiredFromSender);
        if (logger.isDebugEnabled()) {
            logger.debug("Decreasing balance of {} by {}", sender.getId(), formatCurrency(requiredFromSender, sender.getCurrency()));
        }
        sender.setBalance(sender.getBalance().subtract(requiredFromSender));
    }

    private void increaseReceiverBalance(Account receiver, CurrencyConverter converter, Order order)
            throws CurrencyConverter.ConvertingException {
        BigDecimal transferToReceiver = converter.convert(order.getOperationCurrency(), receiver.getCurrency(), order.getAmount());
        if (logger.isDebugEnabled()) {
            logger.debug("Increasing balance of {} by {}", receiver.getId(), formatCurrency(transferToReceiver, receiver.getCurrency()));
        }
        receiver.setBalance(receiver.getBalance().add(transferToReceiver));
    }

    private void assertBalanceIsSufficient(Account account, BigDecimal required) throws TransferFailException {
        logger.debug("Checking balance of {}", account.getId());
        if (account.getBalance().compareTo(required) < 0) {
            final BigDecimal requiredFromSenderRounded = required.setScale(DISPLAY_AND_COMPARE_SCALE, RoundingMode.UP);
            final BigDecimal senderActualRounded = account.getBalance().setScale(DISPLAY_AND_COMPARE_SCALE, RoundingMode.DOWN);
            throw new TransferFailException(String.format(INSUFFICIENT_FUNDS_MSG,
                    requiredFromSenderRounded.toString() + account.getCurrency().toString(),
                    senderActualRounded.toString()));
        }
        logger.debug("Balance of {} is sufficient", account.getId());
    }

    private String formatCurrency(BigDecimal value, Currency currency) {
        return currencyFormat.format(value.doubleValue()) + currency.toString();
    }
}
