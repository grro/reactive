/*
 * Copyright (c) 2014 Gregor Roth
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *  http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package eu.redzoo.reactive.kafka;






import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Consumer;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;

import eu.redzoo.reactive.stream.ReadableStream;
import eu.redzoo.reactive.stream.Streams;



public class ReactiveKafkaTest {

    private static EmbeddedZookeeper zookeeper;
    private static EmbeddedKafka kafka;
    
    
    @BeforeClass
    public static void setUp() throws Exception {
        int zookeeperPort = 8643;
        zookeeper = new EmbeddedZookeeper(zookeeperPort);
        zookeeper.start();
        
        int kafkaPort = 8543;
        kafka = new EmbeddedKafka(ImmutableMap.of("broker.id", Integer.toString(new Random().nextInt(100000)),
                                                  "port", Integer.toString(kafkaPort),
                                                  "zookeeper.connect", "localhost:" + zookeeperPort));
        kafka.start();
    }

    
    @AfterClass
    public static void tearDown() throws Exception {
        kafka.shutdown();
        zookeeper.shutdown();
    }

    
    @Test
    public void testSimple() throws Exception {
        
        Publisher<Message> publisher = Kafkas.newPublisher("test", ImmutableMap.of("zookeeper.connect", "localhost:" + zookeeper.getPort(),
                                                                                   "group.id", UUID.randomUUID().toString()));

        Subscriber<Message> subscriber = Kafkas.newSubscriber("test", ImmutableMap.of("zk.connect", "localhost:" + zookeeper.getPort(),
                                                                                      "metadata.broker.list", "localhost:" + kafka.getPort(),
                                                                                      "request.required.acks", "1"));
        
        
        ReadableStream<Message> stream1 = Streams.newStream(publisher);
        SimpleConsumer consumer1 = new SimpleConsumer();
        stream1.consume(consumer1);

        ReadableStream<Message> stream2 = Streams.newStream(publisher);
        SimpleConsumer consumer2 = new SimpleConsumer();
        stream2.consume(consumer2);

        
        
        
        Publisher<Message> publisher2 = Kafkas.newPublisher("test", ImmutableMap.of("zookeeper.connect", "localhost:" + zookeeper.getPort(),
                                                                                    "group.id", UUID.randomUUID().toString()));

        ReadableStream<Message> stream3 = Streams.newStream(publisher2);
        SimpleConsumer consumer3 = new SimpleConsumer();
        stream3.consume(consumer3);
        
        
        subscriber.onSubscribe(new Subscription() {
            
            @Override
            public void request(long n) {
            }
            
            @Override
            public void cancel() {
            }
        });

        
        try {
            Thread.sleep(1000);
        } catch (InterruptedException ignore) { }
        
        for (int i = 0; i < 100; i++) {
            subscriber.onNext(Message.newMessage(i, "test" + i));
        }
        
        
        try {
            Thread.sleep(1000);
        } catch (InterruptedException ignore) { }
        
        
        Assert.assertEquals("test90", consumer1.getMessages().get("90").getData());
        Assert.assertEquals("test90", consumer2.getMessages().get("90").getData());
        Assert.assertEquals("test90", consumer3.getMessages().get("90").getData());
        
    }
    
    
    
    private static final class SimpleConsumer implements Consumer<Message> {
        
        private final ConcurrentMap<String, Message> messages = Maps.newConcurrentMap();
        
        @Override
        public void accept(Message message) {
            messages.put(message.getId(), message);
        }
        
        public ImmutableMap<String, Message> getMessages() {
            return ImmutableMap.copyOf(messages);
        }
    }
}



 