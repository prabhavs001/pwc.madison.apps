package com.pwc.madison.core.reports;

import com.day.cq.dam.api.Asset;
import com.pwc.madison.core.beans.BackwardReferencesReport;
import com.pwc.madison.core.beans.ForwardReferencesReport;
import com.pwc.madison.core.constants.MadisonConstants;
import com.pwc.madison.core.util.MadisonUtil;
import org.apache.commons.lang3.StringUtils;
import org.apache.jackrabbit.JcrConstants;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.SlingObject;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * The Class UnpublishedAssetReportExecutor is a Extension of ACS Commons Query Report Executor, which gets the assets and
 * filters based on references to find unused assets which are published in nature.
 */
@Model(adaptables = SlingHttpServletRequest.class)
public class UnpublishedAssetReportExecutor extends AssetsReportExecutorModel{

    @SlingObject
    private ResourceResolver resourceResolver;

    @SlingObject
    private SlingHttpServletRequest request;

    private static final String ASSET_TYPE = "assetType";
    private static final String ALL = "all";
    private static final String CQ_LAST_REPLICATION_ACTION = "cq:lastReplicationAction";
    private static final String ACTIVATE = "Activate";
    private static final String DITA = ".dita";
    private static final String DITAMAP = ".ditamap";

    /**
     * Gets the unused assets.
     *
     * @param assetIterator
     *            the nodes
     * @return the unused assets
     */
    @Override
    protected List<String> getUnusedAssets(Iterator<Asset> assetIterator) {
        String cookieValue = MadisonUtil.getTokenCookieValue(request);
        List<String> paths = new ArrayList<>();
        String assetType = request.getParameter(ASSET_TYPE);
        for (Iterator<Asset> it = assetIterator; it.hasNext(); ) {
            Asset asset = it.next();
            String path = asset.getPath();
            if (!(path.endsWith(DITA) || path.endsWith(DITAMAP)) && isPublished(path) &&
                    (StringUtils.equalsIgnoreCase(assetType, ALL) || path.endsWith(assetType)))
                paths.add(path);
        }
        if (!paths.isEmpty() && StringUtils.isNotBlank(cookieValue)) {
            String endApi = getPostUrl(request.getResourceResolver());
            if (StringUtils.isNotBlank(endApi)) {
                URL url;
                try {
                    url = new URL(endApi);
                    List<BackwardReferencesReport> backwardRefsReportList = getBackwardReferencesReportList(paths, url,
                            endApi, cookieValue);
                    removeUsedAssetsFromBackwardRefs(backwardRefsReportList, paths, request.getResourceResolver());
                    List<ForwardReferencesReport> forwardRefsReportList = getForwardReferencesReportList(paths, url,
                            endApi, cookieValue);
                    removeUsedAssetsFromForwardRefs(forwardRefsReportList, paths, request.getResourceResolver());
                    //removeUsedAssetsFromPages(paths, request.getResourceResolver());
                } catch (MalformedURLException e) {
                    log.error("Error getting hostname", e);
                }
            }
        }
        return paths;
    }

    /**
     * Checks if the path is published
     *
     * @param path
     * @return boolean check
     */
    private boolean isPublished(String path){
        boolean isPublished = false;
        Resource res = resourceResolver.getResource(path + MadisonConstants.FORWARD_SLASH+ JcrConstants.JCR_CONTENT);
        ValueMap vm = res.getValueMap();
        if (vm.containsKey(CQ_LAST_REPLICATION_ACTION)
                && vm.get(CQ_LAST_REPLICATION_ACTION, String.class).equalsIgnoreCase(ACTIVATE))
            isPublished = true;

        return isPublished;
    }
}
