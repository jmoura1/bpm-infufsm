/*
 * JBoss, Home of Professional Open Source
 * Copyright 2005, JBoss Inc., and individual contributors as indicated
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.ow2.bonita.runtime.model;

import java.util.Collection;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.ow2.bonita.definition.activity.ExternalActivity;
import org.ow2.bonita.facade.def.InternalActivityDefinition;
import org.ow2.bonita.facade.runtime.impl.InternalActivityInstance;
import org.ow2.bonita.runtime.model.Execution.Propagation;
import org.ow2.bonita.services.Recorder;
import org.ow2.bonita.util.BonitaRuntimeException;
import org.ow2.bonita.util.EnvTool;
import org.ow2.bonita.util.ExceptionManager;

public class ExecuteNode {

  static final Logger LOG = Logger.getLogger(ExecuteNode.class.getName());

  public void perform(final Execution execution) {
    perform(execution, true);
  }

  public void perform(final Execution execution, final boolean checkJoinType) {
    final InternalActivityDefinition node = execution.getNode();

    if (LOG.isLoggable(Level.FINE)) {
      LOG.fine(execution.toString() + " executes " + node);
    }

    final ExternalActivity activity = node.getBehaviour();

    try {
      execution.setPropagation(Propagation.UNSPECIFIED);

      activity.execute(execution, checkJoinType);

    } catch (final Throwable e) {
      if (LOG.isLoggable(Level.WARNING)) {
        LOG.log(Level.WARNING, "Activity will be put in the state FAILED due to", e);
      }

      final Recorder recorder = EnvTool.getRecorder();
      final Collection<Execution> executions = execution.getExecutions();
      if (executions != null && !executions.isEmpty()) {
        for (final Execution nextExecution : executions) {
          final InternalActivityInstance activityInstance = nextExecution.getActivityInstance();
          if (activityInstance != null) {
            recorder.recordActivityFailed(activityInstance);
          } else {
            // enable to get the activity instance
            if (e instanceof RuntimeException) {
              throw (RuntimeException) e;
            } else {
              final String message = ExceptionManager.getInstance().getFullMessage("bp_EHI_5",
                  e + ": " + e.getMessage(), e.getMessage());
              throw new BonitaRuntimeException(message, e);
            }
          }
        }
      } else { // no activity executions
        if (e instanceof RuntimeException) {
          throw (RuntimeException) e;
        } else {
          final String message = ExceptionManager.getInstance().getFullMessage("bp_EHI_5", e + ": " + e.getMessage(),
              e.getMessage());
          throw new BonitaRuntimeException(message, e);
        }
      }

    }

    if (execution.getPropagation() == Propagation.UNSPECIFIED) {
      execution.proceed();
    }
  }

  @Override
  public String toString() {
    return "execute(node)";
  }

}