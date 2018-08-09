package com.rk.resources;


import com.rk.api.Order;
import com.rk.api.OrderStatus;
import com.rk.db.dao.AccountDAO;
import com.rk.db.dao.OrderDAO;
import com.rk.messaging.ObjectMessageProducer;
import io.dropwizard.hibernate.UnitOfWork;

import javax.jms.JMSException;
import javax.validation.Valid;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.util.Optional;

@Path("orders")
public class OrderResource {
    @Context
    private UriInfo uriInfo;
    private final OrderDAO orderDAO;
    private final AccountDAO accountDAO;
    private final ObjectMessageProducer ordersMessageProducer;

    public OrderResource(OrderDAO orderDAO, AccountDAO accountDAO, ObjectMessageProducer ordersMessageProducer) {
        this.orderDAO = orderDAO;
        this.accountDAO = accountDAO;
        this.ordersMessageProducer = ordersMessageProducer;
    }

    @POST
    @UnitOfWork()
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response post(@NotNull @Valid Order order) {
        requireAccountsExist(order);
        order.setStatus(OrderStatus.NEW);
        int orderId = orderDAO.create(order);
        order.setId(orderId);

        try {
            ordersMessageProducer.send(order);
        } catch (JMSException e) {
            throw new WebApplicationException(e.getMessage(), e, Response.Status.INTERNAL_SERVER_ERROR);
        }
        return Response.created(
                uriInfo.getAbsolutePathBuilder()
                        .path(Integer.toString(orderId))
                        .build())
                .entity(order)
                .build();
    }

    @GET
    @UnitOfWork(readOnly = true)
    @Path("{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response get(@PathParam("id") @Min(0) int id) {
        return Optional.ofNullable(orderDAO.findById(id))
                .map(order -> Response.ok()
                        .entity(order)
                        .build())
                .orElseThrow(() -> new WebApplicationException(Response.Status.NOT_FOUND));
    }

    private void requireAccountsExist(Order order) {
        Integer sender = order.getSenderAccount();
        if (sender != null && accountDAO.findById(sender) == null)
            throw new BadRequestException("Cannot submit order. Sender account doesn't exist");

        Integer receiver = order.getReceiverAccount();
        if (receiver != null && accountDAO.findById(receiver) == null)
            throw new BadRequestException("Cannot submit order. Receiver account doesn't exist");
    }
}

