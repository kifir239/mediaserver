package org.restcomm.media.resource.asr;

import org.restcomm.media.spi.pooling.AbstractConcurrentResourcePool;
import org.restcomm.media.spi.pooling.PooledObjectFactory;

/**
 * Created by hamsterksu on 6/5/17.
 */
public class AsrEnginePool extends AbstractConcurrentResourcePool<AsrEngineImpl> {

    private final PooledObjectFactory<AsrEngineImpl> recorderFactory;

    public AsrEnginePool(int initialCapacity, PooledObjectFactory<AsrEngineImpl> recorderFactory) {
        super(initialCapacity);
        this.recorderFactory = recorderFactory;
        populate();
    }

    @Override
    protected AsrEngineImpl createResource() {
        return this.recorderFactory.produce();
    }

}
