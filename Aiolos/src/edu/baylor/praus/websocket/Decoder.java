package edu.baylor.praus.websocket;

import java.io.IOException;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.util.LinkedList;
import java.util.logging.Logger;

import edu.baylor.praus.ClientSession;
import edu.baylor.praus.Server;
import edu.baylor.praus.exceptions.InvalidRequestException;

public abstract class Decoder implements
        CompletionHandler<Integer, ClientSession> {

    public static final Logger log = Logger.getLogger("aiolos.networking");
    protected final AsynchronousSocketChannel channel;
    protected ClientSession attachment;
    protected final ByteBuffer readBuf;
    protected volatile boolean isReading;

    protected LinkedList<DataConsumer> consumerQueue = new LinkedList<>();

    public Decoder(AsynchronousSocketChannel channel, ClientSession attachment) {
        this.channel = channel;
        this.readBuf = ByteBuffer.allocateDirect(Server.BUFF_SIZE);
        this.attachment = attachment;
    }

    @Override
    public void completed(Integer result, ClientSession attachment) {
        if (result == -1) {
            try {
                log.info("Client " + channel.getRemoteAddress()
                        + " disconnected abruptly.");
                channel.close();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
            return;
        }
        this.attachment = attachment;

        while (!consumerQueue.isEmpty() && channel.isOpen()) {
            DataConsumer c = consumerQueue.getFirst();
            int readPos = readBuf.position();
            try {
                c.consume();
            } catch (InvalidRequestException e) {
                e.printStackTrace();
            } catch (BufferUnderflowException e) {
                /*
                 * If the consumer fails on some buffer read, reset the buffer
                 * to the original position. Note this will discard the
                 * consumer's work altogether. If this shouldn't be the case,
                 * the consumer should catch and handle the underflow exception
                 * himself.
                 */
                readBuf.position(readPos);
            }

            if (c.isSuccessful()) {
                consumerSuccessful();
            } else {
                consumerUnsuccessful();
            }
        }
    }

    protected void consumerSuccessful() {
        consumerQueue.removeFirst();
    }
    
    protected void consumerUnsuccessful() {
        /* Consumer was not successful, we will try reading again */
        // channel.read(readBuf, 10, TimeUnit.MILLISECONDS, attachment, this);
        channel.read(readBuf, attachment, this);
    }

    @Override
    public void failed(Throwable exc, ClientSession attachment) {
        exc.printStackTrace();
        log.warning(exc.getMessage());
        closeChannel();
    }

    protected void startReading() {
        readBuf.clear();
        isReading = true;
        channel.read(readBuf, attachment, this);
    }

    protected void closeChannel() {
        try {
            log.info("Disconnecting client " + channel.getRemoteAddress());
            channel.close();
        } catch (IOException e) {
            log.warning(e.getMessage());
        }
    }

    protected void notifyClient(WebSocketFrame frame) {
        attachment.getIncoming().add(frame);
        attachment.getServerHandler().receive();
    }

    public abstract class DataConsumer {
        protected boolean successful = false;

        public abstract void consume() throws InvalidRequestException;
        
        public boolean isSuccessful() {
            return this.successful;
        }

        protected void success() throws InvalidRequestException {
            this.successful = true;
        }
    }

    public interface DataConsumerResult {

    }
}
