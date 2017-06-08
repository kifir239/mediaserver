package org.restcomm.media.resource.asr;

import org.restcomm.media.scheduler.PriorityQueueScheduler;
import org.restcomm.media.spi.pooling.PooledObjectFactory;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by hamsterksu on 6/5/17.
 */
public class AsrEngineFactory implements PooledObjectFactory<AsrEngineImpl> {

    private final AtomicInteger id;

    private final PriorityQueueScheduler mediaScheduler;

    private final AsrDriverManager driverManager;

    public AsrEngineFactory(PriorityQueueScheduler mediaScheduler, AsrDriverManager driverManager) {
        this.mediaScheduler = mediaScheduler;
        this.driverManager = driverManager;
        this.id = new AtomicInteger(0);
    }

    @Override
    public AsrEngineImpl produce() {
        return new AsrEngineImpl(nextId(), this.mediaScheduler, this.driverManager);
    }

    private String nextId() {
        return "asr-engine" + id.getAndIncrement();
    }

}
