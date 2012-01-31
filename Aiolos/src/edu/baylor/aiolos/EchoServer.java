package edu.baylor.aiolos;

import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.logging.Level;
import java.util.logging.Logger;

import edu.baylor.aiolos.websocket.CloseHandler;
import edu.baylor.aiolos.websocket.FrameEncoder;
import edu.baylor.aiolos.websocket.WebSocketFrame;

/**
 * This is a very simple handler of the events from the handlers -- it just
 * echoes the message back.
 */
public class EchoServer implements IServerHandler {

    public static final Logger log = Logger.getLogger("aiolos.handler.echo");

    LinkedBlockingDeque<WebSocketFrame> incoming;

    public EchoServer(LinkedBlockingDeque<WebSocketFrame> incoming) {
        this.incoming = incoming;
    }

    /**
     * @return true if the handler wishes to continue in communication
     */
    @Override
    public boolean receive(ClientSession attachment) {
        try {
            WebSocketFrame frame = incoming.take();
            AsynchronousSocketChannel channel = attachment.getChannel();

            if (frame.isClose()) {
                WebSocketFrame closeFrame = WebSocketFrame.closeFrame();
                channel.write(closeFrame.encode(), attachment,
                        new CloseHandler(channel, attachment));
                return false;
            }

            ByteBuffer data = frame.getDataCopy();
            byte[] d = new byte[data.limit()];
            data.get(d);
            String msg = new String(d);
            log.log(Level.INFO, "Echoing: {0}", msg);

            WebSocketFrame respFrame = WebSocketFrame.message(msg);
            log.log(Level.FINE, "Echo frame: {0}", respFrame);
            FrameEncoder.writeResponse(channel, attachment, respFrame);

        } catch (InterruptedException e) {
            receive(attachment); // try waiting again
        }
        return true;
    }

    @Override
    public LinkedBlockingDeque<WebSocketFrame> getQueue() {
        return incoming;
    }
}