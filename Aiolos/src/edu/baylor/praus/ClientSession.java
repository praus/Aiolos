package edu.baylor.praus;


public class ClientSession {
    private WebSocketHandshakeRequest handshakeRequest;
    
    /**
     * Handshake has been performed successfuly, continue with frames 
     */
    private boolean connected;
    
    public ClientSession() {
    }
    
    public WebSocketHandshakeRequest getHandshakeRequest() {
        return handshakeRequest;
    }

    public void setHandshakeRequest(WebSocketHandshakeRequest request) {
        this.handshakeRequest = request;
    }
    
    public boolean isConnected() {
        return connected;
    }

    public void setConnected(boolean connected) {
        this.connected = connected;
    }
    
}
