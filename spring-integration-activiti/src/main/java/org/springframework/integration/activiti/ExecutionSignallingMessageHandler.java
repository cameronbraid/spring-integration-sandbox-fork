package org.springframework.integration.activiti;

import org.activiti.engine.ProcessEngine;
import org.activiti.engine.RuntimeService;
import org.activiti.engine.impl.pvm.delegate.ActivityExecution;
import org.activiti.engine.runtime.Execution;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.integration.*;
import org.springframework.integration.core.MessageHandler;
import org.springframework.util.Assert;

import java.util.Map;
import java.util.Set;

/**
 * generic implementation of a {@link MessageHandler} that signals execution of a process
 *
 * @author Josh Long
 * @since 5.1
 */
public class ExecutionSignallingMessageHandler implements MessageHandler, InitializingBean {

    private volatile boolean updateProcessVariablesFromReplyMessageHeaders;

    private volatile ProcessEngine processEngine;

    private ProcessSupport processSupport = new ProcessSupport();

    public void setUpdateProcessVariablesFromReplyMessageHeaders(boolean updateProcessVariablesFromReplyMessageHeaders) {
        this.updateProcessVariablesFromReplyMessageHeaders = updateProcessVariablesFromReplyMessageHeaders;
    }

    public void setProcessEngine(ProcessEngine processEngine) {
        this.processEngine = processEngine;
    }


    /**
     * This class listens for results on the reply channel and triggers the renewed flow of execution
     * upon a {@link Message} receipt. Additionally, if {@link #updateProcessVariablesFromReplyMessageHeaders}
     * is set to true, then this class will take all inboiund message headers and use it to set process variables before
     * forwarding execution. It is assumed that all incoming messages have
     * a {@link ActivitiConstants#WELL_KNOWN_EXECUTION_ID_HEADER_KEY} header, otherwise this class can't do its work.
     */

    public void handleMessage(Message<?> message) throws MessageHandlingException, MessageDeliveryException {
        try {

            RuntimeService runtimeService = processEngine.getRuntimeService();

            MessageHeaders messageHeaders = message.getHeaders();
            String executionId = (String) message.getHeaders().get(ActivitiConstants.WELL_KNOWN_EXECUTION_ID_HEADER_KEY);

            Assert.notNull(executionId, "the messages coming into this channel must have a header equal " +
                    "to the value of ActivitiConstants.WELL_KNOWN_EXECUTION_ID_HEADER_KEY (" +
                    ActivitiConstants.WELL_KNOWN_EXECUTION_ID_HEADER_KEY + ")");

            Execution execution = runtimeService.createExecutionQuery().executionId(executionId).singleResult();

            if (updateProcessVariablesFromReplyMessageHeaders) {

                Assert.isInstanceOf(ActivityExecution.class, execution, "the execution must be an instance of " + ActivityExecution.class.getName());

                ActivityExecution activityExecution = (ActivityExecution) execution;

                /*activityExecution.getVariables();*/
                Map<String, Object> vars = runtimeService.getVariables( executionId);

                Set<String> existingVars = vars.keySet();

                Map<String, Object> procVars = processSupport.processVariablesFromMessageHeaders(existingVars, messageHeaders);

                for (String key : procVars.keySet())
                    //activityExecution.setVariable(key, procVars.get(key));
                    runtimeService.setVariable( executionId,key, procVars.get(key));
            }
            runtimeService.signal(executionId);

        } catch (Throwable throwable) {
            throw new RuntimeException(throwable);
        }
    }

    public void afterPropertiesSet() throws Exception {
        Assert.notNull(this.processEngine, "'processEngine' can't be null");
    }
}
