package com.createchance.demo;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
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

    public static void getPcmData(File audioFile, File output) {
        try {
            final String encodeFile = audioFile.getAbsolutePath();
            MediaExtractor extractor = new MediaExtractor();
            extractor.setDataSource(encodeFile);
            MediaFormat mediaFormat = null;
            for (int i = 0; i < extractor.getTrackCount(); i++) {
                MediaFormat format = extractor.getTrackFormat(i);
                String mime = format.getString(MediaFormat.KEY_MIME);
                if (mime.startsWith("audio/")) {
                    extractor.selectTrack(i);
                    mediaFormat = format;
                    break;
                }
            }
            if (mediaFormat == null) {
                Log.e(TAG, "not a valid file with audio track..");
                extractor.release();
                return;
            }
            FileOutputStream fosDecoder = new FileOutputStream(output);//your out file path
            String mediaMime = mediaFormat.getString(MediaFormat.KEY_MIME);
            MediaCodec codec = MediaCodec.createDecoderByType(mediaMime);
            codec.configure(mediaFormat, null, null, 0);
            codec.start();
            ByteBuffer[] codecInputBuffers = codec.getInputBuffers();
            ByteBuffer[] codecOutputBuffers = codec.getOutputBuffers();
            final long kTimeOutUs = 5000;
            MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
            boolean sawInputEOS = false;
            boolean sawOutputEOS = false;
            int totalRawSize = 0;
            try {
                while (!sawOutputEOS) {
                    if (!sawInputEOS) {
                        int inputBufIndex = codec.dequeueInputBuffer(kTimeOutUs);
                        if (inputBufIndex >= 0) {
                            ByteBuffer dstBuf = codecInputBuffers[inputBufIndex];
                            int sampleSize = extractor.readSampleData(dstBuf, 0);
                            if (sampleSize < 0) {
                                Log.i(TAG, "saw input EOS.");
                                sawInputEOS = true;
                                codec.queueInputBuffer(inputBufIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                            } else {
                                long presentationTimeUs = extractor.getSampleTime();
                                codec.queueInputBuffer(inputBufIndex, 0, sampleSize, presentationTimeUs, 0);
                                extractor.advance();
                            }
                        }
                    }
                    int res = codec.dequeueOutputBuffer(info, kTimeOutUs);
                    if (res >= 0) {
                        int outputBufIndex = res;
                        // Simply ignore codec config buffers.
                        if ((info.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
                            Log.i(TAG, "audio encoder: codec config buffer");
                            codec.releaseOutputBuffer(outputBufIndex, false);
                            continue;
                        }

                        if (info.size != 0) {

                            ByteBuffer outBuf = codecOutputBuffers[outputBufIndex];

                            outBuf.position(info.offset);
                            outBuf.limit(info.offset + info.size);
                            byte[] data = new byte[info.size];
                            outBuf.get(data);
                            totalRawSize += data.length;
                            fosDecoder.write(data);

                        }

                        codec.releaseOutputBuffer(outputBufIndex, false);

                        if ((info.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                            Log.i(TAG, "saw output EOS.");
                            sawOutputEOS = true;
                        }

                    } else if (res == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
                        codecOutputBuffers = codec.getOutputBuffers();
                        Log.i(TAG, "output buffers have changed.");
                    } else if (res == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                        MediaFormat oformat = codec.getOutputFormat();
                        Log.i(TAG, "output format has changed to " + oformat);
                    }
                }
            } finally {
                fosDecoder.close();
                codec.stop();
                codec.release();
                extractor.release();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void playPcm(File audioFile) {
        int sampleRateInHz = 44100;
        int channelConfig = AudioFormat.CHANNEL_OUT_STEREO;
        int audioFormat = AudioFormat.ENCODING_PCM_16BIT;
        int bufferSizeInBytes = AudioTrack.getMinBufferSize(sampleRateInHz, channelConfig, audioFormat);
        AudioTrack audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, sampleRateInHz, channelConfig, audioFormat, bufferSizeInBytes, AudioTrack.MODE_STREAM);
        audioTrack.play();

        FileInputStream audioInput = null;
        try {
            audioInput = new FileInputStream(audioFile);//put your wav file in
            audioInput.read(new byte[44]);//skid 44 wav header

            byte[] audioData = new byte[512];

            while (audioInput.read(audioData) != -1) {
                audioTrack.write(audioData, 0, audioData.length); //play raw audio bytes
            }

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            audioTrack.stop();
            audioTrack.release();
            if (audioInput != null)
                try {
                    audioInput.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
        }
    }

    public static void mixPcm(File firstAudio, File secongAudio, File output) {
        try {
            byte[] firstBuff = new byte[2];
            byte[] secondBuff = new byte[2];
            byte[] outBuff = new byte[2];
            short firstVal, secondVal, outputVal;
            FileOutputStream fileOutputStream = new FileOutputStream(output);
            FileInputStream firstAudioStream = new FileInputStream(firstAudio);
            FileInputStream secondAudioStream = new FileInputStream(secongAudio);

            while (true) {
                int firstRet = firstAudioStream.read(firstBuff, 0, 2);
                int secondRet = secondAudioStream.read(secondBuff, 0, 2);
                if (firstRet != -1 && secondRet != -1) {
                    firstVal = (short) (firstBuff[0] & 0xff | (firstBuff[1] & 0xff) << 8);
                    secondVal = (short) (secondBuff[0] & 0xff | (secondBuff[1] & 0xff) << 8);
                    Log.d(TAG, "mixPcm, first: " + firstVal + ", second: " + secondVal);
                    outputVal = (short) ((firstVal + secondVal) / 2);
                    outBuff[0] = (byte) (outputVal & 0x00ff);
                    outBuff[1] = (byte) ((outputVal & 0x00ff) >> 8);
                    Log.d(TAG, "mixPcm, writing data......");
                    fileOutputStream.write(outBuff, 0, 2);
                } else {
                    Log.d(TAG, "mixPcm, read one end, first: " + firstRet + ", second: " + secondRet);
                    break;
                }
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
