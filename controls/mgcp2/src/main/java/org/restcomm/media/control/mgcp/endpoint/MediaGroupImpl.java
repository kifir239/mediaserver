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

package org.restcomm.media.control.mgcp.endpoint;

import javax.sound.sampled.spi.AudioFileReader;

import org.restcomm.media.ComponentType;
import org.restcomm.media.component.audio.AudioComponent;
import org.restcomm.media.component.oob.OOBComponent;
import org.restcomm.media.resource.asr.AsrEngine;
import org.restcomm.media.resource.asr.AsrEngineImpl;
import org.restcomm.media.resource.asr.AsrEngineProvider;
import org.restcomm.media.resource.dtmf.DetectorImpl;
import org.restcomm.media.resource.player.audio.AudioPlayerImpl;
import org.restcomm.media.resource.recorder.audio.AudioRecorderImpl;
import org.restcomm.media.spi.dtmf.DtmfDetector;
import org.restcomm.media.spi.dtmf.DtmfDetectorProvider;
import org.restcomm.media.spi.dtmf.DtmfGenerator;
import org.restcomm.media.spi.player.Player;
import org.restcomm.media.spi.player.PlayerProvider;
import org.restcomm.media.spi.recorder.Recorder;
import org.restcomm.media.spi.recorder.RecorderProvider;

/**
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class MediaGroupImpl implements MediaGroup {

    // Media Components
    private final AudioComponent audioComponent;
    private final OOBComponent oobComponent;

    private final Player player;
    private final Recorder recorder;
    private final DtmfDetector detector;
    private final DtmfGenerator generator;
    private final AsrEngine asrEngine;

    // TODO Add list of enum that declare which media components are created in the media group
    public MediaGroupImpl(AudioComponent audioComponent, OOBComponent oobComponent, PlayerProvider players,
            RecorderProvider recorders, DtmfDetectorProvider detectors, AsrEngineProvider asrEngines) {
        // Media Components
        this.audioComponent = audioComponent;
        this.oobComponent = oobComponent;

        this.player = initializePlayer(players);
        this.recorder = initializeRecorder(recorders);
        this.detector = initializeDetector(detectors);
        this.asrEngine = initializeAsr(asrEngines);
        this.generator = null;
    }

    private Player initializePlayer(PlayerProvider players) {
        // TODO try getting rid of AudioPlayerImpl cast
        AudioPlayerImpl player = (AudioPlayerImpl) players.provide();
        this.audioComponent.addInput(player.getAudioInput());
        // readComponents++;
        audioComponent.updateMode(true, false);
        // updateEndpoint(1,0);
        return player;
    }

    private DtmfDetector initializeDetector(DtmfDetectorProvider detectors) {
        // TODO try getting rid of DetectorImpl cast
        DetectorImpl detector = (DetectorImpl) detectors.provide();
        this.audioComponent.addOutput(detector.getAudioOutput());
        this.oobComponent.addOutput(detector.getOOBOutput());
        // writeComponents++
        // writeDtmfComponents++
        // audioComponent.updateMode(readComponents!=0,true);
        // oobComponent.updateMode(readDtmfComponents!=0,true);
        this.audioComponent.updateMode(true, true);
        this.oobComponent.updateMode(true, true);
        // updateEndpoint(0,1)
        return detector;
    }

    private Recorder initializeRecorder(RecorderProvider recorders) {
        // TODO try getting rid of AudioRecorderImpl cast
        AudioRecorderImpl recorder = (AudioRecorderImpl) recorders.provide();
        audioComponent.addOutput(recorder.getAudioOutput());
        oobComponent.addOutput(recorder.getOOBOutput());
        // writeComponents++;
        // writeDtmfComponents++;
        // audioComponent.updateMode(readComponents!=0,true);
        // oobComponent.updateMode(readDtmfComponents!=0,true);
        audioComponent.updateMode(false, true);
        oobComponent.updateMode(false, true);
        // updateEndpoint(0,1);
        return recorder;
    }

    private AsrEngine initializeAsr(AsrEngineProvider provider) {
        // TODO try getting rid of AudioRecorderImpl cast
        System.out.println("!! initializeAsr");
        AsrEngineImpl engine = (AsrEngineImpl) provider.provide();
        audioComponent.addOutput(engine.getAudioOutput());
        //oobComponent.addOutput(engine.getAudioOutput());
        /*audioComponent.updateMode(false, true);
        oobComponent.updateMode(false, true);*/
        // updateEndpoint(0,1);
        return engine;
    }

    @Override
    public Player getPlayer() {
        return this.player;
    }

    @Override
    public Recorder getRecorder() {
        return this.recorder;
    }

    @Override
    public DtmfDetector getDetector() {
        return this.detector;
    }

    @Override
    public DtmfGenerator getGenerator() {
        return this.generator;
    }

    public AudioComponent getAudioComponent() {
        return audioComponent;
    }

    public OOBComponent getOobComponent() {
        return oobComponent;
    }

    public AsrEngine getAsrEngine() {
        return asrEngine;
    }
}
