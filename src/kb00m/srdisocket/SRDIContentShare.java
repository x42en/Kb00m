/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package kb00m.srdisocket;

import java.net.SocketAddress;
import net.jxta.content.Content;
import net.jxta.content.ContentShareEvent;
import net.jxta.content.ContentShareEvent.Builder;
import net.jxta.content.ContentShareListener;
import net.jxta.id.ID;
import net.jxta.protocol.ContentAdvertisement;
import net.jxta.protocol.PipeAdvertisement;
import net.jxta.socket.JxtaSocketAddress;
import net.jxta.impl.content.AbstractPipeContentShare;

/**
 * Implementation of the ContentShare interface for use in the
 * SRDI-socket implementation.  This class implements the bare minimum requirements
 * for a Content share implementation and will likely always need to be
 * extended by the provider implementation to be useful.
 */
public class SRDIContentShare extends AbstractPipeContentShare<
        ContentAdvertisement, SRDISocketContentShareAdvertisementImpl> {
    /**
     * Construct a new SRDIContentShare object, generating a new
     * PipeAdvertisement.
     *
     * @param origin content provider which created this share
     * @param content content object to share
     * @param pipeAdv pipe advertisement for requests to be sent to
     */
    public SRDIContentShare(
            SRDISocketContentProvider origin,
            Content content,
            PipeAdvertisement pipeAdv) {
	super(origin, content, pipeAdv);
    }

    /**
     * {@inheritDoc}
     */
    protected SRDISocketContentShareAdvertisementImpl
            createContentShareAdvertisement() {
        return new SRDISocketContentShareAdvertisementImpl();
    }
    
    /**
     * Notify all listeners of this object of a new session being
     * created.
     * 
     * @param session session being opened
     */
    protected void fireShareSessionOpened(SocketAddress session) {
        ContentShareEvent event = null;
        for (ContentShareListener listener : getContentShareListeners()) {
            if (event == null) {
                event = createEvent(session);
            }
            listener.shareSessionOpened(event);
        }
    }

    /**
     * Notify all listeners of this object of an idle session being
     * garbage collected.
     * 
     * @param session session being closed
     */
    protected void fireShareSessionClosed(SocketAddress session) {
        ContentShareEvent event = null;
        for (ContentShareListener listener : getContentShareListeners()) {
            if (event == null) {
                event = createEvent(session);
            }
            listener.shareSessionClosed(event);
        }
    }

    /**
     * Notify all listeners of this object that the share is being
     * accessed.
     * 
     * @param session session being accessed
     */
    protected void fireShareSessionAccessed(SocketAddress session) {
        ContentShareEvent event = null;
        for (ContentShareListener listener : getContentShareListeners()) {
            if (event == null) {
                event = createEvent(session);
            }
            listener.shareAccessed(event);
        }
    }
    
    ///////////////////////////////////////////////////////////////////////////
    // Private methods:
    
    /**
     * Creates and initializes a ContentShareEvent for the session
     * given.
     * 
     * @param session session to create event for
     * @return event object
     */
    private ContentShareEvent createEvent(SocketAddress session) {
        Builder result =  new Builder(this, session);
        
        // Name the remote peer by it's pipe ID
        if (session instanceof JxtaSocketAddress) {
            JxtaSocketAddress jxtaAddr = (JxtaSocketAddress) session;
            PipeAdvertisement pipeAdv = jxtaAddr.getPipeAdv();
            ID pipeID = pipeAdv.getPipeID();
            result.remoteName(pipeID.toString());
        }
        
        return result.build();
    }

}