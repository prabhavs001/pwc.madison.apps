package com.pwc.madison.core.models;

import com.adobe.granite.ui.components.rendercondition.RenderCondition;
import com.adobe.granite.ui.components.rendercondition.SimpleRenderCondition;
import com.pwc.madison.core.constants.MadisonConstants;
import com.pwc.madison.core.util.DITAUtils;
import com.pwc.madison.core.util.MadisonUtil;
import org.apache.commons.lang3.StringUtils;
import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.jackrabbit.api.security.user.Group;
import org.apache.jackrabbit.api.security.user.UserManager;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.request.RequestPathInfo;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.api.resource.ResourceUtil;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.OSGiService;
import org.apache.sling.models.annotations.injectorspecific.Self;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import java.util.Iterator;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Model(adaptables = SlingHttpServletRequest.class)
public class DitaPublishRenderCondition {

    public static final Logger LOGGER = LoggerFactory.getLogger(DitaPublishRenderCondition.class);

    @Self
    private SlingHttpServletRequest request;

    private ResourceResolver requestResolver;

    private ResourceResolver madisonServiceUserResolver;

    private boolean isRendered = false;

    private String territoryLevelResourcePath = StringUtils.EMPTY;

    @OSGiService
    private ResourceResolverFactory resolverFactory;

    @PostConstruct
    protected void init() {

        // The 'Generate' button on the DITA Publishing dashboard should be available only if the following conditions are met
        // First - The document has the status as "Approved" TBD (This is TBD)
        // Second - The publisher has been set as the editor/owner for a parent folder in the hierarchy under the territory
        // If there are no exclusive ownerships being set on any of the parent folder, the button will be available
        // for any body who is a publisher. (TBD : Need to think about a fix for this after discussion with Business)
        // button will also be available for the Administrator or admin users

        // first check if the logged in user is a member of the group 'publishers', which is the OOTB publisher group added by XML Add on for DITA

        requestResolver = request.getResourceResolver();

        // get the service resource resolver for reading the users under /home etc
        madisonServiceUserResolver = MadisonUtil.getResourceResolver(resolverFactory, MadisonConstants.MADISON_GENERIC_SUB_SERVICE);
        

        // get logged in user's authorizable ID
        Session jcrSession = requestResolver.adaptTo(Session.class);
        String loggedInUserId = jcrSession.getUserID();


        UserManager userManager = madisonServiceUserResolver.adaptTo(UserManager.class);

        try {
            // get the Authorizable for the logged in USer ID
            Authorizable user = userManager.getAuthorizable(loggedInUserId);

            Authorizable publisherGroup = ((Group) userManager.getAuthorizable(MadisonConstants.USER_GROUPS_DITA_PUBLISHERS));

            //Set the render condition as true for the admin user
            if (MadisonUtil.isAdmin(user, userManager)) {
                isRendered = true;
            }

            // make sure the user is part of the OOTB 'publishers' group first, the following code execute only for non admin users
            if (!isRendered && ((Group) publisherGroup).isMember(user)) {
                //read the Suffix Path info from the request when you are on the DITAMAP output generation dashboard
                //for exampple /libs/fmdita/report/report.html/content/dam/pwc-madison/SAMPLE_bankruptcies_and_liq/bankruptcies_and_liq.US.ditamap
                final RequestPathInfo requestPathInfo = request.getRequestPathInfo();

                String requestSuffixPath = requestPathInfo.getSuffix();

                if(StringUtils.isNotEmpty(requestSuffixPath)) {
                    Pattern pattern = Pattern.compile(MadisonConstants.MADISON_DAM_DITA_TERRITORY_ROOT);
                    Matcher matcher = pattern.matcher(requestSuffixPath);

                    if (matcher.find())
                    {
                        // retrieve hte territory dita root from the suffix path
                        territoryLevelResourcePath=matcher.group();
                    }
                }

                // continue further with the publish permission check etc , only if the suffix path is indicating a DITAMAP, you could figure this
                // out from the selector string
                if (StringUtils.isNoneEmpty(requestSuffixPath) && StringUtils.isNotEmpty(requestSuffixPath) && requestSuffixPath.contains(MadisonConstants.SLING_SELECTORS_DITAMAP)) {

                    // Check recursively on the Suffix path , any parent folder having the folder permission set exclusively, check if
                    // a 'publisher' group is having a editor or owner permission on a parent folder.
                    checkFolderPermissionSettings(requestSuffixPath, userManager, publisherGroup,user);

                    //once the above check has been completed and 'isRendered=true' , the additional condition to be verified, which is nothing but
                    // all topic references within the DITAMAP should have the status 'Approved'
                    if(isRendered) {
                        isRendered = DITAUtils.checkAllTopicRefsApproved(requestSuffixPath, requestResolver);
                    }
                }
            }
        }catch(RepositoryException e){
            LOGGER.error("Exception In Render Condition::",e);
        }finally{
            // close the service user resolver
            if(madisonServiceUserResolver!=null && madisonServiceUserResolver.isLive()){
                madisonServiceUserResolver.close();
            }
        }

        // set the render condition appropriately
        request.setAttribute(RenderCondition.class.getName(), new SimpleRenderCondition(isRendered));
    }

    private void checkFolderPermissionSettings(String requestSuffixPath, UserManager userManager, Authorizable publisherGroup, Authorizable loggedInuser) throws RepositoryException {

        // continue to the access check with the request resolver
        Resource currentResource = requestResolver.getResource(requestSuffixPath);

        if(currentResource == null){
            return;
        }
        //check if the group 'publishers' is having the required permission on this folder
        // which means any of the members of 'publisher' group could be added as viewer/editor or owner to this folder
        //
        Set<Authorizable> allPublishers = MadisonUtil.checkIfGroupHasPermissionOnFolder(currentResource,userManager,publisherGroup,madisonServiceUserResolver,StringUtils.EMPTY, false);
        boolean hasPermission = false;
        //if the returned groups are non null, make sure the logged in user is part of any of those groups to enable the render condition as true.
        if(allPublishers!=null && !allPublishers.isEmpty()){
            Iterator<Authorizable> groupIter = allPublishers.iterator();
            while(groupIter.hasNext()){
                Group memberPublisher = (Group) groupIter.next();
                if(memberPublisher.isMember(loggedInuser)){ hasPermission = true;
                    break;
                }
            }
        }
        if(hasPermission){
           isRendered= true;
        }

        Resource parentResource = currentResource.getParent();
        // proceed only if the current folder doesn't have the render permission also the recursion stops at the territory level
        if (!isRendered && parentResource!=null && !parentResource.getPath().equals(territoryLevelResourcePath)) {
            checkFolderPermissionSettings(parentResource .getPath(), userManager, publisherGroup, loggedInuser);
        }
    }

}
