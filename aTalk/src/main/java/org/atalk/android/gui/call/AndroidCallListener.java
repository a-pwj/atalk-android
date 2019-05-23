/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license. See terms of license at gnu.org.
 */
package org.atalk.android.gui.call;

import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;

import net.java.sip.communicator.service.notification.NotificationData;
import net.java.sip.communicator.service.notification.NotificationService;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.event.*;
import net.java.sip.communicator.util.GuiUtils;
import net.java.sip.communicator.util.ServiceUtils;

import org.atalk.android.R;
import org.atalk.android.aTalkApp;
import org.atalk.android.gui.AndroidGUIActivator;
import org.atalk.android.gui.util.AndroidUtils;
import org.atalk.android.plugin.notificationwiring.AndroidNotifications;

import java.util.*;

import timber.log.Timber;

/**
 * A utility implementation of the {@link CallListener} interface which delivers the <tt>CallEvent</tt>s to the AWT
 * event dispatching thread.
 *
 * @author Yana Stamcheva
 * @author Pawel Domas
 * @author Eng Chong Meng
 */
public class AndroidCallListener implements CallListener, CallChangeListener
{
    /**
     * The application context.
     */
    private final Context appContext = aTalkApp.getGlobalContext();

    /*
     * Flag stores speakerphone status to be restored to initial value once the call has ended.
     */
    private Boolean speakerPhoneBeforeCall;

    /**
     * {@inheritDoc}
     *
     * Delivers the <tt>CallEvent</tt> to the AWT event dispatching thread.
     */
    public void callEnded(CallEvent ev)
    {
        onCallEvent(ev);
        ev.getSourceCall().removeCallChangeListener(this);
    }

    /**
     * {@inheritDoc}
     *
     * Delivers the <tt>CallEvent</tt> to the AWT event dispatching thread.
     */
    public void incomingCallReceived(CallEvent ev)
    {
        onCallEvent(ev);
        ev.getSourceCall().addCallChangeListener(this);
    }

    /**
     * Notifies this <tt>CallListener</tt> about a specific <tt>CallEvent</tt>. Executes in whichever thread brought the
     * event to this listener. Delivers the event to the AWT event dispatching thread.
     *
     * @param evt the <tt>CallEvent</tt> this <tt>CallListener</tt> is being notified about
     */
    protected void onCallEvent(final CallEvent evt)
    {
        switch (evt.getEventID()) {
            case CallEvent.CALL_ENDED:
                // Call Activity must close itself
                // startHomeActivity(evt);
                // Clears the in call notification
                AndroidUtils.clearGeneralNotification(appContext);
                // Removes the call from active calls list
                CallManager.removeActiveCall(evt.getSourceCall());
                // Restores speakerphone status
                restoreSpeakerPhoneStatus();
                break;

            case CallEvent.CALL_INITIATED:
                // Stores speakerphone status to be restored after the call has ended.
                storeSpeakerPhoneStatus();
                clearVideoCallState();

                startVideoCallActivity(evt);
                break;

            case CallEvent.CALL_RECEIVED:
                if (CallManager.getActiveCallsCount() > 0) {
                    // Reject if there are active calls
                    CallManager.hangupCall(evt.getSourceCall());

                    // cmeng - answer call and on hold current - mic not working
                    // startReceivedCallActivity(evt);

                    // merge call - exception
                    // CallManager.answerCallInFirstExistingCall(evt.getSourceCall());
                }
                else {
                    // Stores speakerphone status to be restored after the call has ended.
                    storeSpeakerPhoneStatus();
                    clearVideoCallState();
                    startReceivedCallActivity(evt);
                }
                break;
        }
    }

    /**
     * Clears call state stored in previous calls.
     */
    private void clearVideoCallState()
    {
        VideoCallActivity.callState = new VideoCallActivity.CallStateHolder();
    }

    /**
     * Stores speakerphone status for the call duration.
     */
    private void storeSpeakerPhoneStatus()
    {
        AudioManager audioManager = aTalkApp.getAudioManager();
        this.speakerPhoneBeforeCall = audioManager.isSpeakerphoneOn();
        Timber.d("Storing speakerphone status: %s", speakerPhoneBeforeCall);
    }

    /**
     * Restores speakerphone status.
     */
    private void restoreSpeakerPhoneStatus()
    {
        if (speakerPhoneBeforeCall != null) {
            AudioManager audioManager = aTalkApp.getAudioManager();
            audioManager.setSpeakerphoneOn(speakerPhoneBeforeCall);
            Timber.d("Restoring speakerphone to: %s", speakerPhoneBeforeCall);
            speakerPhoneBeforeCall = null;
        }
    }

    /**
     * {@inheritDoc}
     *
     * Delivers the <tt>CallEvent</tt> to the AWT event dispatching thread.
     */
    public void outgoingCallCreated(CallEvent ev)
    {
        onCallEvent(ev);
    }

    /**
     * Starts the incoming (received) call activity.
     *
     * @param evt the <tt>CallEvent</tt>
     */
    private void startReceivedCallActivity(final CallEvent evt)
    {
        new Thread()
        {
            public void run()
            {
                Call incomingCall = evt.getSourceCall();
                Intent receivedCallIntent = new Intent(appContext, ReceivedCallActivity.class);
                receivedCallIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

                String identifier = CallManager.addActiveCall(incomingCall);
                receivedCallIntent.putExtra(CallManager.CALL_IDENTIFIER, identifier);

                receivedCallIntent.putExtra(CallManager.CALLEE_DISPLAY_NAME, CallUIUtils.getCalleeDisplayName(incomingCall));
                receivedCallIntent.putExtra(CallManager.CALLEE_ADDRESS, CallUIUtils.getCalleeAddress(incomingCall));
                receivedCallIntent.putExtra(CallManager.CALLEE_AVATAR, CallUIUtils.getCalleeAvatar(incomingCall));
                appContext.startActivity(receivedCallIntent);
            }
        }.start();
    }

    /**
     * Starts the video call activity when a call has been started.
     *
     * @param evt the <tt>CallEvent</tt> that notified us
     */
    private void startVideoCallActivity(final CallEvent evt)
    {
        String callIdentifier = CallManager.addActiveCall(evt.getSourceCall());
        Intent videoCall = VideoCallActivity.createVideoCallIntent(appContext, callIdentifier);
        videoCall.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        appContext.startActivity(videoCall);
    }

    @Override
    public void callPeerAdded(CallPeerEvent callPeerEvent)
    {
    }

    @Override
    public void callPeerRemoved(CallPeerEvent callPeerEvent)
    {
    }

    @Override
    public void callStateChanged(CallChangeEvent evt)
    {
        if (CallState.CALL_ENDED.equals(evt.getNewValue())) {
            if (CallState.CALL_INITIALIZATION.equals(evt.getOldValue())) {
                if (evt.getCause() != null
                        && evt.getCause().getReasonCode() != CallPeerChangeEvent.NORMAL_CALL_CLEARING) {
                    // Missed call
                    fireMissedCallNotification(evt);
                }
            }
        }
    }

    /**
     * Fires missed call notification for given <tt>CallChangeEvent</tt>.
     *
     * @param evt the <tt>CallChangeEvent</tt> that describes missed call.
     */
    private void fireMissedCallNotification(CallChangeEvent evt)
    {
        NotificationService notificationService
                = ServiceUtils.getService(AndroidGUIActivator.bundleContext, NotificationService.class);

        Contact contact = evt.getCause().getSourceCallPeer().getContact();
        if ((contact == null) || (notificationService == null)) {
            Timber.w("No contact found - missed call notification skipped");
            return;
        }

        Map<String, Object> extras = new HashMap<>();
        extras.put(NotificationData.POPUP_MESSAGE_HANDLER_TAG_EXTRA, contact);

        byte[] contactIcon = contact.getImage();
        Date when = new Date();

        notificationService.fireNotification(AndroidNotifications.MISSED_CALL,
                aTalkApp.getResString(R.string.service_gui_MISSED_CALLS_TOOL_TIP), contact.getDisplayName() + " "
                        + GuiUtils.formatTime(when) + " " + GuiUtils.formatDate(when), contactIcon, extras);
    }
}