package com.pwc.madison.core.userreg.models.response;

import com.pwc.madison.core.userreg.models.UserProfile;

/**
 *  
 * Model to represent Get User response from UserReg Rest API.
 *  
 */
public class GetUserResponse {
    @Override
    public String toString() {
        return "GetUserResponse [data=" + data + "]";
    }

    private Data data;

    public GetUserResponse(Data data) {
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

        public Data(UserProfile userProfile) {
            super();
            this.userProfile = userProfile;
        }

        @Override
        public String toString() {
            return "Data [userProfile=" + userProfile + "]";
        }

        public UserProfile getUserProfile() {
            return userProfile;
        }

        public void setUserProfile(UserProfile userProfile) {
            this.userProfile = userProfile;
        }

        private UserProfile userProfile;

    }
}
