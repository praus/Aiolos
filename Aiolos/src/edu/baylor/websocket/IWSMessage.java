package edu.baylor.websocket;

/**
 * An interface of a class encapsulating data transported between IWebSocket
 * class and an application.
 * A class implementing this interface represents payload data of a message
 * of a WebSocket protocol after unmasking and unfragmenting.
 */
public interface IWSMessage {
    /**
     * Message type corresponding roughly to opCode in WebSocket frame.
     * Currently just TEXT and BINARY data are enabled. Might be extended
     * in future.
     */
    public enum WSMessageType{
        TEXT, BINARY;
    }
    /**
     * Returns payload data of a message.
     * @return unmasked and unfragmented payload data
     */
    public byte[] getData();
    
    /**
     * Sets the message data.
     * @param data - the unmasked and unfragmented payload data
     */
    public void setData(byte[] d);
    
    /**
     * Returns message data type. This value should be used to correctly interpret
     * the return value of "byte [] getData()".
     * @return message data type
     */
    public WSMessageType getMessageType();
    
    /**
     * Sets the message data type.
     * @param type - the type of message
     */
    public void setMessageType(WSMessageType type);
}
