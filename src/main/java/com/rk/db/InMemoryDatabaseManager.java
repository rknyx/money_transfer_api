package com.rk.db;

import io.dropwizard.lifecycle.Managed;
import liquibase.Contexts;
import liquibase.LabelExpression;
import liquibase.Liquibase;
import liquibase.database.Database;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.exception.LiquibaseException;
import liquibase.resource.ClassLoaderResourceAccessor;
import org.hsqldb.Server;
import org.hsqldb.persist.HsqlProperties;
import org.hsqldb.server.ServerAcl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.io.IOException;
import java.sql.SQLException;

/**
 * Class which manages embedded database.
 *
 * <p>The instance is in charge of starting and stopping embedded sql db server in accordance with application
 * lifecycle. During start phase it creates database schema to avoid explicit liquibase update call. Database start is
 * not an immediate procedure and is not synchronized with attempts of database layer to connect, so during application
 * startup "Connection refused" exceptions are possible which do not affect the application itself. This effect should
 * take place only for standalone "fat-jar" application. In case of separate persistence db which will be started before
 * readers and writers there should be no "Connection refused" exceptions.</p>
 *
 * <p>The idea of embedded sql db is that it is possible to switch to a persistence storage easily. In current
 * application it can be done via setting custom datasource in config file.</p>
 *
 * <p>Contains hardcoded settings since there is no much sense to override them for embedded db.</p>
 */
public class InMemoryDatabaseManager implements Managed {
    private static final String MIGRATIONS_FILE = "migrations.xml";
    private static final Logger logger = LoggerFactory.getLogger(InMemoryDatabaseManager.class);
    private final Server databaseServer;
    private final DataSource dataSource;

    public InMemoryDatabaseManager(DataSource dataSource) {
        this.dataSource = dataSource;
        HsqlProperties hsqlProperties = new HsqlProperties();
        hsqlProperties.setProperty("server.database.0", "mem:temp;user=root;password=password");
        hsqlProperties.setProperty("server.dbname.0", "money_transfer");
        hsqlProperties.setProperty("server.silent", "false");
        hsqlProperties.setProperty("server.trace", "false");
        hsqlProperties.setProperty("server.address", "127.0.0.1");

        databaseServer = new Server();
        try {
            databaseServer.setProperties(hsqlProperties);
        } catch (IOException | ServerAcl.AclFormatException e) {
            logger.error("Unable to set database properties", e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public void start() {
        logger.info("Starting database");
        try {
            databaseServer.start();
        } catch (Exception e) {
            logger.error("Unable to start database", e);
            throw e;
        }
        logger.info("Database started");

        logger.info("Start database schema initialization");
        try {
            Database database = DatabaseFactory.getInstance().findCorrectDatabaseImplementation(new JdbcConnection(dataSource.getConnection()));
            Liquibase liquibase = new Liquibase(MIGRATIONS_FILE, new ClassLoaderResourceAccessor(), database);
            liquibase.update(new Contexts(), new LabelExpression());
        } catch (SQLException e) {
            logger.error("Cannot obtain database connection for init or update database schema");
        } catch (LiquibaseException e) {
            logger.error("Cannot init or update database schema.", e);
            return;
        }
        logger.info("Database schema initialized");
    }

    @Override
    public void stop() {
        logger.info("Stopping database");
        databaseServer.stop();
    }


}
