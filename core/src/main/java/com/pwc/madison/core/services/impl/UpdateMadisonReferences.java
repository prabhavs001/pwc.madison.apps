package com.pwc.madison.core.services.impl;

import java.util.Collections;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Value;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;

import com.day.cq.wcm.api.WCMException;
import com.day.cq.wcm.commons.ReferenceSearch;
import com.day.cq.wcm.msm.api.LiveActionFactory;
import com.day.cq.wcm.msm.api.LiveRelationship;
import com.day.cq.wcm.msm.api.RolloutManager;
import com.day.cq.wcm.msm.commons.BaseActionFactory;
import com.day.cq.wcm.msm.commons.FilteredAction;
import com.day.cq.wcm.msm.commons.FilteredActionFactoryBase;
import com.day.cq.wcm.msm.commons.ItemFilterImpl;
import com.pwc.madison.core.constants.MadisonConstants;
import com.pwc.madison.core.util.MadisonUtil;

@Component(
        immediate = true,
        service = LiveActionFactory.class,
        property = { LiveActionFactory.LIVE_ACTION_NAME + "=" + UpdateMadisonReferences.LIVE_ACTION_CLASS_NAME,
                LiveActionFactory.LIVE_ACTION_NAME + "=" + UpdateMadisonReferences.LIVE_ACTION_NAME })
public class UpdateMadisonReferences
        extends FilteredActionFactoryBase<UpdateMadisonReferences.UpdateMadisonReferencesAction> {
    public static final String LIVE_ACTION_CLASS_NAME = "UpdateMadisonReferencesAction";
    public static final String LIVE_ACTION_NAME = "UpdateMadisonReferences";

    private static final String SOURCE_CONTENT_PATH_REGEXP = "/content/pwc-madison(/ditaroot)?/<source_territory>/<source_language>[\\w\\-/]*";
    private static final String SOURCE_CONTENT_PATH_REGEXP_TERRITORY_PLACEHOLDER = "<source_territory>";
    private static final String SOURCE_CONTENT_PATH_REGEXP_LANGUAGE_PLACEHOLDER = "<source_language>";

    @Reference
    private RolloutManager rolloutManager;

    @Activate
    @Modified
    protected void configure(ComponentContext context) {
        setupFilter(context, this.rolloutManager);
    }

    @Override
    protected UpdateMadisonReferencesAction newActionInstance(ValueMap valueMap) throws WCMException {
        return new UpdateMadisonReferencesAction(valueMap, this.getPagePropertyFilter(), this.getComponentFilter(),
                this);
    }

    @Override
    public String createsAction() {
        return LIVE_ACTION_NAME;
    }

    /**
     * 
     * Action that is called if used in any rollout configuration. Responsible to transform link references used in
     * source page in live copy by changing territory to live copy territory in links.
     * 
     */
    class UpdateMadisonReferencesAction extends FilteredAction {

        protected UpdateMadisonReferencesAction(ValueMap configuration, ItemFilterImpl pageItemFilter,
                ItemFilterImpl componentItemFilter, BaseActionFactory factory) {
            super(configuration, pageItemFilter, componentItemFilter, factory);
        }

        @Override
        protected boolean doHandle(Resource source, Resource target, LiveRelationship relation, boolean resetRollout)
                throws RepositoryException, WCMException {
            return resourceHasNode(source) && resourceHasNode(target);
        }

        @Override
        protected void doExecute(Resource source, Resource target, LiveRelationship relation, boolean resetRollout)
                throws RepositoryException, WCMException {
            final ResourceResolver resolver = target.getResourceResolver();
            final Node sourceNode = source.adaptTo(Node.class);
            final String sourceTerritory = MadisonUtil.getTerritoryCodeFromPagePath(source.getPath());
            final String sourceLanguage = MadisonUtil.getLanguageCodeForPath(source.getPath());
            final String targetTerritory = MadisonUtil.getTerritoryCodeFromPagePath(target.getPath());
            PropertyIterator propertyIterator = sourceNode.getProperties();
            while (propertyIterator.hasNext()) {
                Property property = propertyIterator.nextProperty();
                if (property.isMultiple()) {
                    for (Value value : property.getValues()) {
                        processSingleValue(value, resolver, target, sourceTerritory, sourceLanguage, targetTerritory);
                    }
                } else {
                    processSingleValue(property.getValue(), resolver, target, sourceTerritory, sourceLanguage,
                            targetTerritory);
                }
            }
        }

        private void processSingleValue(final Value value, final ResourceResolver resolver, Resource target,
                final String sourceTerritory, final String sourceLanguage, final String targetTerritory)
                throws RepositoryException, WCMException {
            if (value.getType() != PropertyType.STRING) {
                return;
            }
            final String sourcePath = value.getString();
            if (sourcePath == null || !sourcePath.contains(MadisonConstants.PWC_MADISON_CONTENT_BASEPATH)) {
                return;
            }
            final Pattern SOURCE_CONTENT_PATH_PATTERN = Pattern.compile(SOURCE_CONTENT_PATH_REGEXP
                    .replace(SOURCE_CONTENT_PATH_REGEXP_TERRITORY_PLACEHOLDER, sourceTerritory)
                    .replace(SOURCE_CONTENT_PATH_REGEXP_LANGUAGE_PLACEHOLDER, sourceLanguage));
            Matcher pathMatcher = SOURCE_CONTENT_PATH_PATTERN.matcher(sourcePath);
            while (pathMatcher.find()) {
                String path = pathMatcher.group();
                adjustReferences(path, target, sourceTerritory, targetTerritory);
            }
        }

        private void adjustReferences(final String sourcePath, Resource target, final String sourceTerritory,
                final String targetTerritory) throws RepositoryException, WCMException {
            new ReferenceSearch().adjustReferences(target.adaptTo(Node.class), sourcePath,
                    sourcePath.replace(
                            MadisonConstants.FORWARD_SLASH + sourceTerritory + MadisonConstants.FORWARD_SLASH,
                            MadisonConstants.FORWARD_SLASH + targetTerritory + MadisonConstants.FORWARD_SLASH),
                    true, Collections.emptySet());
        }

    }
}
