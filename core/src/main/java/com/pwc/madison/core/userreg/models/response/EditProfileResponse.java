package com.pwc.madison.core.userreg.models.response;

import java.util.Arrays;

import com.pwc.madison.core.userreg.models.UserProfile;

public class EditProfileResponse {

    private Data data;
    private String[] errors;
    private String message;

    public Data getData() {
        return data;
    }

    public void setData(Data data) {
        this.data = data;
    }

    public String[] getErrors() {
        return errors;
    }

    public void setErrors(String[] errors) {
        this.errors = errors;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
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

    @Override
    public String toString() {
        return "EditProfileResponse [data=" + data + ", errors=" + Arrays.toString(errors) + ", message=" + message
                + "]";
    }

    public EditProfileResponse(Data data, String[] errors, String message) {
        super();
        this.data = data;
        this.errors = errors;
        this.message = message;
    }

}
