package com.rk.messaging;

import org.apache.activemq.pool.PooledConnectionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jms.ConnectionFactory;

/**
 * Managed JMS connection factory implementation with connection pool under the hood.
 * It is not persistent (!) due to usage of the embedded message broker.
 * The class is ActiveMQ implementation-aware due to absence
 */
public class ManagedActiveMQPoolingConnectionFactoryContainer implements ManagedMessageConnectionFactoryContainer {
    private final Logger logger = LoggerFactory.getLogger(getClass().getName());
    private final PooledConnectionFactory connectionFactory;

    public ManagedActiveMQPoolingConnectionFactoryContainer(PooledConnectionFactory connectionFactory) {
        this.connectionFactory = connectionFactory;
    }

    @Override
    public ConnectionFactory getConnectionFactory() {
        return connectionFactory;
    }

    @Override
    public void start() {
        logger.debug("Starting pooling connection factory");
        connectionFactory.initConnectionsPool();
        connectionFactory.start();
    }

    @Override
    public void stop() {
        logger.debug("Stopping pooling connection factory");
        connectionFactory.stop();
    }
}
