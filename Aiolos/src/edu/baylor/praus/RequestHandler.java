package edu.baylor.praus;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.util.logging.Logger;

import edu.baylor.praus.exceptions.InvalidRequestException;

public class RequestHandler implements CompletionHandler<Integer, ClientSession> {

    private static final int BUFF_SIZE = 512;
    private Logger log;
    private ClientSession attachment;
    private boolean shouldClose = false;
    
    @Override
    public void completed(Integer result, ClientSession att) {
        attachment = att;
        log = attachment.getLogger();
        shouldClose = false;
        
        if (result == -1) {
            try {
                log.info("Client " + attachment.getChannel().getRemoteAddress() + "  disconnected abruptly.");
                attachment.getChannel().close();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
            return;
        }

        ByteBuffer readBuff = attachment.getBuffer();
        ByteBuffer responseBuff = ByteBuffer.allocate(BUFF_SIZE);
        
        if (attachment.isConnected()) {
            try {
                WebSocketFrame wsRequest = WebSocketFrame.decode(readBuff);
            
                System.out.format("REQUEST: %s\n", wsRequest);
                //wsRequest.getData().rewind();
                String message = new String(wsRequest.getData().array());
                System.out.format("[%s] %s\n", message.length(), message);
                
                if (wsRequest.isClose()) {
                    closeChannel();
                    return;
                }
                
                // WebSocketFrame wsResponse = new WebSocketFrame(wsRequest.getData());
                WebSocketFrame wsResponse = WebSocketFrame.createMessage("Ahoj svete");
                System.out.format("RESPONSE: %s\n", wsResponse);
                
                responseBuff.put(wsResponse.encode());
                
            } catch (InvalidRequestException ex) {
                ex.printStackTrace();
            }
            
        } else { // handshake
            WebSocketHandshakeRequest wshRequest = null;
            try {
                wshRequest = WebSocketHandshakeRequest.decode(readBuff);
                String wsKey = wshRequest.getWsKey();
                attachment.setConnected(true);
                
                attachment.setRequest(wshRequest);
                
                log.info(wshRequest.toString());
                
                WebSocketHandshakeResponse r = new WebSocketHandshakeResponse(wsKey);
                responseBuff.put(r.getResponse().getBytes());
                log.info("Response sent");
                
                
            } catch (InvalidRequestException e) {
                log.info("Invalid request");
                closeChannel(); // disconnect misbehaving client
                e.printStackTrace();
            }
        }
        responseBuff.flip();
        attachment.getChannel().write(responseBuff);
        
        if (shouldClose) {
            closeChannel();
            return;
        }
        
        // next read from this client
        ByteBuffer nextBuff = ByteBuffer.allocate(BUFF_SIZE);
        attachment.setBuffer(nextBuff);
        
        attachment.getChannel().read(nextBuff, attachment, new RequestHandler());
    }
    
    private void closeChannel() {
        try {
            log.info("Disconnecting client " + attachment.getChannel().getRemoteAddress());
            AsynchronousSocketChannel channel = attachment.getChannel(); 
            channel.close();
        } catch (IOException e) {
            log.warning(e.getMessage());
        }
    }
    
    @Override
    public void failed(Throwable exc, ClientSession attachment) {
        exc.printStackTrace();
        attachment.getLogger().warning(exc.getMessage());
    }
}
