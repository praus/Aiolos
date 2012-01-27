package edu.baylor.praus.websocket;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.util.logging.Logger;

import edu.baylor.praus.ClientSession;
import edu.baylor.praus.IServerHandler;
import edu.baylor.praus.Server;

public abstract class Decoder implements
        CompletionHandler<Integer, ClientSession> {

    public static final Logger log = Logger.getLogger("aiolos.networking");
    protected final AsynchronousSocketChannel channel;
    protected ClientSession attachment;
    protected final ByteBuffer readBuf;
    protected volatile boolean isReading;

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

    protected boolean notifyClient(WebSocketFrame frame) {
        IServerHandler sh = attachment.getServerHandler();
        sh.getQueue().add(frame);
        return attachment.getServerHandler().receive(attachment);
    }
}
