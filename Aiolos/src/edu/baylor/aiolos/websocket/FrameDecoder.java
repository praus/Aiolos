package edu.baylor.aiolos.websocket;

import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;

import edu.baylor.aiolos.ClientSession;
import edu.baylor.aiolos.exceptions.InvalidRequestException;
import edu.baylor.aiolos.exceptions.UnsupportedWebSocketExtensionException;
import edu.baylor.aiolos.exceptions.WebSocketIllegalProtocolException;
import edu.baylor.aiolos.websocket.WebSocketFrame.OpCode;

public class FrameDecoder extends Decoder {
    
    /**
     * Represents the possible states of a decoder decoding WebSocket frame.
     */
    public enum DecoderState {
        HEADER, DATA;
    }
    
    /**
     * Current decoder state
     */
    private DecoderState state = DecoderState.HEADER;
    
    /**
     * If the buffer is too small to fit in entire frame data, we don't want to
     * throw away the data we decoded. This says how much we already decoded
     * and we don't reset the buffer all the way.
     */
    private int dataAlreadyRead = 0;
    
    /**
     * Frame being currently decoded or a frame that was decoded last.
     */
    private WebSocketFrame frame;

    @Override
    public void completed(Integer result, ClientSession attachment) {
        super.completed(result, attachment);
        
        try {
            switch (state) {
                case HEADER:
                    if (decodeWSHeader()) {
                        state = DecoderState.DATA;
                    } else {
                        channel.read(readBuf, attachment, this);
                        break;
                    }
                    
                case DATA:
                    // indicates whether this communication will continue
                    boolean cont = true;
                    
                    if (decodeData()) {
                        state = DecoderState.HEADER;
                        dataAlreadyRead = 0;
                        cont = notifyClient(frame);
                    }
                    if (cont) {
                        readBuf.compact();
                        channel.read(readBuf, attachment, this);
                    }
                break;
            }
        } catch (InvalidRequestException e) {
            e.printStackTrace();
        }
    }

    private boolean decodeWSHeader() throws InvalidRequestException {
        /*
         * TODO: adapt this code to account for the fact that get() on the 
         * readBuf can throw underflow exception at any time. If that
         * happens, we need to return and reset position in readBuffer to
         * the last read we actually consumed.
         */
        
        readBuf.flip(); // we're going to read => flip the buffer
        byte[] fhdr = new byte[2]; // fixed 2-byte header
        if (!Util.getBytes(readBuf, fhdr)) return false;
        
        byte b = fhdr[0];
        //byte flags = (byte) (0xF & (b >> 4));
        //byte flags = (byte) ((0xF0 & b));
        boolean fin = ((0x80 & b) >> 7) == 1; // mask: 10000000
        // RSV flags MUST be 0 unless an extension is negotiated that defines
        // meanings for non-zero values.
        boolean rsv = ((0x70 & b) >> 4) != 0; // mask: 01110000
        OpCode opcode = OpCode.getOpCodeByNumber(0xF & b);
        b = fhdr[1];
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
            if (!Util.getBytes(readBuf, realLen)) return false;
            payloadLength = ((realLen[0] & 0xFF) << 8);
            payloadLength |= realLen[1] & 0xFF;

        } else if (payloadLength == 127) {
            /* Following 8 bytes interpreted as a 64-bit unsigned integer (the
             * most significant bit MUST be 0) are the payload length. */
            byte[] realLen = new byte[8];
            if (!Util.getBytes(readBuf, realLen)) return false;
            // TODO: not implemented yet
        }

        byte[] maskingKey = new byte[4];
        
        if (mask) { // we need to unmask the payload
            if (!Util.getBytes(readBuf, maskingKey)) return false;
        } else { // non-masked payload from the client - fail
            throw new WebSocketIllegalProtocolException(
                    "Client sent unmasked payload.");
        }

        if (rsv) {
            // fail, we don't support any extensions
            throw new UnsupportedWebSocketExtensionException("");
        }
        
        // we've all we need to construct a new frame!
        frame = new WebSocketFrame(fin, opcode, mask,
                payloadLength, maskingKey);
        return true;
    }
        
    private boolean decodeData() throws InvalidRequestException {
        ByteBuffer data = frame.getData();
        byte[] maskingKey = frame.getMaskingKey();
        
        log.info("Starting from: "+(dataAlreadyRead));
        
        for (int i = 0; i < frame.getPayloadLength()-dataAlreadyRead; i++) {
            if (readBuf.hasRemaining()) {
                byte masked = readBuf.get();
                byte unmasked = (byte) (masked ^ maskingKey[i % 4]);
                // System.out.print((char) unmasked);
                data.put(unmasked);
            } else {
                // not enough enough data, return and wait for next batch
                dataAlreadyRead += i - (i % 4);
                log.info("Already read: " + dataAlreadyRead);
                readBuf.position(readBuf.position()-(i % 4));
                log.info("readBuf pos: "+readBuf.position());
                readBuf.compact();
                return false;
            }
        }
        log.info("readBuf pos: "+readBuf.position());
        
        // we've read all the data specified in the header
        data.flip();
        return true;
    }
    
    
    public FrameDecoder(AsynchronousSocketChannel channel, ClientSession att) {
        super(channel, att);
    }
    
    public static void handle(AsynchronousSocketChannel channel, ClientSession attachment) {
        FrameDecoder fd = new FrameDecoder(channel, attachment);
        attachment.setDecoder(fd);
        fd.startReading();
    }

}
