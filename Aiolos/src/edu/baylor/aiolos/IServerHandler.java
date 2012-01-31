package edu.baylor.aiolos;

import java.util.concurrent.LinkedBlockingDeque;

import edu.baylor.aiolos.websocket.WebSocketFrame;

/**
 * This is interface for any object that wants to receive messages from the
 * server. The class-supplied interface unfortunately fits very poorly to the
 * asynchronous nature of this application.
 */
public interface IServerHandler {

    public boolean receive(ClientSession attachment);
    
    public LinkedBlockingDeque<WebSocketFrame> getQueue();
}