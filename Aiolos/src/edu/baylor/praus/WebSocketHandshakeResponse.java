package edu.baylor.praus;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

//import org.apache.commons.codec.binary.Base64;

public class WebSocketHandshakeResponse {
    private String wsKey;
    private String wsAccept;
    private String statusLine = "HTTP/1.1 101 Switching Protocols";
    
    public WebSocketHandshakeResponse(String wsKey) {
        this.wsKey = wsKey;
        computeAccept();
    }
    
    private void computeAccept() {
        String guid = "258EAFA5-E914-47DA-95CA-C5AB0DC85B11";
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            byte[] hash = md.digest(wsKey.concat(guid).getBytes());
            wsAccept = javax.xml.bind.DatatypeConverter.printBase64Binary(hash);
        } catch (NoSuchAlgorithmException e) {
        }
    }

    public String getWsAccept() {
        return wsAccept;
    }
    
    public String getResponse() {
        StringBuilder buf = new StringBuilder(100);
        buf.append(statusLine).append("\r\n");
        buf.append("Upgrade: websocket").append("\r\n");
        buf.append("Connection: Upgrade").append("\r\n");
        buf.append("Sec-WebSocket-Accept: ").append(wsAccept).append("\r\n\r\n");
        return buf.toString();
    }
}
