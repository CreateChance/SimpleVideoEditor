package com.createchance.simplevideoeditor;

import java.io.File;

/**
 * 音视频编辑抽象类，所有的编辑操作都是这个类的子类
 *
 * @author gaochao1-iri
 * @date 25/03/2018
 */

abstract class AbstractAction {

    private AbstractAction mSuccessNext;

    private final String mActionName;

    protected File mInputFile;
    protected File mOutputFile;

    AbstractAction(String actionName) {
        this.mActionName = actionName;
    }

    boolean checkRational() {
        return mInputFile != null &&
                mInputFile.exists() &&
                mInputFile.isFile();
    }

    void makeRational() {

    }

    /**
     * start this action.
     */
    void start(File inputFile) {
        this.mInputFile = inputFile;
        this.mOutputFile = genOutputFile();
    }

    void release() {
        if (mOutputFile != null && mOutputFile.exists()) {
            mOutputFile.delete();
        }
    }

    final void successNext(AbstractAction action) {
        mSuccessNext = action;
    }

    private void execNext() {
        // Our output file is the input of next action.
        if (mSuccessNext != null) {
            mSuccessNext.start(mOutputFile);
        } else {
            // We are the next action, rename our output to dest file.
            // TODO: What if renameTo return false??
            mOutputFile.renameTo(VideoEditorManager.getManager().getOutputFile());
        }
    }

    protected final void onStarted() {
        VideoEditorManager.getManager().onStart(mActionName);
    }

    protected final void onProgress(float progress) {
        VideoEditorManager.getManager().onProgress(mActionName, progress);
    }

    protected final void onSucceeded() {
        execNext();
        VideoEditorManager.getManager().onSucceed(mActionName);
    }

    protected final void onFailed() {
        VideoEditorManager.getManager().onFailed(mActionName);
    }

    protected final File getBaseWorkFolder() {
        return VideoEditorManager.getManager().getBaseWorkFolder();
    }

    private File genOutputFile() {
        return new File(VideoEditorManager.getManager().getBaseWorkFolder(), mActionName + ".tmp");
    }
}
