/**
 * Copyright (C) 2011 BonitaSoft S.A.
 * BonitaSoft, 31 rue Gustave Eiffel - 38000 Grenoble
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2.0 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.bonitasoft.migration.forms.utils;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.bonitasoft.migration.forms.TransformFormXML;
import org.bonitasoft.migration.forms.TransformFormXMLFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * @author Qixiang Zhang
 * 
 */
public abstract class XPathUtil {

    /**
     * document builder factory
     */
    protected DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();

    /**
     * document builder
     */
    protected DocumentBuilder builder;

    /**
     * DOM representation of the XML file to create
     */
    protected Document document;

    /**
     * Xpath evaluation accessor
     */
    protected XPath xpathEvaluator = XPathFactory.newInstance().newXPath();

    /**
     * Logger
     */
    protected static Logger LOGGER = Logger.getLogger(XPathUtil.class.getName());
    
    
    /**
     * valid the xml file with xsd file
     */
    protected void validate(final String XSDFileName) throws SAXException, IOException {
        
        final URL url = Thread.currentThread().getContextClassLoader().getResource(XSDFileName);
        final File XSDFile = new File(url.getFile());
        // create a SchemaFactory capable of understanding WXS schemas
        SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
        // load a WXS schema, represented by a Schema instance
        Source schemaFile = new StreamSource(XSDFile);
        Schema schema = factory.newSchema(schemaFile);
        // create a Validator instance, which can be used to validate an instance document
        Validator validator = schema.newValidator();
        // validate the DOM tree
        validator.validate(new DOMSource(document));
        
    }
    
    
    
    /**
     * parse bar file to document
     * 
     * @param barfileName
     * @throws IOException
     * @throws TransformerException
     * @throws SAXException
     */
    protected void parse(final String barfileName) throws IOException, TransformerException, SAXException {

        TransformFormXML transformer = TransformFormXMLFactory.getInstance().newTransformer();
        final URL url = Thread.currentThread().getContextClassLoader().getResource(barfileName);
        final File barFile = new File(url.getFile());
        File formsXMLFile = transformer.transform(barFile);
        try {
            builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        } catch (ParserConfigurationException e) {
            LOGGER.log(Level.SEVERE, "Error while parse the new transformed forms.xml file.", e);
        }
        document = builder.parse(formsXMLFile);
    }
    
    protected void parse(final File formsXMLFile) throws IOException, TransformerException, SAXException {
        try {
            builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        } catch (ParserConfigurationException e) {
            LOGGER.log(Level.SEVERE, "Error while parse the new transformed forms.xml file.", e);
        }
        document = builder.parse(formsXMLFile);
    }

    /**
     * Retrieve the child node of a node using XPath
     * 
     * @param parentNode the node
     * @param xPath the XPath expression
     * @return the child {@link Node}
     */
    protected Node getNodeByXpath(final Node parentNode, final String xPath) {
        Node node = null;
        try {
            node = (Node) xpathEvaluator.evaluate(xPath, parentNode, XPathConstants.NODE);
        } catch (final XPathExpressionException e) {
            if (LOGGER.isLoggable(Level.SEVERE)) {
                LOGGER.log(Level.SEVERE, "Error in Xpath expression", e);
            }
        }
        return node;
    }

    /**
     * Retrieve the child string of a node using XPath
     * 
     * @param parentNode the node
     * @param xPath the XPath expression
     * @return the child String
     */
    protected String getStringByXpath(final Node parentNode, final String xPath) {
        String string = null;
        try {
            string = (String) xpathEvaluator.evaluate(xPath, parentNode, XPathConstants.STRING);
        } catch (final XPathExpressionException e) {
            if (LOGGER.isLoggable(Level.SEVERE)) {
                LOGGER.log(Level.SEVERE, "Error in Xpath expression", e);
            }
        }
        return string;
    }
    
    /**
     * Retrieve the child string of a node using XPath
     * 
     * @param xPath the XPath expression
     * @return the child String
     */
    protected String getStringByXpath(final String xPath) {
        String string = null;
        try {
            string = (String) xpathEvaluator.evaluate(xPath, document, XPathConstants.STRING);
        } catch (final XPathExpressionException e) {
            if (LOGGER.isLoggable(Level.SEVERE)) {
                LOGGER.log(Level.SEVERE, "Error in Xpath expression", e);
            }
        }
        return string;
    }

    /**
     * Retrieve the child node list of a node using XPath
     * 
     * @param parentNode the node
     * @param xPath the XPath expression
     * @return the child {@link NodeList}
     */
    protected NodeList getNodeListByXpath(final Node parentNode, final String xPath) {
        NodeList nodeList = null;
        try {
            nodeList = (NodeList) xpathEvaluator.evaluate(xPath, parentNode, XPathConstants.NODESET);
        } catch (final XPathExpressionException e) {
            if (LOGGER.isLoggable(Level.SEVERE)) {
                LOGGER.log(Level.SEVERE, "Error in Xpath expression", e);
            }
        }
        return nodeList;
    }

    /**
     * Retrieve the string content of a node (needed for non mandatory nodes)
     * 
     * @param node the node
     * @return the String value of the node if it exists. empty String otherwise
     */
    protected String getStringValue(final Node node) {
        if (node != null) {
            return node.getTextContent();
        } else {
            return "";
        }
    }

    /**
     * Retrieve the integer content of a node (Needed for non mandatory nodes)
     * 
     * @param node the node
     * @return the int value of the node if it exists. 0 otherwise
     */
    protected int getIntValue(final Node node) {
        if (node != null) {
            try {
                return Integer.parseInt(node.getTextContent());
            } catch (final IllegalArgumentException nfe) {
                if (LOGGER.isLoggable(Level.SEVERE)) {
                    LOGGER.log(Level.SEVERE, "the widget property " + node.getNodeName() + " should be numeric", nfe);
                }
            }
        }
        return 0;
    }

    /**
     * Retrieve the boolean content of a node (Needed for non mandatory nodes)
     * 
     * @param node the node
     * @return the boolean value of the node if it exists. false otherwise
     */
    protected boolean getBooleanValue(final Node node) {
        if (node != null) {
            return Boolean.valueOf(node.getTextContent());
        }
        return false;
    }
}
