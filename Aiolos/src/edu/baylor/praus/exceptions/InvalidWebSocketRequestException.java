package edu.baylor.praus.exceptions;

public class InvalidWebSocketRequestException extends InvalidRequestException {

    public InvalidWebSocketRequestException(String message) {
        super(message);
    }
    
    private static final long serialVersionUID = 6663012585862638471L;

}
