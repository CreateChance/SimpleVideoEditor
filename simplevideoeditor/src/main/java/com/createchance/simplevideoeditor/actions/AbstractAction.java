package com.createchance.simplevideoeditor.actions;

import com.createchance.simplevideoeditor.VideoEditorManager;

import java.io.File;

/**
 * 音视频编辑抽象类，所有的编辑操作都是这个类的子类
 *
 * @author createchance
 * @date 25/03/2018
 */

public abstract class AbstractAction {
    public final String mActionName;

    protected File mInputFile;
    protected File mOutputFile;

    public long mToken;

    AbstractAction(String actionName) {
        this.mActionName = actionName;
    }

    /**
     * start this action.
     */
    public abstract void start();

    public void release() {
        if (mOutputFile != null && mOutputFile.exists()) {
//            mOutputFile.delete();
        }
    }

    protected final void onStarted() {
        VideoEditorManager.getManager().onStart(mToken, this);
    }

    protected final void onProgress(float progress) {
        VideoEditorManager.getManager().onProgress(mToken, this, progress);
    }

    protected final void onSucceeded() {
        VideoEditorManager.getManager().onSucceed(mToken, this);
    }

    protected final void onFailed() {
        VideoEditorManager.getManager().onFailed(mToken, this);
    }

    public File getInputFile() {
        return mInputFile;
    }

    public File getOutputFile() {
        return mOutputFile;
    }
}
