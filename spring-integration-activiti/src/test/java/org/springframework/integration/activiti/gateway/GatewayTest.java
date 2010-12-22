/* Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.integration.activiti.gateway;

import org.activiti.engine.test.Deployment;
import org.apache.commons.lang.time.StopWatch;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.After;
import org.springframework.integration.activiti.test.AbstractSpringIntegrationActivitiTestCase;
import org.springframework.test.context.ContextConfiguration;

import java.util.HashMap;
import java.util.Map;

/**
 * This component demonstrates creating a {@link org.springframework.integration.activiti.gateway.AsyncActivityBehaviorMessagingGateway} (factoried from Spring)
 * and exposed for use in a BPMN 2 process.
 *
 * @author Josh Long
 */
@ContextConfiguration(  "GatewayTest-context.xml")
//@RunWith(SpringJUnit4ClassRunner.class)
public class GatewayTest extends AbstractSpringIntegrationActivitiTestCase {

    private Log log = LogFactory.getLog(getClass());


    @Deployment(resources = "processes/si_gateway_example.bpmn20.xml")
    public void testGateway() throws Throwable {
        // setup
        processEngine.getRepositoryService().createDeployment().addClasspathResource("processes/si_gateway_example.bpmn20.xml").deploy();

        // launch a process
        Map<String, Object> vars = new HashMap<String, Object>();
        vars.put("customerId", 232);

        log.debug("about to start the business process");
        StopWatch sw = new StopWatch();
        sw.start();
        processEngine.getRuntimeService().startProcessInstanceByKey("sigatewayProcess", vars);

        sw.stop();
        log.debug("total time to run the process:" + sw.getTime());

          Thread.sleep(1000 * 5);
    }

  @After
  public void after() throws Exception {
    Thread.sleep( 1000 * 5);
  }
}
