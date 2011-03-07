package org.springframework.integration.activiti.gateway;

import org.activiti.engine.ProcessEngine;
import org.activiti.engine.RuntimeService;
import org.activiti.engine.impl.bpmn.behavior.ReceiveTaskActivityBehavior;
import org.activiti.engine.impl.pvm.delegate.ActivityBehavior;
import org.activiti.engine.impl.pvm.delegate.ActivityExecution;
import org.activiti.engine.runtime.Execution;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.BeanNameAware;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.integration.MessageChannel;
import org.springframework.integration.activiti.mapping.DefaultProcessVariableHeaderMapper;
import org.springframework.integration.activiti.mapping.ProcessVariableHeaderMapper;
import org.springframework.integration.activiti.util.ActivityExecutionFactoryBean;
import org.springframework.integration.core.MessagingTemplate;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.util.Assert;

import java.util.HashMap;
import java.util.Map;

/**
 * This class is plugged into an Activiti workflow. <serviceTask /> let's us plugin a custom {@link	ActivityBehavior}.
 * We need to build an {@link	ActivityBehavior} that can send and receive the message,
 * propagating the {@code executionId} and potentially process variables/ header variables.
 * <p/>
 * Simply <code>svn co http://svn.codehaus.org/activiti/activiti/branches/alpha4-spring-integration-adapter</code> that repository and then <code>mvn clean install </code> it.
 * <p/>
 * The class forwards ("pushes") control from a BPM process (in-thread) to a Spring Integration channel where, of course, Spring Integration acts as a client  and
 * can push the execution forward anyway it wants to.
 * <p/>
 * Possible use cases include forwarding the job through an adapter JMS adapter or asyncGateway, forwarding the job through an XMPP adapter, or forwarding the job through
 * to an adapter email adapter.
 * <p/>
 * The only requirement for the reply is that the {@link org.springframework.integration.Message} arrive on the #replyChannel and that it contain a header of
 * {@link org.springframework.integration.activiti.ActivitiConstants#WELL_KNOWN_EXECUTION_ID_HEADER_KEY} (which the adapter {@link org.springframework.integration.Message} will have)
 * so that the Activiti runtime can signalProcessExecution that execution has completed successfully.
 * <p/>
 * Thanks to Dave Syer and Tom Baeyens for the help brainstorming.
 *
 * @author Josh Long
 * @see ReceiveTaskActivityBehavior	the {@link ActivityBehavior} impl that ships w/ Activiti that has the machinery to wake up when signaled
 * @see ProcessEngine the process engine instance is required to be able to use this namespace
 * @see org.activiti.spring.ProcessEngineFactoryBean - use this class to create the aforementioned ProcessEngine instance!
 */
public abstract class AbstractActivityBehaviorMessagingGateway extends ReceiveTaskActivityBehavior implements BeanFactoryAware, BeanNameAware, ActivityBehavior, InitializingBean {

	protected ProcessVariableHeaderMapper headerMapper;

	protected Log log = LogFactory.getLog(getClass());

	/**
	 * Used to handle sending in a standard way
	 */
	protected MessagingTemplate messagingTemplate = new MessagingTemplate();

	/**
	 * This is the channel on which we expect requests - {@link	Execution}s from Activiti - to arrive
	 */
	protected volatile MessageChannel requestChannel;

	/**
	 * This is the channel on which we expect to send replies - ie, the result of our work in
	 * Spring Integration - back to Activiti, which should be waiting for the results
	 */
	protected volatile MessageChannel replyChannel;

	/**
	 * Injected from Spring or some other mechanism. Recommended approach is through a {@link	org.activiti.spring.ProcessEngineFactoryBean}
	 */
	protected volatile ProcessEngine processEngine;

	/**
	 * Should we update the process variables based on the reply {@link org.springframework.integration.Message}'s {@link org.springframework.integration.MessageHeaders}?
	 */
	protected volatile boolean updateProcessVariablesFromReplyMessageHeaders;

	/**
	 * Should we pass the workflow process variables as message headers when we send a message into the Spring Integration framework?
	 */
	protected volatile boolean forwardProcessVariablesAsMessageHeaders;

	/**
	 * A reference to the {@link org.springframework.beans.factory.BeanFactory} that's hosting this component. Spring will inject this reference automatically assuming
	 * this object is hosted in a Spring context.
	 */
	protected volatile BeanFactory beanFactory;

	/**
	 * The process engine instance that controls the Activiti PVM. Recommended creation is through {@link org.activiti.spring.ProcessEngineFactoryBean}
	 */
	protected RuntimeService processService;

	/**
	 * {@link BeanNameAware#setBeanName(String)}
	 */
	protected String beanName;

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
	public void signal(ActivityExecution execution, String signalName, Object data) throws Exception {
		leave(execution);
	}

	/**
	 * Provides an opportunity for subclasses to provide extra headers to the adapter message
	 *
	 * @param activityExecution of the current process as it was received when we entered this {@link ActivityBehavior} instance
	 */
	@SuppressWarnings("unused")
	protected Map<String, Object> contributeHeadersForOutboundMessage(ActivityExecution activityExecution) throws Exception {
		return new HashMap<String, Object>();
	}

	private ActivityExecution activityExecution;

	/**
	 * implementations should use this to handle sending the final message
	 *
	 * @param ex ActivityExecution instance as passed in from {@link ReceiveTaskActivityBehavior#execute(org.activiti.engine.impl.pvm.delegate.ActivityExecution)}
	 */
	protected abstract void onExecute(ActivityExecution ex) throws Exception;

	/**
	 * hook for setup {@link #afterPropertiesSet()} logic after we've setup state
	 *
	 * @throws Exception
	 */

	protected abstract void onInit() throws Exception;

	/**
	 * This is the main interface method from {@link ActivityBehavior}. It will be called when the BPMN process executes the node referencing this logic.
	 *
	 * @param execution the {@link	ActivityExecution} as given to use by the engine
	 * @throws Exception
	 */
	public void execute(ActivityExecution execution) throws Exception {
		onExecute(execution);
	}

	/**
	 * Verify the presence of references to a request and reply {@link	org.springframework.integration.MessageChannel},
	 * the {@link	ProcessEngine}, and setup the {@link org.springframework.integration.core.MessageHandler} that handles the replies
	 *
	 * @throws Exception
	 */
	public void afterPropertiesSet() throws Exception {
		Assert.state(this.replyChannel != null, "'replyChannel' can't be null!");
		Assert.state(this.requestChannel != null, "'requestChannel' can't be null!");
		Assert.state(this.processEngine != null, "'processEngine' can't be null!");

		processService = this.processEngine.getRuntimeService();

		if (this.headerMapper == null) {
			// provides a thread safe proxy
			ActivityExecutionFactoryBean activityExecutionFactoryBean = new ActivityExecutionFactoryBean();
			activityExecutionFactoryBean.afterPropertiesSet();
			ActivityExecution execution = activityExecutionFactoryBean.getObject();

			this.headerMapper = new DefaultProcessVariableHeaderMapper(this.processEngine, execution);
		}

		Assert.notNull(this.headerMapper, "the 'headerMapper' can't be null") ;
		Assert.notNull(this.processService, "'processService' can't be null");
		onInit();
	}

	public void setBeanName(String s) {
		this.beanName = s;
	}

	protected MessageBuilder<?> doBasicOutboundMessageConstruction(ActivityExecution execution) throws Exception {

		Map<String, ?> headers = headerMapper.toHeaders(execution.getVariables());

		MessageBuilder<?> messageBuilder = MessageBuilder.withPayload(execution)
				.copyHeadersIfAbsent(this.contributeHeadersForOutboundMessage(execution))
						//   .setReplyChannel(replyChannel)
				.copyHeaders(headers);
		return messageBuilder;
	}
}
