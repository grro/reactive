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






import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.channels.Channels;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import org.xlightweb.BodyDataSink;
import org.xlightweb.HttpRequestHeader;
import org.xlightweb.IHttpResponse;
import org.xlightweb.IHttpResponseHandler;
import org.xlightweb.client.IHttpClientEndpoint;




class Writer {
    
    public static SSEOutputStream open(IHttpClientEndpoint httpClient, String uri) throws IOException {
        
        ResponseHandler rspHdl = new ResponseHandler();
        BodyDataSink sink = httpClient.send(new HttpRequestHeader("POST", uri), rspHdl);
        
        ErrorThrowingOutputStream eos = new ErrorThrowingOutputStream(Channels.newOutputStream(sink));
        rspHdl.registerErrorHandler(eos);
        
        return new SSEOutputStream(eos);
    }
    
    
  
    

    private static final class ResponseHandler implements IHttpResponseHandler {
        
        
        private final AtomicReference<Consumer<Throwable>> errorConsumerRef = new AtomicReference<>(error -> System.out.println(error));
        
        private void registerErrorHandler(Consumer<Throwable> errorConsumer) {
            this.errorConsumerRef.set(errorConsumer);
        }
        
        
        @Override
        public void onResponse(IHttpResponse response) throws IOException {
            System.out.println(response);
        }
        
        @Override
        public void onException(IOException ioe) throws IOException {
            errorConsumerRef.get().accept(ioe);
        }
    };
    
    
    private static final class ErrorThrowingOutputStream extends FilterOutputStream implements Consumer<Throwable> {
        
        private final AtomicReference<Throwable> throwableRef = new AtomicReference<>();
        
        
        public ErrorThrowingOutputStream(OutputStream os) {
            super(os);
        }
        
        @Override
        public void accept(Throwable t) {
            throwableRef.set(t);
        }
        
        private void checkError() {
            Throwable t = throwableRef.get();
            if (t != null) {
                throw new RuntimeException(t);
            }
        }
        
        
        @Override
        public void write(byte[] b) throws IOException {
            checkError();
            super.write(b);
        }
        
        
        @Override
        public void write(byte[] b, int off, int len) throws IOException {
            checkError();
            super.write(b, off, len);
        }
        
        
        @Override
        public void write(int b) throws IOException {
            checkError();
            super.write(b);
        }
    }  
}



 
