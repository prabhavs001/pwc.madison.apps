package com.pwc.madison.core.servlets;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import javax.servlet.Servlet;
import javax.servlet.ServletException;

import org.apache.commons.lang.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.request.RequestParameterMap;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.servlets.HttpConstants;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.json.JSONException;
import org.json.JSONObject;
import org.osgi.framework.Constants;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.day.cq.commons.jcr.JcrConstants;
import com.day.cq.dam.api.Asset;
import com.day.cq.dam.api.DamConstants;
import com.pwc.madison.core.constants.DITAConstants;
import com.pwc.madison.core.util.DITAUtils;

/**
 * Servlet to fetch all the topics under a ditamap recursively to be shown in the workflow form
 */
@Component(service = Servlet.class,
           property = { Constants.SERVICE_DESCRIPTION + "=Servlet to fetch all the topics referred in a map",
                   "sling.servlet.methods=" + HttpConstants.METHOD_GET, "sling.servlet.paths=/bin/pwc/fetchtopics" },
           configurationPolicy = ConfigurationPolicy.REQUIRE)
public class FetchTopicsServlet extends SlingSafeMethodsServlet {

    private static final Logger LOGGER = LoggerFactory.getLogger(FetchTopicsServlet.class);

    @Override
    protected void doGet(final SlingHttpServletRequest request, final SlingHttpServletResponse response)
            throws ServletException, IOException {
        RequestParameterMap requestParameterMap = request.getRequestParameterMap();
        ResourceResolver requestResolver = request.getResourceResolver();
        List<JSONObject> topicsList = Collections.EMPTY_LIST;

        String ditamapPath = requestParameterMap.containsKey(DITAConstants.DITAMAP)?requestParameterMap.getValue(DITAConstants.DITAMAP).getString():StringUtils.EMPTY;
        Resource ditaMapMetaRes = requestResolver.getResource(ditamapPath);
        if (null != ditaMapMetaRes) {
            topicsList = new ArrayList<JSONObject>();
            List<Asset> topicRefs = DITAUtils.getTopicRefs(ditamapPath, requestResolver, null);
            if (topicRefs != null && topicRefs.size() > 0) {
                // check the document status for each topic
                final Iterator<Asset> topicIter = topicRefs.iterator();
                try {
                    while (topicIter.hasNext()) {
                        final Asset topic = topicIter.next();
                        Boolean reviewStatus = true;
                        JSONObject topicObject = new JSONObject();
                        topicObject.put(DITAConstants.TITLE, topic.getName());
                        topicObject.put(DITAConstants.PATH, topic.getPath());

                        // get the resource path for the Asset's metadata node , for e.g
                        // /content/dam/pwc-madison/ditaroot/us/en/pwc/SAMPLE_bankruptcies_and_liq/SAMPLE_bankruptcies_and_liq/chapter_3_accounting/31-at-a-glance.dita/jcr:content/metadata

                        final Resource ditaTopicMetadata = requestResolver.getResource(
                                topic.getPath() + DITAConstants.FORWARD_SLASH + JcrConstants.JCR_CONTENT + DITAConstants.FORWARD_SLASH
                                        + DamConstants.ACTIVITY_TYPE_METADATA);
                        final Resource ditaTopicJcrRes = requestResolver
                                .getResource(topic.getPath() + DITAConstants.FORWARD_SLASH + JcrConstants.JCR_CONTENT);
                        Long status = 0L;
                        ValueMap metadataMap = null;
                        if (ditaTopicMetadata.getValueMap() != null) {
                            metadataMap = ditaTopicMetadata.getValueMap();
                        }
                        if (null != ditaTopicJcrRes) {
                            ValueMap ditaTopicJcrProps = ditaTopicJcrRes.getValueMap();
                            status = ditaTopicJcrProps.containsKey(DITAConstants.STATUS_PROP_NAME)?ditaTopicJcrProps.get(DITAConstants.STATUS_PROP_NAME, Long.class):0L;
                        }
                        // check the metadata property called 'docstate' for the DITA
                        // topic asset
                        if (ditaTopicMetadata != null && metadataMap != null) {
                            final String documentState = metadataMap
                                    .get(DITAConstants.PN_METADATA_DOCSTATE, String.class);
                            // Assuming the document is in review if the doc status is not "draft" or "Published".
                            if (org.apache.commons.lang3.StringUtils.isNotEmpty(documentState) && (
                                    (documentState.equals(DITAConstants.DITA_DOCUMENTSTATE_DRAFT) || documentState
                                            .equals(DITAConstants.DITA_DOCUMENTSTATE_DONE) || documentState
                                            .equals(DITAConstants.DITA_DOCUMENTSTATE_UNPUBLISHED)) && (status == 0L))) {
                                reviewStatus = false;
                            }
                        }
                        topicObject.put(DITAConstants.TOPIC_REVIEW_STATUS, reviewStatus);
                        topicsList.add(topicObject);
                    }
                } catch (JSONException e) {
                    LOGGER.error("JSONException in FetchTopicsServlet {}", e);
                }
            }
        }
        response.getWriter().print(topicsList);
    }
}
