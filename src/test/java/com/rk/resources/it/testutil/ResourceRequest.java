package com.rk.resources.it.testutil;

import org.apache.commons.lang3.ArrayUtils;

import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;

public class ResourceRequest<T> {
    public static final int UNPROCESSABLE_ENTITY = 422;
    private static final String FAILED_MSG = "Response status code expected to be '%s', but actual code is '%s'";

    private final WebTarget webTarget;
    private final Class<T> requestEntityClass;

    public ResourceRequest(WebTarget webTarget, Class<T> requestEntity) {
        this.webTarget = webTarget;
        this.requestEntityClass = requestEntity;
    }

    public ResourceRequest<T> path(String path) {
        return new ResourceRequest<>(webTarget.path(path), requestEntityClass);
    }

    public ResourceResponse<T> post(T entityObject) {
        Response response = webTarget.request().post(Entity.json(entityObject));
        return getEntityResponse(response, requestEntityClass, Response.Status.CREATED.getStatusCode());
    }

    public <K> ResourceResponse<K> post(T entityObject, Class<K> responseType, int... expectedStatusCode) {
        Response response = webTarget.request().post(Entity.json(entityObject));
        return getEntityResponse(response, responseType, expectedStatusCode);
    }

    public ResourceResponse<T> put(T entityObject) {
        Response response = webTarget.request().put(Entity.json(entityObject));
        return getEntityResponse(response, requestEntityClass, Response.Status.OK.getStatusCode(),
                Response.Status.CREATED.getStatusCode());
    }

    public <K> ResourceResponse<K> put(T entityObject, Class<K> responseType, int... expectedStatusCode) {
        Response response = webTarget.request().put(Entity.json(entityObject));
        return getEntityResponse(response, responseType, expectedStatusCode);
    }

    public ResourceResponse<T> get() {
        Response response = webTarget.request().get();
        return getEntityResponse(response, requestEntityClass,
                Response.Status.OK.getStatusCode(), Response.Status.CREATED.getStatusCode());
    }

    public <K> ResourceResponse<K> get(Class<K> responseType, int expectedCode) {
        Response response = webTarget.request().get();
        return getEntityResponse(response, responseType, expectedCode);
    }

    private <K> ResourceResponse<K> getEntityResponse(Response response, Class<K> responseType, int... expectedStatusCode) {
        if (ArrayUtils.contains(expectedStatusCode, response.getStatus()))
            return new ResourceResponse<>(response.readEntity(responseType), response);

        String msg = String.format(FAILED_MSG, ArrayUtils.toString(expectedStatusCode), response.getStatus());
        throw new AssertionError(msg + "\n" + response.readEntity(String.class));
    }
}
