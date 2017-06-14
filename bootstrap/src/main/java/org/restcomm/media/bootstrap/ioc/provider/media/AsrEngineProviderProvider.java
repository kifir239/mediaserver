package org.restcomm.media.bootstrap.ioc.provider.media;

import com.google.inject.Inject;
import com.google.inject.Provider;
import org.restcomm.media.core.configuration.MediaServerConfiguration;
import org.restcomm.media.resource.asr.*;
import org.restcomm.media.scheduler.PriorityQueueScheduler;

import java.nio.ByteBuffer;

/**
 * Created by hamsterksu on 6/5/17.
 */
public class AsrEngineProviderProvider implements Provider<AsrEngineProvider> {

    private final PriorityQueueScheduler scheduler;
    private final MediaServerConfiguration configuration;
    //private final AsrDriverManager driverManager;

    @Inject
    public AsrEngineProviderProvider(PriorityQueueScheduler scheduler, MediaServerConfiguration configuration) {
        super();
        this.scheduler = scheduler;
        this.configuration = configuration;
    }

/*    @Override
    public DtmfDetectorProvider get() {
        int volume = this.configuration.getResourcesConfiguration().getDtmfDetectorDbi();
        int duration = this.configuration.getResourcesConfiguration().getDtmfDetectorToneDuration();
        int interval = this.configuration.getResourcesConfiguration().getDtmfDetectorToneInterval();
        return new DetectorProvider(scheduler, volume, duration, interval);
    }*/

    @Override
    public AsrEngineProvider get() {
        AsrDriverManager mng = new AsrDriverManager();
        mng.registerDriver("stub", new StubAsrDriver());
        return new AsrEngineProviderIml(scheduler, mng);
    }
}
