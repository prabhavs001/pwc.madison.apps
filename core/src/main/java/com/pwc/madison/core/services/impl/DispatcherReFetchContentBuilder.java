/*
* Copyright 2013 Adobe Systems Incorporated
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
* http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/
package com.pwc.madison.core.services.impl;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.jcr.resource.api.JcrResourceConstants;
import org.osgi.framework.Constants;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.AttributeType;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.day.cq.replication.ContentBuilder;
import com.day.cq.replication.ReplicationAction;
import com.day.cq.replication.ReplicationActionType;
import com.day.cq.replication.ReplicationContent;
import com.day.cq.replication.ReplicationContentFactory;
import com.day.cq.replication.ReplicationException;
import com.day.cq.wcm.api.Page;
import com.pwc.madison.core.constants.MadisonConstants;

/**
 * Custom dispatcher flush content builder that sends a list of URIs to be
 * re-fetched immediately upon flushing a page.
 */
@Component(service = { ContentBuilder.class },configurationPolicy = ConfigurationPolicy.REQUIRE,immediate = true, enabled = true,property={"name=refetch-dispatcher",Constants.SERVICE_RANKING +":Integer=1001"})
@Designate(ocd = DispatcherReFetchContentBuilder.DispatcherRefetchContentBuilderConfig.class)
public class DispatcherReFetchContentBuilder implements ContentBuilder {

    @Reference
    private ResourceResolverFactory resolverFactory;

    /** The name of the replication agent */
    public static final String NAME = "refetch-dispatcher";

    /**
     * The serialization type as it will display in the replication
     * agent edit dialog selection field.
     */
    public static final String TITLE = "Re-fetch Dispatcher Flush";
    
    private static final String NT_DAM_ASSET = "dam:Asset";
    
    private static final Logger logger = LoggerFactory.getLogger(DispatcherReFetchContentBuilder.class);
    
    private String[] skipPaths = ArrayUtils.EMPTY_STRING_ARRAY;
    

    /**
     * {@inheritDoc}
     */
    @Override
    public ReplicationContent create(Session session, ReplicationAction action,
            ReplicationContentFactory factory) throws ReplicationException {
        return create(session, action, factory, null);
    }

    @Activate
    protected void activate(DispatcherReFetchContentBuilder.DispatcherRefetchContentBuilderConfig config) {
        if(ArrayUtils.isNotEmpty(config.getSkipPages())) {
            skipPaths = config.getSkipPages();
        }
    }
    /**
     * Create the replication content containing the URLs for
     * Dispatcher to refetch
     */
    @Override
    public ReplicationContent create(Session session, ReplicationAction action,
            ReplicationContentFactory factory, Map<String, Object> parameters)
            throws ReplicationException {

        ResourceResolver rr = null;

        try {
            HashMap<String, Object> map = new HashMap<String, Object>();
            map.put(JcrResourceConstants.AUTHENTICATION_INFO_SESSION, session);
            rr = resolverFactory.getResourceResolver(map);
            String path = action.getPath();
            if (StringUtils.isNotEmpty(path) && ReplicationActionType.ACTIVATE == action.getType() && !isSkipped(path)) {
                int pathSep = path.lastIndexOf(MadisonConstants.FORWARD_SLASH);
                if (pathSep != -1) {
                    int extSep = path.indexOf('.', pathSep);
                    //Extension less path handling
                    if (extSep == -1) {
                        ArrayList<String> urisList = new ArrayList<String>();
                        //Only fetch pages under /content
                        if (path.startsWith(MadisonConstants.SLASH_CONTENT) && isPage(path,rr)) {
                            urisList.add(path +MadisonConstants.HTML_EXTN);
                        }
                        String[] uris = urisList.toArray(new String[urisList.size()]);
                        return create(factory, uris);
                    } else {
                        Resource res = rr.getResource(path);
                        try {
                            Node node = (Node) res.adaptTo(Node.class);
                            //No fetching of dam asset
                            if (NT_DAM_ASSET.equals(node.getPrimaryNodeType().getName())) {
                                return ReplicationContent.VOID;
                            } else {
                                String[] uris = new String[] { path };
                                return create(factory, uris);
                            }
                        } catch (RepositoryException e) {
                            return ReplicationContent.VOID;
                        }
                    }

                }
            }
        } catch (LoginException e) {
            logger.error(e.getMessage(), e);
        } finally {
            if (rr != null)
                rr.close();
        }
        return ReplicationContent.VOID;
    }
    
    private boolean isPage(String path, ResourceResolver rr) {
        if(rr != null) {
            Resource pageResource = rr.getResource(path);
                return pageResource != null && pageResource.adaptTo(Page.class) != null;
        }
        return false;
    }

    private boolean isSkipped(String path) {
        for(String skip : skipPaths) {
            return StringUtils.startsWith(path, skip);
        }
        return false;
    }

    /**
     * Create the replication content, containing one or more URIs to be re-fetched
     * immediately upon flushing a page.
     *
     * @param factory factory
     * @param uris    URIs to re-fetch
     * @return replication content
     *
     * @throws ReplicationException if an error occurs
     */
    private ReplicationContent create(ReplicationContentFactory factory, String[] uris) throws ReplicationException {

        File tmpFile;
        BufferedWriter out = null;

        try {
            tmpFile = File.createTempFile("cq5", ".post");
        } catch (IOException e) {
            throw new ReplicationException("Unable to create temp file", e);
        }

        try {
            out = new BufferedWriter(new FileWriter(tmpFile));
            for (int i = 0; i < uris.length; i++) {
                out.write(uris[i]);
                out.newLine();
                logger.debug("adding " + uris[i]);
            }
            out.close();
            IOUtils.closeQuietly(out);
            out = null;
            return factory.create("text/plain", tmpFile, true);
        } catch (IOException e) {
            if (out != null) {
                IOUtils.closeQuietly(out);
            }
            tmpFile.delete();
            throw new ReplicationException("Unable to create repository content", e);
        }
    }
    
    /**
     * {@inheritDoc}
     *
     * @return {@value #NAME}
     */
    @Override
    public String getName() {
        return NAME;
    }

    /**
     * {@inheritDoc}
     *
     * @return {@value #TITLE}
     */
    @Override
    public String getTitle() {
        return TITLE;
    }
    @ObjectClassDefinition(
            name = "PwC Viewpoint Dispatcher Refetch Configurations",
            description = "Configuration allows to provide pages need to skip in refetch")
    public @interface DispatcherRefetchContentBuilderConfig {

        @AttributeDefinition(name = "refetch.skippath", 
                description = "Paths to skip while refetching , path starting with these pattern will be skipped",
                type = AttributeType.STRING,
                cardinality = Integer.MAX_VALUE)
        String[] getSkipPages();
    }
}