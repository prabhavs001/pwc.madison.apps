package com.pwc.madison.core.servlets;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.servlet.Servlet;
import javax.servlet.ServletException;

import org.apache.commons.lang3.StringUtils;
import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.api.servlets.HttpConstants;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.apache.sling.xss.XSSAPI;
import org.osgi.framework.Constants;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adobe.acs.commons.notifications.InboxNotificationSender;
import com.pwc.madison.core.constants.MadisonConstants;
import com.pwc.madison.core.services.MailSenderService;
import com.pwc.madison.core.services.SyndicationService;
import com.pwc.madison.core.util.MadisonUtil;
import com.pwc.madison.core.util.SyndicationUtil;

/**
 * Servlet for triggering bulk syndication for a given source folder. curl -u <user>:<password>
 * <host>/bin/pwc-madison/syndication?source=<sourcepath>\&destination=<destination>
 */

@Component(
    service = Servlet.class,
    property = { Constants.SERVICE_DESCRIPTION + "=Bulk Syndication Servlet",
            "sling.servlet.methods=" + HttpConstants.METHOD_GET,
            "sling.servlet.paths=" + "/bin/pwc-madison/syndication" })
public class SyndicationServlet extends SlingSafeMethodsServlet {

    private static final long serialVersionUID = 1L;

    @Reference
    transient ResourceResolverFactory resourceResolverFactory;
    @Reference
    transient SyndicationService syndicationService;
    @Reference
    transient MailSenderService mailSenderService;
    @Reference
    transient InboxNotificationSender inboxNotificationSender;
    @Reference
    private XSSAPI xssAPI;

    private final Logger LOG = LoggerFactory.getLogger(SyndicationServlet.class);

    @Override
    protected void doGet(@Nonnull final SlingHttpServletRequest request,
            @Nonnull final SlingHttpServletResponse response) throws ServletException, IOException {
        final String source = request.getParameter("source");
        final String destination = request.getParameter("destination");
        final PrintWriter writer = response.getWriter();

        final boolean isDestinationProvided = StringUtils.isBlank(destination) ? false : true;

        if (StringUtils.isBlank(source)) {
            writer.write("Source param not provided. Please check the request\n");
            writer.close();
            return;
        }

        final ResourceResolver resourceResolver = request.getResourceResolver();
        if (null == resourceResolver) {
            writer.write("Unable to get service resolver to process data. Please check the permissions\n");
            writer.close();
            return;
        }

        final Resource sourceResource = resourceResolver.getResource(source);

        if (null == sourceResource) {
            writer.write("Source Folder not present. Please validate and try again\n");
            writer.close();
            return;
        }

        final List<Resource> syndicationSubscribers = SyndicationUtil.getSyndicationSubscribers(source,
                resourceResolver);

        if (syndicationSubscribers.isEmpty()) {
            writer.append("Source or Subscribers not configured. Please validate and try again\n");
            writer.close();
            return;
        }

        boolean isDestinationConfigured = false;

        for (final Resource subscriberResource : syndicationSubscribers) {
            final String destinationPath = subscriberResource.getValueMap().get("destinationPath", String.class);

            if (StringUtils.isBlank(destinationPath)) {
                continue;
            }

            final Resource destinationResource = resourceResolver.getResource(destinationPath);

            if (isDestinationProvided) {
                if (destinationPath.equals(destination)) {

                    if (null == destinationResource) {
                        writer.append(
                                String.format("Syndication subscriber %s not present. Please validate and try again.\n",
                                        destinationPath));
                        LOG.error("Syndication subscriber {} not present", destinationPath);
                        writer.close();
                        return;
                    }

                    callSyndicationService(sourceResource, destinationResource, writer, resourceResolver,
                            subscriberResource);

                    isDestinationConfigured = true;
                    break;
                }
            } else {
                if (destinationResource == null) {
                    writer.append(
                            String.format("Syndication subscriber %s not present. Please validate and try again.\n",
                                    destinationPath));
                    LOG.error("Syndication subscriber {} not present", destinationPath);
                    continue;
                }

                callSyndicationService(sourceResource, destinationResource, writer, resourceResolver,
                        subscriberResource);

            }

        }

        if (isDestinationProvided && !isDestinationConfigured) {
            writer.append(String.format("Syndication subscriber %s not present. Please validate and try again.\n",
                    destination));
            LOG.error("Syndication subscriber {} not present", xssAPI.encodeForHTML(destination));
        }

        writer.close();

    }

    private void callSyndicationService(final Resource sourceResource, final Resource destinationResource,
            final PrintWriter writer, final ResourceResolver resourceResolver, final Resource subscriberResource) {

        final String territoryResponse = syndicationService.processSyndication(sourceResource, destinationResource);
        try {
            SyndicationUtil.setSyndicationStatus(subscriberResource, true);
        } catch (final PersistenceException e) {
            LOG.error("Unable to save syndication status for {} ", subscriberResource.getPath(), e);
        }
        sendSyndicationNotifications(sourceResource, destinationResource, resourceResolver);

        writer.append(territoryResponse);
        writer.append("\n");
    }

    private void sendSyndicationNotifications(final Resource sourceResource, final Resource destinationResource,
            final ResourceResolver resourceResolver) {
        try {
            final Map<String, String> emailParams = new HashMap<>();
            final String title = destinationResource.getName();
            emailParams.put("title", title);
            emailParams.put("source", MadisonUtil.getTerritoryCodeForPath(sourceResource.getPath()));

            // parent author group (<territory>-madison-author)
            final String territoryAuthorGroupName = MadisonUtil.getTerritoryCodeForPath(destinationResource.getPath())
                    + "-" + MadisonConstants.MADISON_PUBLISHER;

            // get all the territory author groups
            final Set<Authorizable> authorizables = SyndicationUtil.getTerritoryGroups(destinationResource,
                    resourceResolver, territoryAuthorGroupName);
            final String subject = title + " has been syndicated and is now available";

            for (final Authorizable group : authorizables) {
                if (group.isGroup()) {
                    SyndicationUtil.sendInboxNotification(inboxNotificationSender, resourceResolver,
                            destinationResource, title, subject, group.getID());
                }
            }

            SyndicationUtil.sendSyndicationEmailNotification(MadisonConstants.SYNDICATION_COMPLETE_EMAIL_TEMPLATE,
                    subject, emailParams, mailSenderService, authorizables);
        } catch (final Exception e) {
            LOG.error("sendSyndicationNotifications error ", destinationResource.getPath(), e);
        }
    }
}
