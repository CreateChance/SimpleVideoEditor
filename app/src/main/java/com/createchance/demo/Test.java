package com.createchance.demo;

import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * ${DESC}
 *
 * @author gaochao1-iri
 * @date 18/03/2018
 */

public class Test {

    private static final String TAG = "Test";

    public static void extractMedia(File inputVideo) {
        FileOutputStream videoOutput = null;
        FileOutputStream audioOutput = null;
        MediaExtractor mediaExtractor = new MediaExtractor();

        try {
            videoOutput = new FileOutputStream(new File(Environment.getExternalStorageDirectory(), "output_video.mp4"));
            audioOutput = new FileOutputStream(new File(Environment.getExternalStorageDirectory(), "output_audio"));

            mediaExtractor.setDataSource(inputVideo.getPath());
            int trackCount = mediaExtractor.getTrackCount();
            Log.d(TAG, "Track count: " + trackCount);

            int audioTrackIndex = -1;
            int videoTrackIndex = -1;
            for (int i = 0; i < trackCount; i++) {
                MediaFormat mediaFormat = mediaExtractor.getTrackFormat(i);
                String mineType = mediaFormat.getString(MediaFormat.KEY_MIME);

                if (mineType.startsWith("video/")) {
                    videoTrackIndex = i;
                } else if (mineType.startsWith("audio/")) {
                    audioTrackIndex = i;
                }
            }

            ByteBuffer byteBuffer = ByteBuffer.allocate(500 * 1024);

            mediaExtractor.selectTrack(videoTrackIndex);
            while (true) {
                int count = mediaExtractor.readSampleData(byteBuffer, 0);

                if (count < 0) {
                    Log.e(TAG, "read video error, no more data!");
                    break;
                }

                byte[] buffer = new byte[count];
                byteBuffer.get(buffer);
                videoOutput.write(buffer);
                byteBuffer.clear();

                mediaExtractor.advance();
            }

            mediaExtractor.selectTrack(audioTrackIndex);
            while (true) {
                int count = mediaExtractor.readSampleData(byteBuffer, 0);

                if (count < 0) {
                    Log.e(TAG, "read video error, no more data!");
                    break;
                }

                byte[] buffer = new byte[count];
                byteBuffer.get(buffer);
                audioOutput.write(buffer);
                byteBuffer.clear();

                mediaExtractor.advance();
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            mediaExtractor.release();
        }
    }

    public static void extractVideo(File inputVideo) {
        MediaExtractor mediaExtractor = new MediaExtractor();
        MediaMuxer mediaMuxer = null;
        int videoTrackIndex = -1;

        try {
            mediaExtractor.setDataSource(inputVideo.getPath());

            int trackCount = mediaExtractor.getTrackCount();
            for (int i = 0; i < trackCount; i++) {
                MediaFormat mediaFormat = mediaExtractor.getTrackFormat(i);
                String mediaType = mediaFormat.getString(MediaFormat.KEY_MIME);

                if (mediaType.startsWith("video/")) {
                    videoTrackIndex = i;
                    break;
                }
            }

            mediaExtractor.selectTrack(videoTrackIndex);
            MediaFormat trackFormat = mediaExtractor.getTrackFormat(videoTrackIndex);
            mediaMuxer = new MediaMuxer(Environment.getExternalStorageDirectory().getPath() + "/output_video.mp4",
                    MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);

            int trackIndex = mediaMuxer.addTrack(trackFormat);

            ByteBuffer byteBuffer = ByteBuffer.allocate(1024 * 500);
            MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();

            mediaMuxer.start();

            long videoSampleTime;
            //获取每帧的之间的时间
            {
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
                videoSampleTime = Math.abs(secondVideoPTS - firstVideoPTS);
                Log.d(TAG, "videoSampleTime is " + videoSampleTime);
            }

            //重新切换此信道，不然上面跳过了3帧,造成前面的帧数模糊
            mediaExtractor.unselectTrack(videoTrackIndex);
            mediaExtractor.selectTrack(videoTrackIndex);
            while (true) {
                //读取帧之间的数据
                int readSampleSize = mediaExtractor.readSampleData(byteBuffer, 0);
                if (readSampleSize < 0) {
                    break;
                }
                mediaExtractor.advance();
                bufferInfo.size = readSampleSize;
                bufferInfo.offset = 0;
                bufferInfo.flags = mediaExtractor.getSampleFlags();
                bufferInfo.presentationTimeUs += videoSampleTime;
                //写入帧的数据
                mediaMuxer.writeSampleData(trackIndex, byteBuffer, bufferInfo);
            }
            //release
            mediaMuxer.stop();
            mediaExtractor.release();
            mediaMuxer.release();

            Log.d(TAG, "extractVideo done!");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void extractAudio(File inputVideo) {
        MediaExtractor mediaExtractor = new MediaExtractor();
        MediaMuxer mediaMuxer = null;
        int audioTrackIndex = -1;

        try {
            mediaExtractor.setDataSource(inputVideo.getPath());

            int trackCount = mediaExtractor.getTrackCount();
            for (int i = 0; i < trackCount; i++) {
                MediaFormat mediaFormat = mediaExtractor.getTrackFormat(i);
                String mediaType = mediaFormat.getString(MediaFormat.KEY_MIME);

                if (mediaType.startsWith("audio/")) {
                    audioTrackIndex = i;
                    break;
                }
            }

            mediaExtractor.selectTrack(audioTrackIndex);
            MediaFormat trackFormat = mediaExtractor.getTrackFormat(audioTrackIndex);
            mediaMuxer = new MediaMuxer(Environment.getExternalStorageDirectory().getPath() + "/output_audio.mp4",
                    MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);

            int trackIndex = mediaMuxer.addTrack(trackFormat);

            ByteBuffer byteBuffer = ByteBuffer.allocate(1024 * 500);
            MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();

            mediaMuxer.start();

            long videoSampleTime;
            //获取每帧的之间的时间
            {
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
                videoSampleTime = Math.abs(secondVideoPTS - firstVideoPTS);
                Log.d(TAG, "audioSampleTime is " + videoSampleTime);
            }

            //重新切换此信道，不然上面跳过了3帧,造成前面的帧数模糊
            mediaExtractor.unselectTrack(audioTrackIndex);
            mediaExtractor.selectTrack(audioTrackIndex);
            while (true) {
                //读取帧之间的数据
                int readSampleSize = mediaExtractor.readSampleData(byteBuffer, 0);
                if (readSampleSize < 0) {
                    break;
                }
                mediaExtractor.advance();
                bufferInfo.size = readSampleSize;
                bufferInfo.offset = 0;
                bufferInfo.flags = mediaExtractor.getSampleFlags();
                bufferInfo.presentationTimeUs += videoSampleTime;
                //写入帧的数据
                mediaMuxer.writeSampleData(trackIndex, byteBuffer, bufferInfo);
            }
            //release
            mediaMuxer.stop();
            mediaExtractor.release();
            mediaMuxer.release();

            Log.d(TAG, "extractAudio done!");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void combineVideo(File audioFile, File videoFile) {
        try {
            MediaExtractor videoExtractor = new MediaExtractor();
            videoExtractor.setDataSource(videoFile.getPath());
            MediaFormat videoFormat = null;
            int videoTrackIndex = -1;
            int videoTrackCount = videoExtractor.getTrackCount();
            for (int i = 0; i < videoTrackCount; i++) {
                videoFormat = videoExtractor.getTrackFormat(i);
                String mimeType = videoFormat.getString(MediaFormat.KEY_MIME);
                if (mimeType.startsWith("video/")) {
                    videoTrackIndex = i;
                    break;
                }
            }

            MediaExtractor audioExtractor = new MediaExtractor();
            audioExtractor.setDataSource(audioFile.getPath());
            MediaFormat audioFormat = null;
            int audioTrackIndex = -1;
            int audioTrackCount = audioExtractor.getTrackCount();
            for (int i = 0; i < audioTrackCount; i++) {
                audioFormat = audioExtractor.getTrackFormat(i);
                String mimeType = audioFormat.getString(MediaFormat.KEY_MIME);
                Log.d(TAG, "combineVideo, audio mine: " + mimeType);
                if (mimeType.startsWith("audio/")) {
                    audioTrackIndex = i;
                    break;
                }
            }

            videoExtractor.selectTrack(videoTrackIndex);
            audioExtractor.selectTrack(audioTrackIndex);

            MediaCodec.BufferInfo videoBufferInfo = new MediaCodec.BufferInfo();
            MediaCodec.BufferInfo audioBufferInfo = new MediaCodec.BufferInfo();

            MediaMuxer mediaMuxer = new MediaMuxer(
                    Environment.getExternalStorageDirectory() + "/combine.mp4",
                    MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
            int writeVideoTrackIndex = mediaMuxer.addTrack(videoFormat);
            int writeAudioTrackIndex = mediaMuxer.addTrack(audioFormat);
            mediaMuxer.start();

            ByteBuffer byteBuffer = ByteBuffer.allocate(500 * 1024);
            long sampleTime = 0;
            {
                videoExtractor.readSampleData(byteBuffer, 0);
                if (videoExtractor.getSampleFlags() == MediaExtractor.SAMPLE_FLAG_SYNC) {
                    videoExtractor.advance();
                }
                videoExtractor.readSampleData(byteBuffer, 0);
                long secondTime = videoExtractor.getSampleTime();
                videoExtractor.advance();
                long thirdTime = videoExtractor.getSampleTime();
                sampleTime = Math.abs(thirdTime - secondTime);
            }
            videoExtractor.unselectTrack(videoTrackIndex);
            videoExtractor.selectTrack(videoTrackIndex);

            while (true) {
                int readVideoSampleSize = videoExtractor.readSampleData(byteBuffer, 0);
                if (readVideoSampleSize < 0) {
                    break;
                }
                videoBufferInfo.size = readVideoSampleSize;
                videoBufferInfo.presentationTimeUs += sampleTime;
                videoBufferInfo.offset = 0;
                videoBufferInfo.flags = videoExtractor.getSampleFlags();
                mediaMuxer.writeSampleData(writeVideoTrackIndex, byteBuffer, videoBufferInfo);
                videoExtractor.advance();
            }

            while (true) {
                int readAudioSampleSize = audioExtractor.readSampleData(byteBuffer, 0);
                if (readAudioSampleSize < 0) {
                    break;
                }

                audioBufferInfo.size = readAudioSampleSize;
                audioBufferInfo.presentationTimeUs += sampleTime;
                audioBufferInfo.offset = 0;
                audioBufferInfo.flags = videoExtractor.getSampleFlags();
                mediaMuxer.writeSampleData(writeAudioTrackIndex, byteBuffer, audioBufferInfo);
                audioExtractor.advance();
            }

            mediaMuxer.stop();
            mediaMuxer.release();
            videoExtractor.release();
            audioExtractor.release();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void addBackgroundMusic(File music, File video, File output) {
        try {
            MediaMuxer mediaMuxer = new MediaMuxer(
                    output.getAbsolutePath(),
                    MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
            MediaExtractor audioExtractor = new MediaExtractor();
            audioExtractor.setDataSource(music.getPath());
            int bgmAudioTrackIndex = -1;
            for (int i = 0; i < audioExtractor.getTrackCount(); i++) {
                MediaFormat format = audioExtractor.getTrackFormat(i);
                Log.d(TAG, "addBackgroundMusic, audio track: " + format);
                if (format.getString(MediaFormat.KEY_MIME).startsWith("audio/")) {
                    audioExtractor.selectTrack(i);
                    bgmAudioTrackIndex = mediaMuxer.addTrack(format);
                    break;
                }
            }

            MediaExtractor videoExtractor = new MediaExtractor();
            videoExtractor.setDataSource(video.getPath());
            int inVideoTrackIndex = -1;
            int inAudioTrackIndex = -1;
            int outVideoTrackIndex = -1;
            long videoDuration = 0;
            for (int i = 0; i < videoExtractor.getTrackCount(); i++) {
                MediaFormat format = videoExtractor.getTrackFormat(i);
                Log.d(TAG, "addBackgroundMusic, video track: " + format);
                if (format.getString(MediaFormat.KEY_MIME).startsWith("video/")) {
                    inVideoTrackIndex = i;
                    outVideoTrackIndex = mediaMuxer.addTrack(format);
                    videoDuration = format.getLong(MediaFormat.KEY_DURATION);
                } else if (format.getString(MediaFormat.KEY_MIME).startsWith("audio")) {
                    inAudioTrackIndex = i;
                }
            }

            Log.d(TAG, "addBackgroundMusic, output duration is: " + videoDuration);

            mediaMuxer.start();

            if (bgmAudioTrackIndex != -1) {
                MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
                bufferInfo.presentationTimeUs = 0;
                ByteBuffer byteBuffer = ByteBuffer.allocate(100 * 1024);
                while (true) {
                    int sampleSize = audioExtractor.readSampleData(byteBuffer, 0);
                    if (sampleSize < 0) {
                        Log.d(TAG, "addBackgroundMusic, read audio sample data done!");
                        break;
                    }

                    bufferInfo.offset = 0;
                    bufferInfo.size = sampleSize;
                    bufferInfo.flags = MediaCodec.BUFFER_FLAG_SYNC_FRAME;
                    bufferInfo.presentationTimeUs = audioExtractor.getSampleTime();

                    if (bufferInfo.presentationTimeUs > videoDuration) {
                        break;
                    }

                    mediaMuxer.writeSampleData(bgmAudioTrackIndex, byteBuffer, bufferInfo);

                    Log.d(TAG, "addBackgroundMusic, audio duration: " + bufferInfo.presentationTimeUs);

                    audioExtractor.advance();
                }
            }

            if (inVideoTrackIndex != -1) {
                videoExtractor.selectTrack(inVideoTrackIndex);
                MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
                bufferInfo.presentationTimeUs = 0;
                ByteBuffer byteBuffer = ByteBuffer.allocate(512 * 1024);
                while (true) {
                    int sampleSize = videoExtractor.readSampleData(byteBuffer, 0);
                    if (sampleSize < 0) {
                        Log.d(TAG, "addBackgroundMusic, read video sample data done!");
                        break;
                    }

                    bufferInfo.offset = 0;
                    bufferInfo.size = sampleSize;
                    bufferInfo.flags = MediaCodec.BUFFER_FLAG_SYNC_FRAME;
                    bufferInfo.presentationTimeUs = videoExtractor.getSampleTime();

                    mediaMuxer.writeSampleData(outVideoTrackIndex, byteBuffer, bufferInfo);

                    Log.d(TAG, "addBackgroundMusic, video duration: " + bufferInfo.presentationTimeUs);

                    videoExtractor.advance();
                }

                videoExtractor.unselectTrack(inVideoTrackIndex);
            }

            if (inAudioTrackIndex != -1) {
                videoExtractor.selectTrack(inAudioTrackIndex);
                MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
                bufferInfo.presentationTimeUs = 0;
                ByteBuffer byteBuffer = ByteBuffer.allocate(512 * 1024);
                while (true) {
                    int sampleSize = videoExtractor.readSampleData(byteBuffer, 0);
                    if (sampleSize < 0) {
                        Log.d(TAG, "addBackgroundMusic, read origin audio sample data done!");
                        break;
                    }

                    bufferInfo.offset = 0;
                    bufferInfo.size = sampleSize;
                    bufferInfo.flags = MediaCodec.BUFFER_FLAG_SYNC_FRAME;
                    bufferInfo.presentationTimeUs = videoExtractor.getSampleTime();

                    Log.d(TAG, "addBackgroundMusic, origin audio duration: " + bufferInfo.presentationTimeUs);

                    videoExtractor.advance();
                }
            }

            audioExtractor.release();
            videoExtractor.release();

            mediaMuxer.stop();
            mediaMuxer.release();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void convertMp3ToAac(File mp3File) {

    }

}
