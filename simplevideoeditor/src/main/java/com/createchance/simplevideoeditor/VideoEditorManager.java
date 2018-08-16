package com.createchance.simplevideoeditor;

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
 * @author createchance
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

    private final int MSG_ON_STAGE_START = 200;
    private final int MSG_ON_STAGE_PROGRESS = 201;
    private final int MSG_ON_STAGE_SUCCEED = 202;
    private final int MSG_ON_STAGE_FAILED = 203;
    private final String KEY_TOKEN = "token";
    private final String KEY_ACTION = "action";
    private final String KEY_STAGE_PROGRESS = "stage_progress";
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
                    case MSG_ON_STAGE_START:
                        if (editor.mEditStageListener != null) {
                            editor.mEditStageListener.onStart(params.getString(KEY_ACTION));
                        }
                        break;
                    case MSG_ON_START:
                        if (editor.mEditListener != null) {
                            editor.mEditListener.onStart(token);
                        }
                        break;
                    case MSG_ON_STAGE_PROGRESS:
                        if (editor.mEditStageListener != null) {
                            editor.mEditStageListener.onProgress(params.getString(KEY_ACTION),
                                    params.getFloat(KEY_STAGE_PROGRESS));
                        }
                        break;
                    case MSG_ON_PROGRESS:
                        if (editor.mEditListener != null) {
                            editor.mEditListener.onProgress(token, params.getFloat(KEY_PROGRESS));
                        }
                        break;
                    case MSG_ON_STAGE_SUCCEED:
                        if (editor.mEditStageListener != null) {
                            editor.mEditStageListener.onSucceeded(params.getString(KEY_ACTION));
                        }
                        break;
                    case MSG_ON_SUCCEED:
                        if (editor.mEditListener != null) {
                            editor.mEditListener.onSucceeded(token, editor.mOutputFile);
                        }
                        // clear all.
                        clear(token);
                        break;
                    case MSG_ON_STAGE_FAILED:
                        // clean all the tmp files.
                        for (AbstractAction act : editor.mActionList) {
                            act.release();
                        }

                        if (editor.mEditStageListener != null) {
                            editor.mEditStageListener.onFailed(params.getString(KEY_ACTION));
                        }
                        break;
                    case MSG_ON_FAILED:
                        if (editor.mEditListener != null) {
                            editor.mEditListener.onFailed(token);
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

    public synchronized void init(Context context) {
        this.mContext = context.getApplicationContext();
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
        // notify stage
        Message stageMsg = Message.obtain();
        Bundle stageParams = new Bundle();
        stageParams.putLong(KEY_TOKEN, token);
        stageParams.putString(KEY_ACTION, action.mActionName);
        stageMsg.what = MSG_ON_STAGE_START;
        stageMsg.setData(stageParams);
        mMainHandler.sendMessage(stageMsg);
        // notify overall
        Editor editor = mCallMap.get(token);
        // send overall start msg only when the first action started.
        if (editor.mActionList.indexOf(action) == 0) {
            Message overallMsg = Message.obtain();
            Bundle overallParams = new Bundle();
            overallParams.putLong(KEY_TOKEN, token);
            overallMsg.what = MSG_ON_START;
            overallMsg.setData(overallParams);
            mMainHandler.sendMessage(overallMsg);
        }
    }

    public void onProgress(long token, AbstractAction action, float progress) {
        Editor editor = mCallMap.get(token);
        if (editor == null) {
            return;
        }
        // notify stage
        Message stageMsg = Message.obtain();
        Bundle stageParams = new Bundle();
        stageParams.putLong(KEY_TOKEN, token);
        stageParams.putString(KEY_ACTION, action.mActionName);
        stageParams.putFloat(KEY_STAGE_PROGRESS, progress);
        stageMsg.what = MSG_ON_STAGE_PROGRESS;
        stageMsg.setData(stageParams);
        mMainHandler.sendMessage(stageMsg);
        // notify overall
        Message overallMsg = Message.obtain();
        Bundle overallParams = new Bundle();
        overallParams.putLong(KEY_TOKEN, token);
        int lastPos = editor.mActionList.indexOf(action);
        overallParams.putFloat(KEY_PROGRESS, (lastPos * 1.0f + progress) / editor.mActionList.size());
        overallMsg.what = MSG_ON_PROGRESS;
        overallMsg.setData(overallParams);
        mMainHandler.sendMessage(overallMsg);
    }

    public void onSucceed(long token, AbstractAction action) {
        Editor editor = mCallMap.get(token);
        if (editor.mIsCanceled) {
            Logger.d(TAG, "This editor is canceled!");
            // this editor is canceled, so remove it's output and no callback.
            clear(token);
        } else {
            // notify stage
            Message stageMsg = Message.obtain();
            Bundle stageParams = new Bundle();
            stageParams.putLong(KEY_TOKEN, token);
            stageParams.putString(KEY_ACTION, action.mActionName);
            stageMsg.what = MSG_ON_STAGE_SUCCEED;
            stageMsg.setData(stageParams);
            mMainHandler.sendMessage(stageMsg);

            // try exec next action.
            if (editor.mActionIterator.hasNext()) {
                // our output is the input of next action.
                editor.mActionIterator.next().start(action.mOutputFile);
            } else {
                action.mOutputFile.renameTo(editor.mOutputFile);
                // notify overall
                Message overallMsg = Message.obtain();
                Bundle overallParams = new Bundle();
                overallParams.putLong(KEY_TOKEN, token);
                overallMsg.what = MSG_ON_SUCCEED;
                overallMsg.setData(overallParams);
                mMainHandler.sendMessage(overallMsg);
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
        // notify stage
        Message stageMsg = Message.obtain();
        Bundle stageParams = new Bundle();
        stageParams.putLong(KEY_TOKEN, token);
        stageParams.putString(KEY_ACTION, action.mActionName);
        stageMsg.what = MSG_ON_STAGE_FAILED;
        stageMsg.setData(stageParams);
        mMainHandler.sendMessage(stageMsg);
        // notify overall
        Message overallMsg = Message.obtain();
        Bundle overallParams = new Bundle();
        overallParams.putLong(KEY_TOKEN, token);
        overallMsg.what = MSG_ON_FAILED;
        overallMsg.setData(overallParams);
        mMainHandler.sendMessage(overallMsg);
    }

    private long getToken() {
        // just take current time as our token.
        return System.currentTimeMillis();
    }

    public static class Editor {
        private File mInputFile;
        private File mOutputFile;
        public List<AbstractAction> mActionList = new ArrayList<>();
        public Iterator<AbstractAction> mActionIterator;
        public long mToken;
        public boolean mIsCanceled;

        EditListener mEditListener;
        EditStageListener mEditStageListener;

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

        public long commit(EditListener listener, EditStageListener stageListener) {
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

            mEditListener = listener;
            mEditStageListener = stageListener;
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
