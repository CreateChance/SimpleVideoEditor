package com.createchance.simplevideoeditor;

import android.app.Application;
import android.content.Context;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * ${DESC}
 *
 * @author gaochao1-iri
 * @date 02/05/2018
 */
public class VideoEditorManager {
    private static final String TAG = VideoEditorManager.class.getSimpleName();

    private static VideoEditorManager sManager;

    private Context mContext;

    private Editor mCurrentEditor;

    private VideoEditorManager() {

    }

    public synchronized static VideoEditorManager getManager() {
        if (sManager == null) {
            sManager = new VideoEditorManager();
        }

        return sManager;
    }

    public synchronized void init(Application application) {
        this.mContext = application;
    }

    public Context getContext() {
        return mContext;
    }

    public synchronized Editor edit(File videoFile, VideoEditCallback callback) {
        if (mCurrentEditor != null) {
            Logger.e(TAG, "One edit is on going, try again later.");
            return null;
        }
        mCurrentEditor = new Editor(videoFile, callback);
        return mCurrentEditor;
    }

    VideoEditCallback getCallback() {
        return mCurrentEditor.mCallback;
    }

    public static class Editor {
        private File inputFile;
        private File ouputFile;
        private List<AbstractAction> actionList = new ArrayList<>();

        VideoEditCallback mCallback;

        private Editor(File input, VideoEditCallback callback) {
            this.inputFile = input;
            this.mCallback = callback;
        }

        public Editor withAction(AbstractAction action) {
            if (action != null) {
                actionList.add(action);
            }

            return this;
        }

        public Editor saveAs(File outputFile) {
            this.ouputFile = outputFile;

            return this;
        }

        public void commit() {
            // check if input file is rational.
            if (inputFile == null ||
                    !inputFile.exists() ||
                    !inputFile.isFile()) {
                throw new IllegalArgumentException("Input video is illegal, input video: " + inputFile);
            }
            if (actionList.size() == 0) {
                Logger.e(TAG, "No edit action specified, please set at least one action!");
                return;
            }

            // edit input video file now.
            // parse input video info before we real start.
            VideoInfoParseAction videoInfoParseAction = new VideoInfoParseAction(inputFile);
            videoInfoParseAction.successNext(actionList.get(0));
            int index = 0;
            while (index < actionList.size() - 1) {
                actionList.get(index++).successNext(actionList.get(index++));
            }
            videoInfoParseAction.start();
        }
    }
}
