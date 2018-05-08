package com.createchance.simplevideoeditor;

import android.annotation.TargetApi;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.media.MediaMetadataRetriever;
import android.media.MediaMuxer;
import android.os.Build;

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

    private File mBgmFile;
    private long mVideoStartPosMs;
    private long mVideoDurationMs;
    private long mBgmStartPosMs;
    private long mBgmDurationMs;
    private boolean mOverride = true;

    private VideoBgmAddAction() {
        super(Constants.ACTION_ADD_BGM);
    }

    @Override
    protected boolean checkRational() {
        return super.checkRational() &&
                mInputFile != null &&
                mBgmFile != null &&
                mVideoDurationMs >= 0 &&
                mVideoStartPosMs >= 0 &&
                mBgmStartPosMs >= 0 &&
                mBgmDurationMs >= 0;

    }

    @Override
    void makeRational() {
        super.makeRational();

        // get duration of video.
        MediaMetadataRetriever videoRetriever = new MediaMetadataRetriever();
        videoRetriever.setDataSource(mInputFile.getAbsolutePath());
        long videoDuration = Long.valueOf(videoRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION));
        videoRetriever.release();

        // get duration of bgm file.
        MediaMetadataRetriever bgmRetriever = new MediaMetadataRetriever();
        bgmRetriever.setDataSource(mBgmFile.getAbsolutePath());
        long bgmDuration = Long.valueOf(bgmRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION));

        if (mVideoDurationMs == 0) {
            mVideoDurationMs = videoDuration > bgmDuration ? bgmDuration : videoDuration;
        }

        if (mBgmDurationMs == 0) {
            mBgmDurationMs = mVideoDurationMs;
        }
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
    public void start(File inputFile) {
        super.start(inputFile);
        onStarted();
        if (checkRational()) {
            makeRational();
            try {
                String bgmMime = getBgmMime();
                Logger.d(TAG, "bgm mime type: " + bgmMime);
                switch (bgmMime) {
                    case MediaFormat.MIMETYPE_AUDIO_AAC:
                        addAacBgm(mBgmFile, false);
                        break;
                    case MediaFormat.MIMETYPE_AUDIO_MPEG:
                        addMpegBgm();
                        break;
                    default:
                        break;
                }
            } catch (IOException e) {
                e.printStackTrace();
                onFailed();
            }
        } else {
            Logger.e(TAG, "Add bgm start failed, params error.");
            onFailed();
        }
    }

    private void addAacBgm(File bgmfile, boolean deleteAfteruse) {
        AacBgmAddWorker aacBgmAddWorker = new AacBgmAddWorker(bgmfile, deleteAfteruse);
        WorkRunner.addTaskToBackground(aacBgmAddWorker);
    }

    private void addMpegBgm() {
        new AudioTransCoder.Builder()
                .transcode(mBgmFile)
                .from(mBgmStartPosMs)
                .duration(mBgmDurationMs)
                .saveAs(new File(getBaseWorkFolder(), "transcode.aac"))
                .build()
                .start(new AudioTransCoder.Callback() {
                    @Override
                    public void onProgress(float progress) {

                    }

                    @Override
                    public void onSucceed(File output) {
                        Logger.d(TAG, "Audio trans code done, add with aac file: " + output);
                        addAacBgm(output, false);
                    }

                    @Override
                    public void onFailed() {
                        Logger.e(TAG, "Audio trans code failed.");
                        onFailed();
                    }
                });
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

        public Builder bgmFile(File bgmFile) {
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
        ByteBuffer sourceBuffer = ByteBuffer.allocate(512 * 1024);
        ByteBuffer bgmBuffer = ByteBuffer.allocate(512 * 1024);
        int mixedSize = -1;
        MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
        long outDuration;
        boolean reachBgmEnd = false;
        File bgmFile;
        boolean deleteAfterUse;

        AacBgmAddWorker(File bgmFile, boolean deleteAfterUse) {
            this.bgmFile = bgmFile;
            this.deleteAfterUse = deleteAfterUse;
        }

        @Override
        public void run() {
            try {
                prepare();
                addBgm();
                onSucceeded();
            } catch (Exception e) {
                e.printStackTrace();
                onFailed();
            } finally {
                release();
                if (deleteAfterUse) {
                    bgmFile.delete();
                }
            }

        }

        private void prepare() throws IOException {
            mediaMuxer = new MediaMuxer(mOutputFile.getAbsolutePath(),
                    MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);

            sourceExtractor = new MediaExtractor();
            sourceExtractor.setDataSource(mInputFile.getAbsolutePath());
            for (int i = 0; i < sourceExtractor.getTrackCount(); i++) {
                MediaFormat mediaFormat = sourceExtractor.getTrackFormat(i);
                String mime = mediaFormat.getString(MediaFormat.KEY_MIME);
                if (mime.startsWith("video")) {
                    Logger.d(TAG, "Found source video track , track id: " + i);
                    inVideoTrackId = i;
                    outVideoTrackId = mediaMuxer.addTrack(mediaFormat);
                    outDuration = mediaFormat.getLong(MediaFormat.KEY_DURATION);
                } else if (mime.startsWith("audio")) {
                    Logger.d(TAG, "Found source audio track , track id: " + i);
                    inAudioTrackId = i;
                    outAudioTrackId = mediaMuxer.addTrack(mediaFormat);
                    // create decoder and encoder by audio track of video file.
                    if (!mOverride) {
                        audioDecoder = MediaCodec.createDecoderByType(mime);
                        audioEncoder = MediaCodec.createEncoderByType(mime);
                        audioDecoder.configure(mediaFormat, null, null, 0);
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
            bgmAudioExtractor.setDataSource(bgmFile.getAbsolutePath());
            for (int i = 0; i < bgmAudioExtractor.getTrackCount(); i++) {
                MediaFormat mediaFormat = bgmAudioExtractor.getTrackFormat(i);
                if (mediaFormat.getString(MediaFormat.KEY_MIME).startsWith("audio")) {
                    Logger.d(TAG, "Found bgm audio track, track id: " + i);
                    inBgmTrackId = i;
                    break;
                }
            }

            if (inBgmTrackId == -1) {
                throw new IllegalArgumentException("We do not find any audio track in bgm: " + bgmFile);
            } else {
                mediaMuxer.start();
            }
        }

        private void addBgm() {
            // handle video first
            if (inVideoTrackId != -1) {
                Logger.d(TAG, "Write source video track first.");
                sourceExtractor.selectTrack(inVideoTrackId);
                while (true) {
                    int videoSampleSize = sourceExtractor.readSampleData(sourceBuffer, 0);
                    if (videoSampleSize < 0) {
                        Logger.d(TAG, "Reach source video eos.");
                        sourceExtractor.unselectTrack(inVideoTrackId);
                        break;
                    }
                    bufferInfo.size = videoSampleSize;
                    bufferInfo.flags = sourceExtractor.getSampleFlags();
                    bufferInfo.offset = 0;
                    bufferInfo.presentationTimeUs = sourceExtractor.getSampleTime();
                    mediaMuxer.writeSampleData(outVideoTrackId, sourceBuffer, bufferInfo);
                    // next frame
                    sourceExtractor.advance();
                }
            } else {
                Logger.d(TAG, "No video track in input file: " + mInputFile + ", so skip video.");
            }

            // handle audio then
            sourceExtractor.selectTrack(inAudioTrackId);
            bgmAudioExtractor.selectTrack(inBgmTrackId);
            bgmAudioExtractor.seekTo(mBgmStartPosMs * 1000, MediaExtractor.SEEK_TO_PREVIOUS_SYNC);
            bufferInfo.presentationTimeUs = 0;
            int sourceSampleSize, bgmSampleSize;
            sourceBuffer.clear();
            bgmBuffer.clear();
            if (mOverride) {
                Logger.d(TAG, "Overriding source audio data with bgm audio data.");
                while (true) {
                    sourceSampleSize = sourceExtractor.readSampleData(sourceBuffer, 0);
                    if (sourceSampleSize < 0) {
                        Logger.d(TAG, "Reach source audio eos.");
                        sourceExtractor.unselectTrack(inAudioTrackId);
                        break;
                    }
                    bufferInfo.size = sourceSampleSize;
                    bufferInfo.flags = sourceExtractor.getSampleFlags();
                    bufferInfo.offset = 0;
                    bufferInfo.presentationTimeUs = sourceExtractor.getSampleTime();
                    // next frame
                    sourceExtractor.advance();

                    if (!reachBgmEnd &&
                            bufferInfo.presentationTimeUs >= mVideoStartPosMs * 1000 &&
                            bufferInfo.presentationTimeUs <= (mVideoStartPosMs + mVideoDurationMs) * 1000) {
                        // read bgm audio data.
                        bgmSampleSize = bgmAudioExtractor.readSampleData(bgmBuffer, 0);
                        if (bgmSampleSize < 0) {
                            Logger.d(TAG, "Reach bgm audio eos.");
                            bgmAudioExtractor.unselectTrack(inBgmTrackId);
                            reachBgmEnd = true;
                        } else {
                            bufferInfo.size = bgmSampleSize;
                            bufferInfo.flags = bgmAudioExtractor.getSampleFlags();
                            bufferInfo.offset = 0;
                            long sampleTime = mVideoStartPosMs * 1000 + bgmAudioExtractor.getSampleTime();
                            bufferInfo.presentationTimeUs = sampleTime < (mVideoStartPosMs + mVideoDurationMs) * 1000 ?
                                    sampleTime : (mVideoStartPosMs + mVideoDurationMs) * 1000;
                            Logger.v(TAG, "Got bgm audio data, size: " + bufferInfo.size +
                                    ", presentation time: " + bufferInfo.presentationTimeUs);

                            // next frame
                            bgmAudioExtractor.advance();
                            mediaMuxer.writeSampleData(outAudioTrackId, bgmBuffer, bufferInfo);
                        }
                    } else {
                        Logger.v(TAG, "Got source audio data, size: " + bufferInfo.size +
                                ", presentation time: " + bufferInfo.presentationTimeUs);
                        mediaMuxer.writeSampleData(outAudioTrackId, sourceBuffer, bufferInfo);
                    }
                }
            } else {
                Logger.d(TAG, "Mixing source audio data with bgm audio data.");
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

            Logger.d(TAG, "addBgm done!");
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
            mediaExtractor.readSampleData(sourceBuffer, 0);
            //skip first I frame
            if (mediaExtractor.getSampleFlags() == MediaExtractor.SAMPLE_FLAG_SYNC) {
                mediaExtractor.advance();
            }
            mediaExtractor.readSampleData(sourceBuffer, 0);
            long firstVideoPTS = mediaExtractor.getSampleTime();
            mediaExtractor.advance();
            mediaExtractor.readSampleData(sourceBuffer, 0);
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
                sourceBuffer.put(mixedSize++, (byte) ((firstVal + secondVal) & 0x00ff));
                sourceBuffer.put(mixedSize++, (byte) (((firstVal + secondVal) & 0xff00) >> 8));
            }
        }
    }
}
