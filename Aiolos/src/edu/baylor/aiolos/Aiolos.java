package edu.baylor.aiolos;

import java.io.IOException;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.logging.ConsoleHandler;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

import edu.baylor.aiolos.websocket.WebSocketFrame;

public class Aiolos {
    /**
     * Server log file
     */
    public static final String logFile = "server.log";
    
    /**
     * Level of the server logging
     */
    public static final Level logLevel = Level.INFO;
    
    /**
     * Queue for incoming messages.
     */
    private LinkedBlockingDeque<WebSocketFrame> incoming = new LinkedBlockingDeque<>();

    public static void main(String[] args) {
        configureLogger();
        new Aiolos(args);
    }

    public Aiolos(String[] args) {
        IServerHandler sh = new EchoServer(incoming);
        
        Thread server = new Thread(new Server(sh, parsePort(args)));
        server.start();
    }

    private static int parsePort(String[] args) {
        if (args.length >= 1) {
            try {
                return Integer.parseInt(args[0]);
            } catch (NumberFormatException ex) {
            }
        }
        return 8080;
    }

    private static void configureLogger() {
        Logger log = Logger.getLogger("aiolos");
        
        try {
            // configures the logger with handlers and formatter
            FileHandler fh = new FileHandler(logFile, true);
            log.addHandler(fh);
            log.setLevel(logLevel);
            SimpleFormatter formatter = new SimpleFormatter();
            fh.setFormatter(formatter);
        } catch (SecurityException | IOException e) {
            e.printStackTrace();
        }
        
        // level of console log handler
        // http://stackoverflow.com/questions/470430/java-util-logging-logger-doesnt-respect-java-util-logging-level
        Logger root = java.util.logging.Logger.getLogger("");
        
        // Handler for console (reuse it if it already exists)
        Handler consoleHandler = null;
        //see if there is already a console handler
        for (Handler handler : root.getHandlers()) {
            if (handler instanceof ConsoleHandler) {
                //found the console handler
                consoleHandler = handler;
                break;
            }
        }


        if (consoleHandler == null) {
            //there was no console handler found, create a new one
            consoleHandler = new ConsoleHandler();
            root.addHandler(consoleHandler);
        }
        //set the console handler to fine:
        consoleHandler.setLevel(logLevel);
    }
}
