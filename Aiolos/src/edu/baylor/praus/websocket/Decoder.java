package edu.baylor.praus.websocket;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.util.LinkedList;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import edu.baylor.praus.ClientSession;
import edu.baylor.praus.exceptions.InvalidRequestException;

public abstract class Decoder implements CompletionHandler<Integer, ClientSession> {

    public static final Logger log = Logger.getLogger("aiolos.networking");
    protected final AsynchronousSocketChannel channel;
    protected ClientSession attachment;
    protected final ByteBuffer readBuf;
    protected final ByteBuffer writeBuf;
    private static final int BUFF_SIZE = 4096;
    protected volatile boolean isReading;
    
    protected LinkedList<DataConsumer> consumerQueue = new LinkedList<>();
    
    public Decoder(AsynchronousSocketChannel channel, ClientSession attachment) {
        this.channel = channel;
        this.readBuf = ByteBuffer.allocateDirect(BUFF_SIZE);
        this.writeBuf = ByteBuffer.allocateDirect(BUFF_SIZE);
        this.attachment = attachment;
    }
    
    @Override
    public void completed(Integer result, ClientSession attachment) {
        if (result == -1) {
            try {
                log.info("Client " + channel.getRemoteAddress() + " disconnected abruptly.");
                channel.close();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
            return;
        }
        System.out.println(result);
        this.attachment = attachment;
        
        while (!consumerQueue.isEmpty() && channel.isOpen()) {
            DataConsumer c = consumerQueue.getFirst();
            try {
                c.consume();
            } catch (InvalidRequestException e) {
                e.printStackTrace();
            }
            
            if (c.isSuccessful()) {
                consumerQueue.removeFirst();
            } else { // consumer was not successful, we will read again
//                try {
//                    Thread.sleep(100);
//                } catch (InterruptedException e) {
//                    e.printStackTrace();
//                }
                channel.read(readBuf, 10, TimeUnit.MILLISECONDS, attachment, this);
            }
        }
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
//        public abstract R getResult();
        
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
