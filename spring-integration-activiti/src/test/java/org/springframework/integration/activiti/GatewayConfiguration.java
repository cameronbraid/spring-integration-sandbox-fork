package org.springframework.integration.activiti;

import org.activiti.engine.DbSchemaStrategy;
import org.activiti.engine.impl.cfg.spring.ProcessEngineFactoryBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.TransactionAwareDataSourceProxy;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;


@Component
public class GatewayConfiguration {

    @Value("#{ds}")
   	private DataSource dataSource;

    @Bean
    public DataSource dataSource() {
        return new TransactionAwareDataSourceProxy(this.dataSource);
    }

    @Bean
    public PlatformTransactionManager platformTransactionManager() {
        return new DataSourceTransactionManager(this.dataSource());
    }

    @Bean
    public ProcessEngineFactoryBean processEngineFactoryBean() {
        ProcessEngineFactoryBean pe = new ProcessEngineFactoryBean();
        pe.setDataSource(this.dataSource());
        pe.setTransactionManager(this.platformTransactionManager());
        pe.setDbSchemaStrategy(DbSchemaStrategy.CREATE);
        return pe;
    }
}
