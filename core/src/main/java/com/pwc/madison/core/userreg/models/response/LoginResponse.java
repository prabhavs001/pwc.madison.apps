package com.pwc.madison.core.userreg.models.response;

/**
 * 
 * Model to represent the response data from UserReg rest login API.
 *
 */
public class LoginResponse extends UserRegResponse {

    private Data data;

    public LoginResponse(Data data) {
        super();
        this.data = data;
    }

    public Data getData() {
        return data;
    }

    public void setData(Data data) {
        this.data = data;
    }

    public class Data {

        public Data(String token) {
            super();
            this.token = token;
        }

        private String token;

        public String getToken() {
            return token;
        }

        public void setToken(String token) {
            this.token = token;
        }

    }

}
