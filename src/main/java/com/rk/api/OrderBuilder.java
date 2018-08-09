package com.rk.api;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public final class OrderBuilder {
    private Integer id;
    private Integer receiverAccount;
    private Integer senderAccount;
    private Currency operationCurrencyCode;
    private BigDecimal amount;
    private String description;
    private OrderStatus status;
    private OrderType orderType;
    private OffsetDateTime creationDate;

    private OrderBuilder() {
    }

    public static OrderBuilder anOrder() {
        return new OrderBuilder();
    }

    public OrderBuilder withId(Integer id) {
        this.id = id;
        return this;
    }

    public OrderBuilder withReceiverAccount(Integer receiverAccount) {
        this.receiverAccount = receiverAccount;
        return this;
    }

    public OrderBuilder withSenderAccount(Integer senderAccount) {
        this.senderAccount = senderAccount;
        return this;
    }

    public OrderBuilder withOperationCurrencyCode(Currency operationCurrencyCode) {
        this.operationCurrencyCode = operationCurrencyCode;
        return this;
    }

    public OrderBuilder withAmount(BigDecimal amount) {
        this.amount = amount;
        return this;
    }

    public OrderBuilder withDescription(String description) {
        this.description = description;
        return this;
    }

    public OrderBuilder withStatus(OrderStatus status) {
        this.status = status;
        return this;
    }

    public OrderBuilder withType(OrderType orderType) {
        this.orderType = orderType;
        return this;
    }

    public OrderBuilder withCreationDate(OffsetDateTime creationDate) {
        this.creationDate = creationDate;
        return this;
    }

    public OrderBuilder copyOf(Order order) {
        id = order.getId();
        receiverAccount = order.getReceiverAccount();
        senderAccount = order.getSenderAccount();
        operationCurrencyCode = order.getOperationCurrency();
        amount = order.getAmount();
        description = order.getDescription();
        status = order.getStatus();
        orderType = order.getOrderType();
        return this;
    }

    public Order build() {
        Order order = new Order();
        order.setId(id);
        order.setReceiverAccount(receiverAccount);
        order.setSenderAccount(senderAccount);
        order.setOperationCurrency(operationCurrencyCode);
        order.setAmount(amount);
        order.setDescription(description);
        order.setStatus(status);
        order.setOrderType(orderType);
        order.setCreationDate(creationDate);
        return order;
    }
}
