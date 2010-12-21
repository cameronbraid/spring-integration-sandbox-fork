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
import org.springframework.integration.activiti.ProcessSupport;
import org.springframework.util.*;

import java.util.*;


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

    /**
     * make sure that nobody sets the {@link #currentActivityExecution} while we're using it in service to {@link #toHeaders(java.util.Map)}
     */
    private final Object activityExecutionMonitor = new Object();

    private boolean shouldPrefixProcessVariables = false;

    private Log log = LogFactory.getLog(getClass());

    private String prefix = ActivitiConstants.WELL_KNOWN_SPRING_INTEGRATION_HEADER_PREFIX;

    private int wellKnownHeaderPrefixLength;

    /**
     * whether or not we should include fields that begin with the {@link #prefix}
     */
    public void setIncludeHeadersWithWellKnownPrefix(boolean includeHeadersWithWellKnownPrefix) {
        this.includeHeadersWithWellKnownPrefix = includeHeadersWithWellKnownPrefix;
    }

    /**
     * by default, we'll also correctly forward keys starting with {@link ActivitiConstants#WELL_KNOWN_SPRING_INTEGRATION_HEADER_PREFIX}
     */

    private boolean includeHeadersWithWellKnownPrefix = true;

    /**
     * all headers that we want to forward as process variables. None, by default, as headers may be rich objects where as process variables <em>should</em> be lightweight (primitives, for example)
     */
    private String[] headerToProcessVariableNames = new String[0];

    /**
     * all process variables that should be exposed as headers
     */
    private String[] processVariableToHeaderNames = new String[]{"*"};

    private String currentActivityExecutionCantBeNullErrorMessage =
            String.format("'currentActivityExecution' should be rebound on this instance before all " +
                    "uses of any methods on the HeaderMapper<T> interface. If the instance is null, " +
                    "certain well known headers (like '%s') that are critical to the successful use of much of " +
                    "the support provided by this module will not be available.", ActivitiConstants.WELL_KNOWN_EXECUTION_ID_HEADER_KEY);

    /**
     * @see ProcessEngine
     */
    private ProcessEngine processEngine;

    /**
     * the current {@link ActivityExecution} - should be rebound before each use.
     */
    private ActivityExecution currentActivityExecution;


    /**
     * the {@link RuntimeService} as obtained from a {@link org.activiti.engine.ProcessEngine#getRuntimeService()}
     */
    private RuntimeService runtimeService;

    /**
     * Most of the support provided by this module won't work if this
     * is set to false as it would mean the {@link ActivitiConstants#WELL_KNOWN_EXECUTION_ID_HEADER_KEY}
     * header (and any other headers derived by interrogating the {@link ActivityExecution} instance) would be null
     */
    private volatile boolean requiresActivityExecution = true;

    private Set<String> wellKnownActivitiHeaders = new HashSet<String>(
            Arrays.asList(
                    ActivitiConstants.WELL_KNOWN_ACTIVITY_ID_HEADER_KEY,
                    ActivitiConstants.WELL_KNOWN_EXECUTION_ID_HEADER_KEY,
                    ActivitiConstants.WELL_KNOWN_PROCESS_INSTANCE_ID_HEADER_KEY,
                    ActivitiConstants.WELL_KNOWN_PROCESS_DEFINITION_ID_HEADER_KEY,
                    ActivitiConstants.WELL_KNOWN_PROCESS_DEFINITION_NAME_HEADER_KEY
            ));

    public void setCurrentActivityExecution(ActivityExecution currentActivityExecution) {
        synchronized (this.activityExecutionMonitor) {
            this.currentActivityExecution = currentActivityExecution;
        }
    }

/*
    public DefaultProcessVariableHeaderMapper(DefaultProcessVariableHeaderMapper mapper, ActivityExecution ex) {
        setCurrentActivityExecution(ex);
        setProcessEngine( mapper.processEngine);
        this.runtimeService = this.processEngine.getRuntimeService() ;
        this.headerToProcessVariableNames = mapper.headerToProcessVariableNames;
        this.processVariableToHeaderNames = mapper.processVariableToHeaderNames;
        this.wellKnownActivitiHeaders = mapper.wellKnownActivitiHeaders ;
    }*/

    public DefaultProcessVariableHeaderMapper(ProcessEngine processEngine, ActivityExecution e) {
        setProcessEngine(processEngine);
        setCurrentActivityExecution(e);
    }

    public void fromHeaders(MessageHeaders headers, Map<String, Object> target) {

        validate();

        Assert.notNull(target, "the target can't be null");

        Map<String, Object> procVars = new HashMap<String, Object>();

        for (String messageHeaderKey : headers.keySet()) {
            if (shouldMapHeaderToProcessVariable(messageHeaderKey)) {
                String pvName = messageHeaderKey.startsWith(prefix) ? messageHeaderKey.substring(wellKnownHeaderPrefixLength) : messageHeaderKey;
                procVars.put(pvName, headers.get(messageHeaderKey));
            }
        }

        Map<String, ?> vars = procVars;


        for (String k : vars.keySet())
            target.put(k, vars.get(k));

    }


    /**
     * the variables coming in from a given {@link ActivityExecution} will be mapped out as Spring Integration message headers.
     *
     * @param processVariables the processVariables
     * @return a map of headers to send with the Spring Integration message
     */
    public Map<String, ?> toHeaders(Map<String, Object> processVariables) {

        validate();

        Map<String, Object> headers = new HashMap<String, Object>();

        for (String mhk : processVariables.keySet()){
            if (shouldMapProcessVariableToHeader(mhk)) {
                String hKey = this.shouldPrefixProcessVariables && StringUtils.hasText(this.prefix) ? this.prefix + mhk : mhk;
                headers.put(hKey, processVariables.get(mhk));
            }
        }

        synchronized (this.activityExecutionMonitor) {
            if (this.currentActivityExecution != null) {
                ProcessSupport.encodeCommonProcessVariableDataIntoMessage(this.currentActivityExecution, headers);
            }
        }

        return headers;
    }

    private boolean shouldMapHeaderToProcessVariable(String headerName) {
        Assert.isTrue(StringUtils.hasText(headerName), "the header must not be empty");

        // first test. it might just be a direct match with something that has the prefix
        if (this.includeHeadersWithWellKnownPrefix && headerName.startsWith(prefix))
            return true;

        if (this.wellKnownActivitiHeaders.contains(headerName))
            return true;

        // if this didnt work, then we scan to see if the headers match a fuzzy algorithm
        return matchesAny(this.headerToProcessVariableNames, headerName);
    }

    private boolean shouldMapProcessVariableToHeader(String procVarName) {

        Assert.notNull(StringUtils.hasText(procVarName), "the process variable must not be null");


        return matchesAny(this.processVariableToHeaderNames, procVarName);
    }

    public void setShouldPrefixProcessVariables(boolean shouldPrefixProcessVariables) {
        this.shouldPrefixProcessVariables = shouldPrefixProcessVariables;
    }

    public void afterPropertiesSet() throws Exception {

        // redundant but also ensures any side effects are up to date
        setPrefix(this.prefix);

        runtimeService = this.processEngine.getRuntimeService();
        validate();
    }

    private boolean matchesAny(String[] patterns, String candidate) {
        for (String pattern : patterns) {
            if (PatternMatchUtils.simpleMatch(pattern, candidate)) {
                return true;
            }
        }
        return false;
    }


    private void validate() {


        if (this.requiresActivityExecution) {
            Assert.notNull(this.currentActivityExecution, "the currentActivityExecution should be reset on this instance before all uses");
        } else {
            if (this.currentActivityExecution == null) {
                log.warn(currentActivityExecutionCantBeNullErrorMessage);
            }
        }
        Assert.notNull(this.runtimeService, "'runtimeService' can't be null");


    }

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


    public void setPrefix(String prefix) {
        this.prefix = prefix;
        this.wellKnownHeaderPrefixLength = this.prefix.length();
    }
}
