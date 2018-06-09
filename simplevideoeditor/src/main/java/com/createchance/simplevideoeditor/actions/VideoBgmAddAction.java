package com.createchance.simplevideoeditor.actions;

import android.annotation.TargetApi;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.os.Build;

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
    }

    private void addAacBgm(File bgmFile, boolean deleteAfterUse) {
        AacBgmAddWorker aacBgmAddWorker = new AacBgmAddWorker(bgmFile, deleteAfterUse);
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
                        VideoBgmAddAction.this.onProgress(progress * 0.5f);
                    }

                    @Override
                    public void onSucceed(File output) {
                        Logger.d(TAG, "Audio trans code done, add with aac file: " + output);
                        addAacBgm(output, true);
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
                if (checkRational()) {
                    prepare();
                    addBgm();
                } else {
                    Logger.e(TAG, "Action params error.");
                    onFailed();
                }
            } catch (Exception e) {
                e.printStackTrace();
                release();
                onFailed();
                return;
            }

            release();
            if (deleteAfterUse) {
                bgmFile.delete();
            }
            onSucceeded();
        }

        private boolean checkRational() {
            if (mInputFile != null &&
                    mInputFile.exists() &&
                    mInputFile.isFile() &&
                    mOutputFile != null &&
                    mBgmFile != null &&
                    mBgmFile.exists() &&
                    mBgmFile.isFile() &&
                    mVideoDurationMs >= 0 &&
                    mVideoStartPosMs >= 0 &&
                    mBgmStartPosMs >= 0 &&
                    mBgmDurationMs >= 0) {

                // get duration of video.
                long videoDuration = VideoUtil.getVideoDuration(mInputFile);

                // get duration of bgm file.
                long bgmDuration = VideoUtil.getVideoDuration(mBgmFile);

                if (mVideoStartPosMs > videoDuration || mBgmStartPosMs > bgmDuration) {
                    Logger.e(TAG, "Start position is out of duration!");
                    return false;
                }

                if ((mVideoStartPosMs + mVideoDurationMs) > videoDuration) {
                    Logger.w(TAG, "Video selected section is out of duration!");
                    // adjust it.
                    mVideoDurationMs = videoDuration - mVideoStartPosMs;
                }

                if ((mBgmStartPosMs + mBgmDurationMs) > bgmDuration) {
                    Logger.e(TAG, "Bgm selected section is out of duration!");
                    // adjust it
                    mBgmDurationMs = bgmDuration - mBgmStartPosMs;
                }

                long remainVideoDuration = videoDuration - mVideoStartPosMs;
                long remainBgmDuration = bgmDuration - mBgmStartPosMs;
                if (mVideoDurationMs == 0) {
                    mVideoDurationMs = remainVideoDuration;
                    Logger.d(TAG, "Video duration is 0, adjust it to: " + mVideoDurationMs);
                }
                if (mBgmDurationMs == 0) {
                    mBgmDurationMs = remainVideoDuration > remainBgmDuration ?
                            remainBgmDuration : remainVideoDuration;
                    Logger.d(TAG, "Bgm duration is 0, adjust it to: " + mBgmDurationMs);
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
            MediaFormat audioFormat = null;
            mediaMuxer = new MediaMuxer(mOutputFile.getAbsolutePath(),
                    MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
            mediaMuxer.setOrientationHint(VideoUtil.getVideoRotation(mInputFile));

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
                    audioFormat = mediaFormat;
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
                    if (audioFormat != null) {
                        // select smaller sample rate format
                        // TODO: this will cause audio deformation, should do resample on audio.
                        audioFormat = audioFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE) >
                                mediaFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE) ?
                                mediaFormat : audioFormat;
                    } else {
                        audioFormat = mediaFormat;
                    }
                    break;
                }
            }

            if (audioFormat == null) {
                throw new IllegalArgumentException("We do not find any audio track in bgm: " + bgmFile);
            } else {
                outAudioTrackId = mediaMuxer.addTrack(audioFormat);
                mediaMuxer.start();
            }
        }

        private void addBgm() {
            long videoDuration = VideoUtil.getVideoDuration(mInputFile);
            // handle video first
            if (inVideoTrackId != -1) {
                Logger.d(TAG, "Write source video track first.");
                sourceExtractor.selectTrack(inVideoTrackId);
                while (true) {
                    onProgress((bufferInfo.presentationTimeUs * 0.25f / (videoDuration * 1000)) + 0.5f);
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

                    onProgress((bufferInfo.presentationTimeUs * 0.25f / (videoDuration * 1000)) + 0.75f);

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
//                            long sampleTime = mVideoStartPosMs * 1000 + bgmAudioExtractor.getSampleTime();
//                            bufferInfo.presentationTimeUs = sampleTime < (mVideoStartPosMs + mVideoDurationMs) * 1000 ?
//                                    sampleTime : (mVideoStartPosMs + mVideoDurationMs) * 1000;
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

            onProgress(1f);
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
