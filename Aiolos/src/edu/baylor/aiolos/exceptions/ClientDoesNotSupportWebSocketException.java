package edu.baylor.aiolos.exceptions;

public class ClientDoesNotSupportWebSocketException extends InvalidRequestException {
    
    public ClientDoesNotSupportWebSocketException(String message) {
        super(message);
    }

    private static final long serialVersionUID = -2602226244925767642L;

}
