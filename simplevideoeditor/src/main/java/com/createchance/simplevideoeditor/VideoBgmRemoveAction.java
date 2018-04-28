package com.createchance.simplevideoeditor;

import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * ${DESC}
 *
 * @author gaochao1-iri
 * @date 25/03/2018
 */

public class VideoBgmRemoveAction extends AbstractAction {

    private static final String TAG = "VideoBgmRemoveAction";

    private File mInputFile;
    private File mOutputFile;
    private long mRemoveStartPosMs;
    private long mRemoveDurationMs;

    private MediaExtractor mMediaExtractor;
    private MediaMuxer mMediaMuxer;
    private int mVideoTrackIndex = -1;

    private RemoveWorker mRemoveWorker;

    private VideoBgmRemoveAction() {

    }

    public File getInputFile() {
        return mInputFile;
    }

    public long getRemoveStartPos() {
        return mRemoveStartPosMs;
    }

    public long getRemoveDuration() {
        return mRemoveDurationMs;
    }

    @Override
    public void start(ActionCallback callback) {
        super.start(callback);
        if (checkRational()) {
            mRemoveWorker = new RemoveWorker();
            ActionRunner.addTaskToBackground(mRemoveWorker);
        } else {
            Log.e(TAG, "Remove bgm start failed, params error.");
        }
    }

    private boolean checkRational() {
        if (mInputFile == null) {
            return false;
        }

        if (mOutputFile == null) {
            return false;
        }

        if (mRemoveStartPosMs < 0) {
            return false;
        }

        return mRemoveDurationMs > 0;
    }

    public static class Builder {
        private VideoBgmRemoveAction bgmRemoveAction = new VideoBgmRemoveAction();

        public Builder removeBgm(File video) {
            bgmRemoveAction.mInputFile = video;

            return this;
        }

        public Builder from(long fromMs) {
            bgmRemoveAction.mRemoveStartPosMs = fromMs;

            return this;
        }

        public Builder duration(long durationMs) {
            bgmRemoveAction.mRemoveDurationMs = durationMs;

            return this;
        }

        public Builder saveAs(File output) {
            bgmRemoveAction.mOutputFile = output;

            return this;
        }

        public VideoBgmRemoveAction build() {
            return bgmRemoveAction;
        }
    }

    private class RemoveWorker implements Runnable {
        ByteBuffer byteBuffer = ByteBuffer.allocate(512 * 1024);
        ByteBuffer emptyBuffer = ByteBuffer.allocate(512 * 1024);
        boolean isMuxerStarted;

        @Override
        public void run() {
            try {
                prepare();

                removeBgm();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                release();
            }
        }

        private void prepare() throws IOException {
            mMediaExtractor = new MediaExtractor();
            mMediaExtractor.setDataSource(mInputFile.getAbsolutePath());
            mMediaMuxer = new MediaMuxer(mOutputFile.getAbsolutePath(), MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
        }

        private void removeBgm() {
            int inVideoTrackId = -1;
            int inAudioTrackId = -1;
            int outVideoTrackId = -1;
            int outAudioTrackId = -1;
            MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
            for (int i = 0; i < mMediaExtractor.getTrackCount(); i++) {
                MediaFormat mediaFormat = mMediaExtractor.getTrackFormat(i);
                String mime = mediaFormat.getString(MediaFormat.KEY_MIME);
                if (mime.startsWith("video")) {
                    Log.d(TAG, "removeBgm, found video track.");
                    inVideoTrackId = i;
                    outVideoTrackId = mMediaMuxer.addTrack(mediaFormat);
                    long videoDurationUs = mediaFormat.getLong(MediaFormat.KEY_DURATION);
                    // check start pos and duration setting, only check for video.
                    if ((mRemoveStartPosMs + mRemoveDurationMs) * 1000 > videoDurationUs) {
                        Log.e(TAG, "removeBgm error. Video duration is " + videoDurationUs +
                                ", but start pos: " + mRemoveStartPosMs * 1000 +
                                " and duration is: " + mRemoveDurationMs * 1000);
                        return;
                    }
                } else if (mime.startsWith("audio")) {
                    Log.d(TAG, "removeBgm, found audio track");
                    inAudioTrackId = i;
                    outAudioTrackId = mMediaMuxer.addTrack(mediaFormat);
                }
            }

            // Muxer start only when we found video track.
            if (inVideoTrackId != -1) {
                Log.d(TAG, "removeBgm, media muxer started!");
                mMediaMuxer.start();
                isMuxerStarted = true;
            } else {
                Log.e(TAG, "We do not found any video track in input file: " + mInputFile);
                return;
            }

            // handle video track first, just write all video data to result.
            mMediaExtractor.selectTrack(inVideoTrackId);
            while (true) {
                int sampleSize = mMediaExtractor.readSampleData(byteBuffer, 0);
                Log.d(TAG, "removeBgm, read data: " + sampleSize);
                if (sampleSize > 0) {
                    bufferInfo.size = sampleSize;
                    bufferInfo.offset = 0;
                    bufferInfo.presentationTimeUs = mMediaExtractor.getSampleTime();
                    bufferInfo.flags = mMediaExtractor.getSampleFlags();
                    mMediaMuxer.writeSampleData(outVideoTrackId, byteBuffer, bufferInfo);
                    mMediaExtractor.advance();
                } else {
                    Log.d(TAG, "removeBgm, read video done!");
                    break;
                }
            }

            // handle audio track if input video have one.
            if (inAudioTrackId != -1) {
                mMediaExtractor.unselectTrack(inVideoTrackId);
                mMediaExtractor.selectTrack(inAudioTrackId);
                while (true) {
                    int sampleSize = mMediaExtractor.readSampleData(byteBuffer, 0);
                    if (sampleSize > 0) {
                        bufferInfo.size = sampleSize;
                        bufferInfo.offset = 0;
                        bufferInfo.presentationTimeUs = mMediaExtractor.getSampleTime();
                        bufferInfo.flags = mMediaExtractor.getSampleFlags();

                        mMediaExtractor.advance();

                        if (bufferInfo.presentationTimeUs >= mRemoveStartPosMs * 1000 &&
                                bufferInfo.presentationTimeUs <= (mRemoveStartPosMs + mRemoveDurationMs) * 1000) {
                            Log.d(TAG, "removeBgm, we are in remove section, " +
                                    "so skip writing audio. presentationUs: " + bufferInfo.presentationTimeUs);
                            mMediaMuxer.writeSampleData(outAudioTrackId, emptyBuffer, bufferInfo);
                            byteBuffer.clear();
                            continue;
                        }
                        mMediaMuxer.writeSampleData(outAudioTrackId, byteBuffer, bufferInfo);
                    } else {
                        Log.d(TAG, "removeBgm, read audio done!");
                        break;
                    }
                }
            }

            Log.d(TAG, "removeBgm done!!");
        }

        private void release() {
            if (mMediaExtractor != null) {
                mMediaExtractor.release();
            }

            if (mMediaMuxer != null) {
                if (isMuxerStarted) {
                    mMediaMuxer.stop();
                }
                mMediaMuxer.release();
            }
        }
    }
}
