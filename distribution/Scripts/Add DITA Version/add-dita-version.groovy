import com.day.cq.commons.Externalizer;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.version.Version;
import javax.jcr.version.VersionHistory;
import javax.jcr.version.VersionManager;
import javax.jcr.lock.LockManager;

import javax.servlet.http.HttpServletRequest;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.day.cq.dam.api.Revision;
import com.day.cq.dam.api.Asset;

public class BaselineDefaultVersionUtil {

    private VersionManager vm;
    private Session session;
    private String rootPath;
    private ResourceResolver resourceResolver;
    private LockManager lockManager;
    private Externalizer externalizer;
    List<String> assetList = new ArrayList<String>();
    private final Logger LOG = LoggerFactory.getLogger(this.getClass().getName());

    public BaselineDefaultVersionUtil(ResourceResolver resourceResolver, String rootPath, Externalizer externalizer) {
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

    private void bulkCreateDefaultVersion(out, HttpServletRequest request, labelName, comment) {
        try {
            final long startTime = System.currentTimeMillis();
            if (StringUtils.isNotEmpty(this.rootPath)) {
                Resource resource = resourceResolver.getResource(this.rootPath);
                if(null == resource){
                    out.println ("Folder does not exist: "+ this.rootPath);
                    return;
                }
                createDefaultVersion(resource, out, labelName, comment);

            }
            final long endTime = System.currentTimeMillis();
            out.println ("Time taken: "+ (endTime - startTime) / 1000 + " secs");
        } catch (Exception e) {
            out.println ("Exception: "+ e.getMessage());
        }
    }

    private void createDefaultVersion(Resource folderResource, out, labelName, comment){
        
        Iterator<Resource> iResource = folderResource.getChildren().iterator();
        while (iResource.hasNext()) {
            Resource childResource = iResource.next();
            String childPath = childResource.getPath();
            if(childPath.endsWith("dita") || childPath.endsWith("ditamap")){
                VersionHistory vHistory;
                if (null != vm && isVersionable(childPath)) {
                    
                    vHistory = vm.getVersionHistory(childPath);
                }
                out.println("- " + childPath);
                if (vHistory != null) {

                    String[] versionLabels = vHistory.getVersionLabels();
                    out.println("Version Labels length before: "+versionLabels.length);
                    if(versionLabels.length<=0){
                        out.println("Creating labels...");
                        this.assetList.add(childPath);
                        createRevision(childPath, labelName, comment, childResource, session, out);
                    }
                    out.println("Version Labels length after: "+vHistory.getVersionLabels().length);
                }
            }
            if(childResource.hasChildren()){
                createDefaultVersion(childResource, out, labelName, comment);
            }
        }
    }

    private boolean isVersionable(String path) throws RepositoryException {
        if (this.session.nodeExists(path)) {
            Node node = session.getNode(path);
            return (node != null) && (node.isNodeType("{http://www.jcp.org/jcr/mix/1.0}versionable"));
        }
        return false;
    }

    public void createRevision(final String path, String label, final String comment, final Resource resource,
            final Session session, out) throws Exception {
        // Return if any of the mandatory arguments are null/empty
        if (null == resource || path.isEmpty() || null == session) {
            LOG.error("Error creating revision: resource/session/path is null");
            return;
        }
        if (label == null) {
            label = StringUtils.EMPTY;
        }
        lockManager = session.getWorkspace().getLockManager();
        final Asset asset = resource.adaptTo(Asset.class);
        // Null check for the asset.
        if (null == asset) {
            LOG.error("Error creating revision: asset adaption is null");
            return;
        }

        Revision revision;
        if (lockManager.isLocked(path)) {
            lockManager.unlock(path);
            revision = asset.createRevision(label, comment);
            lockManager.lock(path, true, false, Long.MAX_VALUE, session.getUserID());
        } else {
            revision = asset.createRevision(label, comment);
        }
    }
}

def out = getBinding().out;

def getFolderDetails(resourceResolver, path, Externalizer externalizer, HttpServletRequest request, labelName, comment) {
    new BaselineDefaultVersionUtil(resourceResolver, path, externalizer).bulkCreateDefaultVersion(out, request, labelName, comment);
    null
}

def save(resourceResolver) {
    resourceResolver.close();
}

def bulkCreateVersion(path, labelName, comment) {
    try {
        def resourceResolverFactory = getService(org.apache.sling.api.resource.ResourceResolverFactory.class);
        def externalizer = getService(com.day.cq.commons.Externalizer.class);
        resourceResolver = resourceResolverFactory.getAdministrativeResourceResolver();
        getFolderDetails(resourceResolver, path, externalizer, slingRequest, labelName, comment);
    } finally {
        if (resourceResolver != null) {
            save(resourceResolver);
        }
    }
}

bulkCreateVersion("/content/dam/pwc-madison/ditaroot/us/en/fasb/GAAP/Codification/Broad_Transactions/820/10/50", "1.0", "Default Version");
