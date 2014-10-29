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



import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;

import com.google.common.collect.ImmutableMap;




public class Kafkas {

    public static Publisher<Message> newPublisher(String topic, ImmutableMap<String, String> kafkaProps) {
        return new KafkaTopicPublisher(topic, kafkaProps);
    }

    
    public static Subscriber<Message> newSubscriber(String topic, ImmutableMap<String, String> kafkaProps) {
        return new KafkaTopicSubscriber(topic, kafkaProps);
    }
}



