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






import java.util.concurrent.ConcurrentLinkedQueue;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.xlightweb.GetRequest;
import org.xlightweb.PostRequest;
import org.xlightweb.client.HttpClient;
import org.xlightweb.client.IHttpClientEndpoint;

import com.google.common.base.Joiner;
import com.google.common.collect.Queues;



@Ignore
public class SSEReadpushbackTest {

    private static SSEServer sseServer;
    
    
    @BeforeClass
    public static void setUp() throws Exception {
        sseServer = new SSEServer(0);
        sseServer.start();
    }

    
    @AfterClass
    public static void tearDown() throws Exception {
        sseServer.stop();
    }

    
    @Test
    public void testReadpushback() throws Exception {
        
        IHttpClientEndpoint httpClient = new HttpClient();
        ConcurrentLinkedQueue<SSEEvent> events = Queues.newConcurrentLinkedQueue();
        
        
        // start reader
        Reader.start(httpClient, sseServer.getDefaultBaseUrl() + "/ssechannel/event", events);

        
        // start writer
        SSEOutputStream sseOut = Writer.open(httpClient, sseServer.getDefaultBaseUrl() + "/ssechannel/event");
        
        
        System.out.println("sending events");
        for (int i = 0; i < 1000; i++) {
            SSEEvent event = SSEEvent.newEvent().data("testa" + i);
            sseOut.write(event);
            events.add(event);
        }
        

        // check if all events received
        System.out.println("waiting for response");
        waitingForAllReceivedResponses(events, 10);

        
        
        System.out.println("Suspending server-side read");
        httpClient.call(new PostRequest(sseServer.getDefaultBaseUrl() + "/ssechannel/control/suspension"));
        
        
        
        System.out.println("sending much more events");
        for (int i = 0; i < 3000; i++) {
            SSEEvent event = SSEEvent.newEvent().data("testb" + i);
            sseOut.write(event);
            events.add(event);
        }
        
        
        
        System.out.println("resuming server-side read");
        httpClient.call(new PostRequest(sseServer.getDefaultBaseUrl() + "/ssechannel/control/resumption"));

        
        System.out.println("waiting for response");
        waitingForAllReceivedResponses(events, 10);
        
        System.out.println(httpClient.call(new GetRequest(sseServer.getDefaultBaseUrl() + "/ssechannel/info")).getBody());        
    }
    
    
    
    private void waitingForAllReceivedResponses(ConcurrentLinkedQueue<SSEEvent> events, int maxSec)  {
        
        // check if all events received
        for (int i = 0; i < maxSec * 10; i++) {
            if (events.size() == 0) {
                return;
            }
            
            try {
                Thread.sleep(100);
            } catch (InterruptedException ignore) { }
        }
        
        Assert.fail("missing events " + Joiner.on(", ").join(events));
    }
}



 
