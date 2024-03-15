package com.pwc.madison.core.models.impl;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.PostConstruct;

import org.apache.commons.lang.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.models.annotations.Default;
import org.apache.sling.models.annotations.DefaultInjectionStrategy;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.ScriptVariable;
import org.apache.sling.models.annotations.injectorspecific.Self;
import org.apache.sling.models.annotations.injectorspecific.ValueMapValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.pwc.madison.core.constants.MadisonConstants;
import com.pwc.madison.core.models.NodeJson;

@Model(
        adaptables = { SlingHttpServletRequest.class },
        adapters = { NodeJson.class },
        resourceType = { NodeJsonImpl.RESOURCE_TYPE },
        defaultInjectionStrategy = DefaultInjectionStrategy.OPTIONAL)
public class NodeJsonImpl implements NodeJson {

    private static final Logger LOGGER = LoggerFactory.getLogger(NodeJson.class);

    protected static final String RESOURCE_TYPE = "pwc-madison/components/dialogform/nodeJson";

    private String nodeDataPath;
    @Self
    private SlingHttpServletRequest slingRequest;

    @ValueMapValue
    @Default(values = StringUtils.EMPTY)
    private String nodeName;

    @ValueMapValue
    private String[] nodeProperties;

    @ScriptVariable
    private ResourceResolver resolver;

    private String nodeJsonString;

    @PostConstruct
    protected void init() {
        final String dataPath = slingRequest.getRequestPathInfo().getSuffix();
        if (null != dataPath && StringUtils.isNotBlank(nodeName) && null != nodeProperties
                && nodeProperties.length > 0) {
            nodeDataPath = dataPath + MadisonConstants.FORWARD_SLASH + nodeName;
            List<JsonObject> nodeItemList = new ArrayList<>();
            if (null != nodeDataPath) {
                final Resource resource = resolver.getResource(nodeDataPath);
                if (null != resource) {
                    for (final Resource item : resource.getChildren()) {
                        JsonObject jsonObject = new JsonObject();
                        jsonObject.addProperty("name", item.getName());
                        for (final String propertyName : nodeProperties) {
                            jsonObject.addProperty(propertyName, item.getValueMap().get(propertyName, String.class));
                        }
                        nodeItemList.add(jsonObject);
                    }
                }
            }
            nodeJsonString = new Gson().toJson(nodeItemList);
            LOGGER.debug("NodeJsonImpl : nodeToJsonString() : Json String returned for nodeDataPath {} is{}",
                    nodeDataPath, nodeJsonString);
        }
    }

    @Override
    public String getNodeJsonString() {
        return nodeJsonString;
    }

}
