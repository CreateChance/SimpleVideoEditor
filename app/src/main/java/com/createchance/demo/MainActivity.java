package com.createchance.demo;

import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.createchance.simplevideoeditor.Constants;
import com.createchance.simplevideoeditor.EditListener;
import com.createchance.simplevideoeditor.EditStageListener;
import com.createchance.simplevideoeditor.VideoEditorManager;
import com.createchance.simplevideoeditor.actions.VideoBgmAddAction;
import com.createchance.simplevideoeditor.actions.VideoBgmRemoveAction;
import com.createchance.simplevideoeditor.actions.VideoCutAction;
import com.createchance.simplevideoeditor.actions.VideoFilterAddAction;
import com.createchance.simplevideoeditor.actions.VideoMergeAction;
import com.createchance.simplevideoeditor.gles.VideoFrameLookupFilter;
import com.createchance.simplevideoeditor.gles.WaterMarkFilter;

import java.io.File;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";

    private TextView mActionView;
    private TextView mProgressView;
    private long mToken = -1;

    private EditStageListener mStageListener = new EditStageListener() {
        @Override
        public void onStart(String action) {
            super.onStart(action);
//            Log.d(TAG, "EditStageListener, onStart: " + action);
            mActionView.setText(action + ": started");
        }

        @Override
        public void onProgress(String action, float progress) {
            super.onProgress(action, progress);
//            Log.d(TAG, "EditStageListener, onProgress: " + action + ", progress: " + progress);
            mActionView.setText(action + ": progress");
            mProgressView.setText(String.format("%.2f%%", progress * 100));
        }

        @Override
        public void onSucceeded(String action) {
            super.onSucceeded(action);
//            Log.d(TAG, "EditStageListener, onSucceeded: " + action);
            mActionView.setText(action + ": succeed");
            if (Constants.ACTION_MERGE_VIDEOS.equals(action)) {
                mToken = -1;
            }
        }

        @Override
        public void onFailed(String action) {
            super.onFailed(action);
//            Log.d(TAG, "EditStageListener, onFailed: " + action);
            mActionView.setText(action + ": failed");
        }
    };

    private EditListener mEditListener = new EditListener() {
        @Override
        public void onStart(long token) {
            super.onStart(token);
//            Log.d(TAG, "EditListener, onStart token: " + token);
        }

        @Override
        public void onProgress(long token, float progress) {
            super.onProgress(token, progress);
//            Log.d(TAG, "EditListener, onProgress: " + progress + ", token: " + token);
        }

        @Override
        public void onSucceeded(long token, File outputFile) {
            super.onSucceeded(token, outputFile);
//            Log.d(TAG, "EditListener, onSucceeded token: " + token + ", output: " + outputFile);
            mToken = -1;
        }

        @Override
        public void onFailed(long token) {
            super.onFailed(token);
//            Log.d(TAG, "EditListener, onFailed token: " + token);
            mToken = -1;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mActionView = findViewById(R.id.tv_action);
        mProgressView = findViewById(R.id.tv_progress);
    }

    public void start(View view) {
        if (mToken != -1) {
            Log.e(TAG, "We are on going!!!!!!");
            return;
        }

        // filter
        WaterMarkFilter waterMarkFilter = new WaterMarkFilter.Builder()
                .watermark(BitmapFactory.decodeResource(getResources(), R.drawable.watermark))
                .position(100, 200)
                .scaleFactor(1f)
                .startFrom(5 * 1000)
                .duration(5 * 1000)
                .build();
        VideoFrameLookupFilter lookupFilter = new VideoFrameLookupFilter.Builder()
                .curve(BitmapFactory.decodeResource(getResources(), R.drawable.filter9))
                .strength(1f)
                .startFrom(5 * 1000)
                .duration(5 * 1000)
                .build();
        VideoFilterAddAction filterAddAction = new VideoFilterAddAction.Builder()
                .watermarkFilter(waterMarkFilter)
                .frameFilter(lookupFilter)
                .build();

        // bgm add
        VideoBgmAddAction bgmAddAction = new VideoBgmAddAction.Builder()
                .bgmFile(new File(Environment.getExternalStorageDirectory(), "videoeditor/music_48k.mp3"))
//                .videoFrom(5 * 1000)
                .videoDuration(7 * 1000)
                .override(true)
//                .bgmFrom(5 * 1000)
                .build();

        // bgm remove
        VideoBgmRemoveAction bgmRemoveAction = new VideoBgmRemoveAction.Builder()
                .from(3 * 1000)
                .duration(8 * 1000)
                .build();

        // cut
        VideoCutAction cutAction = new VideoCutAction.Builder()
                .from(5 * 1000)
                .duration(10 * 1000)
                .build();

        // merge
        VideoMergeAction mergeAction = new VideoMergeAction.Builder()
                .merge(new File(Environment.getExternalStorageDirectory(), "videoeditor/input.mp4"))
                .inputHere()
//                .merge(new File(Environment.getExternalStorageDirectory(), "/DCIM/Camera/VID_20180330_181524.mp4"))
//                .merge(new File(Environment.getExternalStorageDirectory(), "/DCIM/Camera/VID_20180401_103412.mp4"))
//                .merge(new File(Environment.getExternalStorageDirectory(), "/DCIM/Camera/VID_20180404_104840.mp4"))
                .build();

        // start it!
        mToken = VideoEditorManager.getManager()
                .edit(new File(Environment.getExternalStorageDirectory(), "videoeditor/input2.mp4"))
//                .withAction(cutAction)
//                .withAction(bgmAddAction)
                .withAction(filterAddAction)
//                .withAction(bgmRemoveAction)
//                .withAction(mergeAction)
                .saveAs(new File(Environment.getExternalStorageDirectory(), "videoeditor/output.mp4"))
                .commit(mEditListener, mStageListener);
    }

    public void cancel(View view) {
        // cancel edit.
        if (mToken != -1) {
            VideoEditorManager.getManager().cancel(mToken);
        }
    }
}
