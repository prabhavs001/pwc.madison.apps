package com.pwc.madison.core.util;

import java.io.StringReader;
import java.io.StringWriter;

import javax.xml.XMLConstants;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.apache.commons.lang3.StringUtils;

/**
 * Utility Class for formatting XML
 * 
 * @author vhs
 *
 */
public class XMLFormatter {

    public String format(String input) {
        if (StringUtils.isBlank(input)) {
            return StringUtils.EMPTY;
        }
        return prettyFormat(input, "2");
    }

    public static String prettyFormat(String input, String indent) {
        // Null check.
        if (StringUtils.isBlank(input)) {
            return StringUtils.EMPTY;
        }
        // Format the input XML to pretty.
        Source xmlInput = new StreamSource(new StringReader(input));
        StringWriter stringWriter = new StringWriter();
        try {
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            transformerFactory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            transformerFactory.setAttribute(XMLConstants.ACCESS_EXTERNAL_STYLESHEET, "");
            Transformer transformer = transformerFactory.newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty(OutputKeys.DOCTYPE_PUBLIC, "yes");
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", indent);
            transformer.transform(xmlInput, new StreamResult(stringWriter));

            return stringWriter.toString().trim();
        } catch (Exception e) {
            if (StringUtils.isBlank(stringWriter.toString())) {
                return input;
            }
            throw new RuntimeException(e);
        }
    }
}
