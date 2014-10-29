package eu.redzoo.reactive.kafka.rest;






import java.io.OutputStream;
import java.net.Socket;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;



public class EventbrokerServerTest {

    private static EventbrokerServer eventbrokerServer;
    
    
    @BeforeClass
    public static void setUp() throws Exception {
        
        eventbrokerServer = new EventbrokerServer(9777);
        eventbrokerServer.start();
    }

    
    @AfterClass
    public static void tearDown() throws Exception {
        eventbrokerServer.stop();
    }

    
    @Test
    public void testSimple() throws Exception {
     
        String eventsUri = eventbrokerServer.getDefaultBaseUrl() + "/rest/topic/test/events";
        
        System.out.println("curl " + eventsUri);
        
        
        final Socket s = new Socket("localhost", eventbrokerServer.getPort());
        OutputStream os = s.getOutputStream();
        
        os.write(new String("POST " + EventbrokerServer.CTX + "/rest/topic/test/events HTTP/1.1\r\n" +
                            "host: localhost:" + eventbrokerServer.getPort() + "\r\n" +
                            "user-agent: me\r\n" +
                            "Content-type: text/event-stream\r\n" +
                            "Content-length: 456456456464\r\n" +
                             "\r\n").getBytes("US-ASCII"));
        os.flush();
        
        
   
        
        for (int i = 0; i < 10000; i++) {
            try {
                Thread.sleep(500);
            } catch (InterruptedException ignore) { }
            
            
            os.write(new String("id: " + i + "\r\n" +
                                "data: test " + i + "test\r\n\r\n").getBytes("UTF-8"));
            os.flush();
        }
            
        
        s.close();
        System.out.println("exit");
    }
}



 