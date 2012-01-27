package edu.baylor.praus;

import java.util.concurrent.LinkedBlockingDeque;

import edu.baylor.praus.websocket.WebSocketFrame;

public interface IServerHandler {

    public boolean receive(ClientSession attachment);
    
    public LinkedBlockingDeque<WebSocketFrame> getQueue();
}