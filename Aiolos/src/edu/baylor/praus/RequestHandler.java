package edu.baylor.praus;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
            WebSocketFrame frame = WebSocketFrame.decode(readBuff);
            System.out.println(new String(frame.getData().array()));
            readBuff.flip();
            
            ByteBuffer response = WebSocketFrame.encode("Ahoj svete");
//            byte[] dst = new byte[4];
//            response.get(dst);
//            response.rewind();
            responseBuff.put(response);
            shouldClose = true;
            
        } else { // handshake
            String handshakeRequest = extractByteBuffer(readBuff);
            WebSocketHandshakeRequest wsRequest = null;
            try {
                wsRequest = parseHandshake(handshakeRequest);
                String wsKey = wsRequest.getWsKey();
                attachment.setConnected(true);
                
                attachment.setRequest(wsRequest);
                
                log.info(wsRequest.toString());
                
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
    
    
    private WebSocketHandshakeRequest parseHandshake(String plainRequest)
            throws InvalidRequestException {
        /**
            GET / HTTP/1.1
            Upgrade: websocket
            Connection: Upgrade
            Host: 66.90.194.192
            Origin: http://websocket.org
            Sec-WebSocket-Key: 6+m3ssD7l08m+9f5bhCaOA==
            Sec-WebSocket-Version: 13
         */
        
        final List<String> lines =
                new ArrayList<String>(Arrays.asList(plainRequest.split("\r\n")));
        
        Matcher m = null;
        
        // Request-Line (GET / HTTP/1.1)
        Pattern requestLinePattern = Pattern.compile(
                "^(?<method>GET)[ ](?<uri>[/][\\w]*)[ ]HTTP/1.1$");
        m = requestLinePattern.matcher(lines.remove(0));
        String method = null;
        String uri = null;
        if (m.matches()) {
            method = m.group("method");
            uri = m.group("uri");
        } else {
            log.info(lines.toString());
            throw new InvalidMethodException();
        }
        
        WebSocketHandshakeRequest wsRequest = new WebSocketHandshakeRequest(method, uri);
        
        for (String line: lines) {
            if (line.trim().equals("")) // skip empty lines
                continue;
            
            String[] s = line.split(":", 2);
            if (s.length != 2)
                throw new InvalidWebSocketRequestException();
            String fieldName = s[0].trim();
            String fieldValue = s[1].trim();
            
            switch (fieldName.toLowerCase()) {
                case "connection":
                    wsRequest.setConnection(fieldValue);
                    break;
                case "upgrade":
                    wsRequest.setUpgrade(fieldValue);
                    break;
                case "sec-websocket-key":
                    wsRequest.setWsKey(fieldValue);
                    break;
                case "sec-websocket-version":
                    wsRequest.setWsVersion(fieldValue);
                    break;
            }
        }
        
        // Upgrade to WS connection
        if (wsRequest.isUpgraded()) {
            // fail, client does not support WebSocket
            throw new ClientDoesNotSupportWebSocketException();
        }
        
        return wsRequest;
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

    private static String extractByteBuffer(ByteBuffer buff) {
        buff.flip();
        return new String(buff.array());
    }
}
