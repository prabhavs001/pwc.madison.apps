package com.pwc.madison.core.services.impl;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.engine.SlingRequestProcessor;
import org.apache.tika.io.IOUtils;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.day.cq.contentsync.handler.util.RequestResponseFactory;
import com.day.cq.wcm.api.WCMMode;
import com.pwc.madison.core.services.MediaWrapperUpdateService;

/**
 * The Class MediaWrapperUpdateServiceImpl is a service to update the media wrapper component on the page by requesting
 * the page.
 */
@Component(service = { MediaWrapperUpdateService.class }, immediate = true)
public class MediaWrapperUpdateServiceImpl implements MediaWrapperUpdateService {

    protected final Logger log = LoggerFactory.getLogger(this.getClass());

    @Reference
    private RequestResponseFactory requestResponseFactory;

    @Reference
    private SlingRequestProcessor requestProcessor;

    /*
     * (non-Javadoc)
     * 
     * @see com.pwc.madison.core.services.MediaWrapperUpdateService#updateMediaWrapperProperties(javax.jcr.Node,
     * org.apache.sling.api.resource.ResourceResolver) This method calls the page containing the media wrapper
     * component. The sightly code of the component updates the component with scene7 related properties. As per the
     * OOTB code, the page has to be opened once before publishing. Simulating the same here.
     */
    @Override
    public void updateMediaWrapperProperties(Node mediaWrapperPageNode, ResourceResolver resourceResolver) {


        HttpServletRequest request;
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            request = requestResponseFactory.createRequest("GET", mediaWrapperPageNode.getPath() + ".html");
            WCMMode.PREVIEW.toRequest(request);
            HttpServletResponse resp = requestResponseFactory.createResponse(out);
            requestProcessor.processRequest(request, resp, resourceResolver);
        } catch (RepositoryException | ServletException | IOException e) {
            log.error("An error ocurred while calling the media wrapper page ", e);
        } finally {
            IOUtils.closeQuietly(out);
        }

    }



}
