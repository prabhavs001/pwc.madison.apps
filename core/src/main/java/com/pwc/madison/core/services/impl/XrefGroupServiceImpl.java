package com.pwc.madison.core.services.impl;

import com.day.cq.search.PredicateGroup;
import com.day.cq.search.Query;
import com.day.cq.search.QueryBuilder;
import com.day.cq.search.result.SearchResult;
import com.pwc.madison.core.constants.DITAConstants;
import com.pwc.madison.core.constants.MadisonConstants;
import com.pwc.madison.core.services.XrefGroupService;
import com.pwc.madison.core.util.MadisonUtil;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Session;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * XrefGroupService service implementation
 */
@Component(service = XrefGroupService.class, immediate = true)
public class XrefGroupServiceImpl implements XrefGroupService {

    private static final Logger LOGGER = LoggerFactory.getLogger(XrefGroupService.class);
    public static final String P_NORM = "p-norm";
    public static final String PNUM = "pnum";

    @Reference
    private QueryBuilder queryBuilder;

    @Reference
    private ResourceResolverFactory resolverFactory;

    /**
     * Method that gets the anchor text of the provided xref resource node
     *
     * @param xrefResource
     *            {@link Resource} xref resource type node.
     *
     * @return {@link String}
     */
    @Override
    public String getReferencedSectionAnchorText(Resource xrefResource) {
        ResourceResolver resourceResolver = null;
        try {
            LOGGER.debug("Getting anchor text for the resource ", xrefResource);

            resourceResolver = MadisonUtil.getResourceResolver(resolverFactory,
                    MadisonConstants.MADISON_CONTENT_ADMIN_SUB_SERVICE);

            String pageFullLink = xrefResource.getValueMap().getOrDefault(DITAConstants.PROPERTY_LINK, StringUtils.EMPTY).toString();

            String sectionAnchorText = StringUtils.EMPTY;
            //1. extract hash string and separate the 2 parts of hash
            String pageLinkWithExtension = pageFullLink.split(DITAConstants.HASH_STR)[0];

            String hash = pageFullLink.split(DITAConstants.HASH_STR)[1];
            String selector2 = hash.substring(hash.lastIndexOf(MadisonConstants.UNDERSCORE)+1);

            //3. create query based on path and 2 parts of hash
            final Map<String, Object> predicateMap = new HashMap<>();
            predicateMap.put("path", pageLinkWithExtension.split(DITAConstants.HTML_EXT)[0]);
            predicateMap.put("property", DITAConstants.PN_ID);
            predicateMap.put("property.value", selector2);

            final Query query = queryBuilder.createQuery(PredicateGroup.create(predicateMap),
                    resourceResolver.adaptTo(Session.class));
            final SearchResult searchResult = query.getResult();
            LOGGER.debug("searchResult size::: {}", searchResult.getHits().size());

            //4. get the "pnum" value
            if (searchResult.getHits().size() == 1) {
                Iterator<Resource> resourcesItr = searchResult.getResources();
                Resource resource = resourcesItr.next();

                if (resource.getName().startsWith(P_NORM)) {
                    sectionAnchorText = resource.getValueMap().getOrDefault(PNUM, StringUtils.EMPTY).toString();
                }
            }
            return sectionAnchorText;
        }catch(Exception e){
            LOGGER.error(e.getMessage(), e);
        } finally {
            if (null != resourceResolver && resourceResolver.isLive()) {
                resourceResolver.close();
            }
        }
        return StringUtils.EMPTY;
    }
}
