package com.pwc.madison.core.userreg;

/**
 * 
 * Class that defines Constants that are used for UserReg functionality.
 * 
 */
public final class Constants {

	private Constants() {
		throw new IllegalStateException("Constants class");
	}
	
    public static final String UTF_8_ENCODING = "UTF-8";
    public static final String AUTH_COOKIE_NAME = "vp_i"; // previously called viewpoint-id
    public static final String AUTH_CLIENT_COOKIE_NAME = "vp_ic"; // created to be used on client side
    public static final String LOGIN_COMPLETE_COOKIE_NAME = "vp_lc";
    public static final String COMPLETE_PROFILE_COOKIE_NAME = "vp_cp";
    public static final String CONTENT_TYPE_JSON = "application/json";
    public static final String MADISON_EXTERNAL_USERS_IMMEDIATE_PATH = "/home/users/pwc-external-users/";
    public static final String MADISON_EXTERNAL_USERS_GROUP = "pwc-external-users";
    public static final String USER_PROFILE_RELATIVE_PATH = "./profile/";
    public static final String USER_FIRST_NAME_PROPERTY = "firstName";
    public static final String USER_LAST_NAME_PROPERTY = "lastName";
    public static final String USER_SAMACCOUNT_NUMBER_PROPERTY = "samaccountNo";
    public static final String USER_COUNTRY_PROPERTY = "country";
    public static final String USER_EMAIL_PROPERTY = "email";
    public static final String USER_MADISON_ID_PROPERTY = "madisonID";
    public static final String USERREG_ADMIN_SUB_SERVICE = "madison-userreg-admin";
    public static final String USER_PROFILE_IMAGE_PROPERTY = "image";
    public static final String USER_SUBSCRIBE_WEEKLY_NEWSLETTER_PROPERTY = "subscribeToWeeklyNewsLetter";
    public static final String USER_SHOW_MULTIMEDIA_SUBTITLE_PROPERTY = "showMultimediaSubtitle";
    public static final String USER_PREFERENCE_VIEW_PROPERTY = "preferencesView";
    public static final String USER_COMPANY_PROPERTY = "company";
    public static final String USER_JOB_TITLE_PROPERTY = "jobTitle";
    public static final String IMAGE_SELECTOR_COOKIE_NAME = "madison-image-selector";
    public static final String USER_RELATIVE_EMAIL_PROPERTY = "profile/email";
    public static final String MADISON_USER_PROFILE_COOKIE_NAME = "vp_p"; // previously called viewpoint-profile
    public static final String MADISON_USER_PROFILE_CLIENT_COOKIE_NAME = "vp_pc"; // created to be used on client side
    public static final String MADISON_USER_REDIRECT_PATH_COOKIE = "madison-redirection";
    public static final String USER_AUTHENTICATION_TOKEN_QUERY_PARAMETER_NAME = "auth";
    public static final String EXTERNAL_USER_AUTHENTICATION_TOKEN_QUERY_PARAMETER_NAME = "extauth";
    public static final String REDIRECT_URL_QUERY_PARAMETER_NAME = "redirectUrl";
    public static final String USER_LANGUAGE_PROPERTY = "defaultLanguage";
    public static final String USER_TERRITORY_PROPERTY = "territory";
    public static final String USER_PRIMARY_TERRITORY_PROPERTY = "primaryTerritory";
    public static final String USER_PRIMARY_LANGUAGE_PROPERTY = "primaryLanguage";
    public static final String USER_PREFERRED_TERRITORIES_PROPERTY = "preferredTerritories";
    public static final String USER_PREFERRED_LANGUAGES_PROPERTY = "preferredLanguages";
    public static final String USER_PREFERRED_GAAP_PROPERTY = "preferredGaap";
    public static final String USER_PREFERRED_GAAS_PROPERTY = "preferredGaas";
    public static final String USER_PROFILE_NODE = "/profile";
    public static final String MADISON_INTERNAL_USERS_IMMEDIATE_PATH = "/home/users/pwc-internal-users/";
    public static final String MADISON_INTERNAL_USERS_GROUP = "pwc-internal-users";
    public static final String USER_PREFERENCES_NODE = "preferences";
    public static final String USER_PRIVACY_POLICY = "privacypolicy";
    public static final String USER_TNC_VERSION_ACCEPTED = "versionAccepted";
    public static final String USER_PREFERRED_TOPIC_PROPERTY = "preferredTopic";
    public static final String USER_PREFERRED_INDUSTRY_PROPERTY = "preferredIndustry";
    public static final String MADISON_USER_PROFILE_USERINFO_COOKIE_NAME = "viewpoint-profile-userinfo";
    public static final String MADISON_USER_PROFILE_USERINFO_COOKIE_NAME_PREFIX = "viewpoint-profile-userinfo-";
    public static final String MADISON_USER_PROFILE_USERINFO_COOKIE_SEPERATOR = ",";
    public static final String MADISON_PREFERENCES_BASE_PATH = "/content/pwc-madison/global/user-preferences";
    public static final String FIRM_WIDE_CODE = "fw";
    public static final String HTTPS = "HTTPS";
    public static final String GET_PROFILE_TOKEN_QUERY_PARAM = "getProfileToken";
    public static final String ACCESS_DENIED_QUERY_PARAMETER_NAME = "accessdenied";
    public static final String FAVORITE_LIST_DEFAULT_IDENTIFIER = "DEFAULT";
    public static final String MADISON_USER_CONTENT_ACCESS_INFO_COOKIE_NAME = "vp_cai";
    public static final String MADISON_USER_LICENSE_INFO_COOKIE_NAME = "vp_li";
    public static final String USER_LICENSES_PATH = "/content/pwc-madison/global/reference-data/authorization/licenses/items";
    public static final String MADISON_CONCURRENT_LICENSE_INFO_COOKIE = "vp_sli";
    public static final String MADISON_EXTEND_CONCURRENT_LICENSE_SESSION_INFO_COOKIE = "vp_ecs";
    public static final String EXTEND_SESSION_COUNTER_KEY = "sessionCounter";
    public static final int INCREMENT_SESSION_COUNTER = 1;
    public static final String COOKIE_EXPIRY_TIME_KEY = "cookieExpiryTimeInMs";
    public static final String SHOW_REMOVE_SESSION_QUERY_PARAM = "removeSession";
    public static final String SHOW_REMOVE_SESSION_COOKIE_NAME = "vp_rs";
    public static final String DELETE_TOKENS_QUERY_PARAM = "deleteTokens";

}
