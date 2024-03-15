package com.pwc.madison.core.models;

import com.day.cq.commons.jcr.JcrConstants;
import com.day.cq.wcm.api.Page;
import com.pwc.madison.core.constants.MadisonConstants;
import com.pwc.madison.core.util.MadisonUtil;
import org.apache.commons.lang.StringUtils;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.models.annotations.Default;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.OSGiService;
import org.apache.sling.models.annotations.injectorspecific.ValueMapValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import java.text.SimpleDateFormat;
import java.util.GregorianCalendar;
import java.util.Objects;

/**
 * Insights Tile Component's composite multifield content model represents a link which contains link text, url, image link
 * and containing page title
 **/
@Model(adaptables = {Resource.class})
public class InsightsTileContent {

    private static final Logger LOGGER = LoggerFactory.getLogger(InsightsTileContent.class);

    @ValueMapValue
    private String insightsContentLabel;

    @ValueMapValue
    private String insightsContentUrl;

    @ValueMapValue
    @Default(values = StringUtils.EMPTY)
    private String insightsContentImage;

    private String contentPageTitle;

    private String contentCreatedDate;

    @OSGiService
    private ResourceResolverFactory resolverFactory;

    @PostConstruct
    protected void init() {
        insightsContentUrl += MadisonUtil.isLinkInternal(insightsContentUrl) ? MadisonConstants.HTML_EXTN : "";
        ResourceResolver resourceResolver = MadisonUtil.getResourceResolver(resolverFactory, MadisonConstants.MADISON_READ_SUB_SERVICE);
        Resource insightContentResource = resourceResolver.getResource(insightsContentUrl);
        if (Objects.nonNull(insightContentResource)) {
            Page page = insightContentResource.adaptTo(Page.class);
            if (Objects.nonNull(page)) {
                contentPageTitle = MadisonUtil.getPageTitle(page);
                setContentCreatedDate((GregorianCalendar) page.getProperties().get(JcrConstants.JCR_CREATED));
            }
        }

        resourceResolver.close();
    }

    public String getContentCreatedDate() {
        return contentCreatedDate;
    }

    /**
     * To retrieve and format the creation date of Page obtained from link url
     *
     * @param createdDate
     */
    private void setContentCreatedDate(GregorianCalendar createdDate) {
        LOGGER.debug("Creation date of Page Obtained from Url in Insights Link: {}", createdDate);
        SimpleDateFormat fmt = new SimpleDateFormat(MadisonConstants.COMPONENTS_DATE_FORMAT);
        fmt.setCalendar(createdDate);
        this.contentCreatedDate = fmt.format(createdDate.getTime());
        LOGGER.debug("Formatted Date of Page Insights: {}", this.contentCreatedDate);
    }

    /**
     * @return pageTitle
     */
    public String getContentPageTitle() {
        return contentPageTitle;
    }

    /**
     * @return link text
     */
    public String getInsightsContentLabel() {
        return insightsContentLabel;
    }

    /**
     * @return link url
     */
    public String getInsightsContentUrl() {

        return insightsContentUrl;
    }

    /**
     * @return image path
     */
    public String getInsightsContentImage() {
        return insightsContentImage;
    }

}
