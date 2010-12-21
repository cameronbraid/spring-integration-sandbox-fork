package org.springframework.integration.activiti.gateway;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.MessageChannel;
import org.springframework.integration.activiti.CommonConfiguration;
import org.springframework.integration.activiti.utils.PrintingServiceActivator;

/**
 * Simple configuration for the gateway test
 *
 * @author Josh Long
 */
@Configuration
public class GatewayTestConfiguration extends CommonConfiguration{


    @Value("#{response}")
    private MessageChannel replies;

    @Value("#{request}")
    private MessageChannel requests;


    @Bean
    public PrintingServiceActivator serviceActivator() {
        return new PrintingServiceActivator();
    }

    @Bean
    public AsyncActivityBehaviorMessagingGateway gateway() throws Exception {
        AsyncActivityBehaviorMessagingGateway asyncActivityBehaviorMessagingGateway = new AsyncActivityBehaviorMessagingGateway();
        asyncActivityBehaviorMessagingGateway.setForwardProcessVariablesAsMessageHeaders(true);
        asyncActivityBehaviorMessagingGateway.setProcessEngine(this.processEngine().getObject());
        asyncActivityBehaviorMessagingGateway.setUpdateProcessVariablesFromReplyMessageHeaders(true);
        asyncActivityBehaviorMessagingGateway.setRequestChannel(this.requests);
        asyncActivityBehaviorMessagingGateway.setReplyChannel(this.replies);
        return asyncActivityBehaviorMessagingGateway;
    }

}
