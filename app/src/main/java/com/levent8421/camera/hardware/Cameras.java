package com.levent8421.camera.hardware;

import android.content.Context;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraManager;

import java.util.Arrays;
import java.util.List;

/**
 * Create by levent8421 2020/10/16 0:02
 * Camera helper
 *
 * @author levent8421
 */
public class Cameras {
    public static CameraManager getCameraManager(Context context) {
        return (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);
    }

    /**
     * 获取相机ID列表
     *
     * @param context context
     * @return camera id list
     * @throws CameraAccessException any exception
     */
    public static List<String> getCameras(Context context) throws CameraAccessException {
        final CameraManager cameraManager = getCameraManager(context);
        final String[] cameras = cameraManager.getCameraIdList();
        return Arrays.asList(cameras);
    }
}
