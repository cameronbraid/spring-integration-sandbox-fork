package org.springframework.integration.gemfire.store;

import com.gemstone.gemfire.cache.Region;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.springframework.integration.Message;
import org.springframework.integration.store.MessageGroup;

import java.io.Serializable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.UUID;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;


/**
 * a {@link org.springframework.integration.store.MessageGroup} that manipulates keys and values
 * <p/>
 * Responsoble for managing one group's messages as a {@link org.springframework.integration.store.MessageGroup}
 *
 * @author Josh Long
 */
public class GemfireMessageGroup implements MessageGroup, Serializable {
    /**
     * this should not be persisted. it's passed in through {@link GemfireMessageGroupStore}, which has the reference to the {@link com.gemstone.gemfire.cache.Region}s
     */
    private transient ConcurrentMap<String, Message<?>> marked;

    /**
     * this should not be persisted. it's passed in through {@link GemfireMessageGroupStore}, which has the reference to the {@link com.gemstone.gemfire.cache.Region}s
     */
    private transient ConcurrentMap<String, Message<?>> unmarked;

    /**
     * the #groupId is the unique ID to associate this aggregation of {@link org.springframework.integration.Message}s
     */
    private Object groupId;

    /**
     * passed in through the {@link org.springframework.integration.store.MessageGroupStore}
     */
    private long timestamp;
    private TimeUnit timeUnit = TimeUnit.MILLISECONDS; // we do everything with standard {@link TimeUnit}
    private long lockAttemptPeriod = 60 * 1000; // 1 minute

    /**
     * default javabean ctor (to lend this object to serialization)
     */
    public GemfireMessageGroup() {
    }

    public GemfireMessageGroup(Object groupId) {
        this(groupId, System.currentTimeMillis(), null, null);
    }

    public GemfireMessageGroup(Object groupId, long timestamp,
        Region<String, Message<?>> marked, Region<String, Message<?>> unmarked) {
        this.groupId = groupId;
        this.timestamp = timestamp;
        this.marked = marked;
        this.unmarked = unmarked;
    }

    public GemfireMessageGroup(Object groupId,
        Region<String, Message<?>> marked, Region<String, Message<?>> unmarked) {
        this.groupId = groupId;
        this.timestamp = System.currentTimeMillis();
        this.marked = marked;
        this.unmarked = unmarked;
    }

    public void setUnmarked(ConcurrentMap<String, Message<?>> unmarked) {
        this.unmarked = unmarked;
    }

    public void setMarked(ConcurrentMap<String, Message<?>> marked) {
        this.marked = marked;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public boolean canAdd(Message<?> message) {
        return !isMember(message);
    }

    public void add(Message<?> message) {
        if (isMember(message)) {
            return;
        }
        String unmarkedKey = this.unmarkedKey(message);
        this.unmarked.put(unmarkedKey, (Message<?>) message);
    }

    private String markedKey(Message<?> msg) {
        return baseKey(msg) + "-m";
    }

    private String unmarkedKey(Message<?> msg) {
        return baseKey(msg) + "-u";
    }

    public void remove(Message<?> message) {
		System.out.println( "remove(" + ToStringBuilder.reflectionToString(message)+")");
        if (unmarked.containsValue(message)) {
            unmarked.remove(unmarkedKey(message));
        }

        if (marked.containsValue(message)) {
            marked.remove(markedKey(message));
        }
    }

    private String groupKey() {
        return (getGroupId()).toString();
    }

    private String baseKey(Message<?> msg) {
        String groupKey = groupKey();
        UUID id = msg.getHeaders().getId();
        Integer sn = msg.getHeaders().getSequenceNumber();
        Integer ss = msg.getHeaders().getSequenceSize();
        return String.format("%s-%s-%s-%s", groupKey, id.toString(), sn.toString(), ss.toString());
    }

    public Collection<Message<?>> getUnmarked() {
        Collection<Message<?>> msgs = getMessagesForMessageGroup(this.unmarked);

        return msgs;
    }

    /**
     * this method will be used to discover all the messages for a given group in a {@link com.gemstone.gemfire.cache.Region}
     *
     * @param region the region from which we're hoping to discover these {@link org.springframework.integration.Message}s
     * @return a collection of messages
     */
    private Collection<Message<?>> getMessagesForMessageGroup(
        ConcurrentMap<String, Message<?>> region) {
        try {
            String groupMsgKey = groupKey();

            Collection<Message<?>> msgs = new ArrayList<Message<?>>();

            for (String k : region.keySet()) {
                if (k.startsWith(groupMsgKey)) {
                    msgs.add(region.get(k));
                }
            }

            return msgs;
        } catch (Throwable th) {
            throw new RuntimeException(th);
        }
    }

    public Collection<Message<?>> getMarked() {
        Collection<Message<?>> m = getMessagesForMessageGroup(this.marked);

        return m;
    }

    public Object getGroupId() {
        return groupId;
    }

    public boolean isComplete() {
        if (size() == 0) {
            return true;
        }

        int sequenceSize = getSequenceSize();

        return (sequenceSize > 0) && (sequenceSize == size());
    }

    public int getSequenceSize() {
        if (size() == 0) {
            return 0;
        }

        return getOne().getHeaders().getSequenceSize();
    }

    /**
     * Mark the given message in this group. If the message is not part of this group then this call has no effect.
     * <p/>
     * todo use the {@link com.gemstone.gemfire.cache.Region#getRegionDistributedLock()} or maybe just {@link com.gemstone.gemfire.cache.Region#getDistributedLock(Object)}
     *
     * @param messageToMark
     */
    public void mark(Message<?> messageToMark) {
        if (this.unmarked.containsValue(messageToMark)) {
            this.unmarked.remove(baseKey(messageToMark));
        }

        this.marked.put(baseKey(messageToMark), messageToMark);
    }

    public void markAll() {
        for (Message<?> msg : getUnmarked())
            mark(msg);

        /*
        Lock mLock = null;
        Lock uLock = null;
        try {
        if (this.marked instanceof Region && this.unmarked instanceof Region) {
        Region<String, Message<?>> mRegion = (Region<String, Message<?>>) this.marked;
        Region<String, Message<?>> uRegion = (Region<String, Message<?>>) this.unmarked;
        uLock = uRegion.getRegionDistributedLock();
        mLock = mRegion.getRegionDistributedLock();

        if (uLock.tryLock(this.lockAttemptPeriod, this.timeUnit) && mLock.tryLock(this.lockAttemptPeriod, this.timeUnit)) {

                 for( Message<?> msg : this.getMessagesForMessageGroup(this.unmarked)){
                                                 this.mark(msg);
                 }
        }
        }
        } catch (Throwable th) {
        throw new RuntimeException(th);
        } finally {
        if (uLock != null) {
        uLock.unlock();
        }
        if (mLock != null) {
        mLock.unlock();
        }
        }*/
    }

    public int size() {
        return getMarked().size() + getUnmarked().size();
    }

    public Message<?> getOne() {
        if (!this.unmarked.isEmpty()) {
            String aKey = this.unmarked.keySet().iterator().next();
            Message<?> msgFromASingleKey = this.unmarked.get(aKey);

            return msgFromASingleKey;
        }

        return null;
    }

    /**
     * This method determines whether messages have been added to this group that supersede the given message based on
     * its sequence id. This can be helpful to avoid ending up with sequences larger than their required sequence size
     * or sequences that are missing certain sequence numbers.
     */
    private boolean isMember(Message<?> message) {
        if (size() == 0) {
            return false;
        }

        Integer messageSequenceNumber = message.getHeaders().getSequenceNumber();

        if ((messageSequenceNumber != null) && (messageSequenceNumber > 0)) {
            Integer messageSequenceSize = message.getHeaders().getSequenceSize();

            if (!messageSequenceSize.equals(getSequenceSize())) {
                return true;
            } else {
                if (containsSequenceNumber(unmarked.values(),
                            messageSequenceNumber) ||
                        containsSequenceNumber(marked.values(),
                            messageSequenceNumber)) {
                    return true;
                }
            }
        }

        return false;
    }

    private boolean containsSequenceNumber(Collection<Message<?>> messages,
        Integer messageSequenceNumber) {
        for (Message<?> member : messages) {
            Integer memberSequenceNumber = member.getHeaders()
                                                 .getSequenceNumber();

            if (messageSequenceNumber.equals(memberSequenceNumber)) {
                return true;
            }
        }

        return false;
    }
}
