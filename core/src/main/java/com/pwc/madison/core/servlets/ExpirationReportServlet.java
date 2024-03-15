package com.pwc.madison.core.servlets;

import com.google.gson.Gson;
import com.pwc.madison.core.constants.DITAConstants;
import com.pwc.madison.core.constants.MadisonConstants;
import org.apache.commons.lang.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.request.RequestParameterMap;
import org.apache.sling.api.resource.*;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.osgi.framework.Constants;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.Servlet;
import java.io.IOException;
import java.util.*;

/**
 * Servlet that sets approval or rejection status.
 */
@Component(service = Servlet.class,
    property = {Constants.SERVICE_DESCRIPTION + "=This servlet is called in Expiration reports page.",
        "sling.servlet.methods=" + "POST", "sling.servlet.paths=" + "/bin/pwc/expirationreport",
        "sling.servlet.extensions=" + "json"})
public class ExpirationReportServlet extends SlingAllMethodsServlet {
    private static final Logger LOG = LoggerFactory.getLogger(ExpirationReportServlet.class);
    private static final long serialVersionUid = 1L;
    Map<String, List<String>> topicsMap = Collections.EMPTY_MAP;

    @Override
    protected void doPost(final SlingHttpServletRequest request, final SlingHttpServletResponse response)
        throws IOException {
        ResourceResolver resourceResolver = request.getResourceResolver();
        RequestParameterMap requestParameterMap = request.getRequestParameterMap();
        String path = StringUtils.EMPTY;
        String approvedRejectedBy = StringUtils.EMPTY;
        String confirmationStatus = StringUtils.EMPTY;
        if (requestParameterMap.containsKey(MadisonConstants.CONFIRMATION_STATUS)) {
            confirmationStatus = requestParameterMap.getValue(MadisonConstants.CONFIRMATION_STATUS).getString();
        }
        if (requestParameterMap.containsKey(MadisonConstants.EXPIRATION_CONTENT_PATH)) {
            path = requestParameterMap.getValue(MadisonConstants.EXPIRATION_CONTENT_PATH).getString();
        }
        if (requestParameterMap.containsKey(MadisonConstants.EXPIRATION_USERID)) {
            approvedRejectedBy = requestParameterMap.getValue(MadisonConstants.EXPIRATION_USERID).getString();
        }
        setConfirmationStatus(confirmationStatus, path, resourceResolver, approvedRejectedBy);
        Gson gson = new Gson();
        gson.toJson(topicsMap, response.getWriter());
    }

    /**
     * sets confirmation on click or reject
     *
     * @param confirmationStatus
     * @param path
     * @param resolver
     */
    private void setConfirmationStatus(String confirmationStatus, String path, ResourceResolver resolver, String approvedRejectedBy) {
        Resource resource = resolver.getResource(path + MadisonConstants.METADATA_PATH);
        if (null != resource) {
            ModifiableValueMap modifiableValueMap = resource.adaptTo(ModifiableValueMap.class);
            if(null != modifiableValueMap){
                modifiableValueMap.put(DITAConstants.META_EXPIRATION_CONFIRMATION_STATUS, confirmationStatus);
                modifiableValueMap.put(DITAConstants.META_APPROVED_REJECTED_DATE, Calendar.getInstance());
                modifiableValueMap.put(DITAConstants.META_EXPIRATION_APPROVED_REJECTED_BY, approvedRejectedBy);
            }
            try {
                resolver.commit();
            } catch (PersistenceException e) {
                LOG.error("Error while adding property - setConfirmationStatus(): ", e);
            }
        }

    }
}
