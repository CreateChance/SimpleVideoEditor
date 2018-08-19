package com.createchance.demo;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.createchance.simplevideoeditor.EditListener;
import com.createchance.simplevideoeditor.VideoEditorManager;
import com.createchance.simplevideoeditor.actions.VideoBgmAddAction;

import java.io.File;

public class AddBgmActivity extends AppCompatActivity implements View.OnClickListener {

    private static final String TAG = "AddBgmActivity";

    private static final int CODE_TAKE_VIDEO = 500;
    private static final int CODE_CHOOSE_BGM = 501;
    private static final int CODE_CHOOSE_VIDEO = 502;

    private TextView mVideoFileView, mBgnFileView, mProgressView;
    private EditText mVideoStartView, mVideoDurationView, mBgmStartView;

    private File mTargetVideoFile, mTargetBgmFile;

    private long mToken;

    private EditListener mEditListener = new EditListener() {
        @Override
        public void onStart(long token) {
            super.onStart(token);
            Log.d(TAG, "onStart: ");
        }


        @Override
        public void onProgress(long token, float progress) {
            super.onProgress(token, progress);

            Log.d(TAG, "onProgress: " + progress);
            mProgressView.setText("Progress: " + (progress * 100) + "%");
        }

        @Override
        public void onSucceeded(long token) {
            super.onSucceeded(token);
            Log.d(TAG, "onSucceeded: ");
        }

        @Override
        public void onFailed(long token) {
            super.onFailed(token);
            Log.d(TAG, "onFailed: ");
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.add_bgm);

        mVideoFileView = findViewById(R.id.tv_video_name);
        mBgnFileView = findViewById(R.id.tv_bgm_name);
        mVideoStartView = findViewById(R.id.et_video_start);
        mProgressView = findViewById(R.id.tv_progress);
        mVideoDurationView = findViewById(R.id.et_video_duration);
        mBgmStartView = findViewById(R.id.et_bgm_start);
        findViewById(R.id.btn_take_video).setOnClickListener(this);
        findViewById(R.id.btn_choose_video).setOnClickListener(this);
        findViewById(R.id.btn_choose_bgm).setOnClickListener(this);
        findViewById(R.id.btn_start).setOnClickListener(this);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case CODE_TAKE_VIDEO:
                if (resultCode == RESULT_OK) {
                    mVideoFileView.setText(mTargetVideoFile.getAbsolutePath());
                }
                break;
            case CODE_CHOOSE_BGM:
                if (resultCode == RESULT_OK) {
                    Uri uri = data.getData();
                    mTargetBgmFile = new File(uri.getPath());
                    mBgnFileView.setText(uri.getPath());
                }
                break;
            case CODE_CHOOSE_VIDEO:
                if (resultCode == RESULT_OK) {
                    Uri uri = data.getData();
                    mTargetVideoFile = new File(uri.getPath());
                    mVideoFileView.setText(uri.getPath());
                }
                break;
            default:
                break;
        }
    }

    public static void start(Context context) {
        Intent intent = new Intent(context, AddBgmActivity.class);
        context.startActivity(intent);
    }

    @Override
    public void onClick(View v) {
        Intent intent;
        switch (v.getId()) {
            case R.id.btn_take_video:
                intent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
                mTargetVideoFile = new File(Environment.getExternalStorageDirectory(), "DCIM/sve/bgmaddinput.mp4");
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {

                    Uri contentUri = FileProvider.getUriForFile(
                            this,
                            "com.createchance.demo.FileProvider",
                            mTargetVideoFile);
                    intent.putExtra(MediaStore.EXTRA_OUTPUT, contentUri);
                    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                } else {
                    intent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(mTargetVideoFile));
                }
                intent.putExtra(MediaStore.EXTRA_VIDEO_QUALITY, 1);
                startActivityForResult(intent, CODE_TAKE_VIDEO);
                break;
            case R.id.btn_choose_video:
                intent = new Intent();
                intent.setType("video/*");
                intent.setAction(Intent.ACTION_GET_CONTENT);
                startActivityForResult(intent, CODE_CHOOSE_VIDEO);
                break;
            case R.id.btn_choose_bgm:
                intent = new Intent();
                intent.setType("audio/*");
                intent.setAction(Intent.ACTION_GET_CONTENT);
                startActivityForResult(intent, CODE_CHOOSE_BGM);
                break;
            case R.id.btn_start:
                addBgm();
                break;
            default:
                break;
        }
    }

    private void addBgm() {
        if (mTargetVideoFile == null ||
                !mTargetVideoFile.exists() ||
                !mTargetVideoFile.isFile() ||
                mTargetBgmFile == null ||
                !mTargetBgmFile.exists() ||
                !mTargetBgmFile.isFile()) {
            Toast.makeText(this, "Invalid params!", Toast.LENGTH_SHORT).show();
            return;
        }

        long videoStart = getLong(((EditText) findViewById(R.id.et_video_start)).getText().toString());
        long videoDuration = getLong(((EditText) findViewById(R.id.et_video_duration)).getText().toString());
        long bgmStart = getLong(((EditText) findViewById(R.id.et_bgm_start)).getText().toString());
        VideoBgmAddAction bgmAddAction = new VideoBgmAddAction.Builder()
                .input(mTargetVideoFile)
                .output(new File(Environment.getExternalStorageDirectory(), "DCIM/sve/bgmaddoutput.mp4"))
                .videoFrom(videoStart)
                .videoDuration(videoDuration)
                .bgmFile(mTargetBgmFile)
                .bgmFrom(bgmStart)
                .build();
        VideoEditorManager.getManager().edit()
                .withAction(bgmAddAction)
                .commit(mEditListener);
    }

    private long getLong(String value) {
        return Long.valueOf(TextUtils.isEmpty(value) ? "0" : value);
    }
}
