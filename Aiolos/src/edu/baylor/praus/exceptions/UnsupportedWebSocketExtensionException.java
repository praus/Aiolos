package edu.baylor.praus.exceptions;


public class UnsupportedWebSocketExtensionException extends
        InvalidRequestException {
    
    private static final long serialVersionUID = -748960630344379752L;
    
    public UnsupportedWebSocketExtensionException(String message) {
        super(message);
    }
}
