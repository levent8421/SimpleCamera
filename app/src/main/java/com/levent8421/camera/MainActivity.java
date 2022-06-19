package com.levent8421.camera;

import android.Manifest;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.os.Bundle;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.levent8421.camera.hardware.CameraWrapper;
import com.levent8421.camera.hardware.Cameras;
import com.levent8421.camera.log.LogView;
import com.levent8421.camera.util.ToastUtils;

import java.util.List;

/**
 * Create by levent8421 2020/10/15 23:55:00
 * Main Activity
 *
 * @author levent8421
 */
public class MainActivity extends AppCompatActivity implements AdapterView.OnItemSelectedListener, TextureView.SurfaceTextureListener, SeekBar.OnSeekBarChangeListener {
    private Spinner camerasSpinner;
    private Button openCameraButton;
    private LogView logView;
    private TextureView previewTextureView;
    private ImageView maskImage;
    private SeekBar transparentSeekBar;
    private TextView topInfoTextView;
    private List<String> cameras;
    private String selectedCamera;
    private CameraWrapper cameraWrapper;
    private Surface previewSurface;
    private int captureCounter = 0;
    private final View.OnClickListener captureClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (cameraWrapper == null || !cameraWrapper.isReady()) {
                logView.error("Camera not ready!");
                return;
            }
            capture();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        requestPermissions(new String[]{Manifest.permission.CAMERA}, 0);
        findView();
        initView();
        refreshCamera();
    }

    private void findView() {
        camerasSpinner = findViewById(R.id.camerasSpinner);
        openCameraButton = findViewById(R.id.openCameraButton);
        logView = findViewById(R.id.logView);
        previewTextureView = findViewById(R.id.previewTextureView);
        maskImage = findViewById(R.id.maskImage);
        transparentSeekBar = findViewById(R.id.transparentSeekBar);
        topInfoTextView = findViewById(R.id.topInfoTextView);
    }

    private void initView() {
        camerasSpinner.setOnItemSelectedListener(this);
        openCameraButton.setText(R.string.loading);
        openCameraButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openCamera();
            }
        });
        maskImage.setOnClickListener(captureClickListener);
        previewTextureView.setSurfaceTextureListener(this);
        openCameraButton.setEnabled(false);
        transparentSeekBar.setOnSeekBarChangeListener(this);
        setInfo("Loading", R.color.colorAccent);
    }

    private void setInfo(String info, int color) {
        topInfoTextView.setText(info);
        topInfoTextView.setTextColor(color);
    }

    private void refreshCamera() {
        try {
            cameras = Cameras.getCameras(this);
            logView.debug("Get camera success!");
        } catch (CameraAccessException e) {
            e.printStackTrace();
            ToastUtils.showToast(this, "Error on get cameras:" + e.getMessage());
            return;
        }
        camerasSpinner.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, cameras));
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        selectedCamera = cameras.get(position);
        logView.debug("Selected camera: " + selectedCamera);
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {
        selectedCamera = null;
    }

    private void capture() {
        try {
            cameraWrapper.capture();
            captureCounter++;
            setInfo("Capture:" + captureCounter, R.color.colorPrimaryDark);
        } catch (CameraAccessException e) {
            e.printStackTrace();
            logView.error("Error on capture: " + e.getMessage());
        }
    }

    private void openCamera() {
        if (cameraWrapper != null && cameraWrapper.isReady()) {
            try {
                cameraWrapper.close();
            } catch (CameraAccessException e) {
                e.printStackTrace();
                logView.error("Error on close camera: " + e.getMessage());
            }
            openCameraButton.setText(R.string.open);
            camerasSpinner.setEnabled(true);
            logView.info("Camera closed!");
            return;
        }
        if (selectedCamera == null) {
            ToastUtils.showToast(this, "Select camera");
            logView.debug("Require a camera!");
            return;
        }
        cameraWrapper = new CameraWrapper(this, selectedCamera, logView, previewSurface);
        try {
            cameraWrapper.init();
        } catch (CameraAccessException e) {
            e.printStackTrace();
            logView.error("Error on open camera: " + e.getMessage());
        }
        openCameraButton.setText(R.string.close);
        this.camerasSpinner.setEnabled(false);
    }

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
        logView.info("Preview target ready!");
        previewSurface = new Surface(previewTextureView.getSurfaceTexture());
        openCameraButton.setEnabled(true);
        openCameraButton.setText(R.string.open);
        setInfo("Ready", R.color.colorPrimary);
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
        final String msg = String.format("Surface size changed, w=%s,h=%s", width, height);
        logView.debug(msg);
    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        logView.info("Preview Destroyed!");
        return false;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface) {

    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        if (fromUser) {
            final int alpha = 255 - (progress * 255 / 100);
            maskImage.getBackground().setAlpha(alpha);
        }
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {

    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {

    }
}
