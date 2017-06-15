package org.restcomm.media.bootstrap.ioc.provider.media;

import com.google.inject.Inject;
import com.google.inject.Provider;
import org.apache.log4j.Logger;
import org.restcomm.media.core.configuration.DriverConfiguration;
import org.restcomm.media.core.configuration.MediaServerConfiguration;
import org.restcomm.media.core.configuration.SubsystemsConfiguration;
import org.restcomm.media.resource.asr.*;
import org.restcomm.media.scheduler.PriorityQueueScheduler;

import java.util.Collection;

/**
 * Created by hamsterksu on 6/5/17.
 */
public class AsrEngineProviderProvider implements Provider<AsrEngineProvider> {
    private Logger logger = Logger.getLogger(AsrEngineProviderProvider.class);

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
        final SubsystemsConfiguration subsystemsConfiguration = configuration.getSubsystemsConfiguration();
        final Collection<DriverConfiguration> drivers = subsystemsConfiguration.getDrivers("asr");
        if (drivers != null) {
            for (final DriverConfiguration driver: drivers) {
                final String driverName = driver.getDriverName();
                final String className = driver.getClassName();
                try {
                    final Class<?> clazz = Class.forName(className);
                    final AsrDriver object = (AsrDriver) clazz.newInstance();
                    mng.registerDriver(driverName, object);
                    logger.info("Driver \'" + driverName + "' (" + className + ") is successfully registered");
                } catch (final ClassNotFoundException | InstantiationException | IllegalAccessException e) {
                    logger.error("Failed to register instance of asr driver (className = " + className + "): "
                            + e.getMessage());
                }
            }
        } else {
            logger.error("Asr driver is not configured - we will use StubAsrDriver");
            mng.registerDriver("stub", new StubAsrDriver());
        }
        return new AsrEngineProviderIml(scheduler, mng);
    }
}
