package com.createchance.simplevideoeditor.audio;

import android.annotation.TargetApi;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.media.MediaPlayer;
import android.os.Build;
import android.text.TextUtils;
import android.util.Log;

import com.createchance.simplevideoeditor.AbstractAction;
import com.createchance.simplevideoeditor.ActionCallback;
import com.createchance.simplevideoeditor.ActionRunner;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * ${DESC}
 *
 * @author gaochao1-iri
 * @date 25/03/2018
 */

public class AudioTransCodeAction extends AbstractAction {

    private static final String TAG = "AudioTransCodeAction";

    private BlockingQueue<RawBuffer> mRawQueue = new LinkedBlockingQueue<>(10);

    @Override
    public void start(ActionCallback callback) {
        super.start(callback);
        if (checkRational()) {
            DecodeWorker decodeWorker = new DecodeWorker();
            ActionRunner.addTaskToBackground(decodeWorker);
        }
    }

    public enum FORMAT {
        NONE,
        MP3,
        AAC
    }

    private File mInputFile;
    private File mOutputFile;
    private FORMAT mTargetFormat = FORMAT.NONE;
    private long mStartPosMs;
    private long mDurationMs;

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

    private boolean checkRational() {
        if (mInputFile == null) {
            return false;
        }

        if (mOutputFile == null) {
            return false;
        }

        if (mTargetFormat == FORMAT.NONE) {
            return false;
        }

        if (mStartPosMs < 0) {
            return false;
        }

        if (mDurationMs < 0) {
            return false;
        }

        return true;
    }

    public static class Builder {
        private AudioTransCodeAction transCodeAction = new AudioTransCodeAction();

        public Builder transCode(File inputFile) {
            transCodeAction.mInputFile = inputFile;

            return this;
        }

        public Builder to(File outputFile) {
            transCodeAction.mOutputFile = outputFile;

            return this;
        }

        public Builder targetFormat(FORMAT format) {
            transCodeAction.mTargetFormat = format;

            return this;
        }

        public Builder from(long fromMs) {
            transCodeAction.mStartPosMs = fromMs;

            return this;
        }

        public Builder duration(long durationMs) {
            transCodeAction.mDurationMs = durationMs;

            return this;
        }

        public AudioTransCodeAction build() {
            return transCodeAction;
        }
    }

    private class DecodeWorker implements Runnable {
        private static final long TIME_OUT = 5000;
        private MediaExtractor extractor;
        private MediaCodec codec;

        @Override
        public void run() {
            if (mCallback != null) {
                mCallback.onStarted();
            }
            try {
                prepare();

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && false) {
                    decode21();
                } else {
                    decode20();
                }
            } catch (Exception e) {
                e.printStackTrace();
                if (mCallback != null) {
                    mCallback.onFailed();
                }
            } finally {
                release();
            }
        }

        private void release() {
            if (extractor != null) {
                extractor.release();
                extractor = null;
            }
            if (codec != null) {
                codec.stop();
                codec.release();
                codec = null;
            }
        }


        private void prepare() throws IOException {
            extractor = new MediaExtractor();
            extractor.setDataSource(mInputFile.getAbsolutePath());
            int numTracks = extractor.getTrackCount();
            for (int i = 0; i < numTracks; i++) {
                MediaFormat format = extractor.getTrackFormat(i);
                String mine = format.getString(MediaFormat.KEY_MIME);
                if (!TextUtils.isEmpty(mine) && mine.startsWith("audio")) {
                    extractor.selectTrack(i);
                    if (mDurationMs == 0) {
                        try {
                            mDurationMs = format.getLong(MediaFormat.KEY_DURATION) / 1000;
                        } catch (Exception e) {
                            e.printStackTrace();
                            MediaPlayer mediaPlayer = new MediaPlayer();
                            mediaPlayer.setDataSource(mInputFile.getAbsolutePath());
                            mediaPlayer.prepare();
                            mDurationMs = mediaPlayer.getDuration();
                            mediaPlayer.release();
                        }
                    }

                    if (mDurationMs == 0) {
                        throw new IllegalStateException("We can not get duration info from input file: " + mInputFile);
                    }

                    codec = MediaCodec.createDecoderByType(mine);
                    codec.configure(format, null, null, 0);
                    codec.start();
                    break;
                }
            }
        }

        @TargetApi(20)
        private void decode20() throws InterruptedException {
            ByteBuffer[] inputBuffers = codec.getInputBuffers();
            ByteBuffer[] outputBuffers = codec.getOutputBuffers();
            MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
            extractor.seekTo(mStartPosMs * 1000, MediaExtractor.SEEK_TO_CLOSEST_SYNC);
            boolean isEOS = false;
            while (true) {
                long timestamp = 0;
                if (!isEOS) {
                    int inIndex = codec.dequeueInputBuffer(TIME_OUT);
                    if (inIndex >= 0) {
                        ByteBuffer buffer = inputBuffers[inIndex];
                        int sampleSize = extractor.readSampleData(buffer, 0);
                        timestamp = extractor.getSampleTime();
                        if (timestamp > (mStartPosMs + mDurationMs) * 1000) {
                            sampleSize = -1;
                        }
                        if (sampleSize <= 0) {
                            codec.queueInputBuffer(
                                    inIndex,
                                    0,
                                    0,
                                    0,
                                    MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                            isEOS = true;
                        } else {
                            codec.queueInputBuffer(
                                    inIndex,
                                    0,
                                    sampleSize,
                                    timestamp,
                                    0);
                            extractor.advance();
                        }
                    }
                }
                int outIndex = codec.dequeueOutputBuffer(info, TIME_OUT);
                switch (outIndex) {
                    case MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED:
                        outputBuffers = codec.getOutputBuffers();
                        break;
                    case MediaCodec.INFO_OUTPUT_FORMAT_CHANGED:
                        MediaFormat mf = codec.getOutputFormat();
                        // start encode worker
                        EncodeWorker encodeTask = new EncodeWorker();
                        int sampleRate = mf.getInteger(MediaFormat.KEY_SAMPLE_RATE);
                        int channelCount = mf.getInteger(MediaFormat.KEY_CHANNEL_COUNT);
                        encodeTask.setAudioParams(sampleRate, channelCount);
                        ActionRunner.addTaskToBackground(encodeTask);
                        break;
                    case MediaCodec.INFO_TRY_AGAIN_LATER:
                        Log.d(TAG, "dequeueOutputBuffer timed out!");
                        break;
                    default:
                        if ((info.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                            mRawQueue.put(new RawBuffer(null, true, timestamp));
                        } else {
                            ByteBuffer buffer = outputBuffers[outIndex];
                            byte[] outData = new byte[info.size];
                            buffer.get(outData, 0, info.size);
                            mRawQueue.put(new RawBuffer(outData, false, timestamp));
                        }
                        codec.releaseOutputBuffer(outIndex, true);
                        break;
                }
                if ((info.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                    Log.d(TAG, "decode20, reach audio eos.");
                    break;
                }
            }

            Log.d(TAG, "decode done!");
        }

        private void decode21() {

        }
    }

    private class EncodeWorker implements Runnable {
        private static final long TIME_OUT = 5000;
        private MediaCodec encoder;
        private OutputStream mOutput;
        private int sampleRate;
        private int channelCount;

        public void setAudioParams(int sampleRate, int channelCount) {
            this.sampleRate = sampleRate;
            this.channelCount = channelCount;
        }

        @Override
        public void run() {
            try {
                prepare();

                encode20();
            } catch (Exception e) {
                e.printStackTrace();
                if (mCallback != null) {
                    mCallback.onFailed();
                }
            } finally {
                release();
            }
        }

        private void prepare() throws IOException {
            encoder = MediaCodec.createEncoderByType("audio/mp4a-latm");
            MediaFormat format = MediaFormat.createAudioFormat("audio/mp4a-latm", sampleRate, channelCount);
            format.setInteger(MediaFormat.KEY_BIT_RATE, 96000);
            format.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 512 * 1024);
            format.setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC);
            encoder.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
            encoder.start();
            mOutput = new DataOutputStream(new FileOutputStream(mOutputFile));
        }

        @TargetApi(20)
        private void encode20() throws IOException, InterruptedException {
            boolean decodeDone = false;
            ByteBuffer[] inputBuffers = encoder.getInputBuffers();
            ByteBuffer[] outputBuffers = encoder.getOutputBuffers();
            MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();

            while (true) {
                if (!decodeDone) {
                    RawBuffer rawBuffer = mRawQueue.take();
                    if (rawBuffer.isLast) {
                        decodeDone = true;
                        int inIndex = encoder.dequeueInputBuffer(TIME_OUT);
                        if (inIndex >= 0) {
                            encoder.queueInputBuffer(
                                    inIndex,
                                    0,
                                    0,
                                    rawBuffer.sampleTime,
                                    MediaCodec.BUFFER_FLAG_END_OF_STREAM
                            );
                        }
                    } else {
                        int inIndex = encoder.dequeueInputBuffer(TIME_OUT);
                        if (inIndex >= 0) {
                            ByteBuffer inputBuffer = inputBuffers[inIndex];
                            inputBuffer.clear();
                            inputBuffer.put(rawBuffer.data);
                            encoder.queueInputBuffer(
                                    inIndex,
                                    0,
                                    rawBuffer.data.length,
                                    rawBuffer.sampleTime,
                                    0);
                        }
                    }
                }

                while (true) {
                    int outIndex = encoder.dequeueOutputBuffer(info, TIME_OUT);
                    if (outIndex >= 0) {
                        if ((info.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                            break;
                        }
                        ByteBuffer outputBuffer = outputBuffers[outIndex];
                        int len = info.size + 7;
                        byte[] outData = new byte[len];
                        addADTStoPacket(outData, len);
                        outputBuffer.get(outData, 7, info.size);
                        encoder.releaseOutputBuffer(outIndex, false);
                        mOutput.write(outData);
                        if (mCallback != null) {
                            float progress = info.presentationTimeUs / ((mStartPosMs + mDurationMs) * 1000.0f);
                            mCallback.onProgress(progress > 1.0f ? 1.0f : progress);
                        }
                    } else {
                        break;
                    }
                }

                if ((info.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                    Log.d(TAG, "encode reach end of stream!");
                    if (mCallback != null) {
                        mCallback.onSuccess();
                    }
                    break;
                }
            }

            Log.d(TAG, "encode done!");
        }

        private void release() {
            if (encoder != null) {
                encoder.stop();
                encoder.release();
                encoder = null;
            }
            if (mOutput != null) {
                try {
                    mOutput.flush();
                    mOutput.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                mOutput = null;
            }
        }

        /**
         * 给编码出的aac裸流添加adts头字段
         *
         * @param packet    要空出前7个字节，否则会搞乱数据
         * @param packetLen
         */
        private void addADTStoPacket(byte[] packet, int packetLen) {
            // AAC LC
            int profile = 2;
            // 44.1KHz
            int freqIdx = 4;
            // CPE
            int chanCfg = 2;
            packet[0] = (byte) 0xFF;
            packet[1] = (byte) 0xF9;
            packet[2] = (byte) (((profile - 1) << 6) + (freqIdx << 2) + (chanCfg >> 2));
            packet[3] = (byte) (((chanCfg & 3) << 6) + (packetLen >> 11));
            packet[4] = (byte) ((packetLen & 0x7FF) >> 3);
            packet[5] = (byte) (((packetLen & 7) << 5) + 0x1F);
            packet[6] = (byte) 0xFC;
        }
    }

    private class RawBuffer {
        byte[] data;
        boolean isLast;
        long sampleTime;

        private RawBuffer(byte[] data, boolean isLast, long sampleTime) {
            this.data = data;
            this.isLast = isLast;
            this.sampleTime = sampleTime;
        }

        @Override
        public String toString() {
            return "RawBuffer{" +
                    "data=" + Arrays.toString(data) +
                    ", isLast=" + isLast +
                    '}';
        }
    }
}
