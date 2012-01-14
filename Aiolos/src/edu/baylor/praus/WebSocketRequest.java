package edu.baylor.praus;

import java.net.URI;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.apache.commons.codec.binary.Base64;

public class WebSocketRequest {
    private String method;
    private URI uri;
    private String wsKey;
    private String wsVersion;
    private String upgrade;
    private String connection;
    private String host;
    
    public WebSocketRequest(String method, String uri) {
        this.method = method;
        this.uri = URI.create(uri);
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
        if (upgrade != null && upgrade.toLowerCase().equals("upgrade"))
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
