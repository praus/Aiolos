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
    }

    @Override
    public void completed(Integer result, ClientSession attachment) {
        super.completed(result, attachment);
        
//        writeMessage();
    }
    
//    public static void handle(AsynchronousSocketChannel channel, ClientSession attachment) {
//        FrameEncoder fe = new FrameEncoder(channel, attachment);
//        attachment.setEncoder(fe);
//        fe.writeMessage();
//    }
    
}
