package edu.baylor.aiolos.websocket;

import java.io.IOException;
import java.nio.channels.AsynchronousSocketChannel;
import java.util.logging.Level;

import edu.baylor.aiolos.ClientSession;

/**
 * This is a lightweight completion handler which takes care of the connection
 * once the close frame has been sent.
 */
public class CloseHandler extends Decoder {

    // TODO: refactor the structure so that CloseHandler does not require this.
    public CloseHandler(AsynchronousSocketChannel channel,
            ClientSession attachment) {
        super(channel, attachment);
    }

    @Override
    public void completed(Integer result, ClientSession attachment) {
        super.completed(result, attachment);

        // Close frame was sent, close the TCP connection
        try {
            log.log(Level.INFO,
                    "Closing TCP connection to {0}",
                    channel.getRemoteAddress().toString());
            channel.close();
        } catch (IOException ex) {
            log.info(ex.getMessage());
        }
    }

}
