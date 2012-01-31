package edu.baylor.aiolos.websocket;

import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;

import edu.baylor.aiolos.ClientSession;
import edu.baylor.aiolos.exceptions.InvalidRequestException;
import edu.baylor.aiolos.exceptions.UnsupportedWebSocketExtensionException;
import edu.baylor.aiolos.exceptions.WebSocketIllegalProtocolException;
import edu.baylor.aiolos.websocket.WebSocketFrame.OpCode;

/**
 * This completion handler handles decoding of frames and calls client handler
 * (implementing IServerHandler interface).
 */
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
    private DecoderState state = DecoderState.HEADER; // decoding starts with
                                                      // header

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

        readBuf.flip(); // we're going to read => flip the buffer

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
                    readBuf.compact();
                    if (cont) {
                        // readBuf.limit(readBuf.capacity());
                        channel.read(readBuf, attachment, this);
                    }
                    break;
            }
        } catch (InvalidRequestException e) {
            invalidRequest(e);
        }
    }

    private void invalidRequest(Throwable exc) {
        WebSocketFrame closeFrame = WebSocketFrame.protocolErrorCloseFrame();
        channel.write(closeFrame.encode(), attachment, new CloseHandler(
                channel, attachment));
    }

    /**
     * Responsible for decoding header and creating object representation of
     * the frame.
     * 
     * @return true if this decoder finished its job
     * @throws InvalidRequestException
     */
    private boolean decodeWSHeader() throws InvalidRequestException {
        /* This code (or the code calling this code) must account for the fact
         * that get() on the readBuf can throw underflow exception at any time.
         * If that happens, we need to return and reset position in readBuffer
         * to the last read we actually consumed. */

        byte[] fhdr = new byte[2]; // fixed 2-byte header
        if (!Util.getBytes(readBuf, fhdr))
            return false;

        byte b = fhdr[0];
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
            if (!Util.getBytes(readBuf, realLen))
                return false;
            payloadLength = ((realLen[0] & 0xFF) << 8);
            payloadLength |= realLen[1] & 0xFF;

        } else if (payloadLength == 127) {
            /* Following 8 bytes interpreted as a 64-bit unsigned integer (the
             * most significant bit MUST be 0) are the payload length. */
            throw new UnsupportedOperationException(
                    "Payloads larger than 2^16 bits are not currently supported");
            // byte[] realLen = new byte[8];
            // if (!Util.getBytes(readBuf, realLen))
            // return false;
            // TODO: not implemented yet
        }

        byte[] maskingKey = new byte[4];

        if (mask) { // we need to unmask the payload
            if (!Util.getBytes(readBuf, maskingKey))
                return false;
        } else { // non-masked payload from the client - fail
            throw new WebSocketIllegalProtocolException(
                    "Client sent unmasked payload.");
        }

        if (rsv) {
            // fail, we don't support any extensions
            throw new UnsupportedWebSocketExtensionException(
                    "Extensions are not supported");
        }

        // we've all we need to construct a new frame!
        frame = new WebSocketFrame(fin, opcode, mask, payloadLength, maskingKey);
        return true;
    }

    private boolean decodeData() throws InvalidRequestException {
        ByteBuffer data = frame.getDataAsByteBuffer();
        byte[] maskingKey = frame.getMaskingKey();

        for (int i = dataAlreadyRead; i < frame.getPayloadLength(); i++) {
            if (readBuf.hasRemaining()) {
                byte masked = readBuf.get();
                byte unmasked = (byte) (masked ^ maskingKey[i % 4]);
                // System.out.print((char) unmasked);
                data.put(unmasked);
            } else {
                // not enough enough data, return and wait for next batch
                dataAlreadyRead = i;
                return false;
            }
        }
        // we've read all the data specified in the header
        data.flip();
        return true;
    }

    public FrameDecoder(AsynchronousSocketChannel channel, ClientSession att) {
        super(channel, att);
    }

    public static void handle(AsynchronousSocketChannel channel,
            ClientSession attachment) {
        FrameDecoder fd = new FrameDecoder(channel, attachment);
        attachment.setDecoder(fd);
        fd.startReading();
    }

}
