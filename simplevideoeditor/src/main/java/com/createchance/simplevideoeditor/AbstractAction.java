package com.createchance.simplevideoeditor;

/**
 * 音视频编辑抽象类，所有的编辑操作都是这个类的子类
 *
 * @author gaochao1-iri
 * @date 25/03/2018
 */

public abstract class AbstractAction {
    protected ActionCallback mCallback;

    public void start(ActionCallback callback) {
        this.mCallback = callback;
    }

    public void release() {
        this.mCallback = null;
    }
}
