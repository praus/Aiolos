package edu.baylor.praus;

import java.nio.channels.AsynchronousSocketChannel;

public class FrameDecoder extends Decoder {

    public FrameDecoder(AsynchronousSocketChannel channel) {
        super(channel);
    }

}
