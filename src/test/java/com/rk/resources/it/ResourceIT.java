package com.rk.resources.it;

import com.rk.MoneyTransferApplicationStandalone;
import com.rk.configuration.MoneyTransferConfiguration;
import com.rk.resources.it.testutil.ResourceRequest;
import io.dropwizard.client.JerseyClientBuilder;
import io.dropwizard.testing.junit.DropwizardAppRule;
import org.junit.BeforeClass;
import org.junit.ClassRule;

import javax.ws.rs.Path;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.UriBuilder;
import java.net.URI;

abstract public class ResourceIT {
    @ClassRule
    public static final DropwizardAppRule<MoneyTransferConfiguration> RULE =
            new DropwizardAppRule<>(MoneyTransferApplicationStandalone.class);
    private static final String HOST_PATTERN = "http://127.0.0.1:%d/%s";
    private static Client client;

    @BeforeClass
    public static void initClass() {
        if (client == null)
            client = new JerseyClientBuilder(RULE.getEnvironment()).build("test client");
    }

    protected <T> ResourceRequest<T> createRequest(Class<?> resourceClass, Class<T> entityClass) {
        String basePath = resourceClass.getAnnotation(Path.class).value();
        URI uri = UriBuilder.fromUri(String.format(HOST_PATTERN, RULE.getLocalPort(), basePath)).build();
        return createRequest(uri, entityClass);
    }

    protected <T> ResourceRequest<T> createRequest(URI uri, Class<T> entityClass) {
        WebTarget webTarget = client.target(uri);
        return new ResourceRequest<>(webTarget, entityClass);
    }
}
