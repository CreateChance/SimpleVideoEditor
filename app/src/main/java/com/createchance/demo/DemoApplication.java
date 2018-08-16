package com.createchance.demo;

import android.app.Application;

import com.createchance.simplevideoeditor.VideoEditorManager;

/**
 * ${DESC}
 *
 * @author createchance
 * @date 03/05/2018
 */
public class DemoApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();

        VideoEditorManager.getManager().init(this);
    }
}
