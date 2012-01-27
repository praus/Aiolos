package edu.baylor.aiolos;

import java.util.concurrent.LinkedBlockingDeque;

import edu.baylor.aiolos.websocket.WebSocketFrame;

public interface IServerHandler {

    public boolean receive(ClientSession attachment);
    
    public LinkedBlockingDeque<WebSocketFrame> getQueue();
}