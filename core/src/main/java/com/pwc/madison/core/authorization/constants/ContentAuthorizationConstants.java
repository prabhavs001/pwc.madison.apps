package com.pwc.madison.core.authorization.constants;

public final class ContentAuthorizationConstants {

    private ContentAuthorizationConstants() {
    }

    public static final String PAGE_PROPERTY_DISABLE_TNC = "disableTNC";
    public static final String PAGE_PROPERTY_AUDIENCE_TYPE = "pwc-audience";
    public static final String PAGE_PROPERTY_ACCESS_LEVEL = "pwc-access";
    public static final String PAGE_PROPERTY_PRIVATE_GROUP = "pwc-privateGroup";
    public static final String PAGE_PROPERTY_LICENSE = "pwc-license";
    public static final String MADISON_DAM_PAGE_FILTER_HIERARCHY = "/content(/dam)?/pwc-madison(/ditaroot)?/(\\w{2})/(\\w{2})/.*";
    public static final String CONTENT_AUTHORIZATION_URI_QUERY_PARAMETER = "uri";
    public static final String AUTHORIZATION_HEADER_PREFIX = "Basic";
    public static final String CONTENT_AUTHORIZATION_REFERER_HEADER_QUERY_PARAMETER = "refererHeader";

    public static final String CONTENT_AUTHORIZATION_IS_PREMIUM_QUERY_PARAMETER = "isPremium";
    public static final String ASSET_METADATA_NODE_REL_PATH = "jcr:content/metadata";
    public static final String ASSET_RENDITIONS_NODE_REL_PATH = "jcr:content/renditions";
    public static final String CONTENT_AUTHORIZATION_IS_INTERNAL_ONLY_QUERY_PARAMETER = "isInternalOnly";
    public static final String CONTENT_AUTHORIZATION_ACCESS_TYPE_QUERY_PARAMETER = "accessType";
}
