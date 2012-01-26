package edu.baylor.praus;


import java.nio.ByteBuffer;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.logging.Level;
import java.util.logging.Logger;

import edu.baylor.praus.websocket.WebSocketFrame;

public class Aiolos {
    private LinkedBlockingDeque<WebSocketFrame> outgoing = new LinkedBlockingDeque<>();
    private LinkedBlockingDeque<WebSocketFrame> incoming = new LinkedBlockingDeque<>();

    public static void main(String[] args) {
        new Aiolos(args);
    }

    public Aiolos(String[] args) {
        Thread server = new Thread(
                new Server(new EchoServer(), outgoing, incoming,parsePort(args))
                );
        server.start();
    }

    public interface ServerHandler {
        public void receive();
    }

    public class EchoServer implements ServerHandler {
        
        public final Logger log = Logger.getLogger("aiolos.handler");
        
        @Override
        public void receive() {         
            try {
                WebSocketFrame frame = incoming.take();
                ByteBuffer data = frame.getDataCopy();
                byte[] d = new byte[data.limit()];
                data.get(d);
                String msg = new String(d);
                log.log(Level.INFO, "Echo: {0}", msg);
                
                WebSocketFrame respFrame = WebSocketFrame.createMessage(msg);
                outgoing.put(respFrame);
            } catch (InterruptedException e) {
            }
        }

    }

    private static int parsePort(String[] args) {
        if (args.length >= 1) {
            try {
                return Integer.parseInt(args[0]);
            } catch (NumberFormatException ex) {
            }
        }
        return 8080;
    }

}
