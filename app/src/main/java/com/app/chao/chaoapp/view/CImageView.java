package com.app.chao.chaoapp.view;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.support.v7.widget.AppCompatImageView;
import android.util.AttributeSet;

/**
 * 自定义圆角ImageView
 *
 * @author Chao
 * @datetime: 2016年4月7日 下午6:40:13 Chao
 */
public class CImageView extends AppCompatImageView {
    private Paint paint;
    private Paint paintBorder;
    /**
     * 圆角的幅度
     **/
    private float mRadius = 0f;
    /**
     * 是否是圆形
     **/
    private boolean mIsCircle = true;

    public CImageView(final Context context) {
        this(context, null);
    }

    public float getmRadius() {
        return mRadius;
    }

    public void setmRadius(float mRadius) {
        this.mRadius = mRadius;
    }

    public boolean ismIsCircle() {
        return mIsCircle;
    }

    public void setmIsCircle(boolean mIsCircle) {
        this.mIsCircle = mIsCircle;
    }

    public CImageView(Context context, AttributeSet attrs) {
        // this(context, attrs, R.attr.customImageViewStyle);
        this(context, attrs, 0);
    }

    public CImageView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        this.setScaleType(ScaleType.FIT_XY);
        // 获取自定属性配置
        // TypedArray ta = context.obtainStyledAttributes(attrs,
        // R.styleable.CustomImageView, defStyle, 0);
        // mRadius = ta.getDimension(R.styleable.CustomImageView_radius, 0);
        // mIsCircle = ta.getBoolean(R.styleable.CustomImageView_circle, false);
        // ta.recycle();
        paint = new Paint();
        paint.setAntiAlias(true);
        paintBorder = new Paint();
        paintBorder.setAntiAlias(true);
    }

    @Override
    public void onDraw(Canvas canvas) {
        // 去点padding值
        int width = canvas.getWidth() - getPaddingLeft() - getPaddingRight();
        int height = canvas.getHeight() - getPaddingTop() - getPaddingBottom();
        // 获取iamgeView 中的drawable 并转成bitmap
        Drawable drawable = getDrawable();
        if (drawable == null) return;
        Bitmap image = drawableToBitmap(drawable);
        // 对获取到的bitmap 按照当前imageView的宽高进行缩放
        Bitmap reSizeImage = reSizeImage(image, width, height);
        // 判断当前需要绘制圆角还是圆
        if (mIsCircle) {
            canvas.drawBitmap(createCircleImage(reSizeImage, width, height),
                    getPaddingLeft(), getPaddingTop(), null);

        } else {
            canvas.drawBitmap(createRoundImage(reSizeImage, width, height),
                    getPaddingLeft(), getPaddingTop(), null);
        }
    }

    /**
     * 绘制圆角
     **/
    private Bitmap createRoundImage(Bitmap source, int width, int height) {
        final Paint paint = new Paint();
        paint.setAntiAlias(true);
        Bitmap target = Bitmap.createBitmap(width, height, Config.ARGB_8888);
        Canvas canvas = new Canvas(target);
        RectF rect = new RectF(0, 0, width, height);
        canvas.drawRoundRect(rect, mRadius, mRadius, paint);
        // 关键代码 设置画笔的Mode 设置为获取交集
        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
        canvas.drawBitmap(source, 0, 0, paint);
        return target;
    }

    private Bitmap createCircleImage(Bitmap source, int width, int height) {
        final Paint paint = new Paint();
        paint.setAntiAlias(true);
        Bitmap target = Bitmap.createBitmap(width, height, Config.ARGB_8888);
        Canvas canvas = new Canvas(target);
        canvas.drawCircle(width / 2, height / 2, Math.min(width, height) / 2,
                paint);
        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
        canvas.drawBitmap(source, 0, 0, paint);
        return target;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = MeasureSpec.getSize(widthMeasureSpec);
        int height = MeasureSpec.getSize(heightMeasureSpec);
        setMeasuredDimension(width, height);
    }

    /**
     * drawable转bitmap
     *
     * @param drawable
     * @return
     */
    private Bitmap drawableToBitmap(Drawable drawable) {
        if (drawable == null) {
            return null;
        } else if (drawable instanceof BitmapDrawable) {
            return ((BitmapDrawable) drawable).getBitmap();
        }
        Bitmap bitmap = Bitmap.createBitmap(drawable.getIntrinsicHeight(),
                drawable.getIntrinsicHeight(), Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);
        return bitmap;
    }

    /**
     * 重设Bitmap的宽高
     *
     * @param bitmap
     * @param newWidth
     * @param newHeight
     * @return
     */
    private Bitmap reSizeImage(Bitmap bitmap, int newWidth, int newHeight) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        // 计算出缩放比
        float scaleWidth = ((float) newWidth) / width;
        float scaleHeight = ((float) newHeight) / height;
        // 矩阵缩放bitmap
        Matrix matrix = new Matrix();
        matrix.postScale(scaleWidth, scaleHeight);
        return Bitmap.createBitmap(bitmap, 0, 0, width, height, matrix, true);
    }

}