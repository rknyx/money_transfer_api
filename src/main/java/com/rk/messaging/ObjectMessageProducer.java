package com.rk.messaging;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.JMSException;
import javax.jms.MessageProducer;
import javax.jms.Session;
import java.io.Serializable;
import java.util.Objects;

/**
 * Queue object message producer.
 * Open connection for specified queue for each {@link #send(Serializable)} call, so is recommended to use with pooled
 * session factory.
 */
public class ObjectMessageProducer {
    private final String queueName;
    private final ConnectionFactory connectionFactory;

    public ObjectMessageProducer(String queueName, ConnectionFactory connectionFactory) {
        Objects.requireNonNull(queueName);
        Objects.requireNonNull(connectionFactory);
        this.queueName = queueName;
        this.connectionFactory = connectionFactory;
    }

    public <T extends Serializable> void send(T obj) throws JMSException {
        Objects.requireNonNull(obj);
        Connection connection = null;
        Session session = null;

        try {
            connection = connectionFactory.createConnection();
            connection.start();
            session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
            MessageProducer messageProducer = session.createProducer(session.createQueue(queueName));
            messageProducer.send(session.createObjectMessage(obj));
        } finally {
            if (session != null) session.close();
            if (connection != null) connection.close();
        }
    }
}
