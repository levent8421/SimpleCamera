package com.levent8421.camera.hardware;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.ImageFormat;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.util.Size;
import android.view.Surface;

import androidx.annotation.NonNull;

import com.levent8421.camera.log.LogView;

import org.apache.commons.lang3.time.FastDateFormat;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Create by levent8421 2020/10/16 0:02
 * Camera device
 *
 * @author levent8421
 */
public class CameraWrapper extends CameraDevice.StateCallback implements ImageReader.OnImageAvailableListener {
    private static final FastDateFormat DATE_FORMAT = FastDateFormat.getInstance("yyyyMMddHHmmssSSS");

    private final Context context;
    private final String cameraId;
    private final CameraManager cameraManager;
    private final LogView logView;
    private CameraDevice cameraDevice;
    private ImageReader imageReader;
    private final Surface previewSurface;
    private CameraCaptureSession cameraCaptureSession;
    private Size cameraSize = new Size(2736, 3648);
    private boolean ready = false;

    public CameraWrapper(Context context, String cameraId, LogView logView, Surface surface) {
        this.context = context;
        this.cameraId = cameraId;
        this.cameraManager = Cameras.getCameraManager(context);
        this.logView = logView;
        previewSurface = surface;
    }

    private ImageReader createImageReader() {
        final ImageReader imageReader = ImageReader.newInstance(
                cameraSize.getWidth(), cameraSize.getHeight(),
                ImageFormat.JPEG, 2);
        imageReader.setOnImageAvailableListener(this, null);
        log("Create imageReader!");
        return imageReader;
    }

    public void init() throws CameraAccessException {
        if (context.checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        final CameraCharacteristics characteristics = cameraManager.getCameraCharacteristics(cameraId);
        final StreamConfigurationMap configurationMap = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
        if (configurationMap != null) {
            final Size[] sizes = configurationMap.getOutputSizes(ImageFormat.JPEG);
            final Size size = getMaxSize(sizes);
            if (size != null) {
                cameraSize = size;
                log(String.format("Camera max size: w=%s, h=%s", size.getWidth(), size.getHeight()));
            }
        }
        if (imageReader == null) {
            imageReader = createImageReader();
        }
        cameraManager.openCamera(cameraId, this, null);
    }

    private Size getMaxSize(Size[] sizes) {
        if (sizes == null || sizes.length <= 0) {
            return null;
        }
        int max = 0;
        Size maxSize = null;
        for (Size size : sizes) {
            final int sizeValue = size.getWidth() * size.getHeight();
            if (sizeValue > max) {
                max = sizeValue;
                maxSize = size;
            }
        }
        return maxSize;
    }

    public void capture() throws CameraAccessException {
        final CaptureRequest.Builder builder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
        builder.addTarget(imageReader.getSurface());
        cameraCaptureSession.capture(builder.build(), null, null);
    }

    public void close() throws CameraAccessException {
        cameraCaptureSession.stopRepeating();
        cameraCaptureSession.close();
        cameraDevice.close();
        imageReader.close();
        ready = false;
    }

    public boolean isReady() {
        return ready;
    }

    private void initCapture() {
        final List<Surface> surfaces = new ArrayList<>();
        surfaces.add(previewSurface);
        surfaces.add(imageReader.getSurface());
        try {
            cameraDevice.createCaptureSession(surfaces, new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(@NonNull CameraCaptureSession session) {
                    cameraCaptureSession = session;
                    startPreview();
                }

                @Override
                public void onConfigureFailed(@NonNull CameraCaptureSession session) {
                    log("Error on Configure capture session");
                }
            }, null);
        } catch (CameraAccessException e) {
            log("Error on create capture session: " + e.getMessage());
        }
    }

    private void startPreview() {
        try {
            final CaptureRequest.Builder requestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            requestBuilder.addTarget(previewSurface);
            final CaptureRequest request = requestBuilder.build();
            cameraCaptureSession.setRepeatingRequest(request, null, null);
            ready = true;
        } catch (CameraAccessException e) {
            e.printStackTrace();
            log("Error on request preview: " + e.getMessage());
        }
    }

    @Override
    public void onOpened(@NonNull CameraDevice camera) {
        log("Camera open success!");
        this.cameraDevice = camera;
        this.initCapture();
    }

    @Override
    public void onDisconnected(@NonNull CameraDevice camera) {
        log("Camera disconnected");
    }

    @Override
    public void onError(@NonNull CameraDevice camera, int error) {
        log("Camera error: " + error);
    }

    private void log(String msg) {
        if (logView == null) {
            return;
        }
        logView.debug(msg);
    }

    @Override
    public void onImageAvailable(ImageReader reader) {
        log("Capture success!");
        final Image image = reader.acquireLatestImage();
        final Image.Plane[] planes = image.getPlanes();
        saveImage(planes[0]);
        image.close();
    }

    private void saveImage(Image.Plane plane) {
        final ByteBuffer buffer = plane.getBuffer();
        final int size = buffer.remaining();
        final byte[] bytes = new byte[size];
        buffer.get(bytes);
        final File root = context.getExternalFilesDir(null);
        final File dir = new File(root, "image");
        if (!dir.isDirectory()) {
            if (!dir.mkdirs()) {
                log("Error on create Dir to save file!");
                return;
            }
        }

        final File file = new File(dir, nextFilename());
        try (final FileOutputStream fos = new FileOutputStream(file)) {
            fos.write(bytes);
            fos.flush();
        } catch (IOException e) {
            e.printStackTrace();
            log("Error on save file:" + e.getMessage());
        }
        log("File saved: " + file.getAbsolutePath());
    }

    private String nextFilename() {
        return String.format("%s.jpg", DATE_FORMAT.format(new Date()));
    }
}

