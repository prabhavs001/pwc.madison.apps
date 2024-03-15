package com.pwc.madison.core.workflows;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.jcr.Session;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.api.resource.ResourceUtil;
import org.apache.sling.api.resource.ValueMap;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.day.cq.commons.jcr.JcrConstants;
import com.day.cq.search.PredicateGroup;
import com.day.cq.search.Query;
import com.day.cq.search.QueryBuilder;
import com.day.cq.search.result.SearchResult;
import com.day.cq.wcm.api.Page;
import com.day.cq.workflow.WorkflowException;
import com.day.cq.workflow.WorkflowSession;
import com.day.cq.workflow.exec.WorkItem;
import com.day.cq.workflow.exec.WorkflowProcess;
import com.day.cq.workflow.metadata.MetaDataMap;
import com.pwc.madison.core.beans.PostProcessing;
import com.pwc.madison.core.constants.DITAConstants;
import com.pwc.madison.core.services.MadisonSystemUserNameProviderService;
import com.pwc.madison.core.services.TocContentService;
import com.pwc.madison.core.util.MadisonUtil;

/**
 *
 */
@Component(service = WorkflowProcess.class, property = { "process.label= Viewpoint - Create Chapter level Toc JSON" })
public class CreateChapterTocJsonWorkflow implements WorkflowProcess {

    private static final String YES = "yes";
	private static final String PN_TYPE = "type";
    private static final String GENERATED_PATH = "generatedPath";
    private static final String HTML_EXTENSION = ".html";
    private static final String METADATA_ERROR = "Workflow metadata null. Cannot proceed with Update.";
    private static final String PATH = "path";
    private static final String P_LIMIT = "p.limit";
    private static final String P_LIMIT_VALUE = "-1";
    private static final String CQ_PAGE = "cq:Page";

    protected final Logger log = LoggerFactory.getLogger(this.getClass());
    @Reference
    private ResourceResolverFactory resolverFactory;
    @Reference
    private QueryBuilder queryBuilder;
    @Reference
    private MadisonSystemUserNameProviderService madisonSystemUserNameProviderService;
    
    @Reference
    private TocContentService tocContentService;

    @Override
    public void execute(final WorkItem item, final WorkflowSession wfsession, final MetaDataMap args)
            throws WorkflowException {
        long startTime = System.currentTimeMillis();
        PostProcessing postProcessing = new PostProcessing();
        postProcessing.setFailure(false);
        postProcessing.appendString(System.lineSeparator());
        postProcessing.appendString("POST PROCESSING WORKFLOW: " + item.getWorkflowData().getPayload().toString());
        postProcessing.appendString(System.lineSeparator());
        postProcessing.appendString("------------------------------");
        postProcessing.appendString(System.lineSeparator());
        ResourceResolver resolver = null;
        Session session = null;
        final MetaDataMap meta = item.getWorkflowData().getMetaDataMap();
        try {
            if (null == meta) {
                log.error(METADATA_ERROR);
                postProcessing.setFailure(true);
                postProcessing.appendException(METADATA_ERROR);
                return;
            }
            resolver = MadisonUtil.getResourceResolver(resolverFactory, madisonSystemUserNameProviderService.getFmditaServiceUsername());
            session = resolver.adaptTo(Session.class);
            final String outputPath = meta.get(GENERATED_PATH, String.class);
            
            final String sourcePath = outputPath.substring(0, outputPath.indexOf(HTML_EXTENSION));
            log.debug("Base Path Is {} ", sourcePath);
            boolean isChapterToc = false;
            boolean isJoinViewEnabled = false;
            int joinedLevel = 0;
            Resource baseContentResource = resolver.getResource(sourcePath + DITAConstants.FORWARD_SLASH + JcrConstants.JCR_CONTENT);
			if (null != baseContentResource && !ResourceUtil.isNonExistingResource(baseContentResource)) {
				ValueMap valueMap = baseContentResource.adaptTo(ValueMap.class);
				if (YES.equals(valueMap.get(DITAConstants.META_LOAD_LITE_TOC, String.class))) {
					isChapterToc = true;
				}
				if (YES.equals(valueMap.get(DITAConstants.META_JOINED_SECTION_TOC, String.class))) {
					isJoinViewEnabled = true;
					joinedLevel = valueMap.containsKey(DITAConstants.META_JOINED_SECTION_LEVEL) ? valueMap.get(DITAConstants.META_JOINED_SECTION_LEVEL, Integer.class) : 0;
				}
			}
            if (isChapterToc) {
				final Map<String, Object> predicateMap = getPredicateMap(sourcePath);
				final Query query = queryBuilder.createQuery(PredicateGroup.create(predicateMap), session);
				final SearchResult searchResult = query.getResult();
				final Iterator<Resource> resources = searchResult.getResources();
				ArrayList<Page> pageList = new ArrayList<>();
				while (resources.hasNext()) {
					final Resource resource = resources.next();
					pageList.add(resource.adaptTo(Page.class));
				}
				tocContentService.createChapterTocContentJson(resolver, sourcePath, pageList, joinedLevel, isJoinViewEnabled);
			}
        } catch (final Exception e) {
            log.error("Failed to create chapter TOC json  {}", e);
        } finally {
            if (resolver != null && resolver.isLive()) {
                resolver.close();
            }
        }
        long endTime = System.currentTimeMillis();
        log.debug("Madision - Chapter TOC json creation took {} seconds to complete the process", (endTime - startTime) / 1000);
    }

    /**
     * Private method to return the Search predicate map
     *
     * @param outputPath
     *            pathOfOutputPage
     * @return predicateMap
     */
    private Map<String, Object> getPredicateMap(final String outputPath) {

        final Map<String, Object> predicateMap = new HashMap<>();
        predicateMap.put(P_LIMIT, P_LIMIT_VALUE);
        predicateMap.put(PATH, outputPath);
        predicateMap.put(PN_TYPE, CQ_PAGE);

        return predicateMap;
    }

}
