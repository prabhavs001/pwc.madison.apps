package com.pwc.madison.core.models;


import java.util.ArrayList;

/**
 * Expiration List Model. Fetches all expiring/expired content
 */
public interface ExpirationListModel {

    /**
     * returns List of Expired and not Approved content
     * @return expiredNotApprovedList
     */
    ArrayList<ExpiryContent> getExpiredNotApprovedList();

    /**
     * returns List of Expired and Approved content
     * @return expiredApprovedList
     */
    ArrayList<ExpiryContent> getExpiredApprovedList();

    /**
     * returns List of Expiring content
     * @return expiringList
     */
    ArrayList<ExpiryContent> getExpiringList();

    /**
     * returns Current User Role
     * @return currentUserRole
     */
    String getCurrentUserRole();

    /**
     * returns logged-in userID
     * @return loggedInUserId
     */
    String getLoggedInUserId();



}
