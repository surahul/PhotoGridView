package com.surahul.photogridview.internal;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.ImageView;

/**
 * Created by Rahul on 6/20/2015.
 */
public class SquareImageView extends ImageView {
    public SquareImageView(Context context) {
        super(context);
        init();
    }

    public SquareImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public SquareImageView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private int heightOffset = 0;

    private void init(){
        setScaleType(ScaleType.CENTER_CROP);
    }

    public void setHeightOffset(int offset){
        this.heightOffset = offset;
        invalidate();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        int width = getMeasuredWidth();
        setMeasuredDimension(width, width+heightOffset);
    }

}
