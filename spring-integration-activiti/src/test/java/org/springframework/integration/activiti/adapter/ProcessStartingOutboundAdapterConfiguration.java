package org.springframework.integration.activiti.adapter;

import org.activiti.engine.ProcessEngine;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.activiti.CommonConfiguration;
import org.springframework.integration.activiti.mapping.DefaultProcessVariableHeaderMapper;

@Configuration
@SuppressWarnings("unused")
public class ProcessStartingOutboundAdapterConfiguration extends CommonConfiguration {

  @Bean
  public DefaultProcessVariableHeaderMapper headerMapper() throws Throwable {
    ProcessEngine e = this.processEngine().getObject();
    DefaultProcessVariableHeaderMapper hm = new DefaultProcessVariableHeaderMapper(e);
    hm.setRequiresActivityExecution(false);
    return hm;
  }

  @Bean
  public ProcessStartingOutboundChannelAdapter processStartingOutboundChannelAdapter() throws Throwable {
    ProcessStartingOutboundChannelAdapter processStartingOutboundChannelAdapter = new ProcessStartingOutboundChannelAdapter();
    processStartingOutboundChannelAdapter.setProcessEngine(this.processEngine().getObject());
    processStartingOutboundChannelAdapter.setDefaultProcessVariableHeaderMapper(headerMapper());
    return processStartingOutboundChannelAdapter;
  }
}
