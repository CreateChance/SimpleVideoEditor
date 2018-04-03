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

public class AudioCutAction extends AbstractAction {

    private File mInputFile;
    private File mOutputFile;
    private long mFromTimePos;
    private long mToTimePos;

    private AudioCutAction() {

    }

    public File getInputFile() {
        return mInputFile;
    }

    public File getOutputFile() {
        return mOutputFile;
    }

    public long getFromTimePos() {
        return mFromTimePos;
    }

    public long getToTimePos() {
        return mToTimePos;
    }

    @Override
    public void start(ActionCallback callback) {

    }

    public static class Builder {
        private AudioCutAction cutAction = new AudioCutAction();

        public Builder cut(File target) {
            cutAction.mInputFile = target;

            return this;
        }

        public Builder from(long fromPos) {
            cutAction.mFromTimePos = fromPos;

            return this;
        }

        public Builder to(long toPos) {
            cutAction.mToTimePos = toPos;

            return this;
        }

        public Builder saveAs(File dest) {
            cutAction.mOutputFile = dest;

            return this;
        }

        public AudioCutAction build() {
            return cutAction;
        }
    }
}
