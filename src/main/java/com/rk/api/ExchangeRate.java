package com.rk.api;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.dropwizard.validation.ValidationMethod;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Objects;

@Entity
@Table(name = "exchange_rates")
public class ExchangeRate implements Serializable {
    @Id //yes, two id fields are not JPA compliant. But fix is very simple. Will do if migrate from hibernate
    @NotNull
    @Column(name = "currency_code_from")
    private Currency currencyCodeFrom;

    @Id
    @NotNull
    @Column(name = "currency_code_to")
    private Currency currencyCodeTo;

    @NotNull
    @DecimalMin("0.00001")
    @Column(name = "rate")
    private BigDecimal rate;

    public ExchangeRate(Currency currencyCodeFrom, Currency currencyCodeTo, BigDecimal rate) {
        this.currencyCodeFrom = currencyCodeFrom;
        this.currencyCodeTo = currencyCodeTo;
        this.rate = rate;
    }

    public ExchangeRate(ExchangeRate that) {
        this.currencyCodeFrom = that.currencyCodeFrom;
        this.currencyCodeTo = that.currencyCodeTo;
        this.rate = that.rate;
    }

    public ExchangeRate() {}

    public Currency getCurrencyCodeFrom() {
        return currencyCodeFrom;
    }

    public void setCurrencyCodeFrom(Currency currencyCodeFrom) {
        this.currencyCodeFrom = currencyCodeFrom;
    }

    public Currency getCurrencyCodeTo() {
        return currencyCodeTo;
    }

    public void setCurrencyCodeTo(Currency currencyCodeTo) {
        this.currencyCodeTo = currencyCodeTo;
    }

    public BigDecimal getRate() {
        return rate;
    }

    public void setRate(BigDecimal rate) {
        this.rate = rate;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ExchangeRate that = (ExchangeRate) o;
        return Objects.equals(currencyCodeFrom, that.currencyCodeFrom) &&
                Objects.equals(currencyCodeTo, that.currencyCodeTo) &&
                rate.compareTo(that.rate) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(currencyCodeFrom, currencyCodeTo, rate);
    }

    @Override
    public String toString() {
        return "ExchangeRate{" +
                "currencyCodeFrom='" + currencyCodeFrom + '\'' +
                ", currencyCodeTo='" + currencyCodeTo + '\'' +
                ", rate=" + rate +
                '}';
    }

    @ValidationMethod(message = "Exchange rate cannot have equal to and from currencies")
    @JsonIgnore
    public boolean isNotEqualToAndFrom() {
        return currencyCodeFrom != currencyCodeTo;
    }
}
