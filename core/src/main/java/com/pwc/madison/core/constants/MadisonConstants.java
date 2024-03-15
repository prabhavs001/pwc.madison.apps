package com.pwc.madison.core.constants;

public final class MadisonConstants {

    private MadisonConstants() {
        throw new IllegalStateException("Viewpoint Constants");
    }

    public static final String MADISON_READ_SUB_SERVICE = "madison-readservice";
    public static final String TERRITORY_NAME = "territoryName";
    public static final String LANGUAGE_TITLE = "languageTitle";
    public static final String SLASH_CONTENT = "/content";
    public static final String SUBSCRIBER_LIST = "subscriberlist";

    public static final String HTML_EXTN = ".html";
    public static final String DWNLD_PDF_EXTN = ".pdfdwnld.html";
    public static final String DWNLD_PDF_SELECTOR = ".dwnldpdf";
    public static final String YES = "yes";
    public static final String NO = "no";
    public static final String PAGE_CONTENT_EXTN = ".pagecontent.html";
    
    public static final String BASIC_AUTHENTICATION = "Basic ";
    public static final String COLON = ":";
    public static final String QUESTION_MARK = "?";

    // Considering the width as 489 due to the screen size
    public static final int MOBILE_MAX_WIDTH = 489;
    public static final String PWC_MADISON_CONTENT_BASEPATH = "/content/pwc-madison/";
    public static final String ALL_SELECTOR_REGEX = "(.*)";
    public static final String PWC_MADISON_SEARCH_PATH_REGEX = "/(\\w{2})/(\\w{2})/search[\\.]html(?:(.*))?";
    public static final String PWC_MADISON_CONTENT_SEARCH_BASEPATH_REGEX = "/content/pwc-madison" + PWC_MADISON_SEARCH_PATH_REGEX;
    public static final String PWC_MADISON_XF_BASEPATH = "/content/experience-fragments/pwc_madison/";
    public static final String PWC_MADISON_DAM_BASEPATH = "/content/dam/pwc-madison/";
    public static final String PWC_MADISON_PREVIEW_BASEPATH = "/content/pwc-madison/dita-preview-root/";
    public static final String PWC_MADISON_DITAROOT_DAM_PATH = "/content/dam/pwc-madison/ditaroot";
    public static final String PWC_MADISON_FASB_DITAROOT_DAM_PATH = "/content/dam/pwc-madison/ditaroot/us/en/fasb";
    public static final String FASB_US_SEARCH_ROOT_PATH="/jcr:root/content/pwc-madison/ditaroot/us/en/fasb";
    public static final String UNDERSCORE = "_";
    public static final String OPTION_VALUE_ELEMENT = "<option value=";
    public static final String CLOSING_OPTION_ELEMENT = "</option>";
    public static final String GREATER_THAN_SIGN = ">";
    public static final String WHITE_SPACE_SEPERATOR = "\\s\"";
    public static final String DOUBLE_QUOTES_REGEX = "\"";
    public static final String PWC_MADISON_GENERIC_HOMEPAGE_PATH = "/content/pwc-madison/us/en";
    public static final String DITA_CONTENT_BASE_PATH = "basePath";
    public static final String HTTPS = "https";
    public static final String TAG_TITLE_CURRENT_YEAR_PLACEHOLDER = "#year#";

    public static final String DITA_CONTENT_SITE_HIERARCHY = "/content/pwc-madison/ditaroot/(\\w{2})/(\\w{2})(.*)?";
    public static final String HOMEPAGE_CONTENT_SITE_HIERARCHY = "/content/pwc-madison/(\\w{2})/(\\w{2})(.*)?";
    public static final String MADISON_DAM_HIERARCHY = "/content/dam/pwc-madison/(\\w{2})/(\\w{2})(.*)?";
    public static final String MADISON_DAM_DITA_HIERARCHY = "/content/dam/pwc-madison/ditaroot/(\\w{2})/(\\w{2})(.*)?";
    public static final String MADISON_DAM_DITA_HIERARCHY_PWC = "/content/dam/pwc-madison/ditaroot/(\\w{2})/(\\w{2})/pwc/(.*)?";
    public static final String MADISON_DAM_DITA_TERRITORY_ROOT = "/content/dam/pwc-madison/ditaroot/\\w{2}/\\w{2}";
    public static final String AICPA_REGEX = "\\/content\\/dam\\/pwc-madison\\/ditaroot\\/.*\\/.*\\/aicpa\\/.*";
    public static final String FASB_REGEX = "\\/content\\/dam\\/pwc-madison\\/ditaroot\\/.*\\/.*\\/fasb\\/.*";
    public static final String FASB_CONTENT_REGEX = "\\/content\\/pwc-madison\\/ditaroot\\/(us|rm|ss)\\/.*\\/fasb\\/.*";
    public static final String IASBV2_CONTENT_REGEX = "\\/content\\/pwc-madison\\/ditaroot\\/(gx)\\/.*\\/iasbv2\\/.*";
    public static final String PWC_DAM_GENERAL_REGEX = "\\/content\\/dam\\/pwc-madison\\/ditaroot\\/.*\\/.*\\/<folderName>\\/.*";
    public static final String PWC__DAM_TERRITORY_FOLDER_REGEX = "/content/dam/pwc-madison/ditaroot\\/[a-zA-Z]+\\/[a-zA-Z]+\\/<folderName>";
    public static final String MADISON_USER_GROUPS_ROOT = "/home/groups/mac/default/admin/madison-workflow";
    public static final String MADISON_SITES_ROOT = "/content/pwc-madison/ditaroot";
    public static final String UTF_8 = "UTF-8";
    public static final String PN_LINKEDIN_SHARE = "linkedinShare";
    public static final String PN_TWITTER_SHARE = "twitterShare";
    public static final String PN_DESIGNATION = "designation";
    public static final String COOKIE_ENABLED = "cookieEnabled";
    public static final String CONTENT_TYPE_SORTING_ENABLED = "contentTypeSortingEnabled";
    public static final String TRUE_TEXT = "true";
    public static final String DITAROOT_TEXT = "ditaroot";
    public static final String WORKFLOW_TITLE = "workflowTitle";
    public static final String DATA_OBJ = "data";
    public static final String PROPERTY_TOPIC_TITLE = "topicTitles";
    public static final String DITA_CONTENT_PAGE_TEMPLATE = "dita-content-page-template-vp";
    public static final String FAILURE = "failure";
    public static final String EXCEPTIONS = "exceptions";
    public static final String EXCEPTION_OUTPUT_TYPE_NULL = "DitaOTGeneration failed, so not proceeding with post-processing workflow";
    public static final String EXCEPTION_OUTPUT_PATH_NON_HTML = "Output path is not html";
    public static final String PODCAST = "podcast";

    public static final String MADISON_GENERIC_SUB_SERVICE = "madison-generic-service";
    public static final String USER_GROUPS_DITA_PUBLISHERS = "publishers";
    public static final String USER_GROUPS_DITA_AUTHORS = "authors";
    public static final String USER_GROUPS_ADMINISTRATORS = "administrators";
    public static final String SLING_SELECTORS_DITAMAP = ".ditamap";
    public static final String SLING_SELECTORS_DITA = ".dita";
    public static final String STR_FOLDER = "Folder";
    public static final String MADISON_AUTHOR = "madison-author";
    public static final String MADISON_PUBLISHER = "madison-publisher";

    public static final String PN_EDITOR = "editor";
    public static final String PN_USERS = "users";
    public static final String MADISON_DAM_ROOT = "pwc-madison";
    public static final String DISPATCHER = "Dispatcher";
    public static final String NO_CACHE = "no-cache";
    public static final String COMMENT = "comment";
    public static final String EVENTS = "events";
    public static final String OLD_WFID = "old_wfID";
    public static final String REVIEW_PAGE = "reviewPage";
    public static final String EVENT_COUNT = "eventCount";
    public static final String TOPIC_PATH = "topicPath";
    public static final String ITEM_PATH = "itemPath";
    public static final String STANDARD_TYPE = "standardtype";
    public static final String CODIFICATION = "codification";
    public static final String STANDARD_NUMBER = "standardnumber";
    public static final String TILT = "~";
    public static final String FORWARD_SLASH = "/";
    public static final String DOUBLE_FORWARD_SLASH = "//";
    public static final String HYPHEN = "-";
    public static final String TOPIC = "topic";
    public static final String SUBTOPIC = "subtopic";
    public static final String SECTION = "section";
    public static final String PARAGRAPH = "pragraph";
    public static final String NA = "NA";
    public static final String DBSTANDARD_TYPE = "STANDARD_TYPE";
    public static final String DBSTANDARD_NUMBER = "STANDARD_NUMBER";
    public static final String COMMA_SEPARATOR = ",";
    public static final String PIPE_SEPARATOR = "|";
    public static final String PWC_REVISED_DATE = "pwc-revisedDate";
    public static final String PWC_PUBLICATION_DATE = "pwc-publicationDate";
    public static final String PWC_PAGE_TITLE = "pageTitle";
    public static final String PWC_CONTENT_ID = "pwc-contentId";
    public static final String QUERY_STANDARD_TYPE = "st";
    public static final String QUERY_STANDARD_NUMBER = "sn";
    public static final String QUERY_PARAGRAPH_LABEL = "pl";
    public static final String QUERY_TOPIC = "topic";
    public static final String QUERY_TERM = "term";
    public static final String QUERY_PARAGRAPH_VALUE = "Term";
    public static final String QUERY_SUBTOPIC = "stp";
    public static final String QUERY_SECTION = "sc";
    public static final String QUERY_PARAGRAPH = "ph";
    public static final String QUERY_TOTAL_COUNT = "totalcount";
    public static final String QUERY_PAGE_PATH = "page";
    public static final String AND = "AND";
    public static final String MADISON_DAM_AND_CONTENT_HIERARCHY = "/content(/dam)?/pwc-madison(/ditaroot|/dita-preview-root)?/(\\w{2})/(\\w{2})(.*)?";
    public static final String MADISON_XF_HIERARCHY = "/content/experience-fragments/pwc_madison/(\\w{2})/(\\w{2})(.*)?";
    public static final String MADISON_PAGE_HIERARCHY = "(/content/pwc-madison(/ditaroot)?/(\\w{2})/(\\w{2}))(.*)?";
    public static final String MADISON_HOMEPAGE_ABSOLUTE_URL = "/content/pwc-madison/(\\w{2})/(\\w{2})(.html)";
    public static final String MADISON_HOMEPAGE_HIERARCHY = "/content/pwc-madison/(\\w{2})/(\\w{2})(.html)?";
    public static final String MADISON_HOMEPAGE_REGEX = "/content/pwc-madison(/ditaroot)?/(\\w{2})/(\\w{2})";
    public static final String TERRITORY_QUERY_PARAM = "territory";
    public static final String LOCALE_QUERY_PARAM = "locale";
    public static final String REFERRER_QUERY_PARAM = "referrer";
    public static final String REQ_PARAM_USER_TYPE = "userType";
    public static final String REQ_PARAM_CONTENT_TYPE = "contentType";
    public static final String MADISON_BASE_CONTENT_HIERARCHY = "/content/pwc-madison/";
    public static final String REGEX_MATCHING_GROUP_ONE = "$1";
    public static final String REGEX_MATCHING_GROUP_TWO = "$2";
    public static final String REGEX_MATCHING_GROUP_THREE = "$3";
    public static final String REGEX_MATCHING_GROUP_FOUR = "$4";
    public static final String REGEX_MATCHING_GROUP_FIVE = "$5";
    public static final Long HOURS_TO_MILLIS = 60 * 60 * 1000L;
    public static final String WORKFLOW_SYSTEM_USER_COMMENT = "Process Step - Workflow System User";
    public static final String WORKFLOW_DATA_PROPERTY_COMMENT = "comment";
    public static final String HASH = "#";
    public static final String PROPERTY_SCOPE = "scope";
    public static final String PROPERTY_UUID = "uuid";
    public static final String PROPERTY_LINK = "link";
    public static final String PROPERTY_IMAGE_PATH = "imagePath";
    public static final String PROPERTY_ED_NOTE = "edNote";
    public static final String PROPERTY_PUBLICATION_DATE = "publicationDate";
    public static final String PROPERTY_REVISION_DATE = "revisionDate";

    public static final String AEM_PAGES = "aempages";
    public static final String AUDIENCE = "s_p_a";
    public static final String CC = "sp_k";
    public static final String PAGE = "page";
    public static final String SAML_ERROR_REQUEST_PARAM = "samlerror";
    public static final String SEARCH_URL = "/search/query/";
    public static final String SEARCH_URL_PROPERTY_NAME = "searchURL";

    public static final String SYNDICATION_SERVICE_USER = "syndication-serviceuser";
    public static final String PN_IS_SYNDICATED = "isSyndicated";
    public static final String CONF_SYNDICATION_SETTINGS_ROOT = "/conf/pwc-madison/settings/syndication";
    public static final String AUTO_PUBLISHING_QUEUE_PATH = "/var/pwc-madison/syndication-autopublish-queue";
    public static final String ONGOING_PUBLISHING = "ongoingPublishing";
    public static final String PN_PUBLISHING_STATUS = "publishingStatus";
    public static final String PUBLISHING_POINTS = "publishingPoints";
    public static final String PUBLISH_BEACON = "PUBLISHBEACON";
    public static final String GENERATE_OUTPUT= "GENERATEOUTPUT";
    public static final String OUTPUT_TYPE_AEMSITE = "aemsite";
    public static final String PENDING = "pending";
    public static final String IN_PROGRESS = "In-progress";
    public static final String NO_REPLY_EMAIL_ID = "noreply@adobe.com";
    public static final String SYNDICATION_COMPLETE_EMAIL_TEMPLATE = "/etc/notification/email/madison/syndication-complete.html/jcr:content";
    public static final String SYNDICATION_UPDATE_EMAIL_TEMPLATE = "/etc/notification/email/madison/syndication-update.html/jcr:content";
    public static final String JOINED_PAGE_URL = "joinedPageUrl";
    public static final String JOINED_SECTION_LEVEL = "joinedSectionLevel";

    /**
     * Constants used for BrokenLinksReportServlet
     */
    public static final String BROKEN_LINKS_INPUT_TYPE = "inputType";
    public static final String BROKEN_LINKS_INPUT_PATH = "path";
    public static final String STR_TOPICS = "topics";
    public static final String METADATA_PATH = "/jcr:content/metadata";
    public static final String SME_PROPERTY = "pwc-smes";
    public static final String REPLICATION_ACTION_PROPERTY = "cq:lastReplicationAction";
    
    
    // Node Flattening
    public static final String FLATTENED_PROPERTY = "flattened";
    public static final String PWC_P_PATH = "/jcr:content/root/maincontainer/readerrow/bodycontainer/docreader/contentbody/topicbody/pwc-topic/pwc-body/p";
    public static final String PWC_XREF_PATH = "/jcr:content/root/maincontainer/readerrow/bodycontainer/docreader/contentbody/topicbody/pwc-topic/pwc-body/p/pwc-xref";
    public static final String PWC_XREF_INFO_TEXT_PATH = "/jcr:content/root/maincontainer/readerrow/bodycontainer/docreader/contentbody/topicbody/pwc-topic/pwc-body/p/pwc-xref/xref-info/_text";
    public static final String PWC_XREF_INFO_TEXT_PROPERTY = "text";
    public static final String PWC_XREF_LINK_POPERTY = "link";
    public static final String HTML_ANCHOR_PWC_XREF = "a.pwc-xref";
    public static final String HTML_HREF = "href";
    public static final String HTML_XREF_INFO_DIV = ".xref-info div";
    public static final String FLATTENED_ALERTS = "Alerts";
    public static final String FLATTENED_NEWS = "News";
    public static final String FLATTENED_RECENTLY_VIEWED = "RecentlyViewed";
    

    /**
     * Date format used in Viewpoint components
     */
    public static final String COMPONENTS_DATE_FORMAT = "dd MMM yyyy";
    public static final String REVISED_DATE_FORMAT = "dd MMM yyyy";
    public static final String HOMEPAGE_COMPONENTS_DATE_FORMAT = "dd MMMM yyyy";

    // ALERT-TILE COMPONENT CONSTANTS
    public static final String JSON_COOKIE_KEY = "CookieValue";
    public static final String DISMISS_PAGE_COOKIE_NAME = "pwc-dismissed-alerts";
    public static final String DISMISS_PAGE_GLOBAL_COOKIE_NAME = "pwc-dismissed-alerts-global";
    public static final String READ_ALERTS_COOKIE_NAME = "pwc-read-alerts";
    public static final String CQ_PAGE = "cq:Page";
    public static final String PWC_CONTENT_TYPE_VAL = "ContentType_Alerts_FW";
    public static final String OFFSET = "0";
    public static final String ORDER_BY = "desc";
    public static final String SYMB_AT = "@";
    public static final String FALSE_TEXT = "false";
    public static final String EXISTS = "exists";
    public static final String SHORT_DESC_PATH = "root/maincontainer/readerrow/bodycontainer/docreader/contentbody";
    public static final String SHORT_DESC_NODENAME = "shortdesc";
    public static final String _TEXT = "_text";
    public static final String NODE_PROP_TEXT = "text";
    public static final String P_LIMIT = "-1";
    public static final String PWC_BODY = "pwc-body";
    public static final String SUB_CHILD_NODE = "p";
    public static final String LINK_NODE = "pwc-xref";
    public static final String LINK_TEXT_NODE = "xref-info";
    public static final String TITLE_NODE = "title";
    public static final String LINK = "link";
    public static final String PWC_TOPIC = "pwc-topic";
    public static final int NUMBER_OF_DAYS = -30;
    public static final int COOKIE_EXPIRE_HOURS = 24 * 30;
    public static final String PUBLISH_INSTANCE = "publish";
    // ALERT-TILE COMPONENT CONSTANTS END

    // BULK TRANSLATION CONSTANTS
    public static final String TRANSLATION_COMPLETE_EMAIL_TEMPLATE = "/etc/notification/email/madison/translation-complete.html/jcr:content";
    public static final String TRANSLATION_UPDATE_EMAIL_TEMPLATE = "/etc/notification/email/madison/translation-update.html/jcr:content";

    // Expiration Report
    public static final String USER_GROUPS_DITA_EXPIRATION_APPROVER = "madison-expiration-approver";
    public static final String USER_GROUPS_DITA_PUBLISHER = "madison-publisher";
    public static final String CONFIRMATION_STATUS = "confirmationstatus";
    public static final String EXPIRATION_CONTENT_PATH = "path";
    public static final String EXPIRATION_USERID = "userID";
    public static final String DATE_RANGE = "daterange";
    public static final String VIEW = "view";
    public static final String APPROVERS_ROLE = "approvers";
    public static final String REVIEWERS_ROLE = "reviewers";
    public static final String PUBLISHERS_ROLE = "publishers";
    public static final String EXPIRING_CONTENT = "expiringContent";
    public static final String DITAMAP_EDITOR_PATH = "/libs/fmdita/report/report.html";
    public static final String DITA_EDITOR_PATH = "/assetdetails.html";
    public static final String EXPIRATION = "expiration-";
    public static final String OPS = "ops-";

    // EXPIRY DATE CALCULATOR CONSTANTS
    public static final String CONTENT_TYPE_REF_DATA_PATH = "/content/pwc-madison/global/reference-data/metadata/content-type/items";
    public static final String EXPIRY_PERIOD = "expiryPeriod";
    public static final String CONTENT_TYPE = "contentType";
    public static final String VALUE_PROPERTY = "value";
    public static final String INPUT_DATE = "inputDate";
    public static final String DATE_FORMAT = "EEE MMM dd HH:mm:ss zzz yyyy";
    public static final String OUTPUT_DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSSZ";
    public static final String PN_ITEM = "item";
    public static final String PN_DEFAULT = "default";

    public static final String METADATA_RELATIVE_PATH = "jcr:content/metadata";
    public static final String SYNDICATION_EDITABLE_PROPERTIES_REF_DATA_PATH = "/content/pwc-madison/global/reference-data/metadata/syndication-editable-metadata/items";
    public static final String GENERIC_INCLUSION_PROPERTIES_REF_DATA_PATH = "/content/pwc-madison/global/reference-data/metadata/syndication-generic-metadata/items";

    /**
     * Constants used for VitalStatsReportServlet
     */
    public static final String VITAL_STATS_INPUT_PATH = "path";
    public static final String REPLICATION_ACTION_ACTIVATE = "Activate";
    public static final String REPLICATION_ACTION_DEACTIVATE = "Deactivate";

    // User type
    public static final String INTERNAL_USER = "internal";
    public static final String EXTERNAL_USER = "external";

    // JWT Signing Key for Insert Recently Published Items to TempTable
    public static final String JWT_SIGNING_KEY = "KY#N!E!c0mRK5#4NTqLzp4n";

    public static final String MADISON_PAGE_URL_REGEX = ".*/content/pwc-madison(/ditaroot)?/(\\w{2})/(\\w{2})(.*)?";
    public static final String MADISON_DOC_CONTEXT_REGEX = ".*/content/pwc-madison(/ditaroot)?/(.*)";

    public static final String LOCAL_CONTENT_UPDATE_TEMPLATE = "/etc/notification/email/madison/local-content-update.html/jcr:content";

    /**
     * Constants for referenced asset replication
     */
    public static final String REPLICATE_REFERENCED_ASSETS_WF_MODEL = "/var/workflow/models/replicate-referenced-assets";
    public static final String PN_JCR_PATH = "JCR_PATH";
    public static final String OUTPUT_PATH = "outputPath";
    public static final String REGENERATED_TOPICS = "regeneratedTopics";

    // Constants for global components relative path
    public static final String GLOBAL_COMPONENTS_RELATIVE_PATH_FROM_PAGE = "root/page_maincontainer/maincontainer/";
    public static final String HEADER_RELATIVE_PATH_FROM_PAGE = GLOBAL_COMPONENTS_RELATIVE_PATH_FROM_PAGE + "header";
    public static final String FOOTER_RELATIVE_PATH_FROM_PAGE = GLOBAL_COMPONENTS_RELATIVE_PATH_FROM_PAGE + "footer";
    public static final String GLOBAL_NAVIGATION_RELATIVE_PATH_FROM_PAGE = HEADER_RELATIVE_PATH_FROM_PAGE
            + FORWARD_SLASH + "global_navigation";
    public static final String TERRITORY_MAPPING_RELATIVE_PATH_FROM_PAGE = GLOBAL_COMPONENTS_RELATIVE_PATH_FROM_PAGE + ".territorymapping";

    // Media Wrapper
    public static final String MEDIA_WRAPPER_RESOURCE_TYPE = "pwc-madison/components/ditacontent/dynamicmedia";

    /**
     * The system user has all rights on Viewpoint content. It is mostly created for generation of sitemap XML file which
     * requires read, write and replication rights.
     */
    public static final String MADISON_CONTENT_ADMIN_SUB_SERVICE = "madison-content-admin";

    // Unused Assets Report
    public static final String SEMI_COLON_SEPARATOR = ";";

    // SetDitaRevisionProcess constant
    public static final String APPROVED = "Approved";

    // i18n key prefix
    public static final String USERREG_TNC_CONTENT = "UserReg_Tnc_Content_";
    public static final String USERREG_REGISTER_ACKNOWLEDGE_TEXT = "UserReg_Register_Acknowledge_Text_";

    // territory code
    public static final String UK_TERRITORY_CODE = "uk";
    public static final String GB_TERRITORY_CODE = "gb";
    public static final String JP_TERRITORY_CODE = "jp";
    
    // default English locale
    public static final String ENGLISH_LOCALE = "en_US";
    public static final String ENGLISH_INT_LOCALE = "en_GX";

    // Syndication Report
    public static final String SYNDICATION_INPUT_PATH = "path";
    // workflow complete
    public static final String WORKFLOW_COMPLETION_TEMPLATE = "/etc/notification/email/madison/workflow-complete.html/jcr:content";

    // Auto Replication via workflow
    public static final String AUTO_REPLICATION_PACKAGE_PATH = "/etc/packages/com.pwc.madison.ditacontent.activation";
    public static final String AUTO_REPLICATION_PACKAGE_GROUP = "com.pwc.madison.ditacontent.activation";
    public static final String REP_STATUS_CONTENT_PATH = "/conf/pwc-madison/settings/content-package-replication-status";
    public static final String CONTENT_FMDITACUSTOM_INDEX = "/content/fmditacustom/xrefpathreferences";

    // Vital Stats Report Columns
    public static final String ASSET_PATH_COLUMN = "assetPath";
    public static final String WORDS_COLUMN = "words";
    public static final String PARAGRAPHS_COLUMN = "paragraphs";
    public static final String TABLES_COLUMN = "tables";
    public static final String LINKS_COLUMN = "links";
    public static final String ANCHORS_COLUMN = "anchors";
    public static final String DAMSHA_COLUMN = "damsha1";

    public static final String NOT_AVAILABLE = "n/a";

    // Create Collaboration
    public static final String ALLOW_ALL_REVIEWERS = "allowAllReviewers";
    public static final String COLLABORATION_TASK_ACTION_COMPLETE = "collaboration-complete";

    // Servlet constants
    public static final String CONTENTTYPE_JSON = "application/json";
    // Activities Report Constants
    public static final String ACTIVITY = "activity";
    
    // Ghost Page
    public static final String GHOST_MODULE_PATH = "jcr:content/root/ghost_page_module";
    public static final String GHOST_PUBLICATION_DATE = "publicationDate";
    public static final String TOPIC_TEXT = "topicText";
    public static final String TOPIC_LABEL = "topicLabel";
    public static final String ABSTRACT_TEXT = "Abstract";
    public static final String SUMMARY_TEXT = "featureSummary";
    public static final String LINK_URL = "linkUrl";
    public static final String LINK_LABEL = "linkLabel";

    //Homepage components name for analytics tracking
    public static final String HERO_COMPONENT_NAME = "Hero";
    public static final String FEATURED_COMPONENT_NAME = "Suggested for you";
    public static final String MOST_POPULAR_COMPONENT_NAME = "Trending";
    public static final String INSIGHTS_COMPONENT_NAME = "Insights from PwC";
    public static final String RECENTLY_VIEWED_COMPONENT_NAME = "Recently Viewed";
    public static final String NEWS_COMPONENT_NAME = "News";
    public static final String WEBCASTS_AND_PODCASTS_COMPONENT_NAME = "Webcasts & Podcasts";
    
    //Sitemap/Records XML constants
    public static final String SITEMAP_TAG_SITEMAP_INDEX = "sitemapindex";
    public static final String NS = "http://www.sitemaps.org/schemas/sitemap/0.9";
    
    //Analytics
    public static final String ANALYTICS_HEADER_COMPONENT_NAME = "header";
    
    public static final String VP_TO_VP_REDIRECTIONS_KEY = "vptovpredirectsk";
        
    public static final String CSV_EXTENSION = "csv";
    public static final String CSV_CONTENT_TYPE = "text/csv";
    public static final String CONTENT_DISPOSITION = "Content-disposition";
    public static final String INTERNAL_ONLY = "Internal Only";
}
