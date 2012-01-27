package edu.baylor.praus;

import java.util.concurrent.LinkedBlockingDeque;
import edu.baylor.praus.websocket.WebSocketFrame;

public class Aiolos {
    private LinkedBlockingDeque<WebSocketFrame> incoming = new LinkedBlockingDeque<>();

    public static void main(String[] args) {
        new Aiolos(args);
    }

    public Aiolos(String[] args) {
        IServerHandler sh = new EchoServer(incoming);
        
        Thread server = new Thread(new Server(sh, parsePort(args)));
        server.start();
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
