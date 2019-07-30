/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Copyright @ 2018 Atlassian Pty Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.xmpp.extensions.jitsimeet;

import org.jivesoftware.smack.packet.ExtensionElement;
import org.jivesoftware.smack.packet.XmlEnvironment;
import org.jivesoftware.smack.provider.ExtensionElementProvider;
import org.jivesoftware.smack.util.XmlStringBuilder;
import org.jivesoftware.smack.xml.XmlPullParser;
import org.jivesoftware.smack.xml.XmlPullParserException;

import java.io.IOException;

/**
 * An extension to the Presence used in jitsi-meet when deployed in an
 * authenticated environment (such as Stride),
 * which stores information about an user and the group of an user
 *
 * The extension looks like the following:
 * <pre>
 *     {@code
 *      <identity>
 *          <user>
 *              <id>some_unique_id</id>
 *              <name>some_name</name>
 *              <avatar>some_url_to_an_avatar</avatar>
 *          </user>
 *          <group>some_unique_id</group>
 *      </identity>
 *     }
 * </pre>
 *
 * @author Nik Vaessen
 * @author Eng Chong Meng
 */
public class IdentityExtensionElement implements ExtensionElement
{
    /**
     * The namespace (xmlns attribute) of this identity presence element
     */
    public static final String NAMESPACE = "jabber:client";

    /**
     * The element name of this identity presence element
     */
    public static final String ELEMENT_NAME = "identity";

    /**
     * The child element of this identity storing information about the user
     */
    public static final String USER_ELEMENT_NAME = "user";

    /**
     * The child element of this identity storing information about the group
     * id
     */
    public static final String GROUP_ELEMENT_NAME = "group";

    /**
     * The child element of the user element storing the user id
     */
    public static final String USER_ID_ELEMENT_NAME = "id";

    /**
     * The child element of the user element storing the user avatar-url
     */
    public static final String USER_AVATAR_URL_ELEMENT_NAME = "avatar";

    /**
     * The child element of the user element storing the user name
     */
    public static final String USER_NAME_ELEMENT_NAME = "name";

    /**
     * The unique ID belonging to the user
     */
    private String userId;

    /**
     * The (non-unique) name of the user
     */
    private String userName;

    /**
     * The URL to the avatar set by the user
     */
    private String userAvatarUrl;

    /**
     * The group ID of the group the user belongs to.
     */
    private String groupId;

    /**
     * Create an instance of this identity element, which stores the user's ID,
     * user's name, the user's avatar-url and the group ID the user belongs to
     *
     * @param userId the id of the user
     * @param userName the name of the user
     * @param userAvatarUrl the avatar-url of the user
     * @param groupId the group id of group the user belongs to
     */
    public IdentityExtensionElement(String userId, String userName, String userAvatarUrl, String groupId)
    {
        this.userId = userId;
        this.userName = userName;
        this.userAvatarUrl = userAvatarUrl;
        this.groupId = groupId;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getNamespace()
    {
        return NAMESPACE;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getElementName()
    {
        return ELEMENT_NAME;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CharSequence toXML(XmlEnvironment xmlEnvironment)
    {
        XmlStringBuilder xml = new XmlStringBuilder();

        // begin identity
        xml.openElement(ELEMENT_NAME);

        //begin user
        xml.openElement(USER_ELEMENT_NAME);

        xml.element(USER_ID_ELEMENT_NAME, getUserId());
        xml.element(USER_NAME_ELEMENT_NAME, getUserName());
        xml.element(USER_AVATAR_URL_ELEMENT_NAME, getUserAvatarUrl());

        // end user
        xml.closeElement(USER_ELEMENT_NAME);

        // begin and end group
        xml.element(GROUP_ELEMENT_NAME, getGroupId());

        // end identity
        xml.closeElement(ELEMENT_NAME);
        return xml.toString();
    }

    /**
     * Get the unique user ID
     *
     * @return the user ID
     */
    public String getUserId()
    {
        return userId;
    }

    /**
     * Get the name of the user
     *
     * @return the user's name
     */
    public String getUserName()
    {
        return userName;
    }

    /**
     * Get the avatar-url of the user
     *
     * @return the avatar-url
     */
    public String getUserAvatarUrl()
    {
        return userAvatarUrl;
    }

    /**
     * Get the id of the group of the user
     *
     * @return the group id
     */
    public String getGroupId()
    {
        return groupId;
    }

    /**
     * The {@link ExtensionElementProvider} which can create an instance of a
     * {@link IdentityExtensionElement} when given the
     * {@link XmlPullParser} of an identity element
     */
    public static class Provider extends ExtensionElementProvider<IdentityExtensionElement>
    {
        /**
         * {@inheritDoc}
         */
        @Override
        public IdentityExtensionElement parse(XmlPullParser parser, int depth, XmlEnvironment xmlEnvironment)
                throws IOException, XmlPullParserException
        {
            String currentTag = parser.getName();
            if (!NAMESPACE.equals(parser.getNamespace())) {
                return null;
            }
            else if (!ELEMENT_NAME.equals(currentTag)) {
                return null;
            }

            String userId = null;
            String userName = null;
            String userAvatarUrl = null;
            String groupId = null;

            do {
                parser.next();

                if (parser.getEventType() == XmlPullParser.Event.START_ELEMENT) {
                    currentTag = parser.getName();
                }
                else if (parser.getEventType() == XmlPullParser.Event.TEXT_CHARACTERS) {
                    switch (currentTag) {
                        case USER_AVATAR_URL_ELEMENT_NAME:
                            userAvatarUrl = parser.getText();
                            break;
                        case USER_ID_ELEMENT_NAME:
                            userId = parser.getText();
                            break;
                        case USER_NAME_ELEMENT_NAME:
                            userName = parser.getText();
                            break;
                        case GROUP_ELEMENT_NAME:
                            groupId = parser.getText();
                            break;
                        default:
                            break;
                    }
                }
                else if (parser.getEventType() == XmlPullParser.Event.END_ELEMENT) {
                    currentTag = parser.getName();
                }
            }
            while (!ELEMENT_NAME.equals(currentTag));

            if (userAvatarUrl != null && userId != null && userName != null
                    && groupId != null) {
                return new IdentityExtensionElement(userId, userName, userAvatarUrl, groupId);
            }
            else {
                return null;
            }
        }
    }
}

