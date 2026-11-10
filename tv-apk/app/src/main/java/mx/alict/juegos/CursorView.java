package mx.alict.juegos;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.view.View;

/** Cursor grande en forma de estrella para jugar con el control remoto. No recibe toques. */
public class CursorView extends View {
    private float x, y;
    private boolean presionado = false;
    private final Paint relleno = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint borde = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint sombra = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint punto = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path estrella = new Path();

    public CursorView(Context c) {
        super(c);
        setFocusable(false);
        setClickable(false);
        relleno.setStyle(Paint.Style.FILL);
        relleno.setColor(0xFFFFD24D);
        borde.setStyle(Paint.Style.STROKE);
        borde.setColor(0xFFFFFFFF);
        borde.setStrokeJoin(Paint.Join.ROUND);
        sombra.setStyle(Paint.Style.FILL);
        sombra.setColor(0x66000000);
        punto.setStyle(Paint.Style.FILL);
        punto.setColor(0xFFD62839);
    }

    public void poner(float nx, float ny) {
        x = nx;
        y = ny;
        invalidate();
    }

    public void setPresionado(boolean p) {
        presionado = p;
        invalidate();
    }

    private void trazar(float cx, float cy, float R) {
        estrella.reset();
        float r = R * 0.45f;
        for (int i = 0; i < 10; i++) {
            double a = -Math.PI / 2 + i * Math.PI / 5;
            float rr = (i % 2 == 0) ? R : r;
            float px = cx + (float) Math.cos(a) * rr, py = cy + (float) Math.sin(a) * rr;
            if (i == 0) estrella.moveTo(px, py); else estrella.lineTo(px, py);
        }
        estrella.close();
    }

    @Override
    protected void onDraw(Canvas c) {
        float R = getHeight() * 0.045f * (presionado ? 0.8f : 1f);
        borde.setStrokeWidth(Math.max(3f, R * 0.16f));
        trazar(x + R * 0.08f, y + R * 0.12f, R);
        c.drawPath(estrella, sombra);
        trazar(x, y, R);
        c.drawPath(estrella, relleno);
        c.drawPath(estrella, borde);
        c.drawCircle(x, y, Math.max(3f, R * 0.14f), punto);
        if (presionado) c.drawCircle(x, y, R * 1.35f, borde);
    }
}
