package com.rk.configuration;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.rk.messaging.ManagedActiveMQPoolingConnectionFactoryContainer;
import com.rk.messaging.ManagedMessageConnectionFactoryContainer;
import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.command.ActiveMQQueue;
import org.apache.activemq.pool.PooledConnectionFactory;
import org.hibernate.validator.constraints.NotEmpty;

import javax.jms.ConnectionFactory;
import javax.jms.Queue;
import javax.validation.constraints.Min;

public class ConnectionFactoryConfiguration {
    @NotEmpty
    private String brokerUrl = "vm://localhost?broker.persistent=false";
    @Min(1)
    private int maxConnections = Runtime.getRuntime().availableProcessors();

    private String ordersQueueName = "orders";

    public String getBrokerUrl() {
        return brokerUrl;
    }

    public void setBrokerUrl(String brokerUrl) {
        this.brokerUrl = brokerUrl;
    }

    public int getMaxConnections() {
        return maxConnections;
    }

    public void setMaxConnections(int maxConnections) {
        this.maxConnections = maxConnections;
    }

    public String getOrdersQueueName() {
        return ordersQueueName;
    }

    public void setOrdersQueueName(String ordersQueueName) {
        this.ordersQueueName = ordersQueueName;
    }

    @JsonIgnore
    public Queue getOrdersQueue() {
        return new ActiveMQQueue(getOrdersQueueName());
    }

    /**
     * Get managed connection factory. It is implied that the factory creates somehow pooled connections which are
     * safe to reuse. The instance is managed, so it
     * @return instance of ManagedMessageConnectionFactoryContainer
     */
    @JsonIgnore
    public ManagedMessageConnectionFactoryContainer getManagedMessageConnectionFactoryContainer() {
        final PooledConnectionFactory pooledConnectionFactory = new PooledConnectionFactory(getActiveMQConnectionFactory());
        pooledConnectionFactory.setMaxConnections(maxConnections);

        return new ManagedActiveMQPoolingConnectionFactoryContainer(pooledConnectionFactory);
    }

    /**
     * Get connection factory.
     * @return ConnectionFactory
     */
    @JsonIgnore
    public ConnectionFactory getConnectionFactory() {
        return getActiveMQConnectionFactory();
    }

    private ActiveMQConnectionFactory getActiveMQConnectionFactory() {
        final ActiveMQConnectionFactory activeMQConnectionFactory = new ActiveMQConnectionFactory(brokerUrl);
        activeMQConnectionFactory.setTrustAllPackages(true);
        return activeMQConnectionFactory;
    }
}
