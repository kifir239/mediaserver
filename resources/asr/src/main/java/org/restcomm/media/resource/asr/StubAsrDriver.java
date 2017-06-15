package org.restcomm.media.resource.asr;

import org.restcomm.media.resource.asr.api.AsrDriver;

import java.nio.ByteBuffer;
import java.util.Map;

/**
 * Created by anikiforov on 6/15/2017.
 */
public class StubAsrDriver implements AsrDriver {
    private AsrDriverEventListener listener;
    private long lastEventTime = 0;

    @Override
    public void configure(Map<String, String> parameters) {
        System.out.println("!!! configure");
    }

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
}
