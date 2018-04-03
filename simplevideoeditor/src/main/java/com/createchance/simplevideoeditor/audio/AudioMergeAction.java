package com.createchance.simplevideoeditor.audio;

import com.createchance.simplevideoeditor.AbstractAction;
import com.createchance.simplevideoeditor.ActionCallback;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * ${DESC}
 *
 * @author gaochao1-iri
 * @date 25/03/2018
 */

public class AudioMergeAction extends AbstractAction {
    private List<File> mInputFiles = new ArrayList<>();
    private File mDestAudioFile;

    private AudioMergeAction() {

    }

    public List<File> getInputFiles() {
        return mInputFiles;
    }

    public File getDestAudioFile() {
        return mDestAudioFile;
    }

    @Override
    public void start(ActionCallback callback) {

    }

    public static class Builder {
        private AudioMergeAction mergeAction = new AudioMergeAction();

        public Builder merge(File audioFile) {
            mergeAction.mInputFiles.add(audioFile);

            return this;
        }

        public Builder saveAs(File dest) {
            mergeAction.mDestAudioFile = dest;

            return this;
        }

        public AudioMergeAction build() {
            return mergeAction;
        }

    }
}
