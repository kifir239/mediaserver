package org.restcomm.media.resource.asr;

import org.restcomm.media.resource.asr.api.AsrDriver;

import java.util.HashMap;

/**
 * Created by hamsterksu on 6/5/17.
 */
public class AsrDriverManager {

    private HashMap<String, AsrDriver> providers = new HashMap();

    public void registerDriver(String name, AsrDriver engine) {
        providers.put(name, engine);
    }

    public AsrDriver getDriver(String name) {
        AsrDriver engine = providers.get(name);
        if (engine == null) {
            throw new IllegalStateException(String.format("Provider with name '%s' is not registered", name));
        }
        return engine;
    }
}
