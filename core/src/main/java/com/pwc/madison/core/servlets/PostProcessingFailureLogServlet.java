package com.pwc.madison.core.servlets;

import com.day.cq.commons.jcr.JcrConstants;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.servlets.HttpConstants;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.osgi.framework.Constants;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.jcr.Binary;
import javax.jcr.Node;
import javax.servlet.Servlet;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

@Component(service = Servlet.class,
property = {
        Constants.SERVICE_DESCRIPTION + "=Servlet for showing postprocessing failure logs",
        "sling.servlet.method="+ HttpConstants.METHOD_GET,
        "sling.servlet.paths=/bin/pwc-madison/post-processing-failure-log"
})
public class PostProcessingFailureLogServlet extends SlingSafeMethodsServlet {
    private final Logger LOG = LoggerFactory.getLogger(PostProcessingFailureLogServlet.class);

    @Override
    public void doGet(@Nonnull SlingHttpServletRequest request, @Nonnull SlingHttpServletResponse response){
        String logPath = request.getParameter("path");
        if(null == logPath){
            LOG.error("Path parameter is empty");
            return;
        }
        ResourceResolver resolver = request.getResourceResolver();
        Resource originalResource = resolver.getResource(logPath.concat("/jcr:content/renditions/original"));
        if(null == originalResource){
            LOG.error("original node is not present");
            return;
        }
        try {
            Node originalNode = originalResource.adaptTo(Node.class);
            Node jcrNode = originalNode.getNode(JcrConstants.JCR_CONTENT);
            if (null == jcrNode) {
                LOG.error("Jcr node is null for: {}", jcrNode.getPath());
                return;
            }
            Binary binary = originalNode.getNode(JcrConstants.JCR_CONTENT).getProperty(JcrConstants.JCR_DATA).getBinary();
            InputStream stream = binary.getStream();
            String log = readFromStream(stream);
            response.getWriter().print(log);
        }catch (Exception e){
            LOG.error("Exception occurred while updating output log file",e);
        }
    }

    public String readFromStream(InputStream stream) throws IOException {
        int MAX_BUFFER_SIZE  = 1024; //Maximal size of the buffer
        StringBuilder result = new StringBuilder();
        byte[] buffer = new byte[MAX_BUFFER_SIZE];
        int readChars;
        while ((readChars = stream.read(buffer, 0, MAX_BUFFER_SIZE)) > 0) {
            result.append(new String(buffer,0,readChars, StandardCharsets.UTF_8));
        }
        stream.close();
        return result.toString();
    }

}
