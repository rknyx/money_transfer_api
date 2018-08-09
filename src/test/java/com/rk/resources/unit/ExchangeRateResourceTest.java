package com.rk.resources.unit;

import com.rk.api.Currency;
import com.rk.api.ExchangeRate;
import com.rk.db.dao.ExchangeRateDAO;
import com.rk.resources.ExchangeRateResource;
import com.rk.resources.FixtureUtil;
import com.rk.resources.ResourceTest;
import com.rk.resources.it.testutil.ResourceRequest;
import io.dropwizard.testing.junit.ResourceTestRule;
import org.junit.Assert;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.Response;
import java.math.BigDecimal;

public class ExchangeRateResourceTest extends ResourceTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(OrderResourceTest.class);
    private static final ExchangeRateDAO dao = Mockito.mock(ExchangeRateDAO.class);
    private ResourceRequest<ExchangeRate> request;
    private ExchangeRate sampleRate;

    @ClassRule
    public static final ResourceTestRule resources = ResourceTestRule.builder()
            .addResource(new ExchangeRateResource(dao))
            .build();

    @Before
    public void resetMock() {
        Mockito.reset(dao);
        request = createRequest(resources, ExchangeRateResource.class, ExchangeRate.class);
        sampleRate = new ExchangeRate(Currency.EUR, Currency.USD, BigDecimal.valueOf(1.0));
    }

    @Test
    public void postValidExchangeRate() {
        Assert.assertEquals(sampleRate, request.post(sampleRate).getEntity());
    }

    @Test
    public void putValidExchangeRate() {
        request.post(sampleRate);

        sampleRate.setRate(sampleRate.getRate().multiply(BigDecimal.TEN));
        Assert.assertEquals(sampleRate, request.put(sampleRate).getEntity());
    }

    @Test
    public void postWithConflictCausesError() {
        Mockito.when(dao.find(Mockito.any(), Mockito.any())).thenReturn(new ExchangeRate());
        request.post(sampleRate, String.class, Response.Status.CONFLICT.getStatusCode());
    }

    @Test
    public void postIncorrectExchangeRate() {
        ResourceRequest<String> incorrectRequest = createRequest(resources, ExchangeRateResource.class, String.class);
        FixtureUtil.jsonStringsFromFixtureDirectory("rates")
                .forEach(exchangeRateString -> {
                    LOGGER.debug("trying to post exchange rate: {}", exchangeRateString);
                    incorrectRequest.post(exchangeRateString, String.class, Response.Status.BAD_REQUEST.getStatusCode(),
                            ResourceRequest.UNPROCESSABLE_ENTITY);
                });
    }
}
