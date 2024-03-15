import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.security.Principal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;

import javax.jcr.AccessDeniedException;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.lock.LockException;
import javax.jcr.security.AccessControlEntry;
import javax.jcr.security.AccessControlException;
import javax.jcr.security.AccessControlList;
import javax.jcr.security.AccessControlManager;
import javax.jcr.security.AccessControlPolicy;
import javax.jcr.security.Privilege;
import javax.jcr.version.VersionException;

import org.apache.commons.lang3.StringUtils;
import org.apache.jackrabbit.api.security.JackrabbitAccessControlEntry;
import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.jackrabbit.api.security.user.Group;
import org.apache.jackrabbit.api.security.user.UserManager;
import org.apache.jackrabbit.commons.jackrabbit.authorization.AccessControlUtils;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.jcr.resource.api.JcrResourceConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.day.text.csv.Csv;

public class PrivateFolderAcl {
    private static final String OTHERS = "others";
    private static final String MAC_DEFAULT = "mac-default";
    private static final String DEFAULT = "default";
    private static final String OWNER = "owner";
    private static final String EDITOR = "editor";
    private ResourceResolver resourceResolver;
    private String path;
    private final Logger LOG = LoggerFactory.getLogger(this.getClass().getName());
    private Session session;
    private Set<String> allFolders = new HashSet<String>();
    private FileWriter fileWriter;
    private String fileSeparator = System.getProperty("file.separator");
    String absoluteFilePathBeforeMove = fileSeparator + "tmp" + fileSeparator +  "before-move-file.csv";
    String absoluteFilePathAfterMove = fileSeparator + "tmp" + fileSeparator + "after-move-file.csv";
    private boolean isMoveEnabled = true;
    private boolean isDryRun = true;
    private Set<String> parentPaths = new TreeSet<String>();

    PrivateFolderAcl(ResourceResolver resourceResolver, String path) {
        this.resourceResolver = resourceResolver;
        this.path = path;
        this.session = resourceResolver.adaptTo(Session.class);
    }

    public PrivateFolderAcl() {

    }

    private AccessControlEntry getDenyAllEveryoneACEFromAccessControlEntries(AccessControlEntry[] entries)
            throws RepositoryException {
        for (AccessControlEntry ace : entries) {
            boolean isEveryone = ace.getPrincipal()
                    .equals(AccessControlUtils.getEveryonePrincipal(this.resourceResolver.adaptTo(Session.class)));
            if (!isEveryone) {
                continue;
            }
            if (ace instanceof JackrabbitAccessControlEntry) {
                boolean isDenyACE = !((JackrabbitAccessControlEntry) ace).isAllow();
                if (!isDenyACE) {
                    continue;
                }
            }
            for (Privilege privilege : ace.getPrivileges()) {
                if (privilege.getName().equalsIgnoreCase("jcr:all")) {
                    return ace;
                }
            }
        }
        return null;
    }

    private void getDenyAllEveryoneACE(String path, Map<String, Map<String, List<Info>>> data)
            throws RepositoryException {
        AccessControlManager acm = this.session.getAccessControlManager();
        boolean isValidMove = false;
        for (AccessControlPolicy policy : acm.getPolicies(path)) {
            if (policy instanceof AccessControlList) {
                AccessControlList accessControlList = (AccessControlList) policy;
                AccessControlEntry[] entries = accessControlList.getAccessControlEntries();
                AccessControlEntry denyAllACE = getDenyAllEveryoneACEFromAccessControlEntries(entries);
                if (denyAllACE != null) {
                    LOG.info("Found private folder {}", path);
                    data.put(path, new HashMap<String, List<PrivateFolderAcl.Info>>());
                    isValidMove = getUsers(accessControlList, entries, resourceResolver.getResource(path).getName(),
                            data, path);
                    cleanPolicy(path, acm, isValidMove, accessControlList, denyAllACE);
                }
            }
        }
    }

    private void cleanPolicy(String path, AccessControlManager acm, boolean isValidMove,
            AccessControlList accessControlList, AccessControlEntry denyAllACE) throws RepositoryException,
            PathNotFoundException, AccessControlException, AccessDeniedException, LockException, VersionException {
        if (isValidMove) {
            LOG.info("Removing private access {}", path);
            removeAccess(accessControlList, denyAllACE);
            if (accessControlList.getAccessControlEntries().length < 1) {
                acm.removePolicy(path, accessControlList);
            } else {
                acm.setPolicy(path, accessControlList);
            }
        }
    }

    private boolean getUsers(AccessControlList accessControlList, AccessControlEntry[] entries, String pathName,
            Map<String, Map<String, List<Info>>> data, String path) throws RepositoryException {
        UserManager userManager = resourceResolver.adaptTo(UserManager.class);
        Info info = null;
        boolean isValidMove = false;
        for (AccessControlEntry ace : entries) {
            boolean isEveryone = ace.getPrincipal().equals(AccessControlUtils.getEveryonePrincipal(this.session));
            if (isEveryone) {
                continue;
            }
            LOG.debug("Principal {} pathName {}", ace.getPrincipal().getName(), pathName);
            String rege = "^mac-default(.*)";
            if (ace.getPrincipal().getName().matches(rege)) {
                String role = StringUtils.substringAfterLast(ace.getPrincipal().getName(), MAC_DEFAULT);
                LOG.debug("Role {}", role);
                if (role.contains(EDITOR)) {
                    info = fetchAce(data, path, userManager, ace, EDITOR);
                    isValidMove = doMove(path, EDITOR, info, accessControlList, ace);
                } else if (role.contains(OWNER)) {
                    info = fetchAce(data, path, userManager, ace, OWNER);
                    isValidMove = doMove(path, OWNER, info, accessControlList, ace);
                } else {
                    info = fetchAce(data, path, userManager, ace, DEFAULT);
                    isValidMove = doMove(path, DEFAULT, info, accessControlList, ace);
                }
            } else {
                info = fetchAce(data, path, userManager, ace, OTHERS);
            }
        }
        return isValidMove;
    }

    private Info fetchAce(Map<String, Map<String, List<Info>>> data, String path, UserManager userManager,
            AccessControlEntry ace, String role) throws RepositoryException {
        Info info = new Info();
        getDetails(ace, userManager, info);
        LOG.debug("Getting privilges for {}",ace.getPrincipal());
        info.setPrivileges(getPrivilegesFromACE(ace.getPrivileges()));
        if (data.get(path).containsKey(role)) {
            List<Info> listOwners = data.get(path).get(role);
            listOwners.add(info);
            data.get(path).put(role, listOwners);
        } else {
            List<Info> listOwners = new ArrayList<PrivateFolderAcl.Info>();
            listOwners.add(info);
            data.get(path).put(role, listOwners);
        }
        return info;
    }

    private Set<String> getPrivilegesFromACE(Privilege[] privileges) {
        Set<String> access = new TreeSet<String>();
        if (privileges != null && privileges.length > 0) {
            for (int i = 0; i < privileges.length; i++) {
                access.add(privileges[i].getName());
            }
        }
        return access;
    }

    private void getDetails(AccessControlEntry ace, UserManager userManager, Info info) throws RepositoryException {
        Group macFolderUserGroup = (Group) userManager.getAuthorizable(ace.getPrincipal());
        if (macFolderUserGroup != null) {
            Iterator<Authorizable> members = macFolderUserGroup.getDeclaredMembers();
            info.setMacName(ace.getPrincipal());
            info.setPrincipalName(ace.getPrincipal().getName());
            while (members.hasNext()) {
                Authorizable member = members.next();
                info.getMembers().add(member.getPrincipal().getName());
                info.getAuthorizables().add(member.getID());
            }
        }
    }

    private void getAllFolders(String path) {
        if (StringUtils.isNoneEmpty(path)) {
            Resource resource = resourceResolver.getResource(path);
            Iterator<Resource> itrRes = resource.listChildren();
            while (itrRes.hasNext()) {
                Resource currentResource = itrRes.next();
                if (currentResource.isResourceType(JcrResourceConstants.NT_SLING_FOLDER)) {
                    allFolders.add(currentResource.getPath());
                    if (currentResource.hasChildren()) {
                        getAllFolders(currentResource.getPath());
                    }
                }
            }
        }
    }

    private void getFolders(String path) {
        if (StringUtils.isNoneEmpty(path)) {
            Resource resource = resourceResolver.getResource(path);
            if (resource.isResourceType(JcrResourceConstants.NT_SLING_FOLDER)) {
                allFolders.add(resource.getPath());
                if (resource.hasChildren()) {
                    getAllFolders(resource.getPath());
                }
            }
        }
    }

    private File createFile(String fileName) {
        try {
            File file = new File(fileName);
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

    private Csv addCsv(String fileName) throws IOException {
        File file = createFile(fileName);
        if (file != null) {
            Csv csv = new Csv();
            fileWriter = new FileWriter(file);
            csv.writeInit(fileWriter);
            if (fileWriter != null && csv != null) {
                csv.writeRow("Path", "Role", "Group", "Access", "Members", "IsMoved", "Moved To");
                fileWriter.flush();
                return csv;
            }
        }
        return null;
    }

    private void addRow(Csv csv, String path, String role, List<Info> list) throws IOException {
        if (csv != null) {
            for (Info info : list) {
                csv.writeRow(path, role, info.getPrincipalName(), String.join(",", info.getPrivileges()),
                        String.join(",", info.getMembers()), Boolean.toString(info.isMove()), info.getMovedPath());
            }
            fileWriter.flush();
        }
    }

    private void closeCsv(Csv csv) throws IOException {
        if (csv != null) {
            csv.close();
        }
    }

    private boolean isPrivateFolder(String path) throws RepositoryException {
        AccessControlManager acm = this.session.getAccessControlManager();
        for (AccessControlPolicy policy : acm.getPolicies(path)) {
            if (policy instanceof AccessControlList) {
                AccessControlList accessControlList = (AccessControlList) policy;
                AccessControlEntry[] entries = accessControlList.getAccessControlEntries();
                AccessControlEntry denyAllACE = getDenyAllEveryoneACEFromAccessControlEntries(entries);
                if (denyAllACE != null) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean doMove(String path, String role, Info info, AccessControlList accessControlList,
            AccessControlEntry ace) throws RepositoryException {
        boolean isValidMove = false;
        if (isMoveEnabled) {
            isValidMove = addInParentGroups(path, role, info);
            if (isValidMove) {
                // remove ace from list
                removeAccess(accessControlList, ace);
            }
        }
        return isValidMove;
    }

    private void removeAccess(AccessControlList accessControlList, AccessControlEntry ace) throws RepositoryException {
        if (ace != null) {
            LOG.info("Removing access {}", ace.getPrincipal());
            accessControlList.removeAccessControlEntry(ace);
        }
    }

    private boolean addInParentGroups(String path, String role, Info info) {
        boolean isPrivateMove = false;
        if (isValidToMove(path)) {
            String parentFolderPath = getParentPath(path);
            parentPaths.add(parentFolderPath);
            try {
                if (isPrivateFolder(parentFolderPath) && !StringUtils.equalsIgnoreCase(parentFolderPath, path)) {
                    moveGroups(parentFolderPath, role, info);
                    // only remove acls from path if parent is valid and private
                    isPrivateMove = true;
                }
            } catch (RepositoryException e) {
                LOG.error("Error moving to parent folder", e);
            }
        }
        return isPrivateMove;
    }

    private void moveGroups(String parentPath, String childRole, Info info) throws RepositoryException {
        LOG.debug("Parent folder for move {}", parentPath);
        LOG.debug("Trying to move {} users {}", childRole, info);
        if (this.resourceResolver.getResource(parentPath) != null) {
            AccessControlManager acm = this.session.getAccessControlManager();
            UserManager userManager = resourceResolver.adaptTo(UserManager.class);
            for (AccessControlPolicy policy : acm.getPolicies(parentPath)) {
                if (policy instanceof AccessControlList) {
                    AccessControlList accessControlList = (AccessControlList) policy;
                    AccessControlEntry[] entries = accessControlList.getAccessControlEntries();
                    for (AccessControlEntry ace : entries) {
                        boolean isEveryone = ace.getPrincipal()
                                .equals(AccessControlUtils.getEveryonePrincipal(this.session));
                        if (isEveryone) {
                            continue;
                        }
                        String rege = "^mac-default(.*)";
                        if (ace.getPrincipal().getName().matches(rege)) {
                            String role = StringUtils.substringAfterLast(ace.getPrincipal().getName(), MAC_DEFAULT);
                            LOG.debug("Parent Role {}", role);
                            if (role.contains(EDITOR) && childRole.equalsIgnoreCase(EDITOR)) {
                                moveGroup(parentPath, userManager, info, ace.getPrincipal());
                            } else if (role.contains(OWNER) && childRole.equalsIgnoreCase(OWNER) && isParentOwner(parentPath, ace)) {
                                moveGroup(parentPath, userManager, info, ace.getPrincipal());
                            } else if (childRole.equalsIgnoreCase(DEFAULT)
                                    && (!role.contains(EDITOR) && !role.contains(OWNER))) {
                                moveGroup(parentPath, userManager, info, ace.getPrincipal());
                            }
                        }

                    }
                }
            }
        }

    }

    private boolean isParentOwner(String parentPath, AccessControlEntry ace) {
        return StringUtils.contains(ace.getPrincipal().getName(), this.resourceResolver.getResource(parentPath).getName());
    }

    private void moveGroup(String parentFolder, UserManager userManager, Info info, Principal principal)
            throws RepositoryException {
        LOG.debug("Parent principal {}", principal.getName());
        if (userManager != null && info != null && !info.isMove() && principal != null) {
            Group macFolderUserGroup = (Group) userManager.getAuthorizable(principal);
            if (macFolderUserGroup != null) {
                for (String member : info.getAuthorizables()) {
                    if (!macFolderUserGroup.isMember(userManager.getAuthorizable(member))) {
                        macFolderUserGroup.addMember(userManager.getAuthorizable(member));
                        LOG.info(String.format("Added %s to %s from %s", member, principal.getName(),
                                info.getMacName().getName()));
                    } else {
                        LOG.info(String.format("User %s of %s is already member of %s", member,
                                info.getMacName().getName(), principal.getName()));
                    }
                }
                info.setMovedPath(parentFolder);
                // mac group can be deleted in child folders so can be null
                if (info.getMacName() != null && this.resourceResolver
                        .getResource(userManager.getAuthorizable(info.getMacName()).getPath()) != null) {
                    String grp = userManager.getAuthorizable(info.getMacName()).getPath();
                    try {
                        this.resourceResolver.delete(this.resourceResolver.getResource(grp));
                        LOG.info("Deleted {} name {}", grp, info.getMacName());
                    } catch (PersistenceException e) {
                        LOG.error("Error deleting group {}", grp, e);
                    }
                }
                info.setMove(true);
            }
        }

    }

    private boolean isValidToMove(String path) {
        if (StringUtils.contains(path, "/") && StringUtils.containsNone(path)) {
            String[] tmp = path.split("/");
            if (tmp.length > 6) {
                LOG.debug("{} is valid to move", path);
                return true;
            }
        }
        LOG.debug("{} is Invalid path skip", path);
        return false;
    }

    private String getParentPath(String path) {
        String parentPath = StringUtils.EMPTY;
        if (StringUtils.contains(path, "/")) {
            String[] tmp = path.split("/");
            if (tmp.length > 6) {
                for (int i = 0; i <= 7; i++) {
                    parentPath = parentPath + tmp[i] + "/";
                }
            }
            parentPath = StringUtils.substringBeforeLast(parentPath, "/");

        }
        return parentPath;
    }

    private void saveSession() throws RepositoryException {
        if (this.session != null) {
            if (this.session.hasPendingChanges() && !isDryRun) {
                this.session.save();
            }
        }
    }

    public void getPrivateACL() throws RepositoryException {
        try {
            Map<String, Map<String, List<Info>>> data = new HashMap<String, Map<String, List<Info>>>();
            getFolders(path);
            LOG.info("allFolders size {}", allFolders.size());
            if (!allFolders.isEmpty()) {
                for (String pathToCheck : allFolders) {
                    getDenyAllEveryoneACE(pathToCheck, data);
                }
                
                cleanParentFolder();
            }
            
            Map<String, Map<String, List<Info>>> dataAfterMove = folderViewAfterMove();
            
            extractCSV(data,this.absoluteFilePathBeforeMove);
            extractCSV(dataAfterMove,this.absoluteFilePathAfterMove);
        } catch (RepositoryException re) {
            LOG.error("Error getting details", re);
        } catch (IOException e) {
            LOG.error("Error creating csv file", e);
        } finally {
            saveSession();
        }

    }

    private void cleanParentFolder() throws RepositoryException {
        if (!parentPaths.isEmpty()) {
            for (String parent : parentPaths) {
                if (this.resourceResolver.getResource(parent) != null) {
                    AccessControlManager acm = this.session.getAccessControlManager();
                    UserManager userManager = resourceResolver.adaptTo(UserManager.class);
                    for (AccessControlPolicy policy : acm.getPolicies(parent)) {
                        if (policy instanceof AccessControlList) {
                            AccessControlList accessControlList = (AccessControlList) policy;
                            AccessControlEntry[] entries = accessControlList.getAccessControlEntries();
                            for (AccessControlEntry ace : entries) {
                                boolean isEveryone = ace.getPrincipal()
                                        .equals(AccessControlUtils.getEveryonePrincipal(this.session));
                                if (isEveryone) {
                                    continue;
                                }
                                String rege = "^mac-default(.*)";
                                if (ace.getPrincipal().getName().matches(rege)) {
                                    String role = StringUtils.substringAfterLast(ace.getPrincipal().getName(),
                                            MAC_DEFAULT);
                                    if (role.contains(OWNER) && !isParentOwner(parent, ace) && isValidDeleteGroup(userManager,ace.getPrincipal())) {
                                        accessControlList.removeAccessControlEntry(ace);
                                    }
                                }

                            }
                            acm.setPolicy(parent, accessControlList);
                        }
                    }
                }
            }
        }
    }

    private boolean isValidDeleteGroup(UserManager userManager, Principal principal) {
        boolean isValidToDelete = false;
        if(userManager != null) {
            try {
                isValidToDelete = userManager.getAuthorizable(principal) == null;
                LOG.info("Owner {} in parent is valid to clear {}",principal.getName(),isValidToDelete);
                return isValidToDelete;
            } catch (RepositoryException e) {
                LOG.error("Error getting parent ace principal",e);
            }
        }
        return isValidToDelete;
        
    }

    private void extractCSV(Map<String, Map<String, List<Info>>> data,String fileName) throws IOException {
        if (!data.isEmpty()) {
            Csv csv = addCsv(fileName);
            for (Entry<String, Map<String, List<Info>>> dataEntry : data.entrySet()) {
                Map<String, List<Info>> value = dataEntry.getValue();
                for (Entry<String, List<Info>> roleDetails : value.entrySet()) {
                    addRow(csv, dataEntry.getKey(), roleDetails.getKey(), roleDetails.getValue());
                }
            }
            closeCsv(csv);
        }
    }

    private Map<String, Map<String, List<Info>>> folderViewAfterMove() throws RepositoryException {
        Map<String, Map<String, List<Info>>> dataAfterMove = new HashMap<String, Map<String, List<Info>>>();
        this.isMoveEnabled = false;
        if (!allFolders.isEmpty()) {
            for (String pathToCheck : allFolders) {
                getDenyAllEveryoneACE(pathToCheck, dataAfterMove);
            }
        }
        return dataAfterMove;
    }

    class Info {
        private Set<String> members = new TreeSet<String>();
        private Principal macName;
        private String principalName;
        private Set<String> authorizables = new TreeSet<String>();
        private boolean move;
        private String movedPath;
        private Set<String> privileges = new TreeSet<String>();

        public Set<String> getPrivileges() {
            return privileges;
        }

        public void setPrivileges(Set<String> privileges) {
            this.privileges = privileges;
        }

        public String getPrincipalName() {
            return principalName;
        }

        public void setPrincipalName(String principalName) {
            this.principalName = principalName;
        }

        public String getMovedPath() {
            return movedPath;
        }

        public void setMovedPath(String movedPath) {
            this.movedPath = movedPath;
        }

        public boolean isMove() {
            return move;
        }

        public void setMove(boolean move) {
            this.move = move;
        }

        public Principal getMacName() {
            return macName;
        }

        public void setMacName(Principal principal) {
            this.macName = principal;
        }

        public Set<String> getMembers() {
            return members;
        }

        public void setMembers(TreeSet<String> members) {
            this.members = members;
        }

        public Set<String> getAuthorizables() {
            return authorizables;
        }

        public void setAuthorizables(Set<String> authorizables) {
            this.authorizables = authorizables;
        }
    }
}


def getFolderDetails(resourceResolver, path) {
 new PrivateFolderAcl(resourceResolver, path).getPrivateACL();
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

getInfo("/content/dam/pwc-madison/ditaroot/us/en/sec");