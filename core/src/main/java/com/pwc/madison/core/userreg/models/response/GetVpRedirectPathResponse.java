package com.pwc.madison.core.userreg.models.response;

/**
 *  
 * Model to represent Get new redirect path response from VP Redirection Rest API.
 *  
 */
public class GetVpRedirectPathResponse {

    String redirectionPath;

    public String getRedirectionPath() {
        return redirectionPath;
    }

    public void setRedirectionPath(String redirectionPath) {
        this.redirectionPath = redirectionPath;
    }

    public GetVpRedirectPathResponse(String redirectionPath) {
        super();
        this.redirectionPath = redirectionPath;
    }
    
}
