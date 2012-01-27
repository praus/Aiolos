package edu.baylor.praus.websocket;

import java.nio.channels.AsynchronousSocketChannel;
import edu.baylor.praus.ClientSession;

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
