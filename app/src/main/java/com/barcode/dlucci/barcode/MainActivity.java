package com.barcode.dlucci.barcode;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.Camera;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.util.SparseArray;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.vision.Frame;
import com.google.android.gms.vision.barcode.Barcode;
import com.google.android.gms.vision.barcode.BarcodeDetector;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class MainActivity extends AppCompatActivity implements ActivityCompat.OnRequestPermissionsResultCallback {

    private static final String TAG = "MainActivity";


    Camera camera;
    CameraPreview mPreview;

    Camera.PreviewCallback callback = new Camera.PreviewCallback() {
        @Override
        public void onPreviewFrame(byte[] data, Camera camera) {
            Log.d(TAG, "onPreviewFrame: ");
        }
    };

    private final int CAMERA_PERMISSION = 7;
    private final int STORAGE_PERMISSION = 8;

    private final String[] PERMISSIONS = {Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE};
    private final int[] PERMISSION_VAL = {CAMERA_PERMISSION, STORAGE_PERMISSION};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data){
        if(requestCode == 1 && resultCode == RESULT_OK) {
            Bundle extras = data.getExtras();
            Bitmap bitmap = (Bitmap) extras.get("data");
            readData(bitmap);
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        if(!hasCamera()) {
            Toast.makeText(this, "Camera ain't here, yo!", Toast.LENGTH_LONG).show();
        } else {
            if(!checkPermissions(PERMISSIONS)){
                ActivityCompat.requestPermissions(this, PERMISSIONS, 1);
            } else {
                setUpCamera();
            }
        }
    }

    public void openExternalCamera() {
        Intent i = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if(i.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(i, 1);
        }
    }

    public void setUpCamera() {
        camera = getCamera();
        camera.setDisplayOrientation(90);
        mPreview = new CameraPreview(this, camera);
        camera.setOneShotPreviewCallback(callback);

    }

    public boolean checkPermissions(String... permissions) {
        for(String check : permissions){
            if(ActivityCompat.checkSelfPermission(this, check) != PackageManager.PERMISSION_GRANTED)
                return false;
        }

        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults){
        switch (requestCode) {
            case 1 : {
                if(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    openExternalCamera();
                }
                return;
            }
        }
    }

    @Override
    public void onPause(){
        super.onPause();
        if(camera != null) {
            camera.release();
            camera = null;
        }
    }

    @OnClick({R.id.button})
    public void onClick(View v){
        //openExternalCamera();
        camera.takePicture(null, null, mPicture);
    }

    public boolean hasCamera(){
        return getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA);
    }

    public Camera getCamera(){
        return Camera.open();
    }

    File getOutPutMediaFile() {
        File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "Barcode");

        if(!mediaStorageDir.exists() && !mediaStorageDir.mkdirs()) {
            Log.d(TAG, "getOutPutMediaFile: Does not exist!");
        }

        String timeStamp = new SimpleDateFormat("yyyMMdd_HHmmss").format(new Date());
        File mediaFile = new File(mediaStorageDir.getPath() + File.separator + "IMG_" + timeStamp + ".jpg");

        return mediaFile;
    }

    public void readData(Bitmap bitmap) {


        BarcodeDetector detector = new BarcodeDetector.Builder(this)
                .setBarcodeFormats(Barcode.DATA_MATRIX | Barcode.QR_CODE)
                .build();

        if(!detector.isOperational()){
            return;
        }

        Frame frame = new Frame.Builder().setBitmap(bitmap).build();
        SparseArray<Barcode> barcodeSparseArray = detector.detect(frame);

        Barcode barcode = barcodeSparseArray.valueAt(0);
//        Intent i = new Intent(Intent.ACTION_VIEW);
//        i.setData(Uri.parse(barcode.url.url));
//        startActivity(i);
    }

    Camera.PictureCallback mPicture = new Camera.PictureCallback() {
        @Override
        public void onPictureTaken(byte[] data, Camera camera) {
            File picture = getOutPutMediaFile();

            if(picture == null){
                Log.e(TAG, "onPictureTaken: File is null");
                return;
            }

            try {
                FileOutputStream fos = new FileOutputStream(picture);
                fos.write(data);
            } catch (IOException e) {
                Log.e(TAG, "onPictureTaken:  issue reading file");
            }
        }
    };
}
