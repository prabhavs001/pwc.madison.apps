package com.pwc.madison.core.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.jackrabbit.commons.flat.Rank;
import org.apache.jackrabbit.vault.util.Text;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import com.day.cq.commons.jcr.JcrConstants;
import com.day.cq.dam.api.Asset;
import com.day.cq.dam.api.DamConstants;
import com.day.cq.dam.commons.util.DamUtil;
import com.pwc.madison.core.constants.DITAConstants;
import com.pwc.madison.core.constants.MadisonConstants;

public class BrokenLinkReportUtils {

    private static final Logger LOG = LoggerFactory.getLogger(BrokenLinkReportUtils.class);
    private static final String PATH = "/var/dxml/versionreferences/pathToGuid";
    private static final String X_PATH = "/content/fmditacustom/xrefpathreferences";
    private static Comparator<String> order = Rank.comparableComparator();
    



    /**
     * Get scope for given path
     * @param path
     * @param resourceResolver
     * @return
     */
    public static Map<String,LinkDetails> getScopeDetails(String path, ResourceResolver resourceResolver) {
        if (StringUtils.isNotBlank(path) && resourceResolver != null) {
            Resource folderResource = resourceResolver.getResource(path);
            Iterator<Asset> assetIterator = DamUtil.getAssets(folderResource);
            while (assetIterator.hasNext()) {
                Asset currentAsset = assetIterator.next();
                if (DITAConstants.APPLICATION_XML.equals(currentAsset.getMimeType())
                        && (currentAsset.getName().endsWith(DITAConstants.DITA_EXTENSION)
                                || currentAsset.getName().endsWith(DITAConstants.DITAMAP_EXT))) {
                    InputStream ditaContent = getDitaContent(currentAsset);
                    return extractDetailsFromDita(ditaContent,
                            getProperty(resourceResolver, currentAsset, "fmditaXrefs"));
                }
            }
        }
        return Collections.EMPTY_MAP;

    }

    /**
     * 
     * @param resourceResolver
     * @param asset
     * @param property
     * @return
     */
    private static String[] getProperty(ResourceResolver resourceResolver, Asset asset, String property) {
        Resource resource = resourceResolver
                .getResource(asset.getPath() + MadisonConstants.FORWARD_SLASH + JcrConstants.JCR_CONTENT);
        if (resource != null && StringUtils.isNotBlank(property)) {
            ValueMap properties = resource.getValueMap();
            if (properties != null && properties.containsKey(property)) {
                return properties.get(property, String[].class);
            }
        }
        return null;

    }

    /**
     * 
     * @param currentAsset
     * @return
     */
    private static InputStream getDitaContent(Asset currentAsset) {
        final InputStream inputStream = currentAsset.getRendition(DamConstants.ORIGINAL_FILE).getStream();
        final StringWriter writer = new StringWriter();
        try {
            IOUtils.copy(inputStream, writer, Charset.forName(MadisonConstants.UTF_8));
        } catch (IOException e) {
            LOG.error("Error parsing", e);
        }
        final String destAssetContent = writer.toString();
        return IOUtils.toInputStream(destAssetContent, Charset.forName(MadisonConstants.UTF_8));
    }

    /**
     * Parse xml and get scope
     * @param ditaContent
     * @param fmditaXrefs
     * @return
     */
    private static Map<String,LinkDetails> extractDetailsFromDita(InputStream ditaContent, String[] fmditaXrefs) {
        final SAXParserFactory saxParserFactory = SAXParserFactory.newInstance();

        saxParserFactory.setValidating(false);
        Map<String,LinkDetails> links = Collections.EMPTY_MAP;
        try {
            saxParserFactory.setFeature(DITAConstants.EXTERNAL_GENERAL_ENTITIES, false);
            saxParserFactory.setFeature(DITAConstants.EXTERNAL_PARAMETER_ENTITIES, false);
            saxParserFactory.setFeature(DITAConstants.EXTERNAL_DTD_PATH, false);
            saxParserFactory.setXIncludeAware(false);
            final SAXParser saxParser = saxParserFactory.newSAXParser();
            final DefaultHandler handler = new BrokenLinkReportUtils.SAXDITAHandler(fmditaXrefs);
            saxParser.parse(ditaContent, handler);
            SAXDITAHandler saxhandler = (SAXDITAHandler) handler;
            links = saxhandler.getPwcScopePeer();
        } catch (ParserConfigurationException | SAXException | IOException e) {
            LOG.error("Error in xml parsing", e);
        }

        return links;

    }
    
    
    public static boolean isReferenced(ResourceResolver resourceResolver,String key) {
        if(StringUtils.isNotBlank(key) && resourceResolver != null) {
            Node node = getReferenceNode(resourceResolver, Text.escapeIllegalJcrChars(key));
            String path = StringUtils.EMPTY;
            if(node != null) {
                try {
                    path = node.getPath();
                    LOG.debug("Node {} with key {}",path,key);
                    if(node.hasProperty(Text.escapeIllegalJcrChars(key))) {
                        String uid = node.getProperty(Text.escapeIllegalJcrChars(key)).getString();
                        return isXRef(resourceResolver, uid);
                    }
                } catch (RepositoryException e) {
                    LOG.error("Error reading {} from {}",new String[] {key,path},e);
                }
            }
        }
        return false;
       
    }
    
    private static boolean isXRef(ResourceResolver resourceResolver,String key) {
        if(StringUtils.isNotBlank(key)) {
            Node node = getXREFNode(resourceResolver, key);
            String path = StringUtils.EMPTY;
            if(node != null) {
                try {
                    path = node.getPath();
                    LOG.debug("Node {} with key {}",path,key);
                    if(node.hasProperty(key) && node.getProperty(key) != null) {
                        String page = node.getProperty(key).getString();
                        LOG.debug("page path {}",page);
                        Resource resource  = resourceResolver.resolve(page);
                        return resource != null && !resource.isResourceType(Resource.RESOURCE_TYPE_NON_EXISTING);
                    }
                    else {
                        return false;
                    }
                    
                } catch (RepositoryException e) {
                    LOG.error("Error reading {} from {}",new String[] {key,path},e);
                }
            }
        }
        return false;
    }
    
    private static Node getXREFNode(ResourceResolver resourceResolver,String key) {
        if(StringUtils.isNotBlank(key)) {
            Resource resource = resourceResolver.getResource(X_PATH);
            if(resource != null && resource.adaptTo(Node.class) != null) {
                Node node = resource.adaptTo(Node.class);
                try {
                    BTreeCustom bTreeCustom = new BTreeCustom(node, order);
                    if(bTreeCustom != null) {
                        return bTreeCustom.getBTreeNodeForKey(key);
                    }
                } catch (RepositoryException e) {
                    LOG.error("Error in traversing node {}",X_PATH,e);
                }
            }
        }
        return null;
    }
    
    private static Node getReferenceNode(ResourceResolver resourceResolver,String key) {
            Resource resource = resourceResolver.getResource(PATH);
            if(resource != null && resource.adaptTo(Node.class) != null) {
                Node node = resource.adaptTo(Node.class);
                try {
                    BTreeCustom bTreeCustom = new BTreeCustom(node, order);
                    if(bTreeCustom != null) {
                        return bTreeCustom.getBTreeNodeForKey(key);
                    }
                } catch (RepositoryException e) {
                   LOG.error("Error traversing {}",PATH,e);
                }
            }
        return null;
    }
    
    public static class LinkDetails{
        private String scope;
        private String link;
        public String getScope() {
            return scope;
        }
        public void setScope(String scope) {
            this.scope = scope;
        }
        public String getLink() {
            return link;
        }
        public void setLink(String link) {
            this.link = link;
        }
        
    }

    /**
     *
     */
    static class SAXDITAHandler extends DefaultHandler {

        private static final String HASH_SEPARATOR = "#";
        private Map<String,LinkDetails> pwcScopePeer = new HashMap<>();
        private String[] fmditaRefs;

        public SAXDITAHandler() {
            // TODO Auto-generated constructor stub
        }

        public SAXDITAHandler(String[] fmditaRefs) {
            super();
            this.fmditaRefs = fmditaRefs;
        }

        @Override
        public void startElement(final String uri, final String localName, final String qName,
                final Attributes attributes) {

            switch (qName) {
                case DITAConstants.DITA_TAG_PWC_XREF: {
                    String href = attributes.getValue("href");
                    String scope = attributes.getValue("scope");
                    LinkDetails linkDetails = new LinkDetails();
                    linkDetails.setScope(StringUtils.isNotBlank(scope) ? scope : StringUtils.EMPTY);
                    String path = getFormattedPath(href);
                    linkDetails.setLink(path);
                    pwcScopePeer.put(path,linkDetails);
                    break;
                }
                case DITAConstants.DITA_TAG_XREF: {
                    String href = attributes.getValue("href");
                    String scope = attributes.getValue("scope");
                    LinkDetails linkDetails = new LinkDetails();
                    linkDetails.setScope(StringUtils.isNotBlank(scope) ? scope : StringUtils.EMPTY);
                    String path = getFormattedPath(href);
                    linkDetails.setLink(path);
                    pwcScopePeer.put(path,linkDetails);
                    break;
                }
                case DITAConstants.PROPERTY_LINK: {
                    String href = attributes.getValue("href");
                    String scope = attributes.getValue("scope");
                    LinkDetails linkDetails = new LinkDetails();
                    linkDetails.setScope(StringUtils.isNotBlank(scope) ? scope : StringUtils.EMPTY);
                    String path = getFormattedPath(href);
                    linkDetails.setLink(path);
                    pwcScopePeer.put(path,linkDetails);
                    break;
                }
                default:
                    break;
            }
        }

        /**
         * get absolute url
         * @param path
         * @return
         */
        private String getFormattedPath(String path) {
                if (StringUtils.isNotEmpty(path)) {
                    if (path.indexOf(HASH_SEPARATOR) > -1) {
                        path = path.substring(0, path.indexOf(HASH_SEPARATOR));
                    }
                    if (path.lastIndexOf("..") > -1) {
                        path = StringUtils.substringAfterLast(path, "..");
                    }
                    path = parseUrl(path);
                }
            return path;
        }

        /**
         * parse relative url
         * @param path
         * @return
         */
        private String parseUrl(String path) {
            try {
                if (ArrayUtils.isNotEmpty(this.fmditaRefs)) {
                    for (String p : this.fmditaRefs) {
                        if (StringUtils.contains(p, path)) {
                            if (StringUtils.equalsIgnoreCase(MadisonConstants.COMMA_SEPARATOR,
                                    Character.toString(p.charAt(0)))) {
                                p = p.substring(1);
                            }
                            return p;
                        }
                    }
                }
            } catch (Exception e) {
                LOG.error("Error in formatting", e);
            }

            return path;
        }

        /**
         * scope list
         * @return
         */
        public Map<String,LinkDetails> getPwcScopePeer() {
            return pwcScopePeer;
        }
    }
}
