package com.pwc.madison.core.servlets;

import com.day.cq.replication.ReplicationActionType;
import com.day.cq.replication.ReplicationException;
import com.day.cq.replication.Replicator;
import com.pwc.madison.core.constants.MadisonConstants;
import com.pwc.madison.core.userreg.Constants;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.servlets.HttpConstants;
import org.apache.sling.api.servlets.ServletResolverConstants;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.AttributeType;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;
import javax.management.InvalidAttributeValueException;
import javax.servlet.Servlet;
import javax.servlet.ServletException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Objects;


@Component(
    service = Servlet.class,
    immediate = true,
    configurationPolicy = ConfigurationPolicy.REQUIRE,
    property = {
        org.osgi.framework.Constants.SERVICE_DESCRIPTION + "=PwC PwC Viewpoint Redirect Manager Servlet",
        ServletResolverConstants.SLING_SERVLET_PATHS + "=/bin/redirectmanager",
        ServletResolverConstants.SLING_SERVLET_METHODS + "=" + HttpConstants.METHOD_POST})
@Designate(ocd = RedirectManagerServlet.RedirectManagerServletConfiguration.class)
public class RedirectManagerServlet extends SlingAllMethodsServlet {

    private static final Logger LOGGER = LoggerFactory.getLogger(RedirectManagerServlet.class);

    private static final String STATUS_REQUEST_PARAM = "status";
    private static final String OLD_URL_REQUEST_PARAM = "oldurl";
    private static final String NEW_URL_REQUEST_PARAM = "newurl";
    private static final String SLING_MATCH_PN = "sling:match";
    private static final String SLING_INTERNAL_REDIRECT_PN = "sling:internalRedirect";
    private static final String SLING_REDIRECT_PN = "sling:redirect";
    private static final String SLING_STATUS_PN = "sling:status";

    private String rootPath;
    private String allowedOldUrlPattern;
    private String allowedNewUrlPattern;

    @Reference
    private Replicator replicator;

    @ObjectClassDefinition(name = "PwC Viewpoint Redirect Manager Servlet Configuration")
    public @interface RedirectManagerServletConfiguration {

        @AttributeDefinition(
            name = "Redirect URL Root Path",
            description = "Example: etc/map/http/localhost.4503",
            type = AttributeType.STRING
        )
        String etc_map_root_path();

        @AttributeDefinition(
            name = "Old URL Pattern",
            type = AttributeType.STRING
        )
        String old_url_pattern() default "^[0-9a-zA-Z_ \\-/.$]*$";

        @AttributeDefinition(
            name = "New URL Pattern",
            type = AttributeType.STRING
        )
        String new_url_pattern() default "^[0-9a-zA-Z_ \\-/.:]*$";
    }

    @Activate
    @Modified
    protected void activate(RedirectManagerServletConfiguration redirectManagerServletConfiguration) {
        LOGGER.trace("RedirectManagerServlet: Inside activate!");
        this.rootPath = redirectManagerServletConfiguration.etc_map_root_path();
        if (this.rootPath.startsWith("/")) {
            this.rootPath = this.rootPath.substring(1);
        }
        this.allowedOldUrlPattern = redirectManagerServletConfiguration.old_url_pattern();
        this.allowedNewUrlPattern = redirectManagerServletConfiguration.new_url_pattern();
        LOGGER.debug("RedirectManagerServlet Activate() -> root path: {}, allowedOldURLPattern: {}, " +
            "allowedNewURLPattern: {}", new Object[]{rootPath, allowedOldUrlPattern, allowedNewUrlPattern});
    }

    @Override
    protected void doPost(SlingHttpServletRequest request,
                          SlingHttpServletResponse response) throws ServletException, IOException {

        LOGGER.debug("RedirectManagerServlet.doPost(): Action Type: " +
            StringUtils.normalizeSpace(request.getParameter("actype")));
        response.setContentType("application/json");
        response.setCharacterEncoding(Constants.UTF_8_ENCODING);

        switch (request.getParameter("actype").toUpperCase()) {
            case "SEARCH":
                response.getWriter().write(doSearch(request));
                break;
            case "SAVE":
                response.getWriter().write(doSave(request));
                break;
            case "DELETE":
                response.getWriter().write(doDelete(request));
                break;
            case "CHECK":
                response.getWriter().write(checkExisting(request));
        }

        response.getWriter().write("");
    }

    private String doSearch(SlingHttpServletRequest request) {
        JSONObject responseJson = new JSONObject();
        Session session = null;

        try {
            LOGGER.debug("doSearch(): Request query parameters: " + StringUtils.normalizeSpace(request.getParameterMap().toString()));
            String status = request.getParameter(STATUS_REQUEST_PARAM);
            String oldurl = request.getParameter(OLD_URL_REQUEST_PARAM).toLowerCase();
            String newurl = request.getParameter(NEW_URL_REQUEST_PARAM);
            if (validateUrlParamsForAllowedCharacters(oldurl, newurl)) {

                session = request.getResourceResolver().adaptTo(Session.class);
                QueryManager queryManager = session.getWorkspace().getQueryManager();

                responseJson.put("queryManager", "queryManager");
                String expression = "SELECT * FROM [sling:Mapping] AS s WHERE ";
                if (status.equalsIgnoreCase("all")) {
                    if (newurl.equals("")) {
                        expression = expression.concat("([" + SLING_REDIRECT_PN + "] IS NOT NULL or [" + SLING_INTERNAL_REDIRECT_PN + "] IS NOT NULL)");
                    } else {
                        expression = expression.concat("([" + SLING_REDIRECT_PN + "] like '").concat(encodeRedirect(newurl)).concat("%'").concat("or [" + SLING_INTERNAL_REDIRECT_PN + "] like '").concat(encodeRedirect(newurl)).concat("%')");
                    }
                } else if (status.equalsIgnoreCase("200")) {
                    expression = newurl.equals("") ? expression.concat("[" + SLING_INTERNAL_REDIRECT_PN + "]<>'' ") : expression.concat("[" + SLING_INTERNAL_REDIRECT_PN + "] like '").concat(encodeRedirect(newurl)).concat("%'");
                } else {
                    expression = expression.concat(("[" + SLING_STATUS_PN + "]=").concat(status));
                    if (!newurl.equals(""))
                        expression = expression.concat(" and [" + SLING_REDIRECT_PN + "] like '").concat(encodeRedirect(newurl)).concat("%'");
                }

                expression = expression.concat(" and ISDESCENDANTNODE([/").concat(rootPath).concat("]) and [jcr:path] LIKE '%").concat(oldurl).concat("%'");

                int i = 0;
                JSONArray jsa = new JSONArray();

                Query qryFolder = queryManager.createQuery(expression, Query.JCR_SQL2);
                QueryResult resultFolder = qryFolder.execute();
                NodeIterator niFolder = resultFolder.getNodes();

                while (niFolder.hasNext()) {
                    jsa.put(this.convertJson(niFolder.nextNode()));
                    i++;
                    if (i > 100) break;
                }

                responseJson.put("total", i);
                responseJson.put("rws", jsa);
            }
        } catch (Exception e) {
            LOGGER.error("RedirectManagerServlet.doSearch(): " + e.getMessage(), e);
            try {
                responseJson.put("error", "Error while searching for redirect link");
            } catch (JSONException je) {
                LOGGER.error("RedirectManagerServlet.doSearch(): JSONException occurred! " + e.getMessage(), e);
            }
        }

        String response = responseJson.toString();
        LOGGER.info("RedirectManagerServlet.doSearch(): Search Response: " + response);

        return response;
    }

    private String checkExisting(SlingHttpServletRequest request) {
        LOGGER.debug("checkExisting(): Request query parameters: {}", StringUtils.normalizeSpace(request.getParameterMap().toString()));
        String status = request.getParameter(STATUS_REQUEST_PARAM);
        String oldUrl = request.getParameter(OLD_URL_REQUEST_PARAM).toLowerCase().trim();
        String newUrl = request.getParameter(NEW_URL_REQUEST_PARAM).trim();

        if (oldUrl.endsWith("/")) {
            oldUrl = oldUrl.substring(0, oldUrl.length() - 1);
        }

        JSONObject responseJson = new JSONObject();
        try {
            if (validateUrlParamsForAllowedCharacters(oldUrl, newUrl)) {
                ResourceResolver resourceResolver = request.getResourceResolver();
                Resource oldUrlResource = resourceResolver.getResource(MadisonConstants.FORWARD_SLASH + rootPath + oldUrl);
                if(Objects.isNull(oldUrlResource)){
                    LOGGER.debug("{} URL doesn't exist in {}", StringUtils.normalizeSpace(oldUrl), rootPath);
                    responseJson.put("exists", false);
                } else {
                    LOGGER.debug("{} URL exist in {}", StringUtils.normalizeSpace(oldUrl), rootPath);
                    responseJson.put("exists", true);
                }

            }
        } catch (Exception e) {
            LOGGER.error("Error while checking redirect link", e);
            try {
                responseJson.put("exists", true);
                responseJson.put("error", "Error while checking redirect link");
            } catch (JSONException je) {
                LOGGER.error("RedirectManagerServlet.checkExisting(): JSONException occurred! " + e.getMessage(), e);
            }
        }
        String response = responseJson.toString();
        LOGGER.info("RedirectManagerServlet.checkExisting(): Check Old URL existence " + response);
        return response;
    }

    private JSONObject convertJson(Node node) throws JSONException, RepositoryException, UnsupportedEncodingException {
        JSONObject j = new JSONObject();
        j.put("oldurl", decodeUrl(node.getPath().replace("/".concat(rootPath), "")));
        String status = node.hasProperty(SLING_STATUS_PN) ? node.getProperty(SLING_STATUS_PN).getString() : "200";
        if (status.equalsIgnoreCase("200") && node.hasProperty(SLING_INTERNAL_REDIRECT_PN)) {
            j.put("newurl", decodeRedirect(node.getProperty(SLING_INTERNAL_REDIRECT_PN).getString()));
            j.put("status", "200");
        } else {
            j.put("newurl", node.hasProperty(SLING_REDIRECT_PN) ? decodeRedirect(node.getProperty(SLING_REDIRECT_PN).getString()) : "");
            j.put("status", node.hasProperty(SLING_STATUS_PN) ? node.getProperty(SLING_STATUS_PN).getString() : "");
        }
        return j;
    }

    private String doSave(SlingHttpServletRequest request) {
        LOGGER.debug("doSave(): Request query parameters: {}", StringUtils.normalizeSpace(request.getParameterMap().toString()));
        String status = request.getParameter(STATUS_REQUEST_PARAM);
        String oldUrl = request.getParameter(OLD_URL_REQUEST_PARAM).toLowerCase().trim();
        String newUrl = request.getParameter(NEW_URL_REQUEST_PARAM).trim();

        if (oldUrl.endsWith("/")) {
            oldUrl = oldUrl.substring(0, oldUrl.length() - 1);
        }

        JSONObject responseJson = new JSONObject();
        Session session = null;
        Node node = null;
        try {
            if (validateUrlParamsForAllowedCharacters(oldUrl, newUrl)) {
                session = request.getResourceResolver().adaptTo(Session.class);
                Node rootNode = session.getRootNode();
                if (!rootNode.hasNode(rootPath)) {
                    LOGGER.error("Configured root path {} doesn't exist. Creating : {}", rootPath, rootPath);
                    String[] rootPathNodeNames = rootPath.split("/");
                    Node currentNode = rootNode;
                    for (String nodeName : rootPathNodeNames) {
                        if (currentNode.hasNode(nodeName)) {
                            currentNode = currentNode.getNode(nodeName);
                        } else {
                            currentNode.addNode(nodeName, "sling:Folder");
                            session.save();
                            activateNode(session, currentNode);
                        }
                    }
                }
                node = rootNode.getNode(rootPath);

                // Create folder
                int idx = oldUrl.lastIndexOf("/");
                try {
                    node = node.getNode(encodeUrl(oldUrl.substring(1, idx)));
                } catch (Exception e) {
                    if (idx > 1) {
                        for (String s : oldUrl.substring(1, idx).split("/")) {
                            if (s.length() > 0) {
                                try {
                                    node = node.getNode(encodeUrl(s));
                                } catch (Exception exx) {
                                    node = node.addNode(encodeUrl(s), "sling:Mapping");
                                    node.setProperty(SLING_MATCH_PN, encodeRedirectMatch(s));
                                    session.save();
                                    activateNode(session, node);
                                }
                            }
                        }
                    }
                }

                String nodeName = oldUrl.substring(idx + 1);
                try {
                    node = node.getNode(encodeUrl(nodeName));
                } catch (Exception ex) {
                    node = node.addNode(encodeUrl(nodeName), "sling:Mapping");
                }

                node.setProperty(SLING_MATCH_PN, encodeRedirectMatch(nodeName));
                if (status.equalsIgnoreCase("200")) {
                    node.setProperty(SLING_INTERNAL_REDIRECT_PN, encodeRedirect(newUrl));
                    node.setProperty(SLING_REDIRECT_PN, (Value) null);
                    node.setProperty(SLING_STATUS_PN, (Value) null);
                } else {
                    node.setProperty(SLING_REDIRECT_PN, encodeRedirect(newUrl));
                    node.setProperty(SLING_STATUS_PN, status);
                    node.setProperty(SLING_INTERNAL_REDIRECT_PN, (Value) null);
                }

                session.save();
                activateNode(session, node);
                responseJson.put("saved", true);
            }
        } catch (Exception e) {
            LOGGER.error("Error while saving redirect link", e);
            try {
                responseJson.put("saved", false);
                responseJson.put("error", "Error while saving redirect link");
            } catch (JSONException je) {
                LOGGER.error("RedirectManagerServlet.doSave(): JSONException occurred! " + e.getMessage(), e);
            }
        }
        String response = responseJson.toString();
        LOGGER.info("RedirectManagerServlet.doSave(): Save Response: " + response);
        return response;
    }

    private String doDelete(SlingHttpServletRequest request) {
        LOGGER.debug("doDelete(): Request query parameters: {}", StringUtils.normalizeSpace(request.getParameterMap().toString()));
        String oldurl = request.getParameter("oldurl");
        JSONObject responseJson = new JSONObject();

        Session session = null;
        Node node = null;
        try {
            if (validateUrlParamsForAllowedCharacters(oldurl, "")) {
                session = request.getResourceResolver().adaptTo(Session.class);
                node = session.getRootNode().getNode(rootPath.concat(encodeUrl(oldurl)));
                if (node.hasNodes()) {
                    node.setProperty(SLING_INTERNAL_REDIRECT_PN, (Value) null);
                    node.setProperty(SLING_REDIRECT_PN, (Value) null);
                    node.setProperty(SLING_STATUS_PN, (Value) null);
                    activateNode(session, node);
                } else {
                    Node parentNode = node.getParent();
                    deactivateAndDeleteNode(session, node);
                    while (parentNode.getNodes().getSize() == 1 && !parentNode.hasProperty(SLING_INTERNAL_REDIRECT_PN) && !parentNode.hasProperty(SLING_REDIRECT_PN)) {
                        node = parentNode;
                        parentNode = node.getParent();
                        deactivateAndDeleteNode(session, node);
                    }
                }
                session.save();
                LOGGER.debug("Successfully deleted mapping for {}", StringUtils.normalizeSpace(oldurl));
                responseJson.put("deleted", true);
            }
        } catch (Exception e) {
            LOGGER.error("RedirectManagerServlet.doDelete(): " + e.getMessage(), e);
            try {
                responseJson.put("deleted", false);
                responseJson.put("error", "Error while deleting redirect link");
            } catch (JSONException je) {
                LOGGER.error("RedirectManagerServlet.doDelete(): JSONException occurred! " + e.getMessage(), e);
            }
        }
        String response = responseJson.toString();
        LOGGER.info("RedirectManagerServlet.doDelete(): Deletion Response: " + response);
        return response;
    }

    private boolean validateUrlParamsForAllowedCharacters(String oldUrl, String newUrl) throws InvalidAttributeValueException {
        if (!oldUrl.matches(allowedOldUrlPattern)) {
            LOGGER.error("validateUrlParamsForAllowedCharacters(): "
                + "Old Url Validation Failed for value: " + StringUtils.normalizeSpace(oldUrl));
            throw new InvalidAttributeValueException("Old Url should be of the pattern: " + allowedOldUrlPattern);
        } else if (!newUrl.matches(allowedNewUrlPattern)) {
            LOGGER.error("validateUrlParamsForAllowedCharacters(): "
                + "New Url Validation Failed for value: " + StringUtils.normalizeSpace(newUrl));
            throw new InvalidAttributeValueException("New Url should be of the pattern: " + allowedNewUrlPattern);
        } else {
            LOGGER.debug("validateUrlParamsForAllowedCharacters(): "
                + "Url Validation success for the values: oldUrl = {}, newUrl = {}",
                StringUtils.normalizeSpace(oldUrl), StringUtils.normalizeSpace(newUrl));
            return true;
        }
    }

    private String encodeUrl(String s) throws UnsupportedEncodingException {
        return java.net.URLEncoder.encode(s, Constants.UTF_8_ENCODING).replace("%2F", "/").replace("%24", "$");
    }

    private String decodeUrl(String s) throws UnsupportedEncodingException {
        return java.net.URLDecoder.decode(s, Constants.UTF_8_ENCODING);
    }

    private String encodeRedirectMatch(String s) {
        return s.replace("[", "\\\\[").replace("]", "\\\\]").replace("&", "\\\\&").replace("(", "\\\\(").replace(")", "\\\\)");
    }

    private String encodeRedirect(String s) {
        return s.replace("[", "\\\\[").replace("]", "\\\\]").replace("&", "\\\\&").replace("(", "\\\\(").replace(")", "\\\\)");
    }

    private String decodeRedirect(String s) {
        return s.replace("\\\\[", "[").replace("\\\\]", "]").replace("\\\\&", "&").replace("\\\\(", "(").replace("\\\\)", ")");
    }

    private void activateNode(Session session, Node node) throws RepositoryException, ReplicationException {
        if(Objects.isNull(replicator)){
            LOGGER.error("replicator is null");
            throw new ReplicationException("Replicator is null");
        }
        replicator.replicate(session, ReplicationActionType.ACTIVATE, node.getPath());
        LOGGER.debug("Successfully activated: {}", node.getPath());
    }

    private void deactivateAndDeleteNode(Session session, Node node) throws RepositoryException, ReplicationException {
        if(Objects.isNull(replicator)){
            LOGGER.error("replicator is null");
            throw new ReplicationException("Replicator is null");
        }
        String nodePath = node.getPath();
        replicator.replicate(session, ReplicationActionType.DEACTIVATE, nodePath);
        LOGGER.debug("Deactivation request for : {}", nodePath);
        node.remove();
        LOGGER.debug("Deletion request for: {}", nodePath);
        session.save();
    }
}
