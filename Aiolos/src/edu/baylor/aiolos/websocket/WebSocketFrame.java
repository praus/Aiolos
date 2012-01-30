package edu.baylor.aiolos.websocket;

import java.nio.ByteBuffer;

import edu.baylor.websocket.IWSMessage;

/* 
 0                   1                   2                   3
 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
 +-+-+-+-+-------+-+-------------+-------------------------------+
 |F|R|R|R| opcode|M| Payload len |    Extended payload length    |
 |I|S|S|S|  (4)  |A|     (7)     |             (16/64)           |
 |N|V|V|V|       |S|             |   (if payload len==126/127)   |
 | |1|2|3|       |K|             |                               |
 +-+-+-+-+-------+-+-------------+ - - - - - - - - - - - - - - - +
 |     Extended payload length continued, if payload len == 127  |
 + - - - - - - - - - - - - - - - +-------------------------------+
 |                               |Masking-key, if MASK set to 1  |
 +-------------------------------+-------------------------------+
 | Masking-key (continued)       |          Payload Data         |
 +-------------------------------- - - - - - - - - - - - - - - - +
 :                     Payload Data continued ...                :
 + - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - +
 |                     Payload Data continued ...                |
 +---------------------------------------------------------------+
 */

/**
 * Object representation of a WebSocket frame.
 */
public class WebSocketFrame implements IWSMessage {

    /**
     * FIN bit, every non-fragmented bit should have this set. It has
     * nothing to do with closing of connection.
     */
    private boolean fin;

    /**
     * Type of the frame.
     */
    private OpCode opcode;

    /**
     * Whether this frame's data are masked by maskingKey.
     */
    private boolean mask;

    /**
     * Size of the payload
     */
    private int payloadLength;

    /**
     * If the payload (data) is masked, it needs to be XORed (in a special way)
     * with this value.
     */
    private byte[] maskingKey;
    private ByteBuffer data;

    /**
     * encoded frame ready for wire transmission
     */
    private ByteBuffer encoded;

    /**
     * Type of frame.
     */
    public enum OpCode {
        Continuation(0x0), Text(0x1), Binary(0x2), ConnectionClose(0x8),
        Ping(0x9), Pong(0xA);

        private final int opcode;

        OpCode(int opcode) {
            this.opcode = opcode;
        }

        public int getOpCodeNumber() {
            return this.opcode;
        }

        @Override
        public String toString() {
            return this.name();
        }

        public static OpCode getOpCodeByNumber(int opcode) {
            OpCode o = OpCode.Text;
            for (OpCode opc : OpCode.values()) {
                if (opc.getOpCodeNumber() == opcode)
                    o = opc;
            }
            return o;
        }
    }

    /**
     * Encodes contents of this frame into bytes stored in ByteBuffer
     * 
     * @return buffer with encoded data, ready for reading (flipped)
     */
    public ByteBuffer encode() {
        encoded = ByteBuffer.allocate(64 + data.limit());
        byte flags = 0b1000; // fin
        byte opcode = (byte) this.opcode.getOpCodeNumber();
        encoded.put((byte) (opcode | (flags << 4)));

        byte mask = 0;
        int payloadLen = data.limit();
        // short statusCode = 1000;

        int firstLen = payloadLen; // first length field
        byte[] extendedLen = new byte[0];
        if (payloadLen > 65536) { // use 64 bit field for really large payloads
            // TODO: implement
            firstLen = 127;
            extendedLen = new byte[8];
        } else if (payloadLen > 125) { // 16 bit field
            firstLen = 126;
            extendedLen = new byte[2];
            extendedLen[0] = (byte) ((payloadLen >> 8) & 0xFF);
            extendedLen[1] = (byte) (payloadLen & 0xFF);
        }
        // System.out.format("0x%x",);
        encoded.put((byte) ((mask << 7) | (firstLen)));
        encoded.put(extendedLen);
        // buf.putShort(statusCode);
        encoded.put(data);
        encoded.flip();
        return encoded;
    }

    /**
     * Wrapper around constructor that allows to easily send a text message.
     * 
     * @param message
     *            text of the message
     * @return frame with message encoded as data in the frame
     */
    public static WebSocketFrame createMessage(String message) {
        ByteBuffer buf = ByteBuffer.allocate(message.length());
        buf.put(message.getBytes());
        buf.flip();
        WebSocketFrame f = new WebSocketFrame(buf);
        return f;
    }

    /**
     * Constructor with values for default frame
     * 
     * @param data
     *            payload of the frame
     */
    public WebSocketFrame(ByteBuffer data) {
        this(true, OpCode.Text, false, data.remaining(), null, data);
    }

    /**
     * Constructor without data, they are to be specified later or left blank
     * 
     * @param fin
     *            whether FIN bit should be set
     * @param opcode
     *            type of frame
     * @param mask
     *            whether this frame is masked
     * @param payloadLength
     *            payload size (capacity of the data buffer)
     * @param maskingKey
     *            for unmasking data (if mask==true)
     */
    public WebSocketFrame(boolean fin, OpCode opcode, boolean mask,
            int payloadLength, byte[] maskingKey) {
        this(fin, opcode, mask, payloadLength, maskingKey, ByteBuffer
                .allocate(payloadLength));
        /* TODO: allocating a whole buffer to the size of the payload will
         * fail if we are going to support large payloads above 64KB!
         * We'll need an option for the data to not be part of this frame object
         * Some kind of streaming API maybe */
    }

    /**
     * Constructor for setting every aspect of the frame
     * 
     * @param fin
     *            whether FIN bit should be set
     * @param opcode
     *            type of frame
     * @param mask
     *            whether this frame is masked
     * @param payloadLength
     *            payload size (capacity of the data buffer)
     * @param maskingKey
     *            for unmasking data (if mask==true)
     */
    public WebSocketFrame(boolean fin, OpCode opcode, boolean mask,
            int payloadLength, byte[] maskingKey, ByteBuffer data) {
        this.fin = fin;
        this.opcode = opcode;
        this.mask = mask;
        this.payloadLength = payloadLength;
        this.maskingKey = maskingKey;
        this.data = data;
    }

    @Override
    public String toString() {
        return String.format("FIN:%s OPCODE:%s MASK:%s LEN:%s\n", fin ? "1"
                : "0", opcode, mask ? "1" : "0", payloadLength);
    }

    /**
     * Clones the frame object. Note that data buffer is not entirely
     * independent
     * for efficiency purposes. There's no need to really duplicate the frame
     * content since data buffers in frame objects are usually not going to be
     * reused.
     * 
     * @return independent WebSocketFrame instance
     */
    @Override
    protected WebSocketFrame clone() {
        return new WebSocketFrame(fin, opcode, mask, payloadLength, maskingKey,
                data.duplicate());
    }

    /**
     * @return Whether this frame is the final frame.
     */
    public boolean isFin() {
        return fin;
    }

    public void setFin(boolean fin) {
        this.fin = fin;
    }

    public OpCode getOpcode() {
        return opcode;
    }

    public void setOpcode(OpCode opcode) {
        this.opcode = opcode;
    }

    /**
     * Indicates whether this frame has Connection Close flag set and
     * therefore the endpoint receiving this frame must close connection.
     */
    public boolean isClose() {
        return getOpcode().equals(OpCode.ConnectionClose);
    }

    public boolean isMask() {
        return mask;
    }

    public void setMask(boolean mask) {
        this.mask = mask;
    }

    public int getPayloadLength() {
        return payloadLength;
    }

    public void setPayloadLength(int payloadLength) {
        this.payloadLength = payloadLength;
    }

    public byte[] getMaskingKey() {
        return maskingKey;
    }

    public void setMaskingKey(byte[] maskingKey) {
        this.maskingKey = maskingKey;
    }

    protected ByteBuffer getDataAsByteBuffer() {
        return data;
    }

    public ByteBuffer getDataCopy() {
        return data.asReadOnlyBuffer();
    }

    public void setData(ByteBuffer data) {
        this.data = data;
    }

    @Override
    public byte[] getData() {
        byte[] d = new byte[this.data.remaining()];
        this.data.asReadOnlyBuffer().get(d);
        return d;
    }

    @Override
    public void setData(byte[] d) {
        this.data.clear();
        this.data.put(d);
    }

    @Override
    public WSMessageType getMessageType() {
        switch (opcode) {
            case Text:
                return WSMessageType.TEXT;
            case Binary:
                return WSMessageType.BINARY;
            default:
                // The interface is incomplete and does not reflect
                // other opcodes, we'll return null in those cases.
                return null;
        }
    }

    @Override
    public void setMessageType(WSMessageType type) {
    }

}
