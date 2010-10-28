package org.springframework.integration.strictordering.entitykey;

import org.springframework.integration.Message;

public class MessageIdEntityKeyExtractor implements EntityKeyExtractor<Message<String>,String> {

	@Override
	public String getKey(Message<String> message) {
		return message.getHeaders().getId()+ ":" + message.getPayload();
	}



}
