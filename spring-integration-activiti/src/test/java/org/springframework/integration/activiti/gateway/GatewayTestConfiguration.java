package org.springframework.integration.activiti.gateway;

import org.activiti.spring.ProcessEngineFactoryBean;
import org.activiti.spring.SpringProcessEngineConfiguration;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.MessageChannel;
import org.springframework.integration.activiti.impls.PrintingServiceActivator;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.SimpleDriverDataSource;
import org.springframework.jdbc.datasource.TransactionAwareDataSourceProxy;

import javax.annotation.PostConstruct;
import javax.sql.DataSource;

@Configuration
public class GatewayTestConfiguration {

    private Log log = LogFactory.getLog(getClass());

    @Value("#{response}")
    private MessageChannel replies;

    @Value("#{request}")
    private MessageChannel requests;

    @Value("${db.url}")
    private String url;

    @Value("${db.password}")
    private String pw;

    @Value("${db.user}")
    private String user;

    @PostConstruct
    public void setup() {
        log.debug("starting up " + getClass().getName());
    }

    @Bean
    public PrintingServiceActivator serviceActivator() {
        return new PrintingServiceActivator();
    }

    @Bean
    public ProcessEngineFactoryBean processEngine() {
        ProcessEngineFactoryBean processEngineFactoryBean = new ProcessEngineFactoryBean();

        SpringProcessEngineConfiguration configuration = new SpringProcessEngineConfiguration();
        configuration.setTransactionManager(this.dataSourceTransactionManager());
        configuration.setDataSource(this.targetDataSource());
        configuration.setDatabaseSchemaUpdate(SpringProcessEngineConfiguration.DB_SCHEMA_UPDATE_DROP_CREATE);
        processEngineFactoryBean.setProcessEngineConfiguration(configuration);
        return processEngineFactoryBean;
    }

    @Bean
    public DataSource targetDataSource() {
        TransactionAwareDataSourceProxy transactionAwareDataSourceProxy = new TransactionAwareDataSourceProxy();
        SimpleDriverDataSource simpleDriverDataSource = new SimpleDriverDataSource();
        simpleDriverDataSource.setPassword(this.pw);
        simpleDriverDataSource.setUsername(this.user);
        simpleDriverDataSource.setUrl(this.url);
        simpleDriverDataSource.setDriverClass(org.h2.Driver.class);
        transactionAwareDataSourceProxy.setTargetDataSource(simpleDriverDataSource);
        return transactionAwareDataSourceProxy;

    }

    @Bean
    public DataSourceTransactionManager dataSourceTransactionManager() {
        DataSourceTransactionManager dataSourceTransactionManager = new DataSourceTransactionManager();
        dataSourceTransactionManager.setDataSource(this.targetDataSource());
        return dataSourceTransactionManager;
    }


    @Bean
    public AsyncActivityBehaviorMessagingGateway gateway() throws Exception {
        AsyncActivityBehaviorMessagingGateway gw = new AsyncActivityBehaviorMessagingGateway();
        gw.setForwardProcessVariablesAsMessageHeaders(true);
        gw.setProcessEngine(this.processEngine().getObject());
        gw.setUpdateProcessVariablesFromReplyMessageHeaders(true);
        gw.setRequestChannel(this.requests);
        gw.setReplyChannel(this.replies);
        return gw;
    }

}
