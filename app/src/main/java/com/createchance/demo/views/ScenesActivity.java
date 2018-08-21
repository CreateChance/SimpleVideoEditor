package com.createchance.demo.views;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v4.content.FileProvider;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.TextView;

import com.createchance.demo.R;
import com.createchance.demo.model.Scene;
import com.createchance.demo.model.SimpleModelManager;
import com.createchance.demo.model.Template;

import java.io.File;

public class ScenesActivity extends Activity implements View.OnClickListener,
        SceneListAdapter.VideoHandler {

    private static final String EXTRA_IS_CUSTOM = "extra_is_custom";
    private final int CODE_PERMISSION_REQUEST = 500;
    private final int CODE_TAKE_VIDEO_REQUEST = 501;

    private Template mTemplate;
    private boolean mIsCustom;

    private RecyclerView mSceneListView;
    private SceneListAdapter mSceneListAdapter;

    private int mCurrentSceneIndex;

    private SimpleDialog mExitDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scenes);

        if (Build.VERSION.SDK_INT >= 23) {
            if (checkSelfPermission(Manifest.permission.CAMERA) !=
                    PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.CAMERA},
                        CODE_PERMISSION_REQUEST);
            }
        }

        Intent intent = getIntent();
        if (intent != null) {
            mTemplate = SimpleModelManager.getInstance().getCurrentTemplate();
            mIsCustom = intent.getBooleanExtra(EXTRA_IS_CUSTOM, true);
        }

        TextView title = findViewById(R.id.tv_title);
        if (mIsCustom) {
            title.setText(R.string.scenes_ui_custom_title);
        } else {
            title.setText(R.string.scenes_ui_template_title);
        }

        mSceneListView = findViewById(R.id.rcv_scene_list);
        mSceneListAdapter = new SceneListAdapter(this, mTemplate, mIsCustom, this);
        mSceneListView.setLayoutManager(new GridLayoutManager(this, 2));
        mSceneListView.setAdapter(mSceneListAdapter);
        findViewById(R.id.iv_back).setOnClickListener(this);
    }

    @Override
    protected void onStart() {
        super.onStart();
        mSceneListAdapter.notifyDataSetChanged();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == CODE_TAKE_VIDEO_REQUEST && resultCode == RESULT_OK) {
            mSceneListAdapter.notifyDataSetChanged();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        // clear scene list.
        SimpleModelManager.getInstance().setSceneList(null);
    }

    @Override
    public void onBackPressed() {
        if (hasVideo()) {
            showExitConfirmDialog();
        } else {
            super.onBackPressed();
        }
    }

    public static void start(Context context, Template template, boolean isCustom) {
        Intent intent = new Intent(context, ScenesActivity.class);
        intent.putExtra(EXTRA_IS_CUSTOM, isCustom);
        context.startActivity(intent);

        SimpleModelManager.getInstance().setCurrentTemplate(template);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.iv_back:
                onBackPressed();
                break;
            default:
                break;
        }
    }

    @Override
    public void takeVideo(int sceneIndex) {
        mCurrentSceneIndex = sceneIndex;
        Scene scene = SimpleModelManager.getInstance().getSceneList().get(sceneIndex);
        Intent intent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);

        scene.video = new File(getFilesDir(), System.currentTimeMillis() + ".mp4");
        Uri videoUri;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            //申请权限
            intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            //getUriForFile的第二个参数就是Manifest中的authorities
            videoUri = FileProvider.getUriForFile(
                    this,
                    "com.createchance.demo.fileProvider",
                    scene.video);
        } else {
            videoUri = Uri.fromFile(scene.video);
        }
        intent.putExtra(MediaStore.EXTRA_OUTPUT, videoUri);
        //设置保存视频文件的质量
        intent.putExtra(MediaStore.EXTRA_VIDEO_QUALITY, 1);
        startActivityForResult(intent, CODE_TAKE_VIDEO_REQUEST);
    }

    @Override
    public void editVideo(int sceneIndex) {
        mCurrentSceneIndex = sceneIndex;
        Scene scene = SimpleModelManager.getInstance().getSceneList().get(sceneIndex);
        VideoEditActivity.start(this, sceneIndex);
    }

    private boolean hasVideo() {
        for (Scene scene : SimpleModelManager.getInstance().getSceneList()) {
            if (scene.video != null && scene.video.exists() && scene.video.isFile()) {
                return true;
            }
        }

        return false;
    }

    private void showExitConfirmDialog() {
        mExitDialog = new SimpleDialog.Builder(this)
                .setCancelTouchout(false)
                .setContentText(R.string.scene_delete_all_video_dialog_content)
                .setCancelText(R.string.scene_delete_dialog_cancel)
                .setConfirmText(R.string.scene_delete_dialog_confirm)
                .setConfirmOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        for (Scene scene : SimpleModelManager.getInstance().getSceneList()) {
                            if (scene.video != null && scene.video.exists() && scene.video.isFile()) {
                                scene.video.delete();
                                scene.video = null;
                            }
                        }
                        mExitDialog.dismiss();
                        finish();
                    }
                }).build();
        mExitDialog.show();
    }
}
