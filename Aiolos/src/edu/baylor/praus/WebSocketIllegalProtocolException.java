package edu.baylor.praus;

import edu.baylor.praus.exceptions.InvalidRequestException;

public class WebSocketIllegalProtocolException extends InvalidRequestException {
    
    public WebSocketIllegalProtocolException(String message) {
        super(message);
    }

    private static final long serialVersionUID = 4363458911965701978L;

}
