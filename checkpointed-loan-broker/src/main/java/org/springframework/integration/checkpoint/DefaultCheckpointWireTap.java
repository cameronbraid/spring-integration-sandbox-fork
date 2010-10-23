package org.springframework.integration.checkpoint;

import java.util.Properties;

import org.springframework.integration.Message;
import org.springframework.integration.MessageChannel;
/**
 * Default implementation of {@link AbstractCheckpointWireTap} that generates a generic {@link Checkpoint} with 
 * no additional properties
 * @author David Turanski
 *
 */
public class DefaultCheckpointWireTap extends AbstractCheckpointWireTap {

	public DefaultCheckpointWireTap(MessageChannel channel) {
		super(channel);
	}

	@Override
	protected Properties addCheckpointProperties(Message<?> message) {
		return null;
	}

}
