package com.pwc.madison.core.servlets;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.servlet.Servlet;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.servlets.HttpConstants;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.json.JSONObject;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.pwc.madison.core.constants.MadisonConstants;

@Component(
    configurationPolicy = ConfigurationPolicy.REQUIRE,
    service = Servlet.class,
    property = {
            org.osgi.framework.Constants.SERVICE_DESCRIPTION
                    + "=This Servlet will generate the content hierarchy report in Json Format",
            "sling.servlet.methods=" + HttpConstants.METHOD_GET, "sling.servlet.paths=" + "/bin/pwc/contenthierarchy",
            "sling.servlet.extensions=" + "json" })
public class ContentHierarchyReportServlet extends SlingAllMethodsServlet {

    /**
     * This report takes a map path and iterates over the map to find the child maps and dita topics
     */
    private static final long serialVersionUID = 1L;
    private static final Logger LOG = LoggerFactory.getLogger(ContentHierarchyReportServlet.class);
    private static final String NODE_PATH = "nodePath";
    private static final String FMDITA_TOPICREFS = "fmditaTopicrefs";
    private static final String METADATA = "metadata";
    private static final String FMDITATITLE = "fmditaTitle";
    private static final String ASSET_TITLE = "title";
    private static final String ASSET_PATH = "assetpath";
    private static final String TOPIC_TEMPATE = "topicTemplate";
    private static final String MAP_TEMPLATE = "mapTemplate";
    private static final String IS_DITATOPIC = "isDITATopic";
    private static final String DAM_SHA1 = "dam:sha1";
    private static final String DITA_CLASS = "dita_class";
    private static final String DAMSHA1 = "damsha1";
    private static final String COMMA = ",";
    private static final String CONTENT_TYPE_JSON = "application/json";
    private static final String JCR_PATH = "/jcr:content";
    private static final String TOPICREFS = "-topicRefs";
    private static final String TOPIC_CLASS = "topic/topic";
    private static final String MAP_CLASS = "- map/map";
    private static final String BOOKMAP_CLASS = "- map/map bookmap/bookmap";
    private static final String IS_BOOKMAP = "isbookmap";
    private static final String PATH_NOT_EXIST_MESSAGE = "The DitaMap Path Does Not Exist, Please try a different DitaMap";
    JSONObject jsonObject;
    ResourceResolver resourceResolver;
    Resource resource;
    LinkedHashMap<String, Object> reportMap;

    @Override
    protected void doGet(final SlingHttpServletRequest request, final SlingHttpServletResponse response)
            throws IOException {
        LOG.debug("inside doGet", this.getClass());
        resourceResolver = request.getResourceResolver();
        response.setContentType(CONTENT_TYPE_JSON);
        jsonObject = new JSONObject();
        String nodePath = request.getParameter(NODE_PATH);
        Gson gson = new Gson();
        resource = getResource(nodePath);
        if (resource != null && nodePath.contains(MadisonConstants.SLING_SELECTORS_DITAMAP)) {
            reportMap = addNode(nodePath);
            LOG.debug("Json Map==>", gson.toJson(reportMap));
            gson.toJson(reportMap, response.getWriter());
        } else {
            gson.toJson(PATH_NOT_EXIST_MESSAGE, response.getWriter());
        }

    }

    private LinkedHashMap<String, Object> addNode(String path) {
        String title = StringUtils.EMPTY;
        String assetPath = StringUtils.EMPTY;
        Resource res = null;
        List<Object> topicRefsList = new ArrayList<Object>();
        LinkedHashMap<String, Object> map = new LinkedHashMap<String, Object>();
        try {
            res = resourceResolver.getResource(path);
            if (res == null) {
                return map;
            }

            assetPath = res.getPath().trim();
            String jcrResource = res.getPath() + JCR_PATH;
            Resource jcrResourcePath = getResource(jcrResource);
            Node jcrNode = jcrResourcePath.adaptTo(Node.class);
            title = getProperty(jcrNode, FMDITATITLE);

            if (jcrNode.hasNode(METADATA)) {
                populateMetaDataNode(res, map);
            }
            if (jcrNode.hasProperty(FMDITA_TOPICREFS)) {
                Property topicRefs = jcrNode.getProperty(FMDITA_TOPICREFS);
                Value values[] = topicRefs.getValues();
                for (Value topicRef : values) {
                    String[] resPath = topicRef.getString().split(COMMA);
                    LinkedHashMap<String, Object> topicRefsArray = addNode(resPath[1]);
                    topicRefsList.add(topicRefsArray);
                }
            }
            map.put(ASSET_TITLE, title);
            map.put(ASSET_PATH, assetPath);
            if (!topicRefsList.isEmpty()) {
                map.put(TOPICREFS, topicRefsList);
            }

        } catch (RepositoryException exception) {
            map.put("error in response json", exception.getMessage());
        }
        return map;
    }

    private String getProperty(Node node, String propertyName) {
        String property = StringUtils.EMPTY;
        try {
            if (node != null && node.hasProperty(propertyName)) {
                property = node.getProperty(propertyName).getString().trim();
            }

        } catch (RepositoryException re) {
            LOG.debug("error in reading properties{}", re.getMessage());
        }
        return property;
    }

    private void populateMetaDataNode(Resource resource, LinkedHashMap<String, Object> map) {
        boolean isDitaTopic = false;
        boolean isBookMap = false;
        boolean isMap = false;
        String damsha1 = StringUtils.EMPTY;
        String ditaTemplate = StringUtils.EMPTY;
        String ditaClass = StringUtils.EMPTY;

        String metaDataresource = resource.getPath() + MadisonConstants.METADATA_PATH;
        Resource metaDataResource = getResource(metaDataresource);

        if (null == metaDataResource) {
            return;
        }

        Node metaNode = metaDataResource.adaptTo(Node.class);
        damsha1 = getProperty(metaNode, DAM_SHA1);
        ditaClass = getProperty(metaNode, DITA_CLASS);
        isDitaTopic = ditaClass.contains(TOPIC_CLASS) ? true : false;
        isBookMap = ditaClass.contains(BOOKMAP_CLASS) ? true : false;
        isMap = ditaClass.contains(MAP_CLASS) ? true : false;
        ditaTemplate = getProperty(metaNode, DITA_CLASS);

        map.put(DAMSHA1, damsha1);
        map.put(IS_BOOKMAP, isBookMap);
        map.put(IS_DITATOPIC, isDitaTopic);
        if (isDitaTopic) {
            map.put(TOPIC_TEMPATE, ditaTemplate);
        } else if (isMap) {
            map.put(MAP_TEMPLATE, ditaTemplate);
        }
    }

    private Resource getResource(String resourcePath) {
        Resource lResource = null;
        if (resourcePath != null && StringUtils.isNoneEmpty(resourcePath)) {
            lResource = resourceResolver.getResource(resourcePath);
        }
        return lResource;
    }

}
