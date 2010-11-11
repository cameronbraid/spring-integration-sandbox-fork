package org.springframework.integration.gemfire.store;

import org.springframework.integration.Message;
import org.springframework.integration.store.AbstractMessageGroupStore;
import org.springframework.integration.store.MessageGroup;
import org.springframework.util.Assert;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentMap;


/**
 * Provides an implementation of {@link org.springframework.integration.store.MessageGroupStore} that delegates to a backend Gemfire instance.
 * Gemfire holds values in using keys and values. This class provides a strategy to hold objects.
 *
 * @author Josh Long
 */
public class KeyValueMessageGroupStore extends AbstractMessageGroupStore {
	/**
	 * hackety. Basically, some operations can be done atomically and we should support them if possible
	 */
	private boolean unmarkedIsConcurrentMap;

	/**
	 * hackety. Basically, some operations can be done atomically and we should support them if possible
	 */
	private boolean markedIsConcurrentMap;

	/**
	 * hackety. Basically, some operations can be done atomically and we should support them if possible
	 */
	private boolean groupIdToMessageGroupIsConcurrentMap;

	/**
	 * required {@link com.gemstone.gemfire.cache.Region} to managed the association of groups => {@link KeyValueMessageGroup}
	 */
	protected Map<Object, KeyValueMessageGroup> groupIdToMessageGroup;

	/**
	 * required {@link com.gemstone.gemfire.cache.Region} to manage the #unmarked data
	 */
	protected Map<String, Message<?>> unmarked;

	/**
	 * required {@link com.gemstone.gemfire.cache.Region} to manage the #marked data
	 */
	protected Map<String, Message<?>> marked;

	/**
	 * Needs two backing regions to handle the state managment
	 *
	 * @param groupIdToMessageGroup the region to associate
	 * @param marked				the collection that will hold which messages are marked (delivered)
	 * @param unmarked			  the collection that holds which messages are unmarked (not yet delivered)
	 */
	public KeyValueMessageGroupStore(
			Map<Object, KeyValueMessageGroup> groupIdToMessageGroup,
			Map<String, Message<?>> marked, Map<String, Message<?>> unmarked) {
		this.marked = marked;
		Assert.notNull(this.marked,
				"you must provide an ConcurrentMap to hold String => Message<?> for marked");

		this.unmarked = unmarked;
		Assert.notNull(this.unmarked,
				"you must provide an ConcurrentMap to hold String => Message<?> for unmarked");

		this.groupIdToMessageGroup = groupIdToMessageGroup;
		Assert.notNull(this.groupIdToMessageGroup,
				"you must provide a ConcurrentMap to hold associations of group ids to message groups ('groupIdToMessageGroup')");

		this.markedIsConcurrentMap = marked instanceof ConcurrentMap;
		this.unmarkedIsConcurrentMap = unmarked instanceof ConcurrentMap;
		this.groupIdToMessageGroupIsConcurrentMap = this.groupIdToMessageGroup instanceof ConcurrentMap;
	}

	public MessageGroup getMessageGroup(Object groupId) {
		Assert.notNull(groupId, "'groupId' must not be null");

		return this.getMessageGroupInternal(groupId);
	}

	public MessageGroup addMessageToGroup(Object groupId, Message<?> message) {
		KeyValueMessageGroup group = getMessageGroupInternal(groupId);
		group.add(message);

		return group;
	}

	public MessageGroup markMessageGroup(MessageGroup group) {
		Object groupId = group.getGroupId();
		KeyValueMessageGroup internal = getMessageGroupInternal(groupId);
		internal.markAll();

		return internal;
	}

	public void removeMessageGroup(Object groupId) {
		groupIdToMessageGroup.remove(groupId);
	}

	public MessageGroup removeMessageFromGroup(Object key,
											   Message<?> messageToRemove) {
		KeyValueMessageGroup group = getMessageGroupInternal(key);
		group.remove(messageToRemove);

		return group;
	}

	public MessageGroup markMessageFromGroup(Object key,
											 Message<?> messageToMark) {
		KeyValueMessageGroup group = getMessageGroupInternal(key);
		group.mark(messageToMark);

		return group;
	}

	@Override
	public Iterator<MessageGroup> iterator() {
		return new HashSet<MessageGroup>(groupIdToMessageGroup.values()).iterator();
	}

	protected KeyValueMessageGroup ensureMessageGroupHasReferencesToRegions(
			KeyValueMessageGroup keyValueMessageGroup) {
		if (keyValueMessageGroup == null) {
			return null;
		}

		keyValueMessageGroup.setMarked(this.marked);
		keyValueMessageGroup.setUnmarked(this.unmarked);

		return keyValueMessageGroup;
	}

	protected KeyValueMessageGroup getMessageGroupInternal(Object groupId) {
		if (this.groupIdToMessageGroupIsConcurrentMap) {
			ConcurrentMap<Object, KeyValueMessageGroup> cm = (ConcurrentMap<Object, KeyValueMessageGroup>) this.groupIdToMessageGroup;
			cm.putIfAbsent(groupId, new KeyValueMessageGroup(groupId));
		} else {
			if (!groupIdToMessageGroup.containsKey(groupId)) {
				groupIdToMessageGroup.put(groupId, new KeyValueMessageGroup(groupId));
			}
		}

		return ensureMessageGroupHasReferencesToRegions(groupIdToMessageGroup.get( groupId));
	}
}
