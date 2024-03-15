package com.pwc.madison.core.servlets;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.servlet.Servlet;

import org.apache.commons.lang3.StringUtils;
import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.ModifiableValueMap;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.servlets.HttpConstants;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.apache.sling.jcr.api.SlingRepository;
import org.osgi.framework.Constants;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adobe.acs.commons.notifications.InboxNotificationSender;
import com.adobe.granite.asset.api.AssetManager;
import com.adobe.granite.taskmanagement.TaskManagerException;
import com.day.cq.commons.jcr.JcrConstants;
import com.day.cq.commons.jcr.JcrUtil;
import com.day.cq.dam.api.Asset;
import com.day.cq.dam.commons.util.DamUtil;
import com.pwc.madison.core.constants.DITAConstants;
import com.pwc.madison.core.constants.MadisonConstants;
import com.pwc.madison.core.services.MailSenderService;
import com.pwc.madison.core.util.MadisonUtil;
import com.pwc.madison.core.util.SyndicationUtil;
import com.pwc.madison.core.util.TranslationUtil;

/**
 * Servlet for triggering bulk translation for a given source folder. curl -u <user>:<password>
 * <host>/bin/pwc-madison/content-translation?source=<source>\&destination=<destination>
 */
@Component(service = Servlet.class,
           property = { Constants.SERVICE_DESCRIPTION + "=Content Translation Servlet",
                   "sling.servlet.methods=" + HttpConstants.METHOD_GET,
                   "sling.servlet.paths=/bin/pwc-madison/content-translation" })
public class ContentTranslationServlet extends SlingSafeMethodsServlet {

    private static final long serialVersionUID = 1L;
    protected final Logger log = LoggerFactory.getLogger(SimplifiedWorkFlowServlet.class);
    private static final String DITAROOT_PATH =
            MadisonConstants.PWC_MADISON_DAM_BASEPATH + MadisonConstants.DITAROOT_TEXT;
    private static final String DAM_PATH = "dam/";
    private static final String PN_FMDITA_TARGET_PATH = "fmdita-targetPath";
    private static final String DITA_ASSET_NAMEDOUTPUTS_AEMSITE = "jcr:content/metadata/namedoutputs/aemsite";
    private static final String INBOX_NOTIFICATION_TITLE = "Translation task";

    @Reference
    protected SlingRepository repository;
    @Reference
    transient InboxNotificationSender inboxNotificationSender;
    @Reference
    private MailSenderService mailSenderService;

    @Override
    protected void doGet(@Nonnull final SlingHttpServletRequest request,
            @Nonnull final SlingHttpServletResponse response) throws IOException {
        Session session = null;

        final String source = request.getParameter("source");
        final String destination = request.getParameter("destination");
        final PrintWriter writer = response.getWriter();

        if (StringUtils.isBlank(source) || StringUtils.isBlank(destination)) {
            writer.write("Source or Destination params are blank\n");
            writer.close();
            return;
        }
        final ResourceResolver resolver = request.getResourceResolver();

        if (null == resolver) {
            writer.write("Unable to get resolver to process data.\n");
            writer.close();
            return;
        }

        session = resolver.adaptTo(Session.class);

        if (resolver.getResource(source) == null || resolver.getResource(destination) == null) {
            writer.write("Please make sure both source and destination path exists.\n");
            writer.close();
            return;
        }
        final Resource sourceResource = resolver.getResource(source);
        final Resource destinationResource = resolver.getResource(destination);

        try {
            final String syndicationSourcePath = StringUtils.remove(destination, DITAROOT_PATH);
            final String configPath = DITAConstants.TRANSLATION_CONFIG_PATH + StringUtils.remove(source, DITAROOT_PATH);
            Resource configResource = resolver.getResource(configPath);

            if (null == configResource) {
                // Create configuration nodes
                createConfigNode(resolver, configPath, session);
                configResource = resolver.getResource(configPath);
            }
            final AssetManager assetManager = resolver.adaptTo(AssetManager.class);

            if (null == assetManager) {
                throw new NullPointerException("AssetManager is Empty");
            }

            // Copy Assets and folder from source to destination
            final Iterator<Asset> assets = DamUtil.getAssets(sourceResource);
            while (assets.hasNext()) {
                final Asset srcAsset = assets.next();
                final String assetPath = srcAsset.getPath();
                syndicateAsset(resolver, assetManager, assetPath, source, destination);
            }

            final Node configNode = configResource.adaptTo(Node.class);
            // Set syndicationSource on source path pointing to configuration node
            final Node sourceJcrNode = resolver
                    .getResource(source + DITAConstants.FORWARD_SLASH + JcrConstants.JCR_CONTENT).adaptTo(Node.class);

            if (!sourceJcrNode.hasProperty(DITAConstants.PN_SYNDICATION_SOURCE)) {
                JcrUtil.setProperty(sourceJcrNode, DITAConstants.PN_SYNDICATION_SOURCE, configPath);
            }
            /// Set subscriber configuration
            if (!subscriberExists(configResource, syndicationSourcePath)) {
                final Node subscriberNode = JcrUtil
                        .createUniqueNode(configNode, DITAConstants.PN_SUBSCRIBER, JcrConstants.NT_UNSTRUCTURED,
                                session);
                JcrUtil.setProperty(subscriberNode, DITAConstants.PN_SUBSCRIBER, syndicationSourcePath);
            }
            session.save();

            final Resource destResource = resolver.getResource(destination);
            final String territoryCode = MadisonUtil.getTerritoryCodeForPath(destination);
            if (StringUtils.isBlank(territoryCode)) {
                writer.write("No territory code found for destination. Send notification failed.\n");
                writer.close();
                return;
            }

            final String territoryAuthorGroup = new StringBuilder().append(territoryCode)
                    .append(MadisonConstants.HYPHEN).append(MadisonConstants.MADISON_AUTHOR).toString();
            final Set<Authorizable> allAllowedMembers = TranslationUtil
                    .getTerritoryGroups(resolver, destResource, territoryAuthorGroup, false);

            if (allAllowedMembers.isEmpty()) {
                writer.write("Author group is not configured on destination folder. Notifications were not sent\n");
                writer.close();
                return;
            }

            final Iterator<Authorizable> itr = allAllowedMembers.iterator();

            while (itr.hasNext()) {
                final String grpName = itr.next().getID();
                if (StringUtils.isNotBlank(grpName)) {
                    inboxNotification(resolver, grpName, destinationResource);

                    final Set<Authorizable> groupMembers = TranslationUtil
                            .getTerritoryGroups(resolver, destResource, grpName, true);
                    emailNotification(sourceResource, destinationResource, groupMembers);

                }

            }

            writer.write("Assets are ready for translation. Notification sent to Territory Authors\n");
            writer.close();

        } catch (final TaskManagerException e) {
            log.error("Error While creating task to send notification {}", e);
            writer.write("Error While creating task to send notification. Please check logs\n");
            writer.close();

        } catch (final Exception e) {
            log.error("Exception {}", e);
            writer.write("Task was not successful. Please check logs\n");
            writer.close();

        } finally {
            if (session != null)
                session.logout();
        }
    }

    /**
     * Create and send inbox notification to authors
     */
    private void inboxNotification(final ResourceResolver resolver, final String assignee, final Resource destResource)
            throws TaskManagerException {
        final String title = INBOX_NOTIFICATION_TITLE;
        final String message = "Content is ready for translation at " + destResource.getPath();
        SyndicationUtil
                .sendInboxNotification(inboxNotificationSender, resolver, destResource, title, message, assignee);
    }

    /**
     * Send email notification to Territory authors
     */
    private void emailNotification(final Resource sourceResource, final Resource destResource,
            final Set<Authorizable> authors) throws RepositoryException {
        final Map<String, String> emailParams = new HashMap<>();
        final String title = destResource.getName();
        emailParams.put("title", title);
        emailParams.put("source", MadisonUtil.getTerritoryCodeForPath(sourceResource.getPath()));
        final String subject = title + " has been copied and is now available for translation";
        SyndicationUtil.sendSyndicationEmailNotification(MadisonConstants.TRANSLATION_COMPLETE_EMAIL_TEMPLATE, subject,
                emailParams, mailSenderService, authors);
    }

    /**
     * Create Configuration nodes to store subscribers
     */
    private void createConfigNode(final ResourceResolver resolver, String path, final Session session) {

        if (path.lastIndexOf(MadisonConstants.FORWARD_SLASH) != path.length()) {
            path = path + MadisonConstants.FORWARD_SLASH;
        }

        final int fwCont = StringUtils.countMatches(path, DITAConstants.FORWARD_SLASH);
        for (int i = 4; i <= fwCont; i++) {
            // Check if node exists
            final Resource configNode = resolver
                    .getResource(path.substring(0, StringUtils.ordinalIndexOf(path, DITAConstants.FORWARD_SLASH, i)));

            if (configNode == null) {
                try {
                    JcrUtil.createPath(
                            path.substring(0, StringUtils.ordinalIndexOf(path, DITAConstants.FORWARD_SLASH, i)),
                            JcrConstants.NT_UNSTRUCTURED, session);
                    session.save();

                } catch (final RepositoryException e) {
                    log.error(" Error while creating configuration nodes {}" + e);
                }
            }

        }

    }

    /**
     * Check if subscriber already exists to avoid duplicate subscribers
     */
    private Boolean subscriberExists(final Resource configResource, final String syndicationSourcePath) {
        Boolean subExits = false;
        final Iterator<Resource> subItr = configResource.listChildren();
        while (subItr.hasNext()) {
            final Resource subscriber = subItr.next();
            if (subscriber.getValueMap().containsKey(DITAConstants.PN_SUBSCRIBER) && subscriber.getValueMap()
                    .get(DITAConstants.PN_SUBSCRIBER).toString().equals(syndicationSourcePath)) {
                subExits = true;
                break;
            }
        }
        return subExits;
    }

    /**
     * Copy assets and folder from source to destination path
     */
    private void syndicateAsset(final ResourceResolver resourceResolver, final AssetManager assetManager,
            final String assetPath, final String sourceBasePath, final String destinationBasePath) {
        final String destAssetPath = assetPath.replace(sourceBasePath, destinationBasePath);
        try {
            if (checkAssetParentPath(resourceResolver, destAssetPath, sourceBasePath, destinationBasePath)) {
                assetManager.copyAsset(assetPath, destAssetPath);
                final Resource assetRes = resourceResolver.getResource(destAssetPath);
                if (null != assetRes) {
                    if (assetRes.getName().endsWith(DITAConstants.DITAMAP_EXT) && null != assetRes
                            .getChild(DITA_ASSET_NAMEDOUTPUTS_AEMSITE)) {
                        final Node presetNode = assetRes.getChild(DITA_ASSET_NAMEDOUTPUTS_AEMSITE).adaptTo(Node.class);
                        final String presetPath = assetRes.getParent().getPath()
                                .replace(DAM_PATH, org.apache.commons.lang.StringUtils.EMPTY);
                        presetNode.setProperty(PN_FMDITA_TARGET_PATH, presetPath);
                    }
                }
            }
        } catch (final Exception e) {
            log.error("Copy assets Error: {}", e);
        }

    }

    /**
     * Creates a parent folder if it does not exits at destination while copying assets
     */
    private boolean checkAssetParentPath(final ResourceResolver resourceResolver, final String destAssetPath,
            final String sourceBasePath, final String destinationBasePath)
            throws PersistenceException, RepositoryException {

        boolean isParExists = false;

        if (org.apache.commons.lang.StringUtils.isBlank(destAssetPath)) {
            return isParExists;
        }
        final String destAssetParPath = destAssetPath
                .substring(0, destAssetPath.lastIndexOf(DITAConstants.FORWARD_SLASH));
        final Session session = resourceResolver.adaptTo(Session.class);
        if (null != session && !session.itemExists(destAssetParPath)) {
            final String resSourcePath = destAssetParPath.replace(destinationBasePath, sourceBasePath);
            final Resource sourceRes = resourceResolver.getResource(resSourcePath);
            final String destAssetParFolderPath = destAssetParPath
                    .substring(0, destAssetParPath.lastIndexOf(DITAConstants.FORWARD_SLASH));
            final Resource destAssetParFolderRes = resourceResolver.getResource(destAssetParFolderPath);
            if (null != destAssetParFolderRes && null != sourceRes) {
                final ModifiableValueMap valueMap = sourceRes.adaptTo(ModifiableValueMap.class);
                resourceResolver.create(destAssetParFolderRes, sourceRes.getName(), valueMap);
                final Resource parResource = resourceResolver.getResource(destAssetParPath);
                if (null != parResource) {
                    final Resource childRes = sourceRes.getChild(JcrConstants.JCR_CONTENT);
                    resourceResolver.create(parResource, JcrConstants.JCR_CONTENT, childRes.getValueMap());
                    isParExists = true;
                }
            }
        } else {
            isParExists = true;
        }
        return isParExists;

    }

}