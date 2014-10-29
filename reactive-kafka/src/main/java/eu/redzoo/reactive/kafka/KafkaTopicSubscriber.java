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



import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import kafka.javaapi.producer.Producer;
import kafka.producer.KeyedMessage;
import kafka.producer.ProducerConfig;

import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import com.google.common.collect.ImmutableMap;


class KafkaTopicSubscriber implements Subscriber<Message> {
    
    private final AtomicBoolean isOpen = new AtomicBoolean(true);
    private final AtomicReference<Subscription> subscriptionRef = new AtomicReference<>(new IllegalStateSubscription());
    
    private final String topic;
    private final Producer<String, String> producer;
    
    public KafkaTopicSubscriber(String topic, ImmutableMap<String, String> kafkaProps) {
        this.topic = topic;
        
        Properties props = new Properties();
        props.putAll(kafkaProps);
        props.put("serializer.class", "kafka.serializer.StringEncoder");
        
        producer = new Producer<>(new ProducerConfig(props));
    }   


    
    @Override
    public void onSubscribe(Subscription subscription) {
        subscriptionRef.set(subscription);
        requestNext();
    }

    
    private void requestNext() {
        subscriptionRef.get().request(1);    
    }
    
    
    @Override
    public void onNext(Message message) {
        try {
            producer.send(new KeyedMessage<String, String>(topic, message.getId(), message.getData()));
            subscriptionRef.get().request(1);
        } catch (RuntimeException e) {
            close();
        }
    }
    

    @Override
    public void onError(Throwable t) {
        close();
    }

  
    @Override
    public void onComplete() {
        close();
    }
    



    private void close() {
        if (isOpen.getAndSet(false)) {
            subscriptionRef.get().cancel();
            producer.close();          
        }
    }
    
    
    
    private static final class IllegalStateSubscription implements Subscription {
        
        @Override
        public void request(long n) {
            throw new IllegalStateException();
        }
        
        @Override
        public void cancel() {
            throw new IllegalStateException();
        }
    }
}
