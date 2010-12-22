package org.springframework.integration.activiti.mapping;

import org.activiti.engine.ProcessEngine;
import org.activiti.engine.RuntimeService;
import org.activiti.engine.impl.pvm.PvmActivity;
import org.activiti.engine.impl.pvm.delegate.ActivityExecution;
import org.activiti.engine.impl.pvm.process.ActivityImpl;
import org.activiti.engine.impl.runtime.ExecutionEntity;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.integration.MessageHeaders;
import org.springframework.integration.activiti.ActivitiConstants;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

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

    private String[] testKeys = new String[]{"crm*", "customerId"};


    @Test
    public void testMappingHeadersToProcessVariables() throws Throwable {

        processVariableHeaderMapper.setHeaderToProcessVariableNames(null);
        processVariableHeaderMapper.afterPropertiesSet();

        processVariableHeaderMapper.setHeaderToProcessVariableNames(this.testKeys);
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
        headersMap.put(ActivitiConstants.WELL_KNOWN_EXECUTION_ID_HEADER_KEY, "exe");


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

    private ActivityExecution execution() {
        ExecutionEntity ex = mock(ExecutionEntity.class);
        when(ex.getId()).thenReturn("execution.executionId");
        when(ex.getProcessDefinitionId()).thenReturn("execution.processDefinitionId");
        when(ex.getId()).thenReturn("execution.id");
        ActivityImpl pvmActivity = mock(ActivityImpl.class);

        when(pvmActivity.getId()).thenReturn("pvmActivity.id");
        when(ex.getActivity()).thenReturn(pvmActivity);

        return ex;
    }

    @Test
    public void testMappingProcVarsToHeadersWithExecution() throws Throwable {

        Map<String, Object> pvs = new HashMap<String, Object>();
        pvs.put("customerId", 22);
        pvs.put("age", 232);

        ActivityExecution ex = execution();
        this.processVariableHeaderMapper.setCurrentActivityExecution(ex);
        this.processVariableHeaderMapper.afterPropertiesSet();

        Map<String, ?> res = this.processVariableHeaderMapper.toHeaders(pvs);
        assertTrue("we should have the process variables " +
                "as well as the headers matching the " +
                ActivitiConstants.class.getName() + ".* constants",
                res.size() > pvs.size());
    }

    @Test
    public void testProcVarToHeaderWhitelist() throws Throwable {

        Map<String, Object> pvs = new HashMap<String, Object>();
        pvs.put("customerId", 22);
        pvs.put("age", 232);

        this.processVariableHeaderMapper.setRequiresActivityExecution(false);
        this.processVariableHeaderMapper.setCurrentActivityExecution(null);
        this.processVariableHeaderMapper.setProcessVariableToHeaderNames("age");
        this.processVariableHeaderMapper.afterPropertiesSet();

        Map<String, ?> rex = this.processVariableHeaderMapper.toHeaders(pvs);
        assertTrue(rex.size() == 1);
    }

    @Test
    public void testPrefixingOfMessageHeaders() throws Throwable {

        Map<String, Object> pvs = new HashMap<String, Object>();
        pvs.put("customerId", 22);
        pvs.put("age", 232);

        String pr = this.prefix;
        this.processVariableHeaderMapper.setPrefix(pr);
        this.processVariableHeaderMapper.setShouldPrefixProcessVariables(true);
        this.processVariableHeaderMapper.setRequiresActivityExecution(false);
        this.processVariableHeaderMapper.setCurrentActivityExecution(null);
        this.processVariableHeaderMapper.afterPropertiesSet();
        Map<String, ?> rex = this.processVariableHeaderMapper.toHeaders(pvs);
        assertTrue(rex.size() == pvs.size());
        for (String pvKey : pvs.keySet())
            assertTrue(rex.containsKey(prefix + pvKey));

        // now do the same thing with a null prefix
        processVariableHeaderMapper.setPrefix(null);
        processVariableHeaderMapper.afterPropertiesSet();

        rex = this.processVariableHeaderMapper.toHeaders(pvs);
        assertTrue(rex.size() == pvs.size());
        for (String pvKey : pvs.keySet())
            assertTrue(rex.containsKey(pvKey));
    }

    @Test
    public void testMappingProcVarsToHeadersWithoutExecution() throws Throwable {

        Map<String, Object> pvs = new HashMap<String, Object>();
        pvs.put("customerId", 22);
        pvs.put("age", 232);

        // can't do any more than respond to what was given since were not passing in an execution
        this.processVariableHeaderMapper.setProcessVariableToHeaderNames("*");
        this.processVariableHeaderMapper.setCurrentActivityExecution(null);
        this.processVariableHeaderMapper.setRequiresActivityExecution(false);
        this.processVariableHeaderMapper.afterPropertiesSet();

        Map<String, ?> res = this.processVariableHeaderMapper.toHeaders(pvs);
        assertEquals(res.size(), pvs.size());

    }

    @Test
    public void testMappingProcessVariablesToHeadersSetup() throws Throwable {

        processVariableHeaderMapper.setProcessVariableToHeaderNames(null);
        processVariableHeaderMapper.afterPropertiesSet();


        processVariableHeaderMapper.setProcessVariableToHeaderNames(this.testKeys);
        processVariableHeaderMapper.afterPropertiesSet();


        // pass in an execution


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
