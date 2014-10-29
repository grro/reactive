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



import java.util.List;
import java.util.Properties;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import kafka.consumer.ConsumerConfig;
import kafka.consumer.KafkaStream;
import kafka.javaapi.consumer.ConsumerConnector;
import kafka.serializer.StringDecoder;
import kafka.utils.VerifiableProperties;

import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import com.google.common.base.Joiner;
import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;





class KafkaTopicPublisher implements Publisher<Message> {     
    
    private final CopyOnWriteArraySet<KafkaTopicSubscription> subscriptions = Sets.newCopyOnWriteArraySet();
    
    private final ConsumerConnector consumerConnector;
    

    public KafkaTopicPublisher(String topic, ImmutableMap<String, String> kafkaProps) {
        Properties props = new Properties();
        props.putAll(kafkaProps);
        
        consumerConnector = kafka.consumer.Consumer.createJavaConsumerConnector(new ConsumerConfig(props));
        
        int numThreads = 1;
        
        StringDecoder decoder = new StringDecoder(new VerifiableProperties());
        List<KafkaStream<String, String>> streams = consumerConnector.createMessageStreams(ImmutableMap.of(topic, new Integer(numThreads)),
                                                                                           decoder,
                                                                                           decoder).get(topic);
        
        ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        for (final KafkaStream<String, String> stream : streams) {
            executor.submit(() ->  stream.forEach(messageAndMetadata -> onNext(Message.newMessage(messageAndMetadata.key(), messageAndMetadata.message()))));
        }
    }
        
    
    private void onNext(Message message) {
        for (KafkaTopicSubscription subscription : subscriptions) {
            subscription.onNext(message);
        }
    }
        
    
    private void close() {
        subscriptions.clear();
        consumerConnector.shutdown();
    }
    
    
    @Override
    public void subscribe(Subscriber<? super Message> subscriber) {
        KafkaTopicSubscription subscription = new KafkaTopicSubscription(subscriber);
        subscriptions.add(subscription);
        
        subscriber.onSubscribe(subscription);
    }
    
    
    @Override
    public String toString() {
        return Joiner.on(", ").join(subscriptions);
    }
     
    
    private final class KafkaTopicSubscription implements Subscription {
        private final Subscriber<? super Message> subscriber;

        private long numRequested = 0; 
       
        public KafkaTopicSubscription(Subscriber<? super Message> subscriber) {
            this.subscriber = subscriber;
        }
        
               
        @Override
        public synchronized void cancel() {
            numRequested = 0;
            subscriptions.remove(this);
        }

        
        @Override
        public synchronized void request(long n) {
            numRequested += n;
        }

        
        private synchronized void onNext(Message message) {
            if (numRequested > 0) {
                try {
                    subscriber.onNext(message);
                } finally {
                    numRequested--;
                }
            }
        }
        
        @Override
        public String toString() {
            return MoreObjects.toStringHelper(this)
                              .add("numRequested", numRequested)
                              .toString();
        }
    }
}
