package org.restcomm.media.resource.asr;

import org.restcomm.media.ComponentType;
import org.restcomm.media.component.AbstractSink;
import org.restcomm.media.component.audio.AudioOutput;
import org.restcomm.media.resource.asr.api.AsrDriver;
import org.restcomm.media.resource.asr.api.AsrError;
import org.restcomm.media.scheduler.PriorityQueueScheduler;
import org.restcomm.media.scheduler.Task;
import org.restcomm.media.spi.memory.Frame;
import org.restcomm.media.spi.pooling.PooledObject;

import java.io.IOException;

/**
 * Created by hamsterksu on 6/5/17.
 */
public class AsrEngineImpl extends AbstractSink implements AsrEngine, PooledObject {

    private final AsrDriverManager driverManager;
    private final PriorityQueueScheduler scheduler;

    private final AudioOutput output;

    private AsrDriver driver;
    private String lang;
    private AsrEngineListener listener;

    public AsrEngineImpl(String name, PriorityQueueScheduler scheduler, AsrDriverManager driverManager) {
        super(name);
        this.driverManager = driverManager;
        this.scheduler = scheduler;

        output = new AudioOutput(scheduler, ComponentType.RECORDER.getType());
        output.join(this);
    }

    @Override
    public void configure(String driverName, String language) {
        this.driver = driverManager.getDriver(driverName);
        this.lang = lang;
    }

    @Override
    public void setListener(AsrEngineListener listener) {
        this.listener = listener;
    }

    @Override
    public void checkIn() {
        System.out.println(" !! checkIn");
    }

    @Override
    public void checkOut() {
        System.out.println(" !! checkOut");
    }

    @Override
    public void onMediaTransfer(Frame frame) throws IOException {
        byte[] data = frame.getData();
        int offset = frame.getOffset();
        int len = frame.getLength();

        driver.write(data, offset, len);
    }

    @Override
    public void activate() {
        System.out.println(" !! activate");
        this.driver.setListener(driverEventListener);
        this.driver.startRecognizing(lang);

        output.start();
    }

    @Override
    public void deactivate() {
        System.out.println(" !! deactivate");
        if (!this.isStarted()) {
            return;
        }
        try {
            output.stop();
        } catch (Exception e) {
            //TODO use logger
            e.printStackTrace();
        }
        driver.finishRecognizing();
        driver.setListener(null);
    }

    public AudioOutput getAudioOutput() {
        return output;
    }

    private void fireEvent(final String text) {
        System.out.println("!! fireEvent " + text);
        scheduler.submit(new Task() {
            @Override
            public int getQueueNumber() {
                return 0;
            }

            @Override
            public long perform() {
                if (AsrEngineImpl.this.listener != null) {
                    System.out.println("!! AsrEngineImpl.this.listener.onSpeechRecognized(text);");
                    AsrEngineImpl.this.listener.onSpeechRecognized(text);
                }
                return 0;
            }
        }, PriorityQueueScheduler.INPUT_QUEUE);
    }

    private AsrDriver.AsrDriverEventListener driverEventListener = new AsrDriver.AsrDriverEventListener() {
        @Override
        public void onSpeechRecognized(String text) {
            fireEvent(text);
        }

        @Override
        public void onError(final AsrError error, final String description) {
        }
    };
}
