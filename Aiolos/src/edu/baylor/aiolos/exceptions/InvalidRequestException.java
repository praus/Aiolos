package edu.baylor.aiolos.exceptions;

import edu.baylor.websocket.WSException;

public class InvalidRequestException extends WSException {
    
    private static final long serialVersionUID = 1L;

    public InvalidRequestException(String message) {
        super(message);
    }
}
