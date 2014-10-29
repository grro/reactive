
reactive
======




reactive.stream
---------------
supports streaming based on reactive-streams.org


```java
Publisher<Message> publisher = ...
ReadableStream<Message> stream = Streams.newStream(publisher);
stream.consume(msg -> System.out.println(msg));       
...

```




reactive.see
---------------
supports reactive-streams.org for servlet-api based Server-Sent Events streams


```java
Publisher<Message> publisher = ServerSentEvents.newPublisher(servletRequest.getInputStream())
publisher.subscribe(...);
...


Subscriber<SSEEvent> subscriber = ServerSentEvents.newSubscriber(servletResponse.getOutputStream(), executor)
subscriber.onSubscribe(...);
subscriber.onNext(...);
...

```


reactive.kafka
---------------
supports reactive-streams.org for kafka 

```java
Publisher<Message> publisher = Kafkas.newPublisher("test", consumerConfig);
publisher.subscribe(...);
...


Subscriber<Message> subscriber = Kafkas.newSubscriber("test", producerConfig);
subscriber.onSubscribe(...);
subscriber.onNext(...);
...

```


reactive.kafka.rest
---------------
reactive Server-Sent Events adapter for kafka 


```java
curl -i http://localhost:9777/eventbroker/rest/topic/test/events
HTTP/1.1 200 OK
Server: Apache-Coyote/1.1
Content-Type: text/event-stream
Transfer-Encoding: chunked
Date: Wed, 29 Oct 2014 10:08:02 GMT

id: 5
data: test 5test

id: 6
data: test 6test

id: 7
data: test 7test
```



```java


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

```


