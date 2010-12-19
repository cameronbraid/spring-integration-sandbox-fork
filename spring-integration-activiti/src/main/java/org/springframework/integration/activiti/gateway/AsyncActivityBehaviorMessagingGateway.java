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
package org.springframework.integration.activiti.gateway;

import org.activiti.engine.ProcessEngine;
import org.activiti.engine.RuntimeService;
import org.activiti.engine.impl.bpmn.ReceiveTaskActivity;
import org.activiti.engine.impl.pvm.PvmActivity;
import org.activiti.engine.impl.pvm.delegate.ActivityBehavior;
import org.activiti.engine.impl.pvm.delegate.ActivityExecution;
import org.activiti.engine.impl.runtime.ExecutionEntity;
import org.activiti.engine.runtime.Execution;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.BeanNameAware;
import org.springframework.beans.factory.InitializingBean;

import org.springframework.integration.*;
import org.springframework.integration.activiti.ActivitiConstants;
import org.springframework.integration.activiti.ProcessSupport;
import org.springframework.integration.activiti.adapter.ExecutionSignallingInboundChannelAdapter;
import org.springframework.integration.config.ConsumerEndpointFactoryBean;
import org.springframework.integration.core.MessageHandler;
import org.springframework.integration.core.MessagingTemplate;
import org.springframework.integration.core.PollableChannel;
import org.springframework.integration.endpoint.AbstractEndpoint;
import org.springframework.integration.scheduling.PollerMetadata;
import org.springframework.integration.support.MessageBuilder;

import org.springframework.scheduling.support.PeriodicTrigger;

import org.springframework.util.Assert;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;


/**
 * This class is plugged into an Activiti workflow. <serviceTask /> let's us plugin a custom {@link  ActivityBehavior}.
 * We need to build an {@link  ActivityBehavior} that can send and receive the message,
 * propagating the {@code executionId} and potentially process variables/ header variables.
 * <p/>
 * Simply <code>svn co http://svn.codehaus.org/activiti/activiti/branches/alpha4-spring-integration-adapter</code> that repository and then <code>mvn clean install </code> it.
 * <p/>
 * The class forwards ("pushes") control from a BPM process (in-thread) to a Spring Integration channel where, of course, Spring Integration acts as a client  and
 * can push the execution forward anyway it wants to.
 * <p/>
 * Possible use cases include forwarding the job through an outbound JMS adapter or gateway, forwarding the job through an XMPP adapter, or forwarding the job through
 * to an outbound email adapter.
 * <p/>
 * <p/>
 * The only requirement for the reply is that the {@link org.springframework.integration.Message} arrive on the #replyChannel and that it contain a header of
 * {@link org.springframework.integration.activiti.ActivitiConstants#WELL_KNOWN_EXECUTION_ID_HEADER_KEY} (which the outbound {@link org.springframework.integration.Message} will have)
 * so that the Activiti runtime can signal that execution has completed successfully.
 * <p/>
 * Thanks to Dave Syer and Tom Baeyens for the help brainstorming.
 *
 * @author Josh Long
 * @see ReceiveTaskActivity  the {@link ActivityBehavior} impl that ships w/ Activiti that has the machinery to wake up when signaled
 * @see ProcessEngine the process engine instance is required to be able to use this namespace
 * @see org.activiti.spring.ProcessEngineFactoryBean - use this class to create the aforementioned ProcessEngine instance!
 */
public class AsyncActivityBehaviorMessagingGateway extends ReceiveTaskActivity implements BeanFactoryAware, BeanNameAware, ActivityBehavior, InitializingBean {


    private Log log = LogFactory.getLog(getClass());

    /**
     * Used to handle sending in a standard way
     */
    private MessagingTemplate messagingTemplate = new MessagingTemplate();

    /**
     * This is the channel on which we expect requests - {@link  Execution}s from Activiti - to arrive
     */
    private volatile MessageChannel requestChannel;

    /**
     * This is the channel on which we expect to send replies - ie, the result of our work in
     * Spring Integration - back to Activiti, which should be waiting for the results
     */
    private volatile MessageChannel replyChannel;

    /**
     * Injected from Spring or some other mechanism. Recommended approach is through a {@link  org.activiti.spring.ProcessEngineFactoryBean}
     */
    private volatile ProcessEngine processEngine;

    /**
     * Should we update the process variables based on the reply {@link org.springframework.integration.Message}'s {@link org.springframework.integration.MessageHeaders}?
     */
    private volatile boolean updateProcessVariablesFromReplyMessageHeaders  ;

    /**
     * Should we pass the workflow process variables as message headers when we send a message into the Spring Integration framework?
     */
    private volatile boolean forwardProcessVariablesAsMessageHeaders  ;


    /**
     * A reference to the {@link org.springframework.beans.factory.BeanFactory} that's hosting this component. Spring will inject this reference automatically assuming
     * this object is hosted in a Spring context.
     */
    private volatile BeanFactory beanFactory;

    /**
     * this handles receiving a message and signaling execution in the process
     */
    private ExecutionSignallingInboundChannelAdapter executionSignallingInboundChannelAdapter;

    /**
     * The process engine instance that controls the Activiti PVM. Recommended creation is through {@link org.activiti.spring.ProcessEngineFactoryBean}
     */
    private RuntimeService processService;

    /**
     * {@link BeanNameAware#setBeanName(String)}
     */
    private String beanName;

    @SuppressWarnings("unused")
    public void setRequestChannel(MessageChannel requestChannel) {
        this.requestChannel = requestChannel;
    }

    @SuppressWarnings("unused")
    public void setReplyChannel(MessageChannel replyChannel) {
        this.replyChannel = replyChannel;
    }

    @SuppressWarnings("unused")
    public void setProcessEngine(ProcessEngine processEngine) {
        this.processEngine = processEngine;
    }

    @SuppressWarnings("unused")
    public void setForwardProcessVariablesAsMessageHeaders(boolean forwardProcessVariablesAsMessageHeaders) {
        this.forwardProcessVariablesAsMessageHeaders = forwardProcessVariablesAsMessageHeaders;
    }

    @SuppressWarnings("unused")
    public void setUpdateProcessVariablesFromReplyMessageHeaders(boolean updateProcessVariablesFromReplyMessageHeaders) {
        this.updateProcessVariablesFromReplyMessageHeaders = updateProcessVariablesFromReplyMessageHeaders;
    }

    public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
        this.beanFactory = beanFactory;
    }

    @Override
    public void signal(ActivityExecution execution, String signalName, Object data)  throws Exception {
        leave(execution);
    }

    /**
     * Provides an opportunity for subclasses to provide extra headers to the outbound message
     *
     * @param activityExecution of the current process as it was received when we entered this {@link ActivityBehavior} instance
     */
    @SuppressWarnings("unused")
    protected Map<String, Object> contributeHeadersForOutboundMessage(ActivityExecution activityExecution) throws Exception {
        return new HashMap<String, Object>();
    }

    /**
     * This is the main interface method from {@link ActivityBehavior}. It will be called when the BPMN process executes the node referencing this logic.
     *
     * @param execution the {@link  ActivityExecution} as given to use by the engine
     * @throws Exception
     */
    public void execute(ActivityExecution execution) throws Exception {
        PvmActivity pvmActivity = execution.getActivity();

        ExecutionEntity executionEntity = (ExecutionEntity) execution;
        String procDefId = executionEntity.getProcessDefinitionId();
        String procInstanceId = executionEntity.getProcessInstanceId();

        String executionId = execution.getId();
        String pvmActivityId = pvmActivity.getId();

        MessageBuilder<?> messageBuilder = MessageBuilder.withPayload(execution)
                .setHeader(ActivitiConstants.WELL_KNOWN_EXECUTION_ID_HEADER_KEY, executionId)
                .setHeader(ActivitiConstants.WELL_KNOWN_ACTIVITY_ID_HEADER_KEY, pvmActivityId)
                .setHeader(ActivitiConstants.WELL_KNOWN_PROCESS_DEFINITION_ID_HEADER_KEY, procDefId)
                .setHeader(ActivitiConstants.WELL_KNOWN_PROCESS_INSTANCE_ID_HEADER_KEY, procInstanceId).setCorrelationId(executionId)
                .copyHeadersIfAbsent(this.contributeHeadersForOutboundMessage(execution));

        if (this.forwardProcessVariablesAsMessageHeaders) {
            Map<String, Object> variables = execution.getVariables();

            if ((variables != null) && (variables.size() > 0)) {
                messageBuilder = messageBuilder.copyHeadersIfAbsent(variables);
            }
        }

        Message<?> msg = messageBuilder.setReplyChannel(replyChannel).build();

        this.messagingTemplate.send(this.requestChannel, msg);

        super.execute(execution);
    }

    /**
     * Verify the presence of references to a request and reply {@link  org.springframework.integration.MessageChannel},
     * the {@link  ProcessEngine}, and setup the {@link org.springframework.integration.core.MessageHandler} that handles the replies
     *
     * @throws Exception
     */
    public void afterPropertiesSet() throws Exception {
        Assert.state(this.replyChannel != null, "'replyChannel' can't be null!");
        Assert.state(this.requestChannel != null, "'requestChannel' can't be null!");
        Assert.state(this.processEngine != null, "'processEngine' can't be null!");

        processService = this.processEngine.getRuntimeService();
        Assert.notNull(this.processService,"'processService' can't be null");

        ExecutionSignallingInboundChannelAdapter eai= new ExecutionSignallingInboundChannelAdapter() ;
        eai.setChannel(this.replyChannel);
        eai.setProcessEngine(this.processEngine);
        eai.setUpdateProcessVariablesFromReplyMessageHeaders(this.updateProcessVariablesFromReplyMessageHeaders);
        eai.setAutoStartup(false );
        eai.setBeanFactory(this.beanFactory);
        eai.setBeanName(this.beanName);
        eai.afterPropertiesSet();

        executionSignallingInboundChannelAdapter = eai;
        executionSignallingInboundChannelAdapter.start();
    }

    public void setBeanName(String s) {
        this.beanName = s;
    }
}
