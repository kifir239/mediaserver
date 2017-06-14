package org.restcomm.media.bootstrap.ioc.provider.media;

import com.google.inject.Inject;
import com.google.inject.Provider;
import org.restcomm.media.core.configuration.MediaServerConfiguration;
import org.restcomm.media.resource.asr.AsrDriver;
import org.restcomm.media.resource.asr.AsrDriverManager;
import org.restcomm.media.resource.asr.AsrEngineProvider;
import org.restcomm.media.resource.asr.AsrEngineProviderIml;
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
        mng.registerDriver("stub", new AsrDriver() {

            private AsrDriverEventListener listener;
            private long lastEventTime = 0;

            @Override
            public void startRecognizing(String lang) {
                System.out.println("!!! startRecognizing");
                lastEventTime = System.currentTimeMillis();
            }

            @Override
            public void write(ByteBuffer byteBuffer) {
                System.out.println("!!! write ByteBuffer");
            }

            @Override
            public void write(byte[] data, int offset, int len) {
                System.out.println("!!! write byte[]: " + (System.currentTimeMillis() - lastEventTime));
                if (System.currentTimeMillis() - lastEventTime > 5000 && listener != null) {
                    lastEventTime = System.currentTimeMillis();
                    listener.onSpeechRecognized("1");
                }
            }

            @Override
            public void finishRecognizing() {
                System.out.println("!!! finishRecognizing");
            }

            @Override
            public void setListener(AsrDriverEventListener listener) {
                this.listener = listener;
            }

        });
        return new AsrEngineProviderIml(scheduler, mng);
    }
}
