package org.springframework.integration.activiti.adapter;

import org.activiti.engine.ProcessEngine;
import org.springframework.integration.Message;
import org.springframework.integration.MessagingException;
import org.springframework.integration.activiti.ProcessSupport;
import org.springframework.integration.core.MessageHandler;

/**
 * this signals execution for a wait-state when an inbound message arrives. it makes no
 * assumptions about how a wait-state was entered, just that it will respond to
 * {@link org.activiti.engine.RuntimeService#signal(String)} appropriately.
 *
 * @author Josh Long
 * @since 2.1
 */
public class ExecutionSignallingMessageHandler implements MessageHandler {

  private ProcessEngine processEngine;

  private boolean updateProcessVariablesFromReplyMessageHeaders;

  public void setProcessEngine(ProcessEngine processEngine) {
    this.processEngine = processEngine;
  }

  /**
   * @param updateProcessVariablesFromReplyMessageHeaders whether or not we should update the business process with headers from the received {@link org.springframework.integration.Message}s. gets passed to {@link ProcessSupport#signalProcessExecution(org.activiti.engine.ProcessEngine, boolean, org.springframework.integration.Message)}
   */
  public void setUpdateProcessVariablesFromReplyMessageHeaders(boolean updateProcessVariablesFromReplyMessageHeaders) {
    this.updateProcessVariablesFromReplyMessageHeaders = updateProcessVariablesFromReplyMessageHeaders;
  }

  public void handleMessage(Message<?> message) throws MessagingException {
    try {
      ProcessSupport.signalProcessExecution(this.processEngine, this.updateProcessVariablesFromReplyMessageHeaders, message);
    } catch (Exception ex) {
      throw new MessagingException(message, ex);
    }
  }
}
