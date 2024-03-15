package com.pwc.madison.core.services.impl;

import com.day.cq.contentsync.handler.util.RequestResponseFactory;
import com.day.cq.search.PredicateGroup;
import com.day.cq.search.Query;
import com.day.cq.search.QueryBuilder;
import com.day.cq.search.result.SearchResult;
import com.day.cq.wcm.api.WCMMode;
import com.pwc.madison.core.constants.DITAConstants;
import com.pwc.madison.core.constants.MadisonConstants;
import com.pwc.madison.core.services.FootNoteService;
import com.pwc.madison.core.util.MadisonUtil;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.engine.SlingRequestProcessor;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * FootNoteService service implementation
 */
@Component(service = FootNoteService.class, immediate = true)
public class FootNoteServiceImpl implements FootNoteService {

    public static final String FN_NODE_REGEX = "fn*";
    public static final String FOOTNOTE_RESOURCE_TYPE = "pwc-madison/components/ditacontent/footnote";
    public static final String FN_LIMIT_VALUE = "-1";
    public static final String SLING_RESOURCE_TYPE = MadisonConstants.SYMB_AT + DITAConstants.PN_SLING_RESOURCE_TYPE;

    @Reference
    private transient RequestResponseFactory requestResponseFactory;

    /** Service to process requests through Sling */
    @Reference
    private transient SlingRequestProcessor requestProcessor;

    @Reference
    private QueryBuilder queryBuilder;

    @Reference
    private ResourceResolverFactory resolverFactory;

    final Logger LOG = LoggerFactory.getLogger(FootNoteServiceImpl.class);
    @Override
    public String getHtml(String nodePath, Resource resource) throws ServletException, IOException {
        final HttpServletRequest req = requestResponseFactory.createRequest("GET", nodePath);
        WCMMode.DISABLED.toRequest(req);
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        final HttpServletResponse resp = requestResponseFactory.createResponse(out);
        requestProcessor.processRequest(req, resp, resource.getResourceResolver());
        return out.toString(StandardCharsets.UTF_8.name()).trim();
    }


    /**
     * Method that inserts the entire HTML of a footnote as a property on the fn node
     *
     * @param path
     *            {@link String} path of a page.
     *
     * @return {@link Void}
     */
    @Override
    public void updateFootnoteHTMLString(String path){
        ResourceResolver resourceResolver  = MadisonUtil.getResourceResolver(resolverFactory,
                MadisonConstants.MADISON_CONTENT_ADMIN_SUB_SERVICE);
        SearchResult searchResult = getFootNotesNodes(path, resourceResolver);

        final Iterator<Resource> resourceItr = searchResult.getResources();
        Pattern p = Pattern.compile("^\\p{Punct}");
        try {
            while (resourceItr.hasNext()) {
                Resource fnResource = resourceItr.next();
                Node fnNode = fnResource.adaptTo(Node.class);
                String outputclassProp = fnNode.hasProperty(DITAConstants.PN_OUTPUTCLASS) ? fnNode.getProperty(DITAConstants.PN_OUTPUTCLASS).getString() : "";
                if(fnResource.hasChildren()) {
                    Iterator<Resource> childrenItr = fnResource.getChildren().iterator();
                    String fnHtmlString = "<div class=\"fnote "+outputclassProp+"\">";

                    while (childrenItr.hasNext()) {
                        Resource childElementResource = childrenItr.next();
                        String childElementPath = childElementResource.getPath();
                        childElementPath = childElementPath + MadisonConstants.DWNLD_PDF_EXTN;
                        String htmlString = getHtml(childElementPath, childElementResource);
                        Matcher matcher = p.matcher(htmlString);
                        if(!htmlString.isEmpty() && (!matcher.find() || htmlString.startsWith("<"))){
                            fnHtmlString += StringUtils.SPACE + htmlString;
                        }else{
                            fnHtmlString += htmlString;
                        }
                    }
                    fnHtmlString += "</div>";
                    Session session = resourceResolver.adaptTo(Session.class);
                    fnNode.setProperty(DITAConstants.PN_FN_HTML_STRING, fnHtmlString);
                    session.save();
                }
            }
        } catch (ServletException | RepositoryException | IOException e) {
            LOG.error("Error while updating footnote html string", e);
        } finally {
            if (null != resourceResolver && resourceResolver.isLive()) {
                resourceResolver.close();
            }
        }

    }

    /**
     * Method that returns all the footnote nodes under the specified {@param path}
     *
     * @param path
     *            {@link String} path of a page.
     *
     * @return {@link SearchResult}
     */
    @Override
    public SearchResult getFootNotesNodes(String path, ResourceResolver resourceResolver){

        final Map<String, Object> predicateMap = new HashMap<>();
        predicateMap.put("path", path);
        predicateMap.put("nodename", FN_NODE_REGEX);
        predicateMap.put("property", SLING_RESOURCE_TYPE);
        predicateMap.put("property.value", FOOTNOTE_RESOURCE_TYPE);
        predicateMap.put("p.limit", FN_LIMIT_VALUE);

        final Query query = queryBuilder.createQuery(PredicateGroup.create(predicateMap),
                resourceResolver.adaptTo(Session.class));
        final SearchResult searchResult = query.getResult();
        LOG.debug("searchResult size::: {}", searchResult.getHits().size());

        return searchResult;
    }
}
