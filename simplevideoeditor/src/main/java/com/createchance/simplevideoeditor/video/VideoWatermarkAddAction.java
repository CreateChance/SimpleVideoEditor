package com.createchance.simplevideoeditor.video;

import android.graphics.Bitmap;

import com.createchance.simplevideoeditor.AbstractAction;
import com.createchance.simplevideoeditor.ActionCallback;

import java.io.File;

/**
 * ${DESC}
 *
 * @author gaochao1-iri
 * @date 25/03/2018
 */

public class VideoWatermarkAddAction extends AbstractAction {
    private File mInputFile;
    private long mFromPos;
    private long mToPos;
    private String mText;
    private Bitmap mImage;
    private float mXPosPercent;
    private float mYPosPercent;

    private VideoWatermarkAddAction() {

    }

    public File getInputFile() {
        return mInputFile;
    }

    public long getFromPos() {
        return mFromPos;
    }

    public long getToPos() {
        return mToPos;
    }

    public String getText() {
        return mText;
    }

    public Bitmap getImage() {
        return mImage;
    }

    public float getXPosPercent() {
        return mXPosPercent;
    }

    public float getYPosPercent() {
        return mYPosPercent;
    }

    @Override
    public void start(ActionCallback callback) {

    }

    public static class Builder {
        private VideoWatermarkAddAction watermarkAddAction = new VideoWatermarkAddAction();

        public Builder addWatermark(String text) {
            watermarkAddAction.mText = text;

            return this;
        }

        public Builder addWatermark(Bitmap image) {
            watermarkAddAction.mImage = image;

            return this;
        }

        public Builder atXPos(float percent) {
            watermarkAddAction.mXPosPercent = percent;

            return this;
        }

        public Builder atYPos(float percent) {
            watermarkAddAction.mYPosPercent = percent;

            return this;
        }

        public Builder from(long from) {
            watermarkAddAction.mFromPos = from;

            return this;
        }

        public Builder to(long to) {
            watermarkAddAction.mToPos = to;

            return this;
        }

        public VideoWatermarkAddAction build() {
            return watermarkAddAction;
        }
    }
}
