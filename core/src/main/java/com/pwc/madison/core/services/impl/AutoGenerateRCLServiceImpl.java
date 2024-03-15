package com.pwc.madison.core.services.impl;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import com.pwc.madison.core.util.DITAUtils;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.resource.ResourceResolver;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Document;
import org.w3c.dom.DocumentType;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.adobe.granite.asset.api.Asset;
import com.adobe.granite.asset.api.AssetManager;
import com.day.cq.dam.api.DamConstants;
import com.pwc.madison.core.constants.DITAConstants;
import com.pwc.madison.core.constants.MadisonConstants;
import com.pwc.madison.core.services.AutoGenerateRCLService;

/**
 * This is a service implmenentation which will update the inline section and generate Inline links which are identified
 * in the the "pwc-body" element for a provided ditaPath
 * 
 * @author sevenkat
 *
 */

@Component(service = { AutoGenerateRCLService.class }, immediate = true)
public class AutoGenerateRCLServiceImpl implements AutoGenerateRCLService {

    private static final String DOCTYPE = "DOCTYPE";
    private static final String PUBLIC = "PUBLIC";
    private static final String SCOPE = "scope";
    private static final String LINKTEXT = "linktext";
    private static final String RELATED_LINKS = "related-links";
    private static final String INLINE_LINKS = "inline-links";
    private static final String LINKLIST = "linklist";
    private static final String YES_STRING = "yes";
    private static final String NO_STRING = "no";
    private static final String HREF = "href";
    private static final String XML = "xml";
    private static final String FORMAT = "format";
    private static final String RELATED_CONTENT = "related-content";
    private static final String DTD_REGEX = "\"([^\"]*)\"";

    private static final Logger LOG = LoggerFactory.getLogger(AutoGenerateRCLServiceImpl.class);

    /**
     * Method takes the dita path and resolver from servlet and generate RCL's Under in-line Links Section.
     *
     */
    @Override
    public void updateInlineLinks(String ditaPath, ResourceResolver resolver) {
        InputStream inputStream = null;
        InputStream assetStream = null;
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try {
            AssetManager assetMgr = resolver.adaptTo(AssetManager.class);
            if (null == assetMgr) {
                return;
            }
            
            Asset dita = assetMgr.getAsset(ditaPath);
            if (null == dita) {
                return;
            }
            
            inputStream = dita.getRendition(DamConstants.ORIGINAL_FILE).getStream();
            Document document = DITAUtils.convertInputStreamToDocument(inputStream);
            if (null != document) {
                populateInlineLinksFromDocument(document, ditaPath);
                assetStream = convertDocumentToInputStream(document, inputStream,outputStream);
                dita.setRendition(DamConstants.ORIGINAL_FILE, assetStream, dita.getValueMap());
                resolver.refresh();
                resolver.commit();
            }
        } catch (TransformerException | IOException | SAXException | ParserConfigurationException e) {
            LOG.error("Error Occurred while processing dita object{} ", e);
        } finally {
            try {
                if (null != inputStream) {
                    inputStream.close();
                }
                if (null != assetStream) {
                    assetStream.close();
                }
                    outputStream.close();
            } catch (IOException e) {
                LOG.error("Error Occurred in finally block while closing the opened streams {} ", e);
            }
        }
    }

    /**
     * This method will iterate through the pwc-body element of the DITA Xml and found out all available Xref nodes
     * which will have href url ended with .dita then saves them to an array list
     * 
     * @param document
     * @param ditaPath
     * 
     * @return the arrayList
     */
    private void populateInlineLinksFromDocument(Document document, String ditaPath) {
        List<Node> inlineLinksList = new ArrayList<>();
        Node inlineLinksNode = null;
        NodeList xRefList = document.getElementsByTagName(RELATED_CONTENT);
        List<String> hrefs = new ArrayList<>();
        for (int i = 0; i < xRefList.getLength(); i++) {
            String value = getAttributeValue(xRefList.item(i), MadisonConstants.VALUE_PROPERTY);
            if (null != value && value.equals(YES_STRING)) {
                Node pwcXrefNode = xRefList.item(i).getParentNode();
                if (null != pwcXrefNode && pwcXrefNode.getNodeName().equals(DITAConstants.DITA_TAG_PWC_XREF)) {
                    String linkType = getAttributeValue(pwcXrefNode, HREF);
                    inlineLinksNode = getInlineLinksNode(document);
                    constructInlineLinksList(ditaPath, inlineLinksList, inlineLinksNode, hrefs, pwcXrefNode, linkType);
                }
            }
        }
        updateInlineLinksSection(document, inlineLinksList, inlineLinksNode);
    }

    /**
     * 
     * This method will check all the exclusion conditions for an RCL and then adds to the LIST of in-line links
     * 
     * @param ditaPath
     * @param inlineLinksList
     * @param inlineLinksNode
     * @param hrefs
     * @param pwcXrefNode
     * @param linkType
     */
    private void constructInlineLinksList(String ditaPath, List<Node> inlineLinksList, Node inlineLinksNode,
            List<String> hrefs, Node pwcXrefNode, String linkType) {
        if (null != linkType && null != inlineLinksNode) {
            boolean flag = checkLinkIfExists(linkType,pwcXrefNode,inlineLinksNode);
            String ditaName = ditaPath.substring(ditaPath.lastIndexOf(MadisonConstants.FORWARD_SLASH)+1,
                    ditaPath.length());
            if (flag && !hrefs.contains(linkType) && !linkType.contains(ditaName)
                    && linkType.contains(MadisonConstants.SLING_SELECTORS_DITA)) {
                hrefs.add(linkType);
                inlineLinksList.add(pwcXrefNode);
            }
        }
    }

    /**
     * This method checks for Inline links node in a dita document returns inline link node with type inline-links
     * 
     * @param document
     * @return
     */
    private Node getInlineLinksNode(Document document) {
        NodeList relLinkList = document.getElementsByTagName(RELATED_LINKS);
        for (int k = 0; k < relLinkList.getLength(); k++) {
            Node nNode = relLinkList.item(k);
            Element eElement = (Element) nNode;
            Node parentNode = eElement.getElementsByTagName(LINKLIST).item(k);
            String type = getAttributeValue(parentNode, DITAConstants.PROPERTY_TYPE);
            if (null != type && type.equals(INLINE_LINKS)) {
                return parentNode;
            }
        }
        return null;
    }

    /**
     * This method will generate the inline links from the provided arraylist which will have xref nodes from the
     * pwc-body element
     * 
     * @param document
     * @param inlineLinksList
     * @param linksNode
     */
    private void updateInlineLinksSection(Document document, List<Node> inlineLinksList, Node linksNode) {
        if (null != linksNode && !inlineLinksList.isEmpty()) {
            for (Node node : inlineLinksList) {
                Element link = document.createElement(DITAConstants.PROPERTY_LINK);
                Element linkText = document.createElement(LINKTEXT);
                populateAttribute(node, link, HREF);
                populateAttribute(node, link, FORMAT);
                populateAttribute(node, link, SCOPE);
                populateAttribute(node, linkText, null);
                link.appendChild(linkText);
                linksNode.appendChild(link);
            }
            removeEmptyInlineTag(linksNode);
        }
    }

    /**
     * This removes the helper text for in-line line links section if any in-line links are present.
     * 
     * @param parentNode
     */
    private void removeEmptyInlineTag(Node parentNode) {
        Element eElement = (Element) parentNode;
        NodeList list = eElement.getElementsByTagName(DITAConstants.PROPERTY_LINK);
        if (list.getLength() > 1) {
            for (int i = 0; i < list.getLength(); i++) {
                Node linkNode = list.item(i);
                if (null != linkNode && !linkNode.hasAttributes()) {
                    parentNode.removeChild(linkNode);
                }
            }
        }
    }

    /**
     * This method will check if the href link already present in inline links section, if not present then only link
     * will get added to the Inline links section
     * @param attributeValue
     * @param pwcXrefNode
     * @param parentNode
     * @return boolean flag to add node or not
     */
    private boolean checkLinkIfExists(String attributeValue, Node pwcXrefNode, Node parentNode) {
        Element element = (Element)parentNode;
        NodeList nodeList=element.getElementsByTagName(DITAConstants.PROPERTY_LINK);
        for (int i = 0; i < nodeList.getLength(); i++) {
            if(!StringUtils.equals(attributeValue, getAttributeValue(nodeList.item(i), HREF))){
                String inlineLinkName=nodeList.item(i).getTextContent();
                String rclLinkName=pwcXrefNode.getTextContent();
                    if(null!=inlineLinkName && null!=rclLinkName && StringUtils.equals(inlineLinkName.trim(), rclLinkName.trim())){
                        parentNode.removeChild(nodeList.item(i));
                        return  Boolean.TRUE;
                    }
            }else {
                return  Boolean.FALSE;
            }
        }
        return Boolean.TRUE;
    }


    /**
     * This method converts the modified document to XML Input Stream.
     * 
     * @param document
     * @param inputStream
     * @param outputStream 
     * @return the modified input stream
     * @throws TransformerException
     * @throws IOException
     */
    private InputStream convertDocumentToInputStream(Document document, InputStream inputStream, ByteArrayOutputStream outputStream)
            throws TransformerException, IOException {
        String pwcDtd = getDocumentDTDFromInputStream(inputStream);
        if (StringUtils.isNotBlank(pwcDtd)) {
            List<String> dtdList = new ArrayList<>();
            String[] docTypes = pwcDtd.split(PUBLIC);
            if (null != docTypes && docTypes.length > 1) {
                Pattern p = Pattern.compile(DTD_REGEX);
                Matcher m = p.matcher(docTypes[1]);
                while (m.find()) {
                    dtdList.add(m.group(1));
                }
            }
            TransformerFactory tfactory = TransformerFactory.newInstance();
            tfactory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            tfactory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            tfactory.setAttribute(XMLConstants.ACCESS_EXTERNAL_STYLESHEET, "");
            Transformer transf = tfactory.newTransformer();
            transf.setOutputProperty(OutputKeys.INDENT, YES_STRING);
            transf.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, NO_STRING);
            transf.setOutputProperty(OutputKeys.METHOD, XML);
            if (CollectionUtils.isNotEmpty(dtdList)) {
                DOMImplementation impl = document.getImplementation();
                DocumentType doctype = impl.createDocumentType(DOCTYPE.toLowerCase(), dtdList.get(0), dtdList.get(1));
                transf.setOutputProperty(OutputKeys.DOCTYPE_PUBLIC, doctype.getPublicId());
                transf.setOutputProperty(OutputKeys.DOCTYPE_SYSTEM, doctype.getSystemId());

            }
            Source xmlSource = new DOMSource(document);
            Result outputTarget = new StreamResult(outputStream);
            transf.transform(xmlSource, outputTarget);
            return new ByteArrayInputStream(outputStream.toByteArray());
        }
        return null;
    }

    /**
     * This method gets the DTD declaration from the InputStream
     * 
     * 
     * @param String
     *            with DTD declaration
     * @return
     * @throws IOException
     */
    private String getDocumentDTDFromInputStream(InputStream is) throws IOException {
        String line;
        BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
        try {
            is.reset();
            while ((line = br.readLine()) != null) {
                if (line.contains(DOCTYPE)) {
                    return line;
                }
            }
        } finally {
            br.close();
        }
        return null;
    }

    /**
     * Will set the element name and element value for a required XML tag
     * 
     * @param node
     *            any XML node
     * @param element
     *            Element to check like prefix
     * @param elementName
     *            XML attributes
     */
    private void populateAttribute(Node node, Element element, String elementName) {
        if (StringUtils.isBlank(elementName)) {
            element.setTextContent(node.getTextContent());
        } else {
            if (null != node && node.hasAttributes() && null != node.getAttributes().getNamedItem(elementName)) {
                element.setAttribute(elementName, node.getAttributes().getNamedItem(elementName).getTextContent());
            }
        }
    }

    /**
     * This method will give the text value of the associated XML element or node
     * 
     * @param attributeNode
     * @param attribute
     * @return
     */
    private String getAttributeValue(Node attributeNode, String attribute) {
        if (null != attributeNode && attributeNode.hasAttributes()
                && null != attributeNode.getAttributes().getNamedItem(attribute)) {
            return attributeNode.getAttributes().getNamedItem(attribute).getTextContent();
        }
        return null;
    }
}
