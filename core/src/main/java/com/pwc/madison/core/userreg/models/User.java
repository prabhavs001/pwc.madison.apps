package com.pwc.madison.core.userreg.models;

import com.google.gson.Gson;

/**
 * User POJO class that provides getter and setters for various properties of the user.
 */
public class User {
    
    private boolean isUserLoggedIn;
    private UserProfile userProfile;
    
    /**
     * Instantiates a new user with the given properties.
     *
     * @param isUserLoggedIn true, if the user is logged in
     * @param userProfile the user profile
     */
    public User(boolean isUserLoggedIn, UserProfile userProfile) {
        super();
        this.isUserLoggedIn = isUserLoggedIn;
        this.userProfile = userProfile;
    }
    
    /**
     * Checks if the user is logged in.
     *
     * @return true, if the user is logged in
     */
    public boolean isUserLoggedIn() {
        return isUserLoggedIn;
    }
    
    /**
     * Sets if the user is logged in.
     *
     * @param isUserLoggedIn true, if the user is logged in
     */
    public void setUserLoggedIn(boolean isUserLoggedIn) {
        this.isUserLoggedIn = isUserLoggedIn;
    }
    
    /**
     * Gets the user profile.
     *
     * @return the user profile
     */
    public UserProfile getUserProfile() {
        return userProfile;
    }
    
    /**
     * Sets the user profile.
     *
     * @param userProfile the new user profile
     */
    public void setUserProfile(UserProfile userProfile) {
        this.userProfile = userProfile;
    }
    
    @Override
    public String toString() {
        return new Gson().toJson(this);
    }
    
}
