package edu.baylor.websocket;

/**
 *  A general exception that is thrown when something bad happens in a Web Socket.
 */
public class WSException extends Exception {
    
    private static final long serialVersionUID = -5518615309481169539L;

    public WSException(String message){
		super(message);
	}
}
