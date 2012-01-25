package edu.baylor.praus;

import java.util.concurrent.LinkedBlockingDeque;

import edu.baylor.praus.Aiolos.ServerHandler;
import edu.baylor.praus.websocket.WebSocketFrame;
import edu.baylor.praus.websocket.WebSocketHandshakeRequest;


public class ClientSession {
    private WebSocketHandshakeRequest handshakeRequest;
    
    /**
     * Handshake has been performed successfuly, continue with frames 
     */
    private boolean connected;
    private ServerHandler serverHandler;
    private LinkedBlockingDeque<byte[]> outgoing;
    private LinkedBlockingDeque<WebSocketFrame> incoming;
    
    public ClientSession(ServerHandler serverHandler,
            LinkedBlockingDeque<byte[]> outgoing,
            LinkedBlockingDeque<WebSocketFrame> incoming) {
        this.serverHandler = serverHandler;
        this.outgoing = outgoing;
        this.incoming = incoming;
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

    public ServerHandler getServerHandler() {
        return serverHandler;
    }

    public void setServerHandler(ServerHandler serverHandler) {
        this.serverHandler = serverHandler;
    }

    public LinkedBlockingDeque<byte[]> getOutgoing() {
        return outgoing;
    }

    public LinkedBlockingDeque<WebSocketFrame> getIncoming() {
        return incoming;
    }
    
    
}
