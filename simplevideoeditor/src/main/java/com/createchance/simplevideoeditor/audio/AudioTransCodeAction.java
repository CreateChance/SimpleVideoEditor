package com.createchance.simplevideoeditor.audio;

import com.createchance.simplevideoeditor.AbstractAction;
import com.createchance.simplevideoeditor.ActionCallback;

import java.io.File;

/**
 * ${DESC}
 *
 * @author gaochao1-iri
 * @date 25/03/2018
 */

public class AudioTransCodeAction extends AbstractAction {
    @Override
    public void start(ActionCallback callback) {

    }

    public enum FORMAT {
        NONE,
        MP3,
        AAC
    }

    private File mInputFile;
    private File mOutputFile;
    private FORMAT mTargetFormat = FORMAT.NONE;

    private AudioTransCodeAction() {

    }

    public File getInputFile() {
        return mInputFile;
    }

    public File getOutputFile() {
        return mOutputFile;
    }

    public FORMAT getTargetFormat() {
        return mTargetFormat;
    }

    public static class Builder {
        private AudioTransCodeAction transCodeAction = new AudioTransCodeAction();

        public Builder transcode(File inputFile) {
            transCodeAction.mInputFile = inputFile;

            return this;
        }

        public Builder to(File outputFile) {
            transCodeAction.mOutputFile = outputFile;

            return this;
        }

        public AudioTransCodeAction build() {
            return transCodeAction;
        }
    }
}
