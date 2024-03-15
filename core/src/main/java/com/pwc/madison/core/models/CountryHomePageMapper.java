package com.pwc.madison.core.models;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class CountryHomePageMapper {

    @SerializedName("country")
    @Expose
    private String country;
    @SerializedName("homePagePath")
    @Expose
    private String homePagePath;

    public String getCounrty() {
        return country;
    }

    public void setCountry(final String country) {
        this.country = country;
    }

    public String getHomePagePath() {
        return homePagePath;
    }

    public void setHomePagePath(final String homePagePath) {
        this.homePagePath = homePagePath;
    }

}
