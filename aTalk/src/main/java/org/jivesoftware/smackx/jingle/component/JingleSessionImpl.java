/**
 *
 * Copyright 2017-2022 Paul Schaub
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
package org.jivesoftware.smackx.jingle.component;

import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.StanzaError;
import org.jivesoftware.smack.util.Async;
import org.jivesoftware.smack.util.StringUtils;
import org.jivesoftware.smackx.jingle.JingleDescriptionManager;
import org.jivesoftware.smackx.jingle.JingleManager;
import org.jivesoftware.smackx.jingle.JingleSession;
import org.jivesoftware.smackx.jingle.JingleUtil;
import org.jivesoftware.smackx.jingle.Role;
import org.jivesoftware.smackx.jingle.element.Jingle;
import org.jivesoftware.smackx.jingle.element.JingleAction;
import org.jivesoftware.smackx.jingle.element.JingleContent;
import org.jivesoftware.smackx.jingle.element.JingleReason;
import org.jivesoftware.smackx.jingle.provider.JingleContentProviderManager;
import org.jivesoftware.smackx.jingle_filetransfer.component.JingleFileTransferImpl;
import org.jxmpp.jid.FullJid;

import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Class that represents a Jingle session.
 *
 * @author Paul Schaub
 * @author Eng Chong Meng
 */
public class JingleSessionImpl extends JingleSession
{
    private static final Logger LOGGER = Logger.getLogger(JingleSessionImpl.class.getName());

    private final ConcurrentHashMap<String, JingleContentImpl> contentImpls = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, JingleContentImpl> proposedContentImpls = new ConcurrentHashMap<>();

    private final JingleManager mManager;
    private final XMPPConnection mConnection;
    private final JingleUtil jutil;
    private SessionState sessionState;

    public enum SessionState
    {
        fresh,      // pre-session-initiate
        pending,    // pre-session-accept
        active,     // pre-session-terminate
        ended       // post-session-terminate
    }

    /**
     * Create a JingleSessionHandler to start a session-initiate for sending file.
     *
     * @param connection XMPPConnection
     * @param recipient The remote file-recipient
     */
    public JingleSessionImpl(XMPPConnection connection, FullJid recipient)
    {
        this(connection, connection.getUser(), recipient, Role.initiator, StringUtils.randomString(24), null);
    }

    /**
     * Create a JingleSessionHandler for the newly received jingleSI (session-initiate).
     *
     * @param connection XMPPConnection
     * @param jingleSI The received JingleIQ for session-initiate
     */
    public JingleSessionImpl(XMPPConnection connection, Jingle jingleSI)
    {
        this(connection, jingleSI.getInitiator(), connection.getUser(), Role.responder, jingleSI.getSid(), jingleSI.getContents());
        for (JingleContent content : getContents()) {
            this.addContent(content);
        }
        sessionState = SessionState.pending;
    }

    /**
     * Construct for the JingleSessionImpl for both session-initiate or session-accept.
     *
     * @param connection XMPPConnection
     * @param initiator JingleSI initiator
     * @param responder JingleSI responder
     * @param role Acting role for the this session-initial
     * @param sid Jingle session Id
     * @param contents Jingle contents
     */
    public JingleSessionImpl(XMPPConnection connection, FullJid initiator, FullJid responder, Role role, String sid, List<JingleContent> contents)
    {
        super(initiator, responder, role, sid, contents);
        sessionState = SessionState.fresh;

        mConnection = connection;
        jutil = new JingleUtil(connection);
        mManager = JingleManager.getInstanceFor(connection);
        mManager.registerJingleSessionHandler(getRemote(), sid, this);
    }

    public void sendInitiate(XMPPConnection connection) throws SmackException.NotConnectedException, InterruptedException, XMPPException.XMPPErrorException, SmackException.NoResponseException
    {
        if (this.sessionState != SessionState.fresh) {
            throw new IllegalStateException("Session is not in fresh state.");
        }

        connection.createStanzaCollectorAndSend(createSessionInitiate()).nextResultOrThrow();
        this.sessionState = SessionState.pending;
    }

    public void sendAccept(XMPPConnection connection) throws SmackException.NotConnectedException, InterruptedException, XMPPException.XMPPErrorException, SmackException.NoResponseException
    {
        LOGGER.log(Level.INFO, "Accepted session.");
        if (this.sessionState != SessionState.pending) {
            throw new IllegalStateException("Session is not in pending state.");
        }

        if (contentImpls.values().size() == 0) {
            LOGGER.log(Level.WARNING, "0 contents!");
        }

        for (JingleContentImpl content : contentImpls.values()) {
            content.start(connection);
        }

        connection.createStanzaCollectorAndSend(createSessionAccept()).nextResultOrThrow();
        this.sessionState = SessionState.active;
    }

    public Jingle createSessionInitiate()
    {
        if (role != Role.initiator) {
            throw new IllegalStateException("Sessions role is not initiator.");
        }

        Jingle.Builder jb = Jingle.builder(mConnection);
        jb.setAction(JingleAction.session_initiate)
                .setSessionId(sid)
                .setInitiator(getInitiator());

        for (JingleContentImpl c : contentImpls.values()) {
            jb.addJingleContent(c.getElement());
        }

        Jingle jingleSI = jb.build();
        jingleSI.setFrom(mConnection.getUser());
        jingleSI.setTo(getResponder());

        return jingleSI;
    }

    public Jingle createSessionAccept()
    {
        if (role != Role.responder) {
            throw new IllegalStateException("Sessions role is not responder.");
        }

        Jingle.Builder jb = Jingle.builder(mConnection);
        jb.setResponder(mConnection.getUser())
                .setAction(JingleAction.session_accept)
                .setSessionId(sid);

        for (JingleContentImpl c : contentImpls.values()) {
            jb.addJingleContent(c.getElement());
        }

        Jingle jingleSA = jb.build();
        jingleSA.setTo(getInitiator());
        jingleSA.setFrom(getResponder());

        return jingleSA;
    }

    void onContentFinished(JingleContentImpl jingleContent)
    {
        if (contentImpls.get(jingleContent.getName()) == null) {
            LOGGER.log(Level.WARNING, "Session does not contain content " + jingleContent.getName() + ". Ignore contentFinished.");
            return;
        }

        if (contentImpls.size() == 1) {
            // Only content has finished. End session.
            terminateSession(JingleReason.Success);
        }

        // Session has still active contents left.
        /*
        try {
            mConnection.createStanzaCollectorAndSend(Jingle.createSessionTerminateContentCancel(
                    getPeer(), getSessionId(), jingleContent.getCreator(), jingleContent.getName()));
        } catch (SmackException.NotConnectedException | InterruptedException e) {
            LOGGER.log(Level.SEVERE, "Could not send content-cancel: " + e, e);
        }
        contents.remove(jingleContent.getName());
        */
    }

    void onContentCancel(JingleContentImpl jingleContent)
    {
        if (contentImpls.get(jingleContent.getName()) == null) {
            LOGGER.log(Level.WARNING, "Session does not contain content " + jingleContent.getName() + ". Ignore onContentCancel.");
            return;
        }

        if (contentImpls.size() == 1) {
            mManager.unregisterJingleSessionHandler(remote, sid, this);
        }
        else {
            try {
                jutil.sendSessionTerminateContentCancel(remote, sid, jingleContent.getCreator(), jingleContent.getName());
            } catch (SmackException.NotConnectedException | InterruptedException
                    | XMPPException.XMPPErrorException | SmackException.NoResponseException e) {
                LOGGER.log(Level.SEVERE, "Could not send content-cancel: " + e, e);
            }
        }
        contentImpls.remove(jingleContent.getName());
    }

    public void terminateSession(JingleReason reason)
    {
        try {
            mConnection.createStanzaCollectorAndSend(jutil.createSessionTerminate(remote, sid, reason));
        } catch (SmackException.NotConnectedException | InterruptedException e) {
            LOGGER.log(Level.SEVERE, "Could not send session-terminate: " + e, e);
        }
        mManager.unregisterJingleSessionHandler(remote, sid, this);
    }

    public IQ handleJingleSessionRequest(Jingle request)
    {
        switch (request.getAction()) {

            case content_modify:
            case description_info:
            case security_info:
            case session_info:
            case transport_accept:
            case transport_info:
            case transport_reject:
            case transport_replace:
                return getSoleAffectedContentOrThrow(request).handleJingleRequest(request, mConnection);

            case content_accept:
                return handleContentAccept(request);
            case content_add:
                return handleContentAdd(request);
            case content_reject:
                return handleContentReject(request);
            case content_remove:
                return handleContentRemove(request);
            case session_accept:
                return handleSessionAccept(request);
            case session_initiate:
                return handleSessionInitiate(request);
            case session_terminate:
                return handleSessionTerminate(request);
            default:
                throw new AssertionError("Illegal jingle action: " + request.getAction());
        }
    }

    /* ############## Processed in this class ############## */

    /**
     * Handle incoming session-accept stanza.
     * Proceed only if JingleSecurity requirement matched between session-accept and session-initiate
     *
     * @param sessionAccept session-accept stanza.
     * @return IQResult.
     */
    @Override
    protected IQ handleSessionAccept(final Jingle sessionAccept)
    {
        this.sessionState = SessionState.active;

        for (final JingleContentImpl content : contentImpls.values()) {
            JingleSecurity<?> security = content.getSecurity();
            if (security != null && sessionAccept.getSoleContentOrThrow().getSecurity() == null) {
                JingleFileTransferImpl jfTransfer = ((JingleFileTransferImpl) content.getDescription());
                jfTransfer.notifyProgressListenersOnError(JingleReason.SecurityError, "JetSecurity protocol not supported by client");
                terminateSession(JingleReason.SecurityError);
                contentImpls.remove(content.getName());
                continue;
            }
            Async.go(() -> content.handleSessionAccept(sessionAccept, mConnection));
        }
        return IQ.createResultIQ(sessionAccept);
    }

    @Override
    protected IQ handleSessionInitiate(Jingle sessionInitiate)
    {
        LOGGER.log(Level.INFO, "Create new session with '" + remote + "': " + sid);
        final JingleDescription<?> description = getSoleContentOrThrow().getDescription();
        final JingleDescriptionManager descriptionManager
                = JingleContentProviderManager.getDescriptionManager(description.getNamespace());

        Async.go(() -> {
            if (descriptionManager == null) {
                LOGGER.log(Level.WARNING, "Unsupported description type: " + description.getNamespace());
                try {
                    jutil.sendSessionTerminateUnsupportedApplications(remote, sid);
                } catch (SmackException.NotConnectedException | InterruptedException
                        | XMPPException.XMPPErrorException | SmackException.NoResponseException e) {
                    LOGGER.log(Level.SEVERE, "Could not send session-terminate: " + e, e);
                }
            }
            else {
                descriptionManager.notifySessionInitiate(JingleSessionImpl.this);
            }
        });
        return IQ.createResultIQ(sessionInitiate);
    }

    @Override
    protected IQ handleSessionTerminate(Jingle sessionTerminate)
    {
        this.sessionState = SessionState.ended;
        JingleReason reason = sessionTerminate.getReason();

        if (reason == null) {
            throw new AssertionError("Reason MUST not be null! (I guess)...");
        }

        // Inform the client on session terminated.
        JingleFileTransferImpl jfTransfer = ((JingleFileTransferImpl) getSoleContentOrThrow().getDescription());
        jfTransfer.notifyProgressListenersOnSessionTerminate(reason);

        mManager.unregisterJingleSessionHandler(remote, sid, this);
        return IQ.createResultIQ(sessionTerminate);
    }

    @Override
    protected IQ handleContentAccept(final Jingle contentAccept)
    {
        for (JingleContent a : contentAccept.getContents()) {
            final JingleContentImpl accepted = proposedContentImpls.get(a.getName());

            if (accepted == null) {
                throw new AssertionError("Illegal content name!");
            }

            proposedContentImpls.remove(accepted.getName());
            contentImpls.put(accepted.getName(), accepted);

            Async.go(() -> accepted.handleContentAccept(contentAccept, mConnection));
        }
        return IQ.createResultIQ(contentAccept);
    }

    @Override
    protected IQ handleContentAdd(Jingle contentAdd)
    {
        final JingleContentImpl proposed = getSoleProposedContentOrThrow(contentAdd);

        final JingleDescriptionManager descriptionManager
                = JingleContentProviderManager.getDescriptionManager(proposed.getDescription().getNamespace());

        if (descriptionManager == null) {
            throw new AssertionError("DescriptionManager is null: " + proposed.getDescription().getNamespace());
        }

        Async.go(() -> descriptionManager.notifyContentAdd(JingleSessionImpl.this, proposed));
        return IQ.createResultIQ(contentAdd);
    }

    @Override
    protected IQ handleContentReject(Jingle contentReject)
    {
        for (JingleContent r : contentReject.getContents()) {
            final JingleContentImpl rejected = proposedContentImpls.get(r.getName());

            if (rejected == null) {
                throw new AssertionError("Illegal content name!");
            }

            proposedContentImpls.remove(rejected.getName());
            // Async.go(() -> rejected.handleContentReject(request, mConnection);
        }
        return IQ.createResultIQ(contentReject);
    }

    @Override
    protected IQ handleContentRemove(final Jingle contentRemove)
    {
        return IQ.createErrorResponse(contentRemove, StanzaError.Condition.feature_not_implemented);
        /*
        for (JingleContentImpl r : request.getContents()) {
            final JingleContentImpl removed = contents.get(r.getName());

            if (removed == null) {
                throw new AssertionError("Illegal content name!");
            }
            contents.remove(removed.getName());
            Async.go(() ->  removed.handleContentRemove(JingleSessionImpl.this, mConnection);
        }

        return IQ.createResultIQ(request);
        */
    }

    protected HashMap<JingleContent, JingleContentImpl> getAffectedContents(Jingle request)
    {
        HashMap<JingleContent, JingleContentImpl> map = new HashMap<>();
        for (org.jivesoftware.smackx.jingle.element.JingleContent e : request.getContents()) {
            JingleContentImpl c = contentImpls.get(e.getName());
            if (c == null) {
                throw new AssertionError("Unknown content: " + e.getName());
            }
            map.put(e, c);
        }
        return map;
    }

    protected JingleContentImpl getSoleAffectedContentOrThrow(Jingle request)
    {
        if (request.getContents().size() != 1) {
            throw new AssertionError("More/less than 1 content in request!");
        }

        JingleContentImpl content = contentImpls.get(request.getContents().get(0).getName());
        if (content == null) {
            throw new AssertionError("Illegal content name!");
        }
        return content;
    }

    protected JingleContentImpl getSoleProposedContentOrThrow(Jingle request)
    {
        if (request.getContents().size() != 1) {
            throw new AssertionError("More/less than 1 content in request!");
        }

        return JingleContentImpl.fromElement(mConnection, request.getContents().get(0));
    }

    public void addContentImpl(JingleContentImpl content)
    {
        if (contentImpls.get(content.getName()) != null) {
            throw new IllegalArgumentException("Session already contains a content with the name " + content.getName());
        }
        contentImpls.put(content.getName(), content);
        content.setParent(this);
    }

    public void addContent(JingleContent content)
    {
        addContentImpl(JingleContentImpl.fromElement(mConnection, content));
    }

    public ConcurrentHashMap<String, JingleContentImpl> getContentImpls()
    {
        return contentImpls;
    }

    public JingleContentImpl getContentImpl(String name)
    {
        return contentImpls.get(name);
    }

    /**
     * Get the only jingle content if one exists, or <code>null</code>. This method will throw an
     * {@link IllegalStateException} if there is more than one jingle content.
     *
     * @return a JingleContent instance or <code>null</code>.
     * @throws IllegalStateException if there is more than one jingle content.
     */
    public JingleContentImpl getSoleContentOrThrow()
    {
        if (contentImpls.isEmpty()) {
            return null;
        }

        if (contentImpls.size() > 1) {
            throw new IllegalStateException();
        }
        return contentImpls.values().iterator().next();
    }

    public SessionState getSessionState()
    {
        return sessionState;
    }

    @Override
    public XMPPConnection getConnection()
    {
        return mConnection;
    }

    @Override
    public void onTransportMethodFailed(String namespace)
    {
    }
}
