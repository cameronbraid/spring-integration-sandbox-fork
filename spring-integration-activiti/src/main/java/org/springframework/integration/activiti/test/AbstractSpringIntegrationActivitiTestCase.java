package org.springframework.integration.activiti.test;

import org.activiti.engine.impl.ProcessEngineImpl;
import org.activiti.engine.impl.db.DbSqlSession;
import org.activiti.engine.impl.interceptor.Command;
import org.activiti.engine.impl.interceptor.CommandContext;
import org.activiti.engine.impl.interceptor.CommandExecutor;
import org.activiti.spring.impl.test.SpringActivitiTestCase;

public class AbstractSpringIntegrationActivitiTestCase extends SpringActivitiTestCase {
  protected void assertAndEnsureCleanDb() throws Throwable {
    CommandExecutor commandExecutor = ((ProcessEngineImpl) processEngine).getProcessEngineConfiguration().getCommandExecutorTxRequired();
    commandExecutor.execute(new Command<Object>() {
      public Object execute(CommandContext commandContext) {
        DbSqlSession session = commandContext.getSession(DbSqlSession.class);
        session.dbSchemaDrop();
        session.dbSchemaCreate();
        return null;
      }
    });

  //  super.assertAndEnsureCleanDb();
  }/* private
    Log log = LogFactory.getLog(getClass());

     protected void assertAndEnsureCleanDb() throws Throwable {
      log.fine("verifying that db is clean after test");
      Map<String, Long> tableCounts = managementService.getTableCount();
      StringBuilder outputMessage = new StringBuilder();
      for (String tableName : tableCounts.keySet()) {
        if (!TABLENAMES_EXCLUDED_FROM_DB_CLEAN_CHECK.contains(tableName)) {
          Long count = tableCounts.get(tableName);
          if (count!=0L) {
            outputMessage.append("  "+tableName + ": " + count + " record(s) ");
          }
        }
      }
      if (outputMessage.length() > 0) {
        outputMessage.insert(0, "DB NOT CLEAN: \n");
        log.severe(EMPTY_LINE);
        log.severe(outputMessage.toString());

        log.info("dropping and recreating db");

        CommandExecutor commandExecutor = ((ProcessEngineImpl)processEngine).getProcessEngineConfiguration().getCommandExecutorTxRequired();
        commandExecutor.execute(new Command<Object>() {
          public Object execute(CommandContext commandContext) {
            DbSqlSession session = commandContext.getSession(DbSqlSession.class);
            session.dbSchemaDrop();
            session.dbSchemaCreate();
            return null;
          }
        });

        if (exception!=null) {
          throw exception;
        } else {
          Assert.fail(outputMessage.toString());
        }
      } else {
        log.info("database was clean");
      }
    }*/
}
