package edu.baylor.praus;

import java.io.IOException;
import java.nio.ByteBuffer;

import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;

public class ConnectionHandler implements CompletionHandler<AsynchronousSocketChannel, ClientSession> {
    private static final int BUFF_SIZE = 256;

    @Override
    /**
     * result - channel connected to the client
     */
    public void completed(AsynchronousSocketChannel result, ClientSession attachment) {

        try {
            attachment.getLogger().info("Client from " + result.getRemoteAddress() + " connected");
        } catch (IOException ex) {
            ex.printStackTrace();
        }


        ByteBuffer readBuff = ByteBuffer.allocate(BUFF_SIZE);
        ClientSession attach = new ClientSession(result, readBuff);
        attach.setLogger(attachment.getLogger());

        result.read(readBuff, attach, new RequestHandler());

        // accept next connection
        attachment.getServerSocketChannel().accept(attachment, new ConnectionHandler());
    }

    @Override
    public void failed(Throwable exc, ClientSession attachment) {
        attachment.getLogger().warning(exc.getMessage());
    }


}
