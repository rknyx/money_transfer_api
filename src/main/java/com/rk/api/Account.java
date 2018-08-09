package com.rk.api;


import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.Objects;

@Entity
@Table(name = "accounts")
public class Account {
    @Min(0)
    @Id
    @Column(name = "account_id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @NotNull
    @Column(name = "currency_code")
    private Currency currency;

    @Column(name = "balance")
    private BigDecimal balance;

    public Account(Integer id, Currency currency, BigDecimal balance) {
        this.id = id;
        this.currency = currency;
        this.balance = balance;
    }

    public Account() {}

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Currency getCurrency() {
        return currency;
    }

    public void setCurrency(Currency currency) {
        this.currency = currency;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public void setBalance(BigDecimal balance) {
        this.balance = balance;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Account account = (Account) o;
        return Objects.equals(id, account.id) &&
                currency == account.currency &&
                balance.compareTo(account.balance) == 0;
    }

    @Override
    public int hashCode() {

        return Objects.hash(id, currency, balance);
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("Account{");
        sb.append("id=").append(id);
        sb.append(", currency=").append(currency);
        sb.append(", balance=").append(balance);
        sb.append('}');
        return sb.toString();
    }
}
