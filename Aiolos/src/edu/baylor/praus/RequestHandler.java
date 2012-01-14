package edu.baylor.praus;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.CompletionHandler;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RequestHandler implements CompletionHandler<Integer, ClientSession> {

    private static final int BUFF_SIZE = 256;
    private Logger log;
    private ClientSession attachment;
            
    private WebSocketRequest parseRequest(String plainRequest)
            throws InvalidRequestException {
        /**
            GET / HTTP/1.1
            Upgrade: websocket
            Connection: Upgrade
            Host: 66.90.194.192
            Origin: http://websocket.org
            Sec-WebSocket-Key: 6+m3ssD7l08m+9f5bhCaOA==
            Sec-WebSocket-Version: 13
         */
        
        final List<String> lines =
                new ArrayList<String>(Arrays.asList(plainRequest.split("\r\n")));
        
        Matcher m = null;
        
        // Request-Line (GET / HTTP/1.1)
        Pattern requestLinePattern = Pattern.compile(
                "^(?<method>GET)[ ](?<uri>[/][\\w]*)[ ]HTTP/1.1$");
        m = requestLinePattern.matcher(lines.remove(0));
        String method = null;
        String uri = null;
        if (m.matches()) {
            method = m.group("method");
            uri = m.group("uri");
        } else {
            throw new InvalidMethodException();
        }
        
        WebSocketRequest wsRequest = new WebSocketRequest(method, uri);
        
        for (String line: lines) {
            if (line.trim().equals("")) // skip empty lines
                continue;
            
            String[] s = line.split(":", 2);
            if (s.length != 2) throw new InvalidWebSocketRequestException();
            String fieldName = s[0];
            String fieldValue = s[1];
            
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
            throw new ClientDoesNotSupportWebSocketException();
        }
        
        return wsRequest;
    }
    
    private void closeChannel() {
        try {
            log.fine("Disconnecting client " + attachment.getChannel().getRemoteAddress());
            attachment.getChannel().close();
        } catch (IOException e) {
            log.warning(e.getMessage());
        }
    }
    
    @Override
    public void completed(Integer result, ClientSession att) {
        attachment = att;
        log = attachment.getLogger();
        
        if (result == -1) {
            try {
                log.info("Client " + attachment.getChannel().getRemoteAddress() + "  disconnected abruptly.");
                attachment.getChannel().close();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
            return;
        }

        ByteBuffer readBuff = attachment.getBuffer();
        ByteBuffer responseBuff = ByteBuffer.allocate(BUFF_SIZE);

        String request = extractByteBuffer(readBuff);
        //System.out.println("REQUEST: " + request);
        
        WebSocketRequest wsRequest = null;
        try {
            wsRequest = parseRequest(request);
            String wsKey = wsRequest.getWsKey();
            
            attachment.setRequest(wsRequest);
            
            log.info(wsRequest.toString());
            
            WebSocketHandshakeResponse r = new WebSocketHandshakeResponse(wsKey);
            responseBuff.put(r.getResponse().getBytes());
            
            
        } catch (InvalidRequestException e) {
            log.info("Invalid request");
            closeChannel(); // disconnect misbehaving client
            e.printStackTrace();
        }
        
//        if (request.startsWith("GET")) {
//
//            responseBuff.put("city\r\n".getBytes());

//        } else if (request.startsWith("end")) {
//            // vypsani soucasnych vysledku klientovi
////            if (attachment.isResults()) {
////                responseBuff.put("vysledky:\r\n".getBytes());
////                responseBuff.put(ClientSession.getPartyvotes().toString().getBytes());
////            }
//
//            responseBuff.put("\r\nBye.\r\n".getBytes());
//            responseBuff.flip();
//            attachment.getChannel().write(responseBuff);
//
//            try {
//                attachment.getLogger().info("Client " + attachment.getChannel().getRemoteAddress() + "  gracefully disconnected.");
//                attachment.getChannel().close();
//            } catch (IOException ex) {
//                ex.printStackTrace();
//            }
//            return;
//
//        } else if (attachment.isResults() && attachment.getCity().equals("")) {
//            Pattern p = Pattern.compile("([a-zA-Z0-9-]+)\r\n");
//            Matcher m = p.matcher(request);
//
//            if (m.find()) {
//                String city = m.group(1);
//                attachment.setCity(city);
//
//                if (!ClientSession.getPartyvotes().containsKey(city)) {
//                    ClientSession.getPartyvotes().put(city, new HashMap<String, Long>());
//                }
//                responseBuff.put("party:votes\r\n".getBytes());
//            } else {
//                responseBuff.put("ERROR: invalid city name, try again\r\n".getBytes());
//            }
//
//
//        } else if (
//                attachment.isResults() &&
//                !attachment.getCity().equals("") &&
//                !request.startsWith("end"))
//        {
//            Pattern p = Pattern.compile("([a-zA-Z0-9 ]+)[:]([0-9]+)\r\n");
//
//            Matcher m = p.matcher(request);
//
//            if (!m.find()) {
//                responseBuff.put("ERROR: unknown format, retry\r\n".getBytes());
//            } else {
//                Map<String, Long> vysledky = attachment.getPartyvotesForCity();
//                if (vysledky == null) {
//                    attachment.getLogger().severe(
//                            "city not found! this shouldn't have happened, disconnecting client");
//                    try {
//                        attachment.getChannel().close();
//                    } catch (IOException ex) {
//                        ex.printStackTrace();
//                    }
//                }
//
//                String strana = m.group(1);
//                Long hlasy = null;
//                try {
//                    hlasy = Long.parseLong(m.group(2));
//                } catch (NumberFormatException ex) {
//                    responseBuff.put("ERROR: Incorrect number format.\r\n".getBytes());
//                    return;
//                }
//
//                if (vysledky.containsKey(strana)) {
//                    hlasy += vysledky.get(strana);
//                }
//                    
//                vysledky.put(strana, hlasy);
//                logger.info("Strana " + strana + " ve meste " + attachment.getCity() + " ma nyni " + hlasy + " hlasu.");
//
//                responseBuff.put("party:votes\r\n".getBytes());
//            }
//
//        
//        } else {
//            responseBuff.put("ERROR: start with \"results\" keyword \r\n".getBytes());
//        }
        
        responseBuff.flip();
        attachment.getChannel().write(responseBuff);
        
        // next read from this client
        ByteBuffer nextBuff = ByteBuffer.allocate(BUFF_SIZE);
        attachment.setBuffer(nextBuff);

        attachment.getChannel().read(nextBuff, attachment, new RequestHandler());
    }

    @Override
    public void failed(Throwable exc, ClientSession attachment) {
        attachment.getLogger().warning(exc.getMessage());
    }

    private static String extractByteBuffer(ByteBuffer buff) {
        buff.flip();
        return new String(buff.array());
    }
}
