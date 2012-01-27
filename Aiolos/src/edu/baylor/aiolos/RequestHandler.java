package edu.baylor.aiolos;
//package edu.baylor.praus;
//
//import java.io.IOException;
//import java.nio.ByteBuffer;
//import java.nio.channels.AsynchronousSocketChannel;
//import java.nio.channels.CompletionHandler;
//import java.util.logging.Logger;
//
//import edu.baylor.praus.exceptions.InvalidRequestException;
//
//public class RequestHandler implements CompletionHandler<Integer, ClientSession> {
//
//    private static final int BUFF_SIZE = 4096;
//    private final Logger log = Logger.getLogger("aiolos.networking");
//    private ClientSession attachment;
//    private boolean shouldClose = false;
//    
//    private final AsynchronousSocketChannel channel;
//    private volatile boolean isReading;
//    private final ByteBuffer buf;
//    
//    
//    public RequestHandler(AsynchronousSocketChannel channel) {
//        this.channel = channel;
//        this.buf = ByteBuffer.allocateDirect(BUFF_SIZE);
//    }
//    
//    public static void handle(AsynchronousSocketChannel channel) {
//        new RequestHandler(channel).startReading();
//    }
//    
//    private void startReading() {
//        buf.rewind();
//        buf.limit(buf.capacity());
//        isReading = true;
//        channel.read(buf, new ClientSession(), this);
//    }
//    
//    @Override
//    public void completed(Integer result, ClientSession att) {
//        attachment = att;
//        shouldClose = false;
//        
//        if (result == -1) {
//            try {
//                log.info("Client " + channel.getRemoteAddress() + " disconnected abruptly.");
//                channel.close();
//            } catch (IOException ex) {
//                ex.printStackTrace();
//            }
//            return;
//        }
//        
//        
//        
//        if (attachment.isConnected()) {
//            try {
//                WebSocketFrame wsRequest = WebSocketFrame.decode(readBuff);
//            
//                System.out.format("REQUEST: %s\n", wsRequest);
//                String message = new String(wsRequest.getData().array());
//                System.out.format("[%s] %s\n", message.length(), message);
//                
//                if (wsRequest.isClose()) {
//                    closeChannel();
//                    return;
//                }
//                
//                WebSocketFrame wsResponse = new WebSocketFrame(wsRequest.getData());
//                //WebSocketFrame wsResponse = WebSocketFrame.createMessage("Ahoj svete");
//                System.out.format("RESPONSE: %s\n", wsResponse);
//                
//                responseBuff.put(wsResponse.encode());
//                
//            } catch (InvalidRequestException ex) {
//                ex.printStackTrace();
//            }
//            
//        } else { // handshake
//            WebSocketHandshakeRequest wshRequest = null;
//            try {
//                wshRequest = WebSocketHandshakeRequest.decode(readBuff);
//                String wsKey = wshRequest.getWsKey();
//                attachment.setConnected(true);
//                
//                attachment.setHandshakeRequest(wshRequest);
//                
//                log.info(wshRequest.toString());
//                
//                WebSocketHandshakeResponse r = new WebSocketHandshakeResponse(wsKey);
//                responseBuff.put(r.getResponse().getBytes());
//                log.info("Response sent");
//                
//                
//            } catch (InvalidRequestException e) {
//                log.info("Invalid request");
//                closeChannel(); // disconnect misbehaving client
//                e.printStackTrace();
//            }
//        }
//        buf.flip();
//        channel.write(buf);
//        
//        if (shouldClose) {
//            closeChannel();
//            return;
//        }
//        
//        channel.read(buf, attachment, this);
//    }
//    
//    private void writeResponse() {
//        
//    }
//    
//    private void closeChannel() {
//        try {
//            log.info("Disconnecting client " + channel.getRemoteAddress()); 
//            channel.close();
//        } catch (IOException e) {
//            log.warning(e.getMessage());
//        }
//    }
//    
//    @Override
//    public void failed(Throwable exc, ClientSession attachment) {
//        exc.printStackTrace();
//        log.warning(exc.getMessage());
//    }
//}
