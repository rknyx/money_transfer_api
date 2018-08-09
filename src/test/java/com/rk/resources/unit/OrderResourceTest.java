package com.rk.resources.unit;

import com.rk.api.Account;
import com.rk.api.AccountBuilder;
import com.rk.api.Currency;
import com.rk.api.Order;
import com.rk.api.OrderBuilder;
import com.rk.api.OrderStatus;
import com.rk.api.OrderType;
import com.rk.db.dao.AccountDAO;
import com.rk.db.dao.ExchangeRateDAO;
import com.rk.db.dao.OrderDAO;
import com.rk.messaging.ObjectMessageProducer;
import com.rk.resources.FixtureUtil;
import com.rk.resources.OrderResource;
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

import javax.jms.JMSException;
import javax.ws.rs.core.Response;
import java.math.BigDecimal;

public class OrderResourceTest extends ResourceTest {
    private static final int SENDER_ID = 1;
    private static final int RECEIVER_ID = 2;
    private static final int ORDER_ID = 1;
    private final static Logger LOGGER = LoggerFactory.getLogger(OrderResourceTest.class);
    private static OrderDAO orderDao = Mockito.mock(OrderDAO.class);
    private static ExchangeRateDAO exchangeRateDAO = Mockito.mock(ExchangeRateDAO.class);
    private static AccountDAO accountDAO = Mockito.mock(AccountDAO.class);
    private static ObjectMessageProducer objectMessageProducer = Mockito.mock(ObjectMessageProducer.class);
    private ResourceRequest<Order> request;
    private Order sampleOrder;


    @ClassRule
    public static final ResourceTestRule resources = ResourceTestRule.builder()
            .addResource(new OrderResource(orderDao, accountDAO, objectMessageProducer))
            .build();

    @Before
    public void resetMock() {
        Mockito.reset(orderDao, exchangeRateDAO, accountDAO, objectMessageProducer);
        request = createRequest(resources, OrderResource.class, Order.class);
        sampleOrder = OrderBuilder.anOrder()
                .withId(ORDER_ID)
                .withSenderAccount(SENDER_ID)
                .withReceiverAccount(RECEIVER_ID)
                .withType(OrderType.TRANSFER)
                .withOperationCurrencyCode(Currency.EUR)
                .withAmount(BigDecimal.valueOf(5))
                .build();
    }

    @Test
    public void postValidTransferOrder() throws JMSException {
        mockAccountExistence(SENDER_ID, RECEIVER_ID);
        mockOrderCreation();
        Order actualOrder = request.post(sampleOrder).getEntity();
        sampleOrder.setStatus(OrderStatus.NEW);
        Assert.assertEquals(sampleOrder, actualOrder);

        assertOrderProcessingStarted(sampleOrder);
    }

    @Test
    public void postValidTransferOrderWithNonExistentSender() throws JMSException {
        mockAccountExistence(null, RECEIVER_ID);
        request.post(sampleOrder, String.class, Response.Status.BAD_REQUEST.getStatusCode());
        assertOrderIsNotProcessed();
    }


    @Test
    public void postValidIncomeOrderWithNonExistentReceiver() throws JMSException {
        sampleOrder = OrderBuilder.anOrder().copyOf(sampleOrder)
                .withType(OrderType.INCOME)
                .withSenderAccount(null)
                .build();
        request.post(sampleOrder, String.class, Response.Status.BAD_REQUEST.getStatusCode());
        assertOrderIsNotProcessed();
    }

    @Test
    public void postValidOutcomeOrderWithNonExistingSender() throws JMSException {
        sampleOrder = OrderBuilder.anOrder().copyOf(sampleOrder)
                .withType(OrderType.OUTCOME)
                .withReceiverAccount(null)
                .build();
        request.post(sampleOrder, String.class, Response.Status.BAD_REQUEST.getStatusCode());
        assertOrderIsNotProcessed();
    }

    @Test
    public void postInvalidOrder() {
        final ResourceRequest<String> invalidRequest = createRequest(resources, OrderResource.class, String.class);
        FixtureUtil.jsonStringsFromFixtureDirectory("orders")
                .forEach(orderString -> {
                        LOGGER.debug("trying post order {}", orderString);
                        invalidRequest.post(orderString, String.class, Response.Status.BAD_REQUEST.getStatusCode(),
                                ResourceRequest.UNPROCESSABLE_ENTITY);
                });
    }

    private void mockOrderCreation() {
        Mockito.when(orderDao.create(Mockito.any())).thenReturn(ORDER_ID);
    }

    private void assertOrderIsNotProcessed() throws JMSException {
        Mockito.verify(objectMessageProducer, Mockito.never()).send(Mockito.any());
    }

    private void assertOrderProcessingStarted(Order expectedOrder) throws JMSException {
        Mockito.verify(objectMessageProducer).send(Mockito.eq(expectedOrder));
    }

    private void mockAccountExistence(Integer senderId, Integer receiverId) {
        if (senderId != null) {
            Account sender = AccountBuilder.anAccount().validSample();
            sender.setId(senderId);
            Mockito.when(accountDAO.findById(Mockito.eq(senderId))).thenReturn(sender);
        }
        if (receiverId != null) {
            Account receiver = AccountBuilder.anAccount().validSample();
            receiver.setId(receiverId);
            Mockito.when(accountDAO.findById(Mockito.eq(receiverId))).thenReturn(receiver);
        }
    }
}
