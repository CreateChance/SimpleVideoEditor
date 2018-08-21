package com.createchance.demo.views;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.Switch;
import android.widget.TextView;

import com.createchance.PlayRequest;
import com.createchance.SimpleVideoPlayer;
import com.createchance.demo.R;
import com.createchance.demo.model.Scene;
import com.createchance.demo.model.SimpleModelManager;
import com.createchance.simplevideoeditor.Constants;
import com.createchance.simplevideoeditor.actions.AbstractAction;
import com.createchance.simplevideoeditor.actions.VideoBgmAddAction;
import com.createchance.simplevideoeditor.actions.VideoBgmRemoveAction;
import com.createchance.simplevideoeditor.actions.VideoCutAction;
import com.createchance.simplevideoeditor.actions.VideoFilterAddAction;
import com.createchance.simplevideoeditor.gles.VideoFrameLookupFilter;
import com.createchance.simplevideoeditor.gles.WaterMarkFilter;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class VideoEditActivity extends Activity implements View.OnClickListener {

    private static final String EXTRA_SCENE_INDEX = "scene_index";

    private List<AbstractAction> mActionList = new ArrayList<>();

    private Scene mScene;

    private SimpleVideoPlayer mVideoPlayer;

    private TextView mScriptView;
    private ViewPager mEditPanel;
    private List<View> mEditPanelList = new ArrayList<>();
    private PagerAdapter pagerAdapter = new PagerAdapter() {
        @Override
        public int getCount() {
            return mEditPanelList.size();
        }

        @Override
        public boolean isViewFromObject(@NonNull View view, @NonNull Object object) {
            return view == object;
        }

        @NonNull
        @Override
        public Object instantiateItem(@NonNull ViewGroup container, int position) {
            container.addView(mEditPanelList.get(position));
            return mEditPanelList.get(position);
        }

        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {
            container.removeView(mEditPanelList.get(position));
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_edit);

        mVideoPlayer = findViewById(R.id.vw_video_player);
        mScriptView = findViewById(R.id.tv_scene_script);
        findViewById(R.id.iv_back).setOnClickListener(this);
        findViewById(R.id.iv_edit).setOnClickListener(this);
        mEditPanel = findViewById(R.id.vw_edit_panel);
        initEditPanel();
        mEditPanel.setAdapter(pagerAdapter);

        Intent intent = getIntent();
        if (intent != null) {
            mScene = SimpleModelManager.getInstance().
                    getSceneList().get(intent.getIntExtra(EXTRA_SCENE_INDEX, 0));
            PlayRequest playRequest = new PlayRequest.Builder()
                    .loop(true)
                    .source(Uri.fromFile(mScene.video))
                    .build();
            mVideoPlayer.start(playRequest);
            mScriptView.setText(mScene.script);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();

        mVideoPlayer.resume();
    }

    @Override
    protected void onStop() {
        super.onStop();

        mVideoPlayer.pause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        mVideoPlayer.stop();
    }

    @Override
    public void onBackPressed() {
        View bgmAdd = mEditPanelList.get(0);
        View bgmRemove = mEditPanelList.get(1);
        View addFilter = mEditPanelList.get(2);
        View cut = mEditPanelList.get(3);
        VideoBgmAddAction bgmAddAction = null;
        if (((Switch) bgmAdd.findViewById(R.id.sw_bgm_add_enabled)).isChecked()) {
            long bgmStart = getLong(((EditText) bgmAdd.findViewById(R.id.et_bgm_add_bgm_start)).getText().toString());
            long bgmDuration = getLong(((EditText) bgmAdd.findViewById(R.id.et_bgm_add_bgm_duration)).getText().toString());
            long videoStart = getLong(((EditText) bgmAdd.findViewById(R.id.et_bgm_add_video_start)).getText().toString());
            long videoDuration = getLong(((EditText) bgmAdd.findViewById(R.id.et_bgm_add_video_duration)).getText().toString());
            File bgmFile = new File(((TextView) bgmAdd.findViewById(R.id.tv_bgm_file)).getText().toString());
            bgmAddAction = new VideoBgmAddAction.Builder()
                    .input(mScene.video)
                    .output(new File(mScene.video.getParent(), Constants.ACTION_ADD_BGM + ".mp4"))
                    .bgmFrom(bgmStart)
                    .bgmDuration(bgmDuration)
                    .videoFrom(videoStart)
                    .videoDuration(videoDuration)
                    .bgmFile(bgmFile)
                    .build();
            mActionList.add(bgmAddAction);
        }
        VideoBgmRemoveAction bgmRemoveAction = null;
        if (((Switch) bgmRemove.findViewById(R.id.sw_bgm_remove_enabled)).isChecked()) {
            long videoStart = getLong(((EditText) bgmRemove.findViewById(R.id.et_bgm_remove_video_start)).getText().toString());
            long videoDuration = getLong(((EditText) bgmRemove.findViewById(R.id.et_bgm_remove_video_duration)).getText().toString());
            File input = bgmAddAction == null ? mScene.video : bgmAddAction.getOutputFile();
            bgmRemoveAction = new VideoBgmRemoveAction.Builder()
                    .from(videoStart)
                    .duration(videoDuration)
                    .input(input)
                    .output(new File(input.getParent(), Constants.ACTION_REMOVE_BGM + ".mp4"))
                    .build();
            mActionList.add(bgmRemoveAction);
        }
        VideoFilterAddAction filterAddAction = null;
        if (((Switch) addFilter.findViewById(R.id.sw_add_filter_enabled)).isChecked()) {
            Bitmap watermark = BitmapFactory.decodeFile(((TextView) addFilter.findViewById(R.id.tv_watermark_file)).getText().toString());
            Bitmap filter = null;
            int checkedFilterId = ((RadioGroup) addFilter.findViewById(R.id.rg_filters)).getCheckedRadioButtonId();
            switch (checkedFilterId) {
                case 0:
                    filter = BitmapFactory.decodeResource(getResources(), R.drawable.filter1);
                    break;
                case 1:
                    filter = BitmapFactory.decodeResource(getResources(), R.drawable.filter2);
                    break;
                case 2:
                    filter = BitmapFactory.decodeResource(getResources(), R.drawable.filter3);
                    break;
                case 3:
                    filter = BitmapFactory.decodeResource(getResources(), R.drawable.filter4);
                    break;
                case 4:
                    filter = BitmapFactory.decodeResource(getResources(), R.drawable.filter5);
                    break;
                case 5:
                    filter = BitmapFactory.decodeResource(getResources(), R.drawable.filter6);
                    break;
                case 6:
                    filter = BitmapFactory.decodeResource(getResources(), R.drawable.filter7);
                    break;
                case 7:
                    filter = BitmapFactory.decodeResource(getResources(), R.drawable.filter8);
                    break;
                case 8:
                    filter = BitmapFactory.decodeResource(getResources(), R.drawable.filter9);
                    break;
                case 9:
                    filter = BitmapFactory.decodeResource(getResources(), R.drawable.filter10);
                    break;
                case 10:
                    filter = BitmapFactory.decodeResource(getResources(), R.drawable.filter11);
                    break;
                default:
                    break;
            }

            WaterMarkFilter waterMarkFilter = null;
            if (watermark != null) {
                waterMarkFilter = new WaterMarkFilter.Builder()
                        .startFrom(0L)
                        .duration(0L)
                        .position(100, 200)
                        .watermark(watermark)
                        .scaleFactor(1.0f)
                        .build();
            }
            VideoFrameLookupFilter lookupFilter = null;
            if (filter != null) {
                lookupFilter = new VideoFrameLookupFilter.Builder()
                        .startFrom(0L)
                        .duration(0L)
                        .curve(filter)
                        .strength(1.0f)
                        .build();
            }

            VideoFilterAddAction.Builder filterAddActionBuilder = new VideoFilterAddAction.Builder();
            if (waterMarkFilter != null) {
                filterAddActionBuilder.watermarkFilter(waterMarkFilter);
            }
            if (lookupFilter != null) {
                filterAddActionBuilder.frameFilter(lookupFilter);
            }
            File input = bgmRemoveAction == null ? mScene.video : bgmRemoveAction.getOutputFile();
            filterAddActionBuilder.input(input)
                    .output(new File(input.getParent(), Constants.ACTION_ADD_FILTER + ".mp4"));
            filterAddAction = filterAddActionBuilder.build();
            mActionList.add(filterAddAction);
        }

        VideoCutAction cutAction = null;
        if (((Switch) cut.findViewById(R.id.sw_cut_enabled)).isChecked()) {
            long videoStart = getLong(((EditText) cut.findViewById(R.id.et_cut_start)).getText().toString());
            long videoDuration = getLong(((EditText) cut.findViewById(R.id.et_cut_duration)).getText().toString());
            File input = filterAddAction == null ? mScene.video : filterAddAction.getOutputFile();
            cutAction = new VideoCutAction.Builder()
                    .input(input)
                    .output(new File(input.getParent(), Constants.ACTION_CUT_VIDEO + ".mp4"))
                    .from(videoStart)
                    .duration(videoDuration)
                    .build();
            mActionList.add(cutAction);
        }

        SimpleModelManager.getInstance().setEditActionList(mActionList);

        super.onBackPressed();
    }

    public static void start(Context context, int sceneIndex) {
        Intent intent = new Intent(context, VideoEditActivity.class);
        intent.putExtra(EXTRA_SCENE_INDEX, sceneIndex);
        context.startActivity(intent);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.iv_back:
                onBackPressed();
                break;
            case R.id.iv_edit:
                // switch edit panel.
                if (mEditPanel.getVisibility() == View.VISIBLE) {
                    mEditPanel.setVisibility(View.GONE);
                } else {
                    mEditPanel.setVisibility(View.VISIBLE);
                }
                break;
            default:
                break;
        }
    }

    private void initEditPanel() {
        View bgmAdd = getLayoutInflater().inflate(R.layout.edit_panel_add_bgm, null);
        View bgmRemove = getLayoutInflater().inflate(R.layout.edit_panel_remove_bgm, null);
        View addFilter = getLayoutInflater().inflate(R.layout.edit_panel_add_filter_and_watermark, null);
        View cut = getLayoutInflater().inflate(R.layout.edit_panel_cut, null);
        mEditPanelList.add(bgmAdd);
        mEditPanelList.add(bgmRemove);
        mEditPanelList.add(addFilter);
        mEditPanelList.add(cut);
    }

    private long getLong(String value) {
        return Long.valueOf(TextUtils.isEmpty(value) ? "0" : value);
    }
}
