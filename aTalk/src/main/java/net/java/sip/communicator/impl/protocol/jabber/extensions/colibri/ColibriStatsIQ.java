/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license. See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.jabber.extensions.colibri;

import org.jivesoftware.smack.packet.IQ;

/**
 * The stats IQ that can be used to request Colibri stats on demand (used in server side focus).
 *
 * @author Pawel Domas
 * @author Eng Chong Meng
 */
public class ColibriStatsIQ extends IQ
{
    /**
     * The XML element name of the Jitsi Videobridge <tt>stats</tt> extension.
     */
    public static final String ELEMENT_NAME = ColibriStatsExtension.ELEMENT_NAME;

    /**
     * The XML COnferencing with LIghtweight BRIdging namespace of the Jitsi Videobridge
     * <tt>stats</tt> extension.
     */
    public static final String NAMESPACE = ColibriStatsExtension.NAMESPACE;

    private final ColibriStatsExtension backEnd = new ColibriStatsExtension();

    public ColibriStatsIQ()
    {
        super(ELEMENT_NAME, NAMESPACE);
    }

    /**
     * Adds stat extension.
     *
     * @param stat the stat to be added
     */
    public void addStat(ColibriStatsExtension.Stat stat)
    {
        backEnd.addStat(stat);
    }

    @Override
    protected IQChildElementXmlStringBuilder getIQChildElementBuilder(IQChildElementXmlStringBuilder xml)
    {
        xml.append('>');
        xml.append(backEnd.toXML());
        return xml;
    }
}
