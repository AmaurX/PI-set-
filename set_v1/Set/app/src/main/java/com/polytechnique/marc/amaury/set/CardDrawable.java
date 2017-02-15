package com.polytechnique.marc.amaury.set;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.ColorMatrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;

/**
 * A drawable graphical representation of a Set! card.
 */
class CardDrawable extends Drawable {
    /**
     * Padding between shapes of a same card, as well as between
     * shapes and borders; expressed as a fraction of the bounding
     * dimension (width or height).
     */
    private static final float SHAPE_PADDING = 0.125F;

    /** Basic spacing step between concentric shapes. */
    private static final float CONCENTRIC_STEP = 6.0F;

    /** The card to draw. */
    private int card;

    /**
     * Whether the card is selected, or in the middle of a transition
     * (valid or invalid).
     */
    private boolean selected;

    /**
     * The paint used internally to draw all parts of the card. It may
     * be mutated by the various drawing methods.
     */
    private final Paint paint = new Paint();

    /**
     * The rectangle used internally to draw the various parts of the
     * card; when a drawing method is called, it is set to the current
     * bounds for the part to be drawn.
     * <p>
     * We reuse a same rectangle throughout the object to avoid
     * creating too many objects that will require garbage collection.
     */
    private final RectF r = new RectF();

    /**
     * A path object used internally to draw diamond shapes.
     */
    private final Path p = new Path();

    /**
     * Creates a drawable that draws the specified card.
     * @param card the card to draw
     */
    CardDrawable(int card) {
        this.card = card;
    }

    /**
     * @returns the card drawn by this drawable
     */
    int getCard() {
        return card;
    }

    /**
     * Sets the card to draw. If any view is displaying the drawable,
     * it should be invalidated.
     * @param card the card to draw
     */
    void setCard(int card) {
        this.card = card;
    }

    public void isSelected(boolean selection){
        selected = selection;
    }

    @Override
    public void draw(Canvas canvas) {
        if (card == 0)
            return;

        Rect b = getBounds();
        int alpha = paint.getAlpha();

        /* Draw border or background. */
        paint.setColor(Color.DKGRAY);
        paint.setAlpha(alpha);
        paint.setStyle(Paint.Style.STROKE);
        canvas.drawRect(b, paint);
         // if(selected)
         //    paint.setColorFilter(new ColorFilter());
         //   }

        /* Pick a color. */
        switch (Cards.colorOf(card)) {
        case 1:
            paint.setColor(0xFF882222);
            break;
        case 2:
            paint.setColor(0xFF228822);
            break;
        case 3:
            paint.setColor(0xFF222288);
            break;
        default:
            throw new IllegalStateException("Illegal color characteristic.");
        }
        paint.setAlpha(alpha);

        /* Draw shapes. */
        int n = Cards.numberOf(card);
        float hPadding = b.width() * SHAPE_PADDING;
        float vPadding = b.height() * SHAPE_PADDING;
        float h = (b.height() - (n + 1) * vPadding) / 3.0F;
        float t = b.top + (b.height() - n * h - (n - 1) * vPadding) / 2.0F;
        for (int i = 0; i < n; ++i) {
            /*
             * Reset the Rect r as it may have been overriden in
             * drawShape(). We also keep the variable t local for the
             * same reason.
             */
            r.left = b.left + hPadding;
            r.right = b.right - hPadding;
            r.top = t;
            r.bottom = t + h;
            t = r.bottom + vPadding;
            drawShapeWithFilling(canvas,
                                 Cards.fillingOf(card), Cards.shapeOf(card));
        }
    }

    /**
     * Draws a single shape with the specified filling.
     * @param canvas the canvas on which to draw
     * @param filling the filling characteristic to draw
     * @param shape the shape characteristic to draw
     */
    private void drawShapeWithFilling(Canvas canvas, int filling, int shape) {
        switch (filling) {
        case 1:
            paint.setStyle(Paint.Style.STROKE);
            drawShape(canvas, shape);
            break;
        case 2:
            /*
             * For intermediate filling, we draw concentric copies of
             * the same shape.
             */
            paint.setStyle(Paint.Style.STROKE);
            float w = r.width() / 2.0F;
            float u = CONCENTRIC_STEP * (r.height() / r.width());
            for (float i = 0; i < w; i += CONCENTRIC_STEP) {
                drawShape(canvas, shape);
                r.left += CONCENTRIC_STEP;
                r.top += u;
                r.right -= CONCENTRIC_STEP;
                r.bottom -= u;
            }
            break;
        case 3:
            paint.setStyle(Paint.Style.FILL);
            drawShape(canvas, shape);
            break;
        default:
            throw new IllegalArgumentException(
                "Illegal filling characteristic.");
        }
    }

    /**
     * Draws a single shape.
     * @param canvas the canvas on which to draw
     * @param shape the shape characteristic to draw
     */
    private void drawShape(Canvas canvas, int shape) {
        switch (shape) {
        case 1:
            canvas.drawOval(r, paint);
            break;
        case 2:
            canvas.drawRect(r, paint);
            break;
        case 3:
            drawDiamond(canvas);
            break;
        default:
            throw new IllegalArgumentException(
                "Illegal shape characteristic.");
        }
    }

    /**
     * Draws a diamond shape within the specified rectangle.
     * @param canvas the canvas on which to draw
     */
    private void drawDiamond(Canvas canvas) {
        p.moveTo(r.left, r.centerY());
        p.lineTo(r.centerX(), r.top);
        p.lineTo(r.right, r.centerY());
        p.lineTo(r.centerX(), r.bottom);
        p.lineTo(r.left, r.centerY());
        canvas.drawPath(p, paint);
    }

    @Override
    public int getOpacity() {
        return PixelFormat.OPAQUE;
    }

    @Override
    public void setAlpha(int alpha) {
        paint.setAlpha(alpha);
    }

    @Override
    public void setColorFilter(ColorFilter cf) {
        paint.setColorFilter(cf);
    }
}
