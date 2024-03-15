package com.pwc.madison.core.models.impl;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.apache.commons.httpclient.URIException;
import org.apache.commons.httpclient.util.URIUtil;
import org.apache.commons.lang.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.ScriptVariable;
import org.apache.sling.models.factory.ModelFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.day.cq.wcm.api.Page;
import com.pwc.madison.core.constants.DITAConstants;
import com.pwc.madison.core.constants.MadisonConstants;
import com.pwc.madison.core.models.Sme;
import com.pwc.madison.core.models.SmeList;

@Model(adaptables = SlingHttpServletRequest.class, adapters = Sme.class)
public class SmeImpl implements Sme {

    private static final Logger LOGGER = LoggerFactory.getLogger(SmeListImpl.class);

    @Inject
    private String subjectText;

    private String encodedSubjectText = StringUtils.EMPTY;

    @ScriptVariable
    private Page currentPage;
    
    @Inject
    SlingHttpServletRequest request;
    

    @Inject
    private ModelFactory modelFactory;
    
    SmeList smeList;

    private static final String RIGHT_RAIL_PATH_ACCORDION = DITAConstants.RIGHT_RAIL_NODE_PATH_V1
            + MadisonConstants.FORWARD_SLASH + "responsivegrid/accordion_container";

    /**
     * Init Method of Model.
     */
    @PostConstruct
    protected void init() {
        final Resource pageResource = currentPage.adaptTo(Resource.class);
        final Resource accordionResource = pageResource.getChild(RIGHT_RAIL_PATH_ACCORDION);
        if (accordionResource == null) {
            return;
        }
        Resource smeListResource = getSmeResourceTypeResource(accordionResource);
        if (smeListResource != null) {
            smeList = modelFactory.getModelFromWrappedRequest(request, smeListResource, SmeList.class);
        }
    }

    /**
     * Return {@link Resource} of SME component under dita page. It traverses until it finds the resource otherwise it
     * returns null.
     * 
     * @param currentResource
     *            {@link Resource}
     * @return {@link Resource}
     */
    private Resource getSmeResourceTypeResource(final Resource currentResource) {
        if (currentResource.getResourceType().equals(SmeListImpl.RESOURCE_TYPE)) {
            return currentResource;
        } else {
            for (final Resource childResource : currentResource.getChildren()) {
                Resource smeResource = getSmeResourceTypeResource(childResource);
                if (null != smeResource) {
                    return smeResource;
                }
            }
        }
        return null;
    }

    @Override
    public SmeList getSmeList() {
        return smeList;
    }
    
    @Override
    public String getEncodedSubjectText() {
        if (StringUtils.isNotBlank(subjectText)) {
            try {
                encodedSubjectText = URIUtil.encodeAll(subjectText, MadisonConstants.UTF_8);
            } catch (URIException e) {
                LOGGER.error("Error encoding mail subject - ", e);
            }
        }
        return encodedSubjectText;
    }

}
