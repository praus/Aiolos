package edu.baylor.aiolos.websocket;

import java.nio.channels.AsynchronousSocketChannel;

import edu.baylor.aiolos.ClientSession;

/**
 * Encodes the response frame and writes it to the channel while ensuring
 * it will get written all
 */
public class FrameEncoder extends Encoder {

    public FrameEncoder(AsynchronousSocketChannel channel,
            ClientSession attachment) {
        super(channel, attachment);
    }

    @Override
    public void completed(Integer result, ClientSession attachment) {
        super.completed(result, attachment);

        // check if we have some leftover to write
        if (writeBuf.hasRemaining()) {
            channel.write(writeBuf, attachment, this);
        }
        // If not, then this encoder is done, we can end and get eaten by the
        // garbage collector.
    }
    
    protected void response(WebSocketFrame respFrame) {
        writeBuf.put(respFrame.encode());
        channel.write(writeBuf, attachment, this);
    }

    public static void writeResponse(AsynchronousSocketChannel channel,
            ClientSession attachment, WebSocketFrame respFrame) {
        FrameEncoder fe = new FrameEncoder(channel, attachment);
        fe.response(respFrame);
    }
}
