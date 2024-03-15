package com.pwc.madison.core.servlets;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.jcr.Node;
import javax.servlet.Servlet;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.request.RequestParameterMap;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.api.servlets.HttpConstants;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.osgi.framework.Constants;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pwc.madison.core.constants.DITAConstants;
import com.pwc.madison.core.constants.MadisonConstants;
import com.pwc.madison.core.services.MadisonSystemUserNameProviderService;
import com.pwc.madison.core.util.MadisonUtil;

@Component(service = Servlet.class,
property = {Constants.SERVICE_DESCRIPTION+"= Servlet to set DocState",
"sling.servlet.methods="+ HttpConstants.METHOD_POST, "sling.servlet.paths="+ "/bin/pwc/setDocState"})
public class SetDocStateServlet extends SlingAllMethodsServlet {

    private static final Logger LOG = LoggerFactory.getLogger(SetDocStateServlet.class);
    private static final String PATH = "path";
    private static final String DOC_STATE = "docstate";
    Map<String, List<String>> topicsMap = Collections.EMPTY_MAP;

    @Reference
    ResourceResolverFactory resolverFactory;

    @Reference
    private MadisonSystemUserNameProviderService madisonSystemUserNameProviderService;

    @Override
    protected void doPost(SlingHttpServletRequest request, SlingHttpServletResponse response){
        String path = StringUtils.EMPTY;
        String docState = StringUtils.EMPTY;
        ResourceResolver resourceResolver = MadisonUtil.getResourceResolver(resolverFactory, madisonSystemUserNameProviderService.getFmditaServiceUsername());
        RequestParameterMap requestParameterMap = request.getRequestParameterMap();
        if(requestParameterMap.containsKey(PATH)){
            path = requestParameterMap.getValue(PATH).toString();
        }
        if(requestParameterMap.containsKey(DOC_STATE)){
            docState = requestParameterMap.getValue(DOC_STATE).toString();
        }
        try{
        if(StringUtils.isNotBlank(path) && StringUtils.isNotBlank(docState)){
            Resource asset = resourceResolver.getResource(path.concat(MadisonConstants.METADATA_PATH));
            if(null == asset){
                return;
            }
            Node metaDataNode = asset.adaptTo(Node.class);
            if(null != metaDataNode){
                metaDataNode.setProperty(DITAConstants.PN_METADATA_DOCSTATE, docState);
                resourceResolver.commit();
                response.getWriter().write("docState property successfully updated");
            }
        }
        }catch (Exception e){
            LOG.error("Error while setting docstate: ", e);
        }
    }
}
