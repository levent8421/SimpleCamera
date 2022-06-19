package com.levent8421.camera.util;


import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

/**
 * Create by levent8421 2020/10/16 0:12
 * Toast Utils
 *
 * @author levent8421
 */
public class ToastUtils {
    private static final Handler HANDLER = new Handler(Looper.getMainLooper());

    public static void showToastInMainLooper(final Context context, final String msg) {
        HANDLER.post(new Runnable() {
            @Override
            public void run() {
                showToast(context, msg);
            }
        });
    }

    public static void showToast(Context context, String msg) {
        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show();
    }
}
