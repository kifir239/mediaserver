package org.restcomm.media.resource.asr.api;

import java.nio.ByteBuffer;
import java.util.Map;

/**
 * Created by hamsterksu on 6/5/17.
 */
public interface AsrDriver {
    void configure(Map<String, String> parameters);

    void startRecognizing(String lang);

    void write(ByteBuffer byteBuffer);

    void write(byte[] data, int offset, int len);

    void finishRecognizing();

    void setListener(AsrDriverEventListener listener);

    interface AsrDriverEventListener {
        void onSpeechRecognized(String text);
        void onError(final AsrError error, final String description);
    }
}
