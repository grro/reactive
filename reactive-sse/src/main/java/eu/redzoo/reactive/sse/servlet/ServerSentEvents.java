package eu.redzoo.reactive.sse.servlet;








import java.util.concurrent.ScheduledExecutorService;

import javax.servlet.ServletInputStream;
import javax.servlet.ServletOutputStream;

import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;

import eu.redzoo.reactive.sse.SSEEvent;



public class ServerSentEvents {     

    private ServerSentEvents() { }
    
    public static Publisher<SSEEvent> newPublisher(ServletInputStream in) {
        return new ServletInputStreamSEEEventPublisher(in);
    }
    
    public static Subscriber<SSEEvent> newSubscriber(ServletOutputStream out, ScheduledExecutorService executor) {
        return new ServletOutputStreamSEEEventSubscriber(out, executor);
    }
}



