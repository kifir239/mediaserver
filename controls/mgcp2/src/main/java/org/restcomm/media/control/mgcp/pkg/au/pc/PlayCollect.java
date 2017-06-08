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

package org.restcomm.media.control.mgcp.pkg.au.pc;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.ListeningScheduledExecutorService;
import org.restcomm.media.control.mgcp.command.param.NotifiedEntity;
import org.restcomm.media.control.mgcp.pkg.AbstractMgcpSignal;
import org.restcomm.media.control.mgcp.pkg.MgcpEventSubject;
import org.restcomm.media.control.mgcp.pkg.SignalType;
import org.restcomm.media.control.mgcp.pkg.au.*;
import org.restcomm.media.control.mgcp.pkg.generic.collect.*;
import org.restcomm.media.spi.dtmf.DtmfDetector;
import org.restcomm.media.spi.player.Player;

import java.util.Map;

/**
 * Plays a prompt and collects DTMF digits entered by a user.
 * 
 * <p>
 * If no digits are entered or an invalid digit pattern is entered, the user may be reprompted and given another chance to enter
 * a correct pattern of digits. The following digits are supported: 0-9, *, #, A, B, C, D.
 * </p>
 * 
 * <p>
 * By default PlayCollect does not play an initial prompt, makes only one attempt to collect digits, and therefore functions as
 * a simple Collect operation.<br>
 * Various special purpose keys, key sequences, and key sets can be defined for use during the PlayCollect operation.
 * </p>
 * 
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class PlayCollect extends AbstractMgcpSignal {

    private static final String SYMBOL = "pc";

    // Finite State Machine
    private final GenericCollectFsm fsm;

    private final GenericCollectContext context;

    public PlayCollect(Player player, DtmfDetector detector, int requestId, NotifiedEntity notifiedEntity, Map<String, String> parameters, ListeningScheduledExecutorService executor) {
        super(AudioPackage.PACKAGE_NAME, SYMBOL, SignalType.TIME_OUT, requestId, notifiedEntity, parameters);

        // Execution Context
        this.context = new GenericCollectContext(new ParameterParser().parse());

        // Build FSM
        this.fsm = GenericCollectFsmBuilder.INSTANCE.build(detector, player, null, handler, this, executor, context);
    }

    public PlayCollect(Player player, DtmfDetector detector, int requestId, Map<String, String> parameters, ListeningScheduledExecutorService executor) {
        this(player, detector, requestId, null, parameters, executor);
    }

    @Override
    protected boolean isParameterSupported(String name) {
        // Check if parameter is valid
        SignalParameters parameter = SignalParameters.fromSymbol(name);
        if (parameter == null) {
            return false;
        }

        // Check if parameter is supported
        switch (parameter) {
            case INITIAL_PROMPT:
            case REPROMPT:
            case NO_DIGITS_REPROMPT:
            case FAILURE_ANNOUNCEMENT:
            case SUCCESS_ANNOUNCEMENT:
            case NON_INTERRUPTIBLE_PLAY:
            case SPEED:
            case VOLUME:
            case CLEAR_DIGIT_BUFFER:
            case MAXIMUM_NUM_DIGITS:
            case MINIMUM_NUM_DIGITS:
            case DIGIT_PATTERN:
            case FIRST_DIGIT_TIMER:
            case INTER_DIGIT_TIMER:
            case EXTRA_DIGIT_TIMER:
            case RESTART_KEY:
            case REINPUT_KEY:
            case RETURN_KEY:
            case POSITION_KEY:
            case STOP_KEY:
            case START_INPUT_KEY:
            case END_INPUT_KEY:
            case INCLUDE_END_INPUT_KEY:
            case NUMBER_OF_ATTEMPTS:
                return true;

            default:
                return false;
        }
    }

    @Override
    public void execute() {
        if (!this.fsm.isStarted()) {
            this.fsm.start(this.context);
        }
    }

    @Override
    public void cancel() {
        if (this.fsm.isStarted()) {
            fsm.fire(GenericCollectEvent.CANCEL, this.context);
        }
    }

    private GenericCollectFsmImpl.CollectStateHandlerExt handler = new GenericCollectFsmImpl.CollectStateHandlerExt() {

        private MgcpEventSubject mgcpEventSubject = PlayCollect.this;

        @Override
        public void enterSucceeded(GenericCollectState from, GenericCollectState to, GenericCollectEvent event, GenericCollectContext context) {
            final int attempt = context.getAttempt();
            String collectedDigits = context.getCollectedDigits();
            if (context.getIncludeEndInputKey()) {
                collectedDigits += context.getEndInputKey();
            }

            final OperationComplete operationComplete = new OperationComplete(PlayCollect.SYMBOL, ReturnCode.SUCCESS.code());
            operationComplete.setParameter("na", String.valueOf(attempt));
            operationComplete.setParameter("dc", collectedDigits);
            mgcpEventSubject.notify(mgcpEventSubject, operationComplete);
        }

        @Override
        public void enterFailed(GenericCollectState from, GenericCollectState to, GenericCollectEvent event, GenericCollectContext context) {
            final OperationFailed operationFailed = new OperationFailed(PlayCollect.SYMBOL, context.getReturnCode());
            mgcpEventSubject.notify(mgcpEventSubject, operationFailed);
        }
    };

    private class ParameterParser {

        GenericCollectContext.Parameters parse(){
            return new GenericCollectContext.Parameters(
                    getInitialPromptSegments(),
                    getRepromptSegments(),
                    getNoDigitsRepromptSegments(),
                    getFailureAnnouncementSegments(),
                    getSuccessAnnouncementSegments(),
                    getNonInterruptibleAudio(),
                    getClearDigitBuffer(),
                    getMinimumDigits(),
                    getMaximumDigits(),
                    getDigitPattern(),
                    hasDigitPattern(),
                    getFirstDigitTimer(),
                    getInterDigitTimer(),
                    getExtraDigitTimer(),
                    getRestartKey(),
                    getReinputKey(),
                    getReturnKey(),
                    getPositionKey(),
                    getStopKey(),
                    getStartInputKeys(),
                    getEndInputKey(),
                    getIncludeEndInputKey(),
                    getNumberOfAttempts()
            );
        }

        /**
         * The initial announcement prompting the user to either enter DTMF digits or to speak.
         * <p>
         * Consists of one or more audio segments.<br>
         * If not specified (the default), the event immediately begins digit collection or recording.
         * </p>
         *
         * @return The array of audio prompts. Array will be empty if none is specified.
         */
        private String[] getInitialPromptSegments() {
            String value = Optional.fromNullable(getParameter(SignalParameters.INITIAL_PROMPT.symbol())).or("");
            return value.isEmpty() ? new String[0] : value.split(",");
        }

        /**
         * Played after the user has made an error such as entering an invalid digit pattern or not speaking.
         * <p>
         * Consists of one or more audio segments. <b>Defaults to the Initial Prompt.</b>
         * </p>
         *
         * @return The array of audio prompts. Array will be empty if none is specified.
         */
        private String[] getRepromptSegments() {
            String segments = Optional.fromNullable(getParameter(SignalParameters.REPROMPT.symbol())).or("");
            return segments.isEmpty() ? getInitialPromptSegments() : segments.split(",");
        }

        /**
         * Played after the user has failed to enter a valid digit pattern during a PlayCollect event.
         * <p>
         * Consists of one or more audio segments. <b>Defaults to the Reprompt.</b>
         * </p>
         *
         * @return The array of audio prompts. Array will be empty if none is specified.
         */
        private String[] getNoDigitsRepromptSegments() {
            String segments = Optional.fromNullable(getParameter(SignalParameters.NO_DIGITS_REPROMPT.symbol())).or("");
            return segments.isEmpty() ? getRepromptSegments() : segments.split(",");
        }

        /**
         * Played when all data entry attempts have failed.
         * <p>
         * Consists of one or more audio segments. No default.
         * </p>
         *
         * @return The array of audio prompts. Array will be empty if none is specified.
         */
        private String[] getFailureAnnouncementSegments() {
            String value = Optional.fromNullable(getParameter(SignalParameters.FAILURE_ANNOUNCEMENT.symbol())).or("");
            return value.isEmpty() ? new String[0] : value.split(",");
        }

        /**
         * Played when all data entry attempts have succeeded.
         * <p>
         * Consists of one or more audio segments. No default.
         * </p>
         *
         * @return The array of audio prompts. Array will be empty if none is specified.
         */
        private String[] getSuccessAnnouncementSegments() {
            String value = Optional.fromNullable(getParameter(SignalParameters.SUCCESS_ANNOUNCEMENT.symbol())).or("");
            return value.isEmpty() ? new String[0] : value.split(",");
        }

        /**
         * If set to true, initial prompt is not interruptible by either voice or digits.
         * <p>
         * <b>Defaults to false.</b> Valid values are the text strings "true" and "false".
         * </p>
         *
         * @return
         */
        public boolean getNonInterruptibleAudio() {
            String value = Optional.fromNullable(getParameter(SignalParameters.NON_INTERRUPTIBLE_PLAY.symbol())).or("false");
            return Boolean.parseBoolean(value);
        }

        /**
         * If set to true, clears the digit buffer before playing the initial prompt.
         * <p>
         * <b>Defaults to false.</b> Valid values are the text strings "true" and "false".
         * </p>
         *
         * @return
         */
        public boolean getClearDigitBuffer() {
            String value = Optional.fromNullable(getParameter(SignalParameters.CLEAR_DIGIT_BUFFER.symbol())).or("false");
            return Boolean.parseBoolean(value);
        }

        /**
         * The minimum number of digits to collect.
         * <p>
         * <b>Defaults to one.</b> This parameter should not be specified if the Digit Pattern parameter is present.
         * </p>
         *
         * @return
         */
        public int getMinimumDigits() {
            String value = Optional.fromNullable(getParameter(SignalParameters.MINIMUM_NUM_DIGITS.symbol())).or("1");
            return Integer.parseInt(value);
        }

        /**
         * The maximum number of digits to collect.
         * <p>
         * <b>Defaults to one.</b> This parameter should not be specified if the Digit Pattern parameter is present.
         * </p>
         *
         * @return
         */
        public int getMaximumDigits() {
            String value = Optional.fromNullable(getParameter(SignalParameters.MAXIMUM_NUM_DIGITS.symbol())).or("1");
            return Integer.parseInt(value);
        }

        /**
         * A legal digit map as described in <a href="https://tools.ietf.org/html/rfc2885#section-7.1.14">section 7.1.14</a> of the
         * MEGACO protocol using the DTMF mappings associated with the Megaco DTMF Detection Package described in the Megaco
         * protocol document.
         * <p>
         * <b>This parameter should not be specified if one or both of the Minimum # Of Digits parameter and the Maximum Number Of
         * Digits parameter is present.</b>
         * </p>
         *
         * @return The digit pattern or an empty String if not specified.
         */
        public String getDigitPattern() {
            String pattern = Optional.fromNullable(getParameter(SignalParameters.DIGIT_PATTERN.symbol())).or("");
            if (!pattern.isEmpty()) {
                // Replace pattern to comply with MEGACO digitMap
                pattern = pattern.replace(".", "*").replace("x", "\\d");
            }
            return pattern;
        }

        public boolean hasDigitPattern() {
            return !Optional.fromNullable(getParameter(SignalParameters.DIGIT_PATTERN.symbol())).or("").isEmpty();
        }

        /**
         * The amount of time allowed for the user to enter the first digit.
         * <p>
         * Specified in units of 100 milliseconds. <b>Defaults to 50 (5 seconds).</b>
         * </p>
         *
         * @return
         */
        public int getFirstDigitTimer() {
            String value = Optional.fromNullable(getParameter(SignalParameters.FIRST_DIGIT_TIMER.symbol())).or("50");
            return Integer.parseInt(value) * 100;
        }

        /**
         * The amount of time allowed for the user to enter each subsequent digit.
         * <p>
         * Specified units of 100 milliseconds seconds. <b>Defaults to 30 (3 seconds).</b>
         * </p>
         *
         * @return
         */
        public int getInterDigitTimer() {
            String value = Optional.fromNullable(getParameter(SignalParameters.INTER_DIGIT_TIMER.symbol())).or("30");
            return Integer.parseInt(value) * 100;
        }

        /**
         * The amount of time to wait for a user to enter a final digit once the maximum expected amount of digits have been
         * entered.
         * <p>
         * Typically this timer is used to wait for a terminating key in applications where a specific key has been defined to
         * terminate input.
         * </p>
         * <p>
         * Specified in units of 100 milliseconds. </b>If not specified, this timer is not activated.</b>
         * </p>
         *
         * @return
         */
        public int getExtraDigitTimer() {
            String value = Optional.fromNullable(getParameter(SignalParameters.EXTRA_DIGIT_TIMER.symbol())).or("");
            return Integer.parseInt(value) * 100;
        }

        /**
         * Defines a key sequence consisting of a command key optionally followed by zero or more keys. This key sequence has the
         * following action: discard any digits collected or recording in progress, replay the prompt, and resume digit collection
         * or recording.
         * <p>
         * <b>No default.</b> An application that defines more than one command key sequence, will typically use the same command
         * key for all command key sequences.
         * <p>
         * If more than one command key sequence is defined, then all key sequences must consist of a command key plus at least one
         * other key.
         * </p>
         *
         * @return
         */
        public char getRestartKey() {
            String value = Optional.fromNullable(getParameter(SignalParameters.RESTART_KEY.symbol())).or("");
            return value.isEmpty() ? ' ' : value.charAt(0);
        }

        /**
         * Defines a key sequence consisting of a command key optionally followed by zero or more keys. This key sequence has the
         * following action: discard any digits collected or recordings in progress and resume digit collection or recording.
         * <p>
         * <b>No default.</b>
         * </p>
         * An application that defines more than one command key sequence, will typically use the same command key for all command
         * key sequences.
         * </p>
         * <p>
         * If more than one command key sequence is defined, then all key sequences must consist of a command key plus at least one
         * other key.
         * </p>
         *
         * @return
         */
        public char getReinputKey() {
            String value = Optional.fromNullable(getParameter(SignalParameters.REINPUT_KEY.symbol())).or("");
            return value.isEmpty() ? ' ' : value.charAt(0);
        }

        /**
         * Defines a key sequence consisting of a command key optionally followed by zero or more keys. This key sequence has the
         * following action: terminate the current event and any queued event and return the terminating key sequence to the call
         * processing agent.
         * <p>
         * <b> No default.</b> An application that defines more than one command key sequence, will typically use the same command
         * key for all command key sequences.
         * <p>
         * If more than one command key sequence is defined, then all key sequences must consist of a command key plus at least one
         * other key.
         * </p>
         *
         * @return
         */
        public char getReturnKey() {
            String value = Optional.fromNullable(getParameter(SignalParameters.RETURN_KEY.symbol())).or("");
            return value.isEmpty() ? ' ' : value.charAt(0);
        }

        /**
         * Defines a key with the following action. Stop playing the current announcement and resume playing at the beginning of the
         * first, last, previous, next, or the current segment of the announcement.
         * <p>
         * <b>No default. The actions for the position key are fst, lst, prv, nxt, and cur.</b>
         * </p>
         *
         * @return
         */
        public char getPositionKey() {
            String value = Optional.fromNullable(getParameter(SignalParameters.POSITION_KEY.symbol())).or("");
            return value.isEmpty() ? ' ' : value.charAt(0);
        }

        /**
         * Defines a key with the following action. Terminate playback of the announcement.
         * <p>
         * <b>No default.</b>
         * </p>
         *
         * @return
         */
        public char getStopKey() {
            String value = Optional.fromNullable(getParameter(SignalParameters.STOP_KEY.symbol())).or("");
            return value.isEmpty() ? ' ' : value.charAt(0);
        }

        /**
         * Defines a set of keys that are acceptable as the first digit collected. This set of keys can be specified to interrupt a
         * playing announcement or to not interrupt a playing announcement.
         * <p>
         * <b>The default key set is 0-9. The default behavior is to interrupt a playing announcement when a Start Input Key is
         * pressed.</b>
         * </p>
         * <p>
         * This behavior can be overidden for the initial prompt only by using the ni (Non-Interruptible Play) parameter.
         * Specification is a list of keys with no separators, e.g. 123456789#.
         * </p>
         *
         * @return
         */
        public String getStartInputKeys() {
            return Optional.fromNullable(getParameter(SignalParameters.START_INPUT_KEY.symbol())).or("0123456789");
        }

        /**
         * Specifies a key that signals the end of digit collection or voice recording.
         * <p>
         * <b>The default end input key is the # key.</b> To specify that no End Input Key be used the parameter is set to the
         * string "null".
         * <p>
         * <b>The default behavior not to return the End Input Key in the digits returned to the call agent.</b> This behavior can
         * be overridden by the Include End Input Key (eik) parameter.
         * </p>
         *
         * @return
         */
        public char getEndInputKey() {
            String value = Optional.fromNullable(getParameter(SignalParameters.END_INPUT_KEY.symbol())).or("");
            return value.isEmpty() ? '#' : value.charAt(0);
        }

        /**
         * By default the End Input Key is not included in the collected digits returned to the call agent. If this parameter is set
         * to "true" then the End Input Key will be returned with the collected digits returned to the call agent.
         * <p>
         * <b>Default is "false".</b>
         * </p>
         *
         * @return
         */
        public boolean getIncludeEndInputKey() {
            String value = Optional.fromNullable(getParameter(SignalParameters.INCLUDE_END_INPUT_KEY.symbol())).or("false");
            return Boolean.parseBoolean(value);
        }

        /**
         * The number of attempts the user needed to enter a valid digit pattern or to make a recording.
         * <p>
         * <b>Defaults to 1.</b> Also used as a return parameter to indicate the number of attempts the user made.
         * </p>
         *
         * @return
         */
        public int getNumberOfAttempts() {
            String value = Optional.fromNullable(getParameter(SignalParameters.NUMBER_OF_ATTEMPTS.symbol())).or("1");
            return Integer.parseInt(value);
        }
    }
}
