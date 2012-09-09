/**
 * Copyright (C) 2009 BonitaSoft S.A.
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
package org.bonitasoft.forms.server.accessor.impl;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.bonitasoft.forms.server.accessor.DefaultFormsPropertiesFactory;
import org.bonitasoft.forms.server.accessor.IProcessConfigDefAccessor;
import org.bonitasoft.forms.server.accessor.impl.util.XPathUtil;
import org.bonitasoft.forms.server.constants.XMLForms;
import org.bonitasoft.forms.server.exception.InvalidFormDefinitionException;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

/**
 * Implementation of {@link IProcessConfigDefAccessor} allowing to parse the xml form definition file to get the process config
 * 
 * @deprecated
 * @author Anthony Birembaut
 */
public class XMLProcessConfigDefAccessorImpl extends XPathUtil implements IProcessConfigDefAccessor {
    
    /**
     * DOM representation of the XML file
     */
    private final Document document;

    /**
     * Logger
     */
    private static Logger LOGGER = Logger.getLogger(XMLProcessConfigDefAccessorImpl.class.getName());

    /**
     * The xpath query to get the process node
     */
    private final String processXpath;
    
    /**
     * Constructor
     * 
     * @param document
     */
    public XMLProcessConfigDefAccessorImpl(final Document document) {

        this.document = document;
        
        //elaborates the process xpath query
        final StringBuilder processXpathBuilder = new StringBuilder();
        processXpathBuilder.append("//");
        processXpathBuilder.append(XMLForms.PROCESS);
        processXpath = processXpathBuilder.toString();
    }
    
    /**
     * {@inheritDoc}
     */
    public String getProcessLabel() throws InvalidFormDefinitionException {

        String processLabel = null;
        
        final String xpath = processXpath + "/" + XMLForms.PROCESS_LABEL;

        final Node processLabelNode = getNodeByXpath(document, xpath);
        if (processLabelNode == null) {
            if (LOGGER.isLoggable(Level.SEVERE)) {
                LOGGER.log(Level.SEVERE, "Failed to retrieve process label element. query : " + xpath);
            }
            throw new InvalidFormDefinitionException("The process label was not found in the forms definition file");
        } else {
            processLabel = processLabelNode.getTextContent();
        }
        return processLabel;
    }

    /**
     * {@inheritDoc}
     */
    public String getProcessTemplate() {

        String processFormTemplate = null;

        final String xpath = processXpath + "/" + XMLForms.PROCESS_TEMPLATE;

        final Node processTemplateNode = getNodeByXpath(document, xpath);
        if (processTemplateNode == null) {
            if (LOGGER.isLoggable(Level.INFO)) {
                LOGGER.log(Level.INFO, "Failed to retrieve process template element. Default process template will be used.");
            }
            processFormTemplate =  DefaultFormsPropertiesFactory.getDefaultFormProperties().getApplicationLayout();
        } else {
            processFormTemplate = processTemplateNode.getTextContent();
        }
        return processFormTemplate;
    }

    /**
     * {@inheritDoc}
     */
    public String getProcessMandatorySymbol() {

        String processMandatorySymbol = null;

        final String xpath = processXpath + "/" + XMLForms.MANDATORY_SYMBOL;

        final Node processMandatorySymbolNode = getNodeByXpath(document, xpath);
        if (processMandatorySymbolNode == null) {
            if (LOGGER.isLoggable(Level.INFO)) {
                LOGGER.log(Level.INFO, "Failed to retrieve process mandatory symbol element. Default symbol will be used.");
            }
            processMandatorySymbol =  DefaultFormsPropertiesFactory.getDefaultFormProperties().getApplicationMandatorySymbol();
        } else {
            processMandatorySymbol = processMandatorySymbolNode.getTextContent();
        }
        return processMandatorySymbol;
    }
    
    /**
     * {@inheritDoc}
     */
    public String getProcessMandatoryLabel() {

        String processMandatoryLabel = null;

        final String xpath = processXpath + "/" + XMLForms.MANDATORY_LABEL;

        final Node processMandatorySymbolNode = getNodeByXpath(document, xpath);
        if (processMandatorySymbolNode == null) {
            if (LOGGER.isLoggable(Level.INFO)) {
                LOGGER.log(Level.INFO, "Failed to retrieve process mandatory label element. Default label will be used.");
            }
            processMandatoryLabel =  DefaultFormsPropertiesFactory.getDefaultFormProperties().getApplicationMandatoryLabel();
        } else {
            processMandatoryLabel = processMandatorySymbolNode.getTextContent();
        }
        return processMandatoryLabel;
    }
    
    /**
     * {@inheritDoc}
     */
    public String getProcessMandatorySymbolStyle() {

        String processMandatorySymbolClasses = null;

        final String xpath = processXpath + "/" + XMLForms.MANDATORY_STYLE;

        final Node processMandatorySymbolClassesNode = getNodeByXpath(document, xpath);
        if (processMandatorySymbolClassesNode == null) {
            if (LOGGER.isLoggable(Level.INFO)) {
                LOGGER.log(Level.INFO, "No CSS classes were found in the definition file for the process mandatory symbol element. Default style will be used");
            }
        } else {
            processMandatorySymbolClasses = processMandatorySymbolClassesNode.getTextContent();
        }
        return processMandatorySymbolClasses;
    }
    
    /**
     * {@inheritDoc}
     */
    public String getProcessErrorTemplate() {

        String processErrorTemplate = null;

        final String xpath = processXpath + "/" + XMLForms.ERROR_TEMPLATE;

        final Node processTemplateNode = getNodeByXpath(document, xpath);
        if (processTemplateNode == null) {
            if (LOGGER.isLoggable(Level.INFO)) {
                LOGGER.log(Level.INFO, "Failed to retrieve error template element. The default error template will be used.");
            }
            processErrorTemplate =  DefaultFormsPropertiesFactory.getDefaultFormProperties().getPageErrorTemplate();
        } else {
            processErrorTemplate = processTemplateNode.getTextContent();
        }
        return processErrorTemplate;
    }
    
    /**
     * {@inheritDoc}
     */
    public String getProcessConfirmationTemplate() {

        String path = null;

        final String xpath = processXpath + "/" + XMLForms.CONFIRMATION_TEMPLATE;

        final Node confirmationTemplateNode = getNodeByXpath(document, xpath);
        if (confirmationTemplateNode == null) {
            if (LOGGER.isLoggable(Level.INFO)) {
                LOGGER.log(Level.INFO, "No confirmation template was found. The default confirmation page will be used.");
            }
            path =  DefaultFormsPropertiesFactory.getDefaultFormProperties().getPageConfirmationTemplate();
        } else {
            path = confirmationTemplateNode.getTextContent();
        }
        return path;
    }
    
    /**
     * {@inheritDoc}
     */
    public String getProcessConfirmationMessage() {
        
        String message = null;

        final String xpath = processXpath + "/" + XMLForms.CONFIRMATION_MESSAGE;
        
        final Node confirmationTemplateNode = getNodeByXpath(document, xpath);
        if (confirmationTemplateNode == null) {
            if (LOGGER.isLoggable(Level.INFO)) {
                LOGGER.log(Level.INFO, "No confirmation message was found. The default confirmation message will be used.");
            }
        } else {
            message = confirmationTemplateNode.getTextContent();
        }
        return message;
    }
    
    /**
     * {@inheritDoc}
     */
    public String getProcessName() throws InvalidFormDefinitionException {
        
        String processName = null;

        final String xpath = processXpath;

        final Node processNode = getNodeByXpath(document, xpath);
        if (processNode == null) {
            if (LOGGER.isLoggable(Level.SEVERE)) {
                LOGGER.log(Level.SEVERE, "Failed to retrieve process element. query : " + xpath);
            }
            throw new InvalidFormDefinitionException("The process was not found in the forms definition file");
        } else {
            processName = getStringByXpath(processNode, "@" + XMLForms.NAME);
        }
        return processName;
    }

    /**
     * {@inheritDoc}
     */
    public String getProcessVersion() throws InvalidFormDefinitionException {
        
        String processVersion = null;

        final String xpath = processXpath;

        final Node processNode = getNodeByXpath(document, xpath);
        if (processNode == null) {
            if (LOGGER.isLoggable(Level.SEVERE)) {
                LOGGER.log(Level.SEVERE, "Failed to retrieve process element. query : " + xpath);
            }
            throw new InvalidFormDefinitionException("The process was not found in the forms definition file");
        } else {
            processVersion = getStringByXpath(processNode, "@" + XMLForms.VERSION);
        }
        return processVersion;
    }
    
}
