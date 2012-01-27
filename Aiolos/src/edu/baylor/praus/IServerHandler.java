package edu.baylor.praus;

import java.util.concurrent.LinkedBlockingDeque;

import edu.baylor.praus.websocket.WebSocketFrame;

public interface IServerHandler {

    public void receive(ClientSession attachment);
    
    public LinkedBlockingDeque<WebSocketFrame> getQueue();
}