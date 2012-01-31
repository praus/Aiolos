package edu.baylor.aiolos.websocket;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class WebSocketHandshakeResponse {
    /**
     * WebSocket key from the client
     */
    private String wsKey;

    /**
     * Our computed accept response.
     */
    private String wsAccept;

    /**
     * Status code of this response. Usually 101 Switching Protocols or 400 Bad
     * Request
     */
    private StatusCode statusCode;
    
    private String httpVersion = "HTTP/1.1";

    /**
     * Possible status codes we can respond with.
     */
    public enum StatusCode {
        SwitchingProtocols(101, "Switching Protocols", false),
        BadRequest(400, "Bad Request", true);

        private int statusCode;
        private String reasonPhrase;
        private boolean error; // Indicates whether this response is an error

        StatusCode(int statusCode, String reasonPhrase, boolean error) {
            this.statusCode = statusCode;
            this.reasonPhrase = reasonPhrase;
            this.error = error;
        }

        public String getResponse() {
            return statusCode + " " + reasonPhrase;
        }
        
        public boolean isError() {
            return error;
        }
    }
    
    public static WebSocketHandshakeResponse badRequest() {
        WebSocketHandshakeResponse badRequest = new WebSocketHandshakeResponse(StatusCode.BadRequest);
        return badRequest;
    }

    public WebSocketHandshakeResponse(StatusCode statusCode) {
        this.statusCode = statusCode; 
    }
    
    public WebSocketHandshakeResponse(String wsKey) {
        this.wsKey = wsKey;
        this.statusCode = StatusCode.SwitchingProtocols;
        computeAccept();
    }

    private void computeAccept() {
        if (wsKey == null) {
            wsAccept = "";
            return;
        }
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
        StringBuilder buf = new StringBuilder();
        buf.append(getStatusLine()).append("\r\n");
        if (!isError()) {
            buf.append("Upgrade: websocket").append("\r\n");
            buf.append("Connection: Upgrade").append("\r\n");
            buf.append("Sec-WebSocket-Accept: ").append(wsAccept).append("\r\n");
        }
        buf.append("\r\n"); // end the response
        return buf.toString();
    }

    /**
     * Indicates whether this response is an error
     */
    public boolean isError() {
        return statusCode.isError();
    }
    
    public String getWsKey() {
        return wsKey;
    }

    public void setWsKey(String wsKey) {
        this.wsKey = wsKey;
    }

    public StatusCode getStatusCode() {
        return statusCode;
    }

    public void setStatusCode(StatusCode statusCode) {
        this.statusCode = statusCode;
    }

    public void setWsAccept(String wsAccept) {
        this.wsAccept = wsAccept;
    }

    public String getStatusLine() {
        return httpVersion + " " + statusCode.getResponse();
    }
    
    
}
