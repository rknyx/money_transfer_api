package com.rk.resources.unit;

import com.rk.api.Account;
import com.rk.api.AccountBuilder;
import com.rk.db.dao.AccountDAO;
import com.rk.resources.AccountResource;
import com.rk.resources.ResourceTest;
import com.rk.resources.it.testutil.ResourceRequest;
import io.dropwizard.testing.junit.ResourceTestRule;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.mockito.Mockito;

import javax.ws.rs.core.Response;
import java.math.BigDecimal;

public class AccountResourceTest extends ResourceTest {
    private static final AccountDAO dao = Mockito.mock(AccountDAO.class);
    private static final String INVALID_ACCOUNT_STRING = "{\"Currency\":\"INVALID\"}";
    private ResourceRequest<Account> request;
    private Account sampleAccount;

    @ClassRule
    public static final ResourceTestRule resources = ResourceTestRule.builder()
            .addResource(new AccountResource(dao))
            .build();

    @After
    public void resetMock() {
        Mockito.reset(dao);
    }

    @Before
    public void init() {
        request = createRequest(resources, AccountResource.class, Account.class);
        sampleAccount = AccountBuilder.anAccount().validSample();
    }

    @Test
    public void postValidAccount() {
        Account accountActual = request.post(sampleAccount).getEntity();

        sampleAccount.setId(accountActual.getId());
        sampleAccount.setBalance(BigDecimal.ZERO);

        Assert.assertEquals(sampleAccount, accountActual);
    }

    @Test
    public void postInvalidAccountCausesError() {
        ResourceRequest<String> invalidRequest = createRequest(resources, AccountResource.class, String.class);
        invalidRequest.post(INVALID_ACCOUNT_STRING, String.class, Response.Status.BAD_REQUEST.getStatusCode());
    }

    @Test
    public void putValidAccountCausesPost() {
        Account accountActual = request.put(sampleAccount).getEntity();

        sampleAccount.setId(accountActual.getId());
        sampleAccount.setBalance(BigDecimal.ZERO);

        Assert.assertEquals(sampleAccount, accountActual);
    }

    @Test
    public void putInvalidAccountCausesError() {
        createRequest(resources, AccountResource.class, String.class)
                .put(INVALID_ACCOUNT_STRING, String.class, Response.Status.BAD_REQUEST.getStatusCode());
    }

    @Test
    public void accountModificationCausesError() {
        Integer accountId = request.post(sampleAccount).getEntity().getId();
        request.path(accountId.toString()).put(sampleAccount, String.class, Response.Status.METHOD_NOT_ALLOWED.getStatusCode());
    }
}
