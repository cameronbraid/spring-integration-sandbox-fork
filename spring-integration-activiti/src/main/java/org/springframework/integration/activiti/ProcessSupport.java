/* Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.integration.activiti;

import org.activiti.engine.ProcessEngine;
import org.activiti.engine.RuntimeService;
import org.activiti.engine.impl.pvm.delegate.ActivityExecution;
import org.activiti.engine.runtime.Execution;
import org.springframework.integration.Message;
import org.springframework.integration.MessageHeaders;
import org.springframework.util.Assert;

import java.util.*;
import java.util.concurrent.ConcurrentSkipListSet;


/**
 * Provides utility logic to take headers from a {@link org.springframework.integration.MessageHeaders} instance and propagate them as
 * values that can be used as process variables for a {@link  org.activiti.engine.runtime.ProcessInstance}.
 *
 * @author Josh Long
 */
public   class ProcessSupport {

      private final int headerPrefixLength = ActivitiConstants.WELL_KNOWN_SPRING_INTEGRATION_HEADER_PREFIX.length();

      private Collection<String> blackList=new ConcurrentSkipListSet<String>(Arrays.asList(ActivitiConstants.WELL_KNOWN_EXECUTION_ID_HEADER_KEY, ActivitiConstants.WELL_KNOWN_PROCESS_DEFINITION_NAME_HEADER_KEY));


      public Map<String, Object> processVariablesFromMessageHeaders(MessageHeaders msg) {
        return processVariablesFromMessageHeaders(new HashSet<String>(), msg);
    }

      public Map<String, Object> processVariablesFromMessageHeaders(Set<String> whiteListOfMustCopyMessageHeaderKeyNames, MessageHeaders msgHeaders) {
        Map<String, Object> procVars = new HashMap<String, Object>();

        Set<String> headers = msgHeaders.keySet();
        Set<String> wl = (whiteListOfMustCopyMessageHeaderKeyNames == null) ? new HashSet<String>() : whiteListOfMustCopyMessageHeaderKeyNames;

        for (String messageHeaderKey : headers) {
            if ((!blackList.contains(messageHeaderKey) && messageHeaderKey.startsWith(ActivitiConstants.WELL_KNOWN_SPRING_INTEGRATION_HEADER_PREFIX)) || wl.contains(messageHeaderKey)) {
                String pvName = messageHeaderKey.startsWith(ActivitiConstants.WELL_KNOWN_SPRING_INTEGRATION_HEADER_PREFIX) ? messageHeaderKey.substring(headerPrefixLength) : messageHeaderKey;
                procVars.put(pvName, msgHeaders.get(messageHeaderKey));
            }
        }

        return procVars;
    }


    /**
     * Utility method that signals the execution of a process based on a {@link org.springframework.integration.Message}
     *
     * @param processEngine the process engine
     * @param updateProcessVariablesFromReplyMessageHeaders should we set process variables
     *
     * @param message       the message containing the payload
     * @throws org.springframework.integration.MessageHandlingException throws an exception
     *
     */
     public void signalProcessExecution(ProcessEngine processEngine, boolean updateProcessVariablesFromReplyMessageHeaders, Message<?> message) throws Exception {

        RuntimeService runtimeService = processEngine.getRuntimeService();

        MessageHeaders messageHeaders = message.getHeaders();
        String executionId = (String) message.getHeaders().get(ActivitiConstants.WELL_KNOWN_EXECUTION_ID_HEADER_KEY);

        Assert.notNull(executionId, "the messages coming into this channel must have a header equal " +
                "to the value of ActivitiConstants.WELL_KNOWN_EXECUTION_ID_HEADER_KEY (" +
                ActivitiConstants.WELL_KNOWN_EXECUTION_ID_HEADER_KEY + ")");

        Execution execution = runtimeService.createExecutionQuery().executionId(executionId).singleResult();

        if (updateProcessVariablesFromReplyMessageHeaders) {

            Assert.isInstanceOf(ActivityExecution.class, execution, "the execution must be an instance of " + ActivityExecution.class.getName());

//            ActivityExecution activityExecution = (ActivityExecution) execution;

            Map<String, Object> vars = runtimeService.getVariables(executionId);

            Set<String> existingVars = vars.keySet();

            Map<String, Object> procVars = processVariablesFromMessageHeaders(existingVars, messageHeaders);

            for (String key : procVars.keySet())
                //activityExecution.setVariable(key, procVars.get(key));
                runtimeService.setVariable(executionId, key, procVars.get(key));
        }
        runtimeService.signal(executionId);
    }
}
