package edu.baylor.aiolos.websocket;

import java.nio.channels.AsynchronousSocketChannel;

import edu.baylor.aiolos.ClientSession;

public class FrameEncoder extends Encoder {

    public FrameEncoder(AsynchronousSocketChannel channel,
            ClientSession attachment) {
        super(channel, attachment);
    }

    @Override
    public void completed(Integer result, ClientSession attachment) {
        super.completed(result, attachment);
    }
}
