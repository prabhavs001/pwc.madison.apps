package com.pwc.madison.core.services.impl;

import java.io.IOException;

import javax.swing.text.html.HTML;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.rewriter.ProcessingComponentConfiguration;
import org.apache.sling.rewriter.ProcessingContext;
import org.apache.sling.rewriter.Transformer;
import org.apache.sling.rewriter.TransformerFactory;
import org.apache.sling.settings.SlingSettingsService;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;

import com.day.cq.commons.Externalizer;
import com.pwc.madison.core.constants.DITAConstants;
import com.pwc.madison.core.util.DITALinkUtils;
import com.pwc.madison.core.util.MadisonUtil;

/**
 * A TransformerFactory service instance for creating a Transformer to add link type icons into links to documents.
 *
 */
@Component(property = { "pipeline.type=linktype" }, service = { TransformerFactory.class })
public class LinkTypeTransformerFactory implements TransformerFactory {

    private static final Logger log = LoggerFactory.getLogger(LinkTypeTransformerFactory.class);

    @Reference
    private SlingSettingsService slingSettingsService;

    /*
     * (non-Javadoc)
     *
     * @see org.apache.sling.rewriter.TransformerFactory#createTransformer()
     */
    @Override
    public Transformer createTransformer() {
        log.trace("createTransformer");
        return new LinkTypeTransformer();
    }

    private class LinkTypeTransformer implements Transformer {

        private static final String SCOPE_LOCAL = "local";

        private final Logger log = LoggerFactory.getLogger(this.getClass());

        private ContentHandler contentHandler;
        private ResourceResolver resolver;

        private boolean isPageRequest = true;

        private static final String DATA_ATTR_FMGUID = "data-fmguid";
        private static final String DATA_ATTR_SCOPE = "data-scope";
        private static final String DATA_ATTR_OLDLINK = "data-oldlink";
        private static final String DATA_LINK_TRUE = "true";

        @Override
        public void setDocumentLocator(final Locator locator) {
            contentHandler.setDocumentLocator(locator);
        }

        @Override
        public void startDocument() throws SAXException {
            contentHandler.startDocument();
        }

        @Override
        public void endDocument() throws SAXException {
            contentHandler.endDocument();
        }

        @Override
        public void startPrefixMapping(final String prefix, final String uri) throws SAXException {
            contentHandler.startPrefixMapping(prefix, uri);
        }

        @Override
        public void endPrefixMapping(final String prefix) throws SAXException {
            contentHandler.endPrefixMapping(prefix);
        }

        @Override
        public void startElement(final String uri, final String localName, final String qName, final Attributes atts)
                throws SAXException {
            log.debug("PwC custom Link transformer initiated");
            if (HTML.Tag.A.toString().equalsIgnoreCase(localName) && isScopePeer(atts) && isPageRequest
                    && !isOldLink(atts)) {
                final String fmguid = atts.getValue(DATA_ATTR_FMGUID);
                log.debug("fmguid for link: {}", fmguid);
                String link = atts.getValue(HTML.Attribute.HREF.toString());
                log.debug("link: {}", link);
                String hashVal = StringUtils.EMPTY;
                if (link.indexOf(DITAConstants.HASH_STR, 1) > -1) {
                    hashVal = link.substring(link.indexOf(DITAConstants.HASH_STR));
                }

                if (StringUtils.isBlank(link)) {
                    contentHandler.startElement(uri, localName, qName, atts);
                    return;
                }

                boolean isDitaLink = link.endsWith(DITAConstants.DITA_EXTENSION);
                if (link.indexOf(DITAConstants.HASH_STR, 1) > -1) {
                    isDitaLink = link.substring(0, link.indexOf(DITAConstants.HASH_STR))
                            .endsWith(DITAConstants.DITA_EXTENSION);
                }

                if (!isDitaLink) {
                    String path = StringUtils.substringBefore(link, DITAConstants.HASH_STR);
                    path = StringUtils.substringBefore(path, DITAConstants.HTML_EXT);
                    if (null == resolver.getResource(path)) {
                        contentHandler.startElement(uri, localName, qName, atts);
                    } else {
                        startElemWithUpdatedLink(uri, localName, qName, atts, link);
                    }
                    return;
                }

                link = DITALinkUtils.getPageFromXrefDita(resolver, fmguid);
                if (!(StringUtils.isNotBlank(link) && link.endsWith(DITAConstants.HTML_EXT)
                        && null != resolver.getResource(link.substring(0, link.indexOf(DITAConstants.HTML_EXT))))) {
                    link = null;
                }
                if (StringUtils.isBlank(link)) {
                    if (StringUtils.equalsIgnoreCase(Externalizer.AUTHOR,
                            MadisonUtil.getCurrentRunmode(slingSettingsService))) {
                        contentHandler.startElement(uri, localName, qName, atts);
                    }
                    return;
                }

                link = link + hashVal;
                startElemWithUpdatedLink(uri, localName, qName, atts, link);

            } else {
                if (SCOPE_LOCAL.equals(atts.getValue(DATA_ATTR_SCOPE)) && !isOldLink(atts)) {
                    final String link = atts.getValue(HTML.Attribute.HREF.toString());
                    String path = StringUtils.substringBefore(link, DITAConstants.HASH_STR);
                    path = StringUtils.substringBefore(path, DITAConstants.HTML_EXT);
                    if (null == resolver.getResource(path)) {
                        contentHandler.startElement(uri, localName, qName, atts);
                    } else {
                        startElemWithUpdatedLink(uri, localName, qName, atts, link);
                    }
                } else {
                    contentHandler.startElement(uri, localName, qName, atts);
                }
            }
        }

        private void startElemWithUpdatedLink(final String uri, final String localName, final String qName,
                final Attributes atts, final String link) throws SAXException {
            final String updatedLink = updateLinkExtension(link);
            log.debug("updated link: {}", updatedLink);
            final AttributesImpl attr = new AttributesImpl(atts);
            attr.setAttribute(1, StringUtils.EMPTY, HTML.Attribute.HREF.toString(), StringUtils.EMPTY,
                    StringUtils.EMPTY, updatedLink);
            contentHandler.startElement(uri, localName, qName, attr);
        }

        private boolean isOldLink(final Attributes atts) {
            final String oldLink = atts.getValue(DATA_ATTR_OLDLINK);
            boolean isOldLink = false;
            if (DATA_LINK_TRUE.equals(oldLink)) {
                isOldLink = true;
            }
            return isOldLink;
        }

        private String updateLinkExtension(final String link) {
            String updatedLink = link;
            String ext = FilenameUtils.getExtension(link);
            if (link.indexOf(DITAConstants.HASH_STR, 1) > -1) {
                ext = FilenameUtils.getExtension(link.substring(0, link.indexOf(DITAConstants.HASH_STR)));
                if (ext.isEmpty() && !link.isEmpty()) {
                    updatedLink = link.substring(0, link.indexOf(DITAConstants.HASH_STR)) + DITAConstants.HTML_EXT
                            + link.substring(link.indexOf(DITAConstants.HASH_STR));
                }
            } else if (ext.isEmpty() && !link.isEmpty()) {
                updatedLink = link + DITAConstants.HTML_EXT;
            }
            return updatedLink;

        }

        private boolean isScopePeer(final Attributes atts) {
            final String scope = atts.getValue(DATA_ATTR_SCOPE);
            boolean peerScope = false;
            if (DITAConstants.PEER_SCOPE.equals(scope)) {
                peerScope = true;
            }
            return peerScope;
        }

        @Override
        public void endElement(final String uri, final String localName, final String qName) throws SAXException {
            contentHandler.endElement(uri, localName, qName);
        }

        @Override
        public void characters(final char[] ch, final int start, final int length) throws SAXException {
            contentHandler.characters(ch, start, length);
        }

        @Override
        public void ignorableWhitespace(final char[] ch, final int start, final int length) throws SAXException {
            contentHandler.ignorableWhitespace(ch, start, length);
        }

        @Override
        public void processingInstruction(final String target, final String data) throws SAXException {
            contentHandler.processingInstruction(target, data);
        }

        @Override
        public void skippedEntity(final String name) throws SAXException {
            contentHandler.skippedEntity(name);
        }

        @Override
        public void dispose() {

        }

        @Override
        public void init(final ProcessingContext context, final ProcessingComponentConfiguration config)
                throws IOException {
            resolver = context.getRequest().getResourceResolver();
            isPageRequest = !context.getRequest().getPathInfo().contains(DITAConstants.DITA_PWC_TOPIC_CONTAINER_V1);
        }

        @Override
        public void setContentHandler(final ContentHandler handler) {
            contentHandler = handler;
        }

    }

}
