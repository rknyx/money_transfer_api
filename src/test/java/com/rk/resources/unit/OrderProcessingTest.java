package com.rk.resources.unit;

import com.google.common.collect.ImmutableTable;
import com.google.common.collect.Table;
import com.rk.api.Account;
import com.rk.api.AccountBuilder;
import com.rk.api.Currency;
import com.rk.api.ExchangeRate;
import com.rk.api.Order;
import com.rk.api.OrderBuilder;
import com.rk.api.OrderStatus;
import com.rk.api.OrderType;
import com.rk.core.CurrencyConverter;
import com.rk.core.OrderProcessingWorker;
import com.rk.db.dao.AccountDAO;
import com.rk.db.dao.ExchangeRateDAO;
import com.rk.db.dao.OrderDAO;
import org.apache.commons.lang3.StringUtils;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.math.BigDecimal;

public class OrderProcessingTest {
    private static final Account RECEIVER_SAMPLE = new Account(4, Currency.USD, BigDecimal.valueOf(5.0));
    private static final Account SENDER_SAMPLE = new Account(7, Currency.EUR, BigDecimal.valueOf(5.0));

    private final Order ORDER_SAMPLE = OrderBuilder.anOrder()
            .withAmount(BigDecimal.valueOf(20))
            .withOperationCurrencyCode(Currency.RUB)
            .withReceiverAccount(RECEIVER_SAMPLE.getId())
            .withSenderAccount(SENDER_SAMPLE.getId())
            .withId(2)
            .build();

    private final Table<Currency, Currency, BigDecimal> exchangeRateTable = ImmutableTable.<Currency, Currency, BigDecimal>builder()
            .put(Currency.USD, Currency.EUR, BigDecimal.valueOf(0.8))
            .put(Currency.EUR, Currency.USD, BigDecimal.valueOf(1.25))
            .put(Currency.USD, Currency.RUB, BigDecimal.valueOf(56.56))
            .put(Currency.RUB, Currency.USD, BigDecimal.valueOf(0.01768033946))
            .put(Currency.RUB, Currency.EUR, BigDecimal.valueOf(0.01414427157))
            .put(Currency.EUR, Currency.RUB, BigDecimal.valueOf(70.7))
            .build();

    private OrderDAO orderDAO = Mockito.mock(OrderDAO.class);
    private AccountDAO accountDAO = Mockito.mock(AccountDAO.class);
    private ExchangeRateDAO exchangeRateDAO = Mockito.mock(ExchangeRateDAO.class);
    private CurrencyConverter currencyConverter = new CurrencyConverter(exchangeRateDAO);
    private OrderProcessingWorker orderProcessingWorker = new OrderProcessingWorker(orderDAO, accountDAO, exchangeRateDAO);


    private Account receiver;
    private Account sender;
    private Order order;

    @After
    public void resetMock() {
        Mockito.reset(orderDAO, accountDAO, exchangeRateDAO);
        receiver = null;
        sender = null;
    }

    @Before
    public void mockCurrencies() {
        exchangeRateTable.rowKeySet().forEach(from ->
                exchangeRateTable.rowKeySet().forEach(to ->
                        Mockito.when(exchangeRateDAO.find(from, to)).thenReturn(
                                new ExchangeRate(from, to, exchangeRateTable.get(from, to)))
                ));
    }

    @Test
    public void incomeNoReceiver() {
        order = buildOrderWithType(OrderType.INCOME);
        mockDatabaseObjects();
        orderProcessingWorker.accept(order);

        String expectedMessage = String.format(OrderProcessingWorker.RECEIVER_ACCOUNT_DOES_NOT_EXIST_MSG, RECEIVER_SAMPLE.getId());
        assertOrderStatusAndMessage(OrderStatus.FAILED, expectedMessage);
    }

    @Test
    public void income() {
        receiver = AccountBuilder.anAccount().copyOf(RECEIVER_SAMPLE).build();
        order = buildOrderWithType(OrderType.INCOME);
        mockDatabaseObjects();
        orderProcessingWorker.accept(order);

        assertOrderStatusAndMessage(OrderStatus.DONE, null);
        assertBalancesAreUpdatedCorrectly();
    }

    @Test
    public void outcomeNoSender() {
        receiver = AccountBuilder.anAccount().copyOf(RECEIVER_SAMPLE).build();
        order = buildOrderWithType(OrderType.OUTCOME);
        mockDatabaseObjects();
        orderProcessingWorker.accept(order);

        String expectedMessage = String.format(OrderProcessingWorker.SENDER_ACCOUNT_DOES_NOT_EXIST_MSG, SENDER_SAMPLE.getId());
        assertOrderStatusAndMessage(OrderStatus.FAILED, expectedMessage);

        assertNoAccountUpdate();
    }

    @Test
    public void outcomeNotEnoughFunds() {
        sender = AccountBuilder.anAccount().copyOf(SENDER_SAMPLE).build();
        order = buildOrderWithType(OrderType.OUTCOME);
        order.setAmount(sender.getBalance().add(BigDecimal.valueOf(1000)));
        mockDatabaseObjects();

        orderProcessingWorker.accept(order);
        String expectedMessage = "Insufficient funds";
        assertOrderStatusAndMessage(OrderStatus.FAILED, expectedMessage);

        assertNoAccountUpdate();
    }

    @Test
    public void outcome() {
        sender = AccountBuilder.anAccount().copyOf(SENDER_SAMPLE).build();
        order = buildOrderWithType(OrderType.OUTCOME);
        order.setAmount(sender.getBalance());
        mockDatabaseObjects();

        orderProcessingWorker.accept(order);
        assertOrderStatusAndMessage(OrderStatus.DONE, null);
        assertBalancesAreUpdatedCorrectly();
    }

    @Test
    public void transferNoSender() {
        receiver = AccountBuilder.anAccount().copyOf(RECEIVER_SAMPLE).build();
        order = buildOrderWithType(OrderType.TRANSFER);
        mockDatabaseObjects();

        orderProcessingWorker.accept(order);
        String expectedMessage = String.format(OrderProcessingWorker.SENDER_ACCOUNT_DOES_NOT_EXIST_MSG, SENDER_SAMPLE.getId());
        assertOrderStatusAndMessage(OrderStatus.FAILED, expectedMessage);

        assertNoAccountUpdate();
    }

    @Test
    public void transferNoReceiver() {
        sender = AccountBuilder.anAccount().copyOf(SENDER_SAMPLE).build();
        order = buildOrderWithType(OrderType.TRANSFER);
        mockDatabaseObjects();

        orderProcessingWorker.accept(order);//looks like no transaction here :(
        String expectedMessage = String.format(OrderProcessingWorker.RECEIVER_ACCOUNT_DOES_NOT_EXIST_MSG, RECEIVER_SAMPLE.getId());
        assertOrderStatusAndMessage(OrderStatus.FAILED, expectedMessage);

        assertNoAccountUpdate();
    }

    @Test
    public void transferNotEnoughFunds() {
        sender = AccountBuilder.anAccount().copyOf(SENDER_SAMPLE).build();
        receiver = AccountBuilder.anAccount().copyOf(RECEIVER_SAMPLE).build();
        order = buildOrderWithType(OrderType.TRANSFER);
        order.setAmount(BigDecimal.valueOf(1000000));
        mockDatabaseObjects();

        orderProcessingWorker.accept(order);
        assertOrderStatusAndMessage(OrderStatus.FAILED, "Insufficient funds");
        assertBalancesRemainTheSame();
    }

    @Test
    public void transfer() throws CurrencyConverter.ConvertingException {
        sender = AccountBuilder.anAccount().copyOf(SENDER_SAMPLE).build();
        receiver = AccountBuilder.anAccount().copyOf(RECEIVER_SAMPLE).build();
        order = buildOrderWithType(OrderType.TRANSFER);
        order.setAmount(currencyConverter.convert(sender.getCurrency(), order.getOperationCurrency(),
                sender.getBalance().subtract(BigDecimal.ONE)));
        mockDatabaseObjects();

        orderProcessingWorker.accept(order);
        assertOrderStatusAndMessage(OrderStatus.DONE, null);
        assertBalancesAreUpdatedCorrectly();
    }

    private Order buildOrderWithType(OrderType orderType) {
        return OrderBuilder.anOrder()
                .copyOf(ORDER_SAMPLE)
                .withType(orderType)
                .build();
    }

    private void assertOrderStatusAndMessage(OrderStatus expectedStatus, String expectedDescription) {
        final String orderDescription = StringUtils.defaultString(order.getDescription(), "null");
        final OrderStatus orderStatus = order.getStatus();

        if (expectedStatus != null) {
            Assert.assertEquals(String.format("Order status is incorrect, expected: '%s', actual: '%s'", expectedStatus,
                    orderStatus), expectedStatus, orderStatus);
        }

        if (expectedDescription != null) {
            Assert.assertTrue(String.format("Order description is incorrect, expected: '%s', actual: '%s", expectedDescription,
                    orderDescription), orderDescription.contains(expectedDescription));
        }

        Mockito.verify(orderDAO).update(Mockito.argThat((_order) -> _order.equals(order)));
    }

    private void assertBalancesRemainTheSame() {
        assertBalances(BigDecimal.ZERO);
    }

    private void assertBalancesAreUpdatedCorrectly() {
        assertBalances(order.getAmount());
    }

    private void assertNoAccountUpdate() {
        Mockito.verify(accountDAO, Mockito.never()).update(Mockito.any());
    }

    private void assertBalances(BigDecimal expectedOrderAmount) {
        final Currency operationalCurrency = order.getOperationCurrency();

        try {
            if (receiver != null) {
                BigDecimal before = RECEIVER_SAMPLE.getBalance();
                BigDecimal expected = before.add(currencyConverter.convert(operationalCurrency, receiver.getCurrency(),
                        expectedOrderAmount));
                BigDecimal actual = receiver.getBalance();
                assertBigDecimalEquals(expected, actual);
            }
            if (sender != null) {
                BigDecimal before = SENDER_SAMPLE.getBalance();
                BigDecimal expected = before.subtract(currencyConverter.convert(operationalCurrency, sender.getCurrency(),
                        expectedOrderAmount));
                BigDecimal actual = sender.getBalance();
                assertBigDecimalEquals(expected, actual);
            }
        } catch (CurrencyConverter.ConvertingException e) {
            throw new AssertionError("Unexpected converting exception");
        }
    }

    private void assertBigDecimalEquals(BigDecimal actual, BigDecimal expected) {
        String errorMessage = String.format("Incorrect account balance, expected: '%s', actual: '%s'", expected, actual);
        Assert.assertTrue(errorMessage, expected.compareTo(actual) == 0);
    }

    private void mockDatabaseObjects() {
        Mockito.when(accountDAO.findById(RECEIVER_SAMPLE.getId())).thenReturn(receiver);
        Mockito.when(accountDAO.findById(SENDER_SAMPLE.getId())).thenReturn(sender);
        Mockito.when(orderDAO.findById(ORDER_SAMPLE.getId())).thenReturn(order);
    }
}
