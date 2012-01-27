package edu.baylor.websocket;

/**
 *  An exception that is thrown indicating that a Web Socket is closed.
 */
public class WSConnectionClosedException extends WSException {
    
    private static final long serialVersionUID = -8744490363114402305L;

    public WSConnectionClosedException(String message) {
		super(message);
	}
}