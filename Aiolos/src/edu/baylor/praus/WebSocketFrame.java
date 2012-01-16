package edu.baylor.praus;

import java.nio.BufferUnderflowException;
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

    public static WebSocketFrame decode(ByteBuffer buf)
            throws WebSocketIllegalProtocolException {
        buf.flip();

        byte b = buf.get();
        byte flags = (byte) (0xF & b);
        boolean fin = (flags & 0x1) == 1;
        // RSV flags MUST be 0 unless an extension is negotiated that defines
        // meanings for non-zero values.
        boolean rsv = flags > 1;
        OpCode opcode = OpCode.getOpCodeByNumber(0xF & b);
        b = buf.get();
        boolean mask = (0x1 & (b >> 7)) == 1;

        /* The length of the "Payload data", in bytes: if 0-125, that is the
         * payload length. If 126, the following 2 bytes interpreted as a
         * 16-bit unsigned integer are the payload length. If 127, the
         * following 8 bytes interpreted as a 64-bit unsigned integer (the
         * most significant bit MUST be 0) are the payload length. */
        int payloadLength = 0x7F & b;
        // TODO: support 4GB+ payloads
        if (payloadLength == 126) {
            /* Following 2 bytes interpreted as a 16-bit unsigned integer are
             * the payload length */
            byte[] realLen = new byte[2];
            buf.get(realLen);
            payloadLength = ((realLen[0] & 0xFF) << 8);
            payloadLength |= realLen[1] & 0xFF;

        } else if (payloadLength == 127) {
            /* Following 8 bytes interpreted as a 64-bit unsigned integer (the
             * most significant bit MUST be 0) are the payload length. */
            byte[] realLen = new byte[8];
            buf.get(realLen);
            // TODO: not implemented yet
        }

        byte[] maskingKey = new byte[4];
        ByteBuffer data = ByteBuffer.allocate(payloadLength);

        if (mask) { // we need to unmask the payload
            buf.get(maskingKey);
            for (int i = 0; i < payloadLength; i++) {
                byte unmasked = (byte) (buf.get() ^ maskingKey[i % 4]);
                //System.out.print(new Character((char) unmasked));
                data.put(unmasked);
            }
            data.rewind();
        } else { // unmasked payload from the client - fail
            throw new WebSocketIllegalProtocolException(
                    "Unmasked payload from the client");
        }

        if (rsv) {
            // fail
            // throw new UnsupportedWebSocketExtensionException();
        }
        
        WebSocketFrame frame = new WebSocketFrame(fin, opcode, mask,
                payloadLength, maskingKey, data);
        return frame;
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
        ByteBuffer buf = ByteBuffer.allocate(message.length());
        buf.put(message.getBytes());
        buf.flip();
        WebSocketFrame f = new WebSocketFrame(buf);
        return f;
    }

    public WebSocketFrame(ByteBuffer data) {
        this(true, OpCode.Text, false, 0, new byte[1], data);
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
        return String.format("FIN:%s OPCODE:%s MASK:%s LEN:%s\n", fin ? "1"
                : "0", opcode, mask ? "1" : "0", payloadLength);
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

    public ByteBuffer getData() {
        return data;
    }

    public void setData(ByteBuffer data) {
        this.data = data;
    }

}
