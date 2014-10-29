package eu.redzoo.reactive.sse.servlet;





import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.logging.Logger;

import javax.servlet.ServletOutputStream;
import javax.servlet.WriteListener;

import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import com.google.common.collect.Lists;

import eu.redzoo.reactive.sse.SSEEvent;




class ServletOutputStreamSEEEventSubscriber implements Subscriber<SSEEvent> {
    private static final Logger LOG = Logger.getLogger(ServletOutputStreamSEEEventSubscriber.class.getName());
    
    private final AtomicBoolean isOpen = new AtomicBoolean(true);
    private final AtomicReference<Subscription> subscriptionRef = new AtomicReference<>(new IllegalStateSubscription());
    
    private final Network network;
    
    
    public ServletOutputStreamSEEEventSubscriber(ServletOutputStream out, ScheduledExecutorService executor) {
        this.network = new Network(out, error -> onError(error));
        
        // start the keep alive emitter 
        new KeepAliveEmitter(network, executor).start();
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
    public void onNext(SSEEvent event) {
        network.writeEventAsnyc(event)           
               .thenAccept(written -> network.getWhenWritePossibleAsync()
                                             .thenAccept(Void -> requestNext())); 
    }
    

    @Override
    public void onError(Throwable t) {
        LOG.fine("error on source stream. stop streaming " + t.getMessage());
        close();
    }

  
    @Override
    public void onComplete() {
        close();
    }
    



    private void close() {
        if (isOpen.getAndSet(false)) {
            subscriptionRef.get().cancel();
            network.close();          
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


    
    
    /**
     * sents keep alive messages to keep the http connection alive in case of idling
     * @author grro
     */
    private static final class KeepAliveEmitter {
        private final Duration noopPeriodSec = Duration.ofSeconds(35); 
        
        private final Network network;
        private final ScheduledExecutorService executor;

        
        public KeepAliveEmitter(Network network, ScheduledExecutorService executor) {
            this.network = network;
            this.executor = executor;
        }
        
        public void start() {
            scheduleNextKeepAliveEvent();
        }
        
        private void scheduleNextKeepAliveEvent() {
            Runnable task = () -> network.getWhenWritePossibleAsync()
                                         .thenAccept(Void -> network.writeEventAsnyc(SSEEvent.newEvent().comment("keep alive"))
                                                                    .thenAccept(written -> scheduleNextKeepAliveEvent()));

            executor.schedule(task, noopPeriodSec.getSeconds(), TimeUnit.SECONDS);
        }
        
    }
        
    
    
    
    private static final class Network {
        private static final List<CompletableFuture<Void>> whenWritePossibles = Lists.newArrayList();
        
        private final ServletOutputStream out;
        private final Consumer<Throwable> errorConsumer;

        
        public Network(ServletOutputStream out, Consumer<Throwable> errorConsumer) {
            this.out = out;
            this.errorConsumer = errorConsumer;
            
            out.setWriteListener(new ServletWriteListener());
        }


        public CompletableFuture<Integer> writeEventAsnyc(SSEEvent event) {       
            
            CompletableFuture<Integer> result = new CompletableFuture<>();
            
            try {
                synchronized (out) {
                    byte[] data = event.toWire().getBytes("UTF-8");
                    out.write(data);
                    out.flush();
                    
                    result.complete(data.length);
                }
                
            } catch (IOException | RuntimeException t) {
                errorConsumer.accept(t);
                result.completeExceptionally(t);
            }
            
            return result;
        }   

        
        public void close() {
            try {
                out.close();
            } catch (IOException ignore) { }
        }
        
        
        public CompletableFuture<Void> getWhenWritePossibleAsync() {
            CompletableFuture<Void> whenWritePossible = new CompletableFuture<Void>();

            synchronized (whenWritePossibles) {
                if (isWritePossible()) {
                    whenWritePossible.complete(null);
                } else {
                    // if not the WriteListener#onWritePossible will be called by the servlet conatiner later
                    whenWritePossibles.add(whenWritePossible);
                }
            }
            
            return whenWritePossible;
        }
        

        private boolean isWritePossible() {
            try {
                return out.isReady();
            } catch (IllegalStateException ise)  {
                return false;
            }
        }
        
        
        private final class ServletWriteListener implements WriteListener {

            @Override
            public void onWritePossible() throws IOException {                
                synchronized (whenWritePossibles) {
                    whenWritePossibles.forEach(whenWritePossible -> whenWritePossible.complete(null));
                    whenWritePossibles.clear();
                }
            }

            @Override
            public void onError(Throwable t) {
                errorConsumer.accept(t);
            }
        }    
    }
}
