package com.rk.core;

import com.rk.api.Currency;
import com.rk.db.dao.ExchangeRateDAO;

import java.math.BigDecimal;
import java.util.Optional;

/**
 * Class intended to encapsulate currency conversion math
 */
public class CurrencyConverter {
    private static final String NO_EXCHANGE_RATE_MSG = "No exchange rate info for %s->%s currencies";
    private final ExchangeRateDAO exchangeRateDAO;

    public CurrencyConverter(ExchangeRateDAO exchangeRateDAO) {
        this.exchangeRateDAO = exchangeRateDAO;
    }

    public class ConvertingException extends Exception {
        public ConvertingException(String message) {
            super(message);
        }
    }

    public BigDecimal convert(Currency from, Currency to, BigDecimal amount) throws ConvertingException {
        BigDecimal multiplier = from == to
                ? BigDecimal.ONE
                : Optional.ofNullable(exchangeRateDAO.find(from, to))
                .orElseThrow(() -> new ConvertingException(String.format(NO_EXCHANGE_RATE_MSG, from, to))).getRate();
        return amount.multiply(multiplier);
    }
}
