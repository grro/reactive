package eu.redzoo.reactive.kafka;


import java.io.IOException;
import java.util.Properties;

import com.google.common.collect.ImmutableMap;

import kafka.server.KafkaConfig;
import kafka.server.KafkaServerStartable;



public class EmbeddedKafka {
 
    private KafkaServerStartable kafka;
    private final int port;

    
    public EmbeddedKafka(ImmutableMap<String, String> kafkaProperties) throws IOException, InterruptedException{
        this.port = Integer.parseInt(kafkaProperties.get("port"));
        
        
        Properties props = new Properties();
        props.putAll(kafkaProperties);
        kafka = new KafkaServerStartable(new KafkaConfig(props));
    }
    
    public int getPort() {
        return port;
    }
    
    public void start() {
        kafka.startup();
    }
    
    public void shutdown(){
        kafka.shutdown();
    }
}



