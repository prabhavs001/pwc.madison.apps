package com.pwc.madison.core.userreg.models.response;

import com.pwc.madison.core.userreg.models.ContentAccessInfo;

/**
 * Content access info POJO class that provides getter and setters for the licenses and private groups of the user.
 */
public class ContentAccessInfoResponse {

    @Override
    public String toString() {
        return "ContentAccessInfoResponse [data=" + data + "]";
    }

    public ContentAccessInfoResponse(Data data) {
        super();
        this.data = data;
    }

    private Data data;

    public Data getData() {
        return data;
    }

    public void setData(Data data) {
        this.data = data;
    }

    public class Data {

        private ContentAccessInfo contentAccessInfo;

        public ContentAccessInfo getContentAccessInfo() {
            return contentAccessInfo;
        }

        public void setContentAccessInfo(ContentAccessInfo contentAccessInfo) {
            this.contentAccessInfo = contentAccessInfo;
        }

        @Override
        public String toString() {
            return "Data [contentAccessInfo=" + contentAccessInfo + "]";
        }

        public Data(ContentAccessInfo contentAccessInfo) {
            super();
            this.contentAccessInfo = contentAccessInfo;
        }

    }

}
