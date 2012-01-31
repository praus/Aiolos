package edu.baylor.aiolos.websocket;

import java.nio.channels.AsynchronousSocketChannel;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;

import edu.baylor.aiolos.ClientSession;

/**
 * Encodes the response frame and writes it to the channel while ensuring
 * it will get written all.
 */
public class FrameEncoder extends Encoder {

    /**
     * The responses need to be queued, otherwise we'll get
     * concurrentwriteexception.
     */
    private ConcurrentLinkedQueue<WebSocketFrame> queue = new ConcurrentLinkedQueue<>();
    
    /**
     * If the handler is already writing, don't stack up more reads but just
     * queue them instead.
     */
    private AtomicBoolean writing = new AtomicBoolean(false);
    
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
        } else {
            // The queue is empty, this encoder is done
            writing.set(false);
            writeBuf.clear();
        }
    }
    
    private void writeFromQueue() {
        WebSocketFrame frame = queue.poll();
        if (frame != null) {
            writeBuf.put(frame.encode());
            writeBuf.flip();
            channel.write(writeBuf, attachment, this);
        }
    }
    
    public void respond(WebSocketFrame respFrame) {
        queue.add(respFrame);
        
        if (!writing.getAndSet(true)) {
            writeFromQueue();
        }
    }

    public static void writeResponse(AsynchronousSocketChannel channel,
            ClientSession attachment, WebSocketFrame respFrame) {
        FrameEncoder fe = new FrameEncoder(channel, attachment);
        fe.respond(respFrame);
    }
}
