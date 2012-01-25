package edu.baylor.praus.websocket;

import java.nio.channels.AsynchronousSocketChannel;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import edu.baylor.praus.ClientSession;
import edu.baylor.praus.exceptions.InvalidMethodException;
import edu.baylor.praus.exceptions.InvalidRequestException;
import edu.baylor.praus.exceptions.InvalidWebSocketRequestException;

public class HandshakeDecoder extends Decoder {

    public class RequestLine extends DataConsumer {
        WebSocketHandshakeRequest request = null;

        @Override
        public void consume() throws InvalidRequestException {
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
                    Matcher m = requestLinePattern.matcher(requestLine
                            .toString());
                    if (m.matches()) {
                        System.out.println(requestLine.toString());
                        success();
                        String method = m.group("method");
                        String uri = m.group("uri");
                        request = new WebSocketHandshakeRequest(method, uri);
                        attachment.setHandshakeRequest(request);
                        return;
                    } else {
                        log.warning("Invalid HTTP method.");
                        throw new InvalidMethodException("Invalid HTTP Method");
                    }

                }
                requestLine.append((char) b);
            }
            // rewind the buffer back to the original position since we didn't
            // consume anything
            readBuf.position(bufPos);
        }
    }

    public class HeadersParser extends DataConsumer {
        
        private WebSocketHandshakeRequest wsRequest;

        @Override
        public void consume() throws InvalidRequestException {
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
                    return;
                }

                if (line.trim().equals("")) {
                    // empty line - end of header
                    success();
                    return;
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
        }

        @Override
        protected void success() throws InvalidRequestException {
            wsRequest.checkValid();
            super.success();
        }
    }

    public HandshakeDecoder(AsynchronousSocketChannel channel, ClientSession attachment) {
        super(channel, attachment);
        consumerQueue.add(new RequestLine());
        consumerQueue.add(new HeadersParser());
    }

    public static void handle(AsynchronousSocketChannel channel, ClientSession attachment) {
        new HandshakeDecoder(channel, attachment).startReading();
    }

    @Override
    public void completed(Integer result, ClientSession attachment) {
        super.completed(result, attachment);
        // our consumers are empty, formulate and write our response to the
        // handshake

        HandshakeResponder.create(attachment, channel);
    }
}
