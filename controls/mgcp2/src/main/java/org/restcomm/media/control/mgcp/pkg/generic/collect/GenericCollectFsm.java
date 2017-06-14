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

import org.restcomm.media.spi.dtmf.DtmfDetectorListener;
import org.restcomm.media.spi.player.PlayerListener;
import org.squirrelframework.foundation.fsm.StateMachine;

/**
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public interface GenericCollectFsm extends StateMachine<GenericCollectFsm, GenericCollectState, GenericCollectEvent, GenericCollectContext> {
    DtmfDetectorListener getDetectorListener();

    PlayerListener getPlayerListener();
    
    void enterPlayCollect(GenericCollectState from, GenericCollectState to, GenericCollectEvent event, GenericCollectContext context);

    void exitPlayCollect(GenericCollectState from, GenericCollectState to, GenericCollectEvent event, GenericCollectContext context);

    void enterLoadingPlaylist(GenericCollectState from, GenericCollectState to, GenericCollectEvent event, GenericCollectContext context);
    
    void exitLoadingPlaylist(GenericCollectState from, GenericCollectState to, GenericCollectEvent event, GenericCollectContext context);

    void enterPrompting(GenericCollectState from, GenericCollectState to, GenericCollectEvent event, GenericCollectContext context);

    void onPrompting(GenericCollectState from, GenericCollectState to, GenericCollectEvent event, GenericCollectContext context);

    void exitPrompting(GenericCollectState from, GenericCollectState to, GenericCollectEvent event, GenericCollectContext context);

    void enterReprompting(GenericCollectState from, GenericCollectState to, GenericCollectEvent event, GenericCollectContext context);

    void onReprompting(GenericCollectState from, GenericCollectState to, GenericCollectEvent event, GenericCollectContext context);

    void exitReprompting(GenericCollectState from, GenericCollectState to, GenericCollectEvent event, GenericCollectContext context);

    void enterNoDigitsReprompting(GenericCollectState from, GenericCollectState to, GenericCollectEvent event, GenericCollectContext context);

    void onNoDigitsReprompting(GenericCollectState from, GenericCollectState to, GenericCollectEvent event, GenericCollectContext context);

    void exitNoDigitsReprompting(GenericCollectState from, GenericCollectState to, GenericCollectEvent event, GenericCollectContext context);

    void enterPrompted(GenericCollectState from, GenericCollectState to, GenericCollectEvent event, GenericCollectContext context);
    
    void enterCollecting(GenericCollectState from, GenericCollectState to, GenericCollectEvent event, GenericCollectContext context);

    void onCollecting(GenericCollectState from, GenericCollectState to, GenericCollectEvent event, GenericCollectContext context);

    void onTextRecognized(GenericCollectState from, GenericCollectState to, GenericCollectEvent event, GenericCollectContext context);

    void exitCollecting(GenericCollectState from, GenericCollectState to, GenericCollectEvent event, GenericCollectContext context);

    void enterEvaluating(GenericCollectState from, GenericCollectState to, GenericCollectEvent event, GenericCollectContext context);

    void exitEvaluating(GenericCollectState from, GenericCollectState to, GenericCollectEvent event, GenericCollectContext context);

    void enterCanceled(GenericCollectState from, GenericCollectState to, GenericCollectEvent event, GenericCollectContext context);

    void exitCanceled(GenericCollectState from, GenericCollectState to, GenericCollectEvent event, GenericCollectContext context);
    
    void enterSucceeding(GenericCollectState from, GenericCollectState to, GenericCollectEvent event, GenericCollectContext context);
    
    void exitSucceeding(GenericCollectState from, GenericCollectState to, GenericCollectEvent event, GenericCollectContext context);
    
    void enterPlayingSuccess(GenericCollectState from, GenericCollectState to, GenericCollectEvent event, GenericCollectContext context);

    void onPlayingSuccess(GenericCollectState from, GenericCollectState to, GenericCollectEvent event, GenericCollectContext context);

    void exitPlayingSuccess(GenericCollectState from, GenericCollectState to, GenericCollectEvent event, GenericCollectContext context);

    void enterSucceeded(GenericCollectState from, GenericCollectState to, GenericCollectEvent event, GenericCollectContext context);
    
    void enterFailing(GenericCollectState from, GenericCollectState to, GenericCollectEvent event, GenericCollectContext context);
    
    void exitFailing(GenericCollectState from, GenericCollectState to, GenericCollectEvent event, GenericCollectContext context);
    
    void enterPlayingFailure(GenericCollectState from, GenericCollectState to, GenericCollectEvent event, GenericCollectContext context);

    void onPlayingFailure(GenericCollectState from, GenericCollectState to, GenericCollectEvent event, GenericCollectContext context);

    void exitPlayingFailure(GenericCollectState from, GenericCollectState to, GenericCollectEvent event, GenericCollectContext context);

    void enterFailed(GenericCollectState from, GenericCollectState to, GenericCollectEvent event, GenericCollectContext context);

}