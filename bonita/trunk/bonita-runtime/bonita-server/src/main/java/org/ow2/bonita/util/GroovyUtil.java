/**
 * Copyright (C) 2009  BonitaSoft S.A.
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
package org.ow2.bonita.util;

import static org.ow2.bonita.util.GroovyExpression.END_DELIMITER;
import static org.ow2.bonita.util.GroovyExpression.START_DELIMITER;
import groovy.lang.Binding;
import groovy.lang.MissingPropertyException;
import groovy.lang.Script;

import java.io.IOException;
import java.io.NotSerializableException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.ow2.bonita.facade.RuntimeAPI;
import org.ow2.bonita.facade.exception.ActivityDefNotFoundException;
import org.ow2.bonita.facade.exception.DataFieldNotFoundException;
import org.ow2.bonita.facade.exception.ProcessNotFoundException;
import org.ow2.bonita.facade.impl.StandardAPIAccessorImpl;
import org.ow2.bonita.facade.runtime.ActivityInstance;
import org.ow2.bonita.facade.runtime.ProcessInstance;
import org.ow2.bonita.facade.runtime.impl.ObjectVariable;
import org.ow2.bonita.facade.uuid.ActivityInstanceUUID;
import org.ow2.bonita.facade.uuid.ProcessDefinitionUUID;
import org.ow2.bonita.facade.uuid.ProcessInstanceUUID;
import org.ow2.bonita.util.GroovyBindingBuilder.PropagateBinding;

/**
 * 
 * @author Matthieu Chaffotte
 *
 */
public class GroovyUtil {

  public static Object evaluate(String expression, Map<String, Object> variables, ClassLoader classLoader) throws GroovyException {
    return evaluate(expression, variables, null, null, null, false, false, false, classLoader);
  }

  public static Object evaluate(String expression, Map<String, Object> variables) throws GroovyException {
    return evaluate(expression, variables, null, null, null, false, false, false, null);
  }

  public static Object evaluate(String expression, Map<String, Object> context, ActivityInstanceUUID activityUUID, boolean useActivityScope, boolean propagate) throws GroovyException {
    final ActivityInstance activity = EnvTool.getAllQueriers().getActivityInstance(activityUUID);
    ProcessInstanceUUID processInstanceUUID = activity.getProcessInstanceUUID();
    ProcessDefinitionUUID processDefinitionUUID = activity.getProcessDefinitionUUID();
    return evaluate(expression, context, processDefinitionUUID, activityUUID, processInstanceUUID, useActivityScope, false, propagate, null);
  }

  public static Object evaluate(String expression, Map<String, Object> context, ProcessInstanceUUID instanceUUID, boolean useInitialVariableValues, boolean propagate) throws GroovyException {
    ProcessDefinitionUUID processDefinitionUUID = null;
    if (instanceUUID != null) {
      final ProcessInstance instance = EnvTool.getAllQueriers().getProcessInstance(instanceUUID);
      processDefinitionUUID = instance.getProcessDefinitionUUID();
    }
    return evaluate(expression, context, processDefinitionUUID, null, instanceUUID, false, useInitialVariableValues, propagate, null);
  }

  public static Object evaluate(String expression, Map<String, Object> context, ProcessDefinitionUUID processUUID, boolean propagate) throws GroovyException {
    return evaluate(expression, context, processUUID, null, null, false, false, propagate, null);
  }

  private static Object evaluate(String expression, Map<String, Object> context, ProcessDefinitionUUID processDefinitionUUID, ActivityInstanceUUID activityUUID, 
      ProcessInstanceUUID instanceUUID, boolean useActivityScope, boolean useInitialVariableValues, boolean propagate, ClassLoader classLoader) throws GroovyException {

    if (expression == null || "".equals(expression.trim())) {
      final String message = getMessage(activityUUID, instanceUUID, processDefinitionUUID, "The expression is null or empty.");
      throw new GroovyException(message);
    } else {
      int begin = expression.indexOf(START_DELIMITER);
      int end = expression.indexOf(END_DELIMITER);
      if (begin >= end) {
        final String message = getMessage(activityUUID, instanceUUID, processDefinitionUUID, "The expression is not a Groovy one: " + expression + ".");
        throw new GroovyException(message);
      }
      final boolean oneUuidNotNull = processDefinitionUUID != null || instanceUUID != null || activityUUID != null;
      if (Misc.isJustAGroovyExpression(expression) && oneUuidNotNull) {
        final String insideExpression = expression.substring(begin + START_DELIMITER.length(), end).trim();
        if (Misc.isJavaIdentifier(insideExpression) && !"true".equals(insideExpression) && !"false".equals(insideExpression)) {
          try {
            final Object injectedVariable = GroovyBindingBuilder.getInjectedVariable(insideExpression, processDefinitionUUID, instanceUUID, activityUUID);
            if (injectedVariable != null) {
              return injectedVariable;
            }
            final Map<String, Object> allVariables = GroovyBindingBuilder.getContext(context, processDefinitionUUID, activityUUID, instanceUUID, useActivityScope, useInitialVariableValues);

            final Object result = allVariables.get(insideExpression);
            if (result != null && result instanceof ObjectVariable) {
              return ((ObjectVariable)result).getValue();
            }
            return result;
          } catch (Throwable t) {}
        }
      }
    }
    ClassLoader ori = Thread.currentThread().getContextClassLoader();
    try {
      ProcessDefinitionUUID pUUID = processDefinitionUUID;
      ActivityInstance activity = null;
      if (pUUID == null && instanceUUID != null) {
        final ProcessInstance instance = EnvTool.getAllQueriers().getProcessInstance(instanceUUID);
        pUUID = instance.getProcessDefinitionUUID();
      } else if (activityUUID != null) {
        activity = EnvTool.getAllQueriers().getActivityInstance(activityUUID);
        if (pUUID == null) {
          pUUID = activity.getProcessDefinitionUUID();
        }
      }

      if (pUUID != null && classLoader == null) {
        ClassLoader processClassLoader = EnvTool.getClassDataLoader().getProcessClassLoader(pUUID);
        Thread.currentThread().setContextClassLoader(processClassLoader);
      } else if (classLoader != null) {
        Thread.currentThread().setContextClassLoader(classLoader);
      }

      Binding binding = null;
      if (propagate) {
        binding = GroovyBindingBuilder.getPropagateBinding(processDefinitionUUID, instanceUUID, activityUUID, context, useActivityScope, useInitialVariableValues);
      } else {
        binding = GroovyBindingBuilder.getSimpleBinding(processDefinitionUUID, instanceUUID, activityUUID, context, useActivityScope, useInitialVariableValues);
      }
      
      final Object result = evaluate(expression, binding);
      if (propagate && instanceUUID != null) {
        propagateVariables(((PropagateBinding)binding).getVariablesToPropagate(), activityUUID, instanceUUID);
      }
      return result;
    } catch (Exception e) {
      final String message = getMessage(activityUUID, instanceUUID, processDefinitionUUID, "Exception while evaluating expression.");
      throw new GroovyException(message, e);
    } finally {
      if (ori != null && ori != Thread.currentThread().getContextClassLoader()) {
        Thread.currentThread().setContextClassLoader(ori);
      }
    }
  }

  public static Object evaluate(final String expression, final Binding binding) throws GroovyException, NotSerializableException, ActivityDefNotFoundException, DataFieldNotFoundException, ProcessNotFoundException, IOException, ClassNotFoundException {
    String workingExpression = expression;
    Object result = null;
    if (Misc.isJustAGroovyExpression(workingExpression)) {
      workingExpression = workingExpression.substring(START_DELIMITER.length());
      workingExpression = workingExpression.substring(0, workingExpression.lastIndexOf(END_DELIMITER));
      result = evaluateGroovyExpression(workingExpression, binding);
    } else {
      result = evaluate(getExpressions(workingExpression), binding);
    }
    return result;
  }

  public static void propagateVariables(Map<String, Object> variables, ActivityInstanceUUID activityUUID, ProcessInstanceUUID instanceUUID) throws GroovyException {
    StandardAPIAccessorImpl accessor = new StandardAPIAccessorImpl();
    RuntimeAPI runtime = accessor.getRuntimeAPI();
    if (variables != null) {
      for (Entry<String, Object> variable : variables.entrySet()) {
        try {
          if (activityUUID != null) {
            runtime.setVariable(activityUUID, variable.getKey(), variable.getValue());
          } else {
            runtime.setProcessInstanceVariable(instanceUUID, variable.getKey(), variable.getValue());
          }
        } catch (BonitaException e) {
          final String message = getMessage(activityUUID, instanceUUID, "Error while propagating variables.");
          throw new GroovyException(message, e);
        }
      }
    }
  }

  private static Object evaluateGroovyExpression(String script, Binding binding) throws GroovyException {
    ClassLoader scriptClassLoader = Thread.currentThread().getContextClassLoader();
    Script groovyScript = GroovyScriptBuilder.getScript(script, scriptClassLoader);
    groovyScript.setBinding(binding);
    Object result = null;
    try {
      result = groovyScript.run();
    } catch (MissingPropertyException e) {
      final String lineSeparator = System.getProperty("line.separator", "\n");
      
      final StringBuilder stb = new StringBuilder();
      
      stb.append("Error in Groovy script: unable to use element \"" + e.getProperty() + "\"");
      stb.append(lineSeparator);
      stb.append(lineSeparator);
      stb.append("Possible cause:");
      stb.append(lineSeparator);
      stb.append("- missing import");
      stb.append(lineSeparator);
      stb.append("- variable not found (wrong name, undefined)");
      stb.append(lineSeparator);
      stb.append("- ...");
      stb.append(lineSeparator);
      stb.append(lineSeparator);
      stb.append("Script:");
      stb.append(lineSeparator);
      stb.append("\"" + script + "\"");
      stb.append(lineSeparator);
    
      throw new GroovyException(stb.toString(), e);
    }
    
    return result;
  }

  private static String evaluate(List<String> expressions, Binding binding)  throws GroovyException {
    StringBuilder builder = new StringBuilder();
    int i = 0;
    while (i < expressions.size()) {
      String expression = expressions.get(i);
      if (expression.equals(START_DELIMITER)) {
        expression = expressions.get(++i);
        builder.append(evaluateGroovyExpression(expression, binding));
      } else {
        builder.append(expression);
      }
      i++;
    }
    return builder.toString();
  }

  private static List<String> getExpressions(final String expression) {
    final List<String> expressions = new ArrayList<String>();
    String concat = expression;
    while (concat.contains(START_DELIMITER)) {
      int index = concat.indexOf(START_DELIMITER);
      expressions.add(concat.substring(0, index));
      concat = concat.substring(index);
      expressions.add(START_DELIMITER);
      int endGroovy = Misc.getGroovyExpressionEndIndex(concat);
      expressions.add(concat.substring(2, endGroovy - 1));
      concat = concat.substring(endGroovy);
    }
    expressions.add(concat);
    return expressions;
  }

  private static String getMessage(final ActivityInstanceUUID activityUUID, final ProcessInstanceUUID instanceUUID, 
      final ProcessDefinitionUUID processDefUUID, final String message) {
    final String partialMessage = getMessage(activityUUID, instanceUUID, message);
    final StringBuilder stb = new StringBuilder(partialMessage);
    if (processDefUUID != null){
      stb.append(" ProcessDefinitionUUID: '").append(processDefUUID).append("'.");
    }
    return stb.toString();
  }

  private static String getMessage(final ActivityInstanceUUID activityUUID, final ProcessInstanceUUID instanceUUID, 
      final String message) {
    final StringBuilder stb = new StringBuilder(message);
    if (instanceUUID != null){
      stb.append(" ProcessInstanceUUID: '").append(instanceUUID).append("'.");
    }
    if (activityUUID != null){
      stb.append(" ActivityInstanceUUID: '").append(activityUUID).append("'.");
    }
    return stb.toString();
  }

}
