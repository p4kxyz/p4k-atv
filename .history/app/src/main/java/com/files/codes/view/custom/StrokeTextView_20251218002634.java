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
            // We use the TextPaint directly to avoid setTextColor() which triggers invalidation
            TextPaint paint = getPaint();
            
            // Save original state
            Paint.Style originalStyle = paint.getStyle();
            int originalColor = paint.getColor();
            float originalStrokeWidth = paint.getStrokeWidth();
            Paint.Join originalJoin = paint.getStrokeJoin();
            
            // Configure for stroke
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(strokeWidth);
            paint.setColor(strokeColor);
            paint.setStrokeJoin(Paint.Join.ROUND); // Round join prevents spikes
            
            // Draw the stroke
            super.onDraw(canvas);
            
            // Restore original state for fill
            paint.setStyle(originalStyle);
            paint.setColor(originalColor);
            paint.setStrokeWidth(originalStrokeWidth);
            paint.setStrokeJoin(originalJoin);
        }
        
        // Draw the fill (standard text)
        super.onDraw(canvas);
    }
}
