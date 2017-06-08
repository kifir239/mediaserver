package org.restcomm.media.resource.asr;

import org.restcomm.media.MediaSink;

/**
 * Created by hamsterksu on 6/5/17.
 */
public interface AsrEngine extends MediaSink {

    void configure(String driver, String language);

    void setListener(AsrEngineListener listener);

    interface AsrEngineListener {
        void onSpeechRecognized(String text);
    }
}
