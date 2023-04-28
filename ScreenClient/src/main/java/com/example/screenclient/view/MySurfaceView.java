package com.example.screenclient.view;

import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

/**
 * @Author hzh
 * E-Mail Address：565150953@qq.com
 * @Date 17:22
 * @Description
 */
public class MySurfaceView extends SurfaceView implements SurfaceHolder.Callback {
    private SurfaceHolder surfaceHolder;
    private RenderThread renderThread;
    private boolean isRender = false;
    private long lastRenderTime = 0;

    public MySurfaceView(Context context) {
        super(context);
        init();
    }

    public MySurfaceView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public MySurfaceView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    private void init() {
        surfaceHolder = getHolder();
        surfaceHolder.addCallback(this);
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        renderThread = new RenderThread(holder);
        renderThread.start();
        isRender = true;
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        // do nothing
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        isRender = false;
        renderThread.interrupt();
    }

    private class RenderThread extends Thread {
        private SurfaceHolder surfaceHolder;

        public RenderThread(SurfaceHolder surfaceHolder) {
            this.surfaceHolder = surfaceHolder;
        }

        @Override
        public void run() {
            while (isRender) {
                long startTime = System.currentTimeMillis();

                Canvas canvas = surfaceHolder.lockCanvas();
                if (canvas != null) {
                    // 渲染代码
                    surfaceHolder.unlockCanvasAndPost(canvas);
                }

                long endTime = System.currentTimeMillis();
                long deltaTime = endTime - startTime;

                lastRenderTime = deltaTime;
            }
        }
    }

    public long getLastRenderTime() {
        return lastRenderTime;
    }
}

