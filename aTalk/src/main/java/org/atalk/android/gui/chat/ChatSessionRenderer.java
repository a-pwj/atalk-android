/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package org.atalk.android.gui.chat;

import net.java.sip.communicator.service.protocol.ConferenceDescription;

import javax.swing.Icon;

/**
 * The <code>ChatSessionRenderer</code> is the connector between the
 * <code>ChatSession</code> and the <code>ChatPanel</code>, which represents the UI
 * part of the chat.
 *
 * @author Yana Stamcheva
 * @author Eng Chong Meng
 */
public interface ChatSessionRenderer
{
    /**
     * Sets the name of the given chat contact.
     *
     * @param chatContact the chat contact to be modified.
     * @param name the new name.
     */
    void setContactName(ChatContact<?> chatContact, String name);

    /**
     * Adds the given chat transport to the UI.
     *
     * @param chatTransport the chat transport to add.
     */
    void addChatTransport(ChatTransport chatTransport);

    /**
     * Removes the given chat transport from the UI.
     *
     * @param chatTransport the chat transport to remove.
     */
    void removeChatTransport(ChatTransport chatTransport);

    /**
     * Adds the given chat contact to the UI.
     *
     * @param chatContact the chat contact to add.
     */
    void addChatContact(ChatContact<?> chatContact);

    /**
     * Removes the given chat contact from the UI.
     *
     * @param chatContact the chat contact to remove.
     */
    void removeChatContact(ChatContact<?> chatContact);

    /**
     * Removes all chat contacts from the contact list of the chat.
     */
    void removeAllChatContacts();

    /**
     * Updates the status of the given chat transport.
     *
     * @param chatTransport the chat transport to update.
     */
    void updateChatTransportStatus(ChatTransport chatTransport);

    /**
     * Sets the given <code>chatTransport</code> to be the selected chat transport.
     *
     * @param chatTransport the <code>ChatTransport</code> to select
     * @param isMessageOrFileTransferReceived Boolean telling us if this change
     * of the chat transport correspond to an effective switch to this new
     * transform (a mesaage received from this transport, or a file transfer
     * request received, or if the resource timeouted), or just a status update
     * telling us a new chatTransport is now available (i.e. another device has startup).
     */
    void setSelectedChatTransport(ChatTransport chatTransport,boolean isMessageOrFileTransferReceived);

    /**
     * Updates the status of the given chat contact.
     *
     * @param chatContact the chat contact to update.
     * @param statusMessage the status message to show to the user.
     */
    void updateChatContactStatus(ChatContact<?> chatContact, String statusMessage);

    /**
     * Sets the chat subject.
     *
     * @param subject the new subject to set.
     */
    void setChatSubject(String subject);

    /**
     * Sets the chat icon.
     *
     * @param icon the chat icon to set
     */
    void setChatIcon(Icon icon);

    /**
     * Adds the given <code>conferenceDescription</code> to the list of chat
     * conferences in this chat renderer.
     * @param conferenceDescription the conference to add.
     */
    void addChatConferenceCall(ConferenceDescription conferenceDescription);

    /**
     * Removes the given <code>conferenceDescription</code> from the list of chat
     * conferences in this chat panel chat.
     * @param conferenceDescription the conference to remove.
     */
    void removeChatConferenceCall(ConferenceDescription conferenceDescription);
    
    /**
     * Sets the visibility of conferences panel to <code>true</code> or <code>false</code>
     * 
     * @param isVisible if <code>true</code> the panel is visible.
     */
    void setConferencesPanelVisible(boolean isVisible);
    
    /**
     * This method is called when the local user publishes a  <code>ConferenceDescription</code> instance.
     * 
     * @param conferenceDescription the <code>ConferenceDescription</code> instance
     * associated with the conference.
     */
    void chatConferenceDescriptionSent(ConferenceDescription conferenceDescription);
}
