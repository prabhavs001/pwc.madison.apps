package com.pwc.madison.core.services.impl;

import com.adobe.acs.commons.notifications.InboxNotification;
import com.adobe.acs.commons.notifications.InboxNotificationSender;
import com.adobe.granite.taskmanagement.TaskManagerException;
import com.day.cq.commons.Externalizer;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import com.pwc.madison.core.beans.GeneratedResponse;
import com.pwc.madison.core.beans.QueuedOutput;
import com.pwc.madison.core.constants.DITAConstants;
import com.pwc.madison.core.constants.MadisonConstants;
import com.pwc.madison.core.services.MadisonDomainsService;
import com.pwc.madison.core.services.PublishDitamapService;
import com.pwc.madison.core.util.BulkDitaUtil;
import com.pwc.madison.core.util.MadisonUtil;
import com.pwc.madison.core.util.PublishingUtil;
import com.pwc.madison.core.util.SyndicationUtil;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.io.Charsets;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicHeader;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.util.EntityUtils;
import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.security.AccessControlManager;
import javax.jcr.security.Privilege;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Component(service = PublishDitamapService.class, immediate = true)
@Designate(ocd = PublishDitamapServiceImpl.PublishDitamapConfiguration.class)
public class PublishDitamapServiceImpl implements PublishDitamapService {

    private static final Logger LOG = LoggerFactory.getLogger(PublishDitamapServiceImpl.class);

    private static final int DEFAULT_TIMEOUT = 5000;
    public static final String WRITE = "write";

    private String publishingUserName;

    private String password;

    @Reference
    private MadisonDomainsService madisonDomainsService;
    @Reference
    private InboxNotificationSender inboxNotificationSender;

    @Activate
    @Modified
    protected void Activate(final PublishDitamapConfiguration publishDitamapConfiguration) {
        LOG.info("PublishDitamapService : Entered Activate/Modify");
        publishingUserName = publishDitamapConfiguration.publishing_username();
        LOG.debug("PublishDitamapServiceImpl - Publisher Username :: " + publishingUserName);
        password = publishDitamapConfiguration.pass_word();
    }

    @Override
    public void initiatePublishingProcess(String publishingPoint, ResourceResolver resourceResolver, String outputType, Externalizer externalizer) {
        if (null == publishingPoint) {
            LOG.error(" Publish Ditamap Service :: publishDitamaps");
            return;
        }

        final List<Header> header = getHeaders();

        final String publishingURL = PublishingUtil.getAPIUrl(madisonDomainsService.getDefaultDomain(), outputType, externalizer, MadisonConstants.GENERATE_OUTPUT);
        final RequestConfig requestConfig = setConnectionData();

        if (StringUtils.isBlank(publishingURL)) {
            LOG.debug(" Error generating Output API URL");
            return;
        }

            LOG.info(" Start. Auto-Publishing for {} ", publishingURL+publishingPoint);
            publishDitaMap(publishingURL, publishingPoint, requestConfig, header);

    }

    @Override
    public boolean publishDitaMap(final String publishingURL, final String ditaMap,
                                   final RequestConfig requestConfig, final List<Header> header) {
        boolean isPublishingStarted = Boolean.FALSE;
        CloseableHttpClient httpClient = null;
        try {
            final HttpGet httpGet = new HttpGet(publishingURL + ditaMap);
            httpClient = createAcceptSelfSignedCertificateClient(requestConfig, header);
            if(httpClient != null) {
                LOG.info("Site generation for DITAMAP {} started", ditaMap);
                HttpResponse httpResponse = httpClient.execute(httpGet);
                int statusCode = httpResponse.getStatusLine().getStatusCode();
                if (statusCode == HttpStatus.SC_OK) {
                    isPublishingStarted = Boolean.TRUE;
                    LOG.info("Site generating for DITAMAP {} in-progress", ditaMap);
                } else {
                    LOG.info("Publish Listener response is {} unexpected", statusCode);
                }
            }
        } catch (IOException e) {
            LOG.error("Error getting response from listener servlet {}", e);
        } catch (KeyManagementException e) {
            LOG.error("Error getting self signed SSL cert from listener servlet {}", e);
        } catch (NoSuchAlgorithmException e) {
            LOG.error("Error getting self signed SSL cert from listener servlet {}", e);
        } catch (KeyStoreException e) {
            LOG.error("Error getting self signed SSL cert from listener servlet {}", e);
        } finally {
            try {
                httpClient.close();
            } catch (IOException e) {
                LOG.error("Error closing the httpCliet connection {}", e);
            }
        }
        return isPublishingStarted;
    }

    @Override
    public RequestConfig setConnectionData() {
        return RequestConfig.custom().setConnectTimeout(DEFAULT_TIMEOUT)
                .setConnectionRequestTimeout(DEFAULT_TIMEOUT).setSocketTimeout(DEFAULT_TIMEOUT)
                .build();
    }


    @Override
    public boolean checkIfUserHasPermission(ResourceResolver resourceResolver, String assetPath, String permission) {
        boolean hasPermission = false;
        if (permission!=null) {
            if (permission.equals(WRITE)) {
                permission = Privilege.JCR_WRITE;
            } else {
                permission = Privilege.JCR_READ;
            }
        }
        try {
            Session userSession = resourceResolver.adaptTo(Session.class);
            AccessControlManager acMgr = userSession.getAccessControlManager();
            hasPermission = userSession.getAccessControlManager()
                    .hasPrivileges(assetPath, new Privilege[] { acMgr.privilegeFromName(permission) });
        } catch (RepositoryException e) {
            LOG.error("RepositoryException in PublishDitamapServiceImpl {}", e);
        }
        return hasPermission;
    }

    public List<Header> getHeaders() {
        // Add Basic Authorization header
        String credentials = publishingUserName + ":" + password;
        String base64Credentials = Base64.getEncoder().encodeToString(credentials.getBytes());

        List<Header> headers = new ArrayList<>();
        Header authorizationHeader = new BasicHeader("Authorization", "Basic " + base64Credentials);
        headers.add(authorizationHeader);
        return headers;
    }

    @Override
    public boolean isSiteGenerated(String json, String outputType, ResourceResolver resourceResolver) {
        boolean generated = Boolean.FALSE;
        final Session session = resourceResolver.adaptTo(Session.class);
        try {
            if (org.apache.commons.lang.StringUtils.isNotBlank(json)) {
                Gson gson = new Gson();
                GeneratedResponse response = gson.fromJson(json, GeneratedResponse.class);
                List<QueuedOutput> queues =  response.getQueuedOutputs();
                if (null != queues && queues.size() < 1 ) {
                    generated = Boolean.TRUE;
                } else if (null != queues ) {
                    for (QueuedOutput queueOutput: queues) {
                        if(session!=null && session.getUserID().equalsIgnoreCase(queueOutput.getInitiator())
                                && outputType.equalsIgnoreCase(queueOutput.getOutputType())) {
                            return generated;
                        }
                    }
                    // It means some other workflow pending or failed.
                    generated = Boolean.TRUE;
                }
            }
        } catch (JsonSyntaxException e) {
            LOG.error("Error parsing json",e);
        }

        return generated;
    }

    @Override
    public String fetchPublishingStatus(String ditaMap, String outputType,
                                        Externalizer externalizer, ResourceResolver resourceResolver) {
        String json = org.apache.commons.lang.StringUtils.EMPTY;
        CloseableHttpClient httpClient = null;

        final RequestConfig requestConfig = setConnectionData();
        final String beaconURL = PublishingUtil.getAPIUrl(madisonDomainsService.getDefaultDomain(), outputType, externalizer, MadisonConstants.PUBLISH_BEACON);
        try {
            final HttpGet httpGet = new HttpGet(beaconURL + ditaMap);
            httpClient = createAcceptSelfSignedCertificateClient(requestConfig, getHeaders());
            if(httpClient != null) {
                HttpResponse httpResponse = httpClient.execute(httpGet);
                int statusCode = httpResponse.getStatusLine().getStatusCode();
                if (statusCode == HttpStatus.SC_OK) {
                    HttpEntity entity = httpResponse.getEntity();
                    Header encodingHeader = entity.getContentEncoding();
                    Charset encoding = encodingHeader == null ? StandardCharsets.UTF_8
                            : Charsets.toCharset(encodingHeader.getValue());
                    json = EntityUtils.toString(entity, encoding);
                    LOG.debug("Response {}", json);
                }  else {
                    LOG.info("Publish Listener response is {} unexpected", statusCode);
                }
            }
        } catch (Exception e) {
            LOG.error("Error getting PublishDitamapServiceImpl {}", e);
        } finally {
            try {
                httpClient.close();
            } catch (IOException e) {
                LOG.error("Error closing the httpCliet connection {}", e);
            }
        }
        return json;
    }

    @Override
    public void generateRevisionAndSetDocState(String ditaMap, ResourceResolver resolver) {

        try {
            LOG.info(" Creating revision and setting document status for ditamap -> "+ditaMap);

            final Session session = resolver.adaptTo(Session.class);
            // Fetch all the topics from given ditamap.
            final List<String> topics = BulkDitaUtil.fetchAllTopicsFromDitamap(ditaMap, session);

            // Set last published date for all the topics
            String lastPublished = BulkDitaUtil.currentDate();
            BulkDitaUtil.setBulkLastPublishedDate(topics, lastPublished, session);

            // Create a revision and set label as Published
            BulkDitaUtil.createBulkRevision(DITAConstants.DITA_DOCUMENTSTATE_DONE, "Auto-Published",  topics, session, resolver);

            // Update the DITA Document Status to Published.
            BulkDitaUtil.setBulkDocStatus(topics, DITAConstants.DITA_DOCUMENTSTATE_DONE, session);

            LOG.info(Thread.currentThread().getName() + " Created revision and setting document status for ditamap -> "+ditaMap);
        } catch (IllegalStateException | RepositoryException e) {
            LOG.error(Thread.currentThread().getName() + " Error creating the revision and setting document status for ditamap -> "+ditaMap);
            e.printStackTrace();
        }
    }

    /**
     * @param requestConfig
     * @param header
     * @return
     * @throws KeyManagementException
     * @throws NoSuchAlgorithmException
     * @throws KeyStoreException
     */
    private CloseableHttpClient createAcceptSelfSignedCertificateClient(RequestConfig requestConfig, List<Header> header)
            throws KeyManagementException, NoSuchAlgorithmException, KeyStoreException  {
        if (header != null && requestConfig != null) {
            // use the TrustSelfSignedStrategy to allow Self Signed Certificates
            SSLContext sslContext = SSLContextBuilder.create().loadTrustMaterial(new TrustSelfSignedStrategy()).build();
            // disable hostname verification.
            HostnameVerifier allowAllHosts = new NoopHostnameVerifier();
            // create an SSL Socket Factory to use the SSLContext with the trust self signed certificate strategy
            // and allow all hosts verifier.
            SSLConnectionSocketFactory connectionFactory = new SSLConnectionSocketFactory(sslContext, allowAllHosts);
            return HttpClients.custom().setSSLSocketFactory(connectionFactory).setDefaultHeaders(header)
                    .setDefaultRequestConfig(requestConfig).build();
        }
        return null;
    }

    @ObjectClassDefinition(name = "PwC Viewpoint Syndication Auto-Publishing Configuration")
    public @interface PublishDitamapConfiguration {

        @AttributeDefinition(name = "Publisher Username",
                description = "Username of the user who will be responsible for initiating the publishing process.")
        String publishing_username();

        @AttributeDefinition(name = "Publisher Passcode",
                description = "Password for the above user in the crypto format.")
        String pass_word();

    }

    public boolean isPublishingPoint(ResourceResolver resourceResolver, Resource ditaMapResource){
        Resource metadataRes = resourceResolver.getResource(ditaMapResource.getPath() + MadisonConstants.METADATA_PATH);
        Resource presetRes = resourceResolver.getResource(ditaMapResource.getPath() + DITAConstants.AEMSITE_PRESETS_NODE);
        ValueMap valueMap = metadataRes.getValueMap();
        boolean isPublishingPoint = false;
        if (valueMap.containsKey(DITAConstants.PN_IS_PUBLISHING_POINTS)) {
            String isPubPoint = valueMap.get(DITAConstants.PN_IS_PUBLISHING_POINTS, String.class);
            isPublishingPoint = isPubPoint.equals("yes");
        }
        return isPublishingPoint;
    }

    public void sendAutoPublishCompletionNotifications(final String publishingPointPath, final ResourceResolver resourceResolver) {
        Resource publishingPointResource = resourceResolver.getResource(publishingPointPath);
        if (publishingPointResource != null) {
            String inboxNotificationMessage = "Auto-Publish completed for following ditamap \"" + publishingPointPath + "\". Please visit the ditamap and check the status.";

            try {
                final String title = "Auto-Pub Scheduler - " + publishingPointResource.getName();
                final String defaultDescription = title + " has been published.";
                final String territoryCode = MadisonUtil.getTerritoryCodeForPath(publishingPointResource.getPath());
                if (org.apache.commons.lang.StringUtils.isBlank(territoryCode)) {
                    return;
                }

                // parent author group (<territory>-madison-author)
                final String territoryAuthorGroupName = territoryCode + "-" + MadisonConstants.MADISON_PUBLISHER;

                // get all the territory author groups
                final Set<Authorizable> authorizables = SyndicationUtil.getTerritoryGroups(publishingPointResource, resourceResolver,
                        territoryAuthorGroupName);

                if (null == authorizables || authorizables.isEmpty()) {
                    LOG.error("sendAutoPublishCompletionNotifications no authors configured for territory::: {}",
                            territoryCode);
                    return;
                }

                if (org.apache.commons.lang.StringUtils.isBlank(inboxNotificationMessage)) {
                    inboxNotificationMessage = defaultDescription;
                }

                for (final Authorizable group : authorizables) {
                    if (group.isGroup()) {
                        sendInboxNotification(inboxNotificationSender, resourceResolver, publishingPointResource, title,
                                inboxNotificationMessage, group.getID());
                    }
                }

            } catch (final RepositoryException | TaskManagerException e) {
                LOG.error("sendSyndicationNotifications Error  ", e);
            }
        }
    }

    /**
     * @param inboxNotificationSender
     * @param resourceResolver
     * @param publishingResource
     * @param title
     * @param message
     * @param principal
     * @throws TaskManagerException
     */
    public static void sendInboxNotification(final InboxNotificationSender inboxNotificationSender,
                                             final ResourceResolver resourceResolver, final Resource publishingResource, final String title,
                                             final String message, final String principal) throws TaskManagerException {
        if (null == inboxNotificationSender || null == resourceResolver || null == publishingResource) {
            return;
        }

        final InboxNotification inboxNotification = inboxNotificationSender.buildInboxNotification();
        inboxNotification.setTitle(title);
        inboxNotification.setAssignee(principal);
        inboxNotification.setMessage(message);
        inboxNotification.setContentPath(publishingResource.getPath());

        inboxNotificationSender.sendInboxNotification(resourceResolver, inboxNotification);
    }
}
