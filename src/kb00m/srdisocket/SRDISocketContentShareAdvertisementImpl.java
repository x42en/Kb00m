/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package kb00m.srdisocket;

import net.jxta.document.Advertisement;
import net.jxta.document.AdvertisementFactory;
import net.jxta.document.Element;
import net.jxta.impl.content.AbstractPipeContentShareAdvertisement;

/**
 * This class is a simple re-badging of the more generic abstract version,
 * AbstractPipeContentAdvertisement.  This allows us to use the same
 * advertisement structure yet identify the pipes which will understand
 * the protocol used by the SRDI socket transfer provider.
 */
public class SRDISocketContentShareAdvertisementImpl
        extends AbstractPipeContentShareAdvertisement {
    /**
     * ContentID field.
     */
    private static final String contentIDTag = "ContentID";

    /**
     * Fields to index on.
     */
    private static final String [] fields = {
        contentIDTag
    };

    /**
     *  Returns the identifying type of this Advertisement.
     *
     * @return String the type of advertisement
     */
    public static String getAdvertisementType() {
        return "jxta:SRDISocketContent";
    }

    /**
     * Instantiator for this Advertisement type.
     */
    public static class Instantiator
            implements AdvertisementFactory.Instantiator {
        /**
         *  {@inheritDoc}
         */
        public String getAdvertisementType() {
            return SRDISocketContentShareAdvertisementImpl.getAdvertisementType();
        }

        /**
         *  {@inheritDoc}
         */
        public Advertisement newInstance() {
            return new SRDISocketContentShareAdvertisementImpl();
        }

        /**
         *  {@inheritDoc}
         */
        public Advertisement newInstance( Element root ) {
            return new SRDISocketContentShareAdvertisementImpl( root );
        }
    };

    /**
     *  Construct a new AbstractPipeContentAdvertisement.
     */
    public SRDISocketContentShareAdvertisementImpl() {
        // Empty.
    }

    /**
     *  Construct a new AbstractPipeContentAdvertisement.
     */
    public SRDISocketContentShareAdvertisementImpl(Element root) {
        super(root);
    }

    /**
     * Clone this SRDISocketContentAdvertisement.
     *
     * @return a copy of this SRDISocketContentAdvertisement
     */
    @Override
    public SRDISocketContentShareAdvertisementImpl clone() {
        // All members are either immutable or never modified nor allowed to
        // be modified: all accessors return clones.
        SRDISocketContentShareAdvertisementImpl clone =
                (SRDISocketContentShareAdvertisementImpl) super.clone();
        return clone;
    }

    /**
     *  {@inheritDoc}
     */
    @Override
    public String [] getIndexFields() {
        return fields;
    }

}
