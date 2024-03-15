package com.pwc.madison.core.reports;

import com.adobe.acs.commons.reports.api.ReportCellCSVExporter;
import com.pwc.madison.core.constants.MadisonConstants;
import com.pwc.madison.core.services.ReportService;
import com.pwc.madison.core.util.MadisonUtil;
import org.apache.commons.lang.StringUtils;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.Optional;
import org.apache.sling.models.annotations.injectorspecific.OSGiService;
import org.apache.sling.models.annotations.injectorspecific.ValueMapValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.ArrayList;

@Model(adaptables = Resource.class)
public class TagTypeCSVExporter implements ReportCellCSVExporter {
    private static final Logger LOG = LoggerFactory.getLogger(TagTypeCSVExporter.class);

    private ResourceResolver madisonServiceUserResolver = null;

    @ValueMapValue
    @Optional
    private String tagType;

    @Inject
    private ReportService reportService;

    @OSGiService
    private ResourceResolverFactory resolverFactory;

    @Override
    public String getValue(Object result) {
        ArrayList<String> tagList = new ArrayList<>();
        if(null == result){
            return StringUtils.EMPTY;
        }
        try{
            Resource resource = (Resource) result;
            madisonServiceUserResolver = MadisonUtil
                    .getResourceResolver(resolverFactory, MadisonConstants.MADISON_GENERIC_SUB_SERVICE);
            if(null == madisonServiceUserResolver){
                return StringUtils.EMPTY;
            }
            String metaPath = resource.getPath() + MadisonConstants.METADATA_PATH;
            tagList = reportService.getTagType(madisonServiceUserResolver, metaPath, tagType);

        }catch (Exception e){
            LOG.error("Error while fetching tagType: ",e);
        }finally {
            if(null != madisonServiceUserResolver){
                madisonServiceUserResolver.close();
            }
        }
        return StringUtils.join(tagList, ";");
    }
}
