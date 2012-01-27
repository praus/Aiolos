package edu.baylor.praus.websocket;

import java.net.URI;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import edu.baylor.praus.exceptions.ClientDoesNotSupportWebSocketException;
import edu.baylor.praus.exceptions.InvalidMethodException;
import edu.baylor.praus.exceptions.InvalidRequestException;
import edu.baylor.praus.exceptions.InvalidWebSocketRequestException;

public class WebSocketHandshakeRequest {
    private String method;
    private URI uri;
    private String wsKey;
    private String wsVersion;
    private String upgrade;
    private String connection;
    private String host;

    public static WebSocketHandshakeRequest decode(ByteBuffer buf)
            throws InvalidRequestException {
        Logger log = Logger.getLogger("aiolos.network");

        /**
         * GET / HTTP/1.1
         * Upgrade: websocket
         * Connection: Upgrade
         * Host: 66.90.194.192
         * Origin: http://websocket.org
         * Sec-WebSocket-Key: 6+m3ssD7l08m+9f5bhCaOA==
         * Sec-WebSocket-Version: 13
         */
        // TODO: Host is mandatory, check for it
        buf.flip();
        byte[] req = new byte[buf.limit()];
        buf.get(req);
        String plainRequest = new String(req);

        final List<String> lines = new ArrayList<String>(
                Arrays.asList(plainRequest.split("\r\n")));

        Matcher m = null;

        // Request-Line (GET / HTTP/1.1)
        Pattern requestLinePattern = Pattern
                .compile("^(?<method>GET)[ ](?<uri>[/][\\w]*)[ ]HTTP/1.1$");
        m = requestLinePattern.matcher(lines.remove(0));
        String method = null;
        String uri = null;
        if (m.matches()) {
            method = m.group("method");
            uri = m.group("uri");
        } else {
            log.info(lines.toString());
            throw new InvalidMethodException("Invalid HTTP Method");
        }

        WebSocketHandshakeRequest wsRequest = new WebSocketHandshakeRequest(
                method, uri);

        for (String line : lines) {
            if (line.trim().equals("")) // skip empty lines
                continue;

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
            }
        }

        // Upgrade to WS connection
        if (wsRequest.isUpgraded()) {
            // fail, client does not support WebSocket
            throw new ClientDoesNotSupportWebSocketException(
                    "Client does not support WebSocket");
        }

        return wsRequest;
    }

    public WebSocketHandshakeRequest() {
    }
    
    public WebSocketHandshakeRequest(String method, String uri) {
        this.method = method;
        this.uri = URI.create(uri);
    }
    
    public void checkValid() throws InvalidRequestException {
        if (!this.isUpgraded()) {
            // fail, client does not support WebSocket
            throw new ClientDoesNotSupportWebSocketException(
                    "Client does not support WebSocket");
        }

        if (this.getWsKey() == null
                || this.getWsVersion() == null
                || !this.getConnection().toLowerCase().equals("upgrade")
                || this.getHost() == null) {
            throw new InvalidWebSocketRequestException(
                    "Request is missing some mandatory header.");
        }
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public URI getUri() {
        return uri;
    }

    public void setUri(URI uri) {
        this.uri = uri;
    }
    
    public void setUri(String uri) {
        this.uri = URI.create(uri);
    }

    public String getWsKey() {
        return wsKey;
    }

    public void setWsKey(String wsKey) {
        this.wsKey = wsKey;
    }

    public String getWsVersion() {
        return wsVersion;
    }

    public void setWsVersion(String wsVersion) {
        this.wsVersion = wsVersion;
    }

    public String getUpgrade() {
        return upgrade;
    }

    public void setUpgrade(String upgrade) {
        this.upgrade = upgrade;
    }

    public boolean isUpgraded() {
        if (upgrade != null && upgrade.toLowerCase().equals("websocket"))
            return true;
        return false;
    }

    public String getConnection() {
        return connection;
    }

    public void setConnection(String connection) {
        this.connection = connection;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    @Override
    public String toString() {
        StringBuffer buf = new StringBuffer();
        buf.append(method).append(" ").append(uri).append("\n");
        buf.append(wsKey).append("\n");
        return buf.toString();
    }
}
