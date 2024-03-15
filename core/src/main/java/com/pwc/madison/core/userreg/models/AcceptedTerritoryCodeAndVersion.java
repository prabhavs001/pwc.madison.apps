package com.pwc.madison.core.userreg.models;

/**
 * Accept TnC POJO class that provides getter and setters for the territory code and version.
 */
public class AcceptedTerritoryCodeAndVersion {

    private String territoryCode; 
    private String version;
    public String getTerritoryCode() {
        return territoryCode;
    }
    public void setTerritoryCode(String territoryCode) {
        this.territoryCode = territoryCode;
    }
    public String getVersion() {
        return version;
    }
    public void setVersion(String version) {
        this.version = version;
    }
    public AcceptedTerritoryCodeAndVersion(String territoryCode, String version) {
        super();
        this.territoryCode = territoryCode;
        this.version = version;
    }
    @Override
    public String toString() {
        return "AcceptedTerritoryCodeAndVersion [territoryCode=" + territoryCode + ", version=" + version + "]";
    }
    
}
