package com.pwc.madison.core.util;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.jcr.Node;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import com.pwc.madison.core.constants.DITAConstants;
import org.apache.jackrabbit.JcrConstants;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * Util Methods for XHTML/DOCX to DITA Conversion
 */

public class ConversionUtils {

    private ConversionUtils() {
    }

    public static final String h2d_IO_XmlPath = "fmdita/config/h2d_io.xml";
    public static final String inputDirs = "inputDirs";
    public static final String outputDirs = "outputDirs";
    public static final String PN_INPUT_DIR_PATH = "inputDirPath";

    private static final Logger LOG = LoggerFactory.getLogger(ConversionUtils.class);

    /**
     * Get the html configs from the h2d config file. Parsing the xml and getting the values. Mostly a copy of product code
     *
     * @param resourceResolver
     * @return
     */
    public static Map<String, List<String>> getHTMLConfigs(final ResourceResolver resourceResolver) {
        final Map<String, List<String>> htmlConfigs = new HashMap<String, List<String>>();

        if (null == resourceResolver) {
            return htmlConfigs;
        }

        final Resource htmlConfigResource = resourceResolver.getResource(h2d_IO_XmlPath);
        final Resource contentResource = htmlConfigResource.getChild(JcrConstants.JCR_CONTENT);

        if (null == contentResource) {
            return htmlConfigs;
        }

        final Node contentNode = contentResource.adaptTo(Node.class);
        InputStream inputStream = null;

        try {
            inputStream = contentNode.getProperty(JcrConstants.JCR_DATA).getBinary().getStream();
        } catch (final Exception e) {
            LOG.error("Error while reading the config file {}", htmlConfigResource.getPath(), e);
        }

        if (null == inputStream) {
            return htmlConfigs;
        }

        try {
            // parse the config file and get the configured output directories
            final List<String> inputDirs = new ArrayList<>();
            final List<String> outputDirs = new ArrayList<>();

            final SAXParserFactory parserFactory = SAXParserFactory.newInstance();
            parserFactory.setFeature(DITAConstants.EXTERNAL_GENERAL_ENTITIES, false);
            parserFactory.setFeature(DITAConstants.EXTERNAL_PARAMETER_ENTITIES, false);
            parserFactory.setFeature(DITAConstants.EXTERNAL_DTD_PATH, false);
            parserFactory.setXIncludeAware(false);
            final SAXParser parser = parserFactory.newSAXParser();
            final XMLParser handler = new XMLParser(inputDirs, outputDirs);
            parser.parse(inputStream, handler);

            htmlConfigs.put(ConversionUtils.inputDirs, inputDirs);
            htmlConfigs.put(ConversionUtils.outputDirs, outputDirs);

        } catch (final SAXException | ParserConfigurationException | IOException e) {
            LOG.error("Error while parsing the config file {}", htmlConfigResource.getPath(), e);
        }

        return htmlConfigs;
    }

    // XML Parser to parse the config files
    private static class XMLParser extends DefaultHandler {
        String content = "";
        List<String> inputDirs = new ArrayList<>();
        List<String> outputDirs = new ArrayList<>();

        public XMLParser(final List<String> inputDirs, final List<String> outputDirs) {
            this.inputDirs = inputDirs;
            this.outputDirs = outputDirs;
        }

        @Override
        public void startElement(final String uri, final String localName, final String qName,
                final Attributes attributes) throws SAXException {

        }

        @Override
        public void endElement(final String uri, final String localName, final String qName) throws SAXException {
            switch (qName.toLowerCase()) {
                case "inputdir":
                    inputDirs.add(content);
                    break;
                case "outputdir":
                    outputDirs.add(content);
                    break;
            }
        }

        @Override
        public void characters(final char[] ch, final int start, final int length) throws SAXException {
            content = String.copyValueOf(ch, start, length).trim();
        }
    }
}
