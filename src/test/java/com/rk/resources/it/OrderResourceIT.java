package com.rk.resources.it;

import com.google.common.util.concurrent.Uninterruptibles;
import com.rk.api.Account;
import com.rk.api.AccountBuilder;
import com.rk.api.Currency;
import com.rk.api.ExchangeRate;
import com.rk.api.Order;
import com.rk.api.OrderBuilder;
import com.rk.api.OrderStatus;
import com.rk.api.OrderType;
import com.rk.resources.AccountResource;
import com.rk.resources.ExchangeRateResource;
import com.rk.resources.OrderResource;
import com.rk.resources.it.testutil.ResourceRequest;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.math.BigDecimal;
import java.util.concurrent.TimeUnit;

public class OrderResourceIT extends ResourceIT {
    private static ResourceRequest<Order> orderRequest;
    private static ResourceRequest<Account> accountRequest;
    private static ResourceRequest<ExchangeRate> exchangeRateRequest;


    @Before
    public void init() {
        orderRequest = createRequest(OrderResource.class, Order.class);
        accountRequest = createRequest(AccountResource.class, Account.class);
        exchangeRateRequest = createRequest(ExchangeRateResource.class, ExchangeRate.class);
    }

    @Test
    public void transferTest() {
        // -- precondition -- //
        final BigDecimal amount = BigDecimal.valueOf(5.0);
        Account receiverBeforeTransfer = accountRequest.post(
                AccountBuilder.anAccount()
                        .withCurrency(Currency.EUR)
                        .withBalance(BigDecimal.ZERO)
                        .build()).getEntity();

        Account senderBeforeTransfer = accountRequest.post(
                AccountBuilder.anAccount()
                        .withCurrency(Currency.USD)
                        .withBalance(BigDecimal.ZERO)
                        .build()).getEntity();

        final Order receiverIncomeOrder = orderRequest.post(OrderBuilder.anOrder()
                .withType(OrderType.INCOME)
                .withOperationCurrencyCode(receiverBeforeTransfer.getCurrency())
                .withReceiverAccount(receiverBeforeTransfer.getId())
                .withAmount(BigDecimal.valueOf(20.0))
                .build()).getEntity();

        final Order senderIncomeOrder = orderRequest.post(OrderBuilder.anOrder()
                .copyOf(receiverIncomeOrder)
                .withReceiverAccount(senderBeforeTransfer.getId())
                .withOperationCurrencyCode(senderBeforeTransfer.getCurrency())
                .build()).getEntity();

        waitForOrderProcessedAndGet(receiverIncomeOrder.getId());
        waitForOrderProcessedAndGet(senderIncomeOrder.getId());

        receiverBeforeTransfer = accountRequest.path(receiverBeforeTransfer.getId().toString()).get().getEntity();
        senderBeforeTransfer = accountRequest.path(senderBeforeTransfer.getId().toString()).get().getEntity();

        final Order orderExpected = OrderBuilder.anOrder()
                .withStatus(OrderStatus.NEW)
                .withType(OrderType.TRANSFER)
                .withAmount(amount)
                .withOperationCurrencyCode(Currency.GBP)
                .withReceiverAccount(receiverBeforeTransfer.getId())
                .withSenderAccount(senderBeforeTransfer.getId())
                .build();

        final ExchangeRate orderToSenderRate = exchangeRateRequest.post(
                new ExchangeRate(orderExpected.getOperationCurrency(), receiverBeforeTransfer.getCurrency(),
                        BigDecimal.valueOf(0.5))).getEntity();

        final ExchangeRate orderToReceiverRate = exchangeRateRequest.post(
                new ExchangeRate(orderExpected.getOperationCurrency(), senderBeforeTransfer.getCurrency(),
                        BigDecimal.valueOf(0.5))).getEntity();
        // -- end of precondition -- //

        Integer orderId = orderRequest.post(orderExpected).getEntity().getId();
        waitForOrderProcessedAndGet(orderId);

        final Account receiverActual = accountRequest.path(receiverBeforeTransfer.getId().toString()).get().getEntity();
        final Account senderActual = accountRequest.path(senderBeforeTransfer.getId().toString()).get().getEntity();

        BigDecimal senderBalanceExpected = senderBeforeTransfer.getBalance()
                .subtract(orderExpected.getAmount().multiply(orderToSenderRate.getRate()));
        BigDecimal receiverBalanceExpected = receiverBeforeTransfer.getBalance()
                .add(orderExpected.getAmount().multiply(orderToReceiverRate.getRate()));

        BigDecimal senderBalanceActual = senderActual.getBalance();
        BigDecimal receiverBalanceActual = receiverActual.getBalance();

        Assert.assertTrue(String.format("Incorrect sender balance. '%s' is expected, but actual is '%s'",
                senderBalanceExpected, senderBalanceActual),
                senderBalanceExpected.compareTo(senderBalanceActual) == 0);

        Assert.assertTrue(String.format("Incorrect receiver balance. '%s' is expected, but actual is '%s'",
                receiverBalanceExpected, receiverBalanceActual),
                receiverBalanceExpected.compareTo(receiverBalanceActual) == 0);
    }

    private void waitForOrderProcessedAndGet(Integer orderId) {
        Order orderActual = null;
        final int ATTEMPTS_COUNT = 10;
        for (int i = 0; i < ATTEMPTS_COUNT; i ++) {
            Uninterruptibles.sleepUninterruptibly(1, TimeUnit.SECONDS);
            orderActual = orderRequest.path(orderId.toString()).get().getEntity();
            if (orderActual.getStatus().equals(OrderStatus.DONE)) {
                return;
            }
        }
        final String msg = "Order is expected to be '%s', but actual is '%s' after '%s' attempts. Description: '%s'";
        throw new AssertionError(String.format(msg, OrderStatus.DONE.toString(), orderActual.getStatus(),
                ATTEMPTS_COUNT, orderActual.getDescription()));
    }
}
