package org.springframework.integration.mongodb.store;

import java.util.UUID;

import org.springframework.integration.Message;
import org.springframework.integration.store.MessageStore;

public class MongoMessageStore implements MessageStore {

	@Override
	public <T> Message<T> addMessage(Message<T> message) {
		return null;
	}

	@Override
	public Message<?> getMessage(UUID id) {
		return null;
	}

	@Override
	public int getMessageCount() {
		return 0;
	}

	@Override
	public Message<?> removeMessage(UUID id) {
		return null;
	}

}
