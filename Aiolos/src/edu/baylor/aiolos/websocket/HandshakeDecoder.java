package edu.baylor.aiolos.websocket;

import java.nio.channels.AsynchronousSocketChannel;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import edu.baylor.aiolos.ClientSession;
import edu.baylor.aiolos.exceptions.InvalidMethodException;
import edu.baylor.aiolos.exceptions.InvalidRequestException;
import edu.baylor.aiolos.exceptions.InvalidWebSocketRequestException;

public class HandshakeDecoder extends Decoder {

    private WebSocketHandshakeRequest wsRequest;
    private DecoderState state = DecoderState.REQUESTLINE;

    public enum DecoderState {
        REQUESTLINE, HEADERS;
    }

    @Override
    public void completed(Integer result, ClientSession attachment) {
        super.completed(result, attachment);
        
        try {
            switch (state) {
                case REQUESTLINE:
                    if (decodeRequestLine()) {
                        state = DecoderState.HEADERS; // continue with headers
                    } else {
                        channel.read(readBuf, attachment, this);
                        break;
                    }

                case HEADERS:
                    if (decodeHeaders()) {
                        state = DecoderState.REQUESTLINE;
                        HandshakeResponder.create(attachment, channel);
                    } else {
                        channel.read(readBuf, attachment, this);
                    }
                    break;
            }
        } catch (InvalidRequestException e) {
            e.printStackTrace();
        }
        // our consumers are empty, formulate and write our response to the
        // handshake
    }

    /**
     * 
     * @return finished with work?
     * @throws InvalidRequestException
     */
    private boolean decodeRequestLine() throws InvalidRequestException {
        // Request-Line (GET / HTTP/1.1)
        Pattern requestLinePattern = Pattern
                .compile("^(?<method>GET)[ ](?<uri>[/][\\w]*)[ ]HTTP/1.1$");

        StringBuilder requestLine = new StringBuilder();
        // read the first line until we run into \r\n
        int bufPos = readBuf.position();
        readBuf.flip();
        byte b;
        while (readBuf.hasRemaining()) {
            b = readBuf.get();
            if (b == '\r' && readBuf.get() == '\n') {
                // successful request line
                Matcher m = requestLinePattern.matcher(requestLine.toString());
                if (m.matches()) {
                    System.out.println(requestLine.toString());
                    String method = m.group("method");
                    String uri = m.group("uri");
                    wsRequest = new WebSocketHandshakeRequest(method, uri);
                    attachment.setHandshakeRequest(wsRequest);
                    return true;
                } else {
                    log.warning("Invalid HTTP method.");
                    throw new InvalidMethodException("Invalid HTTP Method");
                }

            }
            requestLine.append((char) b);
        }
        // rewind the buffer back to the original position since we haven't
        // consumed anything
        readBuf.position(bufPos);
        return false;
    }

    private boolean decodeHeaders() throws InvalidRequestException {
        wsRequest = attachment.getHandshakeRequest();
        int bufPos = readBuf.position();

        while (readBuf.hasRemaining()) {
            bufPos = readBuf.position();

            String line = Util.getCRLFLine(readBuf);
            System.out.println(line);
            if (line == null) {
                // we haven't found crlf on the line, let's fail now and
                // read further
                readBuf.position(bufPos);
                return false;
            }

            if (line.trim().equals("")) {
                // empty line - end of header
                // success();
                return true;
            }

            String[] s = line.split(":", 2);
            if (s.length != 2)
                throw new InvalidWebSocketRequestException(
                        "Invalid HTTP header field");
            String fieldName = s[0].trim();
            String fieldValue = s[1].trim();

            switch (fieldName.toLowerCase()) {
                case "connection":
                    wsRequest.setConnection(fieldValue);
                    break;
                case "upgrade":
                    wsRequest.setUpgrade(fieldValue);
                    break;
                case "sec-websocket-key":
                    wsRequest.setWsKey(fieldValue);
                    break;
                case "sec-websocket-version":
                    wsRequest.setWsVersion(fieldValue);
                    break;
                case "host":
                    wsRequest.setHost(fieldValue);
                    break;
            }
        }

        return false;
    }

    public HandshakeDecoder(AsynchronousSocketChannel channel,
            ClientSession attachment) {
        super(channel, attachment);
    }

    public static void handle(AsynchronousSocketChannel channel,
            ClientSession attachment) {
        new HandshakeDecoder(channel, attachment).startReading();
    }
}
