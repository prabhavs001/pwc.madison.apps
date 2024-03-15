package com.pwc.madison.core.util;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.List;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;

import org.apache.commons.io.Charsets;
import org.apache.commons.lang.StringUtils;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.client.CookieStore;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.util.EntityUtils;
import org.apache.sling.api.SlingHttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.day.crx.security.token.TokenCookie;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.pwc.madison.core.beans.BackwardReferencesReport;
import com.pwc.madison.core.beans.ForwardReferencesReport;
import com.pwc.madison.core.beans.PublishListenerReport;

public class ReportUtils {

    private static final Logger LOG = LoggerFactory.getLogger(BrokenLinkReportUtils.class);

    private static final String ERROR_FROM_API_CALL = "Error getting responsef from listener servlet {}";

    public static PublishListenerReport getPublishListenerReport(String url, String cookie,
            String domain, List<BasicNameValuePair> params, int readTimeOut) {
        String json = getListenerResponse(url, cookie, domain, params, readTimeOut);
        try {
            if (StringUtils.isNotBlank(json)) {
                Gson gson = new Gson();
                return gson.fromJson(json, PublishListenerReport.class);
            }
        } catch (JsonSyntaxException e) {
            LOG.error("Error parsing json", e);
        }

        return null;

    }

    public static BackwardReferencesReport getBackwardReferencesReport(String url, String cookie, String domain,
            List<BasicNameValuePair> params, int readTimeOut) {
        String json = getListenerResponse(url, cookie, domain, params, readTimeOut);
        try {
            if (StringUtils.isNotBlank(json)) {
                Gson gson = new Gson();
                return gson.fromJson(json, BackwardReferencesReport.class);
            }
        } catch (JsonSyntaxException e) {
            LOG.error("Error parsing json", e);
        }

        return null;

    }

    public static ForwardReferencesReport getForwardReferencesReport(String url, String cookie, String domain,
            List<BasicNameValuePair> params, int readTimeOut) {
        String json = getListenerResponse(url, cookie, domain, params, readTimeOut);
        try {
            if (StringUtils.isNotBlank(json)) {
                Gson gson = new Gson();
                return gson.fromJson(json, ForwardReferencesReport.class);
            }
        } catch (JsonSyntaxException e) {
            LOG.error("Error parsing json", e);
        }

        return null;

    }

    /**
     * Get dita report response
     * 
     * @param url
     * @param cookie
     * @param paths
     * @return
     */
	private static String getListenerResponse(String url, String cookie, String domain, List<BasicNameValuePair> params,
			int readTimeOut) {
		String json = StringUtils.EMPTY;
		try {
			RequestConfig requestConfig = setConnectionData(readTimeOut);
			HttpPost httpPost = new HttpPost(url);
			httpPost.setEntity(new UrlEncodedFormEntity(params));
			CookieStore cookieStore = setCookieData(cookie, domain);
			try (CloseableHttpClient httpClient = createAcceptSelfSignedCertificateClient(cookieStore, requestConfig)) {
				if (httpClient != null) {
					try (CloseableHttpResponse httpResponse = httpClient.execute(httpPost)) {
						int statusCode = httpResponse.getStatusLine().getStatusCode();
						LOG.info("Publish Listener response {}", statusCode);
						if (statusCode == SlingHttpServletResponse.SC_OK) {
							HttpEntity entity = httpResponse.getEntity();
							Header encodingHeader = entity.getContentEncoding();
							Charset encoding = encodingHeader == null ? StandardCharsets.UTF_8
									: Charsets.toCharset(encodingHeader.getValue());
							json = EntityUtils.toString(entity, encoding);
							LOG.debug("Response {}", json);
						}
					}
				}
			}
		} catch (IOException | KeyManagementException | NoSuchAlgorithmException | KeyStoreException e) {
			LOG.error(ERROR_FROM_API_CALL, e);
		}
		return json;

	}

    private static CloseableHttpClient createAcceptSelfSignedCertificateClient(CookieStore cookieStore,
            RequestConfig requestConfig) throws KeyManagementException, NoSuchAlgorithmException, KeyStoreException {
        if (cookieStore != null && requestConfig != null) {
            // use the TrustSelfSignedStrategy to allow Self Signed Certificates
            SSLContext sslContext = SSLContextBuilder.create().loadTrustMaterial(new TrustSelfSignedStrategy()).build();
            // disable hostname verification.
            HostnameVerifier allowAllHosts = new NoopHostnameVerifier();
            // create an SSL Socket Factory to use the SSLContext with the trust self signed certificate strategy
            // and allow all hosts verifier.
            SSLConnectionSocketFactory connectionFactory = new SSLConnectionSocketFactory(sslContext, allowAllHosts);
            LOG.info("Created http client");
            return HttpClients.custom().setSSLSocketFactory(connectionFactory).setDefaultCookieStore(cookieStore)
                    .setDefaultRequestConfig(requestConfig).build();
        }
        return null;
    }

    /**
     * 
     * @return
     */
    private static RequestConfig setConnectionData(int readTimeOut) {
        return RequestConfig.custom().setConnectTimeout(readTimeOut).setConnectionRequestTimeout(readTimeOut)
                .setSocketTimeout(readTimeOut).build();
    }

    /**
     * 
     * @param cookie
     * @param domain
     * @return
     */
    private static CookieStore setCookieData(String cookie, String domain) {
        CookieStore cookieStore = new BasicCookieStore();
        BasicClientCookie loginToken = new BasicClientCookie(TokenCookie.NAME, cookie);
        loginToken.setDomain(domain);
        loginToken.setPath("/");
        cookieStore.addCookie(loginToken);
        return cookieStore;
    }


}
