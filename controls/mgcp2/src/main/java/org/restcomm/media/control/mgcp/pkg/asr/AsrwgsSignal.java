package org.restcomm.media.control.mgcp.pkg.asr;

import com.google.common.util.concurrent.ListeningScheduledExecutorService;
import org.restcomm.media.control.mgcp.command.param.NotifiedEntity;
import org.restcomm.media.control.mgcp.pkg.AbstractMgcpSignal;
import org.restcomm.media.control.mgcp.pkg.MgcpEventSubject;
import org.restcomm.media.control.mgcp.pkg.SignalType;
import org.restcomm.media.control.mgcp.pkg.generic.collect.*;
import org.restcomm.media.resource.asr.AsrEngine;
import org.restcomm.media.spi.dtmf.DtmfDetector;
import org.restcomm.media.spi.player.Player;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Created by hamsterksu on 6/5/17.
 */
public class AsrwgsSignal extends AbstractMgcpSignal {

    private static final String SYMBOL = "asrwgs";

    private final static Set<String> supportedParams = new HashSet<>();

    static {
        supportedParams.add("rgs");
        supportedParams.add("rgst");
        supportedParams.add("rgsf");
        supportedParams.add("mrt");
        supportedParams.add("eik");
    }

    private final GenericCollectFsm fsm;

    private final GenericCollectContext context;

    public AsrwgsSignal(Player player, DtmfDetector detector, AsrEngine asrEngine, int requestId, NotifiedEntity notifiedEntity, Map<String, String> parameters, ListeningScheduledExecutorService executor) {
        super(AsrPackage.PACKAGE_NAME, SYMBOL, SignalType.TIME_OUT, requestId, notifiedEntity, parameters);

        // Execution Context
        this.context = new GenericCollectContext(new ParameterParser().parse());

        // Build FSM
        this.fsm = GenericCollectFsmBuilder.INSTANCE.build(detector, player, asrEngine, handler, this, executor, context);
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

    @Override
    protected boolean isParameterSupported(String name) {
        return supportedParams.contains(name);
    }

    private GenericCollectFsmImpl.CollectStateHandlerExt handler = new GenericCollectFsmImpl.CollectStateHandlerExt() {

        private MgcpEventSubject mgcpEventSubject = AsrwgsSignal.this;

        @Override
        public void enterSucceeded(GenericCollectState from, GenericCollectState to, GenericCollectEvent event, GenericCollectContext context) {
            //TODO IMPLEMENT IT
            /*final int attempt = context.getAttempt();
            String collectedDigits = context.getCollectedDigits();
            if (context.getIncludeEndInputKey()) {
                collectedDigits += context.getEndInputKey();
            }

            final OperationComplete operationComplete = new OperationComplete(AsrwgsSignal.SYMBOL, ReturnCode.SUCCESS.code());
            operationComplete.setParameter("na", String.valueOf(attempt));
            operationComplete.setParameter("dc", collectedDigits);
            mgcpEventSubject.notify(mgcpEventSubject, operationComplete);*/
        }

        @Override
        public void enterFailed(GenericCollectState from, GenericCollectState to, GenericCollectEvent event, GenericCollectContext context) {
            //TODO IMPLEMENT IT
            /*final OperationFailed operationFailed = new OperationFailed(AsrwgsSignal.SYMBOL, context.getReturnCode());
            mgcpEventSubject.notify(mgcpEventSubject, operationFailed);*/
        }
    };

    private class ParameterParser {

        GenericCollectContext.Parameters parse() {
            //TODO implement it
            return new GenericCollectContext.Parameters(
                    null,
                    null,
                    null,
                    null,
                    null,
                    false,
                    false,
                    0, 0, null, false, 0, 0, 0,
                    ' ', ' ', ' ', ' ', ' ',
                    null, ' ', false, 0
            );
        }
    }
}
