package com.rk.api;

import java.math.BigDecimal;

public final class AccountBuilder {
    private int id;
    private Currency currency;
    private BigDecimal balance;

    private AccountBuilder() {
    }

    public static AccountBuilder anAccount() {
        return new AccountBuilder();
    }

    public AccountBuilder withId(int id) {
        this.id = id;
        return this;
    }

    public AccountBuilder withCurrency(Currency currency) {
        this.currency = currency;
        return this;
    }

    public AccountBuilder withBalance(BigDecimal balance) {
        this.balance = balance;
        return this;
    }

    public AccountBuilder copyOf(Account other) {
        id = other.getId();
        balance = other.getBalance();
        currency = other.getCurrency();
        return this;
    }

    public Account validSample() {
        return new Account(0, Currency.USD, BigDecimal.valueOf(0.0));
    }

    public Account build() {
        Account account = new Account();
        account.setId(id);
        account.setCurrency(currency);
        account.setBalance(balance);
        return account;
    }
}
