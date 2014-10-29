package eu.redzoo.reactive.kafka.rest;





import java.io.Closeable;
import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.core.Context;

import com.google.common.collect.ImmutableMap;

import eu.redzoo.reactive.kafka.Kafkas;
import eu.redzoo.reactive.kafka.Message;
import eu.redzoo.reactive.sse.SSEEvent;
import eu.redzoo.reactive.sse.servlet.ServerSentEvents;
import eu.redzoo.reactive.stream.Streams;







@Path("topic/{topic}")
public class EventResource implements Closeable {
    private final ScheduledExecutorService executor = Executors.newScheduledThreadPool(1); 
    private final ImmutableMap<String, String> producerConfig; 
    private final ImmutableMap<String, String> consumerConfig;
    
    
    public EventResource() {
        this.producerConfig = new Environment("reactive-kafka-rest").getConfigValues("zookeeper.connect");
        this.consumerConfig = new Environment("reactive-kafka-rest").getConfigValues("zk.connect", 
                                                                                     "metadata.broker.list", 
                                                                                     "request.required.acks");
    }    
    
    @Override
    public void close() throws IOException {
        executor.shutdown(); 
    }
    
    
    
    @Path("events")
    @POST
    @Consumes("text/event-stream")
    public void consumesSSEEventAsync(@Context HttpServletRequest servletRequest, 
                                      @Suspended AsyncResponse asyncResponse,
                                      @PathParam("topic") String topic) throws IOException { 
                            
        consumesSSEEventForGroupAsync(servletRequest, asyncResponse, UUID.randomUUID().toString(), topic);
    }

    
    
    @Path("group/{groupid}/events")
    @POST
    @Consumes("text/event-stream")
    public void consumesSSEEventForGroupAsync(@Context HttpServletRequest servletRequest, 
                                              @Suspended AsyncResponse asyncResponse,
                                              @PathParam("groupid") String groupid,
                                              @PathParam("topic") String topic) throws IOException { 
                            
        Streams.newStream(ServerSentEvents.newPublisher(servletRequest.getInputStream()))
               .map(sseEvent -> Message.newMessage(sseEvent.getId(), sseEvent.getData()))
               .consume(Kafkas.newSubscriber(topic, ImmutableMap.<String, String>builder().putAll(consumerConfig).put("group.id", groupid).build()));
    }

    


    @Path("events")
    @GET
    @Produces("text/event-stream")
    public void produceSSEEventAsync(@Context HttpServletResponse servletResponse, 
                                     @Suspended AsyncResponse asyncResponse,
                                     @PathParam("topic") String topic) throws IOException {
        
        produceSSEEventWithGroupIdAsync(servletResponse, UUID.randomUUID().toString(), asyncResponse, topic);
    }   
   
    
    
    @Path("group/{groupid}/events")
    @GET
    @Produces("text/event-stream")
    public void produceSSEEventWithGroupIdAsync(@Context HttpServletResponse servletResponse, 
                                     @PathParam("groupid") String groupid,
                                     @Suspended AsyncResponse asyncResponse,
                                     @PathParam("topic") String topic) throws IOException {
        
        servletResponse.setHeader("Content-Type", "text/event-stream");
        
        Streams.newStream(Kafkas.newPublisher(topic, ImmutableMap.<String, String>builder().putAll(producerConfig).put("group.id", groupid).build()))
               .map(message -> SSEEvent.newEvent().id(message.getId()).data(message.getData()))
               .consume(ServerSentEvents.newSubscriber(servletResponse.getOutputStream(), executor));
    }   
}



