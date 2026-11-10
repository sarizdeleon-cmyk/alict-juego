package mx.alict.juegos;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.util.Base64;
import android.view.InputDevice;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.webkit.JavascriptInterface;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Menú de juegos de Alict para Android TV.
 * - Todo el contenido (menú + juegos) va dentro de la app (carpeta assets/www) y se sirve desde una dirección falsa
 *   (https://juegos.alict/), así funciona sin internet y no puede abrir nada de afuera.
 * - En el menú, el control remoto manda las flechas/OK a la página.
 * - Dentro de un juego, las flechas mueven un cursor (estrella) y OK "toca" la pantalla: sirve para cualquier juego.
 * - Atrás: cierra la galería, o vuelve al menú, o sale de la app.
 */
public class MainActivity extends Activity {
    static final String HOST = "juegos.alict";
    static final String ORIGEN = "https://" + HOST;
    // se inserta en cada página: avisa que es la tele y apaga el service worker (aquí no hace falta)
    static final String INYECTAR = "<script>window.__TV=true;try{delete Navigator.prototype.serviceWorker}catch(e){}"
            + "try{if('serviceWorker' in navigator)Object.defineProperty(navigator,'serviceWorker',{get:function(){return undefined}})}catch(e){}</script>";

    private WebView web;
    private CursorView cursor;
    private boolean enJuego = false;
    private boolean esInicio = false;                       // la app está funcionando como pantalla de inicio de la tele
    private final Map<String, String> dibujos = new HashMap<String, String>();

    private float cx, cy;                       // posición del cursor
    private boolean izq, der, arr, aba;         // flechas apretadas
    private float ax, ay;                       // palanca del control de videojuegos
    private float tiempoMov = 0;
    private boolean oprimido = false;
    private long tBajo = 0, tUlt = 0;
    private boolean corriendo = false;
    private final Handler h = new Handler(Looper.getMainLooper());
    private final Runnable ciclo = new Runnable() {
        @Override public void run() { avanzar(); }
    };

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        FrameLayout raiz = new FrameLayout(this);
        raiz.setBackgroundColor(0xFF0A1A44);

        web = new WebView(this);
        web.setBackgroundColor(0xFF0A1A44);
        raiz.addView(web, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        cursor = new CursorView(this);
        cursor.setVisibility(View.GONE);
        raiz.addView(cursor, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        setContentView(raiz);

        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);                       // aquí se guarda el avance de Alict
        s.setMediaPlaybackRequiresUserGesture(false);       // la música y la voz arrancan solas
        s.setAllowFileAccess(false);
        s.setAllowContentAccess(false);
        s.setSupportZoom(false);
        web.setOverScrollMode(View.OVER_SCROLL_NEVER);
        web.setVerticalScrollBarEnabled(false);
        web.setHorizontalScrollBarEnabled(false);
        web.setLongClickable(false);

        web.addJavascriptInterface(new PuenteApps(), "AlictApps");
        esInicio = getIntent() != null && getIntent().hasCategory(Intent.CATEGORY_HOME);
        web.setWebChromeClient(new WebChromeClient());
        web.setWebViewClient(new WebViewClient() {
            @Override
            public WebResourceResponse shouldInterceptRequest(WebView v, WebResourceRequest r) {
                return servir(r.getUrl());
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView v, String url) {
                return !url.startsWith(ORIGEN);                // nada de salir a internet
            }

            @Override
            public void onPageStarted(WebView v, String url, Bitmap icono) {
                Uri u = Uri.parse(url);
                String p = u.getPath();
                boolean menu = p == null || p.length() == 0 || p.equals("/") || p.equals("/index.html");
                ponerModo(!menu);
            }
        });
        web.setFocusable(true);
        web.setFocusableInTouchMode(true);
        web.requestFocus();
        web.loadUrl(ORIGEN + "/");
    }

    /* ---------- archivos de la app ---------- */

    private WebResourceResponse servir(Uri u) {
        if (u == null || !HOST.equals(u.getHost())) return respuesta404();
        String p = u.getPath();
        if (p == null || p.length() == 0) p = "/";
        if (p.endsWith("/")) p = p + "index.html";
        if (p.contains("..")) return respuesta404();
        try {
            InputStream in = getAssets().open("www" + p);
            String tipo = tipoDe(p);
            if (tipo.equals("text/html")) {
                String t = new String(leer(in), "UTF-8");
                int i = t.indexOf("<head");
                int j = i >= 0 ? t.indexOf('>', i) : -1;
                if (j >= 0) t = t.substring(0, j + 1) + INYECTAR + t.substring(j + 1);
                else t = t + INYECTAR;                          // sin <head>: al final, para no romper el doctype
                return respuesta(tipo, new ByteArrayInputStream(t.getBytes("UTF-8")));
            }
            return respuesta(tipo, in);
        } catch (IOException | RuntimeException e) {
            return respuesta404();
        }
    }

    private WebResourceResponse respuesta(String tipo, InputStream in) {
        Map<String, String> cab = new HashMap<String, String>();
        cab.put("Cache-Control", "no-cache");
        boolean texto = tipo.startsWith("text/") || tipo.contains("javascript") || tipo.contains("json") || tipo.contains("xml");
        return new WebResourceResponse(tipo, texto ? "UTF-8" : null, 200, "OK", cab, in);
    }

    private WebResourceResponse respuesta404() {
        return new WebResourceResponse("text/plain", "UTF-8", 404, "Not Found", new HashMap<String, String>(), new ByteArrayInputStream(new byte[0]));
    }

    private static byte[] leer(InputStream in) throws IOException {
        ByteArrayOutputStream o = new ByteArrayOutputStream(Math.max(32, in.available()));
        byte[] buf = new byte[16384];
        int n;
        while ((n = in.read(buf)) > 0) o.write(buf, 0, n);
        in.close();
        return o.toByteArray();
    }

    private static String tipoDe(String p) {
        String q = p.toLowerCase();
        if (q.endsWith(".html") || q.endsWith(".htm")) return "text/html";
        if (q.endsWith(".js") || q.endsWith(".mjs")) return "application/javascript";
        if (q.endsWith(".css")) return "text/css";
        if (q.endsWith(".json") || q.endsWith(".webmanifest")) return "application/json";
        if (q.endsWith(".png")) return "image/png";
        if (q.endsWith(".jpg") || q.endsWith(".jpeg")) return "image/jpeg";
        if (q.endsWith(".webp")) return "image/webp";
        if (q.endsWith(".gif")) return "image/gif";
        if (q.endsWith(".svg")) return "image/svg+xml";
        if (q.endsWith(".mp3")) return "audio/mpeg";
        if (q.endsWith(".ogg")) return "audio/ogg";
        if (q.endsWith(".wav")) return "audio/wav";
        if (q.endsWith(".woff2")) return "font/woff2";
        if (q.endsWith(".woff")) return "font/woff";
        if (q.endsWith(".txt") || q.endsWith(".md")) return "text/plain";
        return "application/octet-stream";
    }

    /* ---------- menú o juego ---------- */

    private void ponerModo(boolean juego) {
        enJuego = juego;
        izq = der = arr = aba = false;
        ax = ay = 0;
        oprimido = false;
        cursor.setPresionado(false);
        cursor.setVisibility(juego ? View.VISIBLE : View.GONE);
        if (juego) {
            cx = Math.max(1, web.getWidth()) / 2f;
            cy = Math.max(1, web.getHeight()) * 0.6f;
            cursor.poner(cx, cy);
        }
    }

    @Override
    public void onBackPressed() {
        if (enJuego) {
            web.loadUrl(ORIGEN + "/");                          // de un juego vuelve al menú
            return;
        }
        // en el menú: si hay una galería abierta se cierra; si no, sale de la app
        web.evaluateJavascript("(function(){return window.__tvAtras?window.__tvAtras():false})()", new ValueCallback<String>() {
            @Override public void onReceiveValue(String v) {
                if (!"true".equals(v) && !esInicio) finish();      // como pantalla de inicio no se cierra
            }
        });
    }

    /* ---------- apps instaladas en la tele ---------- */

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        esInicio = intent != null && intent.hasCategory(Intent.CATEGORY_HOME);
        if (esInicio) web.loadUrl(ORIGEN + "/");              // al apretar Inicio vuelve siempre al menú
    }

    /** Lo que la página puede pedirle a la app: la lista de apps instaladas, sus dibujos, y abrir una. */
    private class PuenteApps {
        @JavascriptInterface
        public String lista() {
            try { return listarApps(); } catch (Exception e) { return "[]"; }
        }

        @JavascriptInterface
        public String icono(String pkg) {
            String d = dibujos.get(pkg);
            return d == null ? "" : d;
        }

        @JavascriptInterface
        public void abrir(final String pkg) {
            runOnUiThread(new Runnable() {
                @Override public void run() { abrirApp(pkg); }
            });
        }
    }

    private String listarApps() throws Exception {
        final PackageManager pm = getPackageManager();
        Map<String, ResolveInfo> por = new LinkedHashMap<String, ResolveInfo>();
        Intent a = new Intent(Intent.ACTION_MAIN);
        a.addCategory("android.intent.category.LEANBACK_LAUNCHER");
        for (ResolveInfo r : pm.queryIntentActivities(a, 0)) por.put(r.activityInfo.packageName, r);
        Intent b = new Intent(Intent.ACTION_MAIN);
        b.addCategory(Intent.CATEGORY_LAUNCHER);
        for (ResolveInfo r : pm.queryIntentActivities(b, 0)) {
            if (!por.containsKey(r.activityInfo.packageName)) por.put(r.activityInfo.packageName, r);
        }
        por.remove(getPackageName());                           // esta misma app no se lista
        List<JSONObject> lista = new ArrayList<JSONObject>();
        for (Map.Entry<String, ResolveInfo> en : por.entrySet()) {
            String pkg = en.getKey();
            ResolveInfo r = en.getValue();
            JSONObject o = new JSONObject();
            o.put("p", pkg);
            o.put("n", String.valueOf(r.loadLabel(pm)));
            lista.add(o);
            if (!dibujos.containsKey(pkg)) dibujos.put(pkg, dibujoDe(r, pm));
        }
        Collections.sort(lista, new Comparator<JSONObject>() {
            @Override public int compare(JSONObject x, JSONObject y) {
                return x.optString("n").toLowerCase().compareTo(y.optString("n").toLowerCase());
            }
        });
        JSONArray arr = new JSONArray();
        for (JSONObject o : lista) arr.put(o);
        return arr.toString();
    }

    /** Imagen de la app (el banner de TV si lo tiene, si no el icono) como PNG en base64. */
    private String dibujoDe(ResolveInfo r, PackageManager pm) {
        try {
            Drawable d = null;
            try { d = r.activityInfo.loadBanner(pm); } catch (Exception e) { d = null; }
            if (d == null) { try { d = r.activityInfo.applicationInfo.loadBanner(pm); } catch (Exception e) { d = null; } }
            boolean banner = d != null;
            if (d == null) d = r.loadIcon(pm);
            int w = banner ? 224 : 128, h = banner ? 126 : 128;
            Bitmap bm = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
            Canvas c = new Canvas(bm);
            d.setBounds(0, 0, w, h);
            d.draw(c);
            ByteArrayOutputStream o = new ByteArrayOutputStream();
            bm.compress(Bitmap.CompressFormat.PNG, 100, o);
            bm.recycle();
            return "data:image/png;base64," + Base64.encodeToString(o.toByteArray(), Base64.NO_WRAP);
        } catch (Exception e) {
            return "";
        }
    }

    private void abrirApp(String pkg) {
        try {
            PackageManager pm = getPackageManager();
            Intent i = pm.getLeanbackLaunchIntentForPackage(pkg);
            if (i == null) i = pm.getLaunchIntentForPackage(pkg);
            if (i != null) {
                i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(i);
            }
        } catch (Exception e) {
            // si no se puede abrir, no pasa nada
        }
    }

    /* ---------- control remoto -> cursor ---------- */

    @Override
    public boolean dispatchKeyEvent(KeyEvent e) {
        if (!enJuego) return super.dispatchKeyEvent(e);
        boolean abajo = e.getAction() == KeyEvent.ACTION_DOWN;
        switch (e.getKeyCode()) {
            case KeyEvent.KEYCODE_DPAD_LEFT:  izq = abajo; mover(); return true;
            case KeyEvent.KEYCODE_DPAD_RIGHT: der = abajo; mover(); return true;
            case KeyEvent.KEYCODE_DPAD_UP:    arr = abajo; mover(); return true;
            case KeyEvent.KEYCODE_DPAD_DOWN:  aba = abajo; mover(); return true;
            case KeyEvent.KEYCODE_DPAD_CENTER:
            case KeyEvent.KEYCODE_ENTER:
            case KeyEvent.KEYCODE_NUMPAD_ENTER:
            case KeyEvent.KEYCODE_BUTTON_A:
            case KeyEvent.KEYCODE_SPACE:
                if (abajo) { if (e.getRepeatCount() == 0) oprimir(true); } else oprimir(false);
                return true;
            default:
                return super.dispatchKeyEvent(e);
        }
    }

    @Override
    public boolean onGenericMotionEvent(MotionEvent e) {
        if (enJuego && (e.getSource() & InputDevice.SOURCE_JOYSTICK) == InputDevice.SOURCE_JOYSTICK && e.getAction() == MotionEvent.ACTION_MOVE) {
            ax = zona(e.getAxisValue(MotionEvent.AXIS_X));
            ay = zona(e.getAxisValue(MotionEvent.AXIS_Y));
            mover();
            return true;
        }
        return super.onGenericMotionEvent(e);
    }

    private static float zona(float v) { return Math.abs(v) < 0.2f ? 0f : v; }

    private void mover() {
        if (corriendo) return;
        corriendo = true;
        tUlt = SystemClock.uptimeMillis();
        h.post(ciclo);
    }

    private void avanzar() {
        long ahora = SystemClock.uptimeMillis();
        float dt = Math.min(50, ahora - tUlt) / 1000f;
        tUlt = ahora;
        float vx = (der ? 1 : 0) - (izq ? 1 : 0) + ax;
        float vy = (aba ? 1 : 0) - (arr ? 1 : 0) + ay;
        vx = Math.max(-1f, Math.min(1f, vx));
        vy = Math.max(-1f, Math.min(1f, vy));
        if (vx == 0f && vy == 0f) { corriendo = false; tiempoMov = 0; return; }
        tiempoMov += dt;
        float ancho = Math.max(1, web.getWidth()), alto = Math.max(1, web.getHeight());
        float v = alto * Math.min(1.5f, 0.32f + 1.4f * tiempoMov);       // acelera mientras se mantiene apretada la flecha
        cx = Math.max(0f, Math.min(ancho - 1f, cx + vx * v * dt));
        cy = Math.max(0f, Math.min(alto - 1f, cy + vy * v * dt));
        cursor.poner(cx, cy);
        if (oprimido) toque(MotionEvent.ACTION_MOVE);
        h.postDelayed(ciclo, 16);
    }

    private void oprimir(boolean on) {
        if (on == oprimido) return;
        oprimido = on;
        cursor.setPresionado(on);
        toque(on ? MotionEvent.ACTION_DOWN : MotionEvent.ACTION_UP);
    }

    /** Manda un toque de verdad (como un dedo) a la página en la posición del cursor. */
    private void toque(int accion) {
        long ahora = SystemClock.uptimeMillis();
        if (accion == MotionEvent.ACTION_DOWN) tBajo = ahora;
        MotionEvent.PointerProperties[] pp = new MotionEvent.PointerProperties[1];
        pp[0] = new MotionEvent.PointerProperties();
        pp[0].id = 0;
        pp[0].toolType = MotionEvent.TOOL_TYPE_FINGER;
        MotionEvent.PointerCoords[] pc = new MotionEvent.PointerCoords[1];
        pc[0] = new MotionEvent.PointerCoords();
        pc[0].x = cx;
        pc[0].y = cy;
        pc[0].pressure = 1f;
        pc[0].size = 1f;
        MotionEvent ev = MotionEvent.obtain(tBajo, ahora, accion, 1, pp, pc, 0, 0, 1f, 1f, 0, 0, InputDevice.SOURCE_TOUCHSCREEN, 0);
        web.dispatchTouchEvent(ev);
        ev.recycle();
    }

    /* ---------- ciclo de vida ---------- */

    @Override
    public void onWindowFocusChanged(boolean foco) {
        super.onWindowFocusChanged(foco);
        if (foco) {
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        izq = der = arr = aba = false;
        ax = ay = 0;
        web.onPause();
        web.pauseTimers();
    }

    @Override
    protected void onResume() {
        super.onResume();
        web.onResume();
        web.resumeTimers();
        web.requestFocus();
    }

    @Override
    protected void onDestroy() {
        h.removeCallbacks(ciclo);
        ViewGroup padre = (ViewGroup) web.getParent();
        if (padre != null) padre.removeView(web);
        web.destroy();
        super.onDestroy();
    }
}
