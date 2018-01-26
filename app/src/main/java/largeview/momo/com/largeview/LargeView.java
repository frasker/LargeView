package largeview.momo.com.largeview;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapRegionDecoder;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Scroller;

import java.io.IOException;
import java.io.InputStream;

/**
 * Created by 默默 on 2018/1/25.
 */

public class LargeView extends View implements GestureDetector.OnGestureListener, View.OnTouchListener {

    private BitmapRegionDecoder mBitmapRegionDecoder;
    private Rect mRect;
    private Bitmap mBitmap;
    private int mImageWidth;
    private int mImageHeight;
    private int mViewWidth;
    private int mViewHeight;
    private TYPE mImageType = TYPE.ERROR_IMAGE;
    private BitmapFactory.Options mOptions;
    private float mScale;
    private Matrix mMatrix;
    private Scroller mScroller;
    private GestureDetector mGestureDetector;

    private enum TYPE {
        VERTICAL_LARGE_IMAGE,
        HORIZON_LARGE_IMAGE,
        NORMAL_IMAGE,
        ERROR_IMAGE
    }

    public LargeView(Context context) {
        this(context, null);
    }

    public LargeView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public LargeView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mRect = new Rect();
        mOptions = new BitmapFactory.Options();
        mMatrix = new Matrix();
        mScroller = new Scroller(context);
        mGestureDetector = new GestureDetector(context, this);
        setOnTouchListener(this);
    }

    public void setImageInputStream(InputStream in) {
        mOptions.inJustDecodeBounds = true;
        BitmapFactory.decodeStream(in, null, mOptions);
        mImageWidth = mOptions.outWidth;
        mImageHeight = mOptions.outHeight;
        mOptions.inMutable = true;
        mOptions.inPreferredConfig = Bitmap.Config.RGB_565;
        try {
            mBitmapRegionDecoder = BitmapRegionDecoder.newInstance(in, false);
        } catch (IOException e) {
            e.printStackTrace();
        }
        requestLayout();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        mViewWidth = getMeasuredWidth();
        mViewHeight = getMeasuredHeight();
        setImageType();
    }

    private void setImageType() {
        if (mImageWidth == 0 || mImageHeight == 0) {
            mImageType = TYPE.ERROR_IMAGE;
        }
        final float imageH2W = mImageHeight / (float) mImageWidth;
        final float viewH2W = mViewHeight / (float) mViewWidth;
        if (imageH2W > viewH2W) {
            mImageType = TYPE.VERTICAL_LARGE_IMAGE;
            mScale = mViewWidth / (float) mImageWidth;
            mRect.set(0, 0, mImageWidth, (int) (mViewHeight * mScale));
        } else if (imageH2W < viewH2W) {
            mImageType = TYPE.HORIZON_LARGE_IMAGE;
            mScale = mViewHeight / (float) mImageHeight;
            mRect.set(0, 0, (int) (mViewWidth * mScale), mImageHeight);
        } else {
            mImageType = TYPE.NORMAL_IMAGE;
            mScale = mViewWidth / (float) mImageWidth;
            mRect.set(0, 0, mImageWidth, mImageHeight);
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (mBitmapRegionDecoder == null) {
            return;
        }
        switch (mImageType) {
            case NORMAL_IMAGE:
                mOptions.inJustDecodeBounds = false;
                mOptions.inBitmap = mBitmap;
                mBitmap = mBitmapRegionDecoder.decodeRegion(mRect, mOptions);
                break;
            case HORIZON_LARGE_IMAGE:
                mOptions.inJustDecodeBounds = false;
                mOptions.inBitmap = mBitmap;
                mBitmap = mBitmapRegionDecoder.decodeRegion(mRect, mOptions);
                break;
            case VERTICAL_LARGE_IMAGE:
                mOptions.inJustDecodeBounds = false;
                mOptions.inBitmap = mBitmap;
                mBitmap = mBitmapRegionDecoder.decodeRegion(mRect, mOptions);
                break;
        }
        mMatrix.reset();
        mMatrix.setScale(mScale, mScale);
        canvas.drawBitmap(mBitmap, mMatrix, null);
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        return mGestureDetector.onTouchEvent(event);
    }

    @Override
    public boolean onDown(MotionEvent e) {
        if (mScroller.isFinished()) {
            mScroller.abortAnimation();
        }
        return true;
    }

    @Override
    public void onShowPress(MotionEvent e) {

    }

    @Override
    public boolean onSingleTapUp(MotionEvent e) {
        return false;
    }

    @Override
    public boolean onScroll(MotionEvent e1, MotionEvent e2, float dX, float dY) {
        switch (mImageType) {
            case VERTICAL_LARGE_IMAGE:
                mRect.offset(0, (int) dY);
                if (mRect.bottom > mImageHeight) {
                    mRect.bottom = mImageHeight;
                    mRect.top = mImageHeight - (int) (mViewHeight / mScale);
                }
                if (mRect.top < 0) {
                    mRect.top = 0;
                    mRect.bottom = (int) (mViewHeight / mScale);
                }
                break;
            case HORIZON_LARGE_IMAGE:
                mRect.offset((int) dX, 0);
                if (mRect.right > mImageWidth) {
                    mRect.right = mImageWidth;
                    mRect.left = mImageWidth - (int) (mViewWidth / mScale);
                }
                if (mRect.left < 0) {
                    mRect.left = 0;
                    mRect.right = (int) (mViewWidth / mScale);
                }
                break;
        }
        invalidate();
        return false;
    }

    @Override
    public void onLongPress(MotionEvent e) {

    }

    @Override
    public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
        switch (mImageType) {
            case VERTICAL_LARGE_IMAGE:
                mScroller.fling(0, mRect.top, 0, (int) -velocityY, 0, 0, 0, mImageHeight - (int) (mViewHeight / mScale));
                break;
            case HORIZON_LARGE_IMAGE:
                mScroller.fling(mRect.left, 0, (int) -velocityX, 0, 0, mImageWidth - (int) (mViewWidth / mScale), 0, 0);
                break;
        }
        return false;
    }

    @Override
    public void computeScroll() {
        if (mScroller.computeScrollOffset()) {
            if (mImageType == TYPE.VERTICAL_LARGE_IMAGE) {
                mRect.top = mScroller.getCurrY();
                mRect.bottom = (int) (mScroller.getCurrY() + mImageHeight * mScale);
            } else if (mImageType == TYPE.HORIZON_LARGE_IMAGE) {
                mRect.left = mScroller.getCurrX();
                mRect.right = (int) (mScroller.getCurrX() + mImageWidth * mScale);
            }
            invalidate();
        }
    }
}
