package com.pwc.madison.core.services;

public interface MadisonSearchConfigurationService {
    
    /**
     * 
     * @param locale
     * @return snp id for provided locale
     */
    public String getSnPId(String territory);

}
