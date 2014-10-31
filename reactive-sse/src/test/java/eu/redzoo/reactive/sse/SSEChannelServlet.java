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
package eu.redzoo.reactive.sse;






import java.io.IOException;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import javax.servlet.AsyncContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.common.base.Joiner;
import com.google.common.collect.Sets;

import eu.redzoo.reactive.sse.servlet.SSEReadableChannel;
import eu.redzoo.reactive.sse.servlet.SSEWriteableChannel;



public class SSEChannelServlet extends HttpServlet {
    private static final long serialVersionUID = -7372081048856966492L;
    
    private final FifoQueue<SSEEvent> queue = new FifoQueue<>();
    private final CopyOnWriteArraySet<SSEReader> readers = Sets.newCopyOnWriteArraySet();
    
    private final AtomicBoolean isActive = new AtomicBoolean(true);
    
    
    private CopyOnWriteArraySet<SSEReadableChannel> readableChannels = Sets.newCopyOnWriteArraySet();
    
    
    
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        if (req.getPathInfo().startsWith("/info")) {
            writeInfo(req, resp);
        } else if (req.getPathInfo().startsWith("/event")) {
            writeSSEStream(req, resp);
        } else {
            resp.sendError(400);
        }
    }
    
    
    
    private void writeSSEStream(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        AsyncContext ctx = req.startAsync();
        
        SSEWriteableChannel channel = new SSEWriteableChannel(resp.getOutputStream(), error -> ctx.complete());

        SSEWriter writer = new SSEWriter(channel, queue);
        writer.start();
    }
    
    

    private static class SSEWriter {
        
        private final SSEWriteableChannel channel;
        private final FifoQueue<SSEEvent> queue;
        
        
        public SSEWriter(SSEWriteableChannel channel, FifoQueue<SSEEvent> queue) {
            this.channel = channel;
            this.queue = queue;
        }
        
        public void start() {
            queue.consume(event -> channel.whenWritePossibleAsync()
                                          .thenAccept(channel -> channel.writeEventAsync(event)));
        }
    }

    

    
    private void writeInfo(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.getWriter().write("active: " + isActive.get() +" \r\n" +
                               "SSEReadableChannel:  "  + Joiner.on("\r\n").join(readableChannels));
    }

    
    
    
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        if (req.getPathInfo().startsWith("/control")) {
            readControl(req, resp);
        } else if (req.getPathInfo().startsWith("/event")) {
            readSSEStream(req, resp);
        } else {
            resp.sendError(400);
        }
    }

    
    
    private void readSSEStream(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        AsyncContext ctx = req.startAsync();

        resp.setContentType("text/event-stream");
        SSEReadableChannel channel = new SSEReadableChannel(req.getInputStream(), error -> ctx.complete(), Void -> ctx.complete());

        readableChannels.add(channel);
        
        SSEReader reader = new SSEReader(channel, queue, isActive.get());
        readers.add(reader);
        reader.start();
    }
    
    

    private static class SSEReader {
        
        private final SSEReadableChannel channel;
        private final FifoQueue<SSEEvent> queue;
        
        private final Consumer<SSEEvent> activeAction = (event) -> {  queue.add(event); readNext(); };
        private final Consumer<SSEEvent> passiveAction = (event) -> queue.add(event);
        
        private final AtomicReference<Consumer<SSEEvent>> currentActionRef;
        
        
        public SSEReader(SSEReadableChannel channel, FifoQueue<SSEEvent> queue, boolean isActive) {
            this.channel = channel;
            this.queue = queue;
            this.currentActionRef = new AtomicReference<>(isActive ? activeAction : passiveAction);
        }
        
        public void start() {
            readNext();
        }
        
        public void suspend() {
            currentActionRef.set(passiveAction);
        }

        public void resume() {
            currentActionRef.set(activeAction);      
            start();
        }

        
        private void readNext() {
            channel.readEventAsync()
                   .thenAccept(currentActionRef.get());            
        }
    }

    
    

    private void readControl(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String command = req.getPathInfo().substring("/control/".length());
        
        if (command.equalsIgnoreCase("suspension")) {
            isActive.set(false);
            for (SSEReader reader : readers) {
                reader.suspend();
            }
            
        } else if (command.equalsIgnoreCase("resumption")) {
            isActive.set(true);
            for (SSEReader reader : readers) {
                reader.resume();
            }
        }
    }   
    
    
    
    private final class FifoQueue<T> {
        
        private final Set<Consumer<T>> consumers = Sets.newHashSet();
        
        public synchronized void add(T t) {
            consumers.forEach(consumer -> consumer.accept(t));
        }

        public synchronized void consume(Consumer<T> consumer) {
            consumers.add(consumer);
        }
    }
}



