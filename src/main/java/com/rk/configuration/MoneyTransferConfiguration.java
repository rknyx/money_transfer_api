package com.rk.configuration;

import com.google.common.collect.ImmutableMap;
import io.dropwizard.Configuration;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.dropwizard.db.DataSourceFactory;
import io.dropwizard.util.Duration;

import javax.validation.Valid;

/**
 * Configuration class which holds application parameters and encapsulates implementation classes
 * (with several exclusions, e.x. embedded db).
 */
public class MoneyTransferConfiguration extends Configuration {
    @Valid
    private DataSourceFactory database = constructDefaultDataSourceFactory();

    @Valid
    private ConnectionFactoryConfiguration connectionFactoryConfiguration = new ConnectionFactoryConfiguration();

    @JsonProperty("ConnectionFactory")
    public void setConnectionFactoryConfiguration(ConnectionFactoryConfiguration connectionFactoryConfiguration) {
        this.connectionFactoryConfiguration = connectionFactoryConfiguration;
    }

    @JsonProperty("ConnectionFactory")
    public ConnectionFactoryConfiguration getConnectionFactoryConfiguration() {
        return connectionFactoryConfiguration;
    }

    @JsonProperty("database")
    public void setDataSourceFactory(DataSourceFactory factory) {
        this.database = factory;
    }

    @JsonProperty("database")
    public DataSourceFactory getDataSourceFactory() {
        return database;
    }

    private DataSourceFactory constructDefaultDataSourceFactory() {
        DataSourceFactory dataSourceFactory = new DataSourceFactory();
        dataSourceFactory.setDriverClass("org.hsqldb.jdbc.JDBCDriver");
        dataSourceFactory.setUser("root");
        dataSourceFactory.setPassword("password");
        dataSourceFactory.setUrl("jdbc:hsqldb:hsql://127.0.0.1:9001/money_transfer");

        dataSourceFactory.setProperties(ImmutableMap.of(
                "charSet", "UTF-8", "hibernate.dialect",
                "org.hibernate.dialect.HSQLDialect"));

        dataSourceFactory.setMaxWaitForConnection(Duration.seconds(1));
        dataSourceFactory.setValidationQuery("select 1 from INFORMATION_SCHEMA.SYSTEM_USERS");
        dataSourceFactory.setValidationQueryTimeout(Duration.seconds(1));
        dataSourceFactory.setInitialSize(2);
        dataSourceFactory.setMinSize(2);
        dataSourceFactory.setCheckConnectionWhileIdle(false);
        dataSourceFactory.setEvictionInterval(Duration.seconds(10));
        dataSourceFactory.setMinIdleTime(Duration.seconds(60));
        return dataSourceFactory;
    }
}
