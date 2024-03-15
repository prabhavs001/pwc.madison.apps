package com.pwc.madison.core.services;

/**
 * The interface return cookie consent i.e. whether to enable cookie or not.
 */
public interface CookieConsentService {

    /**
     * @return cookie consent.
     */
    String getCookieConsent();
}
