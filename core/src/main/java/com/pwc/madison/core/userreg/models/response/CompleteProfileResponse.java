package com.pwc.madison.core.userreg.models.response;

public class CompleteProfileResponse {
    
    private String data;

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }

    public CompleteProfileResponse(String data) {
        super();
        this.data = data;
    }
}
