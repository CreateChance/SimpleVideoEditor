package com.createchance.simplevideoeditor;

import android.app.Application;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;

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

    private final int MSG_ON_START = 100;
    private final int MSG_ON_PROGRESS = 101;
    private final int MSG_ON_SUCCEED = 102;
    private final int MSG_ON_FAILED = 103;
    private final int MSG_ON_ALL_SUCCEED = 104;
    private final String KEY_ACTION = "action";
    private final String KEY_PROGRESS = "progress";
    private Handler mMainHandler;

    private VideoEditorManager() {
        mMainHandler = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                Bundle params = msg.getData();
                switch (msg.what) {
                    case MSG_ON_START:
                        if (mCurrentEditor.mCallback != null) {
                            mCurrentEditor.mCallback.onStart(params.getString(KEY_ACTION));
                        }
                        break;
                    case MSG_ON_PROGRESS:
                        if (mCurrentEditor.mCallback != null) {
                            mCurrentEditor.mCallback.onProgress(params.getString(KEY_ACTION),
                                    params.getFloat(KEY_PROGRESS));
                        }
                        break;
                    case MSG_ON_SUCCEED:
                        if (mCurrentEditor.mCallback != null) {
                            mCurrentEditor.mCallback.onSucceeded(params.getString(KEY_ACTION));
                        }
                        break;
                    case MSG_ON_FAILED:
                        // clean all the tmp files.
                        for (AbstractAction act : mCurrentEditor.actionList) {
                            act.release();
                        }

                        if (mCurrentEditor.mCallback != null) {
                            mCurrentEditor.mCallback.onFailed(params.getString(KEY_ACTION));
                        }
                        break;
                    case MSG_ON_ALL_SUCCEED:
                        // clean all the tmp files.
                        for (AbstractAction act : mCurrentEditor.actionList) {
                            act.release();
                        }
                        mCurrentEditor = null;
                        break;
                    default:
                        break;
                }
            }
        };
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

    public File getOutputFile() {
        return mCurrentEditor == null ? null : mCurrentEditor.outputFile;
    }

    public void onStart(String action) {
        Message message = Message.obtain();
        Bundle params = new Bundle();
        params.putString(KEY_ACTION, action);
        message.what = MSG_ON_START;
        message.setData(params);
        mMainHandler.sendMessage(message);
    }

    public void onProgress(String action, float progress) {
        Message message = Message.obtain();
        Bundle params = new Bundle();
        params.putString(KEY_ACTION, action);
        params.putFloat(KEY_PROGRESS, progress);
        message.what = MSG_ON_PROGRESS;
        message.setData(params);
        mMainHandler.sendMessage(message);
    }

    public void onSucceed(String action) {
        Message message = Message.obtain();
        Bundle params = new Bundle();
        params.putString(KEY_ACTION, action);
        message.what = MSG_ON_SUCCEED;
        message.setData(params);
        mMainHandler.sendMessage(message);
    }

    public void onAllSucceed() {
        Message message = Message.obtain();
        Bundle params = new Bundle();
        message.what = MSG_ON_ALL_SUCCEED;
        message.setData(params);
        mMainHandler.sendMessage(message);
    }

    public void onFailed(String action) {
        Message message = Message.obtain();
        Bundle params = new Bundle();
        params.putString(KEY_ACTION, action);
        message.what = MSG_ON_FAILED;
        message.setData(params);
        mMainHandler.sendMessage(message);
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
