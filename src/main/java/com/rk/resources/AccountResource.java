package com.rk.resources;

import com.rk.api.Account;
import com.rk.db.dao.AccountDAO;
import io.dropwizard.hibernate.UnitOfWork;

import javax.validation.Valid;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.math.BigDecimal;
import java.util.Optional;

@Path("accounts")
public class AccountResource {
    private static final String ACCOUNT_UPDATES_ARE_PROHIBITED = "account updates are prohibited";

    @Context
    private UriInfo uriInfo;
    private final AccountDAO accountDAO;

    public AccountResource(AccountDAO accountDAO) {
        this.accountDAO = accountDAO;
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @UnitOfWork
    public Response post(@Valid Account account) {
        account.setId(null);
        account.setBalance(BigDecimal.ZERO);
        Integer accountId = accountDAO.create(account);
        account.setId(accountId);
        return Response.created(
                uriInfo.getAbsolutePathBuilder()
                        .path(accountId.toString())
                        .build())
                .entity(account)
                .build();
    }

    @PUT
    @Path("{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response put(@PathParam("id") int id, @Valid Account account) {
        throw new WebApplicationException(ACCOUNT_UPDATES_ARE_PROHIBITED, Response.Status.METHOD_NOT_ALLOWED);
    }

    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @UnitOfWork
    public Response put(@Valid Account account) {
        return post(account);
    }

    @GET
    @Path("{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @UnitOfWork(readOnly = true)
    public Response get(@PathParam("id") int id) {
        return Optional.ofNullable(accountDAO.findById(id))
                .map(account -> Response.created(
                        uriInfo.getAbsolutePathBuilder()
                                .path(Integer.toString(id))
                                .build())
                        .entity(account)
                        .build())
                .orElseThrow(NotFoundException::new);
    }

}
