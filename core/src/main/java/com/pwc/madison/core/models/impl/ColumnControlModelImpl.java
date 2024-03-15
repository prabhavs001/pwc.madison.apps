package com.pwc.madison.core.models.impl;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.jcr.Node;
import javax.jcr.RepositoryException;

import com.pwc.madison.core.models.ColumnControlModel;
import com.pwc.madison.core.util.MadisonUtil;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.models.annotations.DefaultInjectionStrategy;
import org.apache.sling.models.annotations.Exporter;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.OSGiService;
import org.apache.sling.models.annotations.injectorspecific.Self;
import org.apache.sling.models.annotations.injectorspecific.SlingObject;
import org.apache.sling.models.annotations.injectorspecific.ValueMapValue;
import org.apache.sling.models.factory.ModelFactory;
import org.apache.sling.settings.SlingSettingsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adobe.cq.export.json.ComponentExporter;
import com.adobe.cq.export.json.ContainerExporter;
import com.adobe.cq.export.json.ExporterConstants;
import com.adobe.cq.export.json.SlingModelFilter;
import com.day.cq.commons.jcr.JcrConstants;
import com.day.cq.commons.jcr.JcrUtil;

@Model(adaptables = { SlingHttpServletRequest.class,
        Resource.class }, adapters = { ColumnControlModel.class,ContainerExporter.class,ComponentExporter.class}, resourceType = ColumnControlModelImpl.RESOURCE_TYPE, defaultInjectionStrategy = DefaultInjectionStrategy.OPTIONAL)
@Exporter(name = ExporterConstants.SLING_MODEL_EXPORTER_NAME, extensions = ExporterConstants.SLING_MODEL_EXTENSION)
public class ColumnControlModelImpl extends ContainerExporterImpl implements ColumnControlModel{

    private static final String PAR = "par_";

    static final String RESOURCE_TYPE = "pwc-madison/components/inloop/columnControl";

    private static final String COLUMN2 = "column2";

    private static final String COLUMN1 = "column1";

    private final Logger logger = LoggerFactory.getLogger(getClass());

    @ValueMapValue
    private String layout;

    @ValueMapValue
    private String stackOrderMobile;

    @ValueMapValue
    private String verticallyCenterColumn;

    @SlingObject
    private Resource resource;

    @Self
    private SlingHttpServletRequest request;


    private String columnClass = StringUtils.EMPTY;
    private String columnStackClass = StringUtils.EMPTY;

    private List<String> numberOfColumns = new ArrayList<String>();

    @OSGiService
    private SlingSettingsService slingSettingsService;

    @OSGiService
    private ModelFactory modelFactory;

    @OSGiService
    private SlingModelFilter slingModelFilter;

    @PostConstruct
    protected void init() {
        logger.info("Column Control  **** INIT ***");
        if (StringUtils.isNotBlank(layout)) {
            String[] layoutVals = layout.split(";");
            for (int i = 0; i < Integer.parseInt(layoutVals[0]); i++) {
                numberOfColumns.add(String.valueOf(i));
            }
            try {
                adjustColumnLayout();
            } catch (RepositoryException e) {
                logger.error("Error rearranging columns ",e);
            }
            columnClass = layoutVals[1];
            setColumnStackClass();
        }
    }

    /**
     * Adjust column layout.
     *
     * @throws RepositoryException the repository exception
     */
    private void adjustColumnLayout() throws RepositoryException {
        if(null != resource) {
            int numberOfExistingColumns = getChildNodeCount();
            logger.debug("numberOfExistingColumns {}",numberOfExistingColumns);
            ResourceResolver resourceResolver = resource.getResourceResolver();
            if (MadisonUtil.isAuthor(slingSettingsService)) {
                copyColumnContent(numberOfExistingColumns, resourceResolver);
            }
        }
    }

    /**
     * Copy column content.
     *
     * @param numberOfExistingColumns the number of existing columns
     * @param resourceResolver the resource resolver
     * @throws RepositoryException the repository exception
     */
    private void copyColumnContent(int numberOfExistingColumns, ResourceResolver resourceResolver)
            throws RepositoryException {
        logger.debug("numberOfColumns {}",numberOfColumns);
        if(!numberOfColumns.isEmpty() && numberOfColumns.size() < numberOfExistingColumns) {
            String whereToMove = Integer.toString(numberOfColumns.size() -1 );
            logger.debug("whereToMove {}",whereToMove);
            Resource parentResource = resourceResolver.getResource(resource.getPath() + "/" +PAR+whereToMove);
            if(parentResource != null) {
                Node targetNode = parentResource.adaptTo(Node.class);
                int diff = numberOfExistingColumns - numberOfColumns.size();
                for(int i=numberOfExistingColumns -1 ; i >= diff ;i --) {
                    logger.debug("targetNode {}",targetNode.getName());
                    if(!StringUtils.equalsIgnoreCase(PAR+i, targetNode.getName())) {
                        moveChildren(targetNode, PAR+i, resourceResolver);
                    }
                }
                commitPendingChanges(targetNode);
            }
        }
    }

    private void commitPendingChanges(Node targetNode) throws RepositoryException {
        if(targetNode.getSession().hasPendingChanges()) {
            targetNode.getSession().save();
        }
    }

    /**
     * Gets the child node count.
     *
     * @return the child node count
     */
    private int getChildNodeCount() {
        Iterator<Resource> children = resource.listChildren();
        int numberOfExistingColumns = 0;
        while(children.hasNext()) {
            children.next();
            numberOfExistingColumns++;
        }
        return numberOfExistingColumns;
    }


    private void moveChildren(Node targetNode,String whatToMove,ResourceResolver resourceResolver) throws RepositoryException {
        Resource toMove = resourceResolver.getResource(resource.getPath() + "/" +whatToMove);
        logger.debug("toMove {}",toMove);
        if(toMove != null) {
            for (Resource child : toMove.getChildren()) {
                // Use this to create a unique node name; else existing components might get overwritten.
                final Node uniqueNode = JcrUtil.createUniqueNode(targetNode, child.getName(),
                        JcrConstants.NT_UNSTRUCTURED, targetNode.getSession());
                // Once we have a unique node we made as a place holder, we can copy over it w the real component content
                JcrUtil.copy(child.adaptTo(Node.class), targetNode, uniqueNode.getName(), true);
                logger.debug("Copied {} to {}",child.getPath(),targetNode.getPath());
            }
            // Remove the old par- node
            toMove.adaptTo(Node.class).remove();
        }
    }

    /**
     *Sets the different alignment options as per user selection.
     */
    private void setColumnStackClass() {
        columnStackClass = StringUtils.isBlank(stackOrderMobile)
                || COLUMN1.equals(stackOrderMobile) ? " " : "col-flipping--enable";
        if (null != verticallyCenterColumn) {
            if (verticallyCenterColumn.equals(COLUMN1)) {
                columnStackClass += " " + "vert-center--enable--col1";
            } else if (verticallyCenterColumn.equals(COLUMN2)) {
                columnStackClass += " " + "vert-center--enable--col2";
            }
        }
    }

    public String getColumnStackClass() {
        return columnStackClass;
    }

    public String getColumnClass() {
        return columnClass;
    }

    public List<String> getNumberOfColumns() {
        return numberOfColumns;
    }

    @Override
    public String getExportedType() {
        return RESOURCE_TYPE;
    }
}
