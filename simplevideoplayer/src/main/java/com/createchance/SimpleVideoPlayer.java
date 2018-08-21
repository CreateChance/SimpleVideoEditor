package com.createchance;

import android.app.Activity;
import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.SurfaceTexture;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.FrameLayout;

import com.qihoo360.mobilesafe.myvideo.simplevideoplayer.R;

import static com.createchance.Utils.DEBUG;
import static com.createchance.Utils.adjustAspectRatio;
import static com.createchance.Utils.log;

/**
 * 视频播放器view，提供视频播放基本功能
 * 灵感来自android VideoView和开源项目：
 * https://github.com/afollestad/easy-video-player
 * https://github.com/lipangit/JiaoZiVideoPlayer
 * 基于TextureView实现播放
 *
 * @author createchance
 * @date 2017-09-15
 */

public class SimpleVideoPlayer extends FrameLayout
        implements
        TextureView.SurfaceTextureListener,
        MediaPlayer.OnPreparedListener,
        MediaPlayer.OnBufferingUpdateListener,
        MediaPlayer.OnCompletionListener,
        MediaPlayer.OnVideoSizeChangedListener,
        MediaPlayer.OnErrorListener {

    private static final int STATE_IDLE = 1;
    private static final int STATE_PREPARING = 2;
    private static final int STATE_PREPARED = 3;
    private static final int STATE_PLAYING = 4;
    private static final int STATE_PAUSED = 5;
    private int mCurrentState = STATE_IDLE;

    /**
     * 阻塞任务类型，用于在加载视频还没有开始播放的时候记录停止和暂停请求，待视频正式开始播放之后再执行这个请求。
     */
    private static final int PENDING_ACTION_NONE = -1;
    private static final int PENDING_ACTION_PAUSE = 1;
    private static final int PENDING_ACTION_STOP = 2;
    // 当前的阻塞请求
    private volatile int mPendingAction = PENDING_ACTION_NONE;

    private Context mContext;

    // 播放的时候进度条更新速度
    private static final int UPDATE_INTERVAL = 500;

    private IGestureListener mGestureListener;
    private float CLICK_THRESHOLD;
    private MotionEvent mLastPos;

    private TextureView mTextureView;
    private Surface mSurface;

    private MediaPlayer mPlayer;

    private AudioManager mAudioManager;

    private VideoPlayerCallback mCallback;

    private boolean mIsFullScreen;
    private int mScreenWidth;
    private int mScreenHeight;
    private int mOriginWidth = -1;
    private int mOriginHeight = -1;
    private int mTextureWidth;
    private int mTextureHeight;

    private int mOriginSystemUiVisiability = -1;

    private Uri mSource;
    private int mInitialPosition = -1;
    // 是否循环播放
    private boolean mLoop = false;
    private int mLeftVolume;
    private int mRightVolume;

    private Handler mHandler;
    // Runnable used to run code on an interval to update counters and seeker
    private final Runnable mUpdateProgressTask = new Runnable() {
        @Override
        public void run() {
            if (mCurrentState != STATE_PLAYING) {
                return;
            }
            int pos = mPlayer.getCurrentPosition();
            int dur = mPlayer.getDuration();
            if (pos > dur) {
                pos = dur;
            }

            if (mCallback != null) {
                mCallback.onVideoProgressUpdate(pos, dur);
            }

            if (mHandler != null) {
                mHandler.postDelayed(this, UPDATE_INTERVAL);
            }
        }
    };

    public SimpleVideoPlayer(Context context) {
        super(context);
        init(context, null);
    }

    public SimpleVideoPlayer(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public SimpleVideoPlayer(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    /**
     * Surface listeners
     */
    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surfaceTexture, int width, int height) {
        log("Surface texture available, width: " + width + ", height: " + height);
        mTextureWidth = width;
        mTextureHeight = height;
        if (mOriginWidth == -1) {
            mOriginWidth = width;
        }
        if (mOriginHeight == -1) {
            mOriginHeight = height;
        }
        mSurface = new Surface(surfaceTexture);

        if (mPlayer != null) {
            mPlayer.setSurface(mSurface);
        }
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surfaceTexture, int width, int height) {
        log("Surface texture changed, width: " + width + ", height: " + height);
        // record this change
        mTextureWidth = width;
        mTextureHeight = height;
        if (mPlayer != null) {
            adjustAspectRatio(mTextureView, width, height,
                    mPlayer.getVideoWidth(), mPlayer.getVideoHeight());
        }
    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surfaceTexture) {
        log("Surface texture destroyed");
        mSurface = null;
        return false;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surfaceTexture) {
    }

    /**
     * Media player listeners
     */
    @Override
    public void onPrepared(MediaPlayer mediaPlayer) {
        log("onPrepared()");
        mCurrentState = STATE_PREPARED;

        if (mSource != null) {
            if (mInitialPosition > 0) {
                mPlayer.seekTo(mInitialPosition);
                mInitialPosition = -1;
            }

            log("playing video: " + mSource);
            mPlayer.setVolume(
                    mLeftVolume >= 0 ? mLeftVolume : mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC),
                    mRightVolume >= 0 ? mRightVolume : mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
            );
            mPlayer.start();

            // set state
            mCurrentState = STATE_PLAYING;

            if (mCallback != null) {
                mCallback.onStarted(mSource);
            }

            if (mHandler == null) {
                mHandler = new Handler();
            }
            mHandler.post(mUpdateProgressTask);

            // 播放的时候保持屏幕打开
            setKeepScreenOn(true);

            if (mPendingAction == PENDING_ACTION_PAUSE) {
                // pause this video
                log("Got one pending action: pause, so pause this video now.");
                pause();
            } else if (mPendingAction == PENDING_ACTION_STOP) {
                // stop this video
                log("Got one pending action: stop, so stop this video now.");
                stop();
            }

            // reset pending action
            mPendingAction = PENDING_ACTION_NONE;
        }
    }

    @Override
    public void onBufferingUpdate(MediaPlayer mediaPlayer, int percent) {
        log("Buffering percent: " + percent);
    }

    @Override
    public void onCompletion(MediaPlayer mediaPlayer) {
        log("onCompletion()");
        if (mHandler != null) {
            mHandler.removeCallbacks(mUpdateProgressTask);
        }

        if (mCallback != null) {
            mCallback.onCompletion(mSource);
            if (mLoop) {
                mCurrentState = STATE_PLAYING;
                mCallback.onStarted(mSource);
                log("loop playing: " + mSource);
            } else {
                // 播放完成的时候，如果不是循环播放的话，那么屏幕不要保持常亮
                setKeepScreenOn(false);
                mCurrentState = STATE_IDLE;
                mPlayer.reset();
                release();
                removeView(mTextureView);
                log("video playing is complete, reset player.");
            }
        }
    }

    @Override
    public void onVideoSizeChanged(MediaPlayer mediaPlayer, int width, int height) {
        log("Video size changed, width: " + width + ", height: " + height);
        if (mPlayer != null) {
            adjustAspectRatio(mTextureView, mTextureWidth,
                    mTextureHeight, width, height);
        }
    }

    @Override
    public boolean onError(MediaPlayer mediaPlayer, int what, int extra) {
        removeView(mTextureView);
        // 播放错误，屏幕不要保持常亮
        setKeepScreenOn(false);
        if (what == -38) {
            // fuck samsung: Error code -38 happens on some Samsung devices
            // Just ignore it
            return false;
        }
        String errorMsg = "Preparation/playback error (" + what + "), extra: " + extra;
        switch (what) {
            default:
                errorMsg += "Unknown error";
                break;
            case MediaPlayer.MEDIA_ERROR_IO:
                errorMsg += "I/O error";
                break;
            case MediaPlayer.MEDIA_ERROR_MALFORMED:
                errorMsg += "Malformed";
                break;
            case MediaPlayer.MEDIA_ERROR_NOT_VALID_FOR_PROGRESSIVE_PLAYBACK:
                errorMsg += "Not valid for progressive playback";
                break;
            case MediaPlayer.MEDIA_ERROR_SERVER_DIED:
                errorMsg += "Server died";
                break;
            case MediaPlayer.MEDIA_ERROR_TIMED_OUT:
                errorMsg += "Timed out";
                break;
            case MediaPlayer.MEDIA_ERROR_UNSUPPORTED:
                errorMsg += "Unsupported";
                break;
        }
        log("on error: " + errorMsg);
        throwError(new Exception(errorMsg));
        return false;
    }

    /**
     * View events
     */
    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();

        if (isInEditMode()) {
            return;
        }

        mHandler = new Handler();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        log("Detached from window");
        if (mCurrentState == STATE_PAUSED || mCurrentState == STATE_PLAYING) {
            stop();
        } else {
            release();
        }
    }

    /*********************************** 公开api部分-START ***********************************/

    public void setGestureListener(IGestureListener listener) {
        mGestureListener = listener;
    }

    /**
     * 设置是否处于debug状态，debug状态输出全部log
     *
     * @param debug 是否为debug状态
     */
    public void setDebug(boolean debug) {
        DEBUG = debug;
    }

    /**
     * 查询当前是否处于debug状态
     *
     * @return true：debug状态，false：不是debug状态
     */
    public boolean isDebug() {
        return DEBUG;
    }

    public TextureView getDisplayView() {
        return mTextureView;
    }

    /**
     * 查询当前是否正在播放视频
     *
     * @return true：正在播放，false：没有播放
     */
    public boolean isPlaying() {
        return mPlayer != null && mPlayer.isPlaying();
    }

    /**
     * 查询当前视频播放是否被暂停了
     *
     * @return true：暂停，false：没有暂停，可能是正在播放，或者已经停止等
     */
    public boolean isPaused() {
        return mPlayer != null && mCurrentState == STATE_PAUSED;
    }

    /**
     * 获得当前播放的位置，单位为时间毫秒
     *
     * @return 当前位置
     */
    public int getCurrentPosition() {
        if (mPlayer == null) {
            return -1;
        }
        return mPlayer.getCurrentPosition();
    }

    /**
     * 获得当前视频时长
     *
     * @return 视频时长
     */
    public int getDuration() {
        if (mPlayer == null) {
            return -1;
        }
        return mPlayer.getDuration();
    }

    /**
     * 设置回调，这里的回调给出很多播放过程中的信息，比如播放进度
     *
     * @param callback callback对象
     */
    public void setCallback(VideoPlayerCallback callback) {
        mCallback = callback;
    }

    /**
     * 设置初始播放位置，单位为时间毫秒
     *
     * @param pos 初始播放位置
     */
    public void setInitialPosition(int pos) {
        mInitialPosition = pos;
    }

    /**
     * 开始播放
     *
     * @param request 播放的请求
     */
    public void start(PlayRequest request) {
        if (mCurrentState != STATE_IDLE) {
            log("start on wrong state: " + mCurrentState);
            return;
        }

        if (request.videoSource == null) {
            log("null source is not allowed!");
            return;
        }

        mTextureView = new TextureView(getContext());
        // Instantiate and add TextureView for rendering
        final LayoutParams textureLp =
                new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT);
        textureLp.gravity = Gravity.CENTER;
        // 放在z轴最下面，防止盖住控制view
        addView(mTextureView, 0, textureLp);
        mTextureView.setSurfaceTextureListener(this);
        log("Add texture view");

        mSource = request.videoSource;
        mInitialPosition = request.startPos;
        mLoop = request.loop;
        mLeftVolume = request.leftVolume;
        mRightVolume = request.rightVolume;
        prepare();
    }

    /**
     * 暂停播放，在调用之前请确保视频已经播放了，请查看{@link #isPlaying}
     */
    public void pause() {
        if (mCurrentState != STATE_PLAYING) {
            log("pause on wrong state: " + mCurrentState);
            if (mCurrentState == STATE_PREPARING ||
                    mCurrentState == STATE_PREPARED) {
                log("we are preparing, so pending pause action.");
                mPendingAction = PENDING_ACTION_PAUSE;
            }
            return;
        }

        log("pause video: " + mSource);
        mPlayer.pause();
        if (mCallback != null) {
            mCallback.onPaused(mSource);
        }

        mHandler.removeCallbacks(mUpdateProgressTask);

        // set state
        mCurrentState = STATE_PAUSED;

        // 暂停的时候，屏幕不要保持常亮
        setKeepScreenOn(false);
    }

    /**
     * 从暂停状态恢复播放
     */
    public void resume() {
        if (mCurrentState != STATE_PAUSED) {
            log("resume on wrong state: " + mCurrentState);
            return;
        }

        log("resume playing video: " + mSource);
        mPlayer.start();

        mCurrentState = STATE_PLAYING;

        if (mCallback != null) {
            mCallback.onResumed(mSource);
        }

        mHandler.post(mUpdateProgressTask);

        // 恢复播放屏幕保持常亮
        setKeepScreenOn(true);
    }

    /**
     * 停止播放视频，在调用之前请确保视频已经播放了，请查看{@link #isPlaying}
     */
    public void stop() {
        if (mCurrentState != STATE_PLAYING &&
                mCurrentState != STATE_PAUSED) {
            log("stop on wrong state: " + mCurrentState);
            if (mCurrentState == STATE_PREPARING ||
                    mCurrentState == STATE_PREPARED) {
                log("we are preparing, so pending stop action.");
                mPendingAction = PENDING_ACTION_STOP;
            }
            return;
        }

        log("stop playing: " + mSource);
        mPlayer.stop();
        release();

        removeView(mTextureView);

        // set state
        mCurrentState = STATE_IDLE;

        if (mCallback != null) {
            mCallback.onStopped(mSource);
        }

        // 停止的时候，屏幕不要保持常亮
        setKeepScreenOn(false);
    }

    /**
     * 设置是否循环播放，也就是播放完毕之后是否自动开始播放
     *
     * @param loop 是否循环播放
     */
    public void setLoop(boolean loop) {
        mLoop = loop;
        if (mPlayer != null) {
            mPlayer.setLooping(loop);
        }
    }

    /**
     * 重置播放器
     */
    public void reset() {
        if (isPlaying() || isPaused()) {
            mPlayer.stop();
        }

        mPlayer.reset();

        // set state
        mCurrentState = STATE_IDLE;

        // 屏幕不要保持常亮
        setKeepScreenOn(false);
    }

    /**
     * 拖动到视频的某一位置开始播放，单位为时间毫秒
     *
     * @param pos 指定位置
     */
    public void seekTo(int pos) {
        if (mCurrentState != STATE_PAUSED &&
                mCurrentState != STATE_PLAYING) {
            log("seek on wrong state: " + mCurrentState);
            return;
        }

        log("seek to position: " + pos);
        mPlayer.seekTo(pos);

        if (mCallback != null) {
            mCallback.onVideoProgressUpdate(pos, mPlayer.getDuration());
        }
    }

    /**
     * 设置当前视频音量，可以分别指定左声道和右声道
     *
     * @param leftVolume  左声道音量
     * @param rightVolume 右声道音量
     */
    public void setVolume(float leftVolume, float rightVolume) {
        // 只有正在播放或者暂停状态可以设置音量
        if (mCurrentState != STATE_PLAYING && mCurrentState != STATE_PAUSED) {
            log("set volume on wrong state: " + mCurrentState);
            return;
        }

        log("set volume, left: " + leftVolume + ", right: " + rightVolume);
        mPlayer.setVolume(leftVolume, rightVolume);
    }

    public void enterLandscapeFullScreen(Activity activity) {
        log("Screen width: " + mScreenWidth + ", height: " + mScreenHeight);
        if (!mIsFullScreen) {
            mIsFullScreen = true;
            ViewGroup.LayoutParams params = getLayoutParams();
            params.width = mScreenHeight;
            params.height = mScreenWidth;
            setLayoutParams(params);
            FrameLayout.LayoutParams textureParams = (LayoutParams) mTextureView.getLayoutParams();
            textureParams.width = mScreenHeight;
            textureParams.height = mScreenWidth;
            mTextureView.setLayoutParams(textureParams);
            activity.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);
            activity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);

            if (Build.VERSION.SDK_INT >= 19) {
                View decorView = activity.getWindow().getDecorView();
                mOriginSystemUiVisiability = decorView.getSystemUiVisibility();
                decorView.setSystemUiVisibility(
                        View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                                | View.SYSTEM_UI_FLAG_FULLSCREEN
                                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
            }
        }
    }

    public void exitLandscapeFullScreen(Activity activity) {
        if (mIsFullScreen) {
            mIsFullScreen = false;
            ViewGroup.LayoutParams params = getLayoutParams();
            params.width = mOriginWidth;
            params.height = mOriginHeight;
            setLayoutParams(params);
            FrameLayout.LayoutParams textureParams = (LayoutParams) mTextureView.getLayoutParams();
            textureParams.width = mOriginWidth;
            textureParams.height = mOriginHeight;
            mTextureView.setLayoutParams(textureParams);
            activity.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
            activity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);

            if (Build.VERSION.SDK_INT >= 19) {
                View decorView = activity.getWindow().getDecorView();
                decorView.setSystemUiVisibility(mOriginSystemUiVisiability);
            }
        }
    }

    public void enterPortraitFullScreen(Activity activity) {
        log("enter full screen, height: " + mContext.getResources().getDisplayMetrics().heightPixels);
        if (!mIsFullScreen) {
            mIsFullScreen = true;
            ViewGroup.LayoutParams params = getLayoutParams();
            params.width = mScreenWidth;
            params.height = mScreenHeight;
            setLayoutParams(params);

            if (Build.VERSION.SDK_INT >= 19) {
                View decorView = activity.getWindow().getDecorView();
                mOriginSystemUiVisiability = decorView.getSystemUiVisibility();
                decorView.setSystemUiVisibility(
                        View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                                | View.SYSTEM_UI_FLAG_FULLSCREEN
                                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
            }
        }
    }

    public void exitPortraitFullScreen(Activity activity) {
        log("exit full screen, height: " + mContext.getResources().getDisplayMetrics().heightPixels);
        if (mIsFullScreen) {
            mIsFullScreen = false;
            ViewGroup.LayoutParams params = getLayoutParams();
            params.width = mOriginWidth;
            params.height = mOriginHeight;
            setLayoutParams(params);

            if (Build.VERSION.SDK_INT >= 19) {
                View decorView = activity.getWindow().getDecorView();
                decorView.setSystemUiVisibility(mOriginSystemUiVisiability);
            }
        }
    }

    public boolean isFullScreen() {
        return mIsFullScreen;
    }

    /*********************************** 公开api部分-END ***********************************/

    private void init(Context context, AttributeSet attrs) {
        mContext = context;

        // get audio manager
        mAudioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);

        // get screen width and height
        mScreenHeight = mContext.getResources().getDisplayMetrics().heightPixels + getNaviBarHeight();
        mScreenWidth = mContext.getResources().getDisplayMetrics().widthPixels;

        setBackgroundColor(Color.BLACK);

        if (attrs != null) {
            TypedArray a = context.getTheme().
                    obtainStyledAttributes(attrs, R.styleable.SimpleVideoPlayer, 0, 0);

            /**
             * 获得xml设置的数据
             */
            // 获得播放源uri
            String source = a.getString(R.styleable.SimpleVideoPlayer_video_source);
            if (source != null && !source.trim().isEmpty()) {
                mSource = Uri.parse(source);
            }
            // 是否处于debug状态
            DEBUG = a.getBoolean(R.styleable.SimpleVideoPlayer_debug, false);
            // 是否循环播放视频
            mLoop = a.getBoolean(R.styleable.SimpleVideoPlayer_video_loop, false);

            a.recycle();
        } else {
            mLoop = false;
            DEBUG = false;
            mSource = null;
        }

        // 点击事件阈值: 5dp
        CLICK_THRESHOLD = dpToPx(5);
        setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                int action = motionEvent.getAction();
                float deltaX;
                float deltaY;
                double distance;

                switch (action) {
                    case MotionEvent.ACTION_DOWN:
                        mLastPos = MotionEvent.obtain(motionEvent);
                        break;
                    case MotionEvent.ACTION_MOVE:
                        deltaX = motionEvent.getX() - mLastPos.getX();
                        deltaY = motionEvent.getY() - mLastPos.getY();
                        distance = Math.sqrt(deltaX * deltaX + deltaY * deltaY);
                        if (distance >= CLICK_THRESHOLD) {
                            // 大于阈值是移动
                            if (mGestureListener != null) {
                                mGestureListener.onMoving(mLastPos, motionEvent);
                            }
                        }
                        break;
                    case MotionEvent.ACTION_UP:
                        deltaX = motionEvent.getX() - mLastPos.getX();
                        deltaY = motionEvent.getY() - mLastPos.getY();
                        distance = Math.sqrt(deltaX * deltaX + deltaY * deltaY);
                        if (distance >= CLICK_THRESHOLD) {
                            // 大于阈值是移动
                            if (mGestureListener != null) {
                                mGestureListener.onMoveDone(mLastPos, motionEvent);
                            }
                        } else {
                            // 小于阈值就是点击
                            if (mGestureListener != null) {
                                mGestureListener.onClick(motionEvent);
                            }
                        }
                        break;
                    default:
                        break;
                }

                return true;
            }
        });
    }

    private void release() {
        if (mPlayer != null) {
            try {
                mPlayer.release();
            } catch (Throwable ignored) {
            }
            mPlayer = null;
        }

        if (mHandler != null) {
            mHandler.removeCallbacks(mUpdateProgressTask);
        }

        log("Released player and Handler");
    }

    private void setSourceInternal() throws Exception {
        if (mSource.getScheme() != null
                && (mSource.getScheme().equals("http") || mSource.getScheme().equals("https"))) {
            log("Loading web URI: " + mSource.toString());
            mPlayer.setDataSource(mSource.toString());
        } else if (mSource.getScheme() != null &&
                (mSource.getScheme().equals("file") &&
                        mSource.getPath().contains("/android_assets/"))) {
            log("Loading assets URI: " + mSource.toString());
            AssetFileDescriptor afd;
            afd = getContext().getAssets().
                    openFd(mSource.toString().replace("file:///android_assets/", ""));
            mPlayer.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());
            afd.close();
        } else if (mSource.getScheme() != null && mSource.getScheme().equals("asset")) {
            log("Loading assets URI: " + mSource.toString());
            AssetFileDescriptor afd;
            afd = getContext().getAssets().openFd(mSource.toString().replace("asset://", ""));
            mPlayer.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());
            afd.close();
        } else {
            log("Loading local URI: " + mSource.toString());
            mPlayer.setDataSource(getContext(), mSource);
        }

        log("Ready to prepare async");
        // 异步加载，立即返回
        mPlayer.prepareAsync();

        // set state
        mCurrentState = STATE_PREPARING;
    }

    private void prepare() {
        try {
            if (mPlayer == null) {
                mPlayer = new MediaPlayer();
                mPlayer.setOnPreparedListener(this);
                mPlayer.setOnBufferingUpdateListener(this);
                mPlayer.setOnCompletionListener(this);
                mPlayer.setOnVideoSizeChangedListener(this);
                mPlayer.setOnErrorListener(this);
                mPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
                mPlayer.setLooping(mLoop);
                mPlayer.setSurface(mSurface);
            }
            setSourceInternal();
        } catch (Exception e) {
            log("prepare error, so reset all state.");
            if (DEBUG) {
                e.printStackTrace();
            }
            mPlayer.reset();
            release();
            removeView(mTextureView);
            mSource = null;
            mCurrentState = STATE_IDLE;
            throwError(e);
        }
    }

    private void throwError(Exception e) {
        if (mCallback != null) {
            mCallback.onError(e);
        } else {
            throw new RuntimeException(e);
        }
    }

    /**
     * 根据手机的分辨率从 dp 的单位 转成为 px(像素)
     */
    private int dpToPx(float dpValue) {
        final float scale = mContext.getResources().getDisplayMetrics().density;
        return (int) (dpValue * scale + 0.5f);
    }

    public int getNaviBarHeight() {
        int resourceId = 0;
        int rid = mContext.getResources().getIdentifier(
                "config_showNavigationBar", "bool", "android");
        if (rid != 0) {
            resourceId = mContext.getResources().getIdentifier(
                    "navigation_bar_height", "dimen", "android");
            return mContext.getResources().getDimensionPixelSize(resourceId);
        } else {
            return 0;
        }
    }

    public int getStatusBarHeight() {
        int result = 0;
        int resourceId = mContext.getResources().getIdentifier("status_bar_height", "dimen",
                "android");
        if (resourceId > 0) {
            result = mContext.getResources().getDimensionPixelSize(resourceId);
        }
        return result;
    }
}
