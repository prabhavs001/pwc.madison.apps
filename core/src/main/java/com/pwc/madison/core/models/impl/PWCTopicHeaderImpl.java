package com.pwc.madison.core.models.impl;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.PostConstruct;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.ScriptVariable;
import org.apache.sling.models.annotations.injectorspecific.SlingObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.day.cq.wcm.api.Page;
import com.pwc.madison.core.constants.DITAConstants;
import com.pwc.madison.core.models.PWCTopicHeader;

@Model(
    adaptables = SlingHttpServletRequest.class,
    adapters = { PWCTopicHeader.class },
    resourceType = PWCTopicHeaderImpl.RESOURCE_TYPE)
public class PWCTopicHeaderImpl implements PWCTopicHeader {

    public static final String RESOURCE_TYPE = "pwc-madison/components/ditacontent/pwctopicheader";

    public static final Logger LOGGER = LoggerFactory.getLogger(PWCTopicHeaderImpl.class);

    @SlingObject
    private Resource resource;

    @ScriptVariable
    private Page currentPage;

    @SlingObject
    private ResourceResolver resourceResolver;

    String topicHeader = StringUtils.EMPTY;
    String topicNumberHeader = StringUtils.EMPTY;
    Boolean showWrapper = false;

    String topicType = StringUtils.EMPTY;
    List<String> faqQuestionResourceList = new ArrayList<>();

    @PostConstruct
    private void initModel() {

        // Read the 'pageTitle' property from the current page
        topicHeader = currentPage.getProperties().get(DITAConstants.PN_PAGE_TITLE, String.class);

        // read the ID attribute of the topic node from the parent node considering the following 'title' node
        // '/content/output/sites/bankruptcies_and_liq_US_ditamap/page_2/32-example/jcr:content/root/container/maincontainer/readerrow/docreader/topicnode/pwc-example/title'
        // the ID attribute of the topic node is stored in the parent node which is 'pwc-example', read that value and
        // return as the numbered header
        final Resource topicResource = getTopicResource(resource);
        // parentResource is the 'pwc-example' resource, read the properties
        if (topicResource != null) {
            final ValueMap topicResourceProperties = topicResource.getValueMap();
            // read the property "class" , and cross check it's value contains topic , for e.g. topic pwc-example
            final String className = topicResourceProperties.get(DITAConstants.PN_CLASS_NAME, String.class);

            if (StringUtils.isNotBlank(className) && className.contains(DITAConstants.TOPIC_EXAMPLE_CLASS_NAME)) {
                topicType = DITAConstants.TOPIC_EXAMPLE_CLASS_NAME;
                final String numberedHeader = topicResourceProperties.get(DITAConstants.PN_ID, String.class);
                // By default numbered header looks like 'Example_3-1' , make it display friendly by removing the
                // underscore do the string operations only for the Examples
                if (StringUtils.isNotBlank(numberedHeader)
                        && numberedHeader.startsWith(DITAConstants.STATIC_STR_EXAMPLE)) {
                    topicNumberHeader = numberedHeader.replaceFirst("_", " ");
                }
            } else if (StringUtils.isNotBlank(className) && className.contains(DITAConstants.TOPIC_FAQ_CLASS_NAME)) {
                // set the FAQ document title as the h3 tag as part MD-4347 story

                topicType = DITAConstants.TOPIC_FAQ_CLASS_NAME;
                final Iterable<Resource> questionResList = resource.getChildren();
                for (final Resource quesResource : questionResList) {
                    faqQuestionResourceList.add(quesResource.getPath());
                }

            }
        }

    }

    private Resource getTopicResource(final Resource currentResource) {

        if (currentResource == null) {
            return null;
        }

        // keep traversing the parent , until finding the resource with class 'topic', for e.g. 'topic pwc-example'
        final ValueMap parentProperties = currentResource.getValueMap();

        final String className = parentProperties.get(DITAConstants.PN_CLASS_NAME, String.class);

        if (StringUtils.isNotBlank(className) && className.contains(DITAConstants.TOPIC_CLASS_NAME)) {
            return currentResource;
        } else {
            return getTopicResource(currentResource.getParent());
        }

    }

    @Override
    public List<String> getFaqQuestionResourceList() {
        return faqQuestionResourceList;
    }

    @Override
    public String getNumberedHeader() {
        return topicNumberHeader;
    }

    @Override
    public String getTopicHeader() {
        return topicHeader;
    }

    @Override
    public Boolean showWrapper() {
        return showWrapper;
    }

    @Override
    public String getTopicType() {
        return topicType;
    }

}
