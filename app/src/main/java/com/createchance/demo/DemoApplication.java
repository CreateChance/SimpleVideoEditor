package com.createchance.demo;

import android.app.Application;
import android.content.Context;

import com.createchance.simplevideoeditor.VideoEditorManager;

/**
 * ${DESC}
 *
 * @author createchance
 * @date 03/05/2018
 */
public class DemoApplication extends Application {

    private static Context mContext;

    @Override
    public void onCreate() {
        super.onCreate();

        mContext = this;

        VideoEditorManager.getManager().init(this);
    }

    public static Context getContext() {
        return mContext;
    }
}
