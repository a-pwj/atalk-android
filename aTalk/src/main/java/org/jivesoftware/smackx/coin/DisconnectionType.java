/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license. See terms of license at gnu.org.
 */
package org.jivesoftware.smackx.coin;

/**
 * Disconnection type.
 *
 * @author Sebastien Vincent
 */
public enum DisconnectionType
{
    /**
     * Departed.
     */
    departed("departed"),

    /**
     * Booted.
     */
    booted("booted"),

    /**
     * Failed.
     */
    failed("failed"),

    /**
     * Busy
     */
    busy("busy");

    /**
     * The name of this type.
     */
    private final String type;

    /**
     * Creates a <code>DisconnectionType</code> instance with the specified name.
     *
     * @param type type name.
     */
    private DisconnectionType(String type)
    {
        this.type = type;
    }

    /**
     * Returns the type name.
     *
     * @return type name
     */
    @Override
    public String toString()
    {
        return type;
    }

    /**
     * Returns a <code>DisconnectionType</code>.
     *
     * @param typeStr the <code>String</code> that we'd like to parse.
     * @return an DisconnectionType.
     * @throws IllegalArgumentException in case <code>typeStr</code> is not a valid <code>EndPointType</code>.
     */
    public static DisconnectionType fromString(String typeStr)
            throws IllegalArgumentException
    {
        for (DisconnectionType value : values())
            if (value.toString().equals(typeStr))
                return value;

        throw new IllegalArgumentException(typeStr + " is not a valid reason");
    }
}
