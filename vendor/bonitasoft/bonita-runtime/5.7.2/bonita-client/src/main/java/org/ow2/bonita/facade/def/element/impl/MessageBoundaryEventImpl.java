package org.ow2.bonita.facade.def.element.impl;

import org.ow2.bonita.facade.def.element.CatchMessageEvent;
import org.ow2.bonita.facade.def.majorElement.TransitionDefinition;
import org.ow2.bonita.facade.uuid.ActivityDefinitionUUID;
import org.ow2.bonita.facade.uuid.ProcessDefinitionUUID;


public class MessageBoundaryEventImpl extends BoundaryEventImpl implements CatchMessageEvent {

  private static final long serialVersionUID = -5187827646315564051L;
  protected String expression;

  protected MessageBoundaryEventImpl() {}

  public MessageBoundaryEventImpl(final String eventName, final ProcessDefinitionUUID processUUID, final ActivityDefinitionUUID activityUUID, final TransitionDefinition exceptionTransition, final String expression) {
    super(eventName, processUUID, activityUUID, exceptionTransition);
    this.expression = expression;
  }

  public MessageBoundaryEventImpl(final MessageBoundaryEventImpl src) {
    super(src);
    this.expression = src.getExpression();
  }

  public String getExpression() {
    return expression;
  }

}
