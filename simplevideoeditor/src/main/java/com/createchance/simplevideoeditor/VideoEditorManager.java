package com.createchance.simplevideoeditor;

import android.app.Application;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.LongSparseArray;

import com.createchance.simplevideoeditor.actions.AbstractAction;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
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

    private final int MSG_ON_START = 100;
    private final int MSG_ON_PROGRESS = 101;
    private final int MSG_ON_SUCCEED = 102;
    private final int MSG_ON_FAILED = 103;
    private final String KEY_TOKEN = "token";
    private final String KEY_ACTION = "action";
    private final String KEY_PROGRESS = "progress";
    private Handler mMainHandler;

    // calling map
    private LongSparseArray<Editor> mCallMap;

    private VideoEditorManager() {
        mCallMap = new LongSparseArray<>();
        mMainHandler = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                Bundle params = msg.getData();
                long token = params.getLong(KEY_TOKEN);
                Editor editor = mCallMap.get(token);
                if (editor == null) {
                    Logger.e(TAG, "No editor has token: " + token);
                    return;
                }
                switch (msg.what) {
                    case MSG_ON_START:
                        if (editor.mCallback != null) {
                            editor.mCallback.onStart(params.getString(KEY_ACTION));
                        }
                        break;
                    case MSG_ON_PROGRESS:
                        if (editor.mCallback != null) {
                            editor.mCallback.onProgress(params.getString(KEY_ACTION),
                                    params.getFloat(KEY_PROGRESS));
                        }
                        break;
                    case MSG_ON_SUCCEED:
                        if (editor.mCallback != null) {
                            editor.mCallback.onSucceeded(params.getString(KEY_ACTION));
                        }
                        break;
                    case MSG_ON_FAILED:
                        // clean all the tmp files.
                        for (AbstractAction act : editor.mActionList) {
                            act.release();
                        }

                        if (editor.mCallback != null) {
                            editor.mCallback.onFailed(params.getString(KEY_ACTION));
                        }
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
        return new Editor(videoFile);
    }

    public synchronized void cancel(long token) {
        Editor editor = mCallMap.get(token);
        if (editor != null) {
            editor.mIsCanceled = true;
        }
    }

    public File getBaseWorkFolder(long token) {
        Editor editor = mCallMap.get(token);
        return editor == null ?
                null : editor.mOutputFile == null ?
                null : editor.mOutputFile.getParentFile();
    }

    public void onStart(long token, AbstractAction action) {
        Message message = Message.obtain();
        Bundle params = new Bundle();
        params.putLong(KEY_TOKEN, token);
        params.putString(KEY_ACTION, action.mActionName);
        message.what = MSG_ON_START;
        message.setData(params);
        mMainHandler.sendMessage(message);
    }

    public void onProgress(long token, AbstractAction action, float progress) {
        Message message = Message.obtain();
        Bundle params = new Bundle();
        params.putLong(KEY_TOKEN, token);
        params.putString(KEY_ACTION, action.mActionName);
        params.putFloat(KEY_PROGRESS, progress);
        message.what = MSG_ON_PROGRESS;
        message.setData(params);
        mMainHandler.sendMessage(message);
    }

    public void onSucceed(long token, AbstractAction action) {
        Editor editor = mCallMap.get(token);
        if (editor.mIsCanceled) {
            Logger.d(TAG, "This editor is canceled!");
            // this editor is canceled, so remove it's output and no callback.
            clear(token);
        } else {
            // try exec next action.
            if (editor.mActionIterator.hasNext()) {
                // notify user on ui thread.
                Message message = Message.obtain();
                Bundle params = new Bundle();
                params.putLong(KEY_TOKEN, token);
                params.putString(KEY_ACTION, action.mActionName);
                message.what = MSG_ON_SUCCEED;
                message.setData(params);
                mMainHandler.sendMessage(message);
                // our output is the input of next action.
                editor.mActionIterator.next().start(action.mOutputFile);
            } else {
                action.mOutputFile.renameTo(editor.mOutputFile);
                // notify user on ui thread.
                Message message = Message.obtain();
                Bundle params = new Bundle();
                params.putLong(KEY_TOKEN, token);
                params.putString(KEY_ACTION, action.mActionName);
                message.what = MSG_ON_SUCCEED;
                message.setData(params);
                mMainHandler.sendMessage(message);
                // clear all.
                clear(token);
            }
        }
    }

    private synchronized void clear(long token) {
        Editor editor = mCallMap.get(token);
        if (editor == null) {
            return;
        }
        // clean all the tmp files.
        for (AbstractAction act : editor.mActionList) {
            act.release();
        }
        // remove this editor from call map
        mCallMap.remove(token);
    }

    public void onFailed(long token, AbstractAction action) {
        Message message = Message.obtain();
        Bundle params = new Bundle();
        params.putLong(KEY_TOKEN, token);
        params.putString(KEY_ACTION, action.mActionName);
        message.what = MSG_ON_FAILED;
        message.setData(params);
        mMainHandler.sendMessage(message);
    }

    private long getToken() {
        return System.currentTimeMillis();
    }

    public static class Editor {
        private File mInputFile;
        private File mOutputFile;
        public List<AbstractAction> mActionList = new ArrayList<>();
        public Iterator<AbstractAction> mActionIterator;
        public long mToken;
        public boolean mIsCanceled;

        VideoEditCallback mCallback;

        private Editor(File input) {
            this.mInputFile = input;
            mToken = sManager.getToken();
        }

        public Editor withAction(AbstractAction action) {
            if (action != null) {
                action.mToken = mToken;
                mActionList.add(action);
            }

            return this;
        }

        public Editor saveAs(File outputFile) {
            this.mOutputFile = outputFile;

            return this;
        }

        public long commit(VideoEditCallback callback) {
            // check if input file is rational.
            if (mInputFile == null ||
                    !mInputFile.exists() ||
                    !mInputFile.isFile()) {
                Logger.e(TAG, "Input video is illegal, input video: " + mInputFile);
                return Constants.INVALID_TOKEN;
            }

            if (mOutputFile == null) {
                Logger.e(TAG, "Output should not be null!");
                return Constants.INVALID_TOKEN;
            }
            if (mActionList.size() == 0) {
                Logger.e(TAG, "No edit action specified, please set at least one action!");
                return Constants.INVALID_TOKEN;
            }

            mCallback = callback;
            sManager.mCallMap.put(mToken, this);

            mActionIterator = mActionList.iterator();

            Logger.d(TAG, "Start edit, input file: " + mInputFile
                    + ", output file: " + mOutputFile
                    + ", action list: " + mActionList);

            // start from the first one.
            mActionIterator.next().start(mInputFile);

            return mToken;
        }
    }
}
