package com.rk;

import com.rk.api.Account;
import com.rk.api.ExchangeRate;
import com.rk.api.Order;
import com.rk.configuration.ConnectionFactoryConfiguration;
import com.rk.messaging.ManagedMessageConnectionFactoryContainer;
import com.rk.configuration.MoneyTransferConfiguration;
import com.rk.core.OrderProcessingWorker;
import com.rk.db.InMemoryDatabaseManager;
import com.rk.db.dao.AccountDAO;
import com.rk.db.dao.ExchangeRateDAO;

import com.rk.db.dao.OrderDAO;
import com.rk.messaging.ObjectMessageProducer;
import com.rk.messaging.SimpleMessageListenerContainer;
import com.rk.resources.AccountResource;
import com.rk.resources.ExchangeRateResource;
import com.rk.resources.OrderResource;
import io.dropwizard.Application;
import io.dropwizard.db.DataSourceFactory;
import io.dropwizard.db.PooledDataSourceFactory;
import io.dropwizard.hibernate.HibernateBundle;
import io.dropwizard.hibernate.UnitOfWorkAwareProxyFactory;
import io.dropwizard.lifecycle.JettyManaged;
import io.dropwizard.lifecycle.Managed;
import io.dropwizard.migrations.MigrationsBundle;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;

import javax.sql.DataSource;

import java.util.function.Consumer;

public class MoneyTransferApplicationStandalone extends Application<MoneyTransferConfiguration> {
    private static final String DEFAULT_QUEUE_NAME = "orders";
    private static final String DATASOURCE_NAME = "money_transfer_datasource";

    private final HibernateBundle<MoneyTransferConfiguration> hibernate = new HibernateBundle<MoneyTransferConfiguration>(
            Account.class, ExchangeRate.class, Order.class) {
        @Override
        public DataSourceFactory getDataSourceFactory(MoneyTransferConfiguration configuration) {
            return configuration.getDataSourceFactory();
        }
    };

    private final MigrationsBundle<MoneyTransferConfiguration> migrationsBundle = new MigrationsBundle<MoneyTransferConfiguration>() {
        @Override
        public PooledDataSourceFactory getDataSourceFactory(MoneyTransferConfiguration configuration) {
            return configuration.getDataSourceFactory();
        }
    };

    public static void main(final String[] args) throws Exception {
        new MoneyTransferApplicationStandalone().run(args);
    }

    @Override
    public String getName() {
        return "money_transfer_standalone";
    }

    @Override
    public void initialize(final Bootstrap<MoneyTransferConfiguration> bootstrap) {
        bootstrap.addBundle(hibernate);
        bootstrap.addBundle(migrationsBundle);
    }

    @Override
    public void run(final MoneyTransferConfiguration configuration, final Environment environment)  {
        //database configuration
        final AccountDAO accountDAO = new AccountDAO(hibernate.getSessionFactory());
        final OrderDAO orderDAO = new OrderDAO(hibernate.getSessionFactory());
        final ExchangeRateDAO exchangeRateDAO = new ExchangeRateDAO(hibernate.getSessionFactory());
        final DataSource dataSource = configuration.getDataSourceFactory().build(environment.metrics(), DATASOURCE_NAME);

        //messaging configuration
        final ConnectionFactoryConfiguration connectionFactoryConfiguration = configuration
                .getConnectionFactoryConfiguration();

        final ManagedMessageConnectionFactoryContainer managedMessageConnectionFactoryContainer = connectionFactoryConfiguration
                .getManagedMessageConnectionFactoryContainer();

        final ObjectMessageProducer messageProducer = new ObjectMessageProducer(DEFAULT_QUEUE_NAME,
                managedMessageConnectionFactoryContainer.getConnectionFactory());

        final Consumer<Order> orderProcessingWorker = new UnitOfWorkAwareProxyFactory(hibernate).create(
                OrderProcessingWorker.class,
                new Class[] {OrderDAO.class, AccountDAO.class, ExchangeRateDAO.class},
                new Object[] {orderDAO, accountDAO, exchangeRateDAO});

        final Managed messageListeningContainer = new SimpleMessageListenerContainer(
                connectionFactoryConfiguration.getConnectionFactory(),
                connectionFactoryConfiguration.getOrdersQueue(),
                Runtime.getRuntime().availableProcessors(),
                SimpleMessageListenerContainer.listenerFor(orderProcessingWorker));

        //resources registration
        environment.jersey().register(new AccountResource(accountDAO));
        environment.jersey().register(new ExchangeRateResource(exchangeRateDAO));
        environment.jersey().register(new OrderResource(orderDAO, accountDAO, messageProducer));

        //managed objects configuration
        environment.lifecycle().getManagedObjects().add(0, new JettyManaged(new InMemoryDatabaseManager(dataSource)));
        environment.lifecycle().manage(managedMessageConnectionFactoryContainer);
        environment.lifecycle().manage(messageListeningContainer);
    }
}
