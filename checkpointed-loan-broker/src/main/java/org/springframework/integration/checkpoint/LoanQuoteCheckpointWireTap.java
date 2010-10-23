package org.springframework.integration.checkpoint;

import java.util.Properties;

import org.springframework.integration.Message;
import org.springframework.integration.MessageChannel;
import org.springframework.integration.samples.loanbroker.domain.LoanQuote;
/**
 * A custom implementation of {@link AbstractCheckpointWireTap} that generates {@link LoanQuote} properties
 * @author David Turanski
 *
 */
public class LoanQuoteCheckpointWireTap extends AbstractCheckpointWireTap {

	public LoanQuoteCheckpointWireTap(MessageChannel channel) {
		super(channel);
	}

	@Override
	protected Properties addCheckpointProperties(Message<?> message) {
		Properties businessMetadata = null;
		if (message.getPayload() instanceof LoanQuote){
			LoanQuote loanQuote = (LoanQuote) message.getPayload();
			businessMetadata = new Properties();
			businessMetadata.put("loanAmount",loanQuote.getAmount());
			businessMetadata.put("lender",loanQuote.getLender());
			businessMetadata.put("rate",loanQuote.getRate());
		}  
		return businessMetadata; 	 
	}

}
