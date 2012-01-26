package edu.baylor.praus;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

import edu.baylor.praus.Aiolos.ServerHandler;
import edu.baylor.praus.websocket.HandshakeDecoder;
import edu.baylor.praus.websocket.WebSocketFrame;

public class Server implements Runnable {
    public static final int BUFF_SIZE = 4096;
    public static final String logFile = "server.log";
    public static final Level logLevel = Level.FINEST;

    private int listenPort = 8080;
    // private ServerHandler serverHandler;
    // private LinkedBlockingDeque<byte[]> outgoing;
    // private LinkedBlockingDeque<WebSocketFrame> incoming;
    private ClientSession session;

    public Server(ServerHandler sh,
            LinkedBlockingDeque<WebSocketFrame> outgoing,
            LinkedBlockingDeque<WebSocketFrame> incoming) {
        this(sh, outgoing, incoming, 8080);
    }

    public Server(ServerHandler sh,
            LinkedBlockingDeque<WebSocketFrame> outgoing,
            LinkedBlockingDeque<WebSocketFrame> incoming, int listenPort) {
        // this.serverHandler = sh;
        // this.outgoing = outgoing;
        // this.incoming = incoming;

        this.session = new ClientSession(sh, outgoing, incoming);
    }

    @Override
    public void run() {
        final Logger log = configureLogger();

        try {
            final AsynchronousServerSocketChannel listener = AsynchronousServerSocketChannel
                    .open().bind(new InetSocketAddress(listenPort));

            listener.accept(
                    this.session,
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
                            listener.accept(null, this);

                            HandshakeDecoder.handle(channel, attachment);
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
}