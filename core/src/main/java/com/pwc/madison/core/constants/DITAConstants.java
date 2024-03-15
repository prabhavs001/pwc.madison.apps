package com.pwc.madison.core.constants;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.sling.api.resource.ResourceResolverFactory;

public class DITAConstants {

    private DITAConstants() {
    }

    public static final String META_COUNTRY = "pwc-country";
    public static final String META_REVISION_DATE = "pwc-revisedDate";
    public static final String META_PUBLICATION_DATE = "pwc-publicationDate";
    public static final String META_CONTENT_TYPE = "pwc-contentType";
    public static final String META_CONTENT_ID = "pwc-contentId";
    public static final String PN_SME = "pwc-smes";
    public static final String PN_TEMPLATE_AND_TOOLS = "pwc-relatedtemplates";
    public static final String PN_RELATED_CONTENT = "pwc-relatedContent";
    public static final String META_DESCRIPTION = "jcr:description";
    public static final String META_TEMPLATE_TYPE = "cq:template";
    public static final String META_KEYWORDS = "pwc-keywords";
    public static final String META_GUIDANCE_TERMS = "pwc-guidanceTerms";
    public static final String META_SUGGESTED_GUIDANCE = "pwc-suggestedGuidance";
    public static final String META_SUGGESTED_GUIDANCE_ORDER = "pwc-guidanceOrder";
    public static final String META_HIDE_PUBLICATION_DATE = "pwc-hidePublicationDate";
    public static final String META_LANGUAGE = "pwc-lang";
    public static final String META_SECONDARY_LANGUAGE = "pwc-secondaryLang";
    public static final String META_LOCALE = "jcr:language";
    public static final String META_TAGS = "cq:tags";
    public static final String META_CREATED_DATE = "jcr:created";
    public static final String META_LAST_REPLICATED_DATE = "cq:lastReplicated";
    public static final String META_ORIGINAL_RELEASE_DATE = "pwc-originalReleaseDate";
    public static final String META_SCHEDULED_PUBLISHING_DATE = "pwc-embargoedDate";
    public static final String META_EXPIRY_DATE = "pwc-expirationDate";
    public static final String META_EFFECTIVE_AS_OF_DATE = "pwc-effective-date";
    public static final String META_AUDIENCE = "pwc-audience";
    public static final String META_PRIVATE_GROUP = "pwc-privateGroup";
    public static final String META_ACCESS_LEVEL = "pwc-access";
    public static final String META_LICENSE = "pwc-license";
    public static final String META_RELATED_EXAMPLE_LINKS = "pwc-relatedexamples";
    public static final String META_CANONICAL_URL = "pwc-canonicalurl";
    public static final String META_RELATED_INSIGHTS = "pwc-relatedinsights";
    public static final String META_STANDARD_SETTERS = "pwc-standardsetter";
    public static final String META_HIDE_PAGE_FROM_SITE = "pwc-hiddenFromSiteSearch";
    public static final String META_ROBOTS = "pwc-metaRobots";
    public static final String META_DISABLE_PDF_DWNLD = "pwc-disablePdfDownload";
    public static final String META_HIDE_SEARCH_WITH_IN_DOC = "pwc-hideSearchWithInDoc";
    public static final String META_SHOW_STATIC_TOC = "pwc-showStaticToc";
    public static final String META_LOAD_LITE_TOC = "pwc-loadLiteToc";
    public static final String META_JOINED_SECTION_TOC = "pwc-joinedSectionToc";
    public static final String META_JOINED_SECTION_LEVEL = "pwc-joinedSectionTocLevel";
    public static final String META_OVERRIDE_GLOBAL_JOIN_SETTINGS = "pwc-overrideGlobalJoinSettings";
    public static final String META_DISABLE_FEATURE_SUMMARY = "pwc-featureSummary";
    public static final String META_IS_PUBLISHED = "pwc-isPublished";
    public static final String META_IS_DOC_CONTEXT_SEARCH = "pwc-docContextSearch";
    public static final String META_DOC_CONTEXT_SEARCH_IDENTIFIER = "pwc-docContextSearchIdentifier";
    public static final String META_SORT_ORDER = "pwc-sortOrder";

    public static final String META_COPYRIGHT = "pwc-copyright";
    public static final String META_FAQ = "pwc-relatedfaqs";
    public static final String META_APPROVED_REJECTED_DATE = "pwc-approvedRejectedDate";
    public static final String META_EXPIRATION_CONFIRMATION_STATUS = "pwc-expiryConfirmationState";
    public static final String META_EXPIRATION_APPROVED_REJECTED_BY = "pwc-approvedRejectedBy";
    public static final String META_TOPIC_IMAGE = "pwc-topicTitleImage";
    public static final String PN_VALUE_PWC = "pwc";
    public static final String PWC_SOURCE_VALUE = "ss_pwc";
    public static final String SOURCE_PATH = "sourcePath";
    public static final String BASE_PATH = "basePath";
    public static final String PN_EFFECTIVE_SOURCE_PATH = "effectiveSourcePath";

    public static final String PROPERTY_TYPE = "type";
    public static final String PROPERTY_LINK = "link";
    public static final String PROPERTY_DITA_TITLE = "fmditaTitle";
    public static final String PROPERTY_DITA_CLASS = "dita_class";
    public static final String PROPERTY_FIRST_NAME = "firstname";
    public static final String PROPERTY_LAST_NAME = "lastname";
    public static final String PROPERTY_EMAIL = "email";
    public static final String PROPERTY_PHONE = "phone";
    public static final String PROPERTY_DOWNLOAD_PDF_PATH = "downloadPDFPath";
    public static final String PROPERTY_DOWNLOAD_PDF_TITLE = "downloadPDFTitle";
    public static final String PROPERTY_LEVEL = "pwc-level";
    public static final String PROPERTY_IS_LEVEL_set = "pwc-isLevelSet";
    public static final String JCR_CONTENT = "/jcr:content";
    public static final String SEARCH_DOC_DELIMETER = "X";
    public static final int SEARCH_DOC_TRUNCATE_MAX_LIMIT = 5;
    public static final int SNP_SINGLE_WORD_MAX_CHAR_LIMIT = 32;

    public static final String DITA_TYPE_BOOKMAP = "bookmap";
    public static final String DITA_EXTENSION = ".dita";
    public static final String PWC_BODY = "pwc-body";
    public static final String EXAMPLE_BODY = "example-body";
    public static final String FAQ_QUESTION_TEXT = "/pwc-faq/title";
    public static final String FAQ_ANSWER_TEXT = "/pwc-faq/faq-body/faq-bodydiv/answer-text";
    public static final String FAQ_ADDITIONAL_QUESTION_TEXT = "/pwc-faq/faq-body/faq-bodydiv/question-text";
    public static final String QUESTION_TEXT = "question-text";
    public static final String ANSWER_TEXT = "answer-text";
    public static final String PWC_TOPIC_TITLE = "/pwc-topic/title";
    public static final String PWC_EXAMPLE_TITLE = "/pwc-example/title";
    public static final String PWC_TOPIC_BODY = "/pwc-topic/pwc-body";
    public static final String PWC_EXAMPLE_BODY = "/pwc-example/example-body";

    public static final String CHUNKED_TOPIC_PATHS = "chunkedTopicPaths";
    public static final String PWC_MADISON_COMPONENTS_STRUCTURE_PAGE_VP = "pwc-madison/components/structure/page-vp";

    public static final String DITA_PWC_HEAD_CONTAINER = "/jcr:content/root/container/maincontainer/readerrow/docreader/headnode/";
    public static final String DITA_PWC_TOPIC_CONTAINER = "/jcr:content/root/container/maincontainer/readerrow/docreader/topicnode/topicbody";
    public static final String DITA_PWC_TOPIC = "/jcr:content/root/container/maincontainer/readerrow/docreader/topicnode/topicbody/pwc-topic/";
    public static final String RIGHT_RAIL_NODE_PATH = "jcr:content/root/container/maincontainer/readerrow/rail";
    public static final String DOCREADER_NODE_RELATIVE_PATH = "jcr:content/root/container/maincontainer/readerrow/docreader";

    public static final String DITA_PWC_HEAD_CONTAINER_V1 = "/jcr:content/root/maincontainer/readerrow/bodycontainer/docreader/contentbody/headnode/";
    public static final String DITA_PWC_TOPIC_CONTAINER_V1 = "/jcr:content/root/maincontainer/readerrow/bodycontainer/docreader/contentbody/topicbody";
    public static final String DITA_PWC_TOPIC_V1 = "/jcr:content/root/maincontainer/readerrow/bodycontainer/docreader/contentbody/topicbody/pwc-topic/";
    public static final String RIGHT_RAIL_NODE_PATH_V1 = "jcr:content/root/maincontainer/readerrow/bodycontainer/rail";
    public static final String DOCREADER_NODE_RELATIVE_PATH_V1 = "jcr:content/root/maincontainer/readerrow/bodycontainer/docreader/contentbody/topicbody";

    public static final String TOPIC_HEAD_NODE_NAME = "headnode";
    public static final String TOPIC_CREATED_DATE_NODE_NAME = "critdates";
    public static final String PN_XML_DATA_PROPERTY = "data";
    public static final String PN_TOPIC_NUMBER = "topicNum";
    public static final String PN_SUB_TOPIC_NUMBER = "subtopicNum";
    public static final String PN_SECTION_NUMBER = "sectionNum";
    public static final String PN_ASC_TOPIC_NUMBER = "ascTopicNum";
    public static final String PN_ASC_SUBTOPIC_NUMBER = "ascSubtopicNum";
    public static final String PN_ASC_SECTION_NUMBER = "ascSectionNum";
    public static final String PN_TEXT = "text";
    public static final String PN_SECTION_LONG_TITLE = "longTitle";
    public static final String DITA_COMPONENT_DISCARD = "fmdita/components/dita/discard";
    public static final String PN_PAGE_TITLE = "pageTitle";
    public static final String FORWARD_SLASH = "/";
    public static final String PN_CLASS_NAME = "class";
    public static final String TOPIC_EXAMPLE_CLASS_NAME = "pwc-example";
    public static final String TOPIC_FAQ_CLASS_NAME = "pwc-faq";
    public static final String TOPIC_CLASS_NAME = "topic";
    public static final String PN_ID = "id";
    public static final String PN_FMDITATOPICREFS = "fmditaTopicrefs";
    public static final String PN_METADATA_DOCSTATE = "docstate";
    public static final String PN_METADATA_LAST_PUBLISHED = "pwc-last-published";
    public static final String PN_SLING_RESOURCE_TYPE = "sling:resourceType";
    public static final String PN_METADATA_LAST_UNPUBLISHED = "pwc-lastUnpublished";
    public static final String DITAOT_FAILURE = "ditaotFaliure";
    public static final String POSTPROCESSING_LOG_FILENAME = "postprocessing-logs.txt";
    public static final String TEXT_PLAIN = "text/plain";
    public static final String OUTPUT_HISTORY_PATH = "outputHistoryPath";
    
    //DITA Citation Reporting
    public static final String FAILURE_STATUS = "Failure";
    public static final String SUCCESS_STATUS = "Success";
    public static final String FAILURE_ANCHOR_ID_NOT_FOUND = "Anchor ID '%s' not found within the scope for Citation '%s'";
    public static final String FAILURE_DITA_REPLACE_FAILED = "Could not replace <autolink> element with <pwc-xref> in the Source Dita for Citation '%s'";

    // DITA Optimized XML Node Names
    public static final String XML_NODE_CREATED_DATE = "created";
    public static final String XML_ATTRIBUTE_DATE = "date";

    public static final String STATIC_STR_EXAMPLE = "Example";
    public static final String STATIC_STR_FAQ = "Faq";
    public static final String STATIC_STR_TOPIC = "Topic";

    // DITA Constants used for PublishWorkflow
    public static final String WF_METADATA = "metaData";

    public static final String URL_PARAM_PAGE_READ_ONLY = "&wcmmode=disabled";

    public static final int STATUS_NONE = 0;

    public static final int IN_PROGRESS = 1;

    public static final int COLLAB_PROGRESS = 3;

    public static final String REV_IN_PROGRESS = "reviewProgess";

    public static final String COLLAB_IN_PROGRESS = "collaborationProgess";

    public static final String COMPLETE_STATUS = "complete";

    public static final int COMPLETE = 2;

    public static final String STATUS_PROP_NAME = "status";

    public static final String REPORT_PAGE_URL = "/libs/fmdita/report/report_page.html?payload=";

    public static final String PERFORMANCE_LOGGER = "com.adobe.fmdita.perf";

    public static final String DITAMAP_EXT = ".ditamap";

    public static final String FM_EXT = ".fm";

    public static final String HTML_EXT = ".html";

    public static final String BOOK_EXT = ".book";

    public static final String REVIEW_WF_PROP_NAME = "reviewWorkflows";

    public static final String REVIEW_STORE_PATHS = "/var/dxml/reviews/paths";

    public static final String REVIEW_STORE_DATA = "/var/dxml/reviews/data";

    public static final String WCMMODE_DISABLED = "&wcmmode=disabled";

    public static final String APPROVAL_WF_PROP_NAME = "approvalWorkflows";

    public static final String PATH_PROP_NAME = "path";

    public static final String DEADLINE = "deadline";

    public static final String DESCRIPTION = "description";

    public static final String TITLE = "title";
    public static final String PATH = "path";
    public static final String TOPIC_REVIEW_STATUS = "reviewStatus";

    public static final String TOPIC_INDEX = "idx";

    public static final String NEW_FILE_FLAG = "fmdita-newfile";
    public static final String TEMPLATE_FILE_FLAG = "fmdita-dita-template";

    public static final String FMPS_VERSION_2018 = "2019";

    public static final String WORKFLOW_ID = "wId";
    public static final String COLLAB_WORKFLOW = "collabWorkflow";
    public static final String MULTI_ITEM_SEPARATOR = "\\|";
    public static final String USER_SEPARATOR = ",";

    public static final String PROJECT_PATH = "projectPath";
    public static final String ABSOLUTE_TIME = "absoluteTime";
    public static final String START_TIME = "startTime";
    public static final String DUE_TIME = "dueTime";
    public static final String PARENT_DITAMAP = "parentDitamap";
    public static final String INITIATOR = "initiator";
    public static final String TASK_ID = "taskId";
    public static final String TASK_IDS = "taskIds";
    public static final String TASK_DUE_DATE = "taskDueDate";
    public static final String PUBLISH_TOPICS = "pubTopics";
    public static final String ORIGINAL_TOPICS = "orgTopics";
    public static final String SELECTED_TOPICS = "selectedTopics";

    public static final String REVIEW_TASK_TYPE = "review-task";
    public static final String REVIEW_TASK_NOTIFICATION = "review-task-notification";
    public static final String REVIEW_WF = "RevWf";
    public static final String PROJECT_TASK = "projectTask";
    public static final String INPUT_PAYLOAD = "payloadJson";

    public static final String EMPTY_JSON_OBJECT = "{}";
    public static final String EMPTY_JSON_ARRAY = "[]";

    public static final String REPUBLISH = "republish";
    public static final String PUBLISH_WORKFLOW_MODEL_KEY = "/models/publishditamap";

    public static final String REVIEW_CLOSURE_JOB_ID = "reviewClosureJobId";
    public static final String TASK_ACTION_COMPLETE = "Review Completed";
    public static final String REVIEW_OPERATION = "AEM_REVIEW";
    public static final String DITA_APPROVE_OPERATION = "DITA_APPROVE";
    public static final String REVIEW_PAGE = "/libs/fmdita/review/inlinereview.html?rId=";
    public static final String NEW_REVIEW_UI_PAGE = "/libs/fmdita/clientlibs/xmleditor_review/page.html?rId=";

    public static final String ASSIGNEE = "assignee";
    public static final String COLLABORATORS = "collaborators";
    public static final String APPROVER = "approver";
    public static final String PUBLISHER = "publisher";
    public static final String REJECTION_LIST = "reject-list";
    public static final String REVIEWER = "reviewer";
    public static final String MAC_DEFAULT = "mac-default";
    public static final String INPUT_PAYLOAD_BASE = "base";
    public static final String INPUT_PAYLOAD_ASSET = "asset";
    public static final String IS_DITAMAP = "isDitamap";
    public static final String DITAMAP = "ditamap";
    public static final String DITAMAP_INCLUDE_TOPICS = "ditamaptopics";
    public static final String NOTIFY_EMAIL = "notifyViaEmail";
    public static final String OPERATION = "operation";

    public static final String REVIEW_STATUS = "reviewstatus";
    public static final String APPROVAL_STATUS = "approvalstatus";
    public static final String REJECTED = "rejected";
    public static final String DITA_DOCUMENTSTATE_INREVIEW = "In-Review";
    public static final String DITA_DOCUMENTSTATE_DRAFT = "Draft";
    public static final String DITA_DOCUMENTSTATE_REVIEWED = "Reviewed";
    public static final String DITA_DOCUMENTSTATE_APPROVED = "Approved";
    public static final String DITA_DOCUMENTSTATE_DONE = "Published";
    public static final String DITA_DOCUMENTSTATE_UNPUBLISHED = "Unpublished";

    public static final String WF_MODEL_REVIEW_DITAMAP_KEY = "/var/workflow/models/ReviewDITAMAP";
    public static final String WF_MODEL_FULL_CYCLE_REVIEW_DITAMAP_KEY = "/var/workflow/models/pwc-fullcyle-workflow";
    public static final String WF_MODEL_APPROVAL_DITAMAP_KEY = "/var/workflow/models/pwc-simplified-wf";
    public static final String WF_MODEL_COLLABORATION = "/var/workflow/models/pwc-collaboration-workflow";
    public static final String ITEM_SEPARATOR = "|";

    public static final String FMDITA_PATH = "/var/dxml";
    public static final String REVIEW_NODE = "reviews";
    public static final String REVIEW_DATA_NODE = "data";
    public static final int REVIEW_TREE_MAX_CHILDREN = 20;
    public static final int REVIEW_TREE_DEPTH = 3;
    public static final String REVIEW_ID = "rId";
    public static final String ALLOW_ALL_REVIEWERS = "allowAllReviewers";
    public static final String REVIEW_VERSION = "reviewVersion";
    public static final String DITAMAP_HIERARCHY = "ditamapHierarchy";
    public static final String PROP_REVIEW_PAGE = "reviewPage";
    public static final String PROP_VERSION_JSON = "versionJson";
    public static final String PROP_WORKFLOW_ID = "wfId";

    // To do - Read this data directly from refdata and remove these constants
    public static final String AUDIENCE_INTERNAL_EXTERNAL = "internalExternal";
    public static final String AUDIENCE_INTERNAL_ONLY = "internalOnly";
    public static final String AUDIENCE_EXTERNAL_ONLY = "externalOnly";
    public static final String AUDIENCE_PRIVATE = "privateGroup";
    public static final String ACCESS_LEVEL_FREE = "free";
    public static final String ACCESS_LEVEL_PREMIUM = "premium";
    public static final String ACCESS_LEVEL_LICENSED = "licensed";

    public static final String DITA_TAG_BODY_DIV = "bodydiv";
    public static final String FAQ_DITA_TAG_BODY_DIV = "faq-bodydiv";
    public static final String EXAMPLE_DITA_TAG_BODY_DIV = "example-bodydiv";
    public static final String ATTRIBUTE_ID = "id";
    public static final String ATTRIBUTE_CONREF = "conref";
    public static final String NEWLINE_REGEX = "\r\n|\n";
    public static final String HASH_STR = "#";
    public static final String DITA_BODYDIV_TAG_END = "\"></bodydiv>";
    public static final String FAQ_DITA_BODYDIV_TAG_END = "\"></faq-bodydiv>";
    public static final String EXAMPLE_DITA_BODYDIV_TAG_END = "\"></example-bodydiv>";
    public static final String DITA_BODYDIV_CONREF_ELEMENT = "<bodydiv conref=\"";
    public static final String FAQ_DITA_BODYDIV_CONREF_ELEMENT = "<faq-bodydiv conref=\"";
    public static final String EXAMPLE_DITA_BODYDIV_CONREF_ELEMENT = "<example-bodydiv conref=\"";
    public static final String BODYDIV_REGEX = "(?i)<bodydiv.*?</bodydiv>";
    public static final String FAQ_BODYDIV_REGEX = "(?i)<faq-bodydiv.*?</faq-bodydiv>";
    public static final String EXAMPLE_BODYDIV_REGEX = "(?i)<example-bodydiv.*?</example-bodydiv>";
    public static final String BODYDIV_EMPTY_REGEX = "(?i)<bodydiv.*?/>";
    public static final String FAQ_BODYDIV_EMPTY_REGEX = "(?i)<faq-bodydiv.*?/>";
    public static final String EXAMPLE_BODYDIV_EMPTY_REGEX = "(?i)<example-bodydiv.*?/>";
    public static final String PN_IS_SYDICATED = "isSyndicated";
    public static final String METADATA_NAME = "metadata";
    public static final String APPLICATION_XML = "application/xml";

    public static final String DITA_TAG_ANCHOR_ID = "anchor-id";
    public static final String DITA_TAG_TABLE = "table";
    public static final String DITA_TAG_XREF = "xref";
    public static final String DITA_TAG_PWC_XREF = "pwc-xref";
    public static final String DITA_TAG_PARAGRAPH = "p";
    public static final String DITA_TAG_SUPERSCRIPT = "sup";
    public static final String DITA_TAG_ALT_TEXT = "alt";
    public static final String DITA_TAG_LIST_TAG = "li";
    public static final String EXTERNAL_DTD_PATH = "http://apache.org/xml/features/nonvalidating/load-external-dtd";
    public static final String EXTERNAL_PARAMETER_ENTITIES = "http://xml.org/sax/features/external-parameter-entities";
    public static final String EXTERNAL_GENERAL_ENTITIES = "http://xml.org/sax/features/external-general-entities";

    // BULK TRANSLATION CONSTANTS
    public static final String TRANSLATION_CONFIG_PATH = "/conf/pwc-madison/translation-config";
    public static final String PN_SUBSCRIBER = "subscriber";
    public static final String PN_SYNDICATION_SOURCE = "syndicationSource";
    public static final String PN_FMDITA_CONREF = "fmditaConrefs";

    // Expiration Report
    public static final String PROPERTY_TITLE = "dc:title";
    public static final String PROPERTY_AUTHOR = "jcr:createdBy";

    // Structural changes workflows
    public static final String VERSION_METADATA_NODE = "/jcr:frozenNode/jcr:content";
    public static final String VERSION_FROZEN_NODE_PATH = "/jcr:frozenNode";
    public static final String AEMSITE_PRESETS_NODE = "/jcr:content/metadata/namedoutputs/aemsite";
    public static final String WORKFLOW_PRESETS_NODE = "jcr:content/metadata/namedoutputs/workflowtopicregeneration";
    public static final String VERSION_PARAM = "versionData";
    public static final String REVIEW_DITAMAPS = "reviewDitamaps";
    public static final String PN_IS_PUBLISHING_POINTS = "pwc:isPublishingPoint";
    public static final String PN_LAST_PUBLISHED_PATH = "fmdita-lastPublishedPath";
    public static final String PN_IS_INLINE = "isInlineButton";
    public static final String PATH_TO_GUID = "/var/dxml/versionreferences/pathToGuid";
    public static final String X_PATH_REFERENCES = "/content/fmditacustom/xrefpathreferences";
    public static final String FMGUID = "fmguid";

    // Footnotes constants
    public static final String CALLOUT_TEXT = "callout";
    public static final String FOOTNOTE_TEXT = "text";
    public static final String SHOW = "show";
    public static final String PN_FN_HTML_STRING = "fnHtmlString";
    public static final String PN_OUTPUTCLASS = "outputclass";

    // TOCServlet constants
    public static final String TOC_NODE_PATH = "/jcr:content/toc";
    public static final String REP_POLICY_NODE = "rep:policy";

    public static final String FMDITA_LAST_PUBLISHED = "fmdita-lastPublished";
    public static final String PWC_CONTENT_STATUS = "pwc-content-status";

    public static final String PEER_SCOPE = "peer";
    public static final String DITAMAP_DITA_CLASS = "- map/map";
    public static final String DITA_TOPIC_RESOURCE_TYPE = "fmdita/components/dita/topic";
    public static final String DITA_CONTENT_ROOT = "/content/pwc-madison/ditaroot/";
    public static final String DITA_CONTENT_ROOT_SHORT = "/dt/";
    public static final String GUID_TO_PATH = "/var/dxml/versionreferences/guidToPath";
    public static final String FMDITA_IDS = "fmditaIds";
    public static final String COMPONENT_DITA_RELATED = "pwc-madison/components/ditacontent/related";
    public static final String COMPONENT_DITA_LINKLIST = "fmdita/components/dita/linklist";
    public static final String COMPONENT_DITA_TYPE = "fmdita/components/dita/link";
    public static final String META_HIDE_RELATED_CONTENT = "pwc-hideRelatedContent";
    public static final String JOINED = "-joined";
    public static final String META_PWC_HIDDEN_FROM_SITE_SEARCH = "pwc-hiddenFromSiteSearch";
    
    // See also section Constants
    public static final String META_PWC_SEE_ALSO_ENABLED = "pwc-seeAlsoSectionEnabled";
    public static final String META_PWC_SEE_ALSO_MAX_DISPLAY_COUNT = "pwc-seeAlsoMaxDisplayCount";
    public static final String META_PWC_SEE_ALSO_OVERRIDE_SEE_ALSO = "pwc-overrideSeeAlso";
    public static final String META_PWC_SEE_ALSO_USED_IN = "pwc-usedIn";
    public static final String META_PWC_SEE_ALSO_DEFAULT_USED_IN_REFS = "pwc-defaultUsedInReferences";
    public static final String META_PWC_SEE_ALSO_CONTENT_TYPE = "pwc-seeAlso-contentType";
    public static final String META_PWC_SEE_ALSO_TOPIC_CONTENT_TYPE = "pwc-seeAlso-topic-contentType";

}
