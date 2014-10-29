package eu.redzoo.reactive.stream;








import org.reactivestreams.Publisher;






public class Streams {     

    private Streams() { }
    
    public static <T> ReadableStream<T> newStream(Publisher<T> publisher) {
        return new PublisherBasedReadableStream<T>(publisher);
    }    
}



