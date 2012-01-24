package edu.baylor.praus;

import java.nio.channels.AsynchronousSocketChannel;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import edu.baylor.praus.exceptions.ClientDoesNotSupportWebSocketException;
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
            int bufPos = buf.position();
            buf.flip();
            byte b;
            while (buf.hasRemaining()) {
                b = buf.get();
                if (b == '\r' && buf.get() == '\n') {
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
            buf.position(bufPos);
        }
    }

    public class HeadersParser extends DataConsumer {

        // requirements on a valid WebSocket header:
        // private boolean connectionUpgraded = false;
        // private boolean wsKey = false;
        // private boolean connectionSpecified = false;
        //
        private WebSocketHandshakeRequest wsRequest;

        @Override
        public void consume() throws InvalidRequestException {
            wsRequest = attachment.getHandshakeRequest();
            int bufPos = buf.position();

            while (buf.hasRemaining()) {
                bufPos = buf.position();

                String line = Util.getCRLFLine(buf);
                System.out.println(line);
                if (line == null) {
                    // we haven't found crlf on the line, let's fail now and
                    // read further
                    buf.position(bufPos);
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
            // TODO: move these checks to the Request object
            // end of headers: check if the request we gathered is complete
            // Upgrade to WS connection
            if (!wsRequest.isUpgraded()) {
                // fail, client does not support WebSocket
                throw new ClientDoesNotSupportWebSocketException(
                        "Client does not support WebSocket");
            }

            if (wsRequest.getWsKey() == null
                    || wsRequest.getWsVersion() == null
                    || !wsRequest.getConnection().toLowerCase().equals("upgrade")
                    || wsRequest.getHost() == null) {
                throw new InvalidWebSocketRequestException(
                        "Request is missing some mandatory header.");
            }

            super.success();
        }
    }

    public HandshakeDecoder(AsynchronousSocketChannel channel) {
        super(channel);
        consumerQueue.add(new RequestLine());
        consumerQueue.add(new HeadersParser());
    }

    public static void handle(AsynchronousSocketChannel channel) {
        new HandshakeDecoder(channel).startReading();
    }

    @Override
    public void completed(Integer result, ClientSession attachment) {
        super.completed(result, attachment);
        // our consumers are empty, formulate and write our response to the
        // handshake

        HandshakeResponder.create(attachment, channel);
    }
}
