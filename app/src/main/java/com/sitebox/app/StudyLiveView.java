package com.sitebox.app;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.SurfaceTexture;
import android.media.MediaPlayer;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.widget.FrameLayout;

/** Study-only video and atmospheric layer. The source room and all window frames stay still. */
final class StudyLiveView extends FrameLayout {
    private static final long FRAME_MS = 42; // At most 24 atmospheric updates/second.
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Matrix sceneMatrix = new Matrix(), textureMatrix = new Matrix(), nightMatrix = new Matrix();
    private final Matrix roomMaskMatrix=new Matrix();
    private final Matrix outsideNightMatrix=new Matrix();
    private Bitmap outsideDay,outsideNight;
    private boolean studentRoom,reducedEffects;
    private Runnable frameListener;
    void setStudentRoom(boolean value){studentRoom=value;updateGeometry();}
    void setFrameListener(Runnable listener){frameListener=listener;}
    void setReducedEffects(boolean value){if(reducedEffects==value)return;reducedEffects=value;if(value&&studentRoom)stopVideo();else startVideoIfReady();}
    void setWindowTextures(Bitmap day,Bitmap night){outsideDay=day;outsideNight=night;updateGeometry();}
    void setRoomTransforms(float[] room,float[] outside){
        roomMaskMatrix.setScale(room[0],room[0]);roomMaskMatrix.postTranslate(room[1],room[2]);
        scale=outside[0];offsetX=outside[1];offsetY=outside[2];updateGeometry();
    }
    private void frameChanged(){if(frameListener!=null)frameListener.run();}
    private final Path glass = new Path();
    private final RectF glassBounds = new RectF();
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
    private final Paint glowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RoomLightView roomLight;
    private boolean lampEnabled=true;
    private final GlassFrame videoClip;
    private final TextureView texture;
    private final Atmosphere atmosphere;
    private Bitmap day, night, lampOffDay, lampOffNight;
    private final Matrix lampOffDayMatrix=new Matrix(),lampOffNightMatrix=new Matrix();
    void setLampPlates(Bitmap dayOff,Bitmap nightOff){lampOffDay=dayOff;lampOffNight=nightOff;updateGeometry();roomLight.invalidate();}
    private boolean wide, active, motionEnabled = true, attached, decoderFailed, firstFrame, renderingStarted, videoNight;
    private float scale = 1, offsetX, offsetY, daylight = 1, targetDaylight = 1;
    private float cloudiness = .25f, targetCloudiness = .25f, rain, snow, targetRain, targetSnow;
    private long lastTick;
    private float lampX, lampY, lampRadius;
    private WeatherState weather = WeatherState.empty();
    private MediaPlayer player;
    private Surface surface;
    private String videoFailure = "none";
    private long frameCount, videoFrameCount, motionOrigin = SystemClock.uptimeMillis();
    private final Runnable tick = new Runnable() {
        @Override public void run() {
            if (!canRunMotion()) return;
            long now = SystemClock.uptimeMillis();
            float blend = Math.min(1, Math.max(0, now-lastTick)/1200f);
            lastTick = now;
            daylight = approach(daylight,targetDaylight,blend);
            cloudiness = approach(cloudiness,targetCloudiness,blend);
            rain = approach(rain,targetRain,blend); snow = approach(snow,targetSnow,blend);
            updateVideoAlpha();
            roomLight.invalidate();
            atmosphere.invalidate();
            frameChanged();
            handler.postDelayed(this, reducedEffects?84:FRAME_MS);
        }
    };

    StudyLiveView(Context context) {
        super(context);
        setClickable(false);
        setFocusable(false);
        setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
        roomLight = new RoomLightView(context);
        addView(roomLight, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
        videoClip = new GlassFrame(context);
        texture = new TextureView(context);
        texture.setOpaque(false);
        texture.setAlpha(0);
        videoClip.addView(texture, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
        addView(videoClip, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
        atmosphere = new Atmosphere(context);
        addView(atmosphere, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
        texture.setSurfaceTextureListener(new TextureView.SurfaceTextureListener() {
            @Override public void onSurfaceTextureAvailable(SurfaceTexture value, int width, int height) {
                updateGeometry();
                startVideoIfReady();
            }
            @Override public void onSurfaceTextureSizeChanged(SurfaceTexture value, int width, int height) { updateGeometry(); }
            @Override public boolean onSurfaceTextureDestroyed(SurfaceTexture value) {
                stopVideo();
                return true;
            }
            @Override public void onSurfaceTextureUpdated(SurfaceTexture value) {
                if (player != null && renderingStarted && canRunMotion()) {
                    videoFrameCount++;
                    firstFrame = true;
                    updateVideoAlpha();
                }
            }
        });
    }

    /** Transform is SceneTheme's {source pixel scale, x translation, y translation}. */
    void setScene(Bitmap day, Bitmap night, boolean wide, float[] imageTransform) {
        boolean changed = this.day != day || this.wide != wide;
        this.day = day;
        this.night = night;
        this.wide = wide;
        if (imageTransform != null && imageTransform.length >= 3 && finite(imageTransform[0])
                && finite(imageTransform[1]) && finite(imageTransform[2]) && imageTransform[0] > 0) {
            scale = imageTransform[0]; offsetX = imageTransform[1]; offsetY = imageTransform[2];
        } else if (day != null && getWidth() > 0 && getHeight() > 0) {
            scale = Math.max(getWidth() / (float)day.getWidth(), getHeight() / (float)day.getHeight());
            offsetX = (getWidth() - day.getWidth() * scale) * .5f;
            offsetY = (getHeight() - day.getHeight() * scale) * .5f;
        } else { scale = 1; offsetX = 0; offsetY = 0; }
        if (changed) { stopVideo(); decoderFailed = false; }
        updateGeometry();
        startVideoIfReady();
        atmosphere.invalidate();
    }

    void setWeather(WeatherState state, long now) {
        weather = state == null ? WeatherState.empty() : state;
        boolean wasNight = targetDaylight < .5f;
        targetDaylight = weather.daylight(now);
        boolean wantsNight = targetDaylight < .5f;
        if (wasNight != wantsNight) decoderFailed = false;
        if (player != null && videoNight != wantsNight) stopVideo();
        targetCloudiness = weather.cloudiness();
        if (weather.code == 45 || weather.code == 48) targetCloudiness = Math.max(targetCloudiness,.85f);
        targetRain = weather.precipitation() == 1 ? 1 : 0;
        targetSnow = weather.precipitation() == 2 ? 1 : 0;
        if (!canRunMotion()) settleAtmosphere();
        startVideoIfReady();
        roomLight.invalidate();
        atmosphere.invalidate();
        frameChanged();
    }
    void setLampEnabled(boolean enabled){if(lampEnabled==enabled)return;lampEnabled=enabled;roomLight.invalidate();atmosphere.invalidate();}
    void setActive(boolean value) { if(value&&!active)decoderFailed=false;active = value; reconcile(); }
    void setMotionEnabled(boolean value) { if(value&&!motionEnabled)decoderFailed=false;motionEnabled = value; reconcile(); }
    void release() { lampOffDay=null;lampOffNight=null;active = false; handler.removeCallbacks(tick); stopVideo(); }
    boolean isPlaying() {
        try { return player != null && player.isPlaying(); }
        catch (IllegalStateException ignored) { return false; }
    }
    boolean hasVideoFrame() { return firstFrame; }
    boolean isNightVideo() { return videoNight; }
    boolean isVideoVisible() { return firstFrame && texture.getAlpha() > .9f && isPlaying() && canRunMotion(); }
    float getDaylight() { return daylight; }
    long renderedFrames() { return videoFrameCount; }
    long atmosphericFrames() { return frameCount; }
    void failVideoForTest() { decoderFailed = true; stopVideo(); atmosphere.invalidate(); }
    long videoPositionForTest() { try { return player == null ? -1 : player.getCurrentPosition(); } catch (IllegalStateException failure) { return -2; } }
    String videoStatusForTest() { return "active="+active+",attached="+attached+",motion="+motionEnabled+",surface="+texture.isAvailable()+",size="+getWidth()+"x"+getHeight()+",playing="+isPlaying()+",frames="+videoFrameCount+",first="+firstFrame+",rendering="+renderingStarted+",alpha="+texture.getAlpha()+",visible="+getVisibility()+",position="+videoPositionForTest()+",failed="+decoderFailed+",error="+videoFailure; }
    private void updateVideoAlpha() {
        float visible = (renderingStarted || firstFrame)
                ? (videoNight ? Math.max(0,1-2*daylight) : Math.max(0,2*daylight-1)) : 0;
        // A zero-alpha TextureView may be culled before its surface is created.
        // Prime the texture with one opacity level even before player creation;
        // release/failure/inactivity still hide it completely. The opaque still
        // underneath remains the visible fallback until a decoder frame arrives.
        texture.setAlpha(canRunMotion() && day != null && !decoderFailed && !(studentRoom&&reducedEffects)
                ? Math.max(1f/255f,visible) : 0);
    }
    private void settleAtmosphere() {
        daylight = targetDaylight; cloudiness = targetCloudiness; rain = targetRain; snow = targetSnow;
        frameChanged();
    }
    private static float approach(float value, float target, float blend) {
        float distance = target-value;
        return value+Math.signum(distance)*Math.min(Math.abs(distance),Math.max(0,blend));
    }
    private static boolean finite(float value) { return !Float.isNaN(value) && !Float.isInfinite(value); }
    private boolean canRunMotion() { return active && attached && motionEnabled; }
    private void reconcile() {
        handler.removeCallbacks(tick);
        if (canRunMotion()) {
            startVideoIfReady();
            lastTick = SystemClock.uptimeMillis();
            handler.post(tick);
        } else { stopVideo(); settleAtmosphere(); roomLight.invalidate(); atmosphere.invalidate(); }
    }
    @Override protected void onAttachedToWindow() { super.onAttachedToWindow(); attached = true; reconcile(); }
    @Override protected void onDetachedFromWindow() {
        attached = false; handler.removeCallbacks(tick); stopVideo(); super.onDetachedFromWindow();
    }
    @Override protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh); updateGeometry();
    }

    private void updateGeometry() {
        glass.rewind();
        if (day == null || day.isRecycled()) return;
        sceneMatrix.setScale(scale, scale);
        sceneMatrix.postTranslate(offsetX, offsetY);
        if (night != null && !night.isRecycled()) {
            // Both plates describe the same normalized composition, at independent resolutions.
            nightMatrix.setScale(day.getWidth()*scale/night.getWidth(), day.getHeight()*scale/night.getHeight());
            nightMatrix.postTranslate(offsetX, offsetY);
        }
        if(studentRoom){
            for(float[] p:RoomScene.GLASS)pane(p[0],p[1],p[2],p[3],p[4],p[5],p[6],p[7]);
        } else if (wide) {
            // Insets leave the sloping rails, handles, curtain and plant entirely static.
            pane(.615f,.064f, .750f,.035f, .750f,.172f, .615f,.195f);
            pane(.775f,.027f, .918f,.006f, .918f,.150f, .775f,.170f);
            pane(.615f,.214f, .750f,.194f, .750f,.389f, .615f,.402f);
            pane(.775f,.190f, .918f,.170f, .918f,.378f, .775f,.389f);
            pane(.615f,.418f, .750f,.406f, .750f,.470f, .615f,.470f);
            pane(.775f,.408f, .918f,.396f, .918f,.490f, .775f,.490f);
        } else {
            pane(.637f,.064f, .889f,.046f, .889f,.168f, .637f,.180f);
            pane(.934f,.041f, 1f,.036f, 1f,.163f, .934f,.168f);
            pane(.637f,.195f, .889f,.182f, .889f,.331f, .637f,.337f);
            pane(.934f,.180f, 1f,.176f, 1f,.329f, .934f,.332f);
            pane(.637f,.352f, .889f,.345f, .889f,.420f, .637f,.420f);
            pane(.934f,.345f, 1f,.341f, 1f,.440f, .934f,.440f);
        }
        if(lampOffDay!=null){lampOffDayMatrix.setScale(day.getWidth()*scale/lampOffDay.getWidth(),day.getHeight()*scale/lampOffDay.getHeight());lampOffDayMatrix.postTranslate(offsetX,offsetY);}
        if(lampOffNight!=null){lampOffNightMatrix.setScale(day.getWidth()*scale/lampOffNight.getWidth(),day.getHeight()*scale/lampOffNight.getHeight());lampOffNightMatrix.postTranslate(offsetX,offsetY);}
        if(studentRoom&&outsideNight!=null){
            float[] b=RoomScene.SKY;outsideNightMatrix.setScale(day.getWidth()*(b[2]-b[0])*scale/outsideNight.getWidth(),day.getHeight()*(b[3]-b[1])*scale/outsideNight.getHeight());outsideNightMatrix.postTranslate(offsetX+day.getWidth()*scale*b[0],offsetY+day.getHeight()*scale*b[1]);
        }
        glass.transform(studentRoom?roomMaskMatrix:sceneMatrix);
        glass.computeBounds(glassBounds, true);
        if (getWidth() > 0 && getHeight() > 0) {
            if(studentRoom){float[] b=RoomScene.SKY;textureMatrix.setScale(day.getWidth()*scale*(b[2]-b[0])/getWidth(),day.getHeight()*scale*(b[3]-b[1])/getHeight());textureMatrix.postTranslate(offsetX+day.getWidth()*scale*b[0],offsetY+day.getHeight()*scale*b[1]);}
            else {textureMatrix.setScale(day.getWidth() * scale / getWidth(), day.getHeight() * scale / getHeight());textureMatrix.postTranslate(offsetX, offsetY);}
            texture.setTransform(textureMatrix);
        }
        lampX = offsetX + day.getWidth() * scale * .045f;
        lampY = offsetY + day.getHeight() * scale * (wide ? .51f : .47f);
        lampRadius = day.getWidth() * scale * (wide ? .20f : .28f);
        if(!studentRoom)glowPaint.setShader(new RadialGradient(lampX, lampY, Math.max(1,lampRadius),
                new int[]{0x90ffc572,0x28ffb65c,0x00ffb65c}, null, Shader.TileMode.CLAMP));
        videoClip.invalidate(); roomLight.invalidate(); atmosphere.invalidate();
    }
    private void pane(float x1,float y1,float x2,float y2,float x3,float y3,float x4,float y4) {
        float w = day.getWidth(), h = day.getHeight();
        glass.moveTo(x1*w,y1*h); glass.lineTo(x2*w,y2*h);
        glass.lineTo(x3*w,y3*h); glass.lineTo(x4*w,y4*h); glass.close();
    }

    private void startVideoIfReady() {
        if (!canRunMotion() || studentRoom&&reducedEffects || decoderFailed || player != null || day == null || day.isRecycled()
                || !texture.isAvailable() || getWidth() == 0 || getHeight() == 0) return;
        firstFrame = false; renderingStarted = false; texture.setAlpha(0);
        videoNight = targetDaylight < .5f;
        MediaPlayer candidate = new MediaPlayer();
        player = candidate;
        try {
            surface = new Surface(texture.getSurfaceTexture());
            candidate.setSurface(surface);
            candidate.setVolume(0,0);
            candidate.setLooping(true);
            candidate.setOnPreparedListener(p -> {
                if (p != player || !canRunMotion()) return;
                try { p.start(); } catch (IllegalStateException failure) { videoFailed(p); }
            });
            candidate.setOnInfoListener((p, what, extra) -> {
                if (p == player && canRunMotion() && what == MediaPlayer.MEDIA_INFO_VIDEO_RENDERING_START) {
                    renderingStarted = true; updateVideoAlpha();
                }
                return false;
            });
            candidate.setOnErrorListener((p, what, extra) -> { videoFailure="media:"+what+":"+extra; videoFailed(p); return true; });
            String asset = studentRoom?RoomScene.ROOT+(videoNight?"window_night.mp4":"window_day.mp4"):videoNight
                    ? (wide ? "scenes/study_night_landscape.mp4" : "scenes/study_night_portrait.mp4")
                    : (wide ? "scenes/study_landscape.mp4" : "scenes/study_portrait.mp4");
            try (AssetFileDescriptor fd = getContext().getAssets().openFd(asset)) {
                candidate.setDataSource(fd.getFileDescriptor(),fd.getStartOffset(),fd.getLength());
            }
            candidate.prepareAsync();
        } catch (Exception failure) { videoFailure=failure.getClass().getSimpleName()+":"+failure.getMessage(); videoFailed(candidate); }
    }
    private void videoFailed(MediaPlayer failed) {
        if (failed != player) return;
        decoderFailed = true;
        stopVideo();
        atmosphere.invalidate();
    }
    private void stopVideo() {
        firstFrame = false; renderingStarted = false; texture.setAlpha(0);
        MediaPlayer old = player; player = null;
        if (old != null) {
            old.setOnPreparedListener(null); old.setOnInfoListener(null); old.setOnErrorListener(null);
            try { old.release(); } catch (RuntimeException ignored) { }
        }
        if (surface != null) { surface.release(); surface = null; }
    }
    private final class GlassFrame extends FrameLayout {
        GlassFrame(Context context) { super(context); setWillNotDraw(false); }
        @Override protected void dispatchDraw(Canvas canvas) {
            int save = canvas.save(); canvas.clipPath(glass); super.dispatchDraw(canvas); canvas.restoreToCount(save);
        }
    }
    /** Full-room light plate is below video so opaque night artwork never hides playback. */
    private final class RoomLightView extends View {
        private final Paint roomPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
        private final Paint nightBlend=new Paint(),warmMask=new Paint(Paint.ANTI_ALIAS_FLAG);
        RoomLightView(Context context) {
            super(context);setClickable(false);
            warmMask.setShader(new RadialGradient(0,0,1,new int[]{0xffffffff,0xffffffff,0x00ffffff},new float[]{0,.55f,1},Shader.TileMode.CLAMP));
            warmMask.setXfermode(new android.graphics.PorterDuffXfermode(android.graphics.PorterDuff.Mode.DST_OUT));
        }
        private void keepWarm(Canvas canvas,float x,float y,float rx,float ry,int strength){
            warmMask.setAlpha(strength);int save=canvas.save();canvas.translate(offsetX+day.getWidth()*scale*x,offsetY+day.getHeight()*scale*y);canvas.scale(day.getWidth()*scale*rx,day.getHeight()*scale*ry);canvas.drawCircle(0,0,1,warmMask);canvas.restoreToCount(save);
        }
        @Override protected void onDraw(Canvas canvas) {
            if (day == null || day.isRecycled()) return;
            int windowSave=canvas.save();if(studentRoom)canvas.clipPath(glass);
            Bitmap moon=studentRoom&&outsideNight!=null?outsideNight:night;
            if (moon != null && !moon.isRecycled() && daylight < 1) {
                roomPaint.setAlpha(Math.round(255*(1-daylight)));
                canvas.drawBitmap(moon,studentRoom&&outsideNight!=null?outsideNightMatrix:nightMatrix,roomPaint);
            } else if (night == null && daylight < 1) {
                roomPaint.setColor(0xff101b37); roomPaint.setAlpha(Math.round(160*(1-daylight)));
                canvas.drawRect(0,0,getWidth(),getHeight(),roomPaint);
            }
            if(lampEnabled&&daylight<1&&lampOffNight!=null&&!lampOffNight.isRecycled()){
                // Preserve moonlight and leaf shadows on the room, with warm light
                // confined to the bulb and desk. Both registered source plates stay intact.
                int save=canvas.save();canvas.clipPath(glass,android.graphics.Region.Op.DIFFERENCE);
                nightBlend.setAlpha(Math.round(255*(1-daylight)));
                int layer=canvas.saveLayer(0,0,getWidth(),getHeight(),nightBlend);
                roomPaint.setAlpha(255);canvas.drawBitmap(lampOffNight,lampOffNightMatrix,roomPaint);
                keepWarm(canvas,.04f,wide?.51f:.477f,wide?.12f:.20f,wide?.13f:.09f,255);
                keepWarm(canvas,.22f,.88f,wide?.48f:.70f,wide?.38f:.27f,185);
                canvas.restoreToCount(layer);canvas.restoreToCount(save);
            }
            if(!lampEnabled&&lampOffDay!=null&&!lampOffDay.isRecycled()){
                // Replace indoor illumination, including baked highlights and cast shadows.
                // Retain the exact window pixels and video registration in every lamp state.
                int save=canvas.save();canvas.clipPath(glass,android.graphics.Region.Op.DIFFERENCE);
                roomPaint.setAlpha(255);canvas.drawBitmap(lampOffDay,lampOffDayMatrix,roomPaint);
                if(lampOffNight!=null&&!lampOffNight.isRecycled()&&daylight<1){roomPaint.setAlpha(Math.round(255*(1-daylight)));canvas.drawBitmap(lampOffNight,lampOffNightMatrix,roomPaint);}
                canvas.restoreToCount(save);
            }
            if (cloudiness > .01f && daylight > .01f) {
                roomPaint.setColor(0xff233345);
                roomPaint.setAlpha(Math.round(70*cloudiness*daylight));
                canvas.drawRect(0,0,getWidth(),getHeight(),roomPaint);
            }
            roomPaint.setAlpha(255);
            canvas.restoreToCount(windowSave);
        }
    }
    private final class Atmosphere extends View {
        Atmosphere(Context context) { super(context); setClickable(false); }
        @Override protected void onDraw(Canvas canvas) {
            if (day == null || day.isRecycled() || glassBounds.isEmpty()) return;
            frameCount++;
            paint.setShader(null); paint.setStyle(Paint.Style.FILL);
            int save = canvas.save(); canvas.clipPath(glass);
            float t = motionEnabled ? (SystemClock.uptimeMillis()-motionOrigin)/1000f : 0;
            float w = glassBounds.width(), h = glassBounds.height();
            float cloud = cloudiness;
            paint.setColor(daylight > .3f ? 0xff84969e : 0xff15243f);
            paint.setAlpha(Math.round(cloud*(daylight > .3f ? 110 : 115)));
            canvas.drawRect(glassBounds,paint);
            // Broad translucent cloud banks drift slowly across only the glass.
            paint.setColor(daylight > .3f ? 0xffedf1ee : 0xff77859d);
            paint.setAlpha(Math.round(cloud*(daylight > .3f ? 34 : 21)));
            for (int i=0;i<5;i++) {
                float x=glassBounds.left+(((i*.29f+t*.009f)%1.5f)-.25f)*w;
                float y=glassBounds.top+h*(.08f+i*.13f);
                canvas.drawOval(x-w*.24f,y,x+w*.30f,y+h*.12f,paint);
            }
            // Stars are part of the night artwork; do not paint them over tree silhouettes.
            for (int precipitation=1;precipitation<=2;precipitation++) {
                float amount = precipitation == 1 ? rain : snow;
                if (amount < .001f) continue;
                paint.setColor(precipitation == 2 ? 0xfff4f7ff : 0xffc4dcee);
                paint.setAlpha(Math.round(amount*(precipitation == 2 ? 185 : 120)));
                paint.setStrokeWidth(Math.max(1,getWidth()/700f));
                for (int i=0;i<(reducedEffects?24:60);i++) {
                    float x=glassBounds.left+fraction(i*.618034f+t*(precipitation==2?.012f:.045f))*w;
                    float y=glassBounds.top+fraction(i*.381966f+t*(precipitation==2?.065f:.7f))*h;
                    if (precipitation==2) canvas.drawCircle(x,y,Math.max(1.3f,w*.003f)*(1+(i%3)*.3f),paint);
                    else canvas.drawLine(x,y,x-w*.008f,y+h*.035f,paint);
                }
            }
            canvas.restoreToCount(save);
            float lampStrength = Math.max(1-daylight,Math.max(0,cloudiness-.6f)*.8f*daylight);
            if (!studentRoom && lampEnabled && lampStrength > .01f && lampRadius > 0) {
                glowPaint.setAlpha(Math.round(lampStrength*150));
                canvas.drawCircle(lampX,lampY,lampRadius,glowPaint);
            }
            paint.setAlpha(255);
        }
    }
    private static float fraction(float value) { return value-(float)Math.floor(value); }
}
