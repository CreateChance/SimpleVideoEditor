package com.createchance.simplevideoeditor;

import android.app.Application;
import android.content.Context;

import com.createchance.simplevideoeditor.actions.AbstractAction;

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

    public synchronized Editor edit(File videoFile) {
        if (mCurrentEditor != null) {
            Logger.e(TAG, "One edit is on going, try again later.");
            return null;
        }
        mCurrentEditor = new Editor(videoFile);
        return mCurrentEditor;
    }

    public File getBaseWorkFolder() {
        return mCurrentEditor == null ?
                null : mCurrentEditor.outputFile == null ?
                null : mCurrentEditor.outputFile.getParentFile();
    }

    public  File getOutputFile() {
        return mCurrentEditor == null ? null : mCurrentEditor.outputFile;
    }

    public void onStart(String action) {
        if (mCurrentEditor.mCallback != null) {
            mCurrentEditor.mCallback.onStart(action);
        }
    }

    public void onProgress(String action, float progress) {
        if (mCurrentEditor.mCallback != null) {
            mCurrentEditor.mCallback.onProgress(action, progress);
        }
    }

    public void onSucceed(String action) {
        if (mCurrentEditor.mCallback != null) {
            mCurrentEditor.mCallback.onSucceeded(action);
        }
    }

    public void onAllSucceed() {
        // clean all the tmp files.
        for (AbstractAction act : mCurrentEditor.actionList) {
            act.release();
        }
        mCurrentEditor = null;
    }

    public void onFailed(String action) {
        // clean all the tmp files.
        for (AbstractAction act : mCurrentEditor.actionList) {
            act.release();
        }

        if (mCurrentEditor.mCallback != null) {
            mCurrentEditor.mCallback.onFailed(action);
        }
    }

    public static class Editor {
        private File inputFile;
        private File outputFile;
        private List<AbstractAction> actionList = new ArrayList<>();

        VideoEditCallback mCallback;

        private Editor(File input) {
            this.inputFile = input;
        }

        public Editor withAction(AbstractAction action) {
            if (action != null) {
                actionList.add(action);
            }

            return this;
        }

        public Editor saveAs(File outputFile) {
            this.outputFile = outputFile;

            return this;
        }

        public void commit(VideoEditCallback callback) {
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

            mCallback = callback;

            for (int i = 0; i < actionList.size() - 1; i++) {
                actionList.get(i).successNext(actionList.get(i + 1));
            }

            Logger.d(TAG, "Start edit, input file: " + inputFile
                    + ", output file: " + outputFile
                    + ", action list: " + actionList);

            // start from the first one.
            actionList.get(0).start(inputFile);
        }
    }
}
