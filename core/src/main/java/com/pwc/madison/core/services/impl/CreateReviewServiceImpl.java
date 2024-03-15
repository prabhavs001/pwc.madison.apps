package com.pwc.madison.core.services.impl;

import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.jackrabbit.JcrConstants;
import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.jackrabbit.api.security.user.Query;
import org.apache.jackrabbit.api.security.user.QueryBuilder;
import org.apache.jackrabbit.api.security.user.UserManager;
import org.apache.jackrabbit.util.Text;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.jcr.api.SlingRepository;
import org.json.JSONArray;
import org.json.JSONObject;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adobe.granite.security.user.UserPropertiesManager;
import com.adobe.granite.workflow.WorkflowSession;
import com.adobe.granite.workflow.exec.WorkItem;
import com.adobe.granite.workflow.exec.Workflow;
import com.adobe.granite.workflow.exec.WorkflowData;
import com.adobe.granite.workflow.metadata.MetaDataMap;
import com.day.cq.workflow.WorkflowService;
import com.pwc.madison.core.constants.DITAConstants;
import com.pwc.madison.core.constants.MadisonConstants;
import com.pwc.madison.core.services.CreateReviewService;
import com.pwc.madison.core.services.MadisonSystemUserNameProviderService;
import com.pwc.madison.core.util.BTree;
import com.pwc.madison.core.util.DITAUtils;
import com.pwc.madison.core.util.MadisonUtil;
import com.pwc.madison.core.util.MultiLevelTree;
import com.pwc.madison.core.util.NodeHolder;
import com.pwc.madison.core.util.State;

@Component(service = { CreateReviewService.class },
           immediate = true)
public class CreateReviewServiceImpl implements CreateReviewService {

    private static final String REVIEW_WF_PROP_NAME = DITAConstants.REVIEW_WF_PROP_NAME;
    private static final String DATA_METADATA = "/data/metaData";

    private static final String[] PROPERTY_LIST = new String[]{DITAConstants.ABSOLUTE_TIME, DITAConstants.START_TIME, DITAConstants.ASSIGNEE, DITAConstants.DEADLINE,
            DITAConstants.DESCRIPTION, DITAConstants.INITIATOR, DITAConstants.IS_DITAMAP, DITAConstants.NOTIFY_EMAIL, DITAConstants.OPERATION,
            DITAConstants.ORIGINAL_TOPICS, DITAConstants.INPUT_PAYLOAD, DITAConstants.PROJECT_PATH, DITAConstants.START_TIME, DITAConstants.STATUS_PROP_NAME,
            DITAConstants.TITLE, DITAConstants.PROP_VERSION_JSON, DITAConstants.DITAMAP
    };

    private static final long IN_REVIEW = 1;
    public static final String NEW_REVIEW_UI_VER_VALUE = "2.0";

    protected final Logger log = LoggerFactory.getLogger(getClass());

    protected final Logger perfLogger = LoggerFactory.getLogger(DITAConstants.PERFORMANCE_LOGGER);

    @Reference
    protected SlingRepository repository;

    @Reference
    private WorkflowService workflowService;
    @Reference
    private ResourceResolverFactory resolverFactory;
    @Reference
    private MadisonSystemUserNameProviderService madisonSystemUserNameProviderService;

    private static Value[] getValueArr(final Property prop) throws RepositoryException {
        if (prop.isMultiple())
            return prop.getValues();
        final Value[] values = { prop.getValue() };
        return values;
    }

    /**
     * @param item
     * @param wfsession
     * @param meta
     * @param isReview  :- this flag checks whether it is full-cycle workflow and the doc state to be set to In_Review
     */
    @Override
    public void createReview(final WorkItem item, final WorkflowSession wfsession, final MetaDataMap meta,
            final Boolean isReview) {

        ResourceResolver resolver = null;
        String wfProgressType = DITAConstants.REV_IN_PROGRESS;
        try {
            resolver = MadisonUtil.getResourceResolver(resolverFactory, madisonSystemUserNameProviderService.getFmditaServiceUsername());
            final Session session = resolver.adaptTo(Session.class);
            final UserManager userManager = resolver.adaptTo(UserManager.class);
            String newReviewId = this.getNewReviewId();
            State state = new State(session);
            NodeHolder reviewNode = createReviewNode(state, newReviewId);
            prepareReviewMetadata(item, meta, session, isReview, reviewNode, newReviewId, resolver);
            final Node usersNode = reviewNode.getJcrNode().getNode(MadisonConstants.PN_USERS);
            String reviewGroup = null,approverGroup = null, publisherGroup = null, collabGroup = null;
            if(meta.containsKey(DITAConstants.ASSIGNEE)){
                reviewGroup = (meta.get(DITAConstants.ASSIGNEE, String.class) != null) ? meta.get(DITAConstants.ASSIGNEE, String.class).split(",")[0] : StringUtils.EMPTY;
            }
            if(meta.containsKey(DITAConstants.APPROVER)) {
                approverGroup = meta.get(DITAConstants.APPROVER, String.class);
            }
            if(meta.containsKey(DITAConstants.PUBLISHER)) {
                publisherGroup = meta.get(DITAConstants.PUBLISHER, String.class);
            }
            if(meta.containsKey(DITAConstants.COLLABORATORS)) {
                collabGroup = (meta.get(DITAConstants.COLLABORATORS, String.class) != null) ?  meta.get(DITAConstants.COLLABORATORS, String.class).split(",")[0] : StringUtils.EMPTY;
            }
            final String[] reviewList = reviewGroup != null ? reviewGroup.split(DITAConstants.USER_SEPARATOR) :
                    ArrayUtils.EMPTY_STRING_ARRAY;
            final String[] approverList = approverGroup != null ? approverGroup.split(DITAConstants.USER_SEPARATOR) : ArrayUtils.EMPTY_STRING_ARRAY;
            final String[] publisherList = publisherGroup != null ? publisherGroup.split(DITAConstants.USER_SEPARATOR) : ArrayUtils.EMPTY_STRING_ARRAY;
            final String[] collabList = collabGroup != null ? collabGroup.split(DITAConstants.USER_SEPARATOR) : ArrayUtils.EMPTY_STRING_ARRAY;
            final List<Authorizable> authorizers = new ArrayList<Authorizable>();
            if(!isReview && collabList.length>0){ //checks whether this method is called from collaboration workflow
                wfProgressType = DITAConstants.COLLAB_IN_PROGRESS;
                populateAuthorizers(authorizers, collabList, userManager);
            }else{
                if (isReview) {
                    populateAuthorizers(authorizers, reviewList, userManager);
                }
                populateAuthorizers(authorizers, approverList, userManager);
                populateAuthorizers(authorizers, publisherList, userManager);
            }

            //Add Initiator
            final Authorizable authReviewInitiator = userManager
                    .getAuthorizable(meta.get(DITAConstants.INITIATOR, String.class));
            authorizers.add(authReviewInitiator);
            for (final Authorizable user : authorizers) {
                MadisonUtil.addUserMetadata(user, resolver.adaptTo(UserPropertiesManager.class), usersNode);
            }

            final Iterator<Authorizable> result = authorizers.listIterator();
            while (result.hasNext()) {
                final Authorizable user = result.next();
                MadisonUtil.assignPermissiontoUser(user, item.getWorkflow().getId(), resolver, true);
            }

            // Setting doc state to in-review only for full cycle review workflow
            String docState = DITAConstants.DITA_DOCUMENTSTATE_DRAFT;
            if (isReview) {
                docState = DITAConstants.DITA_DOCUMENTSTATE_INREVIEW;
            }
            boolean isFullSiteGeneration = false;
            String strChangesDitamaps = meta.get(DITAConstants.REVIEW_DITAMAPS,String.class);
            if(null!=strChangesDitamaps && !strChangesDitamaps.isEmpty()){
                String[] ditamapArray = strChangesDitamaps.split("|");
                if(ditamapArray.length>0){
                    isFullSiteGeneration = true;
                }
            }
            String topics = meta.get(DITAConstants.SELECTED_TOPICS, String.class);;
            if(isFullSiteGeneration){
                topics = meta.get(DITAConstants.ORIGINAL_TOPICS, String.class);
            }
            final String[] topicsList = topics.split("\\|");
            DITAUtils.setDocStates(topicsList, docState, session, true, isReview, wfProgressType);

            final Boolean isDitamap = meta.get(DITAConstants.IS_DITAMAP, Boolean.class);
            if (isDitamap != null && isDitamap) {
                final String ditamap = meta.get(DITAConstants.DITAMAP, String.class);
                DITAUtils.setDocStates(new String[] { ditamap }, docState, session, true, isReview, wfProgressType);
            }

            //performance logging
            final Long startTime = meta.get(DITAConstants.START_TIME, Long.class);
            if (startTime != null) {
                final long elapsedTime = System.currentTimeMillis() - startTime;
                perfLogger.info("CREATE_REVIEW : {} ms", elapsedTime);
            }

            final Workflow workflow = item.getWorkflow();
            session.refresh(true);
            wfsession.updateWorkflowData(workflow, workflow.getWorkflowData());
            session.save();
            resolver.close();

        } catch (final Exception e) {
            log.error("Failed to create review ", e);
        } finally {
            if (resolver != null)
                resolver.close();
        }
    }

    private void prepareReviewMetadata(final WorkItem item, final MetaDataMap meta, final Session session,
            final Boolean isReview, NodeHolder reviewNode, String newReviewId, ResourceResolver resolver) {

        try {
            final String wfID = item.getWorkflow().getId();
            reviewNode.getJcrNode().setProperty(DITAConstants.ALLOW_ALL_REVIEWERS, true);
            reviewNode.getJcrNode().setProperty(DITAConstants.REVIEW_VERSION, meta.get(DITAConstants.REVIEW_VERSION, String.class));
            reviewNode.getJcrNode().setProperty(DITAConstants.DITAMAP_HIERARCHY, meta.get(DITAConstants.DITAMAP_HIERARCHY, String.class));
            reviewNode.getJcrNode().setProperty(DITAConstants.PROP_WORKFLOW_ID, wfID);
            log.debug("Successfully review node: {} created", reviewNode.getJcrNode().getPath());
            if(null == reviewNode){
                return;
            }
            final Node metaNode = session.getNode(wfID).getNode(DITAConstants.WF_METADATA);
            reviewNode.addNode(MadisonConstants.PN_USERS);
            final Object old_wfid = meta.get(MadisonConstants.OLD_WFID);
            Node eventsNode = null;
            
            if (null == old_wfid)
                eventsNode = reviewNode.getJcrNode().addNode(MadisonConstants.EVENTS);
            else {
                final Node old_metaNode = session.getNode(old_wfid.toString()).getNode(DITAConstants.WF_METADATA);
                session.getWorkspace().copy(old_metaNode.getNode(MadisonConstants.EVENTS).getPath(),
                        reviewNode.getPath() + MadisonConstants.FORWARD_SLASH + MadisonConstants.EVENTS);
                eventsNode = reviewNode.getJcrNode().getNode(MadisonConstants.EVENTS);
            }

            if (isReview) {
                //Convert deadline datetime into seconds and save for timeout.
                if(meta.containsKey(DITAConstants.DEADLINE)) {
                    final String reviewDeadline = meta.get(DITAConstants.DEADLINE, String.class);
                    final Date deadline = MadisonUtil.FormatDeadline(reviewDeadline);
                    meta.put(DITAConstants.ABSOLUTE_TIME, deadline.getTime());
                }
            }
            String reviewId = reviewNode.getString(DITAConstants.REVIEW_ID);
            String reviewPage = DITAConstants.NEW_REVIEW_UI_PAGE + reviewId + DITAConstants.WCMMODE_DISABLED;
            final WorkflowData wfData = item.getWorkflowData();
            final MetaDataMap wfMetaData = wfData.getMetaDataMap();
            wfMetaData.put(MadisonConstants.REVIEW_PAGE, reviewPage);
            wfMetaData.put(DITAConstants.REVIEW_ID, reviewId);


            //Patch Topics with Workflow Id
            final Object orgNode = meta.get(DITAConstants.ORIGINAL_TOPICS);
            if (orgNode != null) {
                final String[] orgTopics = orgNode.toString().split("\\" + "|");
                for (int i = 0; i < orgTopics.length; i++) {
                    if(!orgTopics[i].isEmpty()) {
                        final String str_i = Integer.toString(i);
                        if (old_wfid == null) {
                            final Node addedNode = eventsNode.addNode(str_i);
                            addedNode.setProperty(MadisonConstants.EVENT_COUNT, 0);
                            addedNode.setProperty(MadisonConstants.TOPIC_PATH, orgTopics[i]);
                        }
                        updateTopicJcrNode(orgTopics[i], reviewId, session);
                    }
                }
                final Boolean isDitamap = meta.get(DITAConstants.IS_DITAMAP, Boolean.class);

                if (isDitamap != null && isDitamap) {
                    final String ditamap = meta.get(DITAConstants.DITAMAP, String.class);
                    updateTopicJcrNode(ditamap, reviewId, session);
                }
                Node workflowMetadata = resolver.getResource(wfID + DATA_METADATA).adaptTo(Node.class);
                /* copy all required properties from workflow metadata node to review data node */
                copyProperties(workflowMetadata, reviewNode.getJcrNode());
                session.save();
            }
        } catch (final Exception e) {
            log.error("Failed to prepareReviewMetadata ", e);
        }
    }

    /**
     * create review node structure(i.e. /var/dxml/reviews/data/1/1/1/c8eb60fd-acfd-4d71-ad32-cb3c6b01ee0d)
     * @param state
     * @param reviewID
     * @return created node
     */
    public NodeHolder createReviewNode(State state, String reviewID){
        NodeHolder fmditaNode = new NodeHolder(DITAConstants.FMDITA_PATH, state);
        NodeHolder reviewPathNode = fmditaNode.getOrAddNode(DITAConstants.REVIEW_NODE, JcrConstants.NT_UNSTRUCTURED);
        NodeHolder dataNode = reviewPathNode.getOrAddNode(DITAConstants.REVIEW_DATA_NODE, JcrConstants.NT_UNSTRUCTURED);
        MultiLevelTree bReviewTree = new MultiLevelTree(dataNode, DITAConstants.REVIEW_TREE_DEPTH, DITAConstants.REVIEW_TREE_MAX_CHILDREN);
        return bReviewTree.insert(DITAConstants.REVIEW_ID, reviewID);
    }

    public String getNewReviewId(){
        return UUID.randomUUID().toString();
    }

    private void updateTopicJcrNode(final String path, final String wfID, final Session session) throws Exception {
        final BTree btree = new BTree(session.getNode(DITAConstants.REVIEW_STORE_PATHS));
        final String escapedPath = Text.escapeIllegalJcrChars(path);
        final Node btreeNode = btree.getBTreeNodeForKey(escapedPath);
        if (!btreeNode.hasProperty(escapedPath)) {
            btree.addPropertyToNode(btreeNode, escapedPath,
                    session.getValueFactory().createValue(new JSONObject(DITAConstants.EMPTY_JSON_OBJECT).toString()));
        }
        final javax.jcr.Property prop = btree.getProperty(escapedPath);
        final JSONObject obj = new JSONObject(prop.getString());
        if (!obj.has(REVIEW_WF_PROP_NAME)) {
            obj.put(REVIEW_WF_PROP_NAME, new JSONArray(DITAConstants.EMPTY_JSON_ARRAY));
        }
        final JSONArray arr = obj.getJSONArray(REVIEW_WF_PROP_NAME);
        int i = 0;
        for (; i < arr.length(); i++) {
            if (arr.getString(i).equals(wfID)) {
                break;
            }
        }
        if (i == arr.length()) {
            arr.put(wfID);
        }
        prop.setValue(obj.toString());
    }

    private String[] getStringsProperty(final Node node, final String property) {

        String[] strings = new String[0];
        try {
            if (node.hasProperty(property)) {
                final Value[] propvalues = getValueArr(node.getProperty(property));
                final String[] stringvalues = new String[propvalues.length];

                for (int i = 0; i < propvalues.length; i++) {
                    stringvalues[i] = propvalues[i].getString();
                }
                strings = stringvalues;
            }
        } catch (final Exception e) {
            log.error(e.getMessage(), e);
        }
        return strings;
    }

    private void populateAuthorizers(final List<Authorizable> authorizers, final String[] usersList,
            final UserManager userManager) {
        try {
            for (final String user : usersList) {
                Authorizable auth = null;

                auth = userManager.getAuthorizable(user);
                if(null!=auth) {
                    if (auth.isGroup()) {
                        final String rG = user;
                        final Iterator<Authorizable> members = userManager.findAuthorizables(new Query() {
                            @Override
                            public <T> void build(final QueryBuilder<T> builder) {
                                builder.setScope(rG, false);
                            }
                        });
                        while (members.hasNext())
                            authorizers.add(members.next());
                    } else {
                        authorizers.add(auth);
                    }
                }
            }
        } catch (final RepositoryException e) {
            log.error(e.getMessage(), e);
        }
    }

    /**
     * Copy properties from source to destination
     * @param source
     * @param dest
     * @throws RepositoryException
     */
    private void copyProperties(final Node source, final Node dest) throws RepositoryException {
        for (String property : PROPERTY_LIST) {
            /* before copying the property check if the property already exists, and remove it */
            if(dest.hasProperty(property)) {
                dest.setProperty(property, (Value)null);
            }
            if (source.hasProperty(property)){
                if (source.getProperty(property).isMultiple()) {
                    final Value[] values = source.getProperty(property).getValues();
                    dest.setProperty(property, values);
                } else {
                    final Value value = source.getProperty(property).getValue();
                    dest.setProperty(property, value);
                }
            }
        }
    }

}
