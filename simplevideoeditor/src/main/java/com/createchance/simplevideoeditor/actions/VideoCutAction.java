package com.createchance.simplevideoeditor.actions;

import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.util.Log;

import com.createchance.simplevideoeditor.Constants;
import com.createchance.simplevideoeditor.Logger;
import com.createchance.simplevideoeditor.VideoUtil;
import com.createchance.simplevideoeditor.WorkRunner;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * ${DESC}
 *
 * @author createchance
 * @date 25/03/2018
 */

public class VideoCutAction extends AbstractAction {

    private static final String TAG = "VideoCutAction";

    private long mCutStartPosMs;
    private long mCutDurationMs;

    private MediaExtractor mMediaExtractor;
    private MediaMuxer mMediaMuxer;
    private boolean mMuxerStarted;

    private CutWorker mCutWorker;

    private VideoCutAction() {
        super(Constants.ACTION_CUT_VIDEO);

    }

    @Override
    public void start() {
        onStarted();

        mCutWorker = new CutWorker();
        WorkRunner.addTaskToBackground(mCutWorker);
    }

    public static class Builder {
        private VideoCutAction cutAction = new VideoCutAction();

        public Builder input(File input) {
            cutAction.mInputFile = input;

            return this;
        }

        public Builder from(long startMs) {
            cutAction.mCutStartPosMs = startMs;

            return this;
        }

        public Builder duration(long durationMs) {
            cutAction.mCutDurationMs = durationMs;

            return this;
        }

        public Builder output(File output) {
            cutAction.mOutputFile = output;

            return this;
        }

        public VideoCutAction build() {
            return cutAction;
        }
    }

    private class CutWorker implements Runnable {
        boolean isFinished;

        @Override
        public void run() {
            try {
                if (checkRational()) {
                    prepare();
                    cut();
                    isFinished = true;
                } else {
                    Logger.e(TAG, "Action params error.");
                    onFailed();
                }
            } catch (IOException e) {
                // delete output file.
                e.printStackTrace();
                onFailed();
            } finally {
                release();
            }

            if (isFinished) {
                onSucceeded();
            }
        }

        private boolean checkRational() {
            if (mInputFile != null &&
                    mInputFile.exists() &&
                    mInputFile.isFile() &&
                    mOutputFile != null &&
                    mCutStartPosMs >= 0 &&
                    mCutDurationMs >= 0) {

                long duration = VideoUtil.getVideoDuration(mInputFile);

                if (mCutDurationMs == 0) {
                    mCutDurationMs = duration - mCutStartPosMs;
                }

                if ((mCutStartPosMs + mCutDurationMs) > duration) {
                    Logger.w(TAG, "Video selected section is out of duration!");
                    mCutDurationMs = duration - mCutStartPosMs;
                }

                if (mOutputFile.exists()) {
                    Logger.w(TAG, "WARNING: Output file: " + mOutputFile
                            + " already exists, we will override it!");
                }

                return true;
            }

            return false;
        }

        private void prepare() throws IOException {
            mMediaExtractor = new MediaExtractor();
            mMediaExtractor.setDataSource(mInputFile.getAbsolutePath());
            //  out put format is mp4
            mMediaMuxer = new MediaMuxer(mOutputFile.getAbsolutePath(),
                    MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
            mMediaMuxer.setOrientationHint(VideoUtil.getVideoRotation(mInputFile));
        }

        private void cut() {
            int inputVideoTrackId = -1;
            int inputAudioTrackId = -1;
            int outputVideoTrackId = -1;
            int outputAudioTrackId = -1;

            int inputVideoMaxSize = -1;
            int inputAudioMaxSize = -1;

            ByteBuffer byteBuffer;
            for (int i = 0; i < mMediaExtractor.getTrackCount(); i++) {
                MediaFormat mediaFormat = mMediaExtractor.getTrackFormat(i);
                String mime = mediaFormat.getString(MediaFormat.KEY_MIME);
                if (mime.startsWith("video")) {
                    inputVideoTrackId = i;
                    inputVideoMaxSize = mediaFormat.getInteger(MediaFormat.KEY_MAX_INPUT_SIZE);
                    long duration = mediaFormat.getLong(MediaFormat.KEY_DURATION);
                    int height = mediaFormat.getInteger(MediaFormat.KEY_HEIGHT);
                    int weight = mediaFormat.getInteger(MediaFormat.KEY_WIDTH);

                    outputVideoTrackId = mMediaMuxer.addTrack(mediaFormat);

                    Log.d(TAG, "Found video track, width: " + weight
                            + ", height: " + height
                            + ", duration: " + duration
                            + ", max input size: " + inputVideoMaxSize);
                } else if (mime.startsWith("audio")) {
                    inputAudioTrackId = i;
                    inputAudioMaxSize = mediaFormat.getInteger(MediaFormat.KEY_MAX_INPUT_SIZE);
                    long duration = mediaFormat.getLong(MediaFormat.KEY_DURATION);
                    int sampleRate = mediaFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE);
                    int channelCount = mediaFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT);

                    outputAudioTrackId = mMediaMuxer.addTrack(mediaFormat);

                    Log.d(TAG, "Found audio track, duration: " + duration
                            + ", sample rate: " + sampleRate
                            + "channel count: " + channelCount
                            + ", max input size: " + inputAudioMaxSize);
                }

                if (inputVideoTrackId != -1 && inputAudioTrackId != -1) {
                    Log.d(TAG, "We have found all the track id, so we are going to cut.");
                    break;
                }
            }

            if (inputVideoTrackId == -1) {
                Log.e(TAG, "We do not found any video in input file: " + mInputFile);
                // delete output file.
                mOutputFile.delete();
                return;
            }

            // use the max input size of video and audio.
            byteBuffer = ByteBuffer.allocate(
                    inputVideoMaxSize > inputAudioMaxSize ? inputVideoMaxSize : inputAudioMaxSize);

            mMediaMuxer.start();
            mMuxerStarted = true;

            // first we handle video part.
            MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
            mMediaExtractor.selectTrack(inputVideoTrackId);
            // select the nearest previous key frame to ensure all selected part will be in the result.
            mMediaExtractor.seekTo(mCutStartPosMs * 1000, MediaExtractor.SEEK_TO_NEXT_SYNC);
            bufferInfo.presentationTimeUs = mMediaExtractor.getSampleTime();
            while (true) {
                onProgress((bufferInfo.presentationTimeUs - mCutStartPosMs * 1000) * 0.5f /
                        (mCutDurationMs * 1000));
                int sampleSize = mMediaExtractor.readSampleData(byteBuffer, 0);
                if (sampleSize < 0) {
                    Log.d(TAG, "Reach video eos.");
                    mMediaExtractor.unselectTrack(inputVideoTrackId);
                    break;
                }

                bufferInfo.size = sampleSize;
                bufferInfo.presentationTimeUs = mMediaExtractor.getSampleTime();
                bufferInfo.offset = 0;
                bufferInfo.flags = mMediaExtractor.getSampleFlags();
                if ((bufferInfo.presentationTimeUs - mCutStartPosMs * 1000) > mCutDurationMs * 1000) {
                    Log.d(TAG, "Reach video duration limitation.");
                    mMediaExtractor.unselectTrack(inputVideoTrackId);
                    break;
                }
                mMediaMuxer.writeSampleData(outputVideoTrackId, byteBuffer, bufferInfo);
                mMediaExtractor.advance();
            }

            // Then handle audio track if input file have one.
            if (inputAudioTrackId != -1) {
                mMediaExtractor.selectTrack(inputAudioTrackId);
                mMediaExtractor.seekTo(mCutStartPosMs * 1000, MediaExtractor.SEEK_TO_NEXT_SYNC);
                bufferInfo.presentationTimeUs = mMediaExtractor.getSampleTime();
                while (true) {
                    onProgress(((bufferInfo.presentationTimeUs - mCutStartPosMs * 1000) * 0.5f /
                            (mCutDurationMs * 1000)) + 0.5f);
                    int sampleSize = mMediaExtractor.readSampleData(byteBuffer, 0);
                    if (sampleSize < 0) {
                        Log.d(TAG, "Reach audio eos.");
                        break;
                    }

                    bufferInfo.size = sampleSize;
                    bufferInfo.presentationTimeUs = mMediaExtractor.getSampleTime();
                    bufferInfo.offset = 0;
                    bufferInfo.flags = mMediaExtractor.getSampleFlags();
                    if ((bufferInfo.presentationTimeUs - mCutStartPosMs * 1000) > mCutDurationMs * 1000) {
                        Log.d(TAG, "Reach audio duration limitation.");
                        break;
                    }
                    mMediaMuxer.writeSampleData(outputAudioTrackId, byteBuffer, bufferInfo);
                    mMediaExtractor.advance();
                }
            }

            Log.d(TAG, "Cut file :" + mInputFile + " done!");

            onProgress(1f);
        }

        private void release() {
            if (mMediaExtractor != null) {
                mMediaExtractor.release();
            }

            if (mMediaMuxer != null) {
                if (mMuxerStarted) {
                    mMediaMuxer.stop();
                }
                mMediaMuxer.release();
            }
        }
    }
}
