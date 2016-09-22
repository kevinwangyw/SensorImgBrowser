package com.kevinwang.sensorimgbrowser;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.ImageView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Random;

public class ImageActivity extends AppCompatActivity implements SensorEventListener{
    public static final String TAG = "ImageActivity";
    ArrayList<String> fileInFolder;
    private static int imgPosInList;
    private ImageView mSensor_img;
    private SensorManager mSensorManager;
    private Sensor magneticSensor;
    private Sensor accelerSensor;

    private float[] accelerometerValues = new float[3];
    private float[] magneticFieldValues = new float[3];


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image);

        imgPosInList = 0;
        Intent intent = getIntent();
        fileInFolder = intent.getStringArrayListExtra("images");

        mSensorManager = (SensorManager)getSystemService(Context.SENSOR_SERVICE);
        magneticSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        accelerSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

        mSensor_img = (ImageView)findViewById(R.id.sensor_img);
        setBitmap();
    }

    @Override
    protected void onResume() {
        if (magneticSensor == null || accelerSensor == null) {
            Toast.makeText(this, "所需传感器不存在", Toast.LENGTH_SHORT);
            return;
        }
        Log.i(TAG, "onResume--->registerListener");
        mSensorManager.registerListener(this, accelerSensor, SensorManager.SENSOR_DELAY_NORMAL);
        super.onResume();
    }

    @Override
    protected void onPause() {
        if (magneticSensor != null && accelerSensor != null) {
            mSensorManager.unregisterListener(this);
        }
        super.onPause();
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        if (sensorEvent.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            accelerometerValues = sensorEvent.values;
            if ((Math.abs(accelerometerValues[0]) > 17 || Math.abs(accelerometerValues[1]) > 17 || Math
                    .abs(accelerometerValues[2]) > 17)) {
                Log.i("摇一摇", "更改图片");
                Random random = new Random();
                if (++imgPosInList == fileInFolder.size()) {
                    imgPosInList = 0;
                }
                setBitmap();
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }

    // 计算方向
    private void calculateOrientation() {
        float[] values = new float[3];
        float[] R = new float[9];
        SensorManager.getRotationMatrix(R, null, accelerometerValues,
                magneticFieldValues);
        SensorManager.getOrientation(R, values);
        values[0] = (float) Math.toDegrees(values[0]);

        Log.i(TAG, values[0] + "");
    }

    private void setBitmap() {
        Bitmap bmp = BitmapFactory.decodeFile(fileInFolder.get(imgPosInList));
        mSensor_img.setImageBitmap(bmp);
    }
}
