package edu.baylor.aiolos;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

import edu.baylor.aiolos.websocket.HandshakeDecoder;


public class Server implements Runnable {
    public static final int BUFF_SIZE = 8192;
    public static final String logFile = "server.log";
    public static final Level logLevel = Level.FINEST;

    private int listenPort = 8080;
    private IServerHandler serverHandler;
    

    public Server(IServerHandler sh) {
        this(sh, 8080);
    }

    public Server(IServerHandler serverHandler, int listenPort) {
        this.serverHandler = serverHandler;
    }

    
    @Override
    public void run() {
        final Logger log = configureLogger();

        try {
            final AsynchronousServerSocketChannel listener = AsynchronousServerSocketChannel
                    .open().bind(new InetSocketAddress(listenPort));

            listener.accept(
                    null,
                    new CompletionHandler<AsynchronousSocketChannel, ClientSession>() {

                        @Override
                        public void completed(
                                AsynchronousSocketChannel channel,
                                ClientSession attachment) {
                            try {
                                log.info("Client from "
                                        + channel.getRemoteAddress()
                                        + " connected");
                            } catch (IOException ex) {
                                ex.printStackTrace();
                            }
                            
                            // accept next connection
                            listener.accept(attachment, this);
                            
                            ClientSession att = new ClientSession(serverHandler, channel);
                            HandshakeDecoder.handle(channel, att);
                        }
                        
                        @Override
                        public void failed(Throwable exc,
                                ClientSession attachment) {
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
}