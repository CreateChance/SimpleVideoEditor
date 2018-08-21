package com.createchance;

import android.net.Uri;

/**
 * ${DESC}
 *
 * @author createchance
 * @date 13/04/2018
 */
public class PlayRequest {
    public Uri videoSource;

    public int startPos;

    public boolean loop;

    public int leftVolume = -1;
    public int rightVolume = -1;

    private PlayRequest() {

    }

    public static class Builder {
        private PlayRequest request = new PlayRequest();

        public Builder source(Uri source) {
            request.videoSource = source;

            return this;
        }

        public Builder startFrom(int from) {
            request.startPos = from;

            return this;
        }

        public Builder loop(boolean loop) {
            request.loop = loop;

            return this;
        }

        public Builder leftVolume(int vol) {
            request.leftVolume = vol;

            return this;
        }

        public Builder rightVolume(int vol) {
            request.rightVolume = vol;

            return this;
        }

        public PlayRequest build() {
            return request;
        }
    }
}
