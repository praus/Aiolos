package edu.baylor.praus;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;


public class Aiolos {
    
    public static int listenPort = 8080;
    public static String logFile = "server.log";
    public static Level logLevel = Level.FINEST;
    
    public static void main(String[] args) {
        final Logger log = configureLogger();
        parseArgs(args);

        try {
            final AsynchronousServerSocketChannel listener =
                    AsynchronousServerSocketChannel.open().bind(
                            new InetSocketAddress(listenPort));
            
            listener.accept(null, new CompletionHandler<AsynchronousSocketChannel, Void>() {

                @Override
                public void completed(AsynchronousSocketChannel channel,
                        Void attachment) {
                    try {
                        log.info("Client from " + channel.getRemoteAddress() + " connected");
                    } catch (IOException ex) {
                        ex.printStackTrace();
                    }
                    
                    // accept next connection
                    listener.accept(null, this);
                    
//                    RequestHandler.handle(channel);
                    HandshakeDecoder.handle(channel);
                }

                @Override
                public void failed(Throwable exc, Void attachment) {
                    log.warning(exc.getMessage());
                }
                
            });
            
            // wait indefinitely
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
            return;
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
