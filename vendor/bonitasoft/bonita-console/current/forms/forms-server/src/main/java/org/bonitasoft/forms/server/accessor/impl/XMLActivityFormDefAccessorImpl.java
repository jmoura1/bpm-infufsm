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
import org.bonitasoft.forms.server.exception.ActivityFormDefinitionNotFoundException;
import org.bonitasoft.forms.server.exception.InvalidFormDefinitionException;
import org.ow2.bonita.facade.uuid.ProcessDefinitionUUID;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Implementation of {@link IFormDefAccessor} allowing to parse the xml form definition file to generate an activity form model
 * 
 * @deprecated
 * @author Anthony Birembaut
 */
public class XMLActivityFormDefAccessorImpl extends XPathUtil implements IFormDefAccessor {

    /**
     * Logger
     */
    private static Logger LOGGER = Logger.getLogger(XMLActivityFormDefAccessorImpl.class.getName());
    
    /**
     * DOM representation of the XML file
     */
    protected Document document;
    
    /**
     * activity Name
     */
    protected String activityName;
    
    /**
     * indicates whether the page has to be displayed in edit mode (for a ready
     * task) or not (view mode)
     */
    protected boolean isEditMode;
    
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
     * The xpath query to get the activity node
     */
    protected String activityXpath;

    /**
     * Constructor
     * 
     * @param document
     * @param processDefinitionUUID
     * @param processDeployementDate 
     * @param activityName the activity name
     * @param isEditMode true if the edit page is required, false if it's the view page
     * @throws ActivityFormDefinitionNotFoundException if no form definition is found in the file for the activity
     */
    public XMLActivityFormDefAccessorImpl(final Document document, final ProcessDefinitionUUID processDefinitionUUID, final String locale, final Date processDeploymentDate, final String activityName, final boolean isEditMode) throws ActivityFormDefinitionNotFoundException {

        this.document = document;
        this.processDefinitionUUID = processDefinitionUUID;
        this.processDeploymentDate = processDeploymentDate;
        this.isEditMode = isEditMode;
        this.locale = locale;
        
        //elaborates the activity xpath query
        final StringBuilder activityXpathBuilder = new StringBuilder();
        activityXpathBuilder.append("//");
        activityXpathBuilder.append(XMLForms.ACTIVITY);
        activityXpathBuilder.append("[@");
        activityXpathBuilder.append(XMLForms.NAME);
        activityXpathBuilder.append("='");
        activityXpathBuilder.append(activityName);
        activityXpathBuilder.append("']");
        activityXpath = activityXpathBuilder.toString();

        Node activityNode = null;
        if (isEditMode) {
            activityNode = getNodeByXpath(document, activityXpath + "/" + XMLForms.PAGEFLOW);
        } else {
            activityNode = getNodeByXpath(document, activityXpath + "/" + XMLForms.VIEW_PAGEFLOW);
        }
        if (activityNode == null) {
            final String message = "The pageflow node for the activity " + activityName + " was not found in the forms definition file";
            if (LOGGER.isLoggable(Level.INFO)) {
                LOGGER.log(Level.INFO, message);
            }
            throw new ActivityFormDefinitionNotFoundException(message);
        } else {
            this.activityName = activityName;
        }
    }
    
    /**
     * {@inheritDoc}
     */
    public List<String> getPages() {

        final List<String> pages = new ArrayList<String>();

        final Node activityNode = getNodeByXpath(document, activityXpath);
        if (activityNode == null) {
            if (LOGGER.isLoggable(Level.SEVERE)) {
                LOGGER.log(Level.SEVERE, "Failed to parse the forms definition file. Activity " + activityXpath + " not found.");
            }
        } else {
            NodeList pageNodes = null;
            if (isEditMode) {
                pageNodes = getNodeListByXpath(activityNode, XMLForms.PAGEFLOW + "/" + XMLForms.PAGES + "/" + XMLForms.PAGE);
            } else {
                pageNodes = getNodeListByXpath(activityNode, XMLForms.VIEW_PAGEFLOW + "/" + XMLForms.VIEW_PAGES + "/" + XMLForms.VIEW_PAGE);
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
    public String getFormPageTemplate(final String pageId) throws InvalidFormDefinitionException {

        String path = null;

        final Node pageNode = getPageNode(pageId);
        if (pageNode == null) {
            final String errorMessage = "Failed to parse the forms definition file. Page " + pageId + " not found.";
            if (LOGGER.isLoggable(Level.SEVERE)) {
                LOGGER.log(Level.SEVERE, errorMessage);
            }
            throw new InvalidFormDefinitionException(errorMessage);
        } else {
            final Node pageTemplateNode = getNodeByXpath(pageNode, XMLForms.PAGE_TEMPLATE);
            if (pageTemplateNode == null) {
                if (LOGGER.isLoggable(Level.WARNING)) {
                    LOGGER.log(Level.WARNING, "Failed to parse the form definition file. The default page template will be used.");
                }
                path = null;
            } else {
                path = pageTemplateNode.getTextContent();
            }
        }
        return path;
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
     * {@inheritDoc}
     */
    public String getPageLabel(final String pageId) throws InvalidFormDefinitionException {

        String label = null;

        final Node pageNode = getPageNode(pageId);
        if (pageNode == null) {
            final String errorMessage = "Failed to parse the forms definition file. Page " + pageId + " not found.";
            if (LOGGER.isLoggable(Level.SEVERE)) {
                LOGGER.log(Level.SEVERE, errorMessage);
            }
            throw new InvalidFormDefinitionException(errorMessage);
        } else {
            final String xpath = XMLForms.PAGE_LABEL;
            final Node pageLabelNode = getNodeByXpath(pageNode, xpath);
            if (pageLabelNode == null) {
                if (LOGGER.isLoggable(Level.SEVERE)) {
                    LOGGER.log(Level.SEVERE, "Failed to parse the form definition file. query : " + xpath);
                }
                throw new InvalidFormDefinitionException("The label for page " + pageId + " of activity " + activityName + " was not found in the forms definition file");
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

        final Node pageNode = getPageNode(pageId);
        if (pageNode == null) {
            final String errorMessage = "Failed to parse the forms definition file. Page " + pageId + " not found.";
            if (LOGGER.isLoggable(Level.SEVERE)) {
                LOGGER.log(Level.SEVERE, errorMessage);
            }
            throw new InvalidFormDefinitionException(errorMessage);
        } else {
            final String xpath = XMLForms.ALLOW_HTML_IN_LABEL;
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

        List<FormAction> actions = new ArrayList<FormAction>();

        if (isEditMode) {
            final Node activityNode = getNodeByXpath(document, activityXpath);
            if (activityNode == null) {
                if (LOGGER.isLoggable(Level.SEVERE)) {
                    LOGGER.log(Level.SEVERE, "Failed to parse the forms definition file. Activity " + activityXpath + " not found.");
                }
            } else {
                actions = WidgetBuilderFactory.getXMLWidgetBuilder(processDefinitionUUID, locale, processDeploymentDate).getActions(activityNode, pageId);
            }
        }
        return actions;
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
     * Retrieve the page node for a given page id
     * @param pageId
     * @return the {@link Node} for the page
     */
    protected Node getPageNode(final String pageId) {
        final String xpath = getPageXpath(pageId, isEditMode);
        return getNodeByXpath(document, xpath);
    }
    
    /**
     * Build the xpath query to get an activity page
     * @param pageId
     * @param isEditMode
     * @return an xpath query under the form of a String
     */
    protected String getPageXpath(final String pageId, final boolean isEditMode) {
        final StringBuilder pageXpathBuilder = new StringBuilder();
        pageXpathBuilder.append(activityXpath);
        pageXpathBuilder.append("/");
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
        pageFlowXpathBuilder.append(activityXpath);
        pageFlowXpathBuilder.append("/");
        if (isEditMode) {
            pageFlowXpathBuilder.append(XMLForms.PAGEFLOW);
        } else {
            pageFlowXpathBuilder.append(XMLForms.VIEW_PAGEFLOW);
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
                    final String errorMessage = "Failed to parse the forms definition file for activity " + activityXpath + ". Name for transient data missing.";
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
                    final String errorMessage = "Failed to parse the forms definition file for activity " + activityXpath + ". classname for transient data " + name + " not found.";
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
