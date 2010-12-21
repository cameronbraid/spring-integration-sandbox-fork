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
package org.springframework.integration.activiti.mapping;

import org.activiti.engine.ProcessEngine;
import org.activiti.engine.RuntimeService;
import org.activiti.engine.impl.pvm.delegate.ActivityExecution;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.integration.MessageHeaders;
import org.springframework.integration.activiti.ActivitiConstants;
import org.springframework.util.Assert;

import java.util.Map;


/**
 * A {@link org.springframework.integration.mapping.HeaderMapper} implementation for mapping
 * to and from a Map which will ultimately be add via {@link org.activiti.engine.RuntimeService#setVariable(String, String, Object)}.
 * <p/>
 * The {@link #processVariableToHeaderNames} and {@link #headerToProcessVariableNames} may be configured.
 * <p/>
 * They accept exact name Strings or simple patterns (e.g. "start*", "*end", or "*").
 * <p/>
 * By default all headers in {@link  org.springframework.integration.activiti.ActivitiConstants} will be accepted.
 * <p/>
 * Any outbound header that should be mapped must be configured explicitly. Note that the outbound mapping only writes
 * String header values into attributes on the SoapHeader. For anything more advanced,
 * one should implement the HeaderMapper interface directly.
 *
 * @author Josh Long
 * @since 5.1
 */
public class DefaultProcessVariableHeaderMapper implements ProcessVariableHeaderMapper, InitializingBean {

    public DefaultProcessVariableHeaderMapper(DefaultProcessVariableHeaderMapper mapper, ActivityExecution ex) {
        this.currentActivityExecution = ex;
        this.processEngine = mapper.processEngine;
        this.runtimeService = mapper.runtimeService;
        this.headerToProcessVariableNames = mapper.headerToProcessVariableNames;
        this.processVariableToHeaderNames = mapper.processVariableToHeaderNames;
        this.wellKnownActivitiHeaders = mapper.wellKnownActivitiHeaders ;
    }

    public DefaultProcessVariableHeaderMapper(ProcessEngine processEngine, ActivityExecution currentActivityExecution) {
        this.processEngine = processEngine;
        this.currentActivityExecution = currentActivityExecution;
    }

    private Log log = LogFactory.getLog(getClass());

    public void fromHeaders(MessageHeaders headers, Map<String, ?> target) {

    }

    public Map<String, ?> toHeaders(Map<String, ?> source) {
        return null;
    }

    /**
     * Headers that will be available
     */
    private volatile String[] wellKnownActivitiHeaders = {
            ActivitiConstants.WELL_KNOWN_ACTIVITY_ID_HEADER_KEY,
            ActivitiConstants.WELL_KNOWN_EXECUTION_ID_HEADER_KEY,
            ActivitiConstants.WELL_KNOWN_PROCESS_INSTANCE_ID_HEADER_KEY,
            ActivitiConstants.WELL_KNOWN_PROCESS_DEFINITION_ID_HEADER_KEY,
            ActivitiConstants.WELL_KNOWN_PROCESS_DEFINITION_NAME_HEADER_KEY
    };

    /**
     * all headers that we want to forward as process variables. None, by default, as headers may be rich objects where as process variables <em>should</em> be lightweight (primitives, for example)
     */
    private volatile String[] headerToProcessVariableNames = new String[0];

    /**
     * all process variables that should be exposed as headers
     */
    private volatile String[] processVariableToHeaderNames = new String[]{"*"};

    private String errMessage = String.format("'currentActivityExecution' should be rebound on this instance before all " +
            "uses of any methods on the HeaderMapper<T> interface. If the instance is null, " +
            "certain well known headers (like '%s') that are critical to the successful use of much of " +
            "the support provided by this module will not be available.", ActivitiConstants.WELL_KNOWN_EXECUTION_ID_HEADER_KEY);

    /**
     * @see ProcessEngine
     */
    private volatile ProcessEngine processEngine;

    /**
     * the current {@link ActivityExecution} - should be rebound before each use.
     */
    private volatile ActivityExecution currentActivityExecution;

    /**
     * the {@link RuntimeService} as obtained from a {@link org.activiti.engine.ProcessEngine#getRuntimeService()}
     */
    private RuntimeService runtimeService;


    /**
     * I'm not sure that most of our components would work if this
     * were true as it would mean the {@link ActivitiConstants#WELL_KNOWN_EXECUTION_ID_HEADER_KEY} would be null
     */
    private volatile boolean requiresActivityExecution = true;

    public void setRequiresActivityExecution(boolean requiresActivityExecution) {
        this.requiresActivityExecution = requiresActivityExecution;
    }

    public void setHeaderToProcessVariableNames(String[] headerToProcessVariableNames) {
        this.headerToProcessVariableNames = null == headerToProcessVariableNames ? new String[0] : headerToProcessVariableNames;
    }

    public void setProcessVariableToHeaderNames(String[] processVariableToHeaderNames) {
        this.processVariableToHeaderNames = null == processVariableToHeaderNames ? new String[0] : processVariableToHeaderNames;
    }

    public void setProcessEngine(ProcessEngine processEngine) {
        this.processEngine = processEngine;
    }

    private void validate() throws Exception {


        if (this.requiresActivityExecution) {
            Assert.notNull(this.currentActivityExecution, "the currentActivityExecution should be reset on this instance before all uses");
        } else {
            if (this.currentActivityExecution == null) {
                log.warn(errMessage);
            }
        }
        Assert.notNull(this.runtimeService, "'runtimeService' can't be null");


    }


    public void afterPropertiesSet() throws Exception {

        runtimeService = this.processEngine.getRuntimeService();

        validate();
    }
}
