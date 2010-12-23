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
import org.activiti.engine.impl.pvm.PvmActivity;
import org.activiti.engine.impl.pvm.delegate.ActivityExecution;
import org.activiti.engine.impl.runtime.ExecutionEntity;
import org.springframework.integration.Message;
import org.springframework.integration.activiti.mapping.DefaultProcessVariableHeaderMapper;

import java.util.HashMap;
import java.util.Map;

/**
 * Provides utility logic to take headers from a {@link org.springframework.integration.MessageHeaders} instance and propagate them as
 * values that can be used as process variables for a {@link  org.activiti.engine.runtime.ProcessInstance}.
 *
 * @author Josh Long
 */
public abstract class ProcessSupport {

  public static void encodeCommonProcessDataIntoMessage(ActivityExecution execution, Map<String, Object> vars) {
    PvmActivity pvmActivity = execution.getActivity();

    ExecutionEntity executionEntity = (ExecutionEntity) execution;

    String procDefId = executionEntity.getProcessDefinitionId();
    String procInstanceId = executionEntity.getProcessInstanceId();
    String executionId = execution.getId();
    String pvmActivityId = pvmActivity.getId();

    vars.put(ActivitiConstants.WELL_KNOWN_EXECUTION_ID_HEADER_KEY, executionId);
    vars.put(ActivitiConstants.WELL_KNOWN_ACTIVITY_ID_HEADER_KEY, pvmActivityId);
    vars.put(ActivitiConstants.WELL_KNOWN_PROCESS_DEFINITION_ID_HEADER_KEY, procDefId);
    vars.put(ActivitiConstants.WELL_KNOWN_PROCESS_INSTANCE_ID_HEADER_KEY, procInstanceId);
  }

  static public interface ProcessExecutionSignallerCallback {

    void setProcessVariable(ProcessEngine en, ActivityExecution ex, String k, Object o);

    void signal(ProcessEngine en, ActivityExecution ex);
  }

  public static void signalProcessExecution(ProcessEngine processEngine, ActivityExecution activityExecution,
                                            ProcessExecutionSignallerCallback callback,
                                            DefaultProcessVariableHeaderMapper processVariableHeaderMapper,
                                            Message<?> message) throws Exception {

    DefaultProcessVariableHeaderMapper defaultProcessVariableHeaderMapper =
        new DefaultProcessVariableHeaderMapper(processVariableHeaderMapper, activityExecution);

    Map<String, Object> vars = new HashMap<String, Object>();
    defaultProcessVariableHeaderMapper.fromHeaders(message.getHeaders(), vars);

    for (String key : vars.keySet())
      callback.setProcessVariable(processEngine, activityExecution, key, vars.get(key));

    callback.signal(processEngine, activityExecution);
  }
}
