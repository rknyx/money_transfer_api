package com.rk.resources.it.testutil;

import javax.ws.rs.core.Response;

public class ResourceResponse<T> {
    private final T entity;
    private final Response response;

    public ResourceResponse(T entity, Response response) {
        this.entity = entity;
        this.response = response;
    }

    public T getEntity() {
        return entity;
    }

    public Response getResponse() {
        return response;
    }
}
