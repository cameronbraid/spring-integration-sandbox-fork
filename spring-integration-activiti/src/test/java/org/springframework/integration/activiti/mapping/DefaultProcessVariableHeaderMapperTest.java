package org.springframework.integration.activiti.mapping;

import org.activiti.engine.ProcessEngine;
import org.activiti.engine.RuntimeService;
import org.activiti.engine.impl.pvm.delegate.ActivityExecution;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.integration.MessageHeaders;
import org.springframework.integration.activiti.ActivitiConstants;

import static org.junit.Assert.*;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.only;
import static org.mockito.Mockito.when;

/**
 * tests the viability of the {@link org.springframework.integration.mapping.HeaderMapper} implementation, {@link DefaultProcessVariableHeaderMapper}.
 *
 * @author Josh Long
 * @since 5.1
 */
public class DefaultProcessVariableHeaderMapperTest {

    private String prefix = "theprefix";
    private DefaultProcessVariableHeaderMapper processVariableHeaderMapper;
    private ProcessEngine processEngine;
    private ActivityExecution activityExecution;
    private RuntimeService runtimeService;

    @Before
    public void begin() throws Throwable {
        this.processEngine = mock(ProcessEngine.class);
        this.runtimeService = mock(RuntimeService.class);
        when(this.processEngine.getRuntimeService()).thenReturn(this.runtimeService);
        activityExecution = mock(ActivityExecution.class);
        processVariableHeaderMapper = new DefaultProcessVariableHeaderMapper(this.processEngine, this.activityExecution);
        processVariableHeaderMapper.setPrefix(this.prefix);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testRequiresActivityExecutionWorks() throws Throwable {
        processVariableHeaderMapper.setRequiresActivityExecution(true);
        processVariableHeaderMapper.setCurrentActivityExecution(null);
        processVariableHeaderMapper.afterPropertiesSet();

    }

    private String [] testKeys = new String[]{"crm*", "customerId"};

    @Test
    public void testMappingHeadersToProcessVariables() throws Throwable {

        processVariableHeaderMapper.setHeaderToProcessVariableNames(null);
        processVariableHeaderMapper.afterPropertiesSet();

        processVariableHeaderMapper.setHeaderToProcessVariableNames( this.testKeys );
        processVariableHeaderMapper.afterPropertiesSet();

        Map<String, Object> vars = new HashMap<String, Object>();

        String prefixKey = prefix + "name";
        String food = "food";

        Map<String, Object> headersMap = new HashMap<String, Object>();
        headersMap.put("crmData", new Date());
        headersMap.put("customerId", 232);
        headersMap.put("crmId", 232);
        headersMap.put(prefixKey, "Spring");
        headersMap.put(food, "donuts");
        headersMap.put(ActivitiConstants.WELL_KNOWN_EXECUTION_ID_HEADER_KEY , "exe");


        MessageHeaders headers = new MessageHeaders(headersMap);

        processVariableHeaderMapper.setIncludeHeadersWithWellKnownPrefix(true);
        processVariableHeaderMapper.afterPropertiesSet();
        processVariableHeaderMapper.fromHeaders(headers, vars);
        assertTrue("all keys except for 'food' should be in the headers.", !vars.containsKey(food) && vars.size() == (headersMap.size() - 1));

        vars.clear();
        processVariableHeaderMapper.setIncludeHeadersWithWellKnownPrefix(false);
        processVariableHeaderMapper.afterPropertiesSet();
        processVariableHeaderMapper.fromHeaders(headers, vars);
        assertTrue("all keys except for 'food' should be in the headers.",
                !vars.containsKey(food) && !vars.containsKey(prefixKey) && vars.size() == (headersMap.size() - 2));

    }

    @Test
    public void testMappingProcessVariablesToHeaders() throws Throwable {

        processVariableHeaderMapper.setProcessVariableToHeaderNames(null);
        processVariableHeaderMapper.afterPropertiesSet() ;


        processVariableHeaderMapper.setProcessVariableToHeaderNames(this.testKeys );
        processVariableHeaderMapper.afterPropertiesSet() ;





    }

    @Test
    public void testActivityExecutionStates() throws Throwable {

        processVariableHeaderMapper.setRequiresActivityExecution(false);
        processVariableHeaderMapper.afterPropertiesSet();

        processVariableHeaderMapper.setCurrentActivityExecution(null);
        processVariableHeaderMapper.afterPropertiesSet();

        processVariableHeaderMapper.setCurrentActivityExecution(activityExecution);
        processVariableHeaderMapper.setRequiresActivityExecution(true);
        processVariableHeaderMapper.afterPropertiesSet();

    }

    @Test
    public void testNormalCtor() throws Throwable {

    }

    @Test
    public void testToProcVars() throws Throwable {

    }

}
