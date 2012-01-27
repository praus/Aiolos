package edu.baylor.websocket;

/**
 * An interface of class implementing WebSocket protocol.
 */
public interface IWebSocket {

    /**
     * Reads next message from web socket, blocking until data is available
     * 
     * @return Next available message or blocking if no available 
     * @throws WSException in case connection is closed.
     * 
     */
    public IWSMessage receiveMessage() throws WSException;

    /**
     * Puts message into a sending queue.	
     * @param msg - message to be put into the queue
     * @throws WSException in case the connection is closed.
     */
    public void sendMessage(IWSMessage msg) throws WSException;

    /**
     * Initiates the closing handshake if in open state. After the handshake is finished,
     * closes the underlying TCP connection.
     * If the WebSocket connection is not open yet, closed the underlying TCP connection.
     * @throws WSException in case connection is already closed.
     */
    public void closeConnection() throws WSException;

    /**
     * Returns True if connection is closed, False if connection is open. However 
     * return value "False" does not quarantee that connection will be open
     * when subsequently calling any of the methods that throw exceptions.
     * @return True if connection is closed, False if connection is open
     */
    public boolean isClosed();

    /**
     * Returns True if receiveMessage will return at least one message without
     * blocking, however return value "False" does not guarantee receiveMessage will block.
     * @return True if receiveMessage will return at least one message without
     * blocking, otherwise returns False
     */
    public boolean hasNextMessage(); 
}
