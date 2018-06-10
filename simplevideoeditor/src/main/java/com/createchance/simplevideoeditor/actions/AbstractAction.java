package com.createchance.simplevideoeditor.actions;

import com.createchance.simplevideoeditor.VideoEditorManager;

import java.io.File;

/**
 * 音视频编辑抽象类，所有的编辑操作都是这个类的子类
 *
 * @author gaochao1-iri
 * @date 25/03/2018
 */

public abstract class AbstractAction {
    public final String mActionName;

    public File mInputFile;
    public File mOutputFile;

    public long mToken;

    AbstractAction(String actionName) {
        this.mActionName = actionName;
    }

    /**
     * start this action.
     */
    public void start(File inputFile) {
        this.mInputFile = inputFile;
        this.mOutputFile = genOutputFile();
    }

    public void release() {
        if (mOutputFile != null && mOutputFile.exists()) {
            mOutputFile.delete();
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

    protected final File getBaseWorkFolder() {
        return VideoEditorManager.getManager().getBaseWorkFolder(mToken);
    }

    private File genOutputFile() {
        return new File(VideoEditorManager.getManager().getBaseWorkFolder(mToken), mActionName + ".tmp");
    }
}
