package com.rk.db.dao;


import com.rk.api.Currency;
import com.rk.api.ExchangeRate;
import io.dropwizard.hibernate.AbstractDAO;
import org.hibernate.SessionFactory;

import java.util.List;

public class ExchangeRateDAO extends AbstractDAO<ExchangeRate> {
    public ExchangeRateDAO(SessionFactory sessionFactory) {
        super(sessionFactory);
    }

    public void create(ExchangeRate exchangeRate) {
        persist(exchangeRate);
    }

    public void update(ExchangeRate rate) {
        persist(rate);
    }

    public ExchangeRate find(Currency codeFrom, Currency codeTo) {
         List<ExchangeRate> res = query("from ExchangeRate where currencyCodeFrom=:currencyCodeFrom and currencyCodeTo=:currencyCodeTo")
                .setParameter("currencyCodeFrom", codeFrom)
                .setParameter("currencyCodeTo", codeTo)
                .getResultList();

         return res.isEmpty() ? null : res.get(0);
    }

}
