package com.createchance.simplevideoeditor;

import java.io.File;

/**
 * ${DESC}
 *
 * @author gaochao1-iri
 * @date 24/04/2018
 */
class VideoInfo {
    static final int INVALID_TRACK_ID = -1;
    File video;
    int width;
    int height;
    long durationMs;
    int rotation;
    int audioTrackId = INVALID_TRACK_ID;
    int videoTrackId = INVALID_TRACK_ID;

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        VideoInfo videoInfo = (VideoInfo) o;

        return video != null ? video.equals(videoInfo.video) : videoInfo.video == null;
    }

    @Override
    public int hashCode() {
        return video != null ? video.hashCode() : 0;
    }

    @Override
    public String toString() {
        return "VideoInfo{" +
                "video=" + video +
                ", width=" + width +
                ", height=" + height +
                ", durationMs=" + durationMs +
                ", rotation=" + rotation +
                ", audioTrackId=" + audioTrackId +
                ", videoTrackId=" + videoTrackId +
                '}';
    }
}
