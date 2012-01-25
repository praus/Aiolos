package edu.baylor.praus.websocket;

import java.nio.ByteBuffer;

public class Util {
    
    public static String getCRLFLine(ByteBuffer buf) {
        StringBuilder str = new StringBuilder();
        byte b;
        while (buf.hasRemaining()) {
            b = buf.get();
            if (b == '\r' && buf.get() == '\n') {
                return str.toString();
            }
            str.append((char) b);
        }
        return null; // we haven't found \r\n
    }
}
