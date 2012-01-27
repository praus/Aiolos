package edu.baylor.aiolos;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.logging.Level;
import java.util.logging.Logger;

import edu.baylor.aiolos.websocket.FrameEncoder;
import edu.baylor.aiolos.websocket.WebSocketFrame;

public class EchoServer implements IServerHandler {
    
    public final Logger log = Logger.getLogger("aiolos.handler.echo");
    
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
                channel.close();
                return false;
            }
            
            ByteBuffer data = frame.getDataCopy();
            byte[] d = new byte[data.limit()];
            data.get(d);
            String msg = new String(d);
            log.log(Level.INFO, "Echo: {0}", msg);

            WebSocketFrame respFrame = WebSocketFrame.createMessage(msg);
            
            
            channel.write(respFrame.encode(), attachment, new FrameEncoder(channel, attachment));
            
        } catch (InterruptedException e) {
        } catch (IOException e) {
            log.info("Connection was closed gracefully");
        }
        return true;
    }

    @Override
    public LinkedBlockingDeque<WebSocketFrame> getQueue() {
        return incoming;
    }
}