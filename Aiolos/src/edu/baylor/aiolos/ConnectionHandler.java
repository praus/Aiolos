package edu.baylor.aiolos;
//package edu.baylor.praus;
//
//import java.io.IOException;
//import java.nio.ByteBuffer;
//
//import java.nio.channels.AsynchronousSocketChannel;
//import java.nio.channels.CompletionHandler;
//import java.util.logging.Logger;
//
///**
// * Handler for new connections.
// */
//public class ConnectionHandler implements CompletionHandler<AsynchronousSocketChannel, ClientSession> {
//    private static final int BUFF_SIZE = 256;
//    
//    /**
//     * @param channel channel connected to the client
//     */
//    @Override
//    public void completed(AsynchronousSocketChannel channel, Void attachment) {
//        Logger log = Logger.getLogger("aiolos.networking");
//        
//        try {
//            log.info("Client from " + channel.getRemoteAddress() + " connected");
//        } catch (IOException ex) {
//            ex.printStackTrace();
//        }
//        
//        ByteBuffer readBuff = ByteBuffer.allocate(BUFF_SIZE);
//        ClientSession attach = new ClientSession(channel, readBuff);
//        attach.setLogger(attachment.getLogger());
//        
//        //channel.read(readBuff, attach, new RequestHandler(channel));
//        
//        // accept next connection
//        attachment.getServerSocketChannel().accept(attachment, new ConnectionHandler());
//    }
//
//    @Override
//    public void failed(Throwable exc, ClientSession attachment) {
//        attachment.getLogger().warning(exc.getMessage());
//    }
//
//
//}
