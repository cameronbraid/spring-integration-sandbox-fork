package org.springframework.integration.activiti;

import org.activiti.spring.ProcessEngineFactoryBean;
import org.activiti.spring.SpringProcessEngineConfiguration;
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
		SpringProcessEngineConfiguration processEngineConfiguration = new SpringProcessEngineConfiguration();

		processEngineConfiguration.setDataSource(this.dataSource());
		processEngineConfiguration.setTransactionManager(this.platformTransactionManager());
		processEngineConfiguration.setDatabaseSchemaUpdate(SpringProcessEngineConfiguration.DB_SCHEMA_UPDATE_TRUE);
		//pe.setTransactionManager(this.platformTransactionManager());
		//pe.setDbSchemaStrategy(DbSchemaStrategy.CREATE);
		// pe.setDataSource(this.dataSource());


		pe.setProcessEngineConfiguration(processEngineConfiguration);


		return pe;
	}
}
