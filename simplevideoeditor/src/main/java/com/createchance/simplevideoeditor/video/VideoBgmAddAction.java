package com.createchance.simplevideoeditor.video;

import android.annotation.TargetApi;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.os.Build;
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
    private long mVideoStartPosMs;
    private long mVideoDurationMs;
    private long mBgmStartPosMs;
    private long mBgmDurationMs;
    private boolean mOverride = true;

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

        if (mVideoDurationMs < 0) {
            return false;
        }

        if (mVideoStartPosMs < 0) {
            return false;
        }

        if (mBgmStartPosMs < 0) {
            return false;
        }

        if (mBgmDurationMs < 0) {
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

    public long getVideoStartPosMs() {
        return mVideoStartPosMs;
    }

    public long getVideoDurationMs() {
        return mVideoDurationMs;
    }

    public long getBgmStartPosMs() {
        return mBgmStartPosMs;
    }

    public long getBgmDurationMs() {
        return mBgmDurationMs;
    }

    public boolean isOverride() {
        return mOverride;
    }

    @Override
    public void start(ActionCallback actionCallback) {
        super.start(actionCallback);

        if (checkRational()) {
            try {
                String bgmMime = getBgmMime();
                switch (bgmMime) {
                    case MediaFormat.MIMETYPE_AUDIO_AAC:
                        addAacBgm();
                        break;
                    case MediaFormat.MIMETYPE_AUDIO_MPEG:
                        addMpegBgm();
                        break;
                    default:
                        break;
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            Log.e(TAG, "Add bgm start failed, params error.");
        }
    }

    private void addAacBgm() {
        AacBgmAddWorker aacBgmAddWorker = new AacBgmAddWorker();
        ActionRunner.addTaskToBackground(aacBgmAddWorker);
    }

    private void addMpegBgm() {
        // not impl
    }

    private String getBgmMime() throws IOException {
        String mine = null;

        MediaExtractor extractor = new MediaExtractor();
        extractor.setDataSource(mBgmFile.getAbsolutePath());
        for (int i = 0; i < extractor.getTrackCount(); i++) {
            MediaFormat mediaFormat = extractor.getTrackFormat(i);
            if (mediaFormat.getString(MediaFormat.KEY_MIME).startsWith("audio")) {
                mine = mediaFormat.getString(MediaFormat.KEY_MIME);
                break;
            }
        }

        return mine;
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

        public Builder override(boolean override) {
            bgmAddAction.mOverride = override;

            return this;
        }

        public Builder videoFrom(long fromMs) {
            bgmAddAction.mVideoStartPosMs = fromMs;

            return this;
        }

        public Builder videoDuration(long durationMs) {
            bgmAddAction.mVideoDurationMs = durationMs;

            return this;
        }

        public Builder bgmFrom(long fromMs) {
            bgmAddAction.mBgmStartPosMs = fromMs;

            return this;
        }

        public Builder bgmDuration(long durationMs) {
            bgmAddAction.mBgmDurationMs = durationMs;

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

    private class AacBgmAddWorker implements Runnable {
        MediaMuxer mediaMuxer;
        MediaExtractor sourceExtractor;
        MediaExtractor bgmAudioExtractor;
        MediaCodec audioEncoder;
        MediaCodec audioDecoder;
        int outVideoTrackId = -1;
        int outAudioTrackId = -1;
        int inVideoTrackId = -1;
        int inAudioTrackId = -1;
        int inBgmTrackId = -1;
        ByteBuffer byteBuffer = ByteBuffer.allocate(512 * 1024);
        int mixedSize = -1;
        MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
        long outDuration;
        long bgmFrameTime;
        boolean reachBgmEnd = false;

        @Override
        public void run() {
            try {
                prepare();
                addBgm();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (IllegalArgumentException e) {
                e.printStackTrace();
            } finally {
                release();
            }

        }

        private void prepare() throws IOException {
            mediaMuxer = new MediaMuxer(mOutFile.getAbsolutePath(),
                    MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);

            sourceExtractor = new MediaExtractor();
            sourceExtractor.setDataSource(mInputFile.getAbsolutePath());
            for (int i = 0; i < sourceExtractor.getTrackCount(); i++) {
                MediaFormat mediaFormat = sourceExtractor.getTrackFormat(i);
                String mime = mediaFormat.getString(MediaFormat.KEY_MIME);
                if (mime.startsWith("video")) {
                    inVideoTrackId = i;
                    outVideoTrackId = mediaMuxer.addTrack(mediaFormat);
                    outDuration = mediaFormat.getLong(MediaFormat.KEY_DURATION);
                } else if (mime.startsWith("audio")) {
                    inAudioTrackId = i;
                    outAudioTrackId = mediaMuxer.addTrack(mediaFormat);
                    // create decoder and encoder by audio track of video file.
                    if (!mOverride) {
                        Log.d(TAG, "111111111111111111111111111111111");
                        audioDecoder = MediaCodec.createDecoderByType(mime);
                        audioEncoder = MediaCodec.createEncoderByType(mime);
                        audioDecoder.configure(mediaFormat, null, null, 0);
                        Log.d(TAG, "222222222222222222222222222222222");
                        MediaFormat format = MediaFormat.createAudioFormat(mime,
                                mediaFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE),
                                mediaFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT));
                        format.setInteger(MediaFormat.KEY_BIT_RATE, 96000);
//                        format.setInteger(MediaFormat.KEY_PCM_ENCODING, mediaFormat.getInteger(MediaFormat.KEY_PCM_ENCODING));
                        format.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 20 * 1024);
                        format.setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC);
                        audioEncoder.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
                        // start codec
                        audioDecoder.start();
                        audioEncoder.start();
                    }
                }
            }

            bgmAudioExtractor = new MediaExtractor();
            bgmAudioExtractor.setDataSource(mBgmFile.getAbsolutePath());
            for (int i = 0; i < bgmAudioExtractor.getTrackCount(); i++) {
                MediaFormat mediaFormat = bgmAudioExtractor.getTrackFormat(i);
                if (mediaFormat.getString(MediaFormat.KEY_MIME).startsWith("audio")) {
                    inBgmTrackId = i;
                    bgmAudioExtractor.selectTrack(i);
                    break;
                }
            }

            if (inBgmTrackId == -1) {
                throw new IllegalArgumentException("We do not find any audio track in bgm: " + mBgmFile);
            } else {
                mediaMuxer.start();
            }
        }

        private void addBgm() {
            // handle video first
            if (inVideoTrackId != -1) {
                sourceExtractor.selectTrack(inVideoTrackId);
                while (true) {
                    int videoSampleSize = sourceExtractor.readSampleData(byteBuffer, 0);
                    if (videoSampleSize < 0) {
                        Log.d(TAG, "addBgm, read video done.");
                        break;
                    }
                    bufferInfo.size = videoSampleSize;
                    bufferInfo.flags = sourceExtractor.getSampleFlags();
                    bufferInfo.offset = 0;
                    bufferInfo.presentationTimeUs = sourceExtractor.getSampleTime();
                    mediaMuxer.writeSampleData(outVideoTrackId, byteBuffer, bufferInfo);

                    Log.d(TAG, "addBgm, presentation: " + bufferInfo.presentationTimeUs);

                    // next frame
                    sourceExtractor.advance();
                }
            }

            // get frame time from bgm.
            bgmFrameTime = getSampleTime(bgmAudioExtractor, inBgmTrackId);

            // handle audio then
            sourceExtractor.unselectTrack(inVideoTrackId);
            sourceExtractor.selectTrack(inAudioTrackId);
            bgmAudioExtractor.selectTrack(inBgmTrackId);
            bgmAudioExtractor.seekTo(mBgmStartPosMs * 1000, MediaExtractor.SEEK_TO_PREVIOUS_SYNC);
            bufferInfo.presentationTimeUs = 0;
            int sourceSampleSize = -1, bgmSampleSize = -1;
            while (true) {
                sourceSampleSize = sourceExtractor.readSampleData(byteBuffer, 0);
                if (sourceSampleSize < 0) {
                    Log.d(TAG, "addBgm, reach source audio eos.");
                    break;
                }
                bufferInfo.size = sourceSampleSize;
                bufferInfo.flags = sourceExtractor.getSampleFlags();
                bufferInfo.offset = 0;
                bufferInfo.presentationTimeUs = sourceExtractor.getSampleTime();
                // next frame
                sourceExtractor.advance();

                if (bufferInfo.presentationTimeUs >= mVideoStartPosMs * 1000 &&
                        bufferInfo.presentationTimeUs <= (mVideoStartPosMs + mVideoDurationMs) * 1000) {
                    if (mOverride) {
                        if (!reachBgmEnd) {
                            // read bgm audio data.
                            bgmSampleSize = bgmAudioExtractor.readSampleData(byteBuffer, 0);
                            if (bgmSampleSize < 0) {
                                Log.d(TAG, "addBgm, reach bgm eos");
                                reachBgmEnd = true;
                            } else {
                                bufferInfo.size = bgmSampleSize;
                                bufferInfo.flags = bgmAudioExtractor.getSampleFlags();
                                bufferInfo.offset = 0;
                                Log.d(TAG, "addBgm, writing audio presentation: " + bufferInfo.presentationTimeUs);

                                // next frame
                                bgmAudioExtractor.advance();
                            }
                        }
                    } else {
                        if (!reachBgmEnd) {
                            // mix two audio, pcm.....
                            if (Build.VERSION.SDK_INT >= 21) {
                                mixAudioAfter21();
                            } else {
                                mixAudioBefore21();
                            }
                            bufferInfo.size = mixedSize;
                        }
                    }
                }
                Log.d(TAG, "addBgm, write audio data.");
                mediaMuxer.writeSampleData(outAudioTrackId, byteBuffer, bufferInfo);
            }

            Log.d(TAG, "addBgm done!");
        }

        private void release() {
            if (mediaMuxer != null) {
                mediaMuxer.stop();
                mediaMuxer.release();
            }

            if (sourceExtractor != null) {
                sourceExtractor.release();
            }

            if (bgmAudioExtractor != null) {
                bgmAudioExtractor.release();
            }
        }

        private long getSampleTime(MediaExtractor mediaExtractor, int track) {
            mediaExtractor.selectTrack(track);
            mediaExtractor.readSampleData(byteBuffer, 0);
            //skip first I frame
            if (mediaExtractor.getSampleFlags() == MediaExtractor.SAMPLE_FLAG_SYNC) {
                mediaExtractor.advance();
            }
            mediaExtractor.readSampleData(byteBuffer, 0);
            long firstVideoPTS = mediaExtractor.getSampleTime();
            mediaExtractor.advance();
            mediaExtractor.readSampleData(byteBuffer, 0);
            long secondVideoPTS = mediaExtractor.getSampleTime();
            mediaExtractor.unselectTrack(track);
            return Math.abs(secondVideoPTS - firstVideoPTS);
        }

        /**
         * mix video audio track and bgm audio track into source buffer.
         */
        @TargetApi(20)
        private void mixAudioBefore21() {
            if (reachBgmEnd) {
                return;
            }
        }

        /**
         * mix video audio track and bgm audio track into source buffer.
         */
        @TargetApi(21)
        private void mixAudioAfter21() {
            /**
             * First we read video audio data and bgm audio data
             */
            int sourceInputId = audioDecoder.dequeueInputBuffer(-1);
            if (sourceInputId >= 0) {
                ByteBuffer buffer = audioDecoder.getInputBuffer(sourceInputId);
                int sampleSize = sourceExtractor.readSampleData(buffer, 0);
                if (sampleSize < 0) {
                    audioDecoder.queueInputBuffer(
                            sourceInputId,
                            0,
                            0,
                            0,
                            MediaCodec.BUFFER_FLAG_END_OF_STREAM
                    );
                } else {
                    audioDecoder.queueInputBuffer(
                            sourceInputId,
                            0,
                            sampleSize,
                            sourceExtractor.getSampleTime(),
                            0

                    );
                }
                sourceExtractor.advance();
            }

            int bgmInputId = audioDecoder.dequeueInputBuffer(-1);
            if (bgmInputId >= 0) {
                ByteBuffer buffer = audioDecoder.getInputBuffer(bgmInputId);
                int sampleSize = bgmAudioExtractor.readSampleData(buffer, 0);
                if (sampleSize < 0) {
                    audioDecoder.queueInputBuffer(
                            bgmInputId,
                            0,
                            0,
                            0,
                            MediaCodec.BUFFER_FLAG_END_OF_STREAM
                    );
                    reachBgmEnd = true;
                } else {
                    audioDecoder.queueInputBuffer(
                            bgmInputId,
                            0,
                            sampleSize,
                            bgmAudioExtractor.getSampleTime(),
                            0
                    );
                }
                bgmAudioExtractor.advance();
            }

            /**
             * Then we get decoded pcm data and mix it.
             */
            MediaCodec.BufferInfo sourceBufferInfo = new MediaCodec.BufferInfo();
            MediaCodec.BufferInfo bgmBufferInfo = new MediaCodec.BufferInfo();
            ByteBuffer sourceBuffer = null, bgmBuffer = null;
            int sourceOutputId = audioDecoder.dequeueOutputBuffer(sourceBufferInfo, -1);
            if (sourceOutputId >= 0) {
                sourceBuffer = audioDecoder.getOutputBuffer(sourceOutputId);
            }
            int bgmOutputId = audioDecoder.dequeueOutputBuffer(bgmBufferInfo, -1);
            if (bgmOutputId >= 0) {
                bgmBuffer = audioDecoder.getOutputBuffer(bgmInputId);
            }

            if (sourceBuffer != null && bgmBuffer != null) {
                doMix(sourceBuffer, bgmBuffer);
            }
        }

        private void doMix(ByteBuffer firstAudio, ByteBuffer secondAudio) {
            short firstVal = -1;
            short secondVal = -1;
            for (int i = 0; i < firstAudio.capacity() - 1; i++) {
                firstVal = (short) (firstAudio.get(i) & 0xff | (firstAudio.get(i + 1) & 0xff) << 8);
            }
            for (int i = 0; i < secondAudio.capacity(); i++) {
                secondVal = (short) (secondAudio.get(i) & 0xff | (secondAudio.get(i + 1) & 0xff) << 8);
            }

            if (firstVal != -1 && secondVal != -1) {
                byteBuffer.put(mixedSize++, (byte) ((firstVal + secondVal) & 0x00ff));
                byteBuffer.put(mixedSize++, (byte) (((firstVal + secondVal) & 0xff00) >> 8));
            }
        }
    }
}
