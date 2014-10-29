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



