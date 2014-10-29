
reactive
======




reactive.stream
---------------
supports reactive-streams.org for streaming


```java
ReadableStream<Message> stream = Streams.newStream(publisher);
stream.consume(msg -> System.out.println(msg));       
...

```




reactive.see
---------------
supports reactive-streams.org for servlet-api based server-sent-event streams


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
supports reactive-streams.org for servlet-api based server-sent-event streams


```java
...

   
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

    
    
  
    @Path("group/{groupid}/events")
    @GET
    @Produces("text/event-stream")
    public void produceSSEEventWithGroupIdAsync(@Context HttpServletResponse servletResponse, 
                                     @PathParam("groupid") String groupid,
                                     @Suspended AsyncResponse asyncResponse,
                                     @PathParam("topic") String topic) throws IOException {
        
        Streams.newStream(Kafkas.newPublisher(topic, ImmutableMap.<String, String>builder().putAll(producerConfig).put("group.id", groupid).build()))
               .map(message -> SSEEvent.newEvent().id(message.getId()).data(message.getData()))
               .consume(ServerSentEvents.newSubscriber(servletResponse.getOutputStream(), executor));
    }       
...

```


