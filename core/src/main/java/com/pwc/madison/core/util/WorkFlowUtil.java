package com.pwc.madison.core.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.jackrabbit.JcrConstants;
import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.jackrabbit.api.security.user.Query;
import org.apache.jackrabbit.api.security.user.QueryBuilder;
import org.apache.jackrabbit.api.security.user.UserManager;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adobe.granite.workflow.WorkflowSession;
import com.adobe.granite.workflow.exec.WorkItem;
import com.adobe.granite.workflow.exec.WorkflowData;
import com.adobe.granite.workflow.metadata.MetaDataMap;
import com.day.cq.commons.Externalizer;
import com.pwc.madison.core.constants.DITAConstants;
import com.pwc.madison.core.constants.MadisonConstants;
import com.pwc.madison.core.models.EmailProfile;
import com.pwc.madison.core.services.MailSenderService;

public final class WorkFlowUtil {

    private WorkFlowUtil() {
    }

    private static final Logger LOGGER = LoggerFactory.getLogger(WorkFlowUtil.class);
    public static final String NOTIFY_WF_USER_MAIL_TEMPLATE_PATH = "/etc/notification/email/madison/notify-wf-user.html/jcr:content";
    private static String PROFILE_EMAIL = "./profile/email";
    private static String PROFILE_FIRST_NAME = "./profile/givenName";
    private static String PROFILE_LAST_NAME = "./profile/familyName";
    private static String TITLE = "title";
    private static String AEM_INBOX = "/aem/inbox";
    private static String DITA_URL = "ditaUrl";
    private static String FIRST_NAME = "firstName";
    private static String LAST_NAME = "lastName";
    private static String BODY_TITLE = "bodyTitle";
    private static String TASK_ITEMS = "tasks";
    private static String NO_REPLY_EMAIL_ID = "noreply@adobe.com";

    public static List<Authorizable> getAuthorizedUsers(UserManager userManager, String[] recipients)
            throws RepositoryException {

        List<Authorizable> authorizers = new ArrayList<>();
        if (userManager == null || recipients.length < 1) {
            return authorizers;
        }
        for (String recipient : recipients) {
            Authorizable auth = userManager.getAuthorizable(recipient);
            if (null != auth) {
                if (auth.isGroup()) {
                    final String author = recipient;
                    Iterator<Authorizable> members = userManager.findAuthorizables(new Query() {
                        @Override
                        public <T> void build(QueryBuilder<T> builder) {
                            builder.setScope(author, false);
                        }
                    });
                    while (members.hasNext()) {
                        authorizers.add(members.next());
                    }
                } else {
                    authorizers.add(auth);
                }
            }
        }
        return authorizers;
    }

    public static List<EmailProfile> getRecipientEmailIDs(List<Authorizable> authorizers) throws RepositoryException {

        List<EmailProfile> emailusers = new ArrayList<>();
        if (authorizers == null) {
            return emailusers;
        }

        Iterator<Authorizable> result = authorizers.listIterator();
        while (result.hasNext()) {
            Authorizable user = result.next();
            // Set the mail values
            if (user.hasProperty(PROFILE_EMAIL)) {
                Value[] emailIds = user.getProperty(PROFILE_EMAIL);
                String firstName = user.getProperty(PROFILE_FIRST_NAME) != null
                        ? user.getProperty(PROFILE_FIRST_NAME)[0].getString()
                        : StringUtils.EMPTY;
                String lastName = user.getProperty(PROFILE_LAST_NAME) != null
                        ? user.getProperty(PROFILE_LAST_NAME)[0].getString()
                        : StringUtils.EMPTY;
                String recipientz = emailIds[0] != null ? emailIds[0].toString() : StringUtils.EMPTY;

                if (!recipientz.isEmpty()) {
                    emailusers.add(new EmailProfile(firstName, lastName, recipientz));
                    LOGGER.debug("E-mail of the Recipient :::: " + firstName + ", " + lastName + " : " + recipientz);
                }
            }
        }
        return emailusers;
    }

    public static void sendEmailNotification(MailSenderService mailSenderService, ResourceResolverFactory factory,
            String[] participants, String wfStatus, String[] orgTopicsList, String bodyTitle, String fmditaUsername) {

        if (mailSenderService == null || factory == null || participants.length < 1 || wfStatus == null) {
            return;
        }
        ResourceResolver resolver = null;
        try {
            Map<String, Object> serviceUserParams = Collections.unmodifiableMap(new HashMap<String, Object>() {
                private static final long serialVersionUID = 1L;

                {
                    put(ResourceResolverFactory.SUBSERVICE, fmditaUsername);
                }
            });
            resolver = factory.getServiceResourceResolver(serviceUserParams);
            UserManager userManager = resolver.adaptTo(UserManager.class);
            List<Authorizable> authUsers = getAuthorizedUsers(userManager, participants);
            List<EmailProfile> emailRecipients = getRecipientEmailIDs(authUsers);
            sendEmail(mailSenderService, resolver, emailRecipients, wfStatus, orgTopicsList, bodyTitle);
        } catch (Exception e) {
            LOGGER.error("Failed to send email to reviewers ", e);
        } finally {
            if (resolver != null)
                resolver.close();
        }
    }

    public static void sendEmail(MailSenderService mailSenderService, ResourceResolver resourceResolver,
            List<EmailProfile> recipients, String wfStatus, String[] orgTopicsList, String bodyTitle) {

        if (resourceResolver == null || recipients.isEmpty()) {
            LOGGER.debug("No recipients found for sending an e-mail");
            return;
        }
        try {
            Externalizer externalizer = resourceResolver.adaptTo(Externalizer.class);
            String page = externalizer.authorLink(resourceResolver, AEM_INBOX);
            String tasks = listHtmlTaskItems(resourceResolver, externalizer, orgTopicsList);
            String item = orgTopicsList[0];
            String ditaName = item.substring(item.lastIndexOf('/') + 1, item.length());
            for (EmailProfile recipient : recipients) {
                final Map<String, String> emailParams = new HashMap<>();
                emailParams.put(TITLE, wfStatus);
                emailParams.put(FIRST_NAME, recipient.getFirstName());
                emailParams.put(LAST_NAME, recipient.getLastName());
                emailParams.put(BODY_TITLE, bodyTitle);
                emailParams.put(DITA_URL, page);
                emailParams.put(TASK_ITEMS, tasks);
                mailSenderService.sendMailWithEmailTemplate(NO_REPLY_EMAIL_ID, new String[] { recipient.getEmailId() },
                        wfStatus + " " + ditaName, WorkFlowUtil.NOTIFY_WF_USER_MAIL_TEMPLATE_PATH, emailParams);
            }

        } catch (Exception e) {
            LOGGER.error("PwC Workflow Notification Email failed to send", e);
        }
    }

    private static String listHtmlTaskItems(ResourceResolver resourceResolver, Externalizer externalizer,
            String[] orgTopicsList) {
        StringBuilder htmlSnippet = new StringBuilder("");

        if (null == orgTopicsList) {
            return htmlSnippet.toString();
        }

        for (int i = 0; i < orgTopicsList.length; i++) {
            String item = orgTopicsList[i];
            if (item != null) {
                String taskLink = externalizer.authorLink(resourceResolver, "/assetdetails.html" + item);
                String ditaName = item.substring(item.lastIndexOf('/') + 1, item.length());
                htmlSnippet.append("<li><a href='").append(taskLink).append("'>").append(ditaName).append("</li>");
            }
        }
        return htmlSnippet.toString();
    }

    /**
     * To get the unique id for naming the dynamically created groups in full cycle and simple review workflows eg:
     * 2019-02-06_2_pwc-fullcyle-workflow_431
     * 
     * @param workflowId
     * @return
     */
    public static String getUniqueWorkId(String workflowId) {
        if (null != workflowId && !workflowId.isEmpty()) {
            String[] workIdArr = workflowId.split("/");
            if (workIdArr.length > 1) {
                workflowId = workIdArr[workIdArr.length - 2] + "_" + workIdArr[workIdArr.length - 1];
            } else if (workIdArr.length > 0) {
                workflowId = workIdArr[workIdArr.length - 1];
            }
        }
        return workflowId;
    }

    /***
     * Method to update the Workflow comment, this can be used to set the default comment for the system process step
     * 
     * @param wfData
     * @param workflowSession
     * @param workflowSystemUserComment
     * @param workItem
     */
    public static void updateWorkflowComment(WorkflowData wfData, WorkflowSession workflowSession,
            String workflowSystemUserComment, WorkItem workItem) {
        if (wfData == null || workflowSession == null || workItem == null
                || StringUtils.isEmpty(workflowSystemUserComment)) {
            return;
        }
        // update the workflow comment, this could also be used as a fix when the user comments are repeated by the
        // system process steps
        wfData.getMetaDataMap().put(MadisonConstants.WORKFLOW_DATA_PROPERTY_COMMENT, workflowSystemUserComment);
        workflowSession.updateWorkflowData(workItem.getWorkflow(), wfData);
    }

    public static String[] getReviewItems(MetaDataMap meta) {
        String orgTopics = meta.get(DITAConstants.ORIGINAL_TOPICS, String.class);
        String[] orgTopicsList = ArrayUtils.EMPTY_STRING_ARRAY, reviewMapsList = ArrayUtils.EMPTY_STRING_ARRAY;
        if (null != orgTopics) {
            orgTopicsList = orgTopics.split("\\|");
        }
        String reviewMaps = meta.get(DITAConstants.REVIEW_DITAMAPS, String.class);
        if (null != reviewMaps) {
            reviewMapsList = reviewMaps.split("\\|");
        }
        int topicArrayLength = orgTopicsList.length;
        int mapArrayLength = reviewMapsList.length;
        String[] combinedReviewList = ArrayUtils.EMPTY_STRING_ARRAY;
        if (topicArrayLength <= 0 && mapArrayLength > 0) {
            combinedReviewList = reviewMapsList;
        } else if (topicArrayLength > 0 && mapArrayLength <= 0) {
            combinedReviewList = orgTopicsList;
        } else if (topicArrayLength > 0 && mapArrayLength > 0) {
            return Stream.concat(Arrays.stream(orgTopicsList).filter(x -> !x.isEmpty()),
                    Arrays.stream(reviewMapsList).filter(x -> !x.isEmpty())).toArray(String[]::new);
        }
        return combinedReviewList;
    }

    /**
     * Sets the correct status on ditamaps/ topics related to an ongoing/completed workflow
     * 
     * @param path
     * @param session
     * @param type
     */
    public static void setStatus(String path, Session session, String type) {
        type = (null != type && !type.isEmpty()) ? type : StringUtils.EMPTY;
        try {
            if (null != path && null != session) {
                Node topicJcrNode = session.getNode(path + DITAConstants.FORWARD_SLASH + JcrConstants.JCR_CONTENT);
                if (null != topicJcrNode) {
                    if (topicJcrNode.hasProperty(DITAConstants.STATUS_PROP_NAME)) {
                        topicJcrNode.getProperty(DITAConstants.STATUS_PROP_NAME).remove();
                    }
                    if (type.equals(DITAConstants.REV_IN_PROGRESS)) {
                        topicJcrNode.setProperty(DITAConstants.STATUS_PROP_NAME, DITAConstants.IN_PROGRESS);
                    } else if (type.equals(DITAConstants.COLLAB_IN_PROGRESS)) {
                        topicJcrNode.setProperty(DITAConstants.STATUS_PROP_NAME, DITAConstants.COLLAB_PROGRESS);
                    }
                    session.save();
                }
            }
        } catch (RepositoryException e) {
            LOGGER.error("RepositoryException in setStatus method {}", e);
        }
    }
}
