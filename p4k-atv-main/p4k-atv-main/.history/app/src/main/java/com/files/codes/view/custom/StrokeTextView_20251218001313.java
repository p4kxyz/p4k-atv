package com.files.codes.view.custom;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.util.AttributeSet;
import androidx.appcompat.widget.AppCompatTextView;

public class StrokeTextView extends AppCompatTextView {
    private float strokeWidth;
    private int strokeColor;
    private Paint.Join strokeJoin;
    private float strokeMiter;

    public StrokeTextView(Context context) {
        super(context);
        init(null);
    }

    public StrokeTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(attrs);
    }

    public StrokeTextView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(attrs);
    }

    public void init(AttributeSet attrs) {
        strokeWidth = 0;
        strokeColor = Color.BLACK;
        strokeJoin = Paint.Join.ROUND;
        strokeMiter = 10;
    }

    public void setStroke(float width, int color) {
        strokeWidth = width;
        strokeColor = color;
        invalidate(); // Request redraw
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (strokeWidth > 0) {
            // Save the current text color
            int currentTextColor = getCurrentTextColor();
            
            // Configure paint for stroke
            TextPaint paint = getPaint();
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(strokeWidth);
            setTextColor(strokeColor); // Set text color to stroke color temporarily
            
            // Draw the stroke
            super.onDraw(canvas);
            
            // Restore paint for fill
            paint.setStyle(Paint.Style.FILL);
            setTextColor(currentTextColor); // Restore original text color
        }
        
        // Draw the fill (standard text)
        super.onDraw(canvas);
    }
}
