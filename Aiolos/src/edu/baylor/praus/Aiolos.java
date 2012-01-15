package edu.baylor.praus;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.AsynchronousChannelGroup;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.util.concurrent.Executors;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;


public class Aiolos {
    
    public static int listenPort = 8080;
    public static String logFile = "server.log";
    public static Level logLevel = Level.FINEST;
    public static int threadPoolSize = 50;
    
    public static void main(String[] args) {
        Logger log = configureLogger();
        parseArgs(args);

        try {
            final AsynchronousChannelGroup acg =
                    AsynchronousChannelGroup.withThreadPool(
                            Executors.newFixedThreadPool(threadPoolSize));
            
            final AsynchronousServerSocketChannel listener =
                    AsynchronousServerSocketChannel.open(acg).bind(
                            new InetSocketAddress(listenPort));

            ClientSession attachment = new ClientSession(listener, log);
            listener.accept(attachment, new ConnectionHandler());

            while (true) {
                Object lock = new Object();
                synchronized (lock) {
                    try {
                        lock.wait();
                    } catch (InterruptedException e) {
                    }
                }
            }
        } catch (IOException e) {
            log.severe(e.getMessage());
        }
    }
    
    
    private static Logger configureLogger() {
        Logger log = Logger.getLogger("aiolos.network");
        FileHandler fh;

        try {
          // configures the logger with handler and formatter
          fh = new FileHandler(logFile, true);
          log.addHandler(fh);
          log.setLevel(logLevel);
          SimpleFormatter formatter = new SimpleFormatter();
          fh.setFormatter(formatter);
        } catch (SecurityException | IOException e) {
          e.printStackTrace();
        }
        return log;
    }
    
    private static void parseArgs(String[] args) {
        if (args.length >= 1) {
            Aiolos.listenPort = Integer.parseInt(args[0]);
        }
    }
}
