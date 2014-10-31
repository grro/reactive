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











import java.io.File;
import java.lang.reflect.Field;
import java.net.ServerSocket;
import java.nio.channels.ServerSocketChannel;

import org.apache.catalina.LifecycleException;
import org.apache.catalina.connector.Connector;
import org.apache.catalina.startup.Tomcat;
import org.apache.coyote.AbstractProtocol;



public class SSEServer {

    final static String CTX = "/sse"; 
    private final Tomcat server;
    
    
   
    public SSEServer(int port) throws Exception  {
        server = new Tomcat();
        server.setPort(port);
        server.addWebapp(CTX, new File("src/test/resources/webapp").getAbsolutePath());
    }

    
    public void start() throws LifecycleException {
        server.start();
    }
    
    
    public void stop() throws LifecycleException {
        server.stop();
    }
    
    


    public int getPort() {
        return computeLocalPort(server.getConnector());
    }


    /*
     * @see http://code.google.com/p/google-web-toolkit/source/browse/releases/2.0/dev/core/src/com/google/gwt/dev/shell/tomcat/EmbeddedTomcatServer.java?r=6710
     */
    private int computeLocalPort(Connector connector) {
      Throwable caught = null;
      try {
          Field phField = Connector.class.getDeclaredField("protocolHandler");
          phField.setAccessible(true);
          Object protocolHandler = phField.get(connector);

          Field epField = AbstractProtocol.class.getDeclaredField("endpoint");
          epField.setAccessible(true);
          Object endPoint = epField.get(protocolHandler);

          // assume connector is bio connector
          try {
              Field ssField = endPoint.getClass().getDeclaredField("serverSocket");
              ssField.setAccessible(true);
              ServerSocket serverSocket = (ServerSocket) ssField.get(endPoint);
              return serverSocket.getLocalPort();
          } catch (NoSuchFieldException nf) {

              // assume connector is nio connector
              Field ssField = endPoint.getClass().getDeclaredField("serverSock");
              ssField.setAccessible(true);
              ServerSocketChannel serverSocketChannel = (ServerSocketChannel) ssField.get(endPoint);
              return serverSocketChannel.socket().getLocalPort();
          }

      } catch (SecurityException e) {
        caught = e;
      } catch (NoSuchFieldException e) {
        caught = e;
      } catch (IllegalArgumentException e) {
        caught = e;
      } catch (IllegalAccessException e) {
        caught = e;
      }
      throw new RuntimeException(
          "Failed to retrieve the startup port from Embedded Tomcat", caught);
    }



    public String getDefaultBaseUrl() {
        return "http://localhost:" + getPort() + CTX;
    }
}



