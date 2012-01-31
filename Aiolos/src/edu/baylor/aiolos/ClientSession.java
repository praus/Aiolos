package edu.baylor.aiolos;

import java.io.IOException;
import java.nio.channels.AsynchronousSocketChannel;

import edu.baylor.aiolos.websocket.FrameDecoder;
import edu.baylor.aiolos.websocket.FrameEncoder;
import edu.baylor.aiolos.websocket.WebSocketHandshakeRequest;
import edu.baylor.websocket.IWSMessage;
import edu.baylor.websocket.IWebSocket;
import edu.baylor.websocket.WSException;

public class ClientSession implements IWebSocket {
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

    // CSI 5321 interface implementation
    /*
     * The supplied interface unfortunately fits very poorly to the asynchronous
     * nature of this application.
     * This functionality is rather basic and not actually used in the code.
     */
    
    @Override
    public IWSMessage receiveMessage() throws WSException {
        return null;
    }

    @Override
    public void sendMessage(IWSMessage msg) throws WSException {
    }

    @Override
    public void closeConnection() throws WSException {
        try {
            channel.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean isClosed() {
        return !channel.isOpen();
    }

    @Override
    public boolean hasNextMessage() {
        return false;
    }

}
