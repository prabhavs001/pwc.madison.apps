package com.pwc.madison.core.thread;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;

import com.pwc.madison.core.services.PublishDitamapService;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.io.Charsets;
import org.apache.commons.lang.StringUtils;
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
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.util.EntityUtils;
import org.apache.sling.api.resource.ResourceResolver;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.pwc.madison.core.beans.GeneratedResponse;
import com.pwc.madison.core.beans.Output;
import com.pwc.madison.core.beans.QueuedOutput;
import com.pwc.madison.core.util.BulkDitaUtil;

/**
 * Thread to invoke HttpGet for DITAMAP publishing.
 * 
 * @author vhs
 *
 */
public class HttpGetThread implements Runnable {

	private String publishingURL;
	private String outputType;
	private String ditaMap;
    private String initiator;
	private RequestConfig requestConfig;
	private ResourceResolver resourceResolver;
	private static final String PUBLISHED = "Published";

    @Reference
    PublishDitamapService publishDitamapService;
	
	private List<Header> header;
	private final Logger LOG = LoggerFactory.getLogger(HttpGetThread.class);
	
	public HttpGetThread(final ResourceResolver resourceResolver, final String publishingURL, final String ditaMap,
			final RequestConfig requestConfig, final List<Header> header, final String outputType, final String initiator) {
		this.resourceResolver = resourceResolver;
		this.outputType = outputType;
		this.publishingURL = publishingURL;
		this.ditaMap = ditaMap;
		this.requestConfig = requestConfig;
		this.header = header;
        this.initiator = initiator;
	}

	@Override
	public void run() {
		LOG.info(Thread.currentThread().getName() + " Start. Publishing = " +publishingURL+ditaMap);
		boolean published = publishDitaMap(publishingURL, ditaMap, requestConfig, header);
		if(published) {
			final String beaconURL = publishingURL.replaceFirst("GENERATEOUTPUT", "PUBLISHBEACON");
			String json = fetchPublishingStatus(beaconURL, ditaMap, requestConfig, header);

			boolean siteGenerated = isSiteGenerated(this.initiator, json);
			while(Boolean.FALSE == siteGenerated) {
				// Sleep for every 5min before checking the completion status.
				try {
                    LOG.info("Waiting for 2 min...");
					TimeUnit.MINUTES.sleep(2);
                    LOG.info("2 min completed.");
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
                LOG.info("Fetching Publishing Status again for "+ditaMap+" ditamap");
				json = fetchPublishingStatus(beaconURL, ditaMap, requestConfig, header);
				siteGenerated = isSiteGenerated(this.initiator, json);
			}
			// If it reaches here, it means site generation successful. 
			// Set the doc status, revision and published date only for AEM-Site output.
			if(outputType.equalsIgnoreCase("aemsite")) {
				generateRevisionAndSetDocState(ditaMap);
			}
			// Fetch the total time to generate the site for the given ditamap.
            Gson gson = new Gson();
            GeneratedResponse response = gson.fromJson(json, GeneratedResponse.class);
            List<Output> outputs = response.getOutputs();
            if(null != outputs && outputs.size() > 0) {
            	Output generatedSite = outputs.get(0);
            	float time = generatedSite.getGeneratedIn()/1000;
            	LOG.info(Thread.currentThread().getName() + " Total time of execution in seconds:: "+time);
            }
		}
		LOG.info(Thread.currentThread().getName() + " End. Publishing "+publishingURL+ditaMap);
	}

	/**
	 * Method to create a revision, published date and document status for all the topics of the given ditamap.
	 * 
	 * @param ditaMap
	 */
	private void generateRevisionAndSetDocState(final String ditaMap) {
		
		try {
			LOG.info(Thread.currentThread().getName() + " Creating revision and setting document status for ditamap -> "+ditaMap);
			
			final Session session = resourceResolver.adaptTo(Session.class);
			// Fetch all the topics from given ditamap.
			final List<String> topics = BulkDitaUtil.fetchAllTopicsFromDitamap(ditaMap, session);
			
			// Set last published date for all the topics
			String lastPublished = BulkDitaUtil.currentDate();
			BulkDitaUtil.setBulkLastPublishedDate(topics, lastPublished, session);
			
			// Create a revision and set label as Published
			BulkDitaUtil.createBulkRevision(PUBLISHED, "Bulk Incremented",  topics, session, resourceResolver);
			
			// Update the DITA Document Status to Published.
			BulkDitaUtil.setBulkDocStatus(topics, PUBLISHED, session);
			
			LOG.info(Thread.currentThread().getName() + " Created revision and setting document status for ditamap -> "+ditaMap);
		} catch (IllegalStateException | RepositoryException e) {
			LOG.error(Thread.currentThread().getName() + " Error creating the revision and setting document status for ditamap -> "+ditaMap);
			e.printStackTrace();
		}
	}
	/**
	 * @param json
	 * @return
	 */
	private boolean isSiteGenerated(final String initiatorUserId, final String json) {
		boolean generated = Boolean.FALSE;
		try {
            if (StringUtils.isNotBlank(json)) {
                Gson gson = new Gson();
                GeneratedResponse response = gson.fromJson(json, GeneratedResponse.class);
                List<QueuedOutput> queues =  response.getQueuedOutputs();
                if (null != queues && queues.size() < 1 ) {
                	generated = Boolean.TRUE;
                } else if (null != queues ) {
                	for (QueuedOutput queueOutput: queues) {
                		if(initiatorUserId.equalsIgnoreCase(queueOutput.getInitiator())
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
    /**
     * @param writer
     * @param publishingURL
     * @param ditaMap
     * @param requestConfig
     * @param header
     * @return
     */
    private String fetchPublishingStatus(final String publishingURL, final String ditaMap, 
    		final RequestConfig requestConfig, final List<Header> header) {
    	String json = StringUtils.EMPTY;
    	CloseableHttpClient httpClient = null;
    	try {
    		final HttpGet httpGet = new HttpGet(publishingURL + ditaMap);
			 httpClient = createAcceptSelfSignedCertificateClient(requestConfig, header);
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
		return json;
    }
    
	/**
     * @param publishingURL
     * @param ditaMap
     * @param cookieStore
     * @param requestConfig
     */
    private boolean publishDitaMap(final String publishingURL, final String ditaMap, 
    		final RequestConfig requestConfig, final List<Header> header) {
    	boolean published = Boolean.FALSE;
    	CloseableHttpClient httpClient = null;
    	try {
    		final HttpGet httpGet = new HttpGet(publishingURL + ditaMap);
			 httpClient = createAcceptSelfSignedCertificateClient(requestConfig, header);
			if(httpClient != null) {
				LOG.info("Site generation for DITAMAP {} started", ditaMap);
                HttpResponse httpResponse = httpClient.execute(httpGet);
                int statusCode = httpResponse.getStatusLine().getStatusCode();
                if (statusCode == HttpStatus.SC_OK) {
                	published = Boolean.TRUE;
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
		return published;
    }
    
    /**
     * @param requestConfig
     * @param header
     * @return
     * @throws KeyManagementException
     * @throws NoSuchAlgorithmException
     * @throws KeyStoreException
     */
    private CloseableHttpClient createAcceptSelfSignedCertificateClient(final RequestConfig requestConfig, final List<Header> header) 
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
}
