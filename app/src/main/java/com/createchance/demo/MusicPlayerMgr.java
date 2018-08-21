package com.createchance.demo;

import android.media.MediaPlayer;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * music player
 *
 * @author createchance
 * @date 2018-08-19
 */
public class MusicPlayerMgr {
    private MediaPlayer mPlayer;

    private boolean isPlaying = false;

    private List<MusicPlayerCallback> mCallbacks = new ArrayList<>();

    private MusicPlayerMgr(MediaPlayer player) {
        this.mPlayer = player;
        mPlayer.setOnErrorListener(new MediaPlayer.OnErrorListener() {
            @Override
            public boolean onError(MediaPlayer mediaPlayer, int i, int i1) {
                mPlayer.reset();
                isPlaying = false;
                return true;
            }
        });
        mPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mediaPlayer) {
                mPlayer.reset();
                isPlaying = false;

                for (MusicPlayerCallback callback : mCallbacks) {
                    callback.onComplete();
                }
            }
        });
        mPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mediaPlayer) {
                mPlayer.start();
            }
        });
    }

    public static MusicPlayerMgr getInstance() {
        return MusicPlayerHolder.instance;
    }

    public void addCallback(MusicPlayerCallback callback) {
        if (callback == null) {
            return;
        }

        mCallbacks.add(callback);
    }

    public void removeCallback(MusicPlayerCallback callback) {
        if (callback == null) {
            return;
        }

        mCallbacks.remove(callback);
    }

    public boolean start(File music, boolean isLoop) {
        if (isPlaying) {
            return false;
        }

        if (music == null || music.isDirectory() || !music.exists()) {
            return false;
        }

        try {
            mPlayer.setDataSource(music.getAbsolutePath());
            mPlayer.prepareAsync();
            mPlayer.setLooping(isLoop);
            isPlaying = true;
        } catch (IOException e) {
            mPlayer.reset();
            return false;
        }

        return true;
    }

    public boolean start(String music, boolean isLoop) {
        if (isPlaying) {
            return false;
        }

        if (music == null) {
            return false;
        }

        try {
            mPlayer.setDataSource(music);
            mPlayer.prepareAsync();
            mPlayer.setLooping(isLoop);
            isPlaying = true;
        } catch (IOException e) {
            mPlayer.reset();
            return false;
        }

        return true;
    }

    public boolean stop() {
        if (!isPlaying) {
            return false;
        }

        mPlayer.stop();

        mPlayer.reset();

        isPlaying = false;

        return true;
    }

    private static class MusicPlayerHolder {
        private static final MusicPlayerMgr instance = new MusicPlayerMgr(new MediaPlayer());
    }

    public static interface MusicPlayerCallback {
        void onComplete();
    }
}
