package edu.baylor.praus;

import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.AsynchronousSocketChannel;
import java.util.logging.Logger;

public class ClientSession {
    private AsynchronousSocketChannel channel;
    private AsynchronousServerSocketChannel serverSocketChannel;
    private ByteBuffer buffer;
    
    private WebSocketRequest request;
    private Logger logger;

    public ClientSession(AsynchronousSocketChannel channel, ByteBuffer buffer) {
        this.channel = channel;
        this.buffer = buffer;
    }

    public ClientSession(AsynchronousSocketChannel channel) {
        this.channel = channel;
    }

    ClientSession(AsynchronousServerSocketChannel listener, Logger logger) {
        this.serverSocketChannel = listener;
        this.logger = logger;
    }


    public ClientSession() {
    }

    
    public ByteBuffer getBuffer() {
        return buffer;
    }

    public void setBuffer(ByteBuffer buffer) {
        this.buffer = buffer;
    }

    public AsynchronousSocketChannel getChannel() {
        return channel;
    }

    public void setChannel(AsynchronousSocketChannel channel) {
        this.channel = channel;
    }

    public Logger getLogger() {
        return logger;
    }

    public void setLogger(Logger logger) {
        this.logger = logger;
    }

    public AsynchronousServerSocketChannel getServerSocketChannel() {
        return serverSocketChannel;
    }

    public WebSocketRequest getRequest() {
        return request;
    }

    public void setRequest(WebSocketRequest request) {
        this.request = request;
    }

    
    
}
