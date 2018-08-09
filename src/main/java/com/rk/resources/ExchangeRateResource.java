package com.rk.resources;

import com.rk.api.Currency;
import com.rk.api.ExchangeRate;
import com.rk.db.dao.ExchangeRateDAO;
import io.dropwizard.hibernate.UnitOfWork;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
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

@Path("rates")
public class ExchangeRateResource {
    @Context
    private UriInfo uriInfo;
    private final ExchangeRateDAO exchangeRateDAO;

    public ExchangeRateResource(ExchangeRateDAO exchangeRateDAO) {
        this.exchangeRateDAO = exchangeRateDAO;
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @UnitOfWork
    public Response post(@NotNull @Valid ExchangeRate exchangeRate) {
        if (exchangeRateDAO.find(exchangeRate.getCurrencyCodeFrom(), exchangeRate.getCurrencyCodeTo()) != null) {
            throw new WebApplicationException("Exchange rate already exists" + exchangeRate.toString(),
                    Response.Status.CONFLICT);
        }
        exchangeRateDAO.create(exchangeRate);
        return Response.created(
                uriInfo.getAbsolutePathBuilder()
                        .path(exchangeRate.getCurrencyCodeFrom().name())
                        .path(exchangeRate.getCurrencyCodeTo().name())
                        .build())
                .entity(exchangeRate)
                .build();
    }

    @GET
    @Path("{from}/{to}")
    @Produces(MediaType.APPLICATION_JSON)
    @UnitOfWork(readOnly = true)
    public Response get(@PathParam("from") @NotNull Currency from, @PathParam("to") @NotNull Currency to) {
        ExchangeRate rate = exchangeRateDAO.find(from, to);
        if (rate == null) {
            throw new NotFoundException();
        } else {
            return Response.ok()
                    .entity(rate)
                    .build();
        }
    }

    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @UnitOfWork
    public Response put(@NotNull @Valid ExchangeRate exchangeRate) {
        ExchangeRate existing = exchangeRateDAO.find(exchangeRate.getCurrencyCodeFrom(), exchangeRate.getCurrencyCodeTo());
        if (existing == null) {
            return post(exchangeRate);
        }
        existing.setRate(exchangeRate.getRate());
        exchangeRateDAO.update(existing);
        return Response.ok().build();
    }
}
