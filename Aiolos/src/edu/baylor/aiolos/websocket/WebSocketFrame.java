package edu.baylor.aiolos.websocket;

import java.nio.ByteBuffer;

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

public class WebSocketFrame {

    boolean fin;
    OpCode opcode;
    boolean mask;
    int payloadLength;
    byte[] maskingKey;
    ByteBuffer data;

    ByteBuffer encoded; // encoded frame ready for wire transmission

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

    public static WebSocketFrame createMessage(String message) {
        ByteBuffer buf = ByteBuffer.allocate(message.length()*2);
        buf.put(message.getBytes());
        buf.flip();
        WebSocketFrame f = new WebSocketFrame(buf);
        return f;
    }

    public WebSocketFrame(ByteBuffer data) {
        this(true, OpCode.Text, false, 0, new byte[1], data);
    }

    public WebSocketFrame(boolean fin, OpCode opcode, boolean mask,
            int payloadLength, byte[] maskingKey) {
        this(fin, opcode, mask, payloadLength, maskingKey,
                ByteBuffer.allocate(payloadLength));
        /*
         * TODO: allocating a whole buffer to the size of the payload will
         * fail if we are going to support large payloads above 64KB!
         * We'll need an option for the data to not be part of this frame object
         */
    }
    
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
        return String.format("FIN:%s OPCODE:%s MASK:%s LEN:%s\n",
                fin ? "1" : "0", opcode, mask ? "1" : "0", payloadLength);
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

    protected ByteBuffer getData() {
        return data;
    }
    
    public ByteBuffer getDataCopy() {
        return data.asReadOnlyBuffer();
    }

    public void setData(ByteBuffer data) {
        this.data = data;
    }

}
