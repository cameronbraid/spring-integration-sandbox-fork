package org.springframework.integration.strictordering.entitykey;

 
import org.springframework.expression.Expression;
import org.springframework.expression.spel.standard.SpelExpressionParser;
/**
 * 
 * @author David Turanski
 *
 */
public class SpelEntityKeyExtractor implements EntityKeyExtractor<Object, String>{
	 
	private volatile Expression payloadExpression;

	private final SpelExpressionParser parser = new SpelExpressionParser();
	
	
	@Override
	public String getKey(Object entity) {
		Object evaluationResult = entity;
		if (payloadExpression != null) {
			 evaluationResult =  payloadExpression.getValue(entity);
		}	 
		return (String) evaluationResult; 
	}
	
	/**
	 * 
	 * @param payloadExpression
	 */
	public void setPayloadExpression(String payloadExpression) {
		if (payloadExpression == null) {
			this.payloadExpression = null;
		}
		else {
			this.payloadExpression = this.parser.parseExpression(payloadExpression);
		}
	}
}
