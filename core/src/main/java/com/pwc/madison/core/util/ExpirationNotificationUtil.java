package com.pwc.madison.core.util;

import com.adobe.acs.commons.notifications.InboxNotificationSender;
import com.adobe.granite.taskmanagement.TaskManagerException;
import com.day.cq.search.result.Hit;
import com.pwc.madison.core.constants.DITAConstants;
import com.pwc.madison.core.constants.MadisonConstants;
import com.pwc.madison.core.services.ExpirationNotificationConfigService;
import org.apache.commons.lang3.StringUtils;
import org.apache.jackrabbit.JcrConstants;
import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.jackrabbit.api.security.user.Group;
import org.apache.jackrabbit.api.security.user.UserManager;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.RepositoryException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class ExpirationNotificationUtil {

	private static final Logger LOGGER = LoggerFactory.getLogger(ExpirationNotificationUtil.class);

	public static void closeResourceResolver(ResourceResolver resourceResolver) {
		// close the service user resolver
		if (null != resourceResolver && resourceResolver.isLive()) {
			resourceResolver.refresh();
			try {
				resourceResolver.commit();
			} catch (PersistenceException persistenExc) {
				LOGGER.error(
						"ExpirationNotificationUtil closeResourceResolver() : Exception while closing the resource resolver {}",
						persistenExc);
			}
			resourceResolver.close();
		}
	}

	public static String getGroupNameForFolder(final String groupNameSuffix, final String assetPath) {
		String groupName = StringUtils.EMPTY;
		if (null != assetPath && !StringUtils.EMPTY.equals(assetPath)) {
			String languageCode = MadisonUtil.getTerritoryCodeForPath(assetPath);
			StringBuilder groupNameStrBuilder = new StringBuilder(languageCode);
			groupNameStrBuilder.append(groupNameSuffix);
			groupName = groupNameStrBuilder.toString();
		}

		return groupName;
	}

	/**
	 * Utility method to check if a given Group has either editor permission on
	 * a given folder.
	 *
	 * @param resourceResolver
	 * @param assetPath
	 * @param groupNameSuffix
	 * @return
	 */
	public static Set<Authorizable> getAllowedMembers(final ResourceResolver resourceResolver, final String assetPath,
			final String groupNameSuffix) {
		LOGGER.debug("Inside getAllowedMembers");
		Set<Authorizable> allAllowedMembers = new HashSet<Authorizable>();
		Resource assetRes = resourceResolver.getResource(assetPath);
		Resource parentResource = assetRes.getParent();
		String parentResourceType = (String) parentResource.getValueMap().getOrDefault(JcrConstants.JCR_PRIMARYTYPE,
				StringUtils.EMPTY);

		String parentGroupId = getGroupNameForFolder(groupNameSuffix, assetPath);

		LOGGER.debug("Parent Group Name" + parentGroupId);

		if (parentResourceType.contains(MadisonConstants.STR_FOLDER) && !StringUtils.EMPTY.equals(parentGroupId)) {
			final UserManager userManager = resourceResolver.adaptTo(UserManager.class);
			try {
				Authorizable parentGroupMember = userManager.getAuthorizable(parentGroupId.toString());
				LOGGER.debug("GroupMember Object" + parentGroupMember);
				allAllowedMembers = MadisonUtil.getTerritoryGroups(resourceResolver, parentResource, parentGroupId.toString(), true);
				LOGGER.debug("Set of Allowed Groups" + allAllowedMembers);
			} catch (RepositoryException repoExc) {
				LOGGER.error(
						"ExpirationNotificationUtil getAllowedMembers() : Exception while checking allowed members {}",
						repoExc);
			}
		}
		return allAllowedMembers;
	}

	public static Map<Authorizable, List<String>> setGroupsMap(Map<Authorizable, List<String>> groupsMap, final Set<Authorizable> allAllowedMembers,
			final String assetPath) {
		if (allAllowedMembers.size() >= 1) {
			Iterator<Authorizable> allowedMemberItr = allAllowedMembers.iterator();
			while (allowedMemberItr.hasNext()) {
				Authorizable allowedMember = allowedMemberItr.next();
				if (null != groupsMap && groupsMap.containsKey(allowedMember)) {
					List<String> assetPaths = groupsMap.get(allowedMember);
					assetPaths.add(assetPath);
					groupsMap.put(allowedMember, assetPaths);
				} else {
					List<String> assetPaths = new ArrayList<String>();
					assetPaths.add(assetPath);
					groupsMap.put(allowedMember, assetPaths);
				}
			}
		}
		return groupsMap;
	}

	/**
	 * Find all the expiration groups who have access to Expiring DITA Topics
	 *
	 * @return groups versus expiring DITA topics map
	 */
	public static Map<Authorizable, List<String>> findExpirationGroups(
			final ResourceResolverFactory resourceResolverFactory, final List<Hit> expiringAssets,
			final String parentGroupNameSuffix, final String fmditaServiceName) {

		LOGGER.debug("Inside findExpirationGroups");

		Map<Authorizable, List<String>> expirationGroupsMap = new HashMap<Authorizable, List<String>>();
		ResourceResolver resourceResolver = MadisonUtil.getResourceResolver(resourceResolverFactory,
				fmditaServiceName);
		if (null == expiringAssets) {
			return expirationGroupsMap;
		}

		try {
			Iterator<Hit> expiringAssetsItr = expiringAssets.iterator();
			while (expiringAssetsItr.hasNext()) {
				Hit hit = expiringAssetsItr.next();
				String assetPath = hit.getNode().getPath();
				if (!assetPath.equals(StringUtils.EMPTY)) {
					Set<Authorizable> allAllowedMembers = getAllowedMembers(resourceResolver, assetPath,
							parentGroupNameSuffix);
					expirationGroupsMap = setGroupsMap(expirationGroupsMap, allAllowedMembers, assetPath);
				}
			}
		} catch (RepositoryException repoException) {
			LOGGER.error(
					"ExpirationReportScheduler findExpirationGroups() : Exception while finding expiration groups {}",
					repoException);
		}finally {
			if(null != resourceResolver){
				resourceResolver.close();
			}
		}

		return expirationGroupsMap;

	}

	public static void sendInboxNotification(final ResourceResolverFactory resourceResolverFactory,
			final ExpirationNotificationConfigService expirationNotificationConfig,
			final InboxNotificationSender inboxNotificationSender, final Map<Authorizable, List<String>> approversMap, final String fmditaServiceName) {

		LOGGER.debug("Inside sendInboxNotification");
		if (null == approversMap) {
			// do nothing
			return;
		}

		ResourceResolver resourceResolver = MadisonUtil.getResourceResolver(resourceResolverFactory,
				fmditaServiceName);

		Set<Authorizable> approvers = approversMap.keySet();
		LOGGER.debug("set of approvers" + approvers);
		Iterator<Authorizable> approversItr = approvers.iterator();
		try {
			while (approversItr.hasNext()) {
				Authorizable approver = approversItr.next();
				List<String> approvedRejectedContentList = approversMap.get(approver);
				if (!approver.isGroup()) {
					String approverId = approver.getID();
					LOGGER.debug("Approver ID" + approverId);
					if (!approverId.equals(StringUtils.EMPTY)) {
						// send inbox notification to approvers
						LOGGER.debug("Prepare Inbox Notification");
						SyndicationUtil.sendInboxNotification(inboxNotificationSender, resourceResolver,
								expirationNotificationConfig.getDitaExpirationReportLink(),
								expirationNotificationConfig.getInboxTitle(),
								expirationNotificationConfig.getInboxMessage(), approverId, approvedRejectedContentList);
					}
				}
			}
		} catch (RepositoryException repoException) {
			LOGGER.error("ExpirationReportScheduler sendNotifications() : Exception while getting authorizables {}",
					repoException);
		} catch (TaskManagerException taskManagerEx) {
			LOGGER.error(
					"ExpirationReportScheduler sendNotifications() : Exception while sending inbox notification {}",
					taskManagerEx);
		} finally {
			closeResourceResolver(resourceResolver);
		}
	}

	public static Set<Authorizable> getUserMembers(Group approverUserGroup) {
		Set<Authorizable> approvers = new HashSet<Authorizable>();

		if (null == approverUserGroup) {
			return approvers;
		}
		try {
			Iterator<Authorizable> approverGroupMembers = approverUserGroup.getMembers();
			while (approverGroupMembers.hasNext()) {
				approvers.add(approverGroupMembers.next());
			}
		} catch (RepositoryException repoException) {
			LOGGER.error(
					"ExpirationNotificationUtil getUserMembers() : Exception while retrieving user members of a group {}",
					repoException);
		}

		return approvers;
	}
}
