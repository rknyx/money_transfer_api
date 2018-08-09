package com.rk.db.dao;

import com.rk.api.Account;
import io.dropwizard.hibernate.AbstractDAO;
import org.hibernate.SessionFactory;

public class AccountDAO extends AbstractDAO<Account> {
    public AccountDAO(SessionFactory sessionFactory) {
        super(sessionFactory);
    }

    public int create(Account account) {
        return persist(account).getId();
    }

    public void update(Account account) {
        persist(account);
    }

    public Account findById(int id) {
        return get(id);
    }
}
