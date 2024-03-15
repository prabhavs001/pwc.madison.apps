package com.pwc.madison.core.services.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.AttributeType;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.day.cq.search.PredicateGroup;
import com.day.cq.search.Query;
import com.day.cq.search.QueryBuilder;
import com.day.cq.search.result.Hit;
import com.day.cq.search.result.SearchResult;
import com.day.cq.tagging.Tag;
import com.day.cq.tagging.TagManager;
import com.pwc.madison.core.constants.DITAConstants;
import com.pwc.madison.core.constants.MadisonConstants;
import com.pwc.madison.core.reports.models.impl.SelectOptionModelImpl;
import com.pwc.madison.core.services.ReportService;
import com.pwc.madison.core.util.MadisonUtil;

/**
 * The Class ReportServiceImpl is the OSGi configuration for configuring custom reports.
 */
@Component(service = ReportService.class, configurationPolicy = ConfigurationPolicy.OPTIONAL, immediate = true)
@Designate(ocd = ReportServiceImpl.ReportConfig.class)
public class ReportServiceImpl implements ReportService {
    private static final Logger LOGGER = LoggerFactory.getLogger(SelectOptionModelImpl.class);

    private int batchLimit;

    private int readTimeOut;

    @Reference
    private QueryBuilder queryBuilder;

    @ObjectClassDefinition(name = "PwC Viewpoint Report Configuration")
    public @interface ReportConfig {

        /**
         * Batch limit.
         *
         * @return the int
         */
        @AttributeDefinition(
            name = "Batch Limit",
            description = "Maximum number of assets which can be checked.",
            type = AttributeType.INTEGER)
        int batchLimit() default 10000;

        /**
         * Read time out.
         *
         * @return the int
         */
        @AttributeDefinition(
            name = "Read Time Out",
            description = "Time Out for API Calls",
            type = AttributeType.INTEGER)
        int readTimeOut() default 60000;
    }

    /**
     * Activate.
     *
     * @param config
     *            the config
     */
    @Activate
    public void activate(ReportConfig config) {
        this.batchLimit = config.batchLimit();
        this.readTimeOut = config.readTimeOut();
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.pwc.madison.core.services.ReportService#getBatchLimit()
     */
    @Override
    public int getBatchLimit() {
        return batchLimit;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.pwc.madison.core.services.ReportService#getReadTimeOut()
     */
    @Override
    public int getReadTimeOut() {
        return readTimeOut;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.pwc.madison.core.services.ReportService#getOptionTextValue()
     */
    @Override
    public String getOptionTextValue(ResourceResolver resourceResolver, String dataSource, String[] value)
            throws RepositoryException {
        // Default Value
        String optionTextValue = StringUtils.EMPTY;

        if (null != resourceResolver && !StringUtils.isBlank(dataSource) && ArrayUtils.isNotEmpty(value)) {

            final Session session = resourceResolver.adaptTo(Session.class);

            // Query Predicate map creation
            final Map<String, String> predicateMap = new HashMap<>();
            predicateMap.put("path", dataSource);
            predicateMap.put("property", "value");
            for(int i=0;i<value.length;i++) {
                predicateMap.put("property."+(i+1)+"_value", String.valueOf(value[i]));
            }
            final Query query = queryBuilder.createQuery(PredicateGroup.create(predicateMap), session);
            LOGGER.debug("Query {}",query);
            // execute the query
            final SearchResult searchResult = query.getResult();

            // Get the Option Display Text from the search result
            if (!searchResult.getHits().isEmpty()) {
                optionTextValue = getRefValue(searchResult.getHits());
            }
        }

        return optionTextValue;
    }
    
    
    private String getRefValue(List<Hit> hits) throws RepositoryException {
        ArrayList<String> refList = new ArrayList<>();
        String result = StringUtils.EMPTY;
        if(hits.size() == 1) {
            result = hits.get(0).getProperties().get(MadisonConstants.NODE_PROP_TEXT,
                    String.class);
        }
        else if(hits.size() > 1) {
            for(Hit hit : hits) {
                refList.add(hit.getProperties().get(MadisonConstants.NODE_PROP_TEXT, String.class));
            }
            result = String.join(",", refList);
        }
        return result;
    }

    @Override
    public ArrayList<String> getTagType(ResourceResolver resolver, String path, String type){
        String localStr = MadisonUtil.getLocaleForPath(path);
        Locale locale = MadisonUtil.getLocaleObj(localStr);
        ArrayList<String> tagList = new ArrayList<>();
        Resource metaDataNode = resolver.getResource(path);
        if(null == metaDataNode){
            return tagList;
        }
        ValueMap valueMap = metaDataNode.getValueMap();
        if(valueMap.containsKey(DITAConstants.META_TAGS)){
            String[] tags = valueMap.get(DITAConstants.META_TAGS, String[].class);
            Arrays.stream(tags).filter(tag -> tag.startsWith(type)).forEach(e -> {
                String tagValue = getTagValue(e, resolver, locale);
                if(org.apache.commons.lang.StringUtils.isNotBlank(tagValue)){
                    tagList.add(tagValue);
                }
            });
        }
        return tagList;
    }

    private String getTagValue(String key, ResourceResolver resolver, Locale locale){
        String tagValue = org.apache.commons.lang.StringUtils.EMPTY;
        TagManager tagManager = resolver.adaptTo(TagManager.class);
        Tag tag = tagManager.resolve(key);
        tagValue = tag.getTitle(locale);
        return tagValue;
    }

}
