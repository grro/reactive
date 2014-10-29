package eu.redzoo.reactive.sse.servlet;





import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import javax.servlet.ReadListener;
import javax.servlet.ServletInputStream;

import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import com.google.common.collect.Lists;
import com.google.common.io.Closeables;

import eu.redzoo.reactive.sse.SSEEvent;
import eu.redzoo.reactive.sse.ServerSentEventParser;




class ServletInputStreamSEEEventPublisher implements Publisher<SSEEvent> {     
    private final AtomicReference<Optional<Subscriber<? super SSEEvent>>> subscriberRef = new AtomicReference<>(Optional.empty());
    private final ServletInputStream in;
    
    
    public ServletInputStreamSEEEventPublisher(ServletInputStream in) {
        this.in = in;
    }
    
    
    @Override
    public void subscribe(Subscriber<? super SSEEvent> subscriber) {
        
        synchronized (subscriberRef) {     
            if (subscriberRef.get().isPresent()) {
                throw new IllegalStateException("subscriber already registered (publisher does not support multi-subscriptions)");
            } 

            subscriberRef.set(Optional.of(subscriber));
            subscriber.onSubscribe(new SEEEventReaderSubscription(in, subscriber));
        }
    }
    
  
 
    
    
    private static final class SEEEventReaderSubscription implements Subscription {
        private final Network network;
        private final Subscriber<? super SSEEvent> subscriber;

        
       
        public SEEEventReaderSubscription(ServletInputStream in, Subscriber<? super SSEEvent> subscriber) {
            this.subscriber = subscriber;
            this.network = new Network(in,                                   // servlet input stream
                                       error -> subscriber.onError(error),   // error consumer
                                       Void -> subscriber.onComplete());     // completion consumer         
        }
        
               
        @Override
        public void cancel() {
            network.close();
        }

        
        @Override
        public void request(long n) {
            network.readNextAsync()
                   .thenAccept(event -> subscriber.onNext(event));
        }
    }
    
    
    

    private static final class Network {
        private final Queue<CompletableFuture<SSEEvent>> pendingReads = Lists.newLinkedList();
        private final ConcurrentLinkedQueue<SSEEvent> bufferedEvents = new ConcurrentLinkedQueue<>();
        
        private final Object readLock = new Object();

        private final ServerSentEventParser parser = new ServerSentEventParser();
        
        private final byte buf[] = new byte[1024];
        private int len = -1;
        
        
        private final ServletInputStream in;
        private final Consumer<Throwable> errorConsumer;
        private final Consumer<Void> completionConsumer;

        
        
        public Network(ServletInputStream in, Consumer<Throwable> errorConsumer, Consumer<Void> completionConsumer) {
            this.in = in;
            this.errorConsumer = errorConsumer;
            this.completionConsumer = completionConsumer;
            
            in.setReadListener(new ServletReadListener());
        }

        
        private final class ServletReadListener implements ReadListener {
            
            @Override
            public void onAllDataRead() throws IOException {
                completionConsumer.accept(null);
            }
            
            @Override
            public void onError(Throwable t) {
                Network.this.onError(t);
            }
            
            @Override
            public void onDataAvailable() throws IOException {
                consumeNetworkAndHandlePendingReads();
            }
        }

        
        private void onError(Throwable t)  {
            errorConsumer.accept(t);
            close();
        }
        
        
        public void close() {
            Closeables.closeQuietly(in);
            pendingReads.clear();
            bufferedEvents.clear();
        }
        
        
        public CompletableFuture<SSEEvent> readNextAsync() {
            
            CompletableFuture<SSEEvent> pendingRead = new CompletableFuture<SSEEvent>();

            synchronized (readLock) {
                
                // buffered available?
                SSEEvent event = bufferedEvents.poll();
                
                // no, try to consume network
                if (event == null) {
                    pendingReads.add(pendingRead);
                    consumeNetworkAndHandlePendingReads(); 
                    
                // yes
                } else {
                    pendingRead.complete(event);
                }
            }
            
            return pendingRead;
        }
        
        
        
       
        private void consumeNetworkAndHandlePendingReads() {
            
            synchronized (readLock) {

                try {
                    while (!pendingReads.isEmpty() && isNetworkdataAvailable() && (len = in.read(buf)) != -1) {
                        
                        for (SSEEvent event : parser.parse(ByteBuffer.wrap(buf, 0, len))) {
         
                            // get next pending read
                            CompletableFuture<SSEEvent> pendingRead = pendingReads.poll();
                            
                            // further pendings?
                            if (pendingRead == null) {
                                // no, add event to buffer
                                bufferedEvents.add(event);
                                
                            } else {
                                // yes, complete it
                                pendingRead.complete(event);
                            }
                        }
                    }
                    
                } catch (IOException | RuntimeException t) {
                   onError(t);
                }                    
            }
        }    
        
                
        private boolean isNetworkdataAvailable() {
            try {
                return in.isReady();
            } catch (IllegalStateException ise)  {
                return false;
            }
        }
    }
}
