package io.github.iammaplezeng.flowmeter;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.View;

/** 首页顶部的气泡背景。 */
public class BubbleView extends View {

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint ring = new Paint(Paint.ANTI_ALIAS_FLAG);

    public BubbleView(Context c) {
        super(c);
    }

    public BubbleView(Context c, AttributeSet a) {
        super(c, a);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        float w = getWidth(), h = getHeight();
        float cx = w / 2f, cy = h / 2f;
        float r = Math.min(w, h) * 0.42f;

        paint.setShader(new RadialGradient(cx, cy - r * 0.1f, r * 1.15f,
                new int[]{0x4DFFFFFF, 0x33FFFFFF, 0x14FFFFFF},
                new float[]{0f, 0.55f, 1f}, Shader.TileMode.CLAMP));
        canvas.drawCircle(cx, cy, r, paint);
        paint.setShader(null);

        paint.setColor(0x33FFFFFF);
        canvas.drawCircle(cx, cy, r * 1.28f, paint);

        paint.setColor(0x30FFFFFF);
        canvas.drawCircle(cx - r * 1.02f, cy + r * 0.22f, r * 0.20f, paint);
        canvas.drawCircle(cx + r * 1.06f, cy - r * 0.32f, r * 0.13f, paint);
        canvas.drawCircle(cx + r * 0.82f, cy + r * 0.62f, r * 0.07f, paint);

        ring.setStyle(Paint.Style.STROKE);
        ring.setStrokeWidth(2f);
        ring.setColor(0x40FFFFFF);
        canvas.drawCircle(cx, cy, r * 0.985f, ring);
    }
}
