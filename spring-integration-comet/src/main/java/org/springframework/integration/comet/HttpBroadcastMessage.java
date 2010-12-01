/*
 * Copyright 2002-2010 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
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
package org.springframework.integration.comet;

import org.springframework.http.HttpHeaders;
import org.springframework.integration.Message;
import org.springframework.integration.mapping.HeaderMapper;


/**
 *
 * Wrapper for the {@link org.springframework.integration.Message} and accompanying HTTP request metadata 
 *
 * @since 2.1
 * @author Jeremy Grelle
 *
 */
public class HttpBroadcastMessage {

	private final Message<?> message;
	
	private final boolean extractPayload;
	
	private final HeaderMapper<HttpHeaders> headerMapper;
	
	public HttpBroadcastMessage(Message<?> message, boolean extractPayload, HeaderMapper<HttpHeaders> headerMapper) {
		this.message = message;
		this.extractPayload = extractPayload;
		this.headerMapper = headerMapper;
	}

	public boolean isExtractPayload() {
		return extractPayload;
	}

	public Message<?> getMessage() {
		return message;
	}
	
	public HeaderMapper<HttpHeaders> getHeaderMapper() {
		return headerMapper;
	}
}
