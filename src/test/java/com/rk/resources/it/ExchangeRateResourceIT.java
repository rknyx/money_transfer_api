package com.rk.resources.it;

import com.google.common.collect.ImmutableSet;
import com.rk.api.Currency;
import com.rk.api.ExchangeRate;
import com.rk.resources.ExchangeRateResource;
import com.rk.resources.it.testutil.ResourceRequest;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.math.BigDecimal;
import java.util.Iterator;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class ExchangeRateResourceIT extends ResourceIT {
    private ExchangeRate expectedRate;
    private ResourceRequest<ExchangeRate> request;
    private static final Set<Currency> currencies = ImmutableSet.copyOf(Currency.values());

    private static final Iterator<ImmutablePair<Currency, Currency>> allCurrenciesPairsIterator = currencies.stream()
            .flatMap(left -> currencies.stream().map(right -> new ImmutablePair<>(left, right)))
            .filter(pair -> !pair.getLeft().equals(pair.getRight()))
            .collect(Collectors.toList()).iterator();

    @Before
    public void initTest() {
        Pair<Currency, Currency> currencyPair = Optional.ofNullable(allCurrenciesPairsIterator.next()).orElseThrow(
                () -> new RuntimeException("Not enough currency pairs for all tests"));
        expectedRate = new ExchangeRate(currencyPair.getLeft(), currencyPair.getRight(), BigDecimal.valueOf(0.5));
        request = createRequest(ExchangeRateResource.class, ExchangeRate.class);
    }

    @Test
    public void postExchangeRateTest() {
        ExchangeRate actualRate = request.post(expectedRate).getEntity();
        Assert.assertEquals(actualRate, expectedRate);
    }

    @Test
    public void putExchangeRate() {
        request.post(expectedRate);
        expectedRate.setRate(BigDecimal.valueOf(3.14));
        request.put(expectedRate);
        ExchangeRate actualRate = request
                .path(expectedRate.getCurrencyCodeFrom().toString())
                .path(expectedRate.getCurrencyCodeTo().toString())
                .get().getEntity();

        Assert.assertEquals(expectedRate, actualRate);
    }
}
