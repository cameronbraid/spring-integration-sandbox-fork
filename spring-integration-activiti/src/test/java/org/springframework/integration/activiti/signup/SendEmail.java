package org.springframework.integration.activiti.signup;

import org.activiti.engine.impl.bpmn.BpmnActivityBehavior;
import org.activiti.engine.impl.pvm.delegate.ActivityBehavior;
import org.activiti.engine.impl.pvm.delegate.ActivityExecution;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.stereotype.Component;


@Component
public class SendEmail extends BpmnActivityBehavior implements ActivityBehavior {

    private Log log  = LogFactory.getLog( getClass());

	public void execute(ActivityExecution activityExecution) throws Exception {
		log.debug(getClass() + ": sending email ");

	//	this.performDefaultOutgoingBehavior(activityExecution);
	}
}
