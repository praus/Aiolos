package edu.baylor.praus.websocket;

import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;

import edu.baylor.praus.ClientSession;
import edu.baylor.praus.exceptions.InvalidRequestException;
import edu.baylor.praus.exceptions.WebSocketIllegalProtocolException;
import edu.baylor.praus.websocket.WebSocketFrame.OpCode;

public class FrameDecoder extends Decoder {

    /**
     * Frame being currently decoded or a frame that was decoded last.
     */
    private WebSocketFrame frame;
    
    class WebSocketHeaderDecoder extends DataConsumer {

        @Override
        public void consume() throws InvalidRequestException {
            /*
             * TODO: adapt this code to account for the fact that get() on the 
             * readBuf can throw underflow exception at any time. If that
             * happens, we need to return and reset position in readBuffer to
             * the last read we actually consumed.
             */
            
            readBuf.flip(); // we're going to read => flip the buffer

            byte b = readBuf.get();
            byte flags = (byte) (0xF & b);
            boolean fin = (flags & 0x1) == 1;
            // RSV flags MUST be 0 unless an extension is negotiated that defines
            // meanings for non-zero values.
            boolean rsv = flags > 1;
            OpCode opcode = OpCode.getOpCodeByNumber(0xF & b);
            b = readBuf.get();
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
                readBuf.get(realLen);
                payloadLength = ((realLen[0] & 0xFF) << 8);
                payloadLength |= realLen[1] & 0xFF;

            } else if (payloadLength == 127) {
                /* Following 8 bytes interpreted as a 64-bit unsigned integer (the
                 * most significant bit MUST be 0) are the payload length. */
                byte[] realLen = new byte[8];
                readBuf.get(realLen);
                // TODO: not implemented yet
            }

            byte[] maskingKey = new byte[4];
            
            if (mask) { // we need to unmask the payload
                readBuf.get(maskingKey);
            } else { // non-masked payload from the client - fail
                throw new WebSocketIllegalProtocolException(
                        "Unmasked payload from the client");
            }

            if (rsv) {
                // fail
                // throw new UnsupportedWebSocketExtensionException();
            }
            success();
            // we've all we need to construct a new frame!
            frame = new WebSocketFrame(fin, opcode, mask,
                    payloadLength, maskingKey);
        }
        
    }
    
    class WebSocketFrameDataDecoder extends DataConsumer {

        private int alreadyRead = 0;
        
        @Override
        public void consume() throws InvalidRequestException {
            ByteBuffer data = frame.getData();
            byte[] maskingKey = frame.getMaskingKey();
            
            for (int i = 0; i < frame.getPayloadLength()-alreadyRead; i++) {
                try {
                    byte masked = readBuf.get();
                    byte unmasked = (byte) (masked ^ maskingKey[i % 4]);
                    // System.out.print((char) unmasked);
                    data.put(unmasked);
                } catch (BufferUnderflowException ex) {
                    // not enough enough data, return and wait for next batch
                    alreadyRead += i - (i % 4);
                    readBuf.position(readBuf.position()-(i % 4));
                    return;
                }
            }
            System.out.println();
            // we've read all the data specified in the header
            success();
            data.flip();
            notifyClient(frame);
        }
    }
    
    public FrameDecoder(AsynchronousSocketChannel channel, ClientSession att) {
        super(channel, att);
        makeConsumerQueue();
    }
    
    private void makeConsumerQueue() {
        consumerQueue.add(new WebSocketHeaderDecoder());
        consumerQueue.add(new WebSocketFrameDataDecoder());
    }
    
    @Override
    protected void consumerSuccessful() {
        // TODO: reuse decoder objects instead of deleting and restoring them 
        super.consumerSuccessful();
        if (consumerQueue.isEmpty()) {
            makeConsumerQueue();
        }
    }
    
    @Override
    public void completed(Integer result, ClientSession attachment) {
        super.completed(result, attachment);
        
        FrameEncoder encoder = attachment.getEncoder();
        if (encoder != null) {
            encoder.startWriting();
        } else {
            FrameEncoder.handle(channel, attachment);
        }
    }
    
    public static void handle(AsynchronousSocketChannel channel, ClientSession attachment) {
        FrameDecoder fd = new FrameDecoder(channel, attachment);
        attachment.setDecoder(fd);
        fd.startReading();
    }

}
