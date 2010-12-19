package org.springframework.integration.activiti.adapter;

import org.activiti.engine.ProcessEngine;
import org.springframework.integration.MessageChannel;
import org.springframework.integration.activiti.ExecutionSignallingMessageHandler;
import org.springframework.integration.config.ConsumerEndpointFactoryBean;
import org.springframework.integration.core.PollableChannel;
import org.springframework.integration.endpoint.AbstractEndpoint;
import org.springframework.integration.scheduling.PollerMetadata;
import org.springframework.scheduling.support.PeriodicTrigger;
import org.springframework.util.Assert;

/**
 * this signals execution for a wait-state when an inbound message arrives. it makes no assumptions about how a wait-state was entered.
 *
 * @author Josh Long
 * @since 2.1
 */
public class ExecutionSignallingInboundChannelAdapter extends AbstractEndpoint {

    private MessageChannel channel;

    private ProcessEngine processEngine;

    private boolean updateProcessVariablesFromReplyMessageHeaders;

    private AbstractEndpoint endpoint;

    private ExecutionSignallingMessageHandler executionSignallingMessageHandler = new ExecutionSignallingMessageHandler();

    public void setChannel(MessageChannel channel) {
        this.channel = channel;
    }

    /**
     * @param processEngine shared instance of the process engine
     * @see org.activiti.spring.ProcessEngineFactoryBean
     */
    public void setProcessEngine(ProcessEngine processEngine) {
        this.processEngine = processEngine;
    }

    /**
     * @param updateProcessVariablesFromReplyMessageHeaders
     *         whether or not we should update the business process with headers from the received {@link org.springframework.integration.Message}s
     */
    public void setUpdateProcessVariablesFromReplyMessageHeaders(boolean updateProcessVariablesFromReplyMessageHeaders) {
        this.updateProcessVariablesFromReplyMessageHeaders = updateProcessVariablesFromReplyMessageHeaders;
    }


    @Override
    protected void onInit() throws Exception {
        Assert.notNull(this.processEngine, "processEngine can't be null");
        Assert.notNull(this.channel, "'channel' can't be null");

        executionSignallingMessageHandler.setProcessEngine(this.processEngine);
        executionSignallingMessageHandler.setUpdateProcessVariablesFromReplyMessageHeaders(this.updateProcessVariablesFromReplyMessageHeaders);
        executionSignallingMessageHandler.afterPropertiesSet();

        ConsumerEndpointFactoryBean consumerEndpointFactoryBean = new ConsumerEndpointFactoryBean();
        consumerEndpointFactoryBean.setAutoStartup(false);
        consumerEndpointFactoryBean.setBeanFactory(this.getBeanFactory());
        consumerEndpointFactoryBean.setHandler(this.executionSignallingMessageHandler);
        consumerEndpointFactoryBean.setInputChannel(this.channel);
        consumerEndpointFactoryBean.setBeanName(this.getComponentName());

        if (this.channel instanceof PollableChannel) {
            PollerMetadata pollerMetadata = new PollerMetadata();
            pollerMetadata.setReceiveTimeout(-1);
            pollerMetadata.setTrigger(new PeriodicTrigger(10));
            consumerEndpointFactoryBean.setPollerMetadata(pollerMetadata);
        }

        this.endpoint = consumerEndpointFactoryBean.getObject();

    }

    @Override
    protected void doStart() {
        if (this.endpoint != null) {
            this.endpoint.start();
            logger.debug("endpoint.start() -- started " + this.getClass() + " instance");
        }
    }

    @Override
    protected void doStop() {
        if (this.endpoint != null) {
            this.endpoint.stop();
            logger.debug("endpoint.stop() -- stopped " + this.getClass() + " instance");
        }
    }
}
