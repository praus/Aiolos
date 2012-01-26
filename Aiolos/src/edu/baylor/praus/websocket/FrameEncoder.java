package edu.baylor.praus.websocket;

import java.nio.channels.AsynchronousSocketChannel;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.logging.Level;

import edu.baylor.praus.ClientSession;

public class FrameEncoder extends Encoder {
    
    LinkedBlockingDeque<WebSocketFrame> out;
    
    public FrameEncoder(AsynchronousSocketChannel channel,
            ClientSession attachment) {
        super(channel, attachment);
        out = attachment.getOutgoing();
    }

    @Override
    public void completed(Integer result, ClientSession attachment) {
        super.completed(result, attachment);
        
        writeMessage();
    }
    
    protected void writeMessage() {
        
        WebSocketFrame response = out.poll();
        if (response != null) {
            log.log(Level.INFO, "Writing {0}", response);
            channel.write(response.encode(), attachment, this);            
        }
//        else {
//            // nothing to write from the queue, back to reading
//            attachment.getDecoder().startReading();
//        }
    }
    
    public static void handle(AsynchronousSocketChannel channel, ClientSession attachment) {
        FrameEncoder fe = new FrameEncoder(channel, attachment);
        attachment.setEncoder(fe);
        fe.writeMessage();
    }
    
}
