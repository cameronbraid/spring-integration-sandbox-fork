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

package org.springframework.integration.activiti.adapter;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.integration.Message;
import org.springframework.integration.MessageChannel;
import org.springframework.integration.activiti.ActivitiConstants;
import org.springframework.integration.core.MessagingTemplate;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@ContextConfiguration(locations = "ProcessStartingOutboundChannelAdapterTest-context.xml")
@RunWith(SpringJUnit4ClassRunner.class)
public class ProcessStartingOutboundAdapterTest {

    @Value("#{triggerChannel}")
    private MessageChannel messageChannel;

    private MessagingTemplate messagingTemplate = new MessagingTemplate();

    @Test
    public void testOutboundAdapter() throws Throwable {
        Message<?> msg = MessageBuilder.withPayload("hello, from " + System.currentTimeMillis())
                .setHeader(ActivitiConstants.WELL_KNOWN_PROCESS_DEFINITION_NAME_HEADER_KEY + "customerId", 2324)
                .build();
        messagingTemplate.send(this.messageChannel, msg);
        //Thread.sleep(1000 * 10);
    }
}
