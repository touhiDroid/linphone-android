package org.linphone.views;

/*
LinphoneOverlay.java
Copyright (C) 2017  Belledonne Communications, Grenoble, France

This program is free software; you can redistribute it and/or
modify it under the terms of the GNU General Public License
as published by the Free Software Foundation; either version 2
of the License, or (at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program; if not, write to the Free Software
Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
*/

import android.content.Context;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.os.Build;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import org.linphone.LinphoneActivity;
import org.linphone.LinphoneManager;
import org.linphone.LinphoneService;
import org.linphone.core.Call;
import org.linphone.core.CallParams;
import org.linphone.mediastream.Version;
import org.linphone.mediastream.video.AndroidVideoWindowImpl;

public class LinphoneOverlay extends org.linphone.mediastream.video.display.GL2JNIView {
    private WindowManager mWindowManager;
    private WindowManager.LayoutParams mParams;
    private DisplayMetrics mMetrics;
    private float mX, mY, mTouchX, mTouchY;
    private boolean mDragEnabled;
    private AndroidVideoWindowImpl mAndroidVideoWindowImpl;

    public LinphoneOverlay(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs);
        mWindowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);

        int LAYOUT_FLAG;
        if (Build.VERSION.SDK_INT >= Version.API26_O_80) {
            LAYOUT_FLAG = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        } else {
            LAYOUT_FLAG = WindowManager.LayoutParams.TYPE_PHONE;
        }

        mParams =
                new WindowManager.LayoutParams(
                        WindowManager.LayoutParams.WRAP_CONTENT,
                        WindowManager.LayoutParams.WRAP_CONTENT,
                        LAYOUT_FLAG,
                        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                        PixelFormat.TRANSLUCENT);
        mParams.gravity = Gravity.TOP | Gravity.LEFT;
        mMetrics = new DisplayMetrics();
        mWindowManager.getDefaultDisplay().getMetrics(mMetrics);

        mAndroidVideoWindowImpl =
                new AndroidVideoWindowImpl(
                        this,
                        null,
                        new AndroidVideoWindowImpl.VideoWindowListener() {
                            public void onVideoRenderingSurfaceReady(
                                    AndroidVideoWindowImpl vw, SurfaceView surface) {
                                LinphoneManager.getLc().setNativeVideoWindowId(vw);
                            }

                            public void onVideoRenderingSurfaceDestroyed(
                                    AndroidVideoWindowImpl vw) {}

                            public void onVideoPreviewSurfaceReady(
                                    AndroidVideoWindowImpl vw, SurfaceView surface) {}

                            public void onVideoPreviewSurfaceDestroyed(AndroidVideoWindowImpl vw) {}
                        });

        Call call = LinphoneManager.getLc().getCurrentCall();
        CallParams callParams = call.getCurrentParams();
        mParams.width = callParams.getReceivedVideoDefinition().getWidth();
        mParams.height = callParams.getReceivedVideoDefinition().getHeight();
        LinphoneManager.getLc().setNativeVideoWindowId(mAndroidVideoWindowImpl);

        setOnClickListener(
                new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Context context = LinphoneService.instance();
                        Intent intent =
                                new Intent(context, LinphoneActivity.class)
                                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        context.startActivity(intent);
                    }
                });
        setOnLongClickListener(
                new OnLongClickListener() {
                    @Override
                    public boolean onLongClick(View v) {
                        mDragEnabled = true;
                        return true;
                    }
                });
    }

    public LinphoneOverlay(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public LinphoneOverlay(Context context) {
        this(context, null);
    }

    public void destroy() {
        mAndroidVideoWindowImpl.release();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        mX = event.getRawX();
        mY = event.getRawY();
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                mTouchX = event.getX();
                mTouchY = event.getY();
                break;
            case MotionEvent.ACTION_MOVE:
                if (mDragEnabled) {
                    updateViewPostion();
                }
                break;
            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_UP:
                mTouchX = mTouchY = 0;
                mDragEnabled = false;
                break;
            default:
                break;
        }
        return super.onTouchEvent(event);
    }

    private void updateViewPostion() {
        mParams.x =
                Math.min(
                        Math.max(0, (int) (mX - mTouchX)),
                        mMetrics.widthPixels - getMeasuredWidth());
        mParams.y =
                Math.min(
                        Math.max(0, (int) (mY - mTouchY)),
                        mMetrics.heightPixels - getMeasuredHeight());
        mWindowManager.updateViewLayout(this, mParams);
    }

    public WindowManager.LayoutParams getWindowManagerLayoutParams() {
        return mParams;
    }
}
