/**
 * 
 */
package com.pwc.madison.core.reports.models.impl;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.models.annotations.DefaultInjectionStrategy;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.RequestAttribute;
import org.apache.sling.models.annotations.injectorspecific.Self;

import com.adobe.granite.asset.api.AssetVersionManager;
import com.pwc.madison.core.reports.models.AssetVersionModel;
import com.pwc.madison.core.services.AssetVersionService;

/**
 * @author kartikkarnayil
 * 
 *         Model Class to get the Asset Version
 */
@Model(
    adaptables = SlingHttpServletRequest.class,
    adapters = AssetVersionModel.class,
    defaultInjectionStrategy = DefaultInjectionStrategy.OPTIONAL)
public class AssetVersionModelImpl implements AssetVersionModel {

    /**
     * SlingHttpServletRequest Request
     */
    @Self
    private SlingHttpServletRequest request;

    /**
     *  AssetVersionService service
     */
    @Inject
    private AssetVersionService assetVersionService;

    /**
     * Request Attribute result
     */
    @RequestAttribute
    private Resource result;

    /**
     * Asset Version Value
     */
    private Object assetVersion;

    public AssetVersionModelImpl() {
    }

    public AssetVersionModelImpl(Resource result) {
        this.result = result;
        init();
    }

    /**
     * Init Method
     */
    @PostConstruct
    private void init() {

        final ResourceResolver resolver = request.getResourceResolver();

        final AssetVersionManager versionManager = resolver.adaptTo(AssetVersionManager.class);

        if (null != result && null != versionManager) {

            final String assetPath = result.getPath();

            // get the asset version
            this.assetVersion = assetVersionService.getAssetVersion(versionManager, assetPath);
        }

    }

    @Override
    public Object getAssetVersion() {
        return this.assetVersion;
    }

}
