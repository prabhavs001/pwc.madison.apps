package com.pwc.madison.core.userreg.models.request;

import java.util.List;

/**
 * Model to represent data to send to VP Redirections Rest add redirects request API.
 *
 */
public class AddRedirectionsRequest extends UserRegRequest {

    List<ViewpointRedirection> redirects;

    public List<ViewpointRedirection> getRedirects() {
        return redirects;
    }

    public void setRedirects(List<ViewpointRedirection> redirects) {
        this.redirects = redirects;
    }

    public AddRedirectionsRequest(List<ViewpointRedirection> redirects) {
        super();
        this.redirects = redirects;
    }

    @Override
    public String toString() {
        return "AddRedirectionsRequest [redirects=" + redirects + "]";
    }

}
