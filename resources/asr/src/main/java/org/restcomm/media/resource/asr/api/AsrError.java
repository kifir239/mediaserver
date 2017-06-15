package org.restcomm.media.resource.asr.api;

/**
 * Created by anikiforov on 6/15/2017.
 */
public enum AsrError {
    UNEXPECTED_ERROR("UNEXPECTED_ERROR");

    private final String stringRepr;
    private AsrError(final String stringRepr) {
        this.stringRepr = stringRepr;
    }

    @Override
    public String toString() {
        return stringRepr;
    }
}
