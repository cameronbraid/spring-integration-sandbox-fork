package org.springframework.integration.activiti.signup;

import org.activiti.engine.impl.bpmn.BpmnActivityBehavior;
import org.activiti.engine.impl.pvm.delegate.ActivityBehavior;
import org.activiti.engine.impl.pvm.delegate.ActivityExecution;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.stereotype.Component;

@Component
public class CheckForm extends BpmnActivityBehavior implements ActivityBehavior {
    private Log log = LogFactory.getLog( CheckForm.class);

	public void execute(ActivityExecution activityExecution) throws Exception {

        log.debug(getClass() + " : checking form: ");

		boolean formOK = Math.random() > .7;

		activityExecution.setVariable("formOK", formOK);
		log.debug( getClass() + " : form OK? " + formOK);

//		performDefaultOutgoingBehavior(activityExecution);

	}
}


