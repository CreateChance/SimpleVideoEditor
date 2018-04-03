package com.createchance.simplevideoeditor.video;

import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.util.Log;

import com.createchance.simplevideoeditor.AbstractAction;
import com.createchance.simplevideoeditor.ActionCallback;
import com.createchance.simplevideoeditor.ActionRunner;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * ${DESC}
 *
 * @author gaochao1-iri
 * @date 25/03/2018
 */

public class VideoCutAction extends AbstractAction {

    private static final String TAG = "VideoCutAction";

    private File mInputFile;
    private File mOutputFile;
    private long mCutStartPosMs;
    private long mCutDurationMs;

    private MediaExtractor mMediaExtractor;
    private MediaMuxer mMediaMuxer;
    private boolean mMuxerStarted;

    private CutWorker mCutWorker;

    private VideoCutAction() {

    }

    public File getInputFile() {
        return mInputFile;
    }

    public File getOutputFile() {
        return mOutputFile;
    }

    public long getCutStartPosMs() {
        return mCutStartPosMs;
    }

    public long getCutDurationMs() {
        return mCutDurationMs;
    }

    @Override
    public void start(ActionCallback callback) {
        mCutWorker = new CutWorker();

        ActionRunner.addTaskToBackground(mCutWorker);
    }

    public static class Builder {
        private VideoCutAction cutAction = new VideoCutAction();

        public Builder cut(File input) {
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

        public Builder saveAs(File output) {
            cutAction.mOutputFile = output;

            return this;
        }

        public VideoCutAction build() {
            return cutAction;
        }
    }

    private class CutWorker implements Runnable {

        @Override
        public void run() {
            try {
                prepare();

                cut();
            } catch (IOException e) {
                // delete output file.
                mOutputFile.delete();
                e.printStackTrace();
            } finally {
                release();
            }
        }

        private void prepare() throws IOException {
            mMediaExtractor = new MediaExtractor();
            mMediaExtractor.setDataSource(mInputFile.getAbsolutePath());
            //  out put format is mp4
            mMediaMuxer = new MediaMuxer(mOutputFile.getAbsolutePath(),
                    MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
        }

        private void cut() {
            int inputVideoTrackId = -1;
            int inputAudioTrackId = -1;
            int outputVideoTrackId = -1;
            int outputAudioTrackId = -1;

            int inputVideoMaxSize = -1;
            int inputAudioMaxSize = -1;

            long videoSampleTime = 0;
            long audioSampleTime = 0;

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

                    // check if pos is rational, only check video time info.
                    if ((mCutStartPosMs + mCutDurationMs) * 1000 > duration) {
                        Log.e(TAG, "Error! Duration: " + mCutDurationMs
                                + " invalid! Video duration: " + duration);
                        // delete output file.
                        mOutputFile.delete();
                        return;
                    }

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

            mMediaExtractor.readSampleData(byteBuffer, 0);
            //skip first I frame
            if (mMediaExtractor.getSampleFlags() == MediaExtractor.SAMPLE_FLAG_SYNC) {
                mMediaExtractor.advance();
            }
            mMediaExtractor.readSampleData(byteBuffer, 0);
            long firstVideoPTS = mMediaExtractor.getSampleTime();
            mMediaExtractor.advance();
            mMediaExtractor.readSampleData(byteBuffer, 0);
            long secondVideoPTS = mMediaExtractor.getSampleTime();
            videoSampleTime = Math.abs(secondVideoPTS - firstVideoPTS);
            Log.d(TAG, "videoSampleTime is " + videoSampleTime);

            // select the nearest previous key frame to ensure all selected part will be in the result.
            mMediaExtractor.seekTo(mCutStartPosMs * 1000, MediaExtractor.SEEK_TO_PREVIOUS_SYNC);
            bufferInfo.presentationTimeUs = 0;
            while (true) {
                int sampleSize = mMediaExtractor.readSampleData(byteBuffer, 0);
                if (sampleSize < 0) {
                    Log.d(TAG, "Reach video eos.");
                    mMediaExtractor.unselectTrack(inputVideoTrackId);
                    break;
                }

                long presentationTimeUs = mMediaExtractor.getSampleTime();
                if (presentationTimeUs > mCutDurationMs * 1000) {
                    Log.d(TAG, "Reach video duration limitation.");
                    mMediaExtractor.unselectTrack(inputVideoTrackId);
                    break;
                }
                bufferInfo.size = sampleSize;
                bufferInfo.presentationTimeUs += videoSampleTime;
                bufferInfo.offset = 0;
                bufferInfo.flags = mMediaExtractor.getSampleFlags();
                mMediaMuxer.writeSampleData(outputVideoTrackId, byteBuffer, bufferInfo);
                mMediaExtractor.advance();
            }

            // Then handle audio track if input file have one.
            if (inputAudioTrackId != -1) {
                mMediaExtractor.selectTrack(inputAudioTrackId);

                mMediaExtractor.readSampleData(byteBuffer, 0);
                //skip first sample
                if (mMediaExtractor.getSampleTime() == 0) {
                    mMediaExtractor.advance();
                }
                mMediaExtractor.readSampleData(byteBuffer, 0);
                long firstAudioPTS = mMediaExtractor.getSampleTime();
                mMediaExtractor.advance();
                mMediaExtractor.readSampleData(byteBuffer, 0);
                long secondAudioPTS = mMediaExtractor.getSampleTime();
                audioSampleTime = Math.abs(secondAudioPTS - firstAudioPTS);
                Log.d(TAG, "AudioSampleTime is " + audioSampleTime);

                mMediaExtractor.seekTo(mCutStartPosMs * 1000, MediaExtractor.SEEK_TO_PREVIOUS_SYNC);
                bufferInfo.presentationTimeUs = 0;
                while (true) {
                    int sampleSize = mMediaExtractor.readSampleData(byteBuffer, 0);
                    if (sampleSize < 0) {
                        Log.d(TAG, "Reach audio eos.");
                        break;
                    }

                    long presentationTimeUs = mMediaExtractor.getSampleTime();
                    if (presentationTimeUs > mCutDurationMs * 1000) {
                        Log.d(TAG, "Reach audio duration limitation.");
                        break;
                    }
                    bufferInfo.size = sampleSize;
                    bufferInfo.presentationTimeUs += audioSampleTime;
                    bufferInfo.offset = 0;
                    bufferInfo.flags = mMediaExtractor.getSampleFlags();
                    mMediaMuxer.writeSampleData(outputAudioTrackId, byteBuffer, bufferInfo);
                    mMediaExtractor.advance();
                }
            }

            Log.d(TAG, "Cut file :" + mInputFile + " done!");
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
