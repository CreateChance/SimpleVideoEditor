package com.createchance.simplevideoeditor;

/**
 * ${DESC}
 *
 * @author gaochao1-iri
 * @date 27/03/2018
 */

public interface ActionCallback {
    void onStarted(int event);

    void onProgress(int event, float progress);

    void onSuccess(int event);

    void onFailed(int event);
}
