package com.acrcloud.demo;

import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.TextView;

import com.acrcloud.utils.ACRCloudRecognizer;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity implements MediaRecorder.OnInfoListener {

    private TextView mVolume, mResult, tv_time, db;

    private boolean mProcessing = false;
    private boolean initState = false;

    private String path = "";
    private MediaRecorder mediaRecorder;

    private Handler mHandler = new Handler(Looper.getMainLooper()) {

        public void handleMessage(android.os.Message msg) {
            switch (msg.what) {
                case 1:
                    String res = (String) msg.obj;
                    mResult.setText(res);
                    break;
                case 2:
                    String amplitude = (String) msg.obj;
                    db.setText(amplitude);
                default:
                    break;
            }
        };
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        path = Environment.getExternalStorageDirectory().toString()
                + "/acrcloud/model";

        File file = new File(path);
        if(!file.exists()){
            file.mkdirs();
        }

        mResult = (TextView) findViewById(R.id.result);
        db = (TextView) findViewById(R.id.som_db);

        Button recBtn = (Button) findViewById(R.id.rec);
        recBtn.setText(getResources().getString(R.string.rec));

        findViewById(R.id.rec).setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View arg0) {
                rec();
            }
        });
    }

    @Override
    public void onInfo(MediaRecorder mediaRecorder, int i, int i1) {
        new RecThread(mediaRecorder).start();
    }

    class RecThread extends Thread {
        private MediaRecorder mediaRecorder;

        public RecThread(MediaRecorder mediaRecorder) {
            this.mediaRecorder = mediaRecorder;
        }

        public void run() {

            Map<String, Object> config = new HashMap<>();
            config.put("access_key", "d59cecda4f17c6c06d6122261b504c80");
            config.put("access_secret", "6VRQ9LxEjI4Q5tYDAYplymVbHh4WDpfNBoAhhgwx");
            config.put("host", "identify-us-west-2.acrcloud.com");
            config.put("debug", false);
            config.put("timeout", 5);


            int maxAmplitude = mediaRecorder.getMaxAmplitude();
            double amplitudeDd = 20 * Math.log10(Math.abs(maxAmplitude));

            try {
                Message msg = new Message();
                msg.obj = String.valueOf(amplitudeDd);

                msg.what = 2;
                mHandler.sendMessage(msg);
            } catch (Exception e) {
                e.printStackTrace();
            }


            mediaRecorder.reset();
            mediaRecorder.release();

            File file = new File(path + "/test.mp3");
            byte[] buffer = new byte[3 * 1024 * 1024];
            if (!file.exists()) {
                return;
            }
            FileInputStream fin = null;
            int bufferLen = 0;
            try {
                fin = new FileInputStream(file);
                bufferLen = fin.read(buffer, 0, buffer.length);
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                try {
                    if (fin != null) {
                        fin.close();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            System.out.println("bufferLen=" + bufferLen);

            if (bufferLen <= 0)
                return;

            ACRCloudRecognizer re = new ACRCloudRecognizer(config);
            String result = re.recognizeByFileBuffer(buffer, bufferLen, 0);

            try {
                Message msg = new Message();
                msg.obj = result;

                msg.what = 1;
                mHandler.sendMessage(msg);
            } catch (Exception e) {
                e.printStackTrace();
            }


        }


        //File file = new File(path + "/test.mp3");
        //byte[] buffer = new byte[3 * 1024 * 1024];
        //if (!file.exists()) {
        //    return;
        //}
        //FileInputStream fin = null;
        //int bufferLen = 0;
        //try {
        //    fin = new FileInputStream(file);
        //    bufferLen = fin.read(buffer, 0, buffer.length);
        //} catch (Exception e) {
        //    e.printStackTrace();
        //} finally {
        //    try {
        //        if (fin != null) {
        //            fin.close();
        //        }
        //    } catch (IOException e) {
        //        e.printStackTrace();
        //    }
        //}
        //System.out.println("bufferLen=" + bufferLen);

        //if (bufferLen <= 0)
        //    return;

        //String result = re.recognizeByFileBuffer(buffer, bufferLen, 80);
    }

    public void rec() {
        mediaRecorder = new MediaRecorder();
        mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
        mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.DEFAULT);
        mediaRecorder.setAudioEncodingBitRate(48000);
        mediaRecorder.setAudioSamplingRate(16000);
        mediaRecorder.setMaxDuration(3000);
        mediaRecorder.setOutputFile(path + "/test.mp3");
        mediaRecorder.setOnInfoListener(this);


        try {
            mediaRecorder.prepare();
            mediaRecorder.start();
            int maxAmplitude = mediaRecorder.getMaxAmplitude();
            System.out.println(maxAmplitude);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
