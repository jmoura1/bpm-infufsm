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
package org.ow2.bonita.parsing.def.binding;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.ow2.bonita.building.XmlDef;
import org.ow2.bonita.facade.def.majorElement.ActivityDefinition.JoinType;
import org.ow2.bonita.facade.def.majorElement.ActivityDefinition.SplitType;
import org.ow2.bonita.facade.def.majorElement.ActivityDefinition.Type;
import org.ow2.bonita.util.BonitaRuntimeException;
import org.ow2.bonita.util.Misc;
import org.ow2.bonita.util.ProcessBuilder;
import org.ow2.bonita.util.xml.Parse;
import org.ow2.bonita.util.xml.Parser;
import org.ow2.bonita.util.xml.XmlUtil;
import org.w3c.dom.Element;

/**
 * @author Anthony Birembaut, Matthieu Chaffotte
 */
public class ActivityBinding extends ElementBinding {

  private static final Logger LOGGER = Logger.getLogger(ActivityBinding.class.getName());

  public ActivityBinding() {
    super(XmlDef.ACTIVITY);
  }

  public Object parse(final Element activityElement, final Parse parse, final Parser parser) {
    if (ActivityBinding.LOGGER.isLoggable(Level.FINE)) {
      ActivityBinding.LOGGER.fine("parsing element = " + activityElement);
    }
    String name = null;
    try {
      ProcessBuilder processBuilder = parse.findObject(ProcessBuilder.class);
      name = XmlUtil.attribute(activityElement, XmlDef.NAME);
      final String description = getChildTextContent(activityElement, XmlDef.DESCRIPTION);
      final String label = getChildTextContent(activityElement, XmlDef.LABEL);
      final String dynamicDescription = getChildTextContent(activityElement, XmlDef.DYNAMIC_DESCRIPTION);
      final String dynamicLabel = getChildTextContent(activityElement, XmlDef.DYNAMIC_LABEL);
      final String dynamicExecutionSummary = getChildTextContent(activityElement, XmlDef.DYNAMIC_EXECUTION_SUMMARY);
      final SplitType splitType = getEnumValue(SplitType.class, getChildTextContent(activityElement, XmlDef.SPLIT_TYPE), SplitType.AND);
      final JoinType joinType = getEnumValue(JoinType.class, getChildTextContent(activityElement, XmlDef.JOIN_TYPE), JoinType.XOR);
      final Type type = getEnumValue(Type.class, getChildTextContent(activityElement, XmlDef.TYPE), Type.Automatic);
      final String asynchronous = getChildTextContent(activityElement, XmlDef.ASYNCHRONOUS);

      final String loopCondition = getChildTextContent(activityElement, XmlDef.LOOP_CONDITION);
      final String loopMaximum = getChildTextContent(activityElement, XmlDef.LOOP_MAXIMUM);
      final String beforeExecution = getChildTextContent(activityElement, XmlDef.BEFORE_EXECUTION);

      final String terminateProcess = getChildTextContent(activityElement, XmlDef.TERMINATE_PROCESS);
      
      Boolean isReceiveEventActivity = Boolean.FALSE;

      if ("true".equals(terminateProcess)) {
        processBuilder.addTerminateEndEvent(name);
      } else if (Type.Automatic.equals(type)) {
        processBuilder.addSystemTask(name);
      } else if (Type.Human.equals(type)) {
        Element performersElement = XmlUtil.element(activityElement, XmlDef.PERFORMERS);
        List<Element> performerElements = XmlUtil.elements(performersElement, XmlDef.PERFORMER);
        if (performerElements != null) {
          String[] performers = new String[performerElements.size()];
          for (int i = 0; i < performerElements.size(); i++) {
            performers[i] = performerElements.get(i).getTextContent();
          }
          processBuilder.addHumanTask(name, performers);
        } else {
          processBuilder.addHumanTask(name);
        }
      } else if (Type.ReceiveEvent.equals(type)) {
        isReceiveEventActivity = Boolean.TRUE;
        Element incommingEventElement = XmlUtil.element(activityElement, XmlDef.INCOMING_EVENT);
        String incommingEventName = XmlUtil.attribute(incommingEventElement, XmlDef.NAME);
        String incommingEventExpression = getChildTextContent(incommingEventElement, XmlDef.EXPRESSION);
        processBuilder.addReceiveEventTask(name, incommingEventName, incommingEventExpression);
      } else if (Type.SendEvents.equals(type)) {
        processBuilder.addSendEventTask(name);
        Element outgoingEventsParametersElement = XmlUtil.element(activityElement, XmlDef.OUTGOING_EVENTS);
        List<Element> outgoingEventElements = XmlUtil.elements(outgoingEventsParametersElement, XmlDef.OUTGOING_EVENT);
        if (outgoingEventElements != null) {
          for (Element outgoingEventElement : outgoingEventElements) {
            String outgoingEventName = XmlUtil.attribute(outgoingEventElement, XmlDef.NAME);
            String outgoingEventToActivityName = getChildTextContent(outgoingEventElement, XmlDef.TO_ACTIVITY);
            String outgoingEventToProcessName = getChildTextContent(outgoingEventElement, XmlDef.TO_PROCESS);
            Element outgoingEventParametersElement = XmlUtil.element(outgoingEventElement, XmlDef.PARAMETERS);
            List<Element> outgoingEventParameterElements = XmlUtil.elements(outgoingEventParametersElement, XmlDef.PARAMETER);
            Map<String, Object> outgoingEventParameters = new HashMap<String, Object>();
            if (outgoingEventParameterElements != null) {
              for (Element outgoingEventparameterElement : outgoingEventParameterElements) {
                try {
                  outgoingEventParameters.put(outgoingEventparameterElement.getAttribute(XmlDef.NAME), Misc.deserialize(Misc.base64DecodeAndGather(outgoingEventparameterElement.getTextContent()), parse.getContextProperties()));
                } catch (Exception e) {
                  throw new BonitaRuntimeException("Error while deserializing", e);
                }
              }
            }
            String outgoingEventTimeToLive = getChildTextContent(outgoingEventElement, XmlDef.TIME_TO_LIVE);
            if (outgoingEventTimeToLive != null) {
              processBuilder.addOutgoingEvent(outgoingEventName, outgoingEventToProcessName, outgoingEventToActivityName, Long.parseLong(outgoingEventTimeToLive), outgoingEventParameters);
            } else {
              processBuilder.addOutgoingEvent(outgoingEventName, outgoingEventToProcessName, outgoingEventToActivityName, outgoingEventParameters);
            }
          }
        }
      } else if (Type.Timer.equals(type)) {
        final String timerCondition = getChildTextContent(activityElement, XmlDef.TIMER_CONDITION);
        processBuilder.addTimerTask(name, timerCondition);
      } else if (Type.ErrorEvent.equals(type)) {
        final String errorCode = getChildTextContent(activityElement, XmlDef.TIMER_CONDITION);
        processBuilder.addErrorEventTask(name, errorCode);
      } else if (Type.SignalEvent.equals(type)) {
        final String signalCode = getChildTextContent(activityElement, XmlDef.TIMER_CONDITION);
        final String catchError = getChildTextContent(activityElement, XmlDef.CATCH_EVENT);
        if (catchError != null) {
          processBuilder.addSignalEventTask(name, signalCode, Boolean.parseBoolean(catchError));
        } else {
          processBuilder.addSignalEventTask(name, signalCode);
        }
      } else if (Type.Subflow.equals(type)) {
        final String subflowProcessName = getChildTextContent(activityElement, XmlDef.SUBFLOW_PROCESS_NAME);
        final String subflowProcessVersion = getChildTextContent(activityElement, XmlDef.SUBFLOW_PROCESS_VERSION);
        processBuilder.addSubProcess(name, subflowProcessName, subflowProcessVersion);
        Element subProcessInParametersElement = XmlUtil.element(activityElement, XmlDef.SUBFLOW_IN_PARAMETERS);
        final List<Element> subProcessInParameterElements = XmlUtil.elements(subProcessInParametersElement, XmlDef.SUBFLOW_IN_PARAMETER);
        if (subProcessInParameterElements != null) {
          for (Element subProcessInParameterElement : subProcessInParameterElements) {
            final String parentProcessDatafieldName = getChildTextContent(subProcessInParameterElement, XmlDef.SOURCE);
            final String subProcessDatafieldName = getChildTextContent(subProcessInParameterElement, XmlDef.DESTINATION);
            processBuilder.addSubProcessInParameter(parentProcessDatafieldName, subProcessDatafieldName);
          }
        }
        Element subProcessOutParametersElement = XmlUtil.element(activityElement, XmlDef.SUBFLOW_OUT_PARAMETERS);
        final List<Element> subProcessOutParameterElements = XmlUtil.elements(subProcessOutParametersElement, XmlDef.SUBFLOW_OUT_PARAMETER);
        if (subProcessOutParameterElements != null) {
          for (Element subProcessOutParameterElement : subProcessOutParameterElements) {
            final String subProcessDatafieldName = getChildTextContent(subProcessOutParameterElement, XmlDef.SOURCE);
            final String parentProcessDatafieldName = getChildTextContent(subProcessOutParameterElement, XmlDef.DESTINATION);
            processBuilder.addSubProcessOutParameter(subProcessDatafieldName, parentProcessDatafieldName);
          }
        }
      } else if (Type.Decision.equals(type)) {
        processBuilder.addDecisionNode(name);
      } else {
        parse.addProblem("unsupported activity type : " + type);
      }
      processBuilder.addLabel(label);
      processBuilder.addDescription(description);
      processBuilder.addDynamicLabel(dynamicLabel);
      processBuilder.addDynamicExecutionSummary(dynamicExecutionSummary);
      processBuilder.addDynamicDescription(dynamicDescription);
      processBuilder.addSplitType(splitType);
      processBuilder.addJoinType(joinType);
      if (loopCondition != null && beforeExecution != null) {
        processBuilder.addLoop(loopCondition, Boolean.parseBoolean(beforeExecution), loopMaximum);
      }
      if (Boolean.parseBoolean(asynchronous)) {
        processBuilder.asynchronous();
      }

      final String priority = getChildTextContent(activityElement, XmlDef.PRIORITY);
      if (priority != null && priority.length() > 0) {
        processBuilder.addActivityPriority(Integer.parseInt(priority));
      }

      final String executingTime = getChildTextContent(activityElement, XmlDef.EXECUTING_TIME);
      if (executingTime != null && Long.parseLong(executingTime) > 0) {
        processBuilder.addActivityExecutingTime(Long.parseLong(executingTime));
      }

      parseBoundaryEvents(activityElement, processBuilder, parse.getContextProperties());
      parseMultiInstantiation(activityElement, processBuilder, parse.getContextProperties());
      parseMultipleActivitiesInstantiator(activityElement, processBuilder, parse.getContextProperties());
      parseMultipleActivitiesJoinChecker(activityElement, processBuilder, parse.getContextProperties());
      parseFilter(activityElement, processBuilder, parse.getContextProperties());
      parseDeadlines(activityElement, processBuilder, parse.getContextProperties());
      parseElementList(activityElement, XmlDef.DATA_FIELDS, XmlDef.DATA_FIELD, parse, parser);
      parse.pushObject(isReceiveEventActivity);
      parseElementList(activityElement, XmlDef.CONNECTORS, XmlDef.CONNECTOR, parse, parser);
      parse.popObject();

    } catch (Exception e) {
      parse.addProblem("Error parsing Activity " + name, e);
    }
    return null;
  }

  private void parseMultiInstantiation(Element activityElement, ProcessBuilder processBuilder, Properties contextProperties) {
    final Element multiInstantiationElement = XmlUtil.element(activityElement, XmlDef.MULTI_INSTANTIATION);
    if (multiInstantiationElement != null && XmlUtil.elements(multiInstantiationElement) != null && !XmlUtil.elements(multiInstantiationElement).isEmpty()) {
      final String multiInstantiationClassName = getChildTextContent(multiInstantiationElement, XmlDef.CLASSNAME);
      final String multiInstantiationVariableName = getChildTextContent(multiInstantiationElement, XmlDef.VARIABLE_NAME);
      processBuilder.addMultiInstanciation(multiInstantiationVariableName, multiInstantiationClassName);
      Element multiInstantiationParametersElement = XmlUtil.element(multiInstantiationElement, XmlDef.PARAMETERS);
      List<Element> multiInstantiationParameterElements = XmlUtil.elements(multiInstantiationParametersElement, XmlDef.PARAMETER);
      if (multiInstantiationParameterElements != null) {
        for (Element multiInstantiationParameterElement : multiInstantiationParameterElements) {
          try {
            processBuilder.addInputParameter(multiInstantiationParameterElement.getAttribute(XmlDef.NAME), (Object[])Misc.deserialize(Misc.base64DecodeAndGather(multiInstantiationParameterElement.getTextContent()), contextProperties));
          } catch (Exception e) {
            throw new BonitaRuntimeException("Error while deserializing", e);
          }
        }
      }
    }
  }

  private void parseMultipleActivitiesInstantiator(Element activityElement, ProcessBuilder processBuilder, Properties contextProperties) {
    final Element instantiatorElement = XmlUtil.element(activityElement, XmlDef.MULTIPLE_ACT_INSTANTIATOR);
    if (instantiatorElement != null && XmlUtil.element(instantiatorElement) != null) {
      String instantiatorClassName = getChildTextContent(instantiatorElement, XmlDef.CLASSNAME);
      processBuilder.addMultipleActivitiesInstantiator(instantiatorClassName);

      Element instantiatorParametersElement = XmlUtil.element(instantiatorElement, XmlDef.PARAMETERS);
      if (instantiatorParametersElement != null) {
        List<Element> instantiatorParametersElements = XmlUtil.elements(instantiatorParametersElement, XmlDef.PARAMETER);
        if (instantiatorParametersElements != null) {
          for (Element instantiatorParameterElement : instantiatorParametersElements) {
            try {
              String key = instantiatorParameterElement.getAttribute(XmlDef.NAME);
              processBuilder.addInputParameter(key, (Object[])Misc.deserialize(Misc.base64DecodeAndGather(instantiatorParameterElement.getTextContent()), contextProperties));
            } catch (Exception e) {
              throw new BonitaRuntimeException("Error while deserializing", e);
            }
          }
        }
      }
    }
  }

  private void parseMultipleActivitiesJoinChecker(Element activityElement, ProcessBuilder processBuilder, Properties contextProperties) {
    final Element joinCheckerElement = XmlUtil.element(activityElement, XmlDef.MULTIPLE_ACT_JOINCHECKER);
    if (joinCheckerElement != null && XmlUtil.element(joinCheckerElement) != null) {
      String joinCheckerClassName = getChildTextContent(joinCheckerElement, XmlDef.CLASSNAME);
      processBuilder.addMultipleActivitiesJoinChecker(joinCheckerClassName);

      Element joinCheckerParametersElement = XmlUtil.element(joinCheckerElement, XmlDef.PARAMETERS);
      if (joinCheckerParametersElement != null) {
        List<Element> joinCheckerParametersElements = XmlUtil.elements(joinCheckerParametersElement, XmlDef.PARAMETER);
        if (joinCheckerParametersElements != null) {
          for (Element joinCheckerParameterElement : joinCheckerParametersElements) {
            try {
              String key = joinCheckerParameterElement.getAttribute(XmlDef.NAME);
              processBuilder.addInputParameter(key, (Object[])Misc.deserialize(Misc.base64DecodeAndGather(joinCheckerParameterElement.getTextContent()), contextProperties));
            } catch (Exception e) {
              throw new BonitaRuntimeException("Error while deserializing", e);
            }
          }
        }
      }
    }
  }

  private void parseFilter(Element activityElement, ProcessBuilder processBuilder, Properties contextProperties) {
    final Element filterElement = XmlUtil.element(activityElement, XmlDef.FILTER);
    if (filterElement != null && XmlUtil.elements(filterElement) != null && !XmlUtil.elements(filterElement).isEmpty()) {
      final String filterClassName = getChildTextContent(filterElement, XmlDef.CLASSNAME);
      final String filterDescription = getChildTextContent(filterElement, XmlDef.DESCRIPTION);
      processBuilder.addFilter(filterClassName);
      processBuilder.addDescription(filterDescription);
      Element filterParametersElement = XmlUtil.element(filterElement, XmlDef.PARAMETERS);
      List<Element> filterParameterElements = XmlUtil.elements(filterParametersElement, XmlDef.PARAMETER);
      if (filterParameterElements != null) {
        for (Element filterParameterElement : filterParameterElements) {
          try {
            processBuilder.addInputParameter(filterParameterElement.getAttribute(XmlDef.NAME), (Object[])Misc.deserialize(Misc.base64DecodeAndGather(filterParameterElement.getTextContent()), contextProperties));
          } catch (Exception e) {
            throw new BonitaRuntimeException("Error while deserializing", e);
          }
        }
      }
    }
  }

  private void parseBoundaryEvents(Element activityElement, ProcessBuilder processBuilder, Properties contextProperties) {
    Element boundaryEventsElement = XmlUtil.element(activityElement, XmlDef.BOUNDARY_EVENTS);
    Element timersElement = XmlUtil.element(boundaryEventsElement, XmlDef.TIMER_EVENTS);
    final List<Element> timerElements = XmlUtil.elements(timersElement);
    if (timerElements != null) {
      for (Element timerElement : timerElements) {
        final String name = XmlUtil.attribute(timerElement, XmlDef.NAME);
        final String condition = getChildTextContent(timerElement, XmlDef.CONDITION);
        final String description = getChildTextContent(timerElement, XmlDef.DESCRIPTION);
        final String label = getChildTextContent(timerElement, XmlDef.LABEL);
        processBuilder.addTimerBoundaryEvent(name, condition);
        processBuilder.addLabel(label);
        processBuilder.addDescription(description);
      }
    }

    Element messagesElement = XmlUtil.element(boundaryEventsElement, XmlDef.MESSAGE_EVENTS);
    final List<Element> messageElements = XmlUtil.elements(messagesElement);
    if (messageElements != null) {
      for (Element messageElement : messageElements) {
        final String name = XmlUtil.attribute(messageElement, XmlDef.NAME);
        final String expression = getChildTextContent(messageElement, XmlDef.EXPRESSION);
        final String description = getChildTextContent(messageElement, XmlDef.DESCRIPTION);
        final String label = getChildTextContent(messageElement, XmlDef.LABEL);
        processBuilder.addMessageBoundaryEvent(name, expression);
        processBuilder.addLabel(label);
        processBuilder.addDescription(description);
      }
    }

    Element errorsElement = XmlUtil.element(boundaryEventsElement, XmlDef.ERROR_EVENTS);
    final List<Element> errorElements = XmlUtil.elements(errorsElement);
    if (errorElements != null) {
      for (Element errorElement : errorElements) {
        final String name = XmlUtil.attribute(errorElement, XmlDef.NAME);
        final String errorCode = getChildTextContent(errorElement, XmlDef.ERROR_CODE);
        final String description = getChildTextContent(errorElement, XmlDef.DESCRIPTION);
        final String label = getChildTextContent(errorElement, XmlDef.LABEL);
        if (errorCode == null) {
          processBuilder.addErrorBoundaryEvent(name);
        } else {
          processBuilder.addErrorBoundaryEvent(name, errorCode);
        }
        processBuilder.addLabel(label);
        processBuilder.addDescription(description);
      }
    }

    Element signalsElement = XmlUtil.element(boundaryEventsElement, XmlDef.SIGNAL_EVENTS);
    final List<Element> signalElements = XmlUtil.elements(signalsElement);
    if (signalElements != null) {
      for (Element signalElement : signalElements) {
        final String name = XmlUtil.attribute(signalElement, XmlDef.NAME);
        final String errorCode = getChildTextContent(signalElement, XmlDef.SIGNAL_CODE);
        final String description = getChildTextContent(signalElement, XmlDef.DESCRIPTION);
        final String label = getChildTextContent(signalElement, XmlDef.LABEL);
        processBuilder.addSignalBoundaryEvent(name, errorCode);
        processBuilder.addLabel(label);
        processBuilder.addDescription(description);
      }
    }
  }

  private void parseDeadlines(Element activityElement, ProcessBuilder processBuilder, Properties contextProperties) {
    Element deadlinesElement = XmlUtil.element(activityElement, XmlDef.DEADLINES);
    final List<Element> deadlineElements = XmlUtil.elements(deadlinesElement, XmlDef.DEADLINE);
    if (deadlineElements != null) {
      for (Element deadlineElement : deadlineElements) {
        final String condition = getChildTextContent(deadlineElement, XmlDef.CONDITION);
        final String connectorClassName = getChildTextContent(deadlineElement, XmlDef.CLASSNAME);
        final String deadlineDescription = getChildTextContent(deadlineElement, XmlDef.DESCRIPTION);
        processBuilder.addDeadline(condition, connectorClassName);
        processBuilder.addDescription(deadlineDescription);
        Element deadlineParametersElement = XmlUtil.element(deadlineElement, XmlDef.PARAMETERS);
        List<Element> deadlineParameterElements = XmlUtil.elements(deadlineParametersElement, XmlDef.PARAMETER);
        if (deadlineParameterElements != null) {
          for (Element deadlineParameterElement : deadlineParameterElements) {
            try {
              String key = deadlineParameterElement.getAttribute(XmlDef.NAME);
              if (Misc.isSetter(key)) {
                processBuilder.addInputParameter(key, (Object[])Misc.deserialize(Misc.base64DecodeAndGather(deadlineParameterElement.getTextContent()), contextProperties));
              } else {
                processBuilder.addOutputParameter(((Object[])Misc.deserialize(Misc.base64DecodeAndGather(deadlineParameterElement.getTextContent()), contextProperties))[0].toString(), key);
              }
            } catch (Exception e) {
              throw new BonitaRuntimeException("Error while deserializing", e);
            }
          }
        }
      }
    }
  }

}
