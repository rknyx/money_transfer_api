package com.rk.resources.it;

import com.rk.api.Account;
import com.rk.api.AccountBuilder;
import com.rk.resources.AccountResource;
import com.rk.resources.it.testutil.ResourceRequest;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import javax.ws.rs.core.Response;

public class AccountResourceIT extends ResourceIT {
    private Account expectedAccount;
    private static ResourceRequest<Account> request;


    @Before
    public void initAccount() {
        expectedAccount = AccountBuilder.anAccount().validSample();
        request = createRequest(AccountResource.class, Account.class);
    }

    @Test
    public void postAccountTest() {
        Account actualAccount = request.post(expectedAccount).getEntity();
        expectedAccount.setId(actualAccount.getId());
        Assert.assertEquals(actualAccount, expectedAccount);
    }

    @Test
    public void putExistingAccountTest() {
        int id = request.post(expectedAccount).getEntity().getId();
        request.path(Integer.toString(id))
                .put(expectedAccount, String.class, Response.Status.METHOD_NOT_ALLOWED.getStatusCode());
    }

    @Test
    public void putNonExistentAccountTest() {
        request.path("1000")
                .put(expectedAccount, String.class, Response.Status.METHOD_NOT_ALLOWED.getStatusCode());
    }

    @Test
    public void getAccountTest() {
        Integer id = request.post(expectedAccount).getEntity().getId();
        expectedAccount.setId(id);
        Account actualAccount = request.path(id.toString()).get().getEntity();
        Assert.assertEquals(expectedAccount, actualAccount);
    }
}
