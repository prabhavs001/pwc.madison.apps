/**
 *
 */
package com.pwc.madison.core.services.impl;

import java.util.ArrayList;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.day.cq.dam.api.Asset;
import com.pwc.madison.core.constants.DITAConstants;
import com.pwc.madison.core.constants.MadisonConstants;
import com.pwc.madison.core.services.OutputPagesService;
import com.pwc.madison.core.util.DITAUtils;

@Component(service = { OutputPagesService.class }, immediate = true)
public class OutputPagesServiceImpl implements OutputPagesService {

    /**
     * Logger Reference
     */
    private static final Logger LOG = LoggerFactory.getLogger(OutputPagesServiceImpl.class);

    @Override
    public Object getNumberOfPages(final ResourceResolver resolver, final String assetPath) {
        try {
            // if path not empty and resolver not null
            if (StringUtils.isNotBlank(assetPath) && null != resolver) {
                final Resource aemSiteResource = resolver.getResource(assetPath + DITAConstants.AEMSITE_PRESETS_NODE);
                if (aemSiteResource == null) {
                    return 0;
                }
                final Node aemSiteNode = aemSiteResource.adaptTo(Node.class);
                if (!aemSiteNode.hasProperty(DITAConstants.PN_LAST_PUBLISHED_PATH)) {
                    return 0;
                }

                final String rootPagePath = aemSiteNode.getProperty(DITAConstants.PN_LAST_PUBLISHED_PATH).getString();
                final Resource rootPageResource = resolver
                        .getResource(rootPagePath.substring(0, rootPagePath.indexOf(MadisonConstants.HTML_EXTN)));
                if (rootPageResource == null) {
                    return 0;
                }
                final Node rootPageNode = rootPageResource.adaptTo(Node.class);

                return getNumberOfChildPages(rootPageNode).size();
            }
        } catch (final RepositoryException e) {
            LOG.error("Error in getting the number of output pages", e);
        }

        return 0;
    }

    @Override
    public Object getTopicsCount(final ResourceResolver resolver, final String assetPath) {
        try {
            // if path not empty and resolver not null
            if (StringUtils.isNotBlank(assetPath) && null != resolver) {
                return DITAUtils.getTopicRefs(assetPath, resolver, new ArrayList<Asset>()).size();
            }
        } catch (final Exception e) {
            LOG.error("Error in getting the topic count", e);
        }

        return 0;
    }

    private List<String> getNumberOfChildPages(final Node rootPageNode) throws RepositoryException {
        final List<String> pagePaths = new ArrayList<>();
        if (rootPageNode.isNodeType(MadisonConstants.CQ_PAGE)) {
            final NodeIterator children = rootPageNode.getNodes();
            if (rootPageNode.hasNode(DITAConstants.DITA_PWC_TOPIC_CONTAINER_V1.substring(1))) {
                pagePaths.add(rootPageNode.getPath());
            }
            while (children.hasNext()) {
                final Node childNode = children.nextNode();
                pagePaths.addAll(getNumberOfChildPages(childNode));
            }
        }
        return pagePaths;
    }

}
