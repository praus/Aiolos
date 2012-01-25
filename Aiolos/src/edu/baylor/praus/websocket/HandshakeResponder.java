package edu.baylor.praus.websocket;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.util.logging.Logger;

import edu.baylor.praus.ClientSession;

public class HandshakeResponder implements CompletionHandler<Integer, ClientSession> {
    public static final Logger log = Logger.getLogger("aiolos.networking");
    protected final AsynchronousSocketChannel channel;
    protected ClientSession attachment;
    protected final ByteBuffer buf;
    private static final int BUFF_SIZE = 4096;
    
    public static void create(ClientSession attachment,
            AsynchronousSocketChannel channel) {
        HandshakeResponder r = new HandshakeResponder(attachment, channel);
        WebSocketHandshakeRequest wsRequest = attachment.getHandshakeRequest();
        r.respond(wsRequest);
    }
    
    public void respond(WebSocketHandshakeRequest wsRequest) {
        // create the response
        if (wsRequest != null) {
            WebSocketHandshakeResponse r =
                    new WebSocketHandshakeResponse(wsRequest.getWsKey());
            buf.put(r.getResponse().getBytes());
            buf.flip();
            log.info("Response sent");
            
            channel.write(buf, attachment, this);
        } else {
            // request is somehow malformed, respond with 400, bad request
        }
    }
    
    public HandshakeResponder(ClientSession attachment,
            AsynchronousSocketChannel channel) {
        this.channel = channel;
        this.attachment = attachment;
        // TODO: don't create the buffer each time, reuse it
        this.buf = ByteBuffer.allocateDirect(BUFF_SIZE);
    }

    @Override
    public void completed(Integer result, ClientSession attachment) {
        if (result == -1) {
            try {
                log.info("Client " + channel.getRemoteAddress() + " disconnected abruptly.");
                channel.close();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
            return;
        }
        this.attachment = attachment;
        
        // check if we have something to write
        if (buf.hasRemaining()) {
            channel.write(buf, attachment, this);
        } else {
            // connection is established and we'll hand control to the
            // frame handler
            FrameDecoder.handle(channel, attachment);
        }
    }

    @Override
    public void failed(Throwable exc, ClientSession attachment) {
        exc.printStackTrace();
        log.warning(exc.getMessage());
    }
}
