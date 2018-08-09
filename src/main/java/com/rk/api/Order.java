package com.rk.api;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.io.Serializable;
import java.time.OffsetDateTime;
import java.util.Objects;
import io.dropwizard.validation.ValidationMethod;
import org.apache.commons.lang3.ObjectUtils;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;

@Entity
@Table(name = "orders")
public class Order implements Serializable {
    @Id
    @Column(name = "order_id")
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "creation_date")
    private OffsetDateTime creationDate;

    @Column(name = "receiver_account")
    private Integer receiverAccount;

    @Column(name = "sender_account")
    private Integer senderAccount;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "operation_currency_code")
    private Currency operationCurrencyCode;

    @NotNull
    @DecimalMin("0.0001")
    @Column
    private BigDecimal amount;

    @NotNull
    @Enumerated(EnumType.ORDINAL)
    @Column(name = "order_type")
    private OrderType orderType;

    @Column
    private String description;

    @Column(name = "order_status")
    @Enumerated(EnumType.ORDINAL)
    private OrderStatus status;

    public Integer getId() {
        return id;
    }

    public void setId(Integer orderId) {
        this.id = orderId;
    }

    public Integer getSenderAccount() {
        return senderAccount;
    }

    public void setSenderAccount(Integer senderAccount) {
        this.senderAccount = senderAccount;
    }

    public Integer getReceiverAccount() {
        return receiverAccount;
    }

    public void setReceiverAccount(Integer receiverAccount) {
        this.receiverAccount = receiverAccount;
    }

    public Currency getOperationCurrency() {
        return operationCurrencyCode;
    }

    public void setOperationCurrency(Currency operationCurrencyCode) {
        this.operationCurrencyCode = operationCurrencyCode;
    }

    public OrderStatus getStatus() {
        return this.status;
    }

    public void setStatus(OrderStatus status) {
        this.status = status;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public OrderType getOrderType() {
        return orderType;
    }

    public void setOrderType(OrderType orderType) {
        this.orderType = orderType;
    }

    public OffsetDateTime getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(OffsetDateTime creationDate) {
        this.creationDate = creationDate;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("Order{");
        sb.append("id=").append(id);
        sb.append(", creationDate=").append(creationDate);
        sb.append(", receiverAccount=").append(receiverAccount);
        sb.append(", senderAccount=").append(senderAccount);
        sb.append(", operationCurrencyCode=").append(operationCurrencyCode);
        sb.append(", amount=").append(amount);
        sb.append(", orderType=").append(orderType);
        sb.append(", description='").append(description).append('\'');
        sb.append(", status='").append(status).append('\'');
        sb.append('}');
        return sb.toString();
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Order order = (Order) o;
        return Objects.equals(id, order.id) &&
                Objects.equals(creationDate, order.creationDate) &&
                Objects.equals(receiverAccount, order.receiverAccount) &&
                Objects.equals(senderAccount, order.senderAccount) &&
                operationCurrencyCode == order.operationCurrencyCode &&
                Objects.equals(amount, order.amount) &&
                orderType == order.orderType &&
                Objects.equals(description, order.description) &&
                Objects.equals(status, order.status);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, creationDate, receiverAccount, senderAccount, operationCurrencyCode,
                amount, orderType, description, status);
    }

    @ValidationMethod(message = "Income order cannot contains sender")
    @JsonIgnore
    public boolean isIncomeWithoutSender() {
        return orderType != OrderType.INCOME || senderAccount == null;
    }

    @ValidationMethod(message = "Outcome order cannot contains receiver")
    @JsonIgnore
    public boolean isOutcomeWithoutReceiver() {
        return orderType != OrderType.OUTCOME || receiverAccount == null;
    }

    @ValidationMethod(message = "Transfer order should contain different not empty sender and receiver")
    @JsonIgnore
    public boolean isTransferWithSenderAndReceiver() {
        return orderType != OrderType.TRANSFER || (ObjectUtils.allNotNull(senderAccount, receiverAccount)
                && !Objects.equals(senderAccount, receiverAccount));
    }
}