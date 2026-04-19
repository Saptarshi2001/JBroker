
package com.jbroker;


import com.jbroker.Subscribers.Subscriber;
import net.jqwik.api.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import net.jqwik.api.constraints.IntRange;
import com.jbroker.Topic.Topic;
import com.jbroker.Clients.Client;

class PropertyTesting {

    @Property
    void sidShouldExistAfterSubscribe(
            @ForAll @IntRange(min = 1, max = 20000) int sid,
            @ForAll String topicName
    ) {

        Map<Integer, Topic> mp = new ConcurrentHashMap<>();
        List<Topic> lst = new CopyOnWriteArrayList<>();

        Client client = new Client("127.0.0.1", 1000, null);

        Subscriber sub = new Subscriber(mp, sid, topicName, lst);
        Topic topic = new Topic(topicName, client,sid);

        sub.subscribe(sid, topic);

        assertTrue(mp.containsKey(sid));
    }
    
    @Property
void unsubRemovesSid(
        @ForAll @IntRange(min = 1, max = 20000) int sid,
        @ForAll String topicName
) {

    Map<Integer, Topic> mp = new ConcurrentHashMap<>();
    List<Topic> lst = new CopyOnWriteArrayList<>();

    Client client = new Client("127.0.0.1", 1000, null);

    Subscriber sub = new Subscriber(mp, sid, topicName, lst);
    Topic topic = new Topic(topicName, client,sid);

    sub.subscribe(sid, topic);

    sub.unsub(client, sid);

    assertFalse(mp.containsKey(sid));
}
    
@Property
void duplicateSidRejected(
        @ForAll @IntRange(min = 1, max = 1000) int sid,
        @ForAll String topicName
) {

    Map<Integer, Topic> mp = new ConcurrentHashMap<>();
    List<Topic> lst = new CopyOnWriteArrayList<>();

    Client client = new Client("127.0.0.1", 1000, null);

    Subscriber sub = new Subscriber(mp, sid, topicName, lst);

    Topic t1 = new Topic(topicName, client,sid);
    Topic t2 = new Topic("other", client,sid);

    sub.subscribe(sid, t1);
    String result = sub.subscribe(sid, t2);

    assertEquals("Id already present", result);
}    


@Property
void topicReferenceAlwaysValid(
        @ForAll @IntRange(min = 1, max = 20000) int sid,
        @ForAll String topicName
) {

    Map<Integer, Topic> mp = new ConcurrentHashMap<>();
    List<Topic> lst = new CopyOnWriteArrayList<>();

    Client client = new Client("127.0.0.1", 1000, null);

    Subscriber sub = new Subscriber(mp, sid, topicName, lst);
    Topic topic = new Topic(topicName, client,sid);

    sub.subscribe(sid, topic);

    Topic stored = mp.get(sid);

    assertTrue(lst.contains(stored));
}

@Property
void multipleSubscriptionsRemain(
        @ForAll @IntRange(min = 1, max = 500) int sid1,
        @ForAll @IntRange(min = 501, max = 1000) int sid2,
        @ForAll String topicName
) {

    Map<Integer, Topic> mp = new ConcurrentHashMap<>();
    List<Topic> lst = new CopyOnWriteArrayList<>();

    Client client = new Client("127.0.0.1", 1000, null);

    Subscriber sub1 = new Subscriber(mp, sid1, topicName, lst);
    Subscriber sub2 = new Subscriber(mp, sid2, topicName, lst);

    Topic topic1 = new Topic(topicName, client,sid1);
    Topic topic2 = new Topic(topicName, client,sid2);

    sub1.subscribe(sid1, topic1);
    sub2.subscribe(sid2, topic2);

    sub1.unsub(client, sid1);

    assertTrue(mp.containsKey(sid2));
}


    
}