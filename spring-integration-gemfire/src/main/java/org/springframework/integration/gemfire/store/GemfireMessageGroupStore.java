package org.springframework.integration.gemfire.store;

import com.gemstone.gemfire.cache.Region;

import org.springframework.integration.Message;
import org.springframework.integration.store.*;

import org.springframework.util.Assert;

import java.io.Serializable;

import java.util.*;
import java.util.concurrent.ConcurrentMap;


/**
 * Provides an implementation of {@link org.springframework.integration.store.MessageGroupStore} that delegates to a backend Gemfire instance.
 * <p/>
 * Gemfire holds values in using keys and values. This class provides a strategy to hold objects
 * <p/>
 * todo: simply reuse {@link org.springframework.integration.gemfire.store.GemfireMessageStore}
 * <p/>
 * todo this class should be the one configured to have the #marked and #unmarked regions
 *
 * @author Josh Long
 */
public class GemfireMessageGroupStore extends AbstractMessageGroupStore /*implements MessageStore, MessageGroupStore */{

	/**
     * required {@link com.gemstone.gemfire.cache.Region} to manage the association of {@link java.util.UUID} => {@link org.springframework.integration.Message}
     */
    private ConcurrentMap<UUID, Message<?>> idToMessage;

    /**
     * required {@link com.gemstone.gemfire.cache.Region} to managed the association of groups => {@link org.springframework.integration.gemfire.store.GemfireMessageGroup}
     */
    private ConcurrentMap<Object, GemfireMessageGroup> groupIdToMessageGroup;

	/**
	 * required {@link com.gemstone.gemfire.cache.Region} to manage the #unmarked data
	 */
    private ConcurrentMap<String, Message<?>> unmarked;

	/**
	 * required {@link com.gemstone.gemfire.cache.Region} to manage the #marked data
	 */
    private ConcurrentMap<String, Message<?>> marked;

    /**
     * Needs two backing regions to handle the state managment
     *
     * @param idToMessage                   the region to associate IDs to a given {@link org.springframework.integration.Message}
     * @param groupIdToMessageGroup the region to associate
     */
    public GemfireMessageGroupStore(
        ConcurrentMap<UUID, Message<?>> idToMessage,
        ConcurrentMap<Object, GemfireMessageGroup> groupIdToMessageGroup,
		ConcurrentMap<String,Message<?>> marked,
		ConcurrentMap<String,Message<?>> unmarked) {

		this.marked = marked ;
		Assert.notNull( this.marked , "you must provide an Region to hold String => Message<?> for marked");

		this.unmarked = unmarked ;
		Assert.notNull( this.unmarked , "you must provide an Region to hold String => Message<?> for unmarked");

		this.idToMessage = idToMessage;
        Assert.notNull(this.idToMessage, "you must provide a Region to hold associations of ids to messages ('idToMessage')");

        this.groupIdToMessageGroup = groupIdToMessageGroup;
        Assert.notNull(this.groupIdToMessageGroup, "you must provide a Region to hold associations of group ids to message groups ('groupIdToMessageGroup')");

    }

    public void setUnmarked(Region<String, Message<?>> unmarked) {
        this.unmarked = unmarked;
    }

    public void setMarked(Region<String, Message<?>> marked) {
        this.marked = marked;
    }

    public <T> Message<T> addMessage(Message<T> message) {
        Assert.isInstanceOf(Serializable.class, message.getPayload(), "the payload must be assignable to java.io.Serializable");
        this.idToMessage.put(message.getHeaders().getId(), message);
        return message;
    }

    /**
     * @param key the {@link java.util.UUID} that every {@link org.springframework.integration.MessageHeaders#getId()} returns
     * @return {@link org.springframework.integration.Message} being stored in the cache
     * @see {@link MessageStore#getMessage(java.util.UUID)}
     */
    public Message<?> getMessage(UUID key) {
        return (key != null) ? this.idToMessage.get(key) : null;
    }

    /**
     * @param key the Message ID to remove
     * @return
     * @see {@link org.springframework.integration.store.MessageStore#removeMessage(java.util.UUID)}
     */
    public Message<?> removeMessage(UUID key) {
        if (key != null) {
            return this.idToMessage.remove(key);
        } else {
            return null;
        }
    }

    public MessageGroup getMessageGroup(Object groupId) {
        Assert.notNull(groupId, "'groupId' must not be null");
        return this.getMessageGroupInternal(groupId);
    }

    public MessageGroup addMessageToGroup(Object groupId, Message<?> message) {
        GemfireMessageGroup group = getMessageGroupInternal(groupId);
        group.add(message);
        return group;
    }

    public MessageGroup markMessageGroup(MessageGroup group) {
        Object groupId = group.getGroupId();
        GemfireMessageGroup internal = getMessageGroupInternal(groupId);
        internal.markAll();
        return internal;
    }

    public void removeMessageGroup(Object groupId) {
        groupIdToMessageGroup.remove(groupId);
    }

    public MessageGroup removeMessageFromGroup(Object key, Message<?> messageToRemove) {
        GemfireMessageGroup group = getMessageGroupInternal(key);
        group.remove(messageToRemove);
        return group;
    }

    public MessageGroup markMessageFromGroup(Object key, Message<?> messageToMark) {
        GemfireMessageGroup group = getMessageGroupInternal(key);
        group.mark(messageToMark);
        return group;
    }

	
    @Override
    public Iterator<MessageGroup> iterator() {
        return new HashSet<MessageGroup>(groupIdToMessageGroup.values()).iterator();
    }

    private GemfireMessageGroup ensureMessageGroupHasReferencesToRegions( GemfireMessageGroup gemfireMessageGroup) {
		if(gemfireMessageGroup == null)
			return null;
        gemfireMessageGroup.setMarked(this.marked);
		gemfireMessageGroup.setUnmarked(this.unmarked);
        return gemfireMessageGroup;
    }

    private GemfireMessageGroup getMessageGroupInternal(Object groupId) {
        if (!groupIdToMessageGroup.containsKey(groupId))
            groupIdToMessageGroup.putIfAbsent(groupId, new GemfireMessageGroup(groupId));

		// todo lets investigate the Instantiator that Costin's put into SGF to handle just this sort of thing
		// todo  -- is the Instantiator unqiue to DataSerializable entities or does it also support regular serialized Java objects? (which Gemfire supports as well)
        return ensureMessageGroupHasReferencesToRegions(groupIdToMessageGroup.get(groupId));
    }
}
