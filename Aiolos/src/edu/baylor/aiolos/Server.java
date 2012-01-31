package edu.baylor.aiolos;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.util.logging.Logger;

import edu.baylor.aiolos.websocket.FrameEncoder;
import edu.baylor.aiolos.websocket.HandshakeDecoder;

/**
 * Main server thread for accepting new connections and handing them over to the
 * handshake decoder.
 */
public class Server implements Runnable {
    /**
     * Default size of the buffer for incoming messages
     */
    public static final int BUFF_SIZE = 65536;

    private int listenPort = 8080; // default port for listening
    
    /**
     * Handler that will get all the messages
     */
    private IServerHandler serverHandler;

    public Server(IServerHandler sh) {
        this(sh, 8080);
    }

    public Server(IServerHandler serverHandler, int listenPort) {
        this.serverHandler = serverHandler;
    }

    @Override
    public void run() {
        final Logger log = Logger.getLogger("aiolos.server");

        try {
            final AsynchronousServerSocketChannel listener = AsynchronousServerSocketChannel
                    .open().bind(new InetSocketAddress("127.0.0.2", listenPort));

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

                            // and hand it over to the handshake decoder
                            ClientSession att = new ClientSession(
                                    serverHandler, channel);
                            att.setEncoder(new FrameEncoder(channel, attachment));
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
}