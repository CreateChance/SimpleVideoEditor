<p align="center"><img src="logo/1.png" alt="SimpleVideoEditor" height="210px"></p>

# SimpleVideoEditor
Video editor library for android, implemented with android media framework and opengl es 2.0.

The aim of this library is:
1. tiny size(no ffmpeg)
2. simple api
3. powerful feature
# *NOTE*
This library is still under development, apis will be changed.

Any suggestion is welcome, you can raise issues or pull requests, thx.
# How to use
## Define progress listeners:
```java
// EditStageListener use to listener every stage progresss.
private EditStageListener mStageListener = new EditStageListener() {
    @Override
    public void onStart(String action) {
        super.onStart(action);
        Log.d(TAG, "EditStageListener, onStart: " + action);
        mActionView.setText(action + ": started");
    }

    @Override
    public void onProgress(String action, float progress) {
        super.onProgress(action, progress);
        Log.d(TAG, "EditStageListener, onProgress: " + action + ", progress: " + progress);
        mActionView.setText(action + ": progress");
        mProgressView.setText(String.format("%.2f%%", progress * 100));
    }

    @Override
    public void onSucceeded(String action) {
        super.onSucceeded(action);
        Log.d(TAG, "EditStageListener, onSucceeded: " + action);
        mActionView.setText(action + ": succeed");
        if (Constants.ACTION_MERGE_VIDEOS.equals(action)) {
            mToken = -1;
        }
    }

    @Override
    public void onFailed(String action) {
        super.onFailed(action);
        Log.d(TAG, "EditStageListener, onFailed: " + action);
        mActionView.setText(action + ": failed");
    }
};
// EditListener use to listen overall progress.
private EditListener mEditListener = new EditListener() {
    @Override
    public void onStart(long token) {
        super.onStart(token);
        Log.d(TAG, "EditListener, onStart token: " + token);
    }

    @Override
    public void onProgress(long token, float progress) {
        super.onProgress(token, progress);
        Log.d(TAG, "EditListener, onProgress: " + progress + ", token: " + token);
    }

    @Override
    public void onSucceeded(long token, File outputFile) {
        super.onSucceeded(token, outputFile);
        Log.d(TAG, "EditListener, onSucceeded token: " + token + ", output: " + outputFile);
        mToken = -1;
    }

    @Override
    public void onFailed(long token) {
        super.onFailed(token);
        Log.d(TAG, "EditListener, onFailed token: " + token);
        mToken = -1;
    }
};
```
## Define actions
```java
// video water mark config
WaterMarkFilter waterMarkFilter = new WaterMarkFilter.Builder()
        .watermark(BitmapFactory.decodeResource(getResources(), R.drawable.watermark))
        .position(100, 200)
        .scaleFactor(1f)
        .startFrom(5 * 1000)
        .duration(5 * 1000)
        .build();
// video filter config.
VideoFrameLookupFilter lookupFilter = new VideoFrameLookupFilter.Builder()
        .curve(BitmapFactory.decodeResource(getResources(), R.drawable.filter9))
        .strength(1f)
        .startFrom(5 * 1000)
        .duration(5 * 1000)
        .build();
// filter action with water mark and filter config.
VideoFilterAddAction filterAddAction = new VideoFilterAddAction.Builder()
        .watermarkFilter(waterMarkFilter)
        .frameFilter(lookupFilter)
        .build();

// bgm add action
VideoBgmAddAction bgmAddAction = new VideoBgmAddAction.Builder()
        .bgmFile(new File(Environment.getExternalStorageDirectory(), "videoeditor/music.mp3"))
        .videoFrom(5 * 1000)
        .videoDuration(7 * 1000)
        .override(true)
        .bgmFrom(5 * 1000)
        .build();

// bgm remove action
VideoBgmRemoveAction bgmRemoveAction = new VideoBgmRemoveAction.Builder()
        .from(3 * 1000)
        .duration(8 * 1000)
        .build();

// cut action
VideoCutAction cutAction = new VideoCutAction.Builder()
        .from(5 * 1000)
        .duration(10 * 1000)
        .build();

// merge action
VideoMergeAction mergeAction = new VideoMergeAction.Builder()
        .merge(new File(Environment.getExternalStorageDirectory(), "videoeditor/input.mp4"))
        // put input file here.
        .inputHere()
        .merge(new File(Environment.getExternalStorageDirectory(), "videoeditor/input2.mp4"))
        .merge(new File(Environment.getExternalStorageDirectory(), "videoeditor/input3.mp4"))
        .merge(new File(Environment.getExternalStorageDirectory(), "videoeditor/input4.mp4"))
        .build();

// start it! token is the unique id of this task.
mToken = VideoEditorManager.getManager()
        // give the input file.
        .edit(new File(Environment.getExternalStorageDirectory(), "videoeditor/input.mp4"))
        // all actions here will be executed in define order.
        .withAction(cutAction)
        .withAction(bgmAddAction)
        .withAction(filterAddAction)
        .withAction(bgmRemoveAction)
        .withAction(mergeAction)
        // give the output file.
        .saveAs(new File(Environment.getExternalStorageDirectory(), "videoeditor/output.mp4"))
        // commit action list, and actions will be executed right now.
        .commit(mEditListener, mStageListener);
```
## Cancel task
```java
// cancel task with token.
VideoEditorManager.cancel(mToken);
```
