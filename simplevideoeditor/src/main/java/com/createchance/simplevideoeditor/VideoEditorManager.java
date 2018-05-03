package com.createchance.simplevideoeditor;

import android.app.Application;
import android.content.Context;

/**
 * ${DESC}
 *
 * @author gaochao1-iri
 * @date 02/05/2018
 */
public class VideoEditorManager {
    private static VideoEditorManager sManager;

    private Context mContext;

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
}
