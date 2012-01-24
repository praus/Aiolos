//package edu.baylor.praus;
//
//import java.nio.ByteBuffer;
//import java.nio.channels.AsynchronousSocketChannel;
//import java.nio.channels.CompletionHandler;
//import java.util.logging.Logger;
//
//public class GeneralCompletionHandler implements
//        CompletionHandler<Integer, ClientSession> {
//    public static final Logger log = Logger.getLogger("aiolos.networking");
//    protected final AsynchronousSocketChannel channel;
//    protected ClientSession attachment;
//    
//    public Decoder(AsynchronousSocketChannel channel) {
//        this.channel = channel;
//        this.buf = ByteBuffer.allocateDirect(BUFF_SIZE);
//    }
//
//}
