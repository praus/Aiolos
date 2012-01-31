package edu.baylor.aiolos.websocket;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.util.logging.Logger;

import edu.baylor.aiolos.ClientSession;
import edu.baylor.aiolos.IServerHandler;
import edu.baylor.aiolos.Server;

/**
 * Functionality common to all (two :-)) decoders.
 */
public abstract class Decoder implements
        CompletionHandler<Integer, ClientSession> {

    public static final Logger log = Logger.getLogger("aiolos.networking");
    protected final AsynchronousSocketChannel channel;
    protected ClientSession attachment;
    protected final ByteBuffer readBuf;

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
        if (exc.getMessage() != null) {
            log.warning(exc.getMessage());
        }
        closeChannel();
    }

    protected void startReading() {
        readBuf.clear();
        channel.read(readBuf, attachment, this);
    }

    protected void closeChannel() {
        try {
            log.info("Disconnecting client " + channel.getRemoteAddress());
            channel.close();
        } catch (IOException e) {
            if (e.getMessage() != null) {
                log.warning(e.getMessage());
            }
        }
    }

    protected boolean notifyClient(WebSocketFrame frame) {
        IServerHandler sh = attachment.getServerHandler();
        log.fine("Received: "+frame);
        sh.getQueue().add(frame);
        return attachment.getServerHandler().receive(attachment);
    }
}
