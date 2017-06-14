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

import org.restcomm.media.control.mgcp.pkg.MgcpEventSubject;
import org.restcomm.media.resource.asr.AsrEngine;
import org.restcomm.media.spi.dtmf.DtmfDetector;
import org.restcomm.media.spi.dtmf.DtmfDetectorListener;
import org.restcomm.media.spi.player.Player;
import org.restcomm.media.spi.player.PlayerListener;
import org.squirrelframework.foundation.fsm.HistoryType;
import org.squirrelframework.foundation.fsm.StateMachineBuilder;
import org.squirrelframework.foundation.fsm.StateMachineBuilderFactory;
import org.squirrelframework.foundation.fsm.StateMachineConfiguration;
import org.squirrelframework.foundation.fsm.TransitionPriority;

import com.google.common.util.concurrent.ListeningScheduledExecutorService;

/**
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class GenericCollectFsmBuilder {

    public static final GenericCollectFsmBuilder INSTANCE = new GenericCollectFsmBuilder();

    private final StateMachineBuilder<GenericCollectFsm, GenericCollectState, GenericCollectEvent, GenericCollectContext> builder;

    private GenericCollectFsmBuilder() {
        // Finite State Machine
        this.builder = StateMachineBuilderFactory
                .<GenericCollectFsm, GenericCollectState, GenericCollectEvent, GenericCollectContext> create(GenericCollectFsmImpl.class,
                        GenericCollectState.class, GenericCollectEvent.class, GenericCollectContext.class,
                        DtmfDetector.class, Player.class, AsrEngine.class,
                        GenericCollectFsmImpl.CollectStateHandlerExt.class, MgcpEventSubject.class,
                        ListeningScheduledExecutorService.class, GenericCollectContext.class);

        this.builder.defineFinishEvent(GenericCollectEvent.EVALUATE);

        this.builder.onEntry(GenericCollectState.PLAY_COLLECT).callMethod("enterPlayCollect");
        this.builder.defineParallelStatesOn(GenericCollectState.PLAY_COLLECT, GenericCollectState.PLAY, GenericCollectState.COLLECT);
        this.builder.defineSequentialStatesOn(GenericCollectState.PLAY, HistoryType.NONE, GenericCollectState.LOADING_PLAYLIST, GenericCollectState.PROMPTING, GenericCollectState.REPROMPTING, GenericCollectState.NO_DIGITS_REPROMPTING, GenericCollectState.PROMPTED);
        this.builder.defineSequentialStatesOn(GenericCollectState.COLLECT, GenericCollectState.COLLECTING, GenericCollectState.COLLECTED);
        this.builder.transition().from(GenericCollectState.PLAY_COLLECT).to(GenericCollectState.EVALUATING).on(GenericCollectEvent.EVALUATE);
        this.builder.transition().from(GenericCollectState.PLAY_COLLECT).to(GenericCollectState.EVALUATING).on(GenericCollectEvent.TIMEOUT);
        this.builder.transition().from(GenericCollectState.PLAY_COLLECT).to(GenericCollectState.FAILING).on(GenericCollectEvent.RESTART);
        this.builder.transition().from(GenericCollectState.PLAY_COLLECT).to(GenericCollectState.FAILING).on(GenericCollectEvent.REINPUT);
        this.builder.transition(TransitionPriority.HIGHEST).from(GenericCollectState.PLAY_COLLECT).to(GenericCollectState.CANCELED).on(GenericCollectEvent.CANCEL);
        this.builder.onExit(GenericCollectState.PLAY_COLLECT).callMethod("exitPlayCollect");

        this.builder.onEntry(GenericCollectState.LOADING_PLAYLIST).callMethod("enterLoadingPlaylist");
        this.builder.transition().from(GenericCollectState.LOADING_PLAYLIST).to(GenericCollectState.PROMPTING).on(GenericCollectEvent.PROMPT);
        this.builder.transition().from(GenericCollectState.LOADING_PLAYLIST).to(GenericCollectState.REPROMPTING).on(GenericCollectEvent.REPROMPT);
        this.builder.transition().from(GenericCollectState.LOADING_PLAYLIST).to(GenericCollectState.NO_DIGITS_REPROMPTING).on(GenericCollectEvent.NO_DIGITS);
        this.builder.transition().from(GenericCollectState.LOADING_PLAYLIST).toFinal(GenericCollectState.PROMPTED).on(GenericCollectEvent.NO_PROMPT);
        this.builder.onExit(GenericCollectState.LOADING_PLAYLIST).callMethod("exitLoadingPlaylist");
        
        this.builder.onEntry(GenericCollectState.PROMPTING).callMethod("enterPrompting");
        this.builder.internalTransition().within(GenericCollectState.PROMPTING).on(GenericCollectEvent.NEXT_TRACK).callMethod("onPrompting");
        this.builder.transition().from(GenericCollectState.PROMPTING).toFinal(GenericCollectState.PROMPTED).on(GenericCollectEvent.END_PROMPT);
        this.builder.transition().from(GenericCollectState.PROMPTING).toFinal(GenericCollectState.PROMPTED).on(GenericCollectEvent.END_INPUT);
        this.builder.onExit(GenericCollectState.PROMPTING).callMethod("exitPrompting");
        
        this.builder.onEntry(GenericCollectState.REPROMPTING).callMethod("enterReprompting");
        this.builder.internalTransition().within(GenericCollectState.REPROMPTING).on(GenericCollectEvent.NEXT_TRACK).callMethod("onReprompting");
        this.builder.transition().from(GenericCollectState.REPROMPTING).toFinal(GenericCollectState.PROMPTED).on(GenericCollectEvent.END_PROMPT);
        this.builder.transition().from(GenericCollectState.REPROMPTING).toFinal(GenericCollectState.PROMPTED).on(GenericCollectEvent.END_INPUT);
        this.builder.onExit(GenericCollectState.REPROMPTING).callMethod("exitReprompting");
        
        this.builder.onEntry(GenericCollectState.NO_DIGITS_REPROMPTING).callMethod("enterNoDigitsReprompting");
        this.builder.internalTransition().within(GenericCollectState.NO_DIGITS_REPROMPTING).on(GenericCollectEvent.NEXT_TRACK).callMethod("onNoDigitsReprompting");
        this.builder.transition().from(GenericCollectState.NO_DIGITS_REPROMPTING).toFinal(GenericCollectState.PROMPTED).on(GenericCollectEvent.END_PROMPT);
        this.builder.transition().from(GenericCollectState.NO_DIGITS_REPROMPTING).toFinal(GenericCollectState.PROMPTED).on(GenericCollectEvent.END_INPUT);
        this.builder.onExit(GenericCollectState.NO_DIGITS_REPROMPTING).callMethod("exitNoDigitsReprompting");
        
        this.builder.onEntry(GenericCollectState.PROMPTED).callMethod("enterPrompted");

        this.builder.onEntry(GenericCollectState.COLLECTING).callMethod("enterCollecting");
        this.builder.internalTransition().within(GenericCollectState.COLLECTING).on(GenericCollectEvent.DTMF_TONE).callMethod("onCollecting");
        this.builder.internalTransition().within(GenericCollectState.COLLECTING).on(GenericCollectEvent.RECOGNIZED_TEXT).callMethod("onTextRecognized");
        this.builder.transition().from(GenericCollectState.COLLECTING).toFinal(GenericCollectState.COLLECTED).on(GenericCollectEvent.END_INPUT);
        this.builder.onExit(GenericCollectState.COLLECTING).callMethod("exitCollecting");
        
        this.builder.onEntry(GenericCollectState.EVALUATING).callMethod("enterEvaluating");
        this.builder.transition().from(GenericCollectState.EVALUATING).to(GenericCollectState.SUCCEEDING).on(GenericCollectEvent.SUCCEED);
        this.builder.transition().from(GenericCollectState.EVALUATING).to(GenericCollectState.FAILING).on(GenericCollectEvent.NO_DIGITS);
        this.builder.transition().from(GenericCollectState.EVALUATING).to(GenericCollectState.FAILING).on(GenericCollectEvent.PATTERN_MISMATCH);
        this.builder.transition(TransitionPriority.HIGHEST).from(GenericCollectState.EVALUATING).to(GenericCollectState.CANCELED).on(GenericCollectEvent.CANCEL);
        this.builder.onExit(GenericCollectState.EVALUATING).callMethod("exitEvaluating");
        
        this.builder.onEntry(GenericCollectState.CANCELED).callMethod("enterCanceled");
        this.builder.transition().from(GenericCollectState.CANCELED).to(GenericCollectState.SUCCEEDED).on(GenericCollectEvent.SUCCEED);
        this.builder.transition().from(GenericCollectState.CANCELED).to(GenericCollectState.FAILED).on(GenericCollectEvent.FAIL);
        this.builder.onExit(GenericCollectState.CANCELED).callMethod("exitCanceled");
        
        this.builder.onEntry(GenericCollectState.FAILING).callMethod("enterFailing");
        this.builder.transition().from(GenericCollectState.FAILING).to(GenericCollectState.PLAY_COLLECT).on(GenericCollectEvent.REINPUT);
        this.builder.transition().from(GenericCollectState.FAILING).to(GenericCollectState.PLAY_COLLECT).on(GenericCollectEvent.RESTART);
        this.builder.transition().from(GenericCollectState.FAILING).to(GenericCollectState.PLAY_COLLECT).on(GenericCollectEvent.NO_DIGITS);
        this.builder.transition().from(GenericCollectState.FAILING).to(GenericCollectState.PLAYING_FAILURE).on(GenericCollectEvent.PROMPT);
        this.builder.transition().from(GenericCollectState.FAILING).toFinal(GenericCollectState.FAILED).on(GenericCollectEvent.NO_PROMPT);
        this.builder.transition(TransitionPriority.HIGHEST).from(GenericCollectState.FAILING).toFinal(GenericCollectState.FAILED).on(GenericCollectEvent.CANCEL);
        this.builder.onExit(GenericCollectState.FAILING).callMethod("exitFailing");

        this.builder.onEntry(GenericCollectState.PLAYING_FAILURE).callMethod("enterPlayingFailure");
        this.builder.internalTransition().within(GenericCollectState.PLAYING_FAILURE).on(GenericCollectEvent.NEXT_TRACK).callMethod("onPlayingFailure");
        this.builder.transition().from(GenericCollectState.PLAYING_FAILURE).toFinal(GenericCollectState.FAILED).on(GenericCollectEvent.END_PROMPT);
        this.builder.transition(TransitionPriority.HIGHEST).from(GenericCollectState.PLAYING_FAILURE).toFinal(GenericCollectState.FAILED).on(GenericCollectEvent.CANCEL);
        this.builder.onExit(GenericCollectState.PLAYING_FAILURE).callMethod("exitPlayingFailure");

        this.builder.onEntry(GenericCollectState.SUCCEEDING).callMethod("enterSucceeding");
        this.builder.transition().from(GenericCollectState.SUCCEEDING).to(GenericCollectState.PLAYING_SUCCESS).on(GenericCollectEvent.PROMPT);
        this.builder.transition().from(GenericCollectState.SUCCEEDING).toFinal(GenericCollectState.SUCCEEDED).on(GenericCollectEvent.NO_PROMPT);
        this.builder.transition(TransitionPriority.HIGHEST).from(GenericCollectState.SUCCEEDING).toFinal(GenericCollectState.SUCCEEDED).on(GenericCollectEvent.CANCEL);
        this.builder.onExit(GenericCollectState.SUCCEEDING).callMethod("exitSucceeding");
        
        this.builder.onEntry(GenericCollectState.PLAYING_SUCCESS).callMethod("enterPlayingSuccess");
        this.builder.internalTransition().within(GenericCollectState.PLAYING_SUCCESS).on(GenericCollectEvent.NEXT_TRACK).callMethod("onPlayingSuccess");
        this.builder.transition().from(GenericCollectState.PLAYING_SUCCESS).toFinal(GenericCollectState.SUCCEEDED).on(GenericCollectEvent.END_PROMPT);
        this.builder.transition(TransitionPriority.HIGHEST).from(GenericCollectState.PLAYING_SUCCESS).toFinal(GenericCollectState.SUCCEEDED).on(GenericCollectEvent.CANCEL);
        this.builder.onExit(GenericCollectState.PLAYING_SUCCESS).callMethod("exitPlayingSuccess");

        this.builder.onEntry(GenericCollectState.SUCCEEDED).callMethod("enterSucceeded");
        this.builder.onEntry(GenericCollectState.FAILED).callMethod("enterFailed");
    }

    public GenericCollectFsm build(DtmfDetector detector, Player player, AsrEngine asrEngine,
                                   GenericCollectFsmImpl.CollectStateHandlerExt handlerExt,
                                   MgcpEventSubject eventSubject, ListeningScheduledExecutorService scheduler,
                                   GenericCollectContext context) {
        return builder.newStateMachine(GenericCollectState.PLAY_COLLECT,
                StateMachineConfiguration.getInstance().enableDebugMode(false), detector, player,
                asrEngine, handlerExt, eventSubject, scheduler, context);
    }

}
