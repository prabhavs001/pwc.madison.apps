/*
 *  Copyright 2015 Adobe Systems Incorporated
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package com.pwc.madison.core.listeners;

import javax.jcr.Session;

import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.osgi.framework.Constants;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.day.cq.replication.ReplicationAction;
import com.day.cq.replication.ReplicationActionType;
import com.day.cq.replication.ReplicationException;
import com.day.cq.replication.Replicator;
import com.pwc.madison.core.constants.DITAConstants;
import com.pwc.madison.core.services.MadisonSystemUserNameProviderService;
import com.pwc.madison.core.util.MadisonUtil;

/**
 * A listener to listen for the page activation and replicate the link patch index nodes to publisher.
 *
 * @author vhs
 *
 */
@Component(service = EventHandler.class,
           immediate = true,
           property = {
                   Constants.SERVICE_DESCRIPTION + "=Listener on page replication to activate the link patching index programatically",
                   EventConstants.EVENT_TOPIC + "=" + ReplicationAction.EVENT_TOPIC
           })
public class PageReplicationListener implements EventHandler {

    @Reference
    private Replicator replicator;
    
    @Reference
    private ResourceResolverFactory resolverFactory;
    
    @Reference
    private MadisonSystemUserNameProviderService madisonSystemUserNameProviderService;

    private final Logger logger = LoggerFactory.getLogger(PageReplicationListener.class);

    private String EVENT_PATHS = "paths";
    private String PWC_PUBLISH_PATH = "/content/pwc-madison/ditaroot/";
    private String XREF_CUSTOM_INDEX_PATH = "/content/fmditacustom/xrefpathreferences";
    
    @Override
    public void handleEvent(final Event event) {
        
        logger.debug("PageReplicationListener :: ");
        final ResourceResolver resolver = MadisonUtil.getResourceResolver(resolverFactory, madisonSystemUserNameProviderService.getFmditaServiceUsername());
        
        if (null == resolver) {
            logger.error("Unable to get fmdita service user resource resolver. Check the permissions");
            return;
        }
        try {
	        final Session session = resolver.adaptTo(Session.class);
	        Object path = event.getProperty(EVENT_PATHS);
	        if(null == path) {
	            return;
	        }
	        if (!(path instanceof String[])) {
	            return;
	        }    
	        String[] paths = (String[])path;
	        for (String pwcPagePath : paths) {
	            logger.debug("Even path for pages :: "+pwcPagePath);
	            if (pwcPagePath.contains(PWC_PUBLISH_PATH)) {
	                try {
	                    replicator.replicate(session, ReplicationActionType.ACTIVATE, XREF_CUSTOM_INDEX_PATH);
	                } catch (ReplicationException e) {
	                    logger.error("Error replicating link patching index /content/fmditacustom/xrefpathreferences ", e);
	                } 
	                break;
	            }
	        }
        } finally {
            closeResourceResolver(resolver);
        }
    }
    
    private void closeResourceResolver(final ResourceResolver resolver) {
     // close the service user resolver
        if (resolver.isLive()) {
            try {
                resolver.refresh();
                resolver.commit();
                resolver.close();
            } catch (final PersistenceException e) {
                logger.error("Error while closing the resolver in PageReplicationListener", e);
            }
        }
    }
}

