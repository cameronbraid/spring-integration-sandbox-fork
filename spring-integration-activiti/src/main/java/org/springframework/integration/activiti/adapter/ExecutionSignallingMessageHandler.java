package org.springframework.integration.activiti.adapter;

import org.activiti.engine.ProcessEngine;
import org.activiti.engine.impl.pvm.delegate.ActivityExecution;
import org.activiti.engine.runtime.Execution;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.integration.Message;
import org.springframework.integration.MessagingException;
import org.springframework.integration.activiti.ActivitiConstants;
import org.springframework.integration.activiti.ProcessSupport;
import org.springframework.integration.activiti.mapping.DefaultProcessVariableHeaderMapper;
import org.springframework.integration.core.MessageHandler;
import org.springframework.util.Assert;

/**
 * this signals execution for a wait-state when an inbound message arrives. it makes no
 * assumptions about how a wait-state was entered, just that it will respond to
 * {@link org.activiti.engine.RuntimeService#signal(String)} appropriately.
 *
 * @author Josh Long
 * @since 2.1
 */
public class ExecutionSignallingMessageHandler implements MessageHandler, InitializingBean {

  private ProcessEngine processEngine;
  private DefaultProcessVariableHeaderMapper processVariableHeaderMapper;

  public void setProcessVariableHeaderMapper(DefaultProcessVariableHeaderMapper processVariableHeaderMapper) {
    this.processVariableHeaderMapper = processVariableHeaderMapper;
  }

  public void setProcessEngine(ProcessEngine processEngine) {
    this.processEngine = processEngine;
  }

  public void handleMessage(Message<?> message) throws MessagingException {
    try {

      ProcessSupport.signalProcessExecution(this.processEngine, processVariableHeaderMapper, message);
    } catch (Exception ex) {
      throw new MessagingException(message, ex);
    }
  }

  public void afterPropertiesSet() throws Exception {
    Assert.notNull(this.processEngine, "processEngine can't be null");
  }
}
