package com.createchance.simplevideoeditor;

/**
 * 音视频编辑抽象类，所有的编辑操作都是这个类的子类
 *
 * @author gaochao1-iri
 * @date 25/03/2018
 */

abstract class AbstractAction {

    private AbstractAction mSuccessNext;

    abstract void start();

    void release() {
    }

    final void successNext(AbstractAction action) {
        mSuccessNext = action;
    }

    protected final void execNext() {
        if (mSuccessNext != null) {
            mSuccessNext.start();
        }
    }

    protected final void onStarted(int stage) {
        if (VideoEditorManager.getManager().getCallback() != null) {
            VideoEditorManager.getManager().getCallback().onStart(stage);
        }
    }

    protected final void onProgress(int stage, float progress) {
        if (VideoEditorManager.getManager().getCallback() != null) {
            VideoEditorManager.getManager().getCallback().onProgress(stage, progress);
        }
    }

    protected final void onSucceeded(int stage) {
        if (VideoEditorManager.getManager().getCallback() != null) {
            VideoEditorManager.getManager().getCallback().onSucceeded(stage);
        }
    }

    protected final void onFailed(int stage) {
        if (VideoEditorManager.getManager().getCallback() != null) {
            VideoEditorManager.getManager().getCallback().onFialed(stage);
        }
    }
}
