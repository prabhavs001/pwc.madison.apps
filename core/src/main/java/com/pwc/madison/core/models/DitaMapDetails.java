package com.pwc.madison.core.models;

import java.util.Date;

/**
 * POJO to hold the basic ditamap details
 */
public class DitaMapDetails {
    private String ditaMapName;
    private Long lastModifiedDate;
    private String ditaMapPath;

    public String getDitaMapName() {
        return ditaMapName;
    }

    public void setDitaMapName(String ditaMapName) {
        this.ditaMapName = ditaMapName;
    }

    public String getDitaMapPath() {
        return ditaMapPath;
    }

    public void setDitaMapPath(String ditaMapPath) {
        this.ditaMapPath = ditaMapPath;
    }

    public Long getLastModifiedDate() {
        return lastModifiedDate;
    }

    public void setLastModifiedDate(Long lastModifiedDate) {
        this.lastModifiedDate = lastModifiedDate;
    }
}
