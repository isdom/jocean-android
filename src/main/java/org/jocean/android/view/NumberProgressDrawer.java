package org.jocean.android.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.View;

/**
 * Created by daimajia on 14-4-30.
 */
public class NumberProgressDrawer implements DrawerOnView {

    private Context mContext;

    /**
     * The max progress, default is 100
     */
    private int mMax = 100;

    /**
     * current progress, can not exceed the max progress.
     */
    private int mProgress = 0;

    /**
     * the progress area bar color
     */
    private int mReachedBarColor;

    /**
     * the bar unreached area color.
     */
    private int mUnreachedBarColor;

    /**
     * the progress text color.
     */
    private int mTextColor;

    /**
     * the progress text size
     */
    private float mTextSize;

    /**
     * the height of the reached area
     */
    private float mReachedBarHeight;

    /**
     * the height of the unreached area
     */
    private float mUnreachedBarHeight;


    private final int default_text_color = Color.rgb(66, 145, 241);
    private final int default_reached_color = Color.rgb(66,145,241);
    private final int default_unreached_color = Color.rgb(204, 204, 204);
    private final float default_progress_text_offset;
    private final float default_text_size;
    private final float default_reached_bar_height;
    private final float default_unreached_bar_height;

    /**
     * for save and restore instance of progressbar.
     */
//    private static final String INSTANCE_STATE = "saved_instance";
//    private static final String INSTANCE_TEXT_COLOR = "text_color";
//    private static final String INSTANCE_TEXT_SIZE = "text_size";
//    private static final String INSTANCE_REACHED_BAR_HEIGHT = "reached_bar_height";
//    private static final String INSTANCE_REACHED_BAR_COLOR = "reached_bar_color";
//    private static final String INSTANCE_UNREACHED_BAR_HEIGHT = "unreached_bar_height";
//    private static final String INSTANCE_UNREACHED_BAR_COLOR = "unreached_bar_color";
//    private static final String INSTANCE_MAX = "max";
//    private static final String INSTANCE_PROGRESS = "progress";

    private static final int PROGRESS_TEXT_VISIBLE = 0;
//    private static final int PROGRESS_TEXT_INVISIBLE = 1;



    /**
     * the width of the text that to be drawn
     */
    private float mDrawTextWidth;

    /**
     * the drawn text start
     */
    private float mDrawTextStart;

    /**
     *the drawn text end
     */
    private float mDrawTextEnd;

    /**
     * the text that to be drawn in onDraw()
     */
    private String mCurrentDrawText;

    /**
     * the Paint of the reached area.
     */
    private Paint mReachedBarPaint;
    /**
     * the Painter of the unreached area.
     */
    private Paint mUnreachedBarPaint;
    /**
     * the Painter of the progress text.
     */
    private Paint mTextPaint;

    /**
     * Unreached Bar area to draw rect.
     */
    private RectF mUnreachedRectF = new RectF(0,0,0,0);
    /**
     * reached bar area rect.
     */
    private RectF mReachedRectF = new RectF(0,0,0,0);

    /**
     * the progress text offset.
     */
    private float mOffset;

    /**
     * determine if need to draw unreached area
     */
    private boolean mDrawUnreachedBar = true;

    private boolean mDrawReachedBar = true;

    private boolean mIfDrawText = true;

    public enum ProgressTextVisibility{
        Visible,Invisible
    };



    public NumberProgressDrawer(Context context) {
        mContext = context;

        default_reached_bar_height = dp2px(1.5f);
        default_unreached_bar_height = dp2px(1.0f);
        default_text_size = sp2px(10);
        default_progress_text_offset = dp2px(3.0f);
        
        mReachedBarColor = default_reached_color;
        mUnreachedBarColor = default_unreached_color;
        mTextColor = default_text_color;
        mTextSize = default_text_size;

        mReachedBarHeight = default_reached_bar_height;
        mUnreachedBarHeight = default_unreached_bar_height;
        mOffset = default_progress_text_offset;

        int textVisible = PROGRESS_TEXT_VISIBLE;
        if(textVisible != PROGRESS_TEXT_VISIBLE){
            mIfDrawText = false;
        }

        setProgress(0);
        setMax(100);
        
        initializePainters();
    }

    @Override
    public void drawOnView(final View view, final Canvas canvas) {
        if(mIfDrawText){
            calculateDrawRectF(view);
        }else{
            calculateDrawRectFWithoutProgressText(view);
        }

        if(mDrawReachedBar){
            canvas.drawRect(mReachedRectF,mReachedBarPaint);
        }

        if(mDrawUnreachedBar) {
            canvas.drawRect(mUnreachedRectF, mUnreachedBarPaint);
        }

        if(mIfDrawText)
            canvas.drawText(mCurrentDrawText,mDrawTextStart,mDrawTextEnd,mTextPaint);
    }

    private void initializePainters(){
        mReachedBarPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mReachedBarPaint.setColor(mReachedBarColor);

        mUnreachedBarPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mUnreachedBarPaint.setColor(mUnreachedBarColor);

        mTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mTextPaint.setColor(mTextColor);
        mTextPaint.setTextSize(mTextSize);
    }


    private void calculateDrawRectFWithoutProgressText(final View view) {
        mReachedRectF.left = view.getPaddingLeft();
        mReachedRectF.top = view.getHeight()/2.0f - mReachedBarHeight / 2.0f;
        mReachedRectF.right = (view.getWidth() - view.getPaddingLeft() - view.getPaddingRight() )/(getMax()*1.0f) * getProgress() + view.getPaddingLeft();
        mReachedRectF.bottom = view.getHeight()/2.0f + mReachedBarHeight / 2.0f;

        mUnreachedRectF.left = mReachedRectF.right;
        mUnreachedRectF.right = view.getWidth() - view.getPaddingRight();
        mUnreachedRectF.top = view.getHeight()/2.0f +  - mUnreachedBarHeight / 2.0f;
        mUnreachedRectF.bottom = view.getHeight()/2.0f  + mUnreachedBarHeight / 2.0f;
    }

    private void calculateDrawRectF(final View view) {

        mCurrentDrawText = String.format("%d%%",getProgress()*100/getMax());
        mDrawTextWidth = mTextPaint.measureText(mCurrentDrawText);

        if(getProgress() == 0){
            mDrawReachedBar = false;
            mDrawTextStart = view.getPaddingLeft();
        }else{
            mDrawReachedBar = true;
            mReachedRectF.left = view.getPaddingLeft();
            mReachedRectF.top = view.getHeight()/2.0f - mReachedBarHeight / 2.0f;
            mReachedRectF.right = (view.getWidth() - view.getPaddingLeft() - view.getPaddingRight() )/(getMax()*1.0f) * getProgress() - mOffset + view.getPaddingLeft();
            mReachedRectF.bottom = view.getHeight()/2.0f + mReachedBarHeight / 2.0f;
            mDrawTextStart = (mReachedRectF.right + mOffset);
        }

        mDrawTextEnd =  (int) ((view.getHeight() / 2.0f) - ((mTextPaint.descent() + mTextPaint.ascent()) / 2.0f)) ;

        if((mDrawTextStart + mDrawTextWidth )>= view.getWidth() - view.getPaddingRight()){
            mDrawTextStart = view.getWidth() - view.getPaddingRight() - mDrawTextWidth;
            mReachedRectF.right = mDrawTextStart - mOffset;
        }

        float unreachedBarStart = mDrawTextStart + mDrawTextWidth + mOffset;
        if(unreachedBarStart >= view.getWidth() - view.getPaddingRight()){
            mDrawUnreachedBar = false;
        }else{
            mDrawUnreachedBar = true;
            mUnreachedRectF.left = unreachedBarStart;
            mUnreachedRectF.right = view.getWidth() - view.getPaddingRight();
            mUnreachedRectF.top = view.getHeight()/2.0f +  - mUnreachedBarHeight / 2.0f;
            mUnreachedRectF.bottom = view.getHeight()/2.0f  + mUnreachedBarHeight / 2.0f;
        }
    }
    /**
     * get progress text color
     * @return progress text color
     */
    public int getTextColor() {
        return mTextColor;
    }

    /**
     * get progress text size
     * @return progress text size
     */
    public float getProgressTextSize() {
        return mTextSize;
    }

    public int getUnreachedBarColor() {
        return mUnreachedBarColor;
    }

    public int getReachedBarColor() {
        return mReachedBarColor;
    }

    public int getProgress() {
        return mProgress;
    }

    public int getMax() {
        return mMax;
    }

    public float getReachedBarHeight(){
        return mReachedBarHeight;
    }

    public float getUnreachedBarHeight(){
        return mUnreachedBarHeight;
    }



    public void setProgressTextSize(float TextSize) {
        this.mTextSize = TextSize;
        mTextPaint.setTextSize(mTextSize);
    }

    public void setProgressTextColor(int TextColor) {
        this.mTextColor = TextColor;
        mTextPaint.setColor(mTextColor);
    }

    public void setUnreachedBarColor(int BarColor) {
        this.mUnreachedBarColor = BarColor;
        mUnreachedBarPaint.setColor(mReachedBarColor);
    }

    public void setReachedBarColor(int ProgressColor) {
        this.mReachedBarColor = ProgressColor;
        mReachedBarPaint.setColor(mReachedBarColor);
    }

    public void setMax(int Max) {
        if(Max > 0){
            this.mMax = Max;
        }
    }

    public void incrementProgressBy(int by){
        if(by > 0){
            setProgress(getProgress() + by);
        }
    }

    public void setProgress(int Progress) {
        if(Progress <= getMax()  && Progress >= 0){
            this.mProgress = Progress;
        }
    }

    public float dp2px(float dp) {
        final float scale = mContext.getResources().getDisplayMetrics().density;
        return  dp * scale + 0.5f;
    }

    public float sp2px(float sp){
        final float scale = mContext.getResources().getDisplayMetrics().scaledDensity;
        return sp * scale;
    }

    public void setProgressTextVisibility(ProgressTextVisibility visibility){
        if(visibility == ProgressTextVisibility.Visible){
            mIfDrawText = true;
        }else{
            mIfDrawText = false;
        }
    }

}