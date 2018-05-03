package com.createchance.simplevideoeditor;

import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.media.MediaMetadataRetriever;

import java.io.File;

/**
 * ${DESC}
 *
 * @author gaochao1-iri
 * @date 03/05/2018
 */
class VideoInfoParseAction extends AbstractAction {

    private static final String TAG = VideoInfoParseAction.class.getSimpleName();

    private File mInputFile;

    VideoInfoParseAction(File inputFile) {
        this.mInputFile = inputFile;
    }

    @Override
    public void start() {
        if (VideoEditorManager.getManager().getCallback() != null) {
            VideoEditorManager.getManager().getCallback().onStart(Constants.STAGE_INFO_PARSE);
        }
        WorkRunner.addTaskToBackground(new VideoInfoParseWorker());
    }

    private class VideoInfoParseWorker implements Runnable {

        @Override
        public void run() {
            MediaMetadataRetriever metadataRetriever = null;
            MediaExtractor mediaExtractor = null;
            try {
                metadataRetriever = new MediaMetadataRetriever();
                metadataRetriever.setDataSource(mInputFile.getAbsolutePath());
                VideoInfo videoInfo = new VideoInfo(mInputFile);
                videoInfo.width = Integer.valueOf(metadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH));
                videoInfo.height = Integer.valueOf(metadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT));
                videoInfo.durationMs = Long.valueOf(metadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION));
                videoInfo.rotation = Integer.valueOf(metadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION));
                mediaExtractor = new MediaExtractor();
                mediaExtractor.setDataSource(mInputFile.getAbsolutePath());
                for (int i = 0; i < mediaExtractor.getTrackCount(); i++) {
                    MediaFormat mediaFormat = mediaExtractor.getTrackFormat(i);
                    String mime = mediaFormat.getString(MediaFormat.KEY_MIME);
                    if (mime.startsWith("video")) {
                        videoInfo.videoTrackId = i;
                        if (videoInfo.audioTrackId != VideoInfo.INVALID_TRACK_ID) {
                            Logger.d(TAG, "We have found all track id.");
                            break;
                        }
                    } else if (mime.startsWith("audio")) {
                        videoInfo.audioTrackId = i;
                        if (videoInfo.videoTrackId != VideoInfo.INVALID_TRACK_ID) {
                            Logger.d(TAG, "We have found all track id.");
                            break;
                        }
                    }
                }

                if (videoInfo.videoTrackId == VideoInfo.INVALID_TRACK_ID) {
                    Logger.e(TAG, "No video track in video: " + mInputFile + ", so exit!");
                    if (VideoEditorManager.getManager().getCallback() != null) {
                        VideoEditorManager.getManager().getCallback().onFialed(Constants.STAGE_INFO_PARSE);
                    }
                } else {
                    Logger.d(TAG, "Video info parse done! Video info: " + videoInfo);
                    // save this video info.
                    EditParamsMap.saveParams(EditParamsMap.KEY_INPUT_VIDEO_INFO, videoInfo);
                    if (VideoEditorManager.getManager().getCallback() != null) {
                        VideoEditorManager.getManager().getCallback().onSucceeded(Constants.STAGE_INFO_PARSE);
                    }
                    execNext();
                }
            } catch (Exception e) {
                e.printStackTrace();
                if (VideoEditorManager.getManager().getCallback() != null) {
                    VideoEditorManager.getManager().getCallback().onFialed(Constants.STAGE_INFO_PARSE);
                }
            } finally {
                if (metadataRetriever != null) {
                    metadataRetriever.release();
                }

                if (mediaExtractor != null) {
                    mediaExtractor.release();
                }
            }
        }
    }
}
