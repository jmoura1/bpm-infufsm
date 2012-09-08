/**
 * Copyright (C) 2010  BonitaSoft S.A.
 * BonitaSoft, 31 rue Gustave Eiffel - 38000 Grenoble
 * This library is free software; you can redistribute it and/or modify it under the terms
 * of the GNU Lesser General Public License as published by the Free Software Foundation
 * version 2.1 of the License.
 * This library is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License along with this
 * program; if not, write to the Free Software Foundation, Inc., 51 Franklin Street, Fifth
 * Floor, Boston, MA  02110-1301, USA.
 **/
package org.ow2.bonita.building;

import java.io.IOException;
import java.io.Serializable;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerFactory;

import org.ow2.bonita.facade.def.element.AttachmentDefinition;
import org.ow2.bonita.facade.def.element.BoundaryEvent;
import org.ow2.bonita.facade.def.element.DeadlineDefinition;
import org.ow2.bonita.facade.def.element.FilterDefinition;
import org.ow2.bonita.facade.def.element.HookDefinition;
import org.ow2.bonita.facade.def.element.IncomingEventDefinition;
import org.ow2.bonita.facade.def.element.MultiInstantiationDefinition;
import org.ow2.bonita.facade.def.element.OutgoingEventDefinition;
import org.ow2.bonita.facade.def.element.RoleMapperDefinition;
import org.ow2.bonita.facade.def.element.SubflowParameterDefinition;
import org.ow2.bonita.facade.def.element.impl.ErrorBoundaryEventImpl;
import org.ow2.bonita.facade.def.element.impl.MessageBoundaryEventImpl;
import org.ow2.bonita.facade.def.element.impl.SignalBoundaryEventImpl;
import org.ow2.bonita.facade.def.element.impl.TimerBoundaryEventImpl;
import org.ow2.bonita.facade.def.majorElement.ActivityDefinition;
import org.ow2.bonita.facade.def.majorElement.DataFieldDefinition;
import org.ow2.bonita.facade.def.majorElement.EventProcessDefinition;
import org.ow2.bonita.facade.def.majorElement.ParticipantDefinition;
import org.ow2.bonita.facade.def.majorElement.ProcessDefinition;
import org.ow2.bonita.facade.def.majorElement.TransitionDefinition;
import org.ow2.bonita.util.BonitaRuntimeException;
import org.ow2.bonita.util.Misc;
import org.ow2.bonita.util.XmlConstants;
import org.w3c.dom.DOMException;
import org.w3c.dom.Node;

/**
 * @author Anthony Birembaut, Matthieu Chaffotte, Elias Ricken de Medeiros
 *
 */
public class XmlDefExporter {

  /**
   * the product version
   */
  private static final String PRODUCT_VERSION = "5.6";

  /**
   * Logger
   */
  private static Logger LOGGER = Logger.getLogger(XmlDefExporter.class.getName());

  /**
   * Document builder factory
   */
  private DocumentBuilderFactory documentBuilderFactory;

  /**
   * Transformer factory
   */
  private TransformerFactory transformerFactory;

  /**
   * Instance attribute
   */
  private static XmlDefExporter INSTANCE = null;

  /**
   * @return the XmlDefExporter instance
   */
  public static XmlDefExporter getInstance() {
    if (INSTANCE == null) {
      INSTANCE = new XmlDefExporter();
    }
    return INSTANCE;
  }

  /**
   * Private constructor to prevent instantiation
   */
  private XmlDefExporter() {
    documentBuilderFactory = DocumentBuilderFactory.newInstance();
    documentBuilderFactory.setNamespaceAware(true);
    documentBuilderFactory.setValidating(true);

    // ignore white space can only be set if parser is validating
    this.documentBuilderFactory.setIgnoringElementContentWhitespace(true);
    // select xml schema as the schema language (a.o.t. DTD)
    this.documentBuilderFactory.setAttribute("http://java.sun.com/xml/jaxp/properties/schemaLanguage", "http://www.w3.org/2001/XMLSchema");

    final URL xsdURL = getClass().getResource("/" + XmlConstants.XML_PROCESS_DEF_STRICT_SCHEMA);
    this.documentBuilderFactory.setAttribute("http://java.sun.com/xml/jaxp/properties/schemaSource", xsdURL.toExternalForm());

    transformerFactory= TransformerFactory.newInstance();
  }

  /**
   * Create the document and root node
   * @return the XML process definition
   * @throws Exception 
   * @throws ParserConfigurationException 
   */
  public byte[] createProcessDefinition(ProcessDefinition processDefinition) {
    XmlBuilder xmlDefBuilder = new XmlBuilder(documentBuilderFactory, transformerFactory);
    try {
      xmlDefBuilder.createDocument();
      Map<String, Serializable> rootElementAttributes = new HashMap<String, Serializable>();
      rootElementAttributes.put(XmlDef.PRODUCT_VERSION, PRODUCT_VERSION);
      Node rootNode = xmlDefBuilder.createRootNode(XmlDef.PROCESS_DEFINITION, rootElementAttributes);

      createProcess(xmlDefBuilder, rootNode, processDefinition);

      return xmlDefBuilder.done();
    } catch (Exception e) {
      String errorMessage = "Unable to build the process definition XML";
      if (LOGGER.isLoggable(Level.SEVERE)) {
        LOGGER.log(Level.SEVERE, errorMessage, e);
      }
      throw new BonitaRuntimeException(errorMessage, e);
    }
  }

  private Node createProcess(XmlBuilder xmlDefBuilder, Node parentNode, ProcessDefinition processDefinition) throws Exception {

    Map<String, Serializable> processElementAttributes = new HashMap<String, Serializable>();
    processElementAttributes.put(XmlDef.NAME, processDefinition.getName());
    processElementAttributes.put(XmlDef.VERSION, processDefinition.getVersion());
    Node processNode = xmlDefBuilder.createNode(parentNode, XmlDef.PROCESS, processElementAttributes);

    xmlDefBuilder.createNode(processNode, XmlDef.LABEL, processDefinition.getLabel());
    xmlDefBuilder.createNode(processNode, XmlDef.DESCRIPTION, processDefinition.getDescription());
    xmlDefBuilder.createNode(processNode, XmlDef.TYPE, processDefinition.getType());

    createParticipants(xmlDefBuilder, processNode, processDefinition.getParticipants());
    createDataFields(xmlDefBuilder, processNode, processDefinition.getDataFields());
    createAttachments(xmlDefBuilder, processNode, processDefinition.getAttachments());
    createActivities(xmlDefBuilder, processNode, processDefinition.getActivities());
    
    Set<TransitionDefinition> transitions = new HashSet<TransitionDefinition>();
    for (ActivityDefinition activity : processDefinition.getActivities()) {
      List<BoundaryEvent> events = activity.getBoundaryEvents();
      for (BoundaryEvent boundaryEvent : events) {
        transitions.add(boundaryEvent.getTransition());
      }
    }
    transitions.addAll(processDefinition.getTransitions());
    createTransitions(xmlDefBuilder, processNode, transitions);
    createConnectors(xmlDefBuilder, processNode, processDefinition.getConnectors());
    createCategories(xmlDefBuilder, processNode, processDefinition.getCategoryNames());
    createEventSubProcess(xmlDefBuilder, processNode, processDefinition.getEventSubProcesses());

    return processNode;
  }

  private void createActivities(XmlBuilder xmlDefBuilder, Node parentNode, Set<ActivityDefinition> activities) throws Exception {
    Node activitiesNode = xmlDefBuilder.createNode(parentNode, XmlDef.ACTIVITIES);
    for (ActivityDefinition activityDefinition : activities) {
      Map<String, Serializable> activityElementAttributes = new HashMap<String, Serializable>();
      activityElementAttributes.put(XmlDef.NAME, activityDefinition.getName());
      Node activityNode = xmlDefBuilder.createNode(activitiesNode, XmlDef.ACTIVITY, activityElementAttributes);

      xmlDefBuilder.createNode(activityNode, XmlDef.LABEL, activityDefinition.getLabel());
      xmlDefBuilder.createNode(activityNode, XmlDef.DESCRIPTION, activityDefinition.getDescription());
      xmlDefBuilder.createNode(activityNode, XmlDef.DYNAMIC_LABEL, activityDefinition.getDynamicLabel());
      xmlDefBuilder.createNode(activityNode, XmlDef.DYNAMIC_DESCRIPTION, activityDefinition.getDynamicDescription());
      xmlDefBuilder.createNode(activityNode, XmlDef.DYNAMIC_EXECUTION_SUMMARY, activityDefinition.getDynamicExecutionSummary());
      xmlDefBuilder.createNode(activityNode, XmlDef.PRIORITY, activityDefinition.getPriority());
      xmlDefBuilder.createNode(activityNode, XmlDef.EXECUTING_TIME, activityDefinition.getExecutingTime());
      xmlDefBuilder.createNode(activityNode, XmlDef.SPLIT_TYPE, activityDefinition.getSplitType());
      xmlDefBuilder.createNode(activityNode, XmlDef.JOIN_TYPE, activityDefinition.getJoinType());
      xmlDefBuilder.createNode(activityNode, XmlDef.SUBFLOW_PROCESS_NAME, activityDefinition.getSubflowProcessName());
      xmlDefBuilder.createNode(activityNode, XmlDef.SUBFLOW_PROCESS_VERSION, activityDefinition.getSubflowProcessVersion());
      xmlDefBuilder.createNode(activityNode, XmlDef.TIMER_CONDITION, activityDefinition.getTimerCondition());
      xmlDefBuilder.createNode(activityNode, XmlDef.TYPE, activityDefinition.getType());
      xmlDefBuilder.createNode(activityNode, XmlDef.ASYNCHRONOUS, activityDefinition.isAsynchronous());
      if (activityDefinition.isThrowingSignalEvent() || activityDefinition.isCatchingSignalEvent()) {
        xmlDefBuilder.createNode(activityNode, XmlDef.CATCH_EVENT, activityDefinition.catchEvent());
      }
      xmlDefBuilder.createNode(activityNode, XmlDef.TERMINATE_PROCESS, activityDefinition.isTerminateProcess());
      Node performersNode = xmlDefBuilder.createNode(activityNode, XmlDef.PERFORMERS);
      for (String performer : activityDefinition.getPerformers()) {
        xmlDefBuilder.createNode(performersNode, XmlDef.PERFORMER, performer);
      }

      if (activityDefinition.isInALoop()) {
        xmlDefBuilder.createNode(activityNode, XmlDef.LOOP_CONDITION, activityDefinition.getLoopCondition());
        xmlDefBuilder.createNode(activityNode, XmlDef.BEFORE_EXECUTION, activityDefinition.evaluateLoopConditionBeforeExecution());
        xmlDefBuilder.createNode(activityNode, XmlDef.LOOP_MAXIMUM, activityDefinition.getLoopMaximum());
      }

      createConnectors(xmlDefBuilder, activityNode, activityDefinition.getConnectors());
      createDataFields(xmlDefBuilder, activityNode, activityDefinition.getDataFields());
      createFilter(xmlDefBuilder, activityNode, activityDefinition.getFilter());
      createDeadlines(xmlDefBuilder, activityNode, activityDefinition.getDeadlines());
      createIncomingEvent(xmlDefBuilder, activityNode, activityDefinition.getIncomingEvent());
      createOutgoingEvents(xmlDefBuilder, activityNode, activityDefinition.getOutgoingEvents());
      createMultiInstantiationDefinition(xmlDefBuilder, activityNode, activityDefinition.getMultiInstantiationDefinition());
      createMultipleActivitiesInstantiator(xmlDefBuilder, activityNode, activityDefinition.getMultipleInstancesInstantiator());
      createMultipleActivitiesJoinChecker(xmlDefBuilder, activityNode, activityDefinition.getMultipleInstancesJoinChecker());

      createEvents(xmlDefBuilder, activityNode, activityDefinition.getBoundaryEvents());
      createSubflowParameters(xmlDefBuilder, activityNode, activityDefinition.getSubflowInParameters(), XmlDef.SUBFLOW_IN_PARAMETERS, XmlDef.SUBFLOW_IN_PARAMETER);
      createSubflowParameters(xmlDefBuilder, activityNode, activityDefinition.getSubflowOutParameters(), XmlDef.SUBFLOW_OUT_PARAMETERS, XmlDef.SUBFLOW_OUT_PARAMETER);
    }
  }

  private void createEvents(XmlBuilder xmlDefBuilder, Node parentNode, List<BoundaryEvent> events) throws DOMException, IOException, ClassNotFoundException {
    Node eventsNode = xmlDefBuilder.createNode(parentNode, XmlDef.BOUNDARY_EVENTS);
    if (!events.isEmpty()) {
      List<TimerBoundaryEventImpl> timers = getTimerBoundaryEvents(events);
      Node timersNode = xmlDefBuilder.createNode(eventsNode, XmlDef.TIMER_EVENTS);
      for (TimerBoundaryEventImpl timer : timers) {
        Map<String, Serializable> eventElementAttributes = new HashMap<String, Serializable>();
        eventElementAttributes.put(XmlDef.NAME, timer.getName());
        Node timerNode = xmlDefBuilder.createNode(timersNode, XmlDef.TIMER_EVENT, eventElementAttributes);
        xmlDefBuilder.createNode(timerNode, XmlDef.DESCRIPTION, timer.getDescription());
        xmlDefBuilder.createNode(timerNode, XmlDef.LABEL, timer.getLabel());
        xmlDefBuilder.createNode(timerNode, XmlDef.CONDITION, timer.getCondition());
      }

      List<MessageBoundaryEventImpl> messages = getMessageBoundaryEvents(events);
      Node messagesNode = xmlDefBuilder.createNode(eventsNode, XmlDef.MESSAGE_EVENTS);
      for (MessageBoundaryEventImpl message : messages) {
        Map<String, Serializable> eventElementAttributes = new HashMap<String, Serializable>();
        eventElementAttributes.put(XmlDef.NAME, message.getName());
        Node messageNode = xmlDefBuilder.createNode(messagesNode, XmlDef.MESSAGE_EVENT, eventElementAttributes);
        xmlDefBuilder.createNode(messageNode, XmlDef.DESCRIPTION, message.getDescription());
        xmlDefBuilder.createNode(messageNode, XmlDef.LABEL, message.getLabel());
        xmlDefBuilder.createNode(messageNode, XmlDef.EXPRESSION, message.getExpression());
      }
      
      List<ErrorBoundaryEventImpl> errors = getErrorBoundaryEvents(events);
      Node errorsNode = xmlDefBuilder.createNode(eventsNode, XmlDef.ERROR_EVENTS);
      for (ErrorBoundaryEventImpl error : errors) {
        Map<String, Serializable> eventElementAttributes = new HashMap<String, Serializable>();
        eventElementAttributes.put(XmlDef.NAME, error.getName());
        Node errorNode = xmlDefBuilder.createNode(errorsNode, XmlDef.ERROR_EVENT, eventElementAttributes);
        xmlDefBuilder.createNode(errorNode, XmlDef.DESCRIPTION, error.getDescription());
        xmlDefBuilder.createNode(errorNode, XmlDef.LABEL, error.getLabel());
        xmlDefBuilder.createNode(errorNode, XmlDef.ERROR_CODE, error.getErrorCode());
      }

      List<SignalBoundaryEventImpl> signals = getSignalBoundaryEvents(events);
      Node signalsNode = xmlDefBuilder.createNode(eventsNode, XmlDef.SIGNAL_EVENTS);
      for (SignalBoundaryEventImpl signal : signals) {
        Map<String, Serializable> eventElementAttributes = new HashMap<String, Serializable>();
        eventElementAttributes.put(XmlDef.NAME, signal.getName());
        Node signalNode = xmlDefBuilder.createNode(signalsNode, XmlDef.SIGNAL_EVENT, eventElementAttributes);
        xmlDefBuilder.createNode(signalNode, XmlDef.DESCRIPTION, signal.getDescription());
        xmlDefBuilder.createNode(signalNode, XmlDef.LABEL, signal.getLabel());
        xmlDefBuilder.createNode(signalNode, XmlDef.SIGNAL_CODE, signal.getSignalCode());
      }
    }
  }

  private List<TimerBoundaryEventImpl> getTimerBoundaryEvents(List<BoundaryEvent> events) {
    List<TimerBoundaryEventImpl> timers = new ArrayList<TimerBoundaryEventImpl>();
    for (BoundaryEvent event : events) {
      if (event instanceof TimerBoundaryEventImpl) {
        timers.add((TimerBoundaryEventImpl) event);
      }
    }
    return timers;
  }
  
  private List<MessageBoundaryEventImpl> getMessageBoundaryEvents(List<BoundaryEvent> events) {
    List<MessageBoundaryEventImpl> messages = new ArrayList<MessageBoundaryEventImpl>();
    for (BoundaryEvent event : events) {
      if (event instanceof MessageBoundaryEventImpl) {
        messages.add((MessageBoundaryEventImpl) event);
      }
    }
    return messages;
  }

  private List<ErrorBoundaryEventImpl> getErrorBoundaryEvents(List<BoundaryEvent> events) {
    List<ErrorBoundaryEventImpl> errors = new ArrayList<ErrorBoundaryEventImpl>();
    for (BoundaryEvent event : events) {
      if (event instanceof ErrorBoundaryEventImpl) {
        errors.add((ErrorBoundaryEventImpl) event);
      }
    }
    return errors;
  }

  private List<SignalBoundaryEventImpl> getSignalBoundaryEvents(List<BoundaryEvent> events) {
    List<SignalBoundaryEventImpl> signals = new ArrayList<SignalBoundaryEventImpl>();
    for (BoundaryEvent event : events) {
      if (event instanceof SignalBoundaryEventImpl) {
        signals.add((SignalBoundaryEventImpl) event);
      }
    }
    return signals;
  }

  private void createCategories(XmlBuilder xmlDefBuilder, Node parentNode, Set<String> categories) throws Exception {
    Node categoriesNode = xmlDefBuilder.createNode(parentNode, XmlDef.CATEGORIES);
    for (String category : categories) {
      Map<String, Serializable> categoryElementAttributes = new HashMap<String, Serializable>();
      categoryElementAttributes.put(XmlDef.NAME, category);
      xmlDefBuilder.createNode(categoriesNode, XmlDef.CATEGORY, categoryElementAttributes);
    }
  }
  
  private void createSubflowParameters(XmlBuilder xmlDefBuilder, Node parentNode,
      Set<SubflowParameterDefinition> subflowParameters, String xmlGroupName, String xmlElementName) throws Exception {

    Node subflowParametersNode = xmlDefBuilder.createNode(parentNode, xmlGroupName);
    for (SubflowParameterDefinition subflowParameter : subflowParameters) {
      Node subflowParameterNode = xmlDefBuilder.createNode(subflowParametersNode, xmlElementName);
      xmlDefBuilder.createNode(subflowParameterNode, XmlDef.SOURCE, subflowParameter.getSource());
      xmlDefBuilder.createNode(subflowParameterNode, XmlDef.DESTINATION, subflowParameter.getDestination());
    }
  }

  private void createMultiInstantiationDefinition(XmlBuilder xmlDefBuilder, Node parentNode, MultiInstantiationDefinition multiInstantiationDefinition) throws Exception {

    if (multiInstantiationDefinition != null) {
      Node multiInstantiationNode = xmlDefBuilder.createNode(parentNode, XmlDef.MULTI_INSTANTIATION);

      if (multiInstantiationDefinition != null) {
        xmlDefBuilder.createNode(multiInstantiationNode, XmlDef.CLASSNAME, multiInstantiationDefinition.getClassName());
        xmlDefBuilder.createNode(multiInstantiationNode, XmlDef.DESCRIPTION, multiInstantiationDefinition.getDescription());
        xmlDefBuilder.createNode(multiInstantiationNode, XmlDef.VARIABLE_NAME, multiInstantiationDefinition.getVariableName());

        Node parametersNode = xmlDefBuilder.createNode(multiInstantiationNode, XmlDef.PARAMETERS);
        Map<String, Object[]> multiInstantiationParameters = multiInstantiationDefinition.getParameters();
        
        for (Entry<String, Object[]> multiInstantiationParameter : multiInstantiationParameters.entrySet()) {
          Map<String, Serializable> multiInstantiationParameterAttributes = new HashMap<String, Serializable>();
          multiInstantiationParameterAttributes.put(XmlDef.NAME, multiInstantiationParameter.getKey());
          byte[] value = Misc.serialize(multiInstantiationParameter.getValue());
          xmlDefBuilder.createNode(parametersNode, XmlDef.PARAMETER, value, multiInstantiationParameterAttributes);
        }
      }
    }
  }
  
  private void createOutgoingEvents(XmlBuilder xmlDefBuilder, Node parentNode, Set<OutgoingEventDefinition> outgoingEvents) throws Exception {

    Node outgoingEventsNode = xmlDefBuilder.createNode(parentNode, XmlDef.OUTGOING_EVENTS);
    for (OutgoingEventDefinition outgoingEvent : outgoingEvents) {
      Map<String, Serializable> outgoingEventElementAttributes = new HashMap<String, Serializable>();
      outgoingEventElementAttributes.put(XmlDef.NAME, outgoingEvent.getName());
      Node outgoingEventNode = xmlDefBuilder.createNode(outgoingEventsNode, XmlDef.OUTGOING_EVENT, outgoingEventElementAttributes);

      xmlDefBuilder.createNode(outgoingEventNode, XmlDef.TIME_TO_LIVE, outgoingEvent.getTimeToLive());
      xmlDefBuilder.createNode(outgoingEventNode, XmlDef.TO_ACTIVITY, outgoingEvent.getToActivityName());
      xmlDefBuilder.createNode(outgoingEventNode, XmlDef.TO_PROCESS, outgoingEvent.getToProcessName());

      Node parametersNode = xmlDefBuilder.createNode(outgoingEventNode, XmlDef.PARAMETERS);
      Map<String, Object> outgoingEventParameters = outgoingEvent.getParameters();

      for (Entry<String, Object> outgoingEventParameter : outgoingEventParameters.entrySet()) {
        Map<String, Serializable> outgoingEventParameterAttributes = new HashMap<String, Serializable>();
        outgoingEventParameterAttributes.put(XmlDef.NAME, outgoingEventParameter.getKey());
        byte[] value = Misc.serialize((Serializable) outgoingEventParameter.getValue());
        xmlDefBuilder.createNode(parametersNode, XmlDef.PARAMETER, value, outgoingEventParameterAttributes);
      }
    }
  }

  private void createIncomingEvent(XmlBuilder xmlDefBuilder, Node parentNode, IncomingEventDefinition incomingEvent) throws Exception {
    if (incomingEvent != null) {
      Map<String, Serializable> incomingEventElementAttributes = new HashMap<String, Serializable>();
      incomingEventElementAttributes.put(XmlDef.NAME, incomingEvent.getName());
      Node incomingEventNode = xmlDefBuilder.createNode(parentNode, XmlDef.INCOMING_EVENT, incomingEventElementAttributes);

      xmlDefBuilder.createNode(incomingEventNode, XmlDef.EXPRESSION, incomingEvent.getExpression());
    }
  }

  private void createEventSubProcess(XmlBuilder xmlDefBuilder, Node parentNode, List<EventProcessDefinition> eventSubProcesses) throws Exception {
    Node eventSubProcessesNode = xmlDefBuilder.createNode(parentNode, XmlDef.EVENT_SUB_PROCESSES);
    for (EventProcessDefinition eventSubProcess : eventSubProcesses) {
      Map<String, Serializable> eventSubProcessElementAttributes = new HashMap<String, Serializable>();
      eventSubProcessElementAttributes.put(XmlDef.NAME, eventSubProcess.getName());
      Node eventSubProcessNode = xmlDefBuilder.createNode(eventSubProcessesNode, XmlDef.EVENT_SUB_PROCESS, eventSubProcessElementAttributes);
      xmlDefBuilder.createNode(eventSubProcessNode, XmlDef.VERSION, eventSubProcess.getVersion());
      xmlDefBuilder.createNode(eventSubProcessNode, XmlDef.DESCRIPTION, eventSubProcess.getDescription());
      xmlDefBuilder.createNode(eventSubProcessNode, XmlDef.LABEL, eventSubProcess.getLabel());
    }
  }
  
  private void createDeadlines(XmlBuilder xmlDefBuilder, Node parentNode, Set<DeadlineDefinition> deadlines) throws Exception {
    Node deadlinesNode = xmlDefBuilder.createNode(parentNode, XmlDef.DEADLINES);
    for (DeadlineDefinition deadline : deadlines) {
      Node deadlineNode = xmlDefBuilder.createNode(deadlinesNode, XmlDef.DEADLINE);

      xmlDefBuilder.createNode(deadlineNode, XmlDef.CLASSNAME, deadline.getClassName());
      xmlDefBuilder.createNode(deadlineNode, XmlDef.CONDITION, deadline.getCondition());
      xmlDefBuilder.createNode(deadlineNode, XmlDef.DESCRIPTION, deadline.getDescription());

      Node parametersNode = xmlDefBuilder.createNode(deadlineNode, XmlDef.PARAMETERS);
      Map<String, Object[]> deadlineParameters = deadline.getParameters();
      for (Entry<String, Object[]> deadlineParameter : deadlineParameters.entrySet()) {
        Map<String, Serializable> deadlineParameterAttributes = new HashMap<String, Serializable>();
        deadlineParameterAttributes.put(XmlDef.NAME, deadlineParameter.getKey());
        byte[] value = Misc.serialize(deadlineParameter.getValue());
        xmlDefBuilder.createNode(parametersNode, XmlDef.PARAMETER, value, deadlineParameterAttributes);
      }
    }
  }

  private void createMultipleActivitiesInstantiator(XmlBuilder xmlDefBuilder, Node parentNode, MultiInstantiationDefinition instantiator) throws Exception {
    if (instantiator != null) {
      Node instantiatorNode = xmlDefBuilder.createNode(parentNode, XmlDef.MULTIPLE_ACT_INSTANTIATOR);
      xmlDefBuilder.createNode(instantiatorNode, XmlDef.CLASSNAME, instantiator.getClassName());
      xmlDefBuilder.createNode(instantiatorNode, XmlDef.DESCRIPTION, instantiator.getDescription());

      Node parametersNode = xmlDefBuilder.createNode(instantiatorNode, XmlDef.PARAMETERS);
      Map<String, Object[]> instantiatorParameters = instantiator.getParameters();
      for (Entry<String, Object[]> instantiatorParameter : instantiatorParameters.entrySet()) {
        Map<String, Serializable> instantiatorParameterAttributes = new HashMap<String, Serializable>();
        instantiatorParameterAttributes.put(XmlDef.NAME, instantiatorParameter.getKey());
        byte[] value = Misc.serialize(instantiatorParameter.getValue());
        xmlDefBuilder.createNode(parametersNode, XmlDef.PARAMETER, value, instantiatorParameterAttributes);
      }
    }
  }

  private void createMultipleActivitiesJoinChecker(XmlBuilder xmlDefBuilder, Node parentNode, MultiInstantiationDefinition joinChecker) throws Exception {
    if (joinChecker != null) {
      Node joinCheckerNode = xmlDefBuilder.createNode(parentNode, XmlDef.MULTIPLE_ACT_JOINCHECKER);
      xmlDefBuilder.createNode(joinCheckerNode, XmlDef.CLASSNAME, joinChecker.getClassName());
      xmlDefBuilder.createNode(joinCheckerNode, XmlDef.DESCRIPTION, joinChecker.getDescription());

      Node parametersNode = xmlDefBuilder.createNode(joinCheckerNode, XmlDef.PARAMETERS);
      Map<String, Object[]> joinCheckerParameters = joinChecker.getParameters();
      for (Entry<String, Object[]> joinCheckerParameter : joinCheckerParameters.entrySet()) {
        Map<String, Serializable> joinCheckerParameterAttributes = new HashMap<String, Serializable>();
        joinCheckerParameterAttributes.put(XmlDef.NAME, joinCheckerParameter.getKey());
        byte[] value =  Misc.serialize(joinCheckerParameter.getValue());
        xmlDefBuilder.createNode(parametersNode, XmlDef.PARAMETER, value, joinCheckerParameterAttributes);
      }
    }
  }

  private void createFilter(XmlBuilder xmlDefBuilder, Node parentNode, FilterDefinition filter) throws Exception {
    if (filter != null) {
      Node filterNode = xmlDefBuilder.createNode(parentNode, XmlDef.FILTER);
      xmlDefBuilder.createNode(filterNode, XmlDef.CLASSNAME, filter.getClassName());
      xmlDefBuilder.createNode(filterNode, XmlDef.DESCRIPTION, filter.getDescription());

      Node parametersNode = xmlDefBuilder.createNode(filterNode, XmlDef.PARAMETERS);
      Map<String, Object[]> filterParameters = filter.getParameters();
      for (Entry<String, Object[]> filterParameter : filterParameters.entrySet()) {
        Map<String, Serializable> filterParameterAttributes = new HashMap<String, Serializable>();
        filterParameterAttributes.put(XmlDef.NAME, filterParameter.getKey());
        byte[] value = Misc.serialize(filterParameter.getValue());
        xmlDefBuilder.createNode(parametersNode, XmlDef.PARAMETER, value, filterParameterAttributes);
      }
    }
  }

  private void createTransitions(XmlBuilder xmlDefBuilder, Node parentNode, Set<TransitionDefinition> transitions) throws Exception {

    Node transitionsNode = xmlDefBuilder.createNode(parentNode, XmlDef.TRANSITIONS);
    for (TransitionDefinition transitionDefinition : transitions) {
      Map<String, Serializable> transitionElementAttributes = new HashMap<String, Serializable>();
      transitionElementAttributes.put(XmlDef.NAME, transitionDefinition.getName());
      Node transitionNode = xmlDefBuilder.createNode(transitionsNode, XmlDef.TRANSITION, transitionElementAttributes);

      xmlDefBuilder.createNode(transitionNode, XmlDef.LABEL, transitionDefinition.getLabel());
      xmlDefBuilder.createNode(transitionNode, XmlDef.CONDITION, transitionDefinition.getCondition());
      xmlDefBuilder.createNode(transitionNode, XmlDef.DESCRIPTION, transitionDefinition.getDescription());
      xmlDefBuilder.createNode(transitionNode, XmlDef.FROM, transitionDefinition.getFrom());
      xmlDefBuilder.createNode(transitionNode, XmlDef.TO, transitionDefinition.getTo());
      xmlDefBuilder.createNode(transitionNode, XmlDef.ISDEFAULT, transitionDefinition.isDefault());
      xmlDefBuilder.createNode(transitionNode, XmlDef.BOUNDARY_EVENT, transitionDefinition.getFromBoundaryEvent());
    }
  }

  private void createDataFields(XmlBuilder xmlDefBuilder, Node parentNode, Set<DataFieldDefinition> dataFields) throws Exception {

    Node dataFieldsNode = xmlDefBuilder.createNode(parentNode, XmlDef.DATA_FIELDS);
    for (DataFieldDefinition dataFieldDefinition : dataFields) {
      Map<String, Serializable> dataFieldElementAttributes = new HashMap<String, Serializable>();
      dataFieldElementAttributes.put(XmlDef.NAME, dataFieldDefinition.getName());
      Node dataFieldNode = xmlDefBuilder.createNode(dataFieldsNode, XmlDef.DATA_FIELD, dataFieldElementAttributes);

      xmlDefBuilder.createNode(dataFieldNode, XmlDef.LABEL, dataFieldDefinition.getLabel());
      xmlDefBuilder.createNode(dataFieldNode, XmlDef.VALUE, Misc.serialize(dataFieldDefinition.getInitialValue()));
      xmlDefBuilder.createNode(dataFieldNode, XmlDef.DESCRIPTION, dataFieldDefinition.getDescription());
      xmlDefBuilder.createNode(dataFieldNode, XmlDef.DATATYPE_CLASSNAME, dataFieldDefinition.getDataTypeClassName());
      xmlDefBuilder.createNode(dataFieldNode, XmlDef.SCRIPTING_VALUE, dataFieldDefinition.getScriptingValue());
      xmlDefBuilder.createNode(dataFieldNode, XmlDef.IS_TRANSIENT, dataFieldDefinition.isTransient());

      Node enumerationValuesNode = xmlDefBuilder.createNode(dataFieldNode, XmlDef.ENUMERATION_VALUES);
      for (String enumerationValue : dataFieldDefinition.getEnumerationValues()) {
        xmlDefBuilder.createNode(enumerationValuesNode, XmlDef.ENUMERATION_VALUE, enumerationValue);
      }
    }
  }

  private void createAttachments(XmlBuilder xmlDefBuilder, Node parentNode, Map<String, AttachmentDefinition> attachments) throws Exception {

    Node attachmentsNode = xmlDefBuilder.createNode(parentNode, XmlDef.ATTACHMENTS);
    for (AttachmentDefinition attachmentDefinition : attachments.values()) {
      Map<String, Serializable> attachmentElementAttributes = new HashMap<String, Serializable>();
      attachmentElementAttributes.put(XmlDef.NAME, attachmentDefinition.getName());
      Node attachmentNode = xmlDefBuilder.createNode(attachmentsNode, XmlDef.ATTACHMENT, attachmentElementAttributes);

      xmlDefBuilder.createNode(attachmentNode, XmlDef.LABEL, attachmentDefinition.getLabel());
      xmlDefBuilder.createNode(attachmentNode, XmlDef.FILE_PATH, attachmentDefinition.getFilePath());
      xmlDefBuilder.createNode(attachmentNode, XmlDef.DESCRIPTION, attachmentDefinition.getDescription());
      xmlDefBuilder.createNode(attachmentNode, XmlDef.FILE_NAME, attachmentDefinition.getFileName());
    }
  }

  private void createParticipants(XmlBuilder xmlDefBuilder, Node parentNode, Set<ParticipantDefinition> participants) throws Exception {

    Node participantsNode = xmlDefBuilder.createNode(parentNode, XmlDef.PARTICIPANTS);
    for (ParticipantDefinition participantDefinition : participants) {
      Map<String, Serializable> participantElementAttributes = new HashMap<String, Serializable>();
      participantElementAttributes.put(XmlDef.NAME, participantDefinition.getName());
      Node participantNode = xmlDefBuilder.createNode(participantsNode, XmlDef.PARTICIPANT, participantElementAttributes);

      xmlDefBuilder.createNode(participantNode, XmlDef.LABEL, participantDefinition.getLabel());
      xmlDefBuilder.createNode(participantNode, XmlDef.DESCRIPTION, participantDefinition.getDescription());

      createRoleMapper(xmlDefBuilder, participantNode, participantDefinition.getRoleMapper());
    }
  }

  private void createRoleMapper(XmlBuilder xmlDefBuilder, Node parentNode, RoleMapperDefinition roleMapperDefinition) throws Exception {

    if (roleMapperDefinition != null) {
      Node roleMapperNode = xmlDefBuilder.createNode(parentNode, XmlDef.ROLE_MAPPER);

      if (roleMapperDefinition != null){
        xmlDefBuilder.createNode(roleMapperNode, XmlDef.CLASSNAME, roleMapperDefinition.getClassName());
        xmlDefBuilder.createNode(roleMapperNode, XmlDef.DESCRIPTION, roleMapperDefinition.getDescription());

        Node parametersNode = xmlDefBuilder.createNode(roleMapperNode, XmlDef.PARAMETERS);
        Map<String, Object[]> roleMapperParameters = roleMapperDefinition.getParameters();
        for (Entry<String, Object[]> roleMapperParameter : roleMapperParameters.entrySet()) {
          Map<String, Serializable> roleMapperParameterAttributes = new HashMap<String, Serializable>();
          roleMapperParameterAttributes.put(XmlDef.NAME, roleMapperParameter.getKey());
          byte[] value = Misc.serialize(roleMapperParameter.getValue());
          xmlDefBuilder.createNode(parametersNode, XmlDef.PARAMETER, value, roleMapperParameterAttributes);
        }
      }
    }
  }

  private void createConnectors(XmlBuilder xmlDefBuilder, Node parentNode, List<HookDefinition> hooks) throws Exception {

    Node connectorsNode = xmlDefBuilder.createNode(parentNode, XmlDef.CONNECTORS);
    for (HookDefinition hook : hooks) {
      Node connectorNode = xmlDefBuilder.createNode(connectorsNode, XmlDef.CONNECTOR);

      xmlDefBuilder.createNode(connectorNode, XmlDef.CLASSNAME, hook.getClassName());
      xmlDefBuilder.createNode(connectorNode, XmlDef.DESCRIPTION, hook.getDescription());
      xmlDefBuilder.createNode(connectorNode, XmlDef.EVENT, hook.getEvent());
      xmlDefBuilder.createNode(connectorNode, XmlDef.IS_THROWING_EXCEPTION, hook.isThrowingException());
      xmlDefBuilder.createNode(connectorNode, XmlDef.ERROR_CODE, hook.getErrorCode());

      Node parametersNode = xmlDefBuilder.createNode(connectorNode, XmlDef.PARAMETERS);
      Map<String, Object[]> hookParameters = hook.getParameters();
      
      for (Entry<String, Object[]> hookParameter : hookParameters.entrySet()) {
        Map<String, Serializable> hookParametersAttributes = new HashMap<String, Serializable>();
        hookParametersAttributes.put(XmlDef.NAME, hookParameter.getKey());
        byte[] value = Misc.serialize(hookParameter.getValue());
        xmlDefBuilder.createNode(parametersNode, XmlDef.PARAMETER, value, hookParametersAttributes);
      }
    }
  }

}
