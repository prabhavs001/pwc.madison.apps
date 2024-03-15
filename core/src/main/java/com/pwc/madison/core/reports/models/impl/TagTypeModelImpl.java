package com.pwc.madison.core.reports.models.impl;

import com.pwc.madison.core.constants.MadisonConstants;
import com.pwc.madison.core.reports.models.TagTypeModel;
import com.pwc.madison.core.services.ReportService;
import com.pwc.madison.core.util.MadisonUtil;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.Optional;
import org.apache.sling.models.annotations.injectorspecific.OSGiService;
import org.apache.sling.models.annotations.injectorspecific.RequestAttribute;
import org.apache.sling.models.annotations.injectorspecific.ValueMapValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import java.util.Collections;
import java.util.List;

@Model(adaptables = SlingHttpServletRequest.class, adapters = TagTypeModel.class)
public class TagTypeModelImpl implements TagTypeModel {
    private final Logger LOG = LoggerFactory.getLogger(TagTypeModelImpl.class);
    private final String TAG_TYPE = "tagType";
    private ResourceResolver madisonServiceUserResolver;

    private List tagList = Collections.emptyList();

    @ValueMapValue
    @Optional
    private String tagType;

    @RequestAttribute
    private Resource result;

    @OSGiService
    private ResourceResolverFactory resolverFactory;

    @Inject
    private ReportService reportService;

    @PostConstruct
    private void init(){
        if(null == result){
            return;
        }
        try{
            madisonServiceUserResolver = MadisonUtil
                    .getResourceResolver(resolverFactory, MadisonConstants.MADISON_GENERIC_SUB_SERVICE);
            if(null == madisonServiceUserResolver){
                return;
            }
            tagList = reportService.getTagType(madisonServiceUserResolver, result.getPath()+ MadisonConstants.METADATA_PATH, tagType);
        }catch (Exception e){
            LOG.error("Error while getting tag Type");
        }finally {
            if(null != madisonServiceUserResolver){
                madisonServiceUserResolver.close();
            }
        }
    }

    @Override
    public List getTagType() {
        return tagList;
    }
}
