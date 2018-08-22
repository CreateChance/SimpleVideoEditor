package com.createchance.demo.views;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.widget.ProgressBar;

import com.createchance.demo.R;

/**
 * 自定义的圆形倒计时view
 *
 * @author gaochao1-iri
 * @date 2018-08-13
 */
public class RoundProgressbar extends ProgressBar {

    private static final String TAG = "RoundProgressbar";

    public static final int DEFAULT_UNREACHED_COLOR = 0xFF6495ED;
    public static final int DEFAULT_REACHED_COLOR = 0xFFFF0000;
    public static final int DEFAULT_TEXT_COLOR = 0xFF0000CD;
    // 设置默认圆大小，单位为 dp
    public static final int DEFAULT_RADIUS = 60;
    // 设置默认画笔宽度，单位为 dp
    public static final int DEFAULT_STROKE_WIDTH = 1;
    // 默认小球的大小，单位dp
    public static final int DEFAULT_BALL_SIZE = 7;
    // 设置文字默认大小，单位为 sp
    public static final int DEFAULT_TEXT_SIZE = 12;

    private Paint paint;
    private RectF rectF;
    private Rect rect;

    int radius;
    int centerX;
    int centerY;

    int unreachedColor;
    int reachedColor;
    int centerColor;
    int textColor;
    int ballColor;

    int paintWidth;
    int textSize;
    int ballSize;

    private String text;

    private float progress;

    public RoundProgressbar(Context context) {
        this(context, null);
    }

    public RoundProgressbar(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public RoundProgressbar(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        paint = new Paint();
        rectF = new RectF();
        rect = new Rect();

        radius = dp2px(DEFAULT_RADIUS);
        paintWidth = dp2px(DEFAULT_STROKE_WIDTH);
        textSize = sp2px(DEFAULT_TEXT_SIZE);

        unreachedColor = DEFAULT_UNREACHED_COLOR;
        reachedColor = DEFAULT_REACHED_COLOR;
        textColor = DEFAULT_TEXT_COLOR;

        obtainStyledAttributes(context, attrs, defStyleAttr);
    }

    @Override
    protected synchronized void onDraw(Canvas canvas) {
//        super.onDraw(canvas);
        int width = getWidth();
        int height = getHeight();

        int contentWidth = width - getPaddingLeft() - getPaddingRight();
        int contentHeight = height - getPaddingTop() - getPaddingBottom();

        // 设置圆心为画布正中心
        radius = contentWidth >= contentHeight ? contentHeight / 2 : contentWidth / 2;
        centerX = width / 2;
        centerY = height / 2;
        paint.setAntiAlias(true);

        // 根据进度计算当前需要绘制的弧度
        float angle = progress * 360;

        rectF.set(centerX - radius + paintWidth + ballSize / 2f,
                centerY - radius + paintWidth + ballSize / 2f,
                centerX + radius - paintWidth - ballSize / 2f,
                centerY + radius - paintWidth - ballSize / 2f);

        // 绘制内部填充颜色
        paint.setColor(centerColor);
        paint.setStyle(Paint.Style.FILL_AND_STROKE);
        canvas.drawArc(rectF, 0, 360, true, paint);

        // 绘制已到达的扇形边框
        paint.setStyle(Paint.Style.STROKE);
        paint.setColor(reachedColor);
        paint.setStrokeWidth(paintWidth);
        canvas.drawArc(rectF, 270, angle, false, paint);

        // 绘制未到达的扇形边框
        if (progress < 100) {
            paint.setStyle(Paint.Style.STROKE);
            paint.setColor(unreachedColor);
            paint.setStrokeWidth(paintWidth);
            canvas.drawArc(rectF, 270 + angle, 360 - angle, false, paint);
        }

        // 绘制小球
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(ballColor);
        canvas.drawCircle(
                centerX + (float) ((radius - paintWidth - ballSize / 2.0) * Math.sin(Math.PI * 2 * angle / 360f)),
                centerY - (float) ((radius - paintWidth - ballSize / 2.0) * Math.cos(Math.PI * 2 * angle / 360f)),
                ballSize / 2f,
                paint
        );

        // 绘制文字
        if (!TextUtils.isEmpty(text)) {
            paint.setColor(textColor);
            paint.setStyle(Paint.Style.FILL);
            paint.setTextSize(textSize);
            paint.setTextAlign(Paint.Align.CENTER);
            Paint.FontMetrics fontMetrics = paint.getFontMetrics();
            float baseLineY = centerY - fontMetrics.descent / 2 - fontMetrics.ascent / 2;
            canvas.drawText(text, centerX, baseLineY, paint);
        }
    }

    public void setProgress(float progress) {
        this.progress = progress;
        invalidate();
    }

    public void setText(String text) {
        this.text = text;
    }

    private void obtainStyledAttributes(Context context, AttributeSet attrs, int defStyleAttr) {
        TypedArray a = context.getTheme().obtainStyledAttributes(attrs, R.styleable.RoundProgressbar, defStyleAttr, 0);

        int count = a.getIndexCount();
        for (int i = 0; i < count; i++) {
            int attr = a.getIndex(i);
            switch (attr) {
                case R.styleable.RoundProgressbar_strokeWidth:
                    paintWidth = dp2px((int) a.getDimension(attr, DEFAULT_STROKE_WIDTH));
                    break;
                case R.styleable.RoundProgressbar_textSize:
                    textSize = sp2px((int) (a.getDimension(attr, DEFAULT_TEXT_SIZE)));
                    break;
                case R.styleable.RoundProgressbar_ballSize:
                    ballSize = dp2px((int) a.getDimension(attr, DEFAULT_BALL_SIZE));
                    break;
                case R.styleable.RoundProgressbar_unreachedColor:
                    unreachedColor = a.getColor(attr, DEFAULT_UNREACHED_COLOR);
                    break;
                case R.styleable.RoundProgressbar_reachedColor:
                    reachedColor = a.getColor(attr, DEFAULT_REACHED_COLOR);
                    break;
                case R.styleable.RoundProgressbar_textColor:
                    textColor = a.getColor(attr, DEFAULT_TEXT_COLOR);
                    break;
                case R.styleable.RoundProgressbar_centerColor:
                    centerColor = a.getColor(attr, DEFAULT_REACHED_COLOR);
                    break;
                case R.styleable.RoundProgressbar_centerText:
                    text = a.getString(attr);
                    break;
                case R.styleable.RoundProgressbar_ballColor:
                    ballColor = a.getColor(attr, DEFAULT_UNREACHED_COLOR);
                    break;
                default:
                    break;
            }
        }

        a.recycle();
    }

    /**
     * dp 2 px
     *
     * @param dpVal
     */
    protected int dp2px(int dpVal) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
                dpVal, getResources().getDisplayMetrics());
    }

    /**
     * sp 2 px
     *
     * @param spVal
     * @return
     */
    protected int sp2px(int spVal) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP,
                spVal, getResources().getDisplayMetrics());
    }
}
