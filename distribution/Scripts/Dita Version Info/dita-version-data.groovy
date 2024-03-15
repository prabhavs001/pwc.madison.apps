import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.version.Version;
import javax.jcr.version.VersionHistory;
import javax.jcr.version.VersionIterator;
import javax.jcr.version.VersionManager;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.resource.ResourceResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.day.cq.commons.jcr.JcrConstants;
import com.day.cq.dam.api.Asset;
import com.day.cq.dam.api.DamConstants;
import com.day.cq.dam.commons.util.DamUtil;
import com.day.text.csv.Csv;

public class VersionData {
    private VersionManager vm;
    private Session session;
    private String rootPath;
    private ResourceResolver resourceResolver;
    private final Logger LOG = LoggerFactory.getLogger(this.getClass().getName());
    private FileWriter fileWriter;
    private String fileSeparator = System.getProperty("file.separator");
    String absoluteFilePath = fileSeparator + "tmp" + fileSeparator + "file-version-details.csv";
    String absoluteLabelFilePath = fileSeparator + "tmp" + fileSeparator + "file-count-by.csv";
    private  Map<String, List<Info>> map = new HashMap<String, List<Info>>();
    /* valid values 
       *createdBy 
       *label 
       *comments
     */
    private String countBy = "createdBy";
    
    
    public VersionData(ResourceResolver resourceResolver, String rootPath) {
        this.resourceResolver = resourceResolver;
        this.rootPath = rootPath;
        this.session = resourceResolver.adaptTo(Session.class);
        if (this.session != null) {
            try {
                this.vm = this.session.getWorkspace().getVersionManager();
            } catch (RepositoryException e) {
                LOG.error("Error getting version manager", e);
            }
        }
    }

    public void getAllVersions() throws IOException {
        Map<String, List<Info>> data = new HashMap<String, List<Info>>();
        getAllPathVesions(data);
        Csv csv = null;
        if (!data.isEmpty()) {
            extractCsv(data, csv,false);
        }
        
        if(!map.isEmpty()) {
            extractCsv(map, csv,true);
        }
        
    }

    private void extractCsv(Map<String, List<Info>> data, Csv csv,boolean isLabel) throws IOException {
        try {
            if(!isLabel) {
                csv = addCsv();
                for (Entry<String, List<Info>> dataEntry : data.entrySet()) {
                    addRow(csv, dataEntry.getValue(), dataEntry.getKey());
                }
            }
            else {
                csv = addLabelCsv();
                for (Entry<String, List<Info>> dataEntry : data.entrySet()) {
                    addLabelRow(csv, dataEntry.getValue(), dataEntry.getKey());
                }
            }
        } catch (IOException e) {
           LOG.error("Error in extractig to csv",e);
        }
        finally {
            closeCsv(csv);
        }
    }

    private void addLabelRow(Csv csv, List<Info> value, String key) throws IOException {
        if (csv != null && value != null) {
                csv.writeRow(key,Integer.toString(value.size()));
            fileWriter.flush();
        }
        
    }

    private File createFile(String absoluteFilePath) {
        try {
            File file = new File(absoluteFilePath);
            if (file.createNewFile()) {
                return file;
            } else {
                file.delete();
                if (file.createNewFile()) {
                    return file;
                }
            }
        } catch (Exception e) {
            LOG.error("Error creating file" + e);
        }
        return null;
    }

    private Csv addCsv() throws IOException {
        File file = createFile(this.absoluteFilePath);
        if (file != null) {
            Csv csv = new Csv();
            fileWriter = new FileWriter(file);
            csv.writeInit(fileWriter);
            if (fileWriter != null && csv != null) {
                csv.writeRow("Asset Path", "Version Path", "Version Label", "Version Comments", "Version Created By","Version Created Date");
                fileWriter.flush();
                return csv;
            }
        }
        return null;
    }
    
    private Csv addLabelCsv() throws IOException {
        File file = createFile(this.absoluteLabelFilePath);
        if (file != null) {
            Csv csv = new Csv();
            fileWriter = new FileWriter(file);
            csv.writeInit(fileWriter);
            if (fileWriter != null && csv != null) {
                csv.writeRow("Label", "Count");
                fileWriter.flush();
                return csv;
            }
        }
        return null;
    }

    private void addRow(Csv csv, List<Info> allVersions, String path) throws IOException {
        if (csv != null) {
            for (Info info : allVersions) {
                csv.writeRow(path, info.getVersionedNodePath(), String.join(",", info.getLabels()), info.getComments(),info.getVersionCreatedBy(),
                        info.getFormattedCreatedDate());
            }
            fileWriter.flush();
        }
    }

    private void closeCsv(Csv csv) throws IOException {
        if (csv != null) {
            csv.close();
        }
    }

    private void getAllPathVesions(Map<String, List<Info>> data) {
        Iterator<Asset> assetInFolder = DamUtil.getAssets(this.resourceResolver.getResource(rootPath));
        if (assetInFolder != null) {
            while (assetInFolder.hasNext()) {
                Asset asset = assetInFolder.next();
                if (asset != null && "application/xml".equals(asset.getMimeType())
                        && (asset.getName().endsWith(".dita") || asset.getName().endsWith(".ditamap"))) {
                    try {
                        List<Info> all = getVersionData(asset.getPath());
                        data.put(asset.getPath(), all);
                    } catch (RepositoryException e) {
                        LOG.error("Error getting version for path {}", asset.getPath(), e);
                    }
                }
            }
        }
    }

    private boolean isVersionable(String path) {
        try {
            if (this.session.nodeExists(path)) {
                Node node = session.getNode(path);
                return (node != null) && (node.isNodeType("{http://www.jcp.org/jcr/mix/1.0}versionable"));
            }
        } catch (RepositoryException e) {
            LOG.error("Error checkng verisonable mixim", e);
        }
        return false;
    }

    private VersionHistory getVersionHistoryFromPath(String path) throws RepositoryException {
        if (null != this.vm && isVersionable(path)) {
            return this.vm.getVersionHistory(path);
        }
        return null;
    }

    private List<Info> getVersionData(String path) throws RepositoryException {
        List<Info> allVerisonInfo = new ArrayList<Info>();
        VersionHistory versionHistory = getVersionHistoryFromPath(path);
        if (versionHistory != null) {
            VersionIterator vIterator = versionHistory.getAllVersions();
            while (vIterator.hasNext()) {
                Version baseVersion = this.session.getWorkspace().getVersionManager().getBaseVersion(path);
                Version currentVersion = vIterator.nextVersion();
                if (baseVersion != null && currentVersion != null
                        && !baseVersion.getPath().equals(currentVersion.getPath())
                        && !StringUtils.equals(JcrConstants.JCR_ROOTVERSION, currentVersion.getName())) {
                    Info info = getVersionInfo(currentVersion, path);
                    getLabelCount(info);
                    allVerisonInfo.add(info);
                }
            }
        }
        return allVerisonInfo;
    }

    private void getLabelCount(Info info){
        if(info != null) {
            String key = null;
            switch (countBy) {
                case "label":
                    extractLabelCount(info);
                    break;
                    
                case "comments":
                    key = info.getComments();
                    if(StringUtils.isNotBlank(key))
                        addLabelInfo(info, key);
                    else
                        addLabelInfo(info, "NO COMMENT");
                    break;
                    
                case "createdBy":
                    key = info.getVersionCreatedBy();
                    if(StringUtils.isNotBlank(key))
                        addLabelInfo(info, key);
                    else
                        addLabelInfo(info, "EMPTY");
                    break;    

                default:
                    break;
            }
        }
        
    }

    private void extractLabelCount(Info info) {
        String[] label = info.getLabels();
        if(label != null && label.length > 0) {
            for(int i=0;i<label.length;i++) {
                String key = label[i];
                addLabelInfo(info, key);
            }
        }
        else {
            //lable is empty
            String key = "NO LABEL";
            addLabelInfo(info, key);
        }
    }

    private void addLabelInfo(Info info, String key) {
        if(!map.containsKey(key)) {
            List<Info> allLabel = new ArrayList<VersionData.Info>();
            allLabel.add(info);
            map.put(key, allLabel);
        }
        else {
            List<Info> allLabel = map.get(key);
            allLabel.add(info);
            map.put(key, allLabel);
        }
    }
    
    private Info getVersionInfo(Version version, String path) throws RepositoryException {
        Info infoDetails = new Info(version);
        infoDetails.setAssetPath(path);
        return infoDetails;
    }

    class Info {
        private String assetPath;
        private String[] labels;
        private Calendar created;
        private String versionedNodePath;
        private Node frozenNode;
        private String comments;
        private SimpleDateFormat format = new SimpleDateFormat("dd/mm/yyyy");
        private String versionCreatedBy;
        
        public String getVersionCreatedBy() {
            return versionCreatedBy;
        }

        public Info(Version version) throws RepositoryException {
            this.versionedNodePath = version.getPath();
            this.labels = version.getContainingHistory().getVersionLabels(version);
            this.created = version.getCreated();
            this.frozenNode = version.getFrozenNode();
            setComments();
        }

        public String getFormattedCreatedDate() {
            return format.format(this.created.getTime());
        }

        public String getVersionedNodePath() {
            return versionedNodePath;
        }

        public String getAssetPath() {
            return assetPath;
        }

        public void setAssetPath(String assetPath) {
            this.assetPath = assetPath;
        }

        private void setComments() {
            try {
                if (this.frozenNode != null && frozenNode.getNode(JcrConstants.JCR_CONTENT) != null) {
                    Node jcrContent = frozenNode.getNode(JcrConstants.JCR_CONTENT);
                    if(jcrContent.hasProperty(DamConstants.PN_VERSION_COMMENT)) {
                        this.comments =  jcrContent.getProperty(DamConstants.PN_VERSION_COMMENT).getString();
                    }
                    if(jcrContent.hasProperty(DamConstants.PN_VERSION_CREATOR)) {
                        this.versionCreatedBy =  jcrContent.getProperty(DamConstants.PN_VERSION_CREATOR).getString();
                    }

                }
            } catch (RepositoryException e) {
                LOG.error("Error getting frozenNode jcrContent at {}", this.versionedNodePath, e);
            }
        }

        public String getComments() {
            return comments;
        }

        public String[] getLabels() {
            return labels;
        }

        public Calendar getCreated() {
            return created;
        }

    }
}


def getFolderDetails(resourceResolver, path) {
 new VersionData(resourceResolver, path).getAllVersions();
 null
}

def save(resourceResolver) {
 resourceResolver.close();
}

def getInfo(path) {
 try {
  def resourceResolverFactory = osgi.getService(org.apache.sling.api.resource.ResourceResolverFactory.class);
  resourceResolver = resourceResolverFactory.getAdministrativeResourceResolver();
  getFolderDetails(resourceResolver, path);
 } finally {
  if (resourceResolver != null) {
   save(resourceResolver);
  }
 }
}

getInfo("/content/dam/pwc-madison/ditaroot/us/en/pwc");