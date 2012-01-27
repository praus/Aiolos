package edu.baylor.praus;

import java.nio.channels.AsynchronousSocketChannel;
import edu.baylor.praus.websocket.FrameDecoder;
import edu.baylor.praus.websocket.FrameEncoder;
import edu.baylor.praus.websocket.WebSocketHandshakeRequest;

public class ClientSession {
    private WebSocketHandshakeRequest handshakeRequest;

    /**
     * Handshake has been performed successfuly, continue with frames
     */
    private boolean connected;
    private IServerHandler serverHandler;

    private AsynchronousSocketChannel channel;

    private FrameDecoder frameDecoder;
    private FrameEncoder frameEncoder;

    public ClientSession(IServerHandler serverHandler,
            AsynchronousSocketChannel channel) {
        this.serverHandler = serverHandler;
        this.channel = channel;
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

    public IServerHandler getServerHandler() {
        return serverHandler;
    }

    public void setServerHandler(IServerHandler serverHandler) {
        this.serverHandler = serverHandler;
    }

    public FrameDecoder getDecoder() {
        return frameDecoder;
    }

    public void setDecoder(FrameDecoder decoder) {
        this.frameDecoder = decoder;
    }

    public FrameEncoder getEncoder() {
        return frameEncoder;
    }

    public void setEncoder(FrameEncoder encoder) {
        this.frameEncoder = encoder;
    }

    public AsynchronousSocketChannel getChannel() {
        return channel;
    }

    public void setChannel(AsynchronousSocketChannel channel) {
        this.channel = channel;
    }

}
