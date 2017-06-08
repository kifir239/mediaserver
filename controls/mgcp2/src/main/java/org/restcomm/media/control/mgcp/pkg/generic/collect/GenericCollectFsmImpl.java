/*
 * TeleStax, Open Source Cloud Communications
 * Copyright 2011-2016, Telestax Inc and individual contributors
 * by the @authors tag. 
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.restcomm.media.control.mgcp.pkg.generic.collect;

import com.google.common.util.concurrent.ListeningScheduledExecutorService;
import org.apache.log4j.Logger;
import org.restcomm.media.control.mgcp.pkg.MgcpEventSubject;
import org.restcomm.media.control.mgcp.pkg.au.Playlist;
import org.restcomm.media.control.mgcp.pkg.au.ReturnCode;
import org.restcomm.media.resource.asr.AsrEngine;
import org.restcomm.media.spi.ResourceUnavailableException;
import org.restcomm.media.spi.dtmf.DtmfDetector;
import org.restcomm.media.spi.dtmf.DtmfDetectorListener;
import org.restcomm.media.spi.dtmf.DtmfEvent;
import org.restcomm.media.spi.listener.TooManyListenersException;
import org.restcomm.media.spi.player.Player;
import org.restcomm.media.spi.player.PlayerEvent;
import org.restcomm.media.spi.player.PlayerListener;
import org.squirrelframework.foundation.fsm.impl.AbstractStateMachine;

import java.net.MalformedURLException;
import java.util.concurrent.TimeUnit;

/**
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 */
public class GenericCollectFsmImpl extends
        AbstractStateMachine<GenericCollectFsm, GenericCollectState, GenericCollectEvent, GenericCollectContext> implements GenericCollectFsm {

    private static final Logger log = Logger.getLogger(GenericCollectFsmImpl.class);

    // Scheduler
    private final ListeningScheduledExecutorService executor;

    // Event Listener
    private final MgcpEventSubject mgcpEventSubject;

    // Media Components
    private final DtmfDetector detector;
    private final DtmfDetectorListener detectorListener;

    private final Player player;
    private final PlayerListener playerListener;

    // Execution Context
    private final GenericCollectContext context;

    //Nullable - use supportAsr to check
    private final AsrEngine asrEngine;
    //Nullable - use supportAsr to check
    private final AsrEngine.AsrEngineListener asrEngineListener;

    private final CollectStateHandlerExt extension;

    public GenericCollectFsmImpl(DtmfDetector detector, Player player, AsrEngine asrEngine,
                                 CollectStateHandlerExt extension, MgcpEventSubject mgcpEventSubject,
                                 ListeningScheduledExecutorService executor, GenericCollectContext context) {
        super();
        // Scheduler
        this.executor = executor;

        System.out.println("!!! new asrEngine = " + asrEngine);
        this.asrEngine = asrEngine;

        // Event Listener
        this.mgcpEventSubject = mgcpEventSubject;

        // Media Components
        this.detector = detector;
        this.detectorListener = new DetectorListener();

        this.player = player;
        this.playerListener = new AudioPlayerListener();

        // Execution Context
        this.context = context;

        if (extension == null) {
            throw new IllegalStateException("Signal must provide extension implementation");
        }
        this.extension = extension;

        this.asrEngineListener = supportAsr() ? new LocalAsrEngineListener() : null;
    }

    public boolean supportAsr() {
        return this.asrEngine != null;
    }

    private void playAnnouncement(String url, long delay) {
        try {
            this.player.setInitialDelay(delay);
            this.player.setURL(url);
            this.player.activate();
        } catch (MalformedURLException e) {
            log.warn("Could not play malformed segment " + url);
            context.setReturnCode(ReturnCode.BAD_AUDIO_ID.code());
            fire(GenericCollectEvent.FAIL, context);
            // TODO create transition from PROMPTING to FAILED
        } catch (ResourceUnavailableException e) {
            log.warn("Could not play unavailable segment " + url);
            context.setReturnCode(ReturnCode.BAD_AUDIO_ID.code());
            fire(GenericCollectEvent.FAIL, context);
            // TODO create transition from PROMPTING to FAILED
        }
    }

    public DtmfDetectorListener getDetectorListener() {
        return detectorListener;
    }

    public PlayerListener getPlayerListener() {
        return playerListener;
    }

    @Override
    public void enterPlayCollect(GenericCollectState from, GenericCollectState to, GenericCollectEvent event,
                                 GenericCollectContext context) {
        if (log.isTraceEnabled()) {
            log.trace("Entered PLAY_COLLECT state");
        }

    }

    @Override
    public void exitPlayCollect(GenericCollectState from, GenericCollectState to, GenericCollectEvent event,
                                GenericCollectContext context) {
        if (log.isTraceEnabled()) {
            log.trace("Exited PLAY_COLLECT state");
        }

    }

    @Override
    public void enterLoadingPlaylist(GenericCollectState from, GenericCollectState to, GenericCollectEvent event,
                                     GenericCollectContext context) {
        if (log.isTraceEnabled()) {
            log.trace("Entered LOADING PLAYLIST state");
        }

        if (event == null) {
            final Playlist prompt = context.getInitialPrompt();
            if (prompt.isEmpty()) {
                fire(GenericCollectEvent.NO_PROMPT, context);
            } else {
                fire(GenericCollectEvent.PROMPT, context);
            }
        } else {
            switch (event) {
                case RESTART:
                    final Playlist reprompt = context.getReprompt();
                    if (reprompt.isEmpty()) {
                        fire(GenericCollectEvent.NO_PROMPT, context);
                    } else {
                        fire(GenericCollectEvent.REPROMPT, context);
                    }
                    break;

                case NO_DIGITS:
                    final Playlist noDigitsReprompt = context.getNoDigitsReprompt();
                    if (noDigitsReprompt.isEmpty()) {
                        fire(GenericCollectEvent.NO_PROMPT, context);
                    } else {
                        fire(GenericCollectEvent.NO_DIGITS, context);
                    }
                    break;

                case REINPUT:
                default:
                    fire(GenericCollectEvent.NO_PROMPT, context);
                    break;
            }
        }
    }

    @Override
    public void exitLoadingPlaylist(GenericCollectState from, GenericCollectState to, GenericCollectEvent event,
                                    GenericCollectContext context) {
        if (log.isTraceEnabled()) {
            log.trace("Exited LOADING PLAYLIST state");
        }
    }

    @Override
    public void enterPrompting(GenericCollectState from, GenericCollectState to, GenericCollectEvent event, GenericCollectContext context) {
        if (log.isTraceEnabled()) {
            log.trace("Entered PROMPTING state");
        }

        final Playlist prompt = context.getInitialPrompt();
        final String track = prompt.next();
        try {
            this.player.addListener(this.playerListener);
            playAnnouncement(track, 0L);
        } catch (TooManyListenersException e) {
            log.error("Too many player listeners", e);
            context.setReturnCode(ReturnCode.UNSPECIFIED_FAILURE.code());
            fire(GenericCollectEvent.FAIL, context);
        }
    }

    @Override
    public void onPrompting(GenericCollectState from, GenericCollectState to, GenericCollectEvent event, GenericCollectContext context) {
        if (log.isTraceEnabled()) {
            log.trace("On PROMPTING state");
        }

        final Playlist prompt = context.getInitialPrompt();
        final String track = prompt.next();

        if (track.isEmpty()) {
            // No more announcements to play
            fire(GenericCollectEvent.END_PROMPT, context);
        } else {
            playAnnouncement(track, 10 * 100);
        }
    }

    @Override
    public void exitPrompting(GenericCollectState from, GenericCollectState to, GenericCollectEvent event, GenericCollectContext context) {
        if (log.isTraceEnabled()) {
            log.trace("Exited PROMPTING state");
        }

        this.player.removeListener(this.playerListener);
        this.player.deactivate();
    }

    @Override
    public void enterReprompting(GenericCollectState from, GenericCollectState to, GenericCollectEvent event,
                                 GenericCollectContext context) {
        if (log.isTraceEnabled()) {
            log.trace("Entered REPROMPTING state");
        }

        final Playlist prompt = context.getReprompt();
        final String track = prompt.next();
        try {
            this.player.addListener(this.playerListener);
            playAnnouncement(track, 0L);
        } catch (TooManyListenersException e) {
            log.error("Too many player listeners", e);
            context.setReturnCode(ReturnCode.UNSPECIFIED_FAILURE.code());
            fire(GenericCollectEvent.FAIL, context);
        }
    }

    @Override
    public void onReprompting(GenericCollectState from, GenericCollectState to, GenericCollectEvent event, GenericCollectContext context) {
        if (log.isTraceEnabled()) {
            log.trace("On REPROMPTING state");
        }

        final Playlist prompt = context.getReprompt();
        final String track = prompt.next();

        if (track.isEmpty()) {
            // No more announcements to play
            fire(GenericCollectEvent.END_PROMPT, context);
        } else {
            playAnnouncement(track, 10 * 100);
        }
    }

    @Override
    public void exitReprompting(GenericCollectState from, GenericCollectState to, GenericCollectEvent event,
                                GenericCollectContext context) {
        if (log.isTraceEnabled()) {
            log.trace("Exited REPROMPTING state");
        }

        this.player.removeListener(this.playerListener);
        this.player.deactivate();
    }

    @Override
    public void enterNoDigitsReprompting(GenericCollectState from, GenericCollectState to, GenericCollectEvent event,
                                         GenericCollectContext context) {
        if (log.isTraceEnabled()) {
            log.trace("Entered NO DIGITS REPROMPTING state");
        }

        final Playlist prompt = context.getNoDigitsReprompt();
        final String track = prompt.next();
        try {
            this.player.addListener(this.playerListener);
            playAnnouncement(track, 0L);
        } catch (TooManyListenersException e) {
            log.error("Too many player listeners", e);
            context.setReturnCode(ReturnCode.UNSPECIFIED_FAILURE.code());
            fire(GenericCollectEvent.FAIL, context);
        }
    }

    @Override
    public void onNoDigitsReprompting(GenericCollectState from, GenericCollectState to, GenericCollectEvent event,
                                      GenericCollectContext context) {
        if (log.isTraceEnabled()) {
            log.trace("On NO DIGITS REPROMPTING state");
        }

        final Playlist prompt = context.getNoDigitsReprompt();
        final String track = prompt.next();

        if (track.isEmpty()) {
            // No more announcements to play
            fire(GenericCollectEvent.END_PROMPT, context);
        } else {
            playAnnouncement(track, 10 * 100);
        }
    }

    @Override
    public void exitNoDigitsReprompting(GenericCollectState from, GenericCollectState to, GenericCollectEvent event,
                                        GenericCollectContext context) {
        if (log.isTraceEnabled()) {
            log.trace("Exited NO DIGITS REPROMPTING state");
        }

        this.player.removeListener(this.playerListener);
        this.player.deactivate();
    }

    @Override
    public void enterPrompted(GenericCollectState from, GenericCollectState to, GenericCollectEvent event, GenericCollectContext context) {
        // Check if no digit has been pressed while prompt was playing
        if (context.countCollectedDigits() == 0) {
            // Activate timer for first digit
            if (log.isTraceEnabled()) {
                log.trace("Scheduled First Digit Timer to fire in " + context.getFirstDigitTimer() + " ms");
            }
            this.executor.schedule(new DetectorTimer(context), context.getFirstDigitTimer(), TimeUnit.MILLISECONDS);
        }
    }

    @Override
    public void enterCollecting(GenericCollectState from, GenericCollectState to, GenericCollectEvent event,
                                GenericCollectContext context) {
        if (log.isTraceEnabled()) {
            log.trace("Entered COLLECTING state");
        }

        try {
            // Activate DTMF detector and bind listener
            this.detector.addListener(this.detectorListener);
            this.detector.activate();
        } catch (TooManyListenersException e) {
            log.error("Too many DTMF listeners", e);
        }

        System.out.println("!!! this.asrEngine = " + this.asrEngine);
        if (supportAsr()) {
            this.asrEngine.configure("stub", "en");
            this.asrEngine.setListener(asrEngineListener);
            this.asrEngine.activate();
        }
    }

    @Override
    public void onCollecting(GenericCollectState from, GenericCollectState to, GenericCollectEvent event, GenericCollectContext context) {
        if (log.isTraceEnabled()) {
            log.trace(
                    "On COLLECTING state [digits=" + context.getCollectedDigits() + ", attempt=" + context.getAttempt() + "]");
        }

        // Stop current prompt IF is interruptible
        if (!context.getNonInterruptibleAudio()) {
            // TODO check if child state PLAYING is currently active
            fire(GenericCollectEvent.END_PROMPT, context);
        }

        final char tone = context.getLastTone();

        if (context.getReinputKey() == tone) {
            // Force collection to cancel any scheduled timeout
            context.collectDigit(tone);
            fire(GenericCollectEvent.REINPUT, context);
        } else if (context.getRestartKey() == tone) {
            // Force collection to cancel any scheduled timeout
            context.collectDigit(tone);
            fire(GenericCollectEvent.RESTART, context);
        } else if (context.getEndInputKey() == tone) {
            fire(GenericCollectEvent.END_INPUT, context);
        } else {
            // Make sure first digit matches StartInputKey
            if (context.countCollectedDigits() == 0 && context.getStartInputKeys().indexOf(tone) == -1) {
                log.info("Dropping tone " + tone + " because it does not match any of StartInputKeys "
                        + context.getStartInputKeys());
                return;
            }

            // Append tone to list of collected digits
            context.collectDigit(tone);

            // Stop collecting if maximum number of digits was reached.
            // Only verified if no Digit Pattern was defined.
            if (!context.hasDigitPattern() && context.countCollectedDigits() == context.getMaximumDigits()) {
                fire(GenericCollectEvent.END_INPUT, context);
            } else {
                // Start interdigit timer
                if (log.isTraceEnabled()) {
                    log.trace("Scheduled Inter Digit Timer to fire in " + context.getFirstDigitTimer() + " ms");
                }
                this.executor.schedule(new DetectorTimer(context), context.getInterDigitTimer(), TimeUnit.MILLISECONDS);
            }
        }
    }

    @Override
    public void exitCollecting(GenericCollectState from, GenericCollectState to, GenericCollectEvent event, GenericCollectContext context) {
        if (log.isTraceEnabled()) {
            log.trace("Exited COLLECTING state");
        }

        this.detector.removeListener(this.detectorListener);
        this.detector.deactivate();

        if (supportAsr()) {
            this.asrEngine.setListener(null);
            this.asrEngine.deactivate();
        }
    }

    @Override
    public void enterEvaluating(GenericCollectState from, GenericCollectState to, GenericCollectEvent event,
                                GenericCollectContext context) {
        if (log.isTraceEnabled()) {
            log.trace("Entered EVALUATING state.");
        }

        final int digitCount = context.countCollectedDigits();
        if (digitCount == 0) {
            // No digits were collected
            fire(GenericCollectEvent.NO_DIGITS, context);
        } else if (context.hasDigitPattern()) {
            // Succeed if digit pattern matches. Otherwise retry
            if (context.getCollectedDigits().matches(context.getDigitPattern())) {
                fire(GenericCollectEvent.SUCCEED, context);
            } else {
                fire(GenericCollectEvent.PATTERN_MISMATCH, context);
            }
        } else if (digitCount < context.getMinimumDigits()) {
            // Minimum digits not met
            fire(GenericCollectEvent.PATTERN_MISMATCH, context);
        } else {
            // Pattern validation was successful
            fire(GenericCollectEvent.SUCCEED, context);
        }
    }

    @Override
    public void exitEvaluating(GenericCollectState from, GenericCollectState to, GenericCollectEvent event, GenericCollectContext context) {
        if (log.isTraceEnabled()) {
            log.trace("Exited EVALUATING state");
        }
    }

    @Override
    public void enterCanceled(GenericCollectState from, GenericCollectState to, GenericCollectEvent event, GenericCollectContext context) {
        if (log.isTraceEnabled()) {
            log.trace("Entered CANCELED state");
        }

        final int digitCount = context.countCollectedDigits();
        if (digitCount == 0) {
            // No digits were collected
            context.setReturnCode(ReturnCode.NO_DIGITS.code());
            fire(GenericCollectEvent.FAIL, context);
        } else if (context.hasDigitPattern()) {
            // Succeed if digit pattern matches. Otherwise retry
            if (context.getCollectedDigits().matches(context.getDigitPattern())) {
                fire(GenericCollectEvent.SUCCEED, context);
            } else {
                context.setReturnCode(ReturnCode.DIGIT_PATTERN_NOT_MATCHED.code());
                fire(GenericCollectEvent.FAIL, context);
            }
        } else if (digitCount < context.getMinimumDigits()) {
            // Minimum digits not met
            context.setReturnCode(ReturnCode.DIGIT_PATTERN_NOT_MATCHED.code());
            fire(GenericCollectEvent.FAIL, context);
        } else {
            // Pattern validation was successful
            fire(GenericCollectEvent.SUCCEED, context);
        }
    }

    @Override
    public void exitCanceled(GenericCollectState from, GenericCollectState to, GenericCollectEvent event, GenericCollectContext context) {
        if (log.isTraceEnabled()) {
            log.trace("Exited CANCELED state");
        }
    }

    @Override
    public void enterSucceeding(GenericCollectState from, GenericCollectState to, GenericCollectEvent event,
                                GenericCollectContext context) {
        if (log.isTraceEnabled()) {
            log.trace("Entered SUCCEEDING state");
        }

        final Playlist prompt = context.getSuccessAnnouncement();
        if (prompt.isEmpty()) {
            fire(GenericCollectEvent.NO_PROMPT, context);
        } else {
            fire(GenericCollectEvent.PROMPT, context);
        }
    }

    @Override
    public void exitSucceeding(GenericCollectState from, GenericCollectState to, GenericCollectEvent event, GenericCollectContext context) {
        if (log.isTraceEnabled()) {
            log.trace("Entered SUCCEEDING state");
        }
    }

    @Override
    public void enterPlayingSuccess(GenericCollectState from, GenericCollectState to, GenericCollectEvent event,
                                    GenericCollectContext context) {
        if (log.isTraceEnabled()) {
            log.trace("Entered PLAYING SUCCESS state");
        }

        final Playlist prompt = context.getSuccessAnnouncement();
        final String track = prompt.next();
        try {
            this.player.addListener(this.playerListener);
            playAnnouncement(track, 0L);
        } catch (TooManyListenersException e) {
            log.error("Too many player listeners", e);
            context.setReturnCode(ReturnCode.UNSPECIFIED_FAILURE.code());
            fire(GenericCollectEvent.FAIL, context);
        }
    }

    @Override
    public void onPlayingSuccess(GenericCollectState from, GenericCollectState to, GenericCollectEvent event,
                                 GenericCollectContext context) {
        if (log.isTraceEnabled()) {
            log.trace("On PLAYING SUCCESS state");
        }

        final Playlist prompt = context.getSuccessAnnouncement();
        final String track = prompt.next();

        if (track.isEmpty()) {
            // No more announcements to play
            fire(GenericCollectEvent.END_PROMPT, context);
        } else {
            playAnnouncement(track, 10 * 100);
        }
    }

    @Override
    public void exitPlayingSuccess(GenericCollectState from, GenericCollectState to, GenericCollectEvent event,
                                   GenericCollectContext context) {
        if (log.isTraceEnabled()) {
            log.trace("Exited PLAYING SUCCESS state");
        }

        this.player.removeListener(this.playerListener);
        this.player.deactivate();
    }

    @Override
    public void enterSucceeded(GenericCollectState from, GenericCollectState to, GenericCollectEvent event, GenericCollectContext context) {
        if (log.isTraceEnabled()) {
            log.trace("Entered SUCCEEDED state");
        }
        extension.enterSucceeded(from, to, event, context);
    }

    @Override
    public void enterFailing(GenericCollectState from, GenericCollectState to, GenericCollectEvent event, GenericCollectContext context) {
        if (log.isTraceEnabled()) {
            log.trace("Entered FAILING state");
        }

        if (context.hasMoreAttempts()) {
            context.newAttempt();
            switch (event) {
                case RESTART:
                case REINPUT:
                case NO_DIGITS:
                    fire(event, context);
                    break;

                case PATTERN_MISMATCH:
                default:
                    fire(GenericCollectEvent.RESTART, context);
                    break;
            }
        } else {
            switch (event) {
                case NO_DIGITS:
                    context.setReturnCode(ReturnCode.NO_DIGITS.code());
                    break;

                case PATTERN_MISMATCH:
                    context.setReturnCode(ReturnCode.DIGIT_PATTERN_NOT_MATCHED.code());
                    break;

                case RESTART:
                case REINPUT:
                    context.setReturnCode(ReturnCode.MAX_ATTEMPTS_EXCEEDED.code());
                    break;

                default:
                    context.setReturnCode(ReturnCode.UNSPECIFIED_FAILURE.code());
                    break;
            }

            final Playlist prompt = context.getFailureAnnouncement();
            if (prompt.isEmpty()) {
                fire(GenericCollectEvent.NO_PROMPT, context);
            } else {
                fire(GenericCollectEvent.PROMPT, context);
            }
        }
    }

    @Override
    public void exitFailing(GenericCollectState from, GenericCollectState to, GenericCollectEvent event, GenericCollectContext context) {
        if (log.isTraceEnabled()) {
            log.trace("Exited FAILING state");
        }
    }

    @Override
    public void enterPlayingFailure(GenericCollectState from, GenericCollectState to, GenericCollectEvent event,
                                    GenericCollectContext context) {
        if (log.isTraceEnabled()) {
            log.trace("Entered PLAYING FAILURE state");
        }

        final Playlist prompt = context.getFailureAnnouncement();
        final String track = prompt.next();
        try {
            this.player.addListener(this.playerListener);
            playAnnouncement(track, 0L);
        } catch (TooManyListenersException e) {
            log.error("Too many player listeners", e);
            context.setReturnCode(ReturnCode.UNSPECIFIED_FAILURE.code());
            fire(GenericCollectEvent.FAIL, context);
        }
    }

    @Override
    public void onPlayingFailure(GenericCollectState from, GenericCollectState to, GenericCollectEvent event,
                                 GenericCollectContext context) {
        if (log.isTraceEnabled()) {
            log.trace("On PLAYING FAILURE state");
        }

        final Playlist prompt = context.getFailureAnnouncement();
        final String track = prompt.next();

        if (track.isEmpty()) {
            // No more announcements to play
            fire(GenericCollectEvent.END_PROMPT, context);
        } else {
            playAnnouncement(track, 10 * 100);
        }
    }

    @Override
    public void exitPlayingFailure(GenericCollectState from, GenericCollectState to, GenericCollectEvent event,
                                   GenericCollectContext context) {
        if (log.isTraceEnabled()) {
            log.trace("Exited PLAYING FAILURE state");
        }

        this.player.removeListener(this.playerListener);
        this.player.deactivate();
    }

    @Override
    public void enterFailed(GenericCollectState from, GenericCollectState to, GenericCollectEvent event, GenericCollectContext context) {
        if (log.isTraceEnabled()) {
            log.trace("Entered FAILED state");
        }
        extension.enterFailed(from, to, event, context);
    }

    private final class LocalAsrEngineListener implements AsrEngine.AsrEngineListener {

        @Override
        public void onSpeechRecognized(String text) {
            fire(GenericCollectEvent.RECOGNIZED_TEXT, context);
        }
    }

    /**
     * Timer that defines interval the system will wait for user's input. May interrupt Collect process.
     *
     * @author Henrique Rosa (henrique.rosa@telestax.com)
     */
    private final class DetectorTimer implements Runnable {

        private final long timestamp;
        private final GenericCollectContext context;

        public DetectorTimer(GenericCollectContext context) {
            this.timestamp = System.currentTimeMillis();
            this.context = context;
        }

        @Override
        public void run() {
            if (context.getLastCollectedDigitOn() <= this.timestamp) {
                if (GenericCollectState.PLAY_COLLECT.equals(getCurrentState())) {
                    if (log.isDebugEnabled()) {
                        log.debug("Timing out collect operation! " + context.getLastCollectedDigitOn() + " <= " + this.timestamp);
                    }
                    fire(GenericCollectEvent.TIMEOUT, context);
                }
            } else {
                if (log.isTraceEnabled()) {
                    log.trace("Aborting timeout operation because a tone has been received in the meantime.");
                }
            }

        }

    }

    /**
     * Listens to DTMF events raised by the DTMF Detector.
     *
     * @author Henrique Rosa (henrique.rosa@telestax.com)
     */
    private final class DetectorListener implements DtmfDetectorListener {

        @Override
        public void process(DtmfEvent event) {
            final char tone = event.getTone().charAt(0);
            context.setLastTone(tone);
            GenericCollectFsmImpl.this.fire(GenericCollectEvent.DTMF_TONE, GenericCollectFsmImpl.this.context);
        }

    }

    /**
     * Listen to Play events raised by the Player.
     *
     * @author Henrique Rosa (henrique.rosa@telestax.com)
     */
    private final class AudioPlayerListener implements PlayerListener {

        @Override
        public void process(PlayerEvent event) {
            switch (event.getID()) {
                case PlayerEvent.STOP:
                    GenericCollectFsmImpl.this.fire(GenericCollectEvent.NEXT_TRACK, GenericCollectFsmImpl.this.context);
                    break;

                case PlayerEvent.FAILED:
                    // TODO handle player failure
                    break;

                default:
                    break;
            }
        }
    }

    public interface CollectStateHandlerExt {
        void enterSucceeded(GenericCollectState from, GenericCollectState to, GenericCollectEvent event, GenericCollectContext context);

        void enterFailed(GenericCollectState from, GenericCollectState to, GenericCollectEvent event, GenericCollectContext context);
    }

}