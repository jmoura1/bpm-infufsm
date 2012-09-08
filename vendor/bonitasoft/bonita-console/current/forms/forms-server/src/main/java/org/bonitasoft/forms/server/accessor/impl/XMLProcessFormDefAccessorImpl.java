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

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.bonitasoft.forms.client.model.FormAction;
import org.bonitasoft.forms.client.model.FormValidator;
import org.bonitasoft.forms.client.model.FormWidget;
import org.bonitasoft.forms.client.model.TransientData;
import org.bonitasoft.forms.server.accessor.DefaultFormsPropertiesFactory;
import org.bonitasoft.forms.server.accessor.IFormDefAccessor;
import org.bonitasoft.forms.server.accessor.impl.util.XPathUtil;
import org.bonitasoft.forms.server.accessor.widget.WidgetBuilderFactory;
import org.bonitasoft.forms.server.constants.XMLForms;
import org.bonitasoft.forms.server.exception.InvalidFormDefinitionException;
import org.bonitasoft.forms.server.exception.ProcessFormDefinitionNotFoundException;
import org.ow2.bonita.facade.uuid.ProcessDefinitionUUID;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Implementation of {@link IFormDefAccessor} allowing to parse the xml form definition file to generate a process instantiation form model
 * 
 * @deprecated
 * @author Anthony Birembaut
 */
public class XMLProcessFormDefAccessorImpl extends XPathUtil implements IFormDefAccessor {
    
    /**
     * Logger
     */
    private static Logger LOGGER = Logger.getLogger(XMLProcessFormDefAccessorImpl.class.getName());
    
    /**
     * DOM representation of the XML file
     */
    protected Document document;
    
    /**
     * indicates whether the page has to be displayed in edit mode (for an instantiation) or not (view mode)
     */
    protected boolean isEditMode;
    
    /**
     * indicates whether the page is an instance recap or not (otherwise it's a process instantiation page)
     */
    protected boolean isInstanceRecap;
    
    /**
     * the process definition UUID
     */
    protected ProcessDefinitionUUID processDefinitionUUID;
    
    /**
     * the user locale
     */
    protected String locale;

    /**
     * the {@link Date} of the process deployment
     */
    protected Date processDeploymentDate;
    
    /**
     * The xpath query to get the process node
     */
    protected String processXpath;
    
    /**
     * Constructor
     * 
     * @param document
     * @param processDefinitionUUID
     * @param locale the user locale
     * @param processDeployementDate 
     * @param isInstanceRecap
     * @param isEditMode true if the edit page is required, false if it's the view page
     * @throws ProcessFormDefinitionNotFoundException 
     */
    public XMLProcessFormDefAccessorImpl(final Document document, final ProcessDefinitionUUID processDefinitionUUID, final String locale, final Date processDeploymentDate, final boolean isInstanceRecap, final boolean isEditMode) throws ProcessFormDefinitionNotFoundException {

        this.document = document;
        this.processDefinitionUUID = processDefinitionUUID;
        this.processDeploymentDate = processDeploymentDate;
        this.isEditMode = isEditMode;
        this.isInstanceRecap = isInstanceRecap;
        this.locale = locale;
        
        //elaborates the process xpath query
        final StringBuilder processXpathBuilder = new StringBuilder();
        processXpathBuilder.append("//");
        processXpathBuilder.append(XMLForms.PROCESS);
        processXpath = processXpathBuilder.toString();

        Node pageflowNode = null;
        if (isInstanceRecap) {
            pageflowNode = getNodeByXpath(document, processXpath + "/" + XMLForms.RECAP_PAGEFLOW);
        } else {
            if (isEditMode) {
                pageflowNode = getNodeByXpath(document, processXpath + "/" + XMLForms.PAGEFLOW);
            } else {
                pageflowNode = getNodeByXpath(document, processXpath + "/" + XMLForms.VIEW_PAGEFLOW);
            }
        }
        if (pageflowNode == null) {
            final String message = "The pageflow node for the process was not found in the forms definition file";
            if (LOGGER.isLoggable(Level.INFO)) {
                LOGGER.log(Level.INFO, message);
            }
            throw new ProcessFormDefinitionNotFoundException(message);
        }
    }

    /**
     * {@inheritDoc}
     */
    public String getPageLabel(final String pageId) throws InvalidFormDefinitionException {

        String label = null;

        String xpath = getPageXpath(pageId, isEditMode);

        final Node pageNode = getPageNode(pageId);
        if (pageNode == null) {
            final String errorMessage = "Failed to parse the forms definition file. Page " + pageId + " not found.";
            if (LOGGER.isLoggable(Level.SEVERE)) {
                LOGGER.log(Level.SEVERE, errorMessage);
            }
            throw new InvalidFormDefinitionException(errorMessage);
        } else {
            xpath = XMLForms.PAGE_LABEL;
            final Node pageLabelNode = getNodeByXpath(pageNode, xpath);
            if (pageLabelNode == null) {
                if (LOGGER.isLoggable(Level.SEVERE)) {
                    LOGGER.log(Level.SEVERE, "Failed to parse the form definition file. query : " + xpath);
                }
                throw new InvalidFormDefinitionException("The label for page " + pageId + " for process instantiation was not found in the forms definition file");
            } else {
                label = pageLabelNode.getTextContent();
            }
        }
        return label;
    }
    
    /**
     * {@inheritDoc}
     */
    public boolean isHTMLAllowedInLabel(final String pageId) throws InvalidFormDefinitionException {

        boolean allowHTMLInLabel = false;

        String xpath = getPageXpath(pageId, isEditMode);

        final Node pageNode = getPageNode(pageId);
        if (pageNode == null) {
            final String errorMessage = "Failed to parse the forms definition file. Page " + pageId + " not found.";
            if (LOGGER.isLoggable(Level.SEVERE)) {
                LOGGER.log(Level.SEVERE, errorMessage);
            }
            throw new InvalidFormDefinitionException(errorMessage);
        } else {
            xpath = XMLForms.ALLOW_HTML_IN_LABEL;
            final Node pageAllowHTMLNode = getNodeByXpath(pageNode, xpath);
            if (pageAllowHTMLNode != null) {
                final String allowHTMLInLabelStr = pageAllowHTMLNode.getTextContent();
                allowHTMLInLabel = Boolean.parseBoolean(allowHTMLInLabelStr);
            }
        }
        return allowHTMLInLabel;
    }

    /**
     * {@inheritDoc} 
     */
    public String getFormPageTemplate(final String pageId) throws InvalidFormDefinitionException {

        String templatePath = null;

        final Node pageNode = getPageNode(pageId);
        if (pageNode == null) {
            final String errorMessage = "Failed to parse the forms definition file. Page " + pageId + " not found.";
            if (LOGGER.isLoggable(Level.SEVERE)) {
                LOGGER.log(Level.SEVERE, errorMessage);
            }
            throw new InvalidFormDefinitionException(errorMessage);
        } else {
            final Node pageTemplateNode =  getNodeByXpath(pageNode, XMLForms.PAGE_TEMPLATE);
            if (pageTemplateNode == null) {
                if (LOGGER.isLoggable(Level.WARNING)) {
                    LOGGER.log(Level.WARNING, "Failed to parse the form definition file. The default page template will be used.");
                }
                templatePath = null;
            } else {
                templatePath = pageTemplateNode.getTextContent();
            }
        }
        return templatePath;
    }

    /**
     * {@inheritDoc}
     */
    public List<FormValidator> getPageValidators(final String pageId) throws InvalidFormDefinitionException {
        
        List<FormValidator> pageValidators = new ArrayList<FormValidator>();

        if (isEditMode) {
            final String xpath = getPageXpath(pageId, true);
    
            final Node pageNode = getNodeByXpath(document, xpath);
            if (pageNode == null) {
                final String errorMessage = "Failed to parse the forms definition file. Page " + pageId + " not found.";
                if (LOGGER.isLoggable(Level.SEVERE)) {
                    LOGGER.log(Level.SEVERE, errorMessage);
                }
                throw new InvalidFormDefinitionException(errorMessage);
            } else {
                pageValidators = WidgetBuilderFactory.getXMLWidgetBuilder(processDefinitionUUID, locale, processDeploymentDate).getPageValidators(pageNode);
            }
        }
        return pageValidators;
    }

    /**
     * {@inheritDoc}
     */
    public List<FormWidget> getPageWidgets(final String pageId) throws InvalidFormDefinitionException {

        List<FormWidget> widgets = new ArrayList<FormWidget>();

        final Node pageNode = getPageNode(pageId);
        if (pageNode == null) {
            final String errorMessage = "Failed to parse the forms definition file. Page " + pageId + " not found.";
            if (LOGGER.isLoggable(Level.SEVERE)) {
                LOGGER.log(Level.SEVERE, errorMessage);
            }
            throw new InvalidFormDefinitionException(errorMessage);
        } else {
            widgets = WidgetBuilderFactory.getXMLWidgetBuilder(processDefinitionUUID, locale, processDeploymentDate).getPageWidgets(pageNode, isEditMode);
        }
        return widgets;
    }
    

    /**
     * {@inheritDoc}
     */
    public List<FormAction> getActions(final String pageId) throws InvalidFormDefinitionException {

        List<FormAction> formActions = new ArrayList<FormAction>();

        if (isEditMode) {
            final Node processNode = getNodeByXpath(document, processXpath);
            if (processNode == null) {
                if (LOGGER.isLoggable(Level.SEVERE)) {
                    LOGGER.log(Level.SEVERE, "Failed to parse the forms definition file. query : " + processXpath);
                }
            } else {
                formActions = WidgetBuilderFactory.getXMLWidgetBuilder(processDefinitionUUID, locale, processDeploymentDate).getActions(processNode, pageId);
            }
        }
        return formActions;
    }

    /**
     * {@inheritDoc}
     */
    public List<String> getPages() {
        final List<String> pages = new ArrayList<String>();

        final Node processNode = getNodeByXpath(document, processXpath);
        if (processNode == null) {
            if (LOGGER.isLoggable(Level.SEVERE)) {
                LOGGER.log(Level.SEVERE, "Failed to parse the forms definition file. query : " + processXpath);
            }
        } else {
            NodeList pageNodes = null;
            if (isInstanceRecap) {
                pageNodes = getNodeListByXpath(processNode, XMLForms.RECAP_PAGEFLOW + "/" + XMLForms.RECAP_PAGES + "/" + XMLForms.RECAP_PAGE);
            } else {
                if (isEditMode) {
                    pageNodes = getNodeListByXpath(processNode, XMLForms.PAGEFLOW + "/" + XMLForms.PAGES + "/" + XMLForms.PAGE);
                } else {
                    pageNodes = getNodeListByXpath(processNode, XMLForms.VIEW_PAGEFLOW + "/" + XMLForms.VIEW_PAGES + "/" + XMLForms.VIEW_PAGE);
                }
            }
            if (pageNodes != null) {
                for (int i=0; i<pageNodes.getLength(); i++) {
                    final String id = getStringByXpath(pageNodes.item(i), "@" + XMLForms.ID);
                    pages.add(id);
                }
            }
        }
        return pages;
    }

    /**
     * {@inheritDoc}
     */
    public String getConfirmationTemplate() {

        String path = null;

        final Node pageFlowNode = getPageFlowNode();
        
        final Node confirmationTemplateNode = getNodeByXpath(pageFlowNode, XMLForms.CONFIRMATION_TEMPLATE);
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
    public String getConfirmationMessage() {
        
        String message = null;
        
        final Node pageFlowNode = getPageFlowNode();
        
        final Node confirmationMessageNode = getNodeByXpath(pageFlowNode, XMLForms.CONFIRMATION_MESSAGE);
        if (confirmationMessageNode == null) {
            if (LOGGER.isLoggable(Level.INFO)) {
                LOGGER.log(Level.INFO, "No confirmation message was found. The default confirmation message will be used.");
            }
        } else {
            message = confirmationMessageNode.getTextContent();
        }
        return message;
    }
    
    /**
     * Retrieve the page node for a given page id
     * @param pageId
     * @return the {@link Node} for the page
     */
    protected Node getPageNode(final String pageId) {
        final String xpath = getPageXpath(pageId, isEditMode);
        return getNodeByXpath(document, xpath);
    }
    
    /**
     * Build the xpath query to get a process page
     * @param pageId
     * @param isEditMode
     * @return an xpath query under the form of a String
     */
    protected String getPageXpath(final String pageId, final boolean isEditMode) {
        final StringBuilder pageXpathBuilder = new StringBuilder();
        pageXpathBuilder.append(processXpath);
        pageXpathBuilder.append("/");
        if (isInstanceRecap) {
            pageXpathBuilder.append(XMLForms.RECAP_PAGEFLOW);
            pageXpathBuilder.append("/");
            pageXpathBuilder.append(XMLForms.RECAP_PAGES);
            pageXpathBuilder.append("/");
            pageXpathBuilder.append(XMLForms.RECAP_PAGE);
        } else {
            if (isEditMode) {
                pageXpathBuilder.append(XMLForms.PAGEFLOW);
                pageXpathBuilder.append("/");
                pageXpathBuilder.append(XMLForms.PAGES);
                pageXpathBuilder.append("/");
                pageXpathBuilder.append(XMLForms.PAGE);
            } else {
                pageXpathBuilder.append(XMLForms.VIEW_PAGEFLOW);
                pageXpathBuilder.append("/");
                pageXpathBuilder.append(XMLForms.VIEW_PAGES);
                pageXpathBuilder.append("/");
                pageXpathBuilder.append(XMLForms.VIEW_PAGE);
            }
        }
        pageXpathBuilder.append("[@");
        pageXpathBuilder.append(XMLForms.ID);
        pageXpathBuilder.append("='");
        pageXpathBuilder.append(pageId);
        pageXpathBuilder.append("']");
        return pageXpathBuilder.toString();
    }
    
    /**
     * Retrieve the page flow node
     * @return the {@link Node} for the page flow
     */
    protected Node getPageFlowNode() {
        final StringBuilder pageFlowXpathBuilder = new StringBuilder();
        pageFlowXpathBuilder.append(processXpath);
        pageFlowXpathBuilder.append("/");
        if (isInstanceRecap) {
            pageFlowXpathBuilder.append(XMLForms.RECAP_PAGEFLOW);
        } else {
            if (isEditMode) {
                pageFlowXpathBuilder.append(XMLForms.PAGEFLOW);
            } else {
                pageFlowXpathBuilder.append(XMLForms.VIEW_PAGEFLOW);
            }
        }
        final String pageFlowXpath = pageFlowXpathBuilder.toString();
        final Node pageFlowNode = getNodeByXpath(document, pageFlowXpath);
        if (pageFlowNode == null) {
            if (LOGGER.isLoggable(Level.SEVERE)) {
                LOGGER.log(Level.SEVERE, "Failed to parse the forms definition file. Page flow " + pageFlowXpath + " not found.");
            }
        }
        return pageFlowNode;
    }

    /**
     * {@inheritDoc}
     */
    public List<TransientData> getTransientData() throws InvalidFormDefinitionException {
        final List<TransientData> transientData = new ArrayList<TransientData>();
        
        final Node pageFlowNode = getPageFlowNode();
        final NodeList dataNodes = getNodeListByXpath(pageFlowNode, XMLForms.TRANSIENT_DATA + "/" + XMLForms.DATA);
        if (dataNodes != null) {
            for (int i=0; i<dataNodes.getLength(); i++) {
                final Node dataNode = dataNodes.item(i);
                final String name = getStringByXpath(dataNode, "@" + XMLForms.NAME);
                if (name == null || name.trim().length() == 0) {
                    final String errorMessage = "Failed to parse the forms definition file for the process " + processXpath + ". name for transient data missing.";
                    if (LOGGER.isLoggable(Level.SEVERE)) {
                        LOGGER.log(Level.SEVERE, errorMessage);
                    }
                    throw new InvalidFormDefinitionException(errorMessage);
                }
                final Node classNameNode = getNodeByXpath(dataNode, XMLForms.CLASSNAME);
                String className = null;
                if (classNameNode != null) {
                    className = classNameNode.getTextContent();
                }
                if (className == null || className.trim().length() == 0) {
                    final String errorMessage = "Failed to parse the forms definition file for the process " + processXpath + ". classname for transient data " + name + " not found.";
                    if (LOGGER.isLoggable(Level.SEVERE)) {
                        LOGGER.log(Level.SEVERE, errorMessage);
                    }
                    throw new InvalidFormDefinitionException(errorMessage);
                }
                final Node valueNode = getNodeByXpath(dataNode, XMLForms.VALUE);
                String value = null;
                if (valueNode != null) {
                    value = valueNode.getTextContent();
                }
                transientData.add(new TransientData(name, className, value));
            }
        }
        return transientData;
    }
    
    /**
     * {@inheritDoc}
     */
    public String getFirstPageExpression() throws InvalidFormDefinitionException {

        String firstPageExpression = null;
        final Node pageFlowNode = getPageFlowNode();
        if (pageFlowNode != null) {
        	final Node firstPageNode = getNodeByXpath(pageFlowNode, "@" + XMLForms.FIRST_PAGE);
        	if (firstPageNode != null) {
        		firstPageExpression = firstPageNode.getTextContent();
        	}
        }
        return firstPageExpression;
    }

    /**
     * {@inheritDoc}
     */
    public String getNextPageExpression(final String pageId) throws InvalidFormDefinitionException {
        
        String nextPageExpression = null;
        final Node pageNode = getPageNode(pageId);
        if (pageNode == null) {
            final String errorMessage = "Failed to parse the forms definition file. Page " + pageId + " not found.";
            if (LOGGER.isLoggable(Level.SEVERE)) {
                LOGGER.log(Level.SEVERE, errorMessage);
            }
            throw new InvalidFormDefinitionException(errorMessage);
        } else {
        	final Node nextPageNode = getNodeByXpath(pageNode, XMLForms.NEXT_PAGE);
        	if (nextPageNode != null) {
        		nextPageExpression = nextPageNode.getTextContent();
        	}
        }
        return nextPageExpression;
    }
}
