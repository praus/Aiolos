package edu.baylor.praus;

import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.logging.Level;
import java.util.logging.Logger;

import edu.baylor.praus.websocket.FrameEncoder;
import edu.baylor.praus.websocket.WebSocketFrame;

public class EchoServer implements IServerHandler {
    
    public final Logger log = Logger.getLogger("aiolos.handler.echo");
    
    LinkedBlockingDeque<WebSocketFrame> incoming;
    
    
    public EchoServer(LinkedBlockingDeque<WebSocketFrame> incoming) {
        this.incoming = incoming;
    }

    @Override
    public void receive(ClientSession attachment) {
        try {
            WebSocketFrame frame = incoming.take();
            ByteBuffer data = frame.getDataCopy();
            byte[] d = new byte[data.limit()];
            data.get(d);
            String msg = new String(d);
            log.log(Level.INFO, "Echo: {0}", msg);

            WebSocketFrame respFrame = WebSocketFrame.createMessage(msg);
            
            AsynchronousSocketChannel channel = attachment.getChannel();
            channel.write(respFrame.encode(), attachment, new FrameEncoder(channel, attachment));
            
        } catch (InterruptedException e) {
        }
    }

    @Override
    public LinkedBlockingDeque<WebSocketFrame> getQueue() {
        return incoming;
    }
}