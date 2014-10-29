
reactive
======




reactive kafka
---------------
supports reactive-streams.org for kafka 

```java
 Publisher<Message> publisher = Kafkas.newPublisher("test", ImmutableMap.of("zookeeper.connect", "localhost:" + zookeeper.getPort(),
                                                                                                         "group.id", UUID.randomUUID().toString()));

 Subscriber<Message> subscriber = Kafkas.newSubscriber("test", ImmutableMap.of("zk.connect", "localhost:" + zookeeper.getPort(),
                                                                                                               "metadata.broker.list", "localhost:" + kafka.getPort(),
                                                                                                               "request.required.acks", "1"));
```