package com.rk.messaging;

import io.dropwizard.lifecycle.Managed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.concurrent.NotThreadSafe;
import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageListener;
import javax.jms.ObjectMessage;
import javax.jms.Session;
import java.io.Serializable;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;

/**
 * Minimalistic message listener container
 * <p>Provides multithreaded messages consumption.
 * Operates with pool of sessions for a single connection, as ActiveMQ documentation recommends to do firstly.
 * Connection-based pooling is omitted for the sake of brevity, but is possible by creating several
 * SimpleMessageListenerContainer instances with few amount of listeners per connection.</p>
 *
 * <p>Creates fixed-size non-adjustable pool of sessions. Adjustable polling-based pool is more efficient but such
 * implementation is about ten times larger than fixed-size listenter-based container.</p>
 *
 * <p>Be aware that listeners are executed in message broker client's thread, so their execution blocks one
 * client's thread. There are no explicit set of pre-fetching or dispatching policy. Experiments on ActiveMQ
 * implementation showed that the implementation is quite fast and most messages are dispatched to the first session
 * for moderate load. As soon as database start freezing dispatching will involve all threads.</p>
 */
@NotThreadSafe
public class SimpleMessageListenerContainer implements Managed {
    private static final Logger logger = LoggerFactory.getLogger(SimpleMessageListenerContainer.class);

    private final int consumersCount;
    private final ConnectionFactory connectionFactory;
    private final Destination destination;
    private final MessageListener messageListener;

    private Set<MessageConsumer> consumers;
    private Set<Session> sessions;
    private Connection connection;
    private boolean isStarted;

    /**
     * Wrapper class which intended to make actual message consumer simple and encapsulate jms specific.
     * @param <T>
     */
    public static class ObjectMessageListener<T extends Serializable> implements MessageListener {
        private final Consumer<T> consumer;

        private ObjectMessageListener(Consumer<T> consumer) {
            Objects.requireNonNull(consumer);
            this.consumer = consumer;
        }

        public void onMessage(Message message) {
            try {
                ObjectMessage objectMessage = (ObjectMessage) message;
                @SuppressWarnings("unchecked")
                T obj = (T) objectMessage.getObject();
                consumer.accept(obj);
            } catch (JMSException e) {
                logger.debug("Unexpected JMS exception", e);
                throw new RuntimeException(e);
            }
        }
    }

    public SimpleMessageListenerContainer(ConnectionFactory connectionFactory,
                                          Destination destination,
                                          int consumersCount,
                                         MessageListener messageListener) {
        Objects.requireNonNull(connectionFactory);
        Objects.requireNonNull(destination);
        Objects.requireNonNull(messageListener);

        this.consumersCount = consumersCount;
        this.connectionFactory = connectionFactory;
        this.destination = destination;
        this.messageListener = messageListener;
    }

    /**
     * Method for obtaining a listener instance for specific consumer.
     * @param consumer
     * @param <T>
     * @return
     */
    public static <T extends Serializable> ObjectMessageListener<T> listenerFor(Consumer<T> consumer) {
        return new ObjectMessageListener<>(consumer);
    }

    /**
     * Creates connection, set of sessions and listeners, starts message listening.
     * @throws JMSException
     */
    @Override
    public void start() throws JMSException {
        logger.debug("Starting message listening container with {} consumes", consumersCount);
        if (consumers == null) {
            sessions = new HashSet<>(consumersCount);
            consumers = new HashSet<>(consumersCount);
            connection = connectionFactory.createConnection();
            connection.start();

            for (int i = 0; i < consumersCount; i++) {
                Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
                MessageConsumer consumer = session.createConsumer(destination);
                consumer.setMessageListener(messageListener);

                sessions.add(session);
                consumers.add(consumer);
            }
        }
        isStarted = true;
    }

    /**
     * Closes related connection, consumers and sessions.
     * @throws JMSException
     */
    @Override
    public void stop() throws JMSException {
        if (!isStarted) {
            return;
        }
        logger.debug("Shutting down message listening container");
        for (MessageConsumer consumer : consumers) {
            consumer.close();
        }
        for (Session session : sessions) {
            session.close();
        }
        connection.close();
    }
}
