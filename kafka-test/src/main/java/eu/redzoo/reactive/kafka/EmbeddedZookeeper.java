package eu.redzoo.reactive.kafka;


import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Random;

import kafka.utils.Utils;

import org.apache.zookeeper.server.NIOServerCnxnFactory;
import org.apache.zookeeper.server.ZooKeeperServer;


public class EmbeddedZookeeper {

    private final int port;
    private final File snapshotDir;
    private final File logDir;
    private final NIOServerCnxnFactory factory;

   
    public EmbeddedZookeeper(int port) throws IOException {
        this.port = port;
        this.snapshotDir = getTempDir();
        this.logDir = getTempDir();
        this.factory = new NIOServerCnxnFactory();
        factory.configure(new InetSocketAddress("localhost", port), 1024);
   
    }

    public int getPort() {
        return port;
    }
    
    
    public void start() throws IOException {
        try {
            int tickTime = 500;
            factory.startup(new ZooKeeperServer(snapshotDir, logDir, tickTime));
        } catch (InterruptedException e) {
            throw new IOException(e);
        }     
    }
    
    
    /**
     * Shuts down the embedded Zookeeper instance.
     */ 
    public void shutdown() {
        factory.shutdown();
        Utils.rm(snapshotDir);
        Utils.rm(logDir);
    }
    
    public String getConnection() {
        return "localhost:" + port;
    }
    
    
    
    private static File getTempDir() {
        File file = new File(System.getProperty("java.io.tmpdir"), "_test_" + new Random().nextInt(10000000));
        if (!file.mkdirs()) {
            throw new RuntimeException("could not create temp directory: " + file.getAbsolutePath());
        }
        file.deleteOnExit();
        return file;
    }
 }



