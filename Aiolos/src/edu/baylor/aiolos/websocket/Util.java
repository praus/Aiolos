package edu.baylor.aiolos.websocket;

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
    
    /**
     * Retrieves arr.length bytes from ByteBuffer
     * @param buf
     * @param arr
     * @return whether the BB.get() operation was successful or not
     */
    public static boolean getBytes(ByteBuffer buf, byte[] arr) {
        if (buf.remaining() < arr.length)
            return false;
        buf.get(arr);
        return true;
    }
    
    public static byte[] readByteBuffer(ByteBuffer buf) {
        int pos = buf.position();
        byte[] r = new byte[buf.limit()-buf.position()];
        buf.get(r);
        buf.position(pos);
        return r;
    }
}
