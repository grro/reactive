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





import java.io.IOException;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import javax.servlet.ServletOutputStream;
import javax.servlet.WriteListener;

import com.google.common.collect.Lists;

import eu.redzoo.reactive.sse.SSEEvent;




class SSEWriteableChannel {
    private static final List<CompletableFuture<Void>> whenWritePossibles = Lists.newArrayList();
    
    private final ServletOutputStream out;
    private final Consumer<Throwable> errorConsumer;

    
    public SSEWriteableChannel(ServletOutputStream out, Consumer<Throwable> errorConsumer) {
        this.out = out;
        this.errorConsumer = errorConsumer;
        
        out.setWriteListener(new ServletWriteListener());
    }


    public CompletableFuture<Integer> writeEventAsnyc(SSEEvent event) {       
        CompletableFuture<Integer> result = new CompletableFuture<>();
        
        try {
            synchronized (out) {
                byte[] data = event.toWire().getBytes("UTF-8");
                out.write(event.toWire().getBytes("UTF-8"));
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
    
    
    public CompletableFuture<Void> whenWritePossibleAsync() {
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
        
        // Unfortunately the Servlet 3.1 spec left it open how many bytes can be written
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
