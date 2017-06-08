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

import java.util.Map;

import org.restcomm.media.control.mgcp.pkg.au.Playlist;
import org.restcomm.media.control.mgcp.pkg.au.SignalParameters;
import org.restcomm.media.spi.dtmf.DtmfDetector;
import org.restcomm.media.spi.dtmf.DtmfDetectorListener;

import com.google.common.base.Optional;

/**
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class GenericCollectContext {

    // Playlists
    private final Playlist initialPrompt;
    private final Playlist reprompt;
    private final Playlist noDigitsReprompt;
    private final Playlist failureAnnouncement;
    private final Playlist successAnnouncement;

    // Runtime data
    private final StringBuilder collectedDigits;
    private long lastCollectedDigitOn;
    private char lastTone;
    private int attempt;
    private int returnCode;

    private final Parameters params;

    public GenericCollectContext(Parameters params) {
        // Signal Options
        this.params = params;

        // Playlists
        this.initialPrompt = new Playlist(getInitialPromptSegments(), 1);
        this.reprompt = new Playlist(getRepromptSegments(), 1);
        this.noDigitsReprompt = new Playlist(getNoDigitsRepromptSegments(), 1);
        this.failureAnnouncement = new Playlist(getFailureAnnouncementSegments(), 1);
        this.successAnnouncement = new Playlist(getSuccessAnnouncementSegments(), 1);

        // Runtime Data
        this.collectedDigits = new StringBuilder("");
        this.lastCollectedDigitOn = 0L;
        this.lastTone = ' ';
        this.returnCode = 0;
        this.attempt = 1;
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
        return this.params.initialPromptParam;
    }

    public Playlist getInitialPrompt() {
        return initialPrompt;
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
        return this.params.repromptSegments;
    }

    public Playlist getReprompt() {
        return reprompt;
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
        return this.params.noDigitsRepromptSegments;
    }

    public Playlist getNoDigitsReprompt() {
        return noDigitsReprompt;
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
        return this.params.failureAnnSegments;
    }

    public Playlist getFailureAnnouncement() {
        return failureAnnouncement;
    }

    public boolean hasFailureAnnouncement() {
        return !this.failureAnnouncement.isEmpty();
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
        return this.params.seccessAnnSegments;
    }

    public Playlist getSuccessAnnouncement() {
        return successAnnouncement;
    }

    public boolean hasSuccessAnnouncement() {
        return !this.successAnnouncement.isEmpty();
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
        return this.params.nonInterruptibleAudio;
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
        return this.params.clearDigitBuffer;
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
        return this.params.minimumDigits;
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
        return this.params.maximumDigits;
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
        return this.params.digitsPattern;
    }

    public boolean hasDigitPattern() {
        return this.params.hasDigitsPattern;
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
        return this.params.firstDigitTimer;
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
        return this.params.interDigitTimer;
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
        return this.params.extraDigitTimer;
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
        return this.params.restartKey;
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
        return this.params.reinputKey;
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
        return this.params.returnKey;
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
        return this.params.getPositionKey;
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
        return this.params.stopKey;
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
        return this.params.startInputKeys;
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
        return this.params.endInputKey;
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
        return this.params.hasEndInputKey;
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
        return this.params.numOfAttemts;
    }

    /*
     * Runtime Data
     */
    public void collectDigit(char digit) {
        this.collectedDigits.append(digit);
        this.lastCollectedDigitOn = System.currentTimeMillis();
    }

    public void clearCollectedDigits() {
        this.collectedDigits.setLength(0);
    }

    public String getCollectedDigits() {
        return collectedDigits.toString();
    }

    public int countCollectedDigits() {
        return collectedDigits.length();
    }

    public long getLastCollectedDigitOn() {
        return lastCollectedDigitOn;
    }

    public char getLastTone() {
        return lastTone;
    }

    public void setLastTone(char lastTone) {
        this.lastTone = lastTone;
    }

    public int getAttempt() {
        return attempt;
    }

    public boolean hasMoreAttempts() {
        return this.attempt < getNumberOfAttempts();
    }

    public int getReturnCode() {
        return returnCode;
    }

    protected void setReturnCode(int returnCode) {
        this.returnCode = returnCode;
    }

    /**
     * Resets the collected digits and increments the attempts counter.
     */
    protected void newAttempt() {
        this.attempt++;
        this.collectedDigits.setLength(0);
        this.lastTone = ' ';
        this.initialPrompt.rewind();
        this.reprompt.rewind();
        this.noDigitsReprompt.rewind();
        this.successAnnouncement.rewind();
        this.failureAnnouncement.rewind();
    }

    public static class Parameters {
        public final String[] initialPromptParam;
        public final String[] repromptSegments;
        public final String[] noDigitsRepromptSegments;
        public final String[] failureAnnSegments;
        public final String[] seccessAnnSegments;
        public boolean nonInterruptibleAudio;
        public boolean clearDigitBuffer;
        public int minimumDigits;
        public int maximumDigits;
        public String digitsPattern;
        public boolean hasDigitsPattern;
        public int firstDigitTimer;
        public int interDigitTimer;
        public int extraDigitTimer;
        public char restartKey;
        public char reinputKey;
        public char returnKey;
        public char getPositionKey;
        public char stopKey;
        public String startInputKeys;
        public char endInputKey;
        public boolean hasEndInputKey;
        public int numOfAttemts;

        public Parameters(String[] initialPromptParam,
                          String[] repromptSegments,
                          String[] noDigitsRepromptSegments,
                          String[] failureAnnSegments,
                          String[] seccessAnnSegments,
                          boolean nonInterruptibleAudio,
                          boolean clearDigitBuffer,
                          int minimumDigits,
                          int maximumDigits,
                          String digitsPattern,
                          boolean hasDigitsPattern,
                          int firstDigitTimer,
                          int interDigitTimer,
                          int extraDigitTimer,
                          char restartKey,
                          char reinputKey,
                          char returnKey,
                          char getPositionKey,
                          char stopKey,
                          String startInputKeys,
                          char endInputKey,
                          boolean hasEndInputKey,
                          int numOfAttemts) { this.initialPromptParam = initialPromptParam;
            this.repromptSegments = repromptSegments;
            this.noDigitsRepromptSegments = noDigitsRepromptSegments;
            this.failureAnnSegments = failureAnnSegments;
            this.seccessAnnSegments = seccessAnnSegments;
            this.nonInterruptibleAudio = nonInterruptibleAudio;
            this.clearDigitBuffer = clearDigitBuffer;
            this.minimumDigits = minimumDigits;
            this.maximumDigits = maximumDigits;
            this.digitsPattern = digitsPattern;
            this.hasDigitsPattern = hasDigitsPattern;
            this.firstDigitTimer = firstDigitTimer;
            this.interDigitTimer = interDigitTimer;
            this.extraDigitTimer = extraDigitTimer;
            this.restartKey = restartKey;
            this.reinputKey = reinputKey;
            this.returnKey = returnKey;
            this.getPositionKey = getPositionKey;
            this.stopKey = stopKey;
            this.startInputKeys = startInputKeys;
            this.endInputKey = endInputKey;
            this.hasEndInputKey = hasEndInputKey;
            this.numOfAttemts = numOfAttemts;
        }
    }
}
