package com.rk.resources;

import com.rk.resources.it.testutil.ResourceRequest;
import io.dropwizard.testing.junit.ResourceTestRule;

import javax.ws.rs.Path;
import javax.ws.rs.client.WebTarget;

public abstract class ResourceTest {
    protected <T>ResourceRequest<T> createRequest(ResourceTestRule testRule, Class<?> resourceClass, Class<T> entityClass) {
        WebTarget target = testRule.target(resourceClass.getAnnotation(Path.class).value());
        return new ResourceRequest<>(target, entityClass);
    }
}
