package com.hzn.easypickerview;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.widget.Scroller;

import java.util.ArrayList;

/**
 * @author hyx
 * @description 横向滑动选择。问题：会由于字体大小、显示数量多少、屏幕大小而造成文字重叠。
 * @date 2018/2/1.
 */

public class HPickerView extends View {

    /**
     * 文字大小
     */
    private int textSize;
    /**
     * 颜色，默认Color.BLACK
     */
    private int textColor;
    /**
     * 文字最大放大比例，默认2.0f
     */
    private float textMaxScale;
    /**
     * 文字最小alpha值，范围0.0f~1.0f，默认0.4f
     */
    private float textMinAlpha;
    /**
     * 是否循环模式，默认是
     */
    private boolean isRecycleMode;
    /**
     * 正常状态下最多显示几个文字，默认3（偶数时，边缘的文字会截断）
     */
    private int maxShowNum;
    /**
     * 文字画笔
     */
    private TextPaint textPaint;
    /**
     * 文字测量
     */
    private Paint.FontMetrics fm;

    private Scroller scroller;
    /**
     * 滑动速度测量
     */
    private VelocityTracker velocityTracker;
    /**
     * 屏幕最小滑动速度
     */
    private int minimumVelocity;
    /**
     * 屏幕最大滑动速度
     */
    private int maximumVelocity;
    /**
     * 系统所能识别的滑动最小距离
     */
    private int scaledTouchSlop;

    /**
     * 数据
     */
    private ArrayList<String> dataList = new ArrayList<>();
    /**
     * 中间x坐标
     */
    private int cx;
    /**
     * 中间y坐标
     */
    private int cy;
    /**
     * <br>文字最大宽度<br/>
     * <br>这里没实际用到（考虑是否可根据这个数值计算文字是否重叠，进而缩小字体大小到不重叠）<br/>
     */
    private float maxTextWidth;
    /**
     * 文字可绘制高度
     */
    private int textHeight;
    /**
     * 按下时的x坐标
     */
    private float downX;
    /**
     * 本次滑动的x坐标偏移值
     */
    private float offsetX;
    /**
     * 在fling之前的offsetX
     */
    private float oldOffsetX;
    /**
     * 当前选中项<br/>
     * curIndex 在 setDataList() 方法中第一次初始化0
     */
    private int curIndex;
    /**
     * 偏移索引数
     */
    private int offsetIndex;
    /**
     * 是否正处于触摸滑动状态
     */
    private boolean isSliding = false;
    /**
     * 一个文字所占的画布宽度（包括文字两边的空白）
     */
    private int textContentWidth;
    /**
     * 禁止文字放大（当设置的文字缩放值不大于 1 时： true）
     */
    private boolean forbidScale;

    public HPickerView(Context context) {
        this(context, null);
    }

    public HPickerView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public HPickerView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        TypedArray a = context.getTheme().obtainStyledAttributes(attrs, R.styleable.EasyPickerView, defStyleAttr, 0);
        textSize = a.getDimensionPixelSize(R.styleable.EasyPickerView_epvTextSize, (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_SP, 16, getResources().getDisplayMetrics()));
        textColor = a.getColor(R.styleable.EasyPickerView_epvTextColor, Color.BLACK);
        textMaxScale = a.getFloat(R.styleable.EasyPickerView_epvTextMaxScale, 2.0f);
        if (textMaxScale <= 1.0f) {
            textMaxScale = 2.0f;
            forbidScale = true;
        }
        textMinAlpha = a.getFloat(R.styleable.EasyPickerView_epvTextMinAlpha, 0.4f);
        isRecycleMode = a.getBoolean(R.styleable.EasyPickerView_epvRecycleMode, true);
        maxShowNum = a.getInteger(R.styleable.EasyPickerView_epvMaxShowNum, 3);
        a.recycle();

        textPaint = new TextPaint();
        textPaint.setColor(textColor);
        textPaint.setTextSize(textSize);
        textPaint.setAntiAlias(true);
        fm = textPaint.getFontMetrics();

        scroller = new Scroller(context);
        minimumVelocity = ViewConfiguration.get(getContext()).getScaledMinimumFlingVelocity();
        maximumVelocity = ViewConfiguration.get(getContext()).getScaledMaximumFlingVelocity();
        scaledTouchSlop = ViewConfiguration.get(getContext()).getScaledTouchSlop();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int mode = MeasureSpec.getMode(widthMeasureSpec);
        int width = MeasureSpec.getSize(widthMeasureSpec);

        //这里必须加上左右的padding,否则xml文件中的padding将失效
        textContentWidth = (width - getPaddingLeft() - getPaddingRight()) / maxShowNum;

        mode = MeasureSpec.getMode(heightMeasureSpec);//高的测量模式
        int height = MeasureSpec.getSize(heightMeasureSpec);
        textHeight = (int) (fm.bottom - fm.top);
        if (mode != MeasureSpec.EXACTLY) { // wrap_content
            height = textHeight + getPaddingTop() + getPaddingBottom();
        }

        cx = width / 2;
        cy = height / 2;

        setMeasuredDimension(width, height);
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        //父容器不允许拦截事件
        getParent().requestDisallowInterceptTouchEvent(true);
        return super.dispatchTouchEvent(event);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        addVelocityTracker(event);
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                if (!scroller.isFinished()) {
                    scroller.forceFinished(true);
                    finishScroll();
                }
                downX = event.getX();
                break;

            case MotionEvent.ACTION_MOVE:
                offsetX = event.getX() - downX;
                if (isSliding || Math.abs(offsetX) > scaledTouchSlop) {
                    isSliding = true;
                    reDraw();
                }
                break;

            case MotionEvent.ACTION_UP:
                int scrollXVelocity = getScrollXVelocity();
                if (Math.abs(scrollXVelocity) > minimumVelocity) {
                    oldOffsetX = offsetX;
                    scroller.fling(0, 0, scrollXVelocity, 0, -Integer.MAX_VALUE, Integer.MAX_VALUE, 0, 0);
                    invalidate();
                } else {
                    finishScroll();
                }

                // 没有滑动，则判断点击事件
                if (!isSliding) {
                    if (downX < 3 * getMeasuredWidth() / 7)
                        moveBy(-1);
                    else if (downX > 4 * getMeasuredWidth() / 7)
                        moveBy(1);
                }

                isSliding = false;
                recycleVelocityTracker();
                break;
        }
        return true;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (null != dataList && dataList.size() > 0) {
            //注意 裁剪矩形 的坐标，需要用到padding,否则xml中的padding将不准确
            canvas.clipRect(
                    getPaddingLeft(),
                    getPaddingTop(),
                    getMeasuredWidth() - getPaddingRight(),
                    getMeasuredHeight() - getPaddingBottom()
            );
            // 绘制文字，从当前中间项往前、后一共绘制maxShowNum个字
            int size = dataList.size();

            int half = maxShowNum / 2 + 1;//View中间位置显示的 是第几个文字（相对于最大显示数量而言）
            for (int i = -half; i <= half; i++) {
                int index = curIndex - offsetIndex + i;//curIndex 在 setDataList() 方法中第一次初始化0

                if (isRecycleMode) {
                    if (index < 0)
                        index = (index + 1) % dataList.size() + dataList.size() - 1;
                    else if (index > dataList.size() - 1)
                        index = index % dataList.size();
                }

                if (index >= 0 && index < size) {
                    // 计算每个字的中间 x 坐标
                    int tempX = cx + i * textContentWidth;
                    //滑动偏移时的 x 坐标
                    tempX += offsetX % textContentWidth;

                    // 根据每个字中间x坐标到cx的距离，计算出scale值
                    float offsetValue = (1.0f * Math.abs(tempX - cx) / textContentWidth);
                    //    -1
                    // 根据textMaxScale，计算出tempScale值，即实际text应该放大的倍数，范围 1~textMaxScale
                    float tempScale = textMaxScale - offsetValue * (textMaxScale - 1.0f);

                    tempScale = tempScale < 1.0f ? 1.0f : tempScale;
                    float tempAlpha = (tempScale - 1) / (textMaxScale - 1);
                    float textAlpha = (1 - textMinAlpha) * tempAlpha + textMinAlpha;
                    textPaint.setTextSize(forbidScale ? textSize : textSize * tempScale);
                    textPaint.setColor(Color.rgb((int) ((52 * tempAlpha)), (int) ((146 * tempAlpha)), (int) ((233 * tempAlpha))));
                    textPaint.setAlpha((int) (255 * textAlpha));

                    // 绘制
                    Paint.FontMetrics tempFm = textPaint.getFontMetrics();
                    String text = dataList.get(index);
                    float textWidth = textPaint.measureText(text);
                    canvas.drawText(text, tempX - textWidth / 2, (cy - (tempFm.bottom - tempFm.top) / 2) - tempFm.top, textPaint);
                }
            }
        }
    }

    @Override
    public void computeScroll() {
        if (scroller.computeScrollOffset()) {
            //fling后，需不断计算滑动的距离
            offsetX = oldOffsetX + scroller.getCurrX();

            if (!scroller.isFinished())//滑动没有停
                reDraw();
            else
                finishScroll();
        }
    }

    private void addVelocityTracker(MotionEvent event) {
        if (velocityTracker == null)
            velocityTracker = VelocityTracker.obtain();

        velocityTracker.addMovement(event);
    }

    private void recycleVelocityTracker() {
        if (velocityTracker != null) {
            velocityTracker.recycle();
            velocityTracker = null;
        }
    }

    private int getScrollXVelocity() {
        velocityTracker.computeCurrentVelocity(1000, maximumVelocity);
        int velocity = (int) velocityTracker.getXVelocity();
        return velocity;
    }

    private void reDraw() {
        // curIndex需要偏移的量
        //计算滑动偏移量 是 多少个 文字距离（多少个文字），不到一个的时候，为0个
        //向左滑动的时候 offsetX 为负值，向右滑动为正值 （所以 offsetIndex正负值同理）
        //curIndex - i，对应：向左滑动索引增大，向右索引减小
        int i = (int) (offsetX / textContentWidth);
        if (isRecycleMode || (curIndex - i >= 0 && curIndex - i < dataList.size())) {
            if (offsetIndex != i) {
                offsetIndex = i;

                if (null != onScrollChangedListener)
                    onScrollChangedListener.onScrollChanged(getNowIndex(-offsetIndex));
            }
            postInvalidate();
        } else {
            finishScroll();
        }
    }

    private void finishScroll() {
        // 判断结束滑动后应该停留在哪个位置

        float v = offsetX % textContentWidth;
        if (v > 0.5f * textContentWidth)
            ++offsetIndex;
        else if (v < -0.5f * textContentWidth)
            --offsetIndex;

        // 重置curIndex
        curIndex = getNowIndex(-offsetIndex);

        // 计算回弹的距离
        offsetX = offsetIndex * textContentWidth;

        // 更新
        if (null != onScrollChangedListener)
            onScrollChangedListener.onScrollFinished(curIndex);

        // 重绘
        reset();
        postInvalidate();
    }

    private int getNowIndex(int offsetIndex) {
        int index = curIndex + offsetIndex;
        if (isRecycleMode) {
            if (index < 0)
                index = (index + 1) % dataList.size() + dataList.size() - 1;
            else if (index > dataList.size() - 1)
                index = index % dataList.size();
        } else {
            if (index < 0)
                index = 0;
            else if (index > dataList.size() - 1)
                index = dataList.size() - 1;
        }

        return index;
    }

    private void reset() {
        offsetIndex = 0;

        offsetX = 0;
        oldOffsetX = 0;
    }

    /**
     * 设置要显示的数据
     *
     * @param dataList 要显示的数据
     */
    public void setDataList(ArrayList<String> dataList) {
        this.dataList.clear();
        this.dataList.addAll(dataList);

        // 更新maxTextWidth
        if (null != dataList && dataList.size() > 0) {
            int size = dataList.size();
            for (int i = 0; i < size; i++) {
                float tempWidth = textPaint.measureText(dataList.get(i));
                if (tempWidth > maxTextWidth)
                    maxTextWidth = tempWidth;
            }
            curIndex = 0;
        }
        requestLayout();
        invalidate();
    }

    /**
     * 获取当前状态下，选中的下标
     *
     * @return 选中的下标
     */
    public int getCurIndex() {
        return curIndex - offsetIndex;
    }

    /**
     * 滚动到指定位置
     *
     * @param index 需要滚动到的指定位置
     */
    public void moveTo(int index) {
        if (index < 0 || index >= dataList.size() || curIndex == index)
            return;
        /**
         *  暂时注销，scroller.forceFinished(true);期初加上是为了防止重复设置moveTo，<br/>
         *  现在需求功能中不存在同一pickerview的重复滑动。<br/>
         *  此时加上后弊端：后一个pickerview设置moveTo后，会把上一个的滑动未结束的pickerview强制结束，造成无法停留在目标值<br/>
         */
//        if (!scroller.isFinished())
//            scroller.forceFinished(true);

//        finishScroll();

        int dx = 0;

        if (!isRecycleMode) {
            dx = (curIndex - index) * textContentWidth;
        } else {
            int offsetIndex = curIndex - index;
            int d1 = Math.abs(offsetIndex) * textContentWidth;
            int d2 = (dataList.size() - Math.abs(offsetIndex)) * textContentWidth;

            if (offsetIndex > 0) {
                if (d1 < d2)
                    dx = d1; // ascent
                else
                    dx = -d2; // descent
            } else {
                if (d1 < d2)
                    dx = -d1; // descent
                else
                    dx = d2; // ascent
            }
        }
        scroller.startScroll(0, 0, dx, 0, 500);
        invalidate();
    }

    /**
     * 滚动指定的偏移量
     *
     * @param offsetIndex 指定的偏移量
     */
    public void moveBy(int offsetIndex) {
        moveTo(getNowIndex(offsetIndex));
    }

    /**
     * 滚动发生变化时的回调接口
     */
    public interface OnScrollChangedListener {
        public void onScrollChanged(int curIndex);

        public void onScrollFinished(int curIndex);
    }

    private OnScrollChangedListener onScrollChangedListener;

    public void setOnScrollChangedListener(OnScrollChangedListener onScrollChangedListener) {
        this.onScrollChangedListener = onScrollChangedListener;
    }
}

