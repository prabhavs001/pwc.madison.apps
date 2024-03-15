import com.day.cq.commons.Externalizer;
import com.day.crx.security.token.TokenCookie;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.pwc.madison.core.beans.ForwardReference;
import com.pwc.madison.core.beans.ForwardReferencesReport;
import com.pwc.madison.core.util.MadisonUtil;
import com.pwc.madison.core.util.ReportUtils;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.version.Version;
import javax.jcr.version.VersionHistory;
import javax.jcr.version.VersionManager;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.day.cq.dam.api.Asset;
import com.day.cq.commons.jcr.JcrConstants;
import org.apache.http.message.BasicNameValuePair;

public class VersionLabelPurge {

    private VersionManager vm;
    private Session session;
    private String rootPath;
    private ResourceResolver resourceResolver;
    private Externalizer externalizer;
    private final Logger LOG = LoggerFactory.getLogger(this.getClass().getName());
    private int counter = 0;
    private int purgeCounter = 0;
    private boolean brokenLabelFound = false;
    private boolean dryRun = false;

    public VersionLabelPurge(ResourceResolver resourceResolver, String rootPath, Externalizer externalizer) {
        this.resourceResolver = resourceResolver;
        this.rootPath = rootPath;
        this.session = resourceResolver.adaptTo(Session.class);
        this.externalizer = externalizer;
        if (this.session != null) {
            try {
                this.vm = this.session.getWorkspace().getVersionManager();
            } catch (RepositoryException e) {
                LOG.error("Error getting version manager", e);
            }
        }
    }

    public void getInfo(out, HttpServletRequest request) {
        try {
            final long startTime = System.currentTimeMillis();
            if (StringUtils.isNotEmpty(this.rootPath)) {
                List<String> mapList = new ArrayList<>();
                Resource resource = resourceResolver.getResource(this.rootPath);
                if(null == resource){
                    out.println ("Resource not found: "+ this.rootPath);
                    return;
                }
                if(dryRun){
                    out.println ("Simulating broken version label purging of path" + rootPath);
                }else{
                    out.println ("Starting broken version label purging of path" + rootPath);
                }
                String endApi = getPostUrl(resourceResolver, externalizer, out);
                String cookieValue = getCookieByName(request, TokenCookie.NAME);
                if(StringUtils.isBlank(cookieValue)){
                    out.println ("Login cookie not found");
                    return;
                }
                if (StringUtils.isNotBlank(cookieValue)) {
                    URL url = new URL(endApi);
                    if(this.rootPath.endsWith(".ditamap")){
                        mapList.add(this.rootPath);
                    }else{
                        populateMapList(resource, mapList, out);
                    }
                    mapList.each{mapPath ->
                        processPurge(mapPath, url, endApi, cookieValue, out);
                    }
                }
                if(!dryRun){
                    out.println ("Number of broken version labels purged: "+ purgeCounter);
                }
                if(!dryRun){
                    out.println ("Number of assets traversed: "+ counter);
                }
                final long endTime = System.currentTimeMillis();
                out.println ("Time taken: "+ (endTime - startTime) / 1000 + " secs");
            }
        } catch (Exception e) {
            out.println ("Exception: "+ e.getMessage());
        }
    }

    private String getCookieByName(final HttpServletRequest request, String cookieName) {
        if (request != null) {
            final Cookie[] cookies = request.getCookies();
            if (cookies != null) {
                for (final Cookie cookie : cookies) {
                    if (cookie.getName().equalsIgnoreCase(cookieName)) {
                        return cookie.getValue();
                    }
                }
            }
        }
        return null;
    }

    private boolean isVersionable(String path) throws RepositoryException {
        if (this.session.nodeExists(path)) {
            Node node = session.getNode(path);
            return (node != null) && (node.isNodeType("{http://www.jcp.org/jcr/mix/1.0}versionable"));
        }
        return false;
    }

    private VersionHistory getPaths(String path) throws RepositoryException {
        if (null != vm && isVersionable(path)) {
            return vm.getVersionHistory(path);
        }
        return null;
    }

    private void populateMapList(Resource folderResource, List<String> mapList, out) throws RepositoryException {
        Iterator<Resource> iResource = folderResource.getChildren().iterator();
        while (iResource.hasNext()) {
            Resource child = iResource.next();
            if(child.getPath().endsWith("ditamap")){
                Asset map = child.adaptTo(Asset.class);
                if(null != map && null != map.getMetadata("pwc:isPublishingPoint") && map.getMetadata("pwc:isPublishingPoint").toString().equals("yes")){
                    mapList.add(child.getPath());
                }
            }
            populateMapList(child, mapList, out);
        }
    }

    private void processPurge(String mapPath, URL url, String endApi, String cookieValue, out){
        if(!dryRun){
            out.println ("Processing publishing point: "+ mapPath);
        }
        Map<String, Object> baseLineReferences = getBaseLineReferences(mapPath, url, endApi, cookieValue, out);
        if(null != baseLineReferences){
            for (Map.Entry<String, Object> entry: baseLineReferences.entrySet()){
                if(entry.getKey().endsWith(".dita") || entry.getKey().endsWith(".ditamap")){
                    purgeBrokenLabel(entry.getKey(), out, mapPath);
                    if(dryRun && brokenLabelFound){
                        break;
                    }
                    counter++;
                }
            }
        }
    }

    private void purgeBrokenLabel(String path, out, String mapPath) throws RepositoryException {
        brokenLabelFound = false;
        VersionHistory vHistory = getPaths(path);
        if (vHistory != null) {
            String[] versionLabels = vHistory.getVersionLabels();
            for (String label : versionLabels) {
                try {
                    Version version = vHistory.getVersionByLabel(label);
                } catch (Exception e) {
                    brokenLabelFound = true;
                    if (dryRun) {
                        out.println (mapPath);
                        break;
                    }
                    out.println(path);
                    out.println("Purged broken Label: " + label);
                    purgeCounter++;
                    if (!dryRun) {
                        vHistory.removeVersionLabel(label);
                    }
                }
            }
        }
    }

    private Map<String, Object> getBaseLineReferences(String mapPath, URL url, String endApi,
                                                      String cookieValue, out){
        List<BasicNameValuePair> postParams = new ArrayList<>();
        postParams.add(new BasicNameValuePair("sourcePath", mapPath));
        postParams.add(new BasicNameValuePair(":operation", "getversiondata"));
        return getVersionData(endApi, cookieValue,
                url.getHost(), postParams, 5000000, out);
    }

    private Map<String, Object> getVersionData(String url, String cookie, String domain,
                                               List<BasicNameValuePair> params, int readTimeOut, out){
        String json = ReportUtils.getListenerResponse(url, cookie, domain, params, readTimeOut);
        try {
            if (org.apache.commons.lang.StringUtils.isNotBlank(json)) {
                Gson gson = new Gson();
                return gson.fromJson(json, Map.class);
            }
        } catch (JsonSyntaxException e) {
            LOG.error("Error parsing json", e);
        }
        return null;
    }

    private String getPostUrl(ResourceResolver resourceResolver, Externalizer externalizer, out) {
        String postRequestApiEndPoint = org.apache.commons.lang.StringUtils.EMPTY;
        if (externalizer != null) {
            postRequestApiEndPoint = externalizer.externalLink(resourceResolver, Externalizer.LOCAL,
                    "/bin/publishlistener");
        }
        return postRequestApiEndPoint;
    }

}

def out = getBinding().out;

def getFolderDetails(resourceResolver, path, Externalizer externalizer, HttpServletRequest request) {
    new VersionLabelPurge(resourceResolver, path, externalizer).getInfo(out, request);
    null
}

def save(resourceResolver) {
    resourceResolver.close();
}

def getInfo(path) {
    try {
        def resourceResolverFactory = osgi.getService(org.apache.sling.api.resource.ResourceResolverFactory.class);
        def externalizer = osgi.getService(com.day.cq.commons.Externalizer.class);
        resourceResolver = resourceResolverFactory.getAdministrativeResourceResolver();
        getFolderDetails(resourceResolver, path, externalizer, request);
    } finally {
        if (resourceResolver != null) {
            save(resourceResolver);
        }
    }
}

getInfo("/content/dam/pwc-madison/ditaroot/us/en/pwc/workflow/other-map.ditamap");