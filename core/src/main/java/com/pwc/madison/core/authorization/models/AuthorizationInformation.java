package com.pwc.madison.core.authorization.models;

/**
 * The model defines the authorization information for a user on accessing Viewpoint page. The information includes
 * whether user is authorized or not and whether user should be redirected to forbidden page or not and whether the page
 * requested was premium or not.
 */
public class AuthorizationInformation {

    private boolean isAuthorized;
    private boolean redirectToForbidden;

    private boolean isPremiumPageRequested;

    public boolean isAuthorized() {
        return isAuthorized;
    }

    public void setAuthorized(boolean isAuthorized) {
        this.isAuthorized = isAuthorized;
    }

    public boolean isRedirectToForbidden() {
        return redirectToForbidden;
    }

    public void setRedirectToForbidden(boolean redirectToForbidden) {
        this.redirectToForbidden = redirectToForbidden;
    }

    public AuthorizationInformation(boolean isAuthorized, boolean redirectToForbidden, boolean isPremiumPageRequested) {
        super();
        this.isAuthorized = isAuthorized;
        this.redirectToForbidden = redirectToForbidden;
        this.isPremiumPageRequested = isPremiumPageRequested;
    }

    public AuthorizationInformation() {
    }

    public boolean isPremiumPageRequested() {
        return isPremiumPageRequested;
    }

    public void setPremiumPageRequested(boolean isPremiumPageRequested) {
        this.isPremiumPageRequested = isPremiumPageRequested;
    }

}
