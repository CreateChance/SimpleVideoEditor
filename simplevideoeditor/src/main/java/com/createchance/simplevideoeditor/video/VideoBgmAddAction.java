package com.createchance.simplevideoeditor.video;

import android.annotation.TargetApi;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.os.Build;
import android.text.TextUtils;
import android.util.Log;

import com.createchance.simplevideoeditor.AbstractAction;
import com.createchance.simplevideoeditor.ActionCallback;
import com.createchance.simplevideoeditor.ActionRunner;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;

/**
 * ${DESC}
 *
 * @author gaochao1-iri
 * @date 25/03/2018
 */

public class VideoBgmAddAction extends AbstractAction {

    private static final String TAG = "VideoBgmAddAction";

    public static final int EVENT_DECODE_STARTED = 100;
    public static final int EVNET_DECODE_GOING = 101;
    public static final int EVNET_DECODE_DONE = 102;
    public static final int EVENT_DECODE_FAILED = 103;

    public static final int EVENT_ENCODE_STARTED = 200;
    public static final int EVENT_ENCODE_GOING = 201;
    public static final int EVENT_ENCODE_DONE = 202;
    public static final int EVENT_ENCODE_FAILED = 203;

    private File mInputFile;
    private File mOutFile;
    private File mBgmFile;
    private long mVideoFromPos;
    private long mVideoToPos;
    private long mAudioFromPos;
    private long mAudioToPos;

    private Encoder mEncoder;
    private Decoder mDecoder;
    private BlockingQueue<byte[]> mBufferQueue = new LinkedBlockingDeque<>();

    private VideoBgmAddAction() {

    }

    private boolean checkRational() {
        if (mInputFile == null) {
            return false;
        }

        if (mOutFile == null) {
            return false;
        }

        if (mBgmFile == null) {
            return false;
        }

        return true;
    }

    public File getInputFile() {
        return mInputFile;
    }

    public File getOutFile() {
        return mOutFile;
    }

    public File getBgmFile() {
        return mBgmFile;
    }

    public long getVideoFromPos() {
        return mVideoFromPos;
    }

    public long getVideoToPos() {
        return mVideoToPos;
    }

    public long getAudioFromPos() {
        return mAudioFromPos;
    }

    public long getAudioToPos() {
        return mAudioToPos;
    }

    @Override
    public void start(ActionCallback actionCallback) {
        super.start(actionCallback);

        if (checkRational()) {
            mDecoder = new Decoder();
            mDecoder.start();
        } else {
            Log.e(TAG, "Add bgm start failed, params error.");
        }
    }

    public static class Builder {
        private VideoBgmAddAction bgmAddAction = new VideoBgmAddAction();

        public Builder edit(File video) {
            bgmAddAction.mInputFile = video;

            return this;
        }

        public Builder withBgm(File bgmFile) {
            bgmAddAction.mBgmFile = bgmFile;

            return this;
        }

        public Builder fromBgm(long fromMs) {
            bgmAddAction.mAudioFromPos = fromMs;

            return this;
        }

        public Builder toBgm(long toMs) {
            bgmAddAction.mAudioToPos = toMs;

            return this;
        }

        public Builder fromVideo(long fromMs) {
            bgmAddAction.mVideoFromPos = fromMs;

            return this;
        }

        public Builder toVideo(long toMs) {
            bgmAddAction.mVideoToPos = toMs;

            return this;
        }

        public Builder saveAs(File output) {
            bgmAddAction.mOutFile = output;

            return this;
        }

        public VideoBgmAddAction build() {
            return bgmAddAction;
        }
    }

    private class Decoder {
        MediaExtractor mediaExtractor;
        MediaCodec mediaCodec;
        DecoderInputWorker mInputWorker;
        DecoderOutputWorker mOutputWorker;

        public void start() {
            try {
                if (mCallback != null) {
                    mCallback.onStarted(EVENT_DECODE_STARTED);
                }

                prepare();

                mInputWorker = new DecoderInputWorker();
                mOutputWorker = new DecoderOutputWorker();

                ActionRunner.addTaskToBackground(mInputWorker);
                ActionRunner.addTaskToBackground(mOutputWorker);
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                release();
            }
        }

        private void prepare() throws IOException {
            mediaExtractor = new MediaExtractor();
            mediaExtractor.setDataSource(mInputFile.getPath());

            int trackCount = mediaExtractor.getTrackCount();
            for (int i = 0; i < trackCount; i++) {
                MediaFormat mediaFormat = mediaExtractor.getTrackFormat(i);
                String mine = mediaFormat.getString(MediaFormat.KEY_MIME);
                if (!TextUtils.isEmpty(mine) && mine.startsWith("audio")) {
                    mediaExtractor.selectTrack(i);
                    mediaCodec = MediaCodec.createDecoderByType(mine);
                    mediaCodec.configure(mediaFormat, null, null, 0);
                    mediaCodec.start();
                    break;
                }
            }
        }

        private void release() {
            if (mediaExtractor != null) {
                mediaExtractor.release();
            }

            if (mediaCodec != null) {
                mediaCodec.stop();
                mediaCodec.release();
            }
        }

        private class DecoderInputWorker implements Runnable {
            @Override
            public void run() {
                if (Build.VERSION.SDK_INT < 21) {
                    decodeInputBefore21();
                } else {
                    decodeInputAfter21();
                }
            }

            @TargetApi(20)
            private void decodeInputBefore21() {
                ByteBuffer[] inputBuffers = mediaCodec.getInputBuffers();
                while (true) {
                    int inputBufferId = mediaCodec.dequeueInputBuffer(-1);
                    if (inputBufferId >= 0) {
                        // fill buffer with data
                        ByteBuffer buffer = inputBuffers[inputBufferId];
                        int sampleSize = mediaExtractor.readSampleData(buffer, 0);
                        long sampleTime = mediaExtractor.getSampleTime();

                        if (sampleSize / 1000 > mVideoToPos) {
                            mediaCodec.queueInputBuffer(
                                    inputBufferId,
                                    0,
                                    0,
                                    0,
                                    MediaCodec.BUFFER_FLAG_END_OF_STREAM
                            );
                            break;
                        } else {
                            mediaCodec.queueInputBuffer(
                                    inputBufferId,
                                    0,
                                    sampleSize,
                                    sampleTime,
                                    0
                            );
                            mediaExtractor.advance();
                        }
                    }
                }
            }

            @TargetApi(21)
            private void decodeInputAfter21() {
                while (true) {
                    int inputBufferId = mediaCodec.dequeueInputBuffer(-1);
                    if (inputBufferId >= 0) {
                        ByteBuffer buffer = mediaCodec.getInputBuffer(inputBufferId);
                        // fill buffer with data.
                        int sampleSize = mediaExtractor.readSampleData(buffer, 0);
                        long sampleTime = mediaExtractor.getSampleTime();

                        if (sampleSize / 1000 > mVideoToPos) {
                            mediaCodec.queueInputBuffer(
                                    inputBufferId,
                                    0,
                                    0,
                                    0,
                                    MediaCodec.BUFFER_FLAG_END_OF_STREAM
                            );
                            break;
                        } else {
                            mediaCodec.queueInputBuffer(
                                    inputBufferId,
                                    0,
                                    sampleSize,
                                    sampleTime,
                                    0
                            );
                            mediaExtractor.advance();
                        }
                    }
                }
            }
        }

        private class DecoderOutputWorker implements Runnable {
            MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();

            @Override
            public void run() {
                try {
                    if (Build.VERSION.SDK_INT < 21) {
                        decodeOutputBefore21();
                    } else {
                        decodeOutputAfter21();
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            @TargetApi(20)
            private void decodeOutputBefore21() throws InterruptedException {
                ByteBuffer[] outputBuffers = mediaCodec.getOutputBuffers();
                while (true) {
                    int outputBufferId = mediaCodec.dequeueOutputBuffer(bufferInfo, -1);
                    Log.d(TAG, "decodeOutputBefore21, output buffer id: " + outputBufferId);
                    switch (outputBufferId) {
                        case MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED:
                            outputBuffers = mediaCodec.getOutputBuffers();
                            break;
                        case MediaCodec.INFO_OUTPUT_FORMAT_CHANGED:
                            // start encoder now.
                            Log.d(TAG, "decodeOutputBefore21, start encoder now.");
                            MediaFormat mediaFormat = mediaCodec.getOutputFormat();
                            mEncoder = new Encoder(mediaFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE),
                                    mediaFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT));
                            mEncoder.start();
                            break;
                        default:
                            if (outputBufferId >= 0) {
                                ByteBuffer buffer = outputBuffers[outputBufferId];
                                // put this data to queue.
                                mBufferQueue.put(buffer.array());
                            }
                            break;
                    }

                    if ((bufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                        Log.d(TAG, "decodeOutputBefore21 reach eos!");
                        mBufferQueue.put(null);
                        break;
                    }
                }
            }

            @TargetApi(21)
            private void decodeOutputAfter21() throws InterruptedException {

                while (true) {
                    int outputBufferId = mediaCodec.dequeueOutputBuffer(bufferInfo, -1);
                    Log.d(TAG, "decodeOutputAfter21, output buffer id: " + outputBufferId);
                    switch (outputBufferId) {
                        case MediaCodec.INFO_OUTPUT_FORMAT_CHANGED:
                            // start encoder now.
                            Log.d(TAG, "decodeOutputBefore21, start encoder now.");
                            MediaFormat mediaFormat = mediaCodec.getOutputFormat();
                            mEncoder = new Encoder(mediaFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE),
                                    mediaFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT));
                            mEncoder.start();
                            break;
                        default:
                            if (outputBufferId >= 0) {
                                ByteBuffer buffer = mediaCodec.getOutputBuffer(outputBufferId);
                                // put this data to queue.
                                mBufferQueue.put(buffer.array());
                            }
                            break;
                    }

                    if ((bufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                        Log.d(TAG, "decodeOutputAfter21 reach eos!");
                        mBufferQueue.put(null);
                        break;
                    }
                }
            }
        }

    }

    private class Encoder {
        private MediaCodec mediaCodec;
        private MediaMuxer mediaMuxer;
        private EncoderInputWorker mInputWorker;
        private EncoderOutputWorker mOutputWorker;

        private int audioSampleRate;
        private int audioChannelCount;
        private int videoAudioTrack;

        public Encoder(int sampleRate, int channelCount) {
            this.audioSampleRate = sampleRate;
            this.audioChannelCount = channelCount;
        }

        public void start() {
            try {
                prepare();

                mInputWorker = new EncoderInputWorker();
                mOutputWorker = new EncoderOutputWorker();

                ActionRunner.addTaskToBackground(mInputWorker);
                ActionRunner.addTaskToBackground(mOutputWorker);
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                release();
            }
        }

        private void prepare() throws IOException {
            mediaMuxer = new MediaMuxer(mOutFile.getAbsolutePath(),
                    MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
            MediaExtractor extractor = new MediaExtractor();
            extractor.setDataSource(mInputFile.getAbsolutePath());
            for (int i = 0; i < extractor.getTrackCount(); i++) {
                MediaFormat mediaFormat = extractor.getTrackFormat(i);
                String mime = mediaFormat.getString(MediaFormat.KEY_MIME);
                if (mime.startsWith("audio")) {
                    videoAudioTrack = i;
                    break;
                }
            }

            mediaCodec = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_AUDIO_AAC);
            MediaFormat format = MediaFormat.createAudioFormat(
                    MediaFormat.MIMETYPE_AUDIO_AAC,
                    audioSampleRate, audioChannelCount);
            format.setInteger(MediaFormat.KEY_BIT_RATE, 96000);
            format.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 20 * 1024);
            format.setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC);
            mediaCodec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
            mediaCodec.start();
            mediaMuxer.start();
        }

        private void release() {
            if (mediaCodec != null) {
                mediaCodec.stop();
                mediaCodec.release();
            }

            if (mediaMuxer != null) {
                mediaMuxer.stop();
                mediaMuxer.release();
            }
        }

        private class EncoderInputWorker implements Runnable {

            @Override
            public void run() {
                try {
                    if (Build.VERSION.SDK_INT < 21) {
                        encodeInputBefore21();
                    } else {
                        encodeInputAfter21();
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            @TargetApi(20)
            private void encodeInputBefore21() throws InterruptedException {
                ByteBuffer[] inputBuffers = mediaCodec.getInputBuffers();
                while (true) {
                    byte[] pcmData = mBufferQueue.take();
                    int inputBufferId = mediaCodec.dequeueInputBuffer(-1);
                    switch (inputBufferId) {
                        case MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED:
                            inputBuffers = mediaCodec.getInputBuffers();
                            break;
                        default:
                            if (pcmData != null) {
                                ByteBuffer buffer = inputBuffers[inputBufferId];
                                buffer.clear();
                                buffer.put(pcmData);
                                mediaCodec.queueInputBuffer(
                                        inputBufferId, 0, pcmData.length, System.nanoTime(), 0);
                            } else {
                                mediaCodec.queueInputBuffer(
                                        inputBufferId, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                            }
                            break;
                    }

                    if (pcmData == null) {
                        break;
                    }
                }
            }

            @TargetApi(21)
            private void encodeInputAfter21() throws InterruptedException {
                while (true) {
                    byte[] pcmData = mBufferQueue.take();
                    int inputBufferId = mediaCodec.dequeueInputBuffer(-1);
                    if (pcmData != null) {
                        ByteBuffer buffer = mediaCodec.getOutputBuffer(inputBufferId);
                        buffer.clear();
                        buffer.put(pcmData);
                        mediaCodec.queueInputBuffer(
                                inputBufferId, 0, pcmData.length, System.nanoTime(), 0);
                    } else {
                        mediaCodec.queueInputBuffer(
                                inputBufferId, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                        break;
                    }
                }
            }
        }

        private class EncoderOutputWorker implements Runnable {
            MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();

            @Override
            public void run() {
                if (Build.VERSION.SDK_INT < 21) {
                    encodeOutputBefore21();
                } else {
                    encodeOutputAfter21();
                }
            }

            @TargetApi(20)
            private void encodeOutputBefore21() {
                ByteBuffer[] outputBuffers = mediaCodec.getOutputBuffers();
                int outputBufferId = mediaCodec.dequeueOutputBuffer(bufferInfo, -1);
                if (outputBufferId >= 0) {
                    ByteBuffer buffer = outputBuffers[outputBufferId];
                    int aacDataLen = bufferInfo.size + 7;
                    byte[] aacData = new byte[aacDataLen];
                    addADTStoPacket(aacData, aacDataLen);
                    buffer.get(aacData, 7, bufferInfo.size);
//                    mediaCodec.releaseOutputBuffer(outputBufferId, false);

                    // write data to result mp4 video.
                    mediaMuxer.writeSampleData(videoAudioTrack, ByteBuffer.wrap(aacData), bufferInfo);
                }
            }

            @TargetApi(21)
            private void encodeOutputAfter21() {
                int outputBufferId = mediaCodec.dequeueOutputBuffer(bufferInfo, -1);
                if (outputBufferId >= 0) {
                    ByteBuffer buffer = mediaCodec.getOutputBuffer(outputBufferId);
                    int aacDataLen = bufferInfo.size + 7;
                    byte[] aacData = new byte[aacDataLen];
                    addADTStoPacket(aacData, aacDataLen);
                    buffer.get(aacData, 7, bufferInfo.size);
//                    mediaCodec.releaseOutputBuffer(outputBufferId, false);

                    // write data to result mp4 video.
                    mediaMuxer.writeSampleData(videoAudioTrack, ByteBuffer.wrap(aacData), bufferInfo);
                }
            }

            /**
             * 给编码出的aac裸流添加adts头字段
             *
             * @param packet    要空出前7个字节，否则会搞乱数据
             * @param packetLen
             */
            private void addADTStoPacket(byte[] packet, int packetLen) {
                //AAC LC
                int profile = 2;
                //44.1KHz
                int freqIdx = 4;
                //CPE
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
    }
}
