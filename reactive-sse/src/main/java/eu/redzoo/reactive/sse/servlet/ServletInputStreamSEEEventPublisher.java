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
package eu.redzoo.reactive.sse.servlet;





import java.io.FilterInputStream;
import java.io.IOException;
import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import javax.servlet.ReadListener;
import javax.servlet.ServletInputStream;

import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import com.google.common.collect.Lists;

import eu.redzoo.reactive.sse.SSEEvent;
import eu.redzoo.reactive.sse.ServerSentEventInputStream;




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
                throw new IllegalStateException("subscriber is already registered (publisher does not support multi-subscriptions)");
            } 

            subscriberRef.set(Optional.of(subscriber));
            subscriber.onSubscribe(new SEEEventReaderSubscription(in, subscriber));
        }
    }
    
  
 
    
    
    private static final class SEEEventReaderSubscription implements Subscription {
       
        private final ServletServerSentEventChannel channel;
        private final Subscriber<? super SSEEvent> subscriber;
        
       
        public SEEEventReaderSubscription(ServletInputStream in, Subscriber<? super SSEEvent> subscriber) {
            this.subscriber = subscriber;
            this.channel = new ServletServerSentEventChannel(in,                                   // servlet input stream
                                                             error -> subscriber.onError(error),   // error consumer
                                                             Void -> subscriber.onComplete());     // completion consumer         
        }
        
               
        @Override
        public void cancel() {
            channel.close();
        }

        
        @Override
        public void request(long n) {
            channel.readNextAsync()
                   .thenAccept(event -> subscriber.onNext(event));
        }
    }
    
    
    

    private static final class ServletServerSentEventChannel {
        private final Queue<CompletableFuture<SSEEvent>> pendingReads = Lists.newLinkedList();

        private final ServerSentEventInputStream serverSentEventsStream;
        
        private final Consumer<Throwable> errorConsumer;
        private final Consumer<Void> completionConsumer;

        
        
        public ServletServerSentEventChannel(ServletInputStream in, Consumer<Throwable> errorConsumer, Consumer<Void> completionConsumer) {
            this.errorConsumer = errorConsumer;
            this.completionConsumer = completionConsumer;
            this.serverSentEventsStream = new ServerSentEventInputStream(new NonBlockingInputStream(in));
            
            in.setReadListener(new ServletReadListener());
        }

        
        private final class ServletReadListener implements ReadListener {
            
            @Override
            public void onAllDataRead() throws IOException {
                completionConsumer.accept(null);
            }
            
            @Override
            public void onError(Throwable t) {
                ServletServerSentEventChannel.this.onError(t);
            }
            
            @Override
            public void onDataAvailable() throws IOException {
                proccessPendingReads();
            }
        }

        
        public CompletableFuture<SSEEvent> readNextAsync() {
            CompletableFuture<SSEEvent> pendingRead = new CompletableFuture<SSEEvent>();
            
            synchronized (pendingReads) {   
                
                try {
                    // reading has to be processed inside the sync block to avoid shuffling events 
                    Optional<SSEEvent> optionalEvent = serverSentEventsStream.next();
                    
                    // got an event?
                    if (optionalEvent.isPresent()) {
                        pendingRead.complete(optionalEvent.get());
                     
                    // no, queue the pending read request    
                    // will be handled by performing the read listener's onDataAvailable callback     
                    } else {
                        pendingReads.add(pendingRead);
                    }
                    
                } catch (IOException | RuntimeException t) {
                    onError(t);
                }
            }
            
            return pendingRead;
        }
        
        
        private void proccessPendingReads() {
            
            synchronized (pendingReads) {
                
                try {
                    while(!pendingReads.isEmpty()) {
                        Optional<SSEEvent> optionalEvent = serverSentEventsStream.next();
                        if (optionalEvent.isPresent()) {
                            pendingReads.poll().complete(optionalEvent.get());
                        } else {
                            return;
                        }
                    }
                } catch (IOException | RuntimeException t) {
                    onError(t);
                }
            }
        }
        
        
        private void onError(Throwable t)  {
            errorConsumer.accept(t);
            close();
        }
        
        
        public void close() {
            serverSentEventsStream.close();
            
            synchronized (pendingReads) { 
                pendingReads.clear();
            }
        }
        
        
        
        private static final class NonBlockingInputStream extends FilterInputStream  {
            private final ServletInputStream is;
            
            public NonBlockingInputStream(ServletInputStream is) {
                super(is);
                this.is = is;
            }
          
            
            @Override
            public int read(byte[] b) throws IOException {
                if (isNetworkdataAvailable()) {
                    return super.read(b);
                } else {
                    return 0;
                }
            }
            
            
            @Override
            public int read(byte[] b, int off, int len) throws IOException {
                if (isNetworkdataAvailable()) {
                    return super.read(b, off, len);
                } else {
                    return 0;
                }
            }
            
            
            private boolean isNetworkdataAvailable() {
                try {
                    return is.isReady();
                } catch (IllegalStateException ise)  {
                    return false;
                }
            }
        }
    }    
}
