package edu.baylor.praus.websocket;

import java.nio.channels.AsynchronousSocketChannel;
import java.util.logging.Logger;

import edu.baylor.praus.ClientSession;

public class HandshakeResponder extends Encoder {

    public static final Logger log = Logger.getLogger("aiolos.networking");
    
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
            writeBuf.put(r.getResponse().getBytes());
            writeBuf.flip();
            log.info("Response sent");
            
            channel.write(writeBuf, attachment, this);
        } else {
            // request is somehow malformed, respond with 400, bad request
        }
    }
    
    public HandshakeResponder(ClientSession attachment,
            AsynchronousSocketChannel channel) {
        super(channel, attachment);
    }

    @Override
    public void completed(Integer result, ClientSession attachment) {
        super.completed(result, attachment);
        
        // check if we have something to write
        if (writeBuf.hasRemaining()) {
            channel.write(writeBuf, attachment, this);
        } else {
            // connection is established and we'll hand control to the
            // frame handler
            FrameDecoder.handle(channel, attachment);
        }
    }

    
}
