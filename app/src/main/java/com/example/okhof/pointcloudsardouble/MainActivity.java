/*
 * Copyright 2017 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.okhof.pointcloudsardouble;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.AssetManager;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.PowerManager;
import android.support.design.widget.BaseTransientBottomBar;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.ar.core.Anchor;
import com.google.ar.core.ArCoreApk;
import com.google.ar.core.Camera;
import com.google.ar.core.Config;
import com.google.ar.core.Frame;
import com.google.ar.core.Pose;
import com.google.ar.core.Session;
import com.google.ar.core.TrackingState;
import com.example.okhof.pointcloudsardouble.rendering.BackgroundRenderer;
import com.example.okhof.pointcloudsardouble.rendering.PointCloudReader;
import com.example.okhof.pointcloudsardouble.rendering.PointClass;
import com.example.okhof.pointcloudsardouble.rendering.PointCloudRenderer;
import com.google.ar.core.exceptions.UnavailableApkTooOldException;
import com.google.ar.core.exceptions.UnavailableArcoreNotInstalledException;
import com.google.ar.core.exceptions.UnavailableSdkTooOldException;
import com.google.ar.core.exceptions.UnavailableUserDeclinedInstallationException;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;

/**
 * created by Patrick Ley using ARCore functionalities
 * it is designed to display colored point clouds and
 * can be used in experiments for subjective quality
 * assessments of point clouds.
 *
 * This version is for single point cloud evaluation.
 * For toggling see other version: [LINK]
 *
 * For more information see: [LINK]
 *
 */
public class MainActivity extends AppCompatActivity implements GLSurfaceView.Renderer {
    private static final String TAG = com.example.okhof.pointcloudsardouble.MainActivity.class.getSimpleName();

    // The Renderer is created here, and initialized when the GL surface is created.
    private GLSurfaceView surfaceView;

    private boolean installRequested;

    private Session session;
    private Snackbar messageSnackbar;
    private com.google.ar.core.examples.java.helloar.DisplayRotationHelper displayRotationHelper;

    private final BackgroundRenderer backgroundRenderer = new BackgroundRenderer();
    private final PointCloudRenderer pointCloud = new PointCloudRenderer();
    private final PointClass pointClass = new PointClass();
    private PointCloudReader reader = new PointCloudReader();
    private List<PointCloudReader> readers = new ArrayList<PointCloudReader>();
    TextView textView;
    private List<Float> data_loc = new ArrayList<Float>();

    TextView titleStart;

    // -
    private final float SWITCH = 1995;
    // -double the number because two per pair-
    private final int NUMBERBATCHELEMENTS = 108;
    private final int ALLELEMENTS = 57;
    // -includes training-
    private final int TRAININGNUMBER = 3;

    Button newPointCloud;
    Button startButton;
    Button forwardBtn;
    Button backwardsBtn;
    Button batchButton;

    private TextView progressValue;
    private TextView maxValue;
    private TextView evaluationTitle;
    private TextView thankyouText;
    private TextView batchText;
    private TextView scoretext1;
    private TextView scoretext2;
    private TextView scoretext3;
    private TextView scoretext4;
    private TextView scoretext5;

    private EditText batchNumberText;

    private Button scoreBtn;
    private SeekBar seekBar;
    private int score;

    int scores[];
    String namesDocs[];
    String batchList[];

    float startTime;

    // -change size of points with this variable-
    float sizePoints;


    int nbrPC;
    int listsize;
    int count_scores;
    int numElements;
    int trainingElements;
    int batchNumber;

    boolean first_time;
    boolean home_screen;
    boolean switch_on;

    FloatBuffer storedPoints, coloredPoints;

    // Tap handling and UI.
    private final ArrayBlockingQueue<MotionEvent> queuedSingleTaps = new ArrayBlockingQueue<>(16);
    private final ArrayList<Anchor> anchors = new ArrayList<>();

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_home);

        // -stop phone from going into sleep mode during loading-
        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        final PowerManager.WakeLock wl = pm.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK, "My Tag");
        wl.acquire();

        // -Read training text file to load content-
        numElements = ALLELEMENTS;
        trainingElements = TRAININGNUMBER;
        AssetManager am = MainActivity.this.getAssets();
        InputStream is = null;
        try {
            is = am.open("_training.txt");
        } catch (IOException e) {
            e.printStackTrace();
        }
        BufferedReader br = new BufferedReader(new InputStreamReader(is));

        batchList = new String[NUMBERBATCHELEMENTS+trainingElements*2];
        String[] line_String = null;
        String temp = null;

        for (int i = 0; i < trainingElements; i++) {
            try {
                line_String = br.readLine().split(" ");
                batchList[2*i] = line_String[0];
                batchList[2*i+1] = line_String[1];
                Log.d("ListNotification", "BatchList " + Integer.toString(2*(i)) + ": " + line_String[0]);
                Log.d("ListNotification", "BatchList " + Integer.toString(2*(i)+1) + ": " + line_String[1]);

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        try {
            br.close();
            is.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        // -Read text file loading all content-
        AssetManager am_2 = this.getAssets();
        InputStream is_2 = null;
        try {
            is_2 = am_2.open("allDocs.txt");
        } catch (IOException e) {
            e.printStackTrace();
        }
        BufferedReader br_2 = new BufferedReader(new InputStreamReader(is_2));

        namesDocs = new String[numElements];

        for (int i = 0; i < numElements; i++) {
            try {
                namesDocs[i] = br_2.readLine();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        try {
            br_2.close();
            is_2.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        //-Load all contents-

        long startTime;
        long endTime;
        long duration;
        long totDuration=0;
        int j = 0;
        try {
            for(int i = 0; i < numElements; i++){
                startTime = System.nanoTime();
                reader.reads(namesDocs[i], this);
                readers.add(reader);
                endTime = System.nanoTime();
                duration = (endTime - startTime)/1000000000;
                totDuration = totDuration + duration;
                Log.d("ListNotification", "Duration: " + Long.toString(duration));
                Log.d("ListNotification", "Name: " + namesDocs[i]);
                j++;
                Log.d("ListNotification", "Iteration: " + Integer.toString(j));

                reader = new PointCloudReader();

            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        Log.d("ListNotification", "Total Duration: " + Long.toString(totDuration));

        // -Permissions for writing on external storage-
        if(Build.VERSION.SDK_INT>22){
            requestPermissions(new String[] {WRITE_EXTERNAL_STORAGE}, 1);
        }

        // -initialization of variables and buttons/textviews-
        storedPoints = null;
        nbrPC = 0;
        listsize = NUMBERBATCHELEMENTS+trainingElements*2;
        count_scores = 0;
        batchNumber = 0;
        switch_on = false;
        sizePoints = 5.0f;

        scores = new int[listsize];

        surfaceView = findViewById(R.id.surfaceviewhome);
        displayRotationHelper = new com.google.ar.core.examples.java.helloar.DisplayRotationHelper(/*context=*/ this);


        home_screen = true;

        titleStart = findViewById(R.id.textTitle);
        thankyouText = findViewById(R.id.endText);
        batchText = findViewById(R.id.textBatch);
        batchNumberText = findViewById(R.id.editText);
        scoretext1 = findViewById(R.id.text1score);
        scoretext2 = findViewById(R.id.text2score);
        scoretext3 = findViewById(R.id.text3score);
        scoretext4 = findViewById(R.id.text4score);
        scoretext5 = findViewById(R.id.text5score);

        batchButton = findViewById(R.id.btnBatch);
        batchButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startButton.setVisibility(View.VISIBLE);
                batchText.setVisibility(View.GONE);
                batchButton.setVisibility(View.GONE);
                batchNumberText.setVisibility(View.GONE);

                // -Loading of Batch and arranging order of point clouds accordingly-
                batchNumber = Integer.parseInt(batchNumberText.getText().toString());
                AssetManager am = MainActivity.this.getAssets();
                InputStream is = null;
                try {
                    if(batchNumber > 9) is = am.open("batch_" + batchNumber +".txt");
                    else is = am.open("batch_0" + batchNumber +".txt");
                } catch (IOException e) {
                    e.printStackTrace();
                }
                BufferedReader br = new BufferedReader(new InputStreamReader(is));

                String[] line_String = null;
                String temp = null;

                for (int i = 0; i < NUMBERBATCHELEMENTS/2; i++) {
                    try {
                        line_String = br.readLine().split(" ");
                        batchList[2*(i+trainingElements)] = line_String[0];
                        batchList[2*(i+trainingElements)+1] = line_String[1];
                        Log.d("ListNotification", "BatchList " + Integer.toString(2*(i+trainingElements)) + ": " + line_String[0]);
                        Log.d("ListNotification", "BatchList " + Integer.toString(2*(i+trainingElements)+1) + ": " + line_String[1]);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                try {
                    br.close();
                    is.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });

        startButton = findViewById(R.id.buttonStart);
        startButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Toast.makeText(getApplicationContext(), "Pressed", Toast.LENGTH_SHORT).show();
                startButton.setVisibility(View.GONE);
                titleStart.setVisibility(View.GONE);
                setPointCloudViewPage();
                backwardsBtn.setVisibility(View.GONE);
                home_screen = false;
                initilalizePointCloud();
                wl.release();
            }
        });

        newPointCloud = findViewById(R.id.chooseNewbtn);
        newPointCloud.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setEvaluationaPage();
                progressValue.setText(Integer.toString(0));
                seekBar.setProgress(0);
                score = 0;
            }
        });

        //FORWARDS BUTTON
        forwardBtn = findViewById(R.id.forwardBtn);
        forwardBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(nbrPC+1 < listsize && nbrPC%2 == 0) {
                    nbrPC = nbrPC+1;
                    switch_on = true;
                    initilalizePointCloud();
                    forwardBtn.setVisibility(View.GONE);
                    backwardsBtn.setVisibility(View.VISIBLE);
                    newPointCloud.setVisibility(View.VISIBLE);
                }
                Log.d("ListNotification", "NbrPC: " + Integer.toString(nbrPC));
            }
        });

        //BACKWARDS BUTTON
        backwardsBtn = findViewById(R.id.backBtn);
        backwardsBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(nbrPC-1 >= 0 && nbrPC%2 == 1) {
                    nbrPC = nbrPC-1;
                    switch_on = true;
                    initilalizePointCloud();
                    forwardBtn.setVisibility(View.VISIBLE);
                    backwardsBtn.setVisibility(View.GONE);
                    newPointCloud.setVisibility(View.GONE);
                }
                Log.d("ListNotification", "NbrPC: " + Integer.toString(nbrPC));
            }
        });

        score = 0;

        scoreBtn = findViewById(R.id.btnSubmit);
        scoreBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //SAVE THE VALUE HERE
                if(score != 0) {
                    scores[count_scores] = score;
                    count_scores++;
                    //upon completion of the experiment this is executed
                    if (2 * (nbrPC / 2) + 2 == listsize) {
                        seekBar.setVisibility(View.GONE);
                        scoretext1.setVisibility(View.GONE);
                        scoretext2.setVisibility(View.GONE);
                        scoretext3.setVisibility(View.GONE);
                        scoretext4.setVisibility(View.GONE);
                        scoretext5.setVisibility(View.GONE);
                        scoreBtn.setVisibility(View.GONE);
                        progressValue.setVisibility(View.GONE);
                        evaluationTitle.setVisibility(View.GONE);
                        maxValue.setVisibility(View.GONE);
                        thankyouText.setVisibility(View.VISIBLE);
                        writeFileEnd("Scores.txt");
                        writeFileLocation("LocationData" + Integer.toString(count_scores) + ".txt");
                    }
                    //Show next two pointclouds
                    if (nbrPC >= 0 && 2 * (nbrPC / 2) + 2 < listsize) {
                        nbrPC = 2 * (nbrPC / 2) + 2;
                        writeFileLocation("LocationData" + Integer.toString(count_scores) + ".txt");
                        data_loc = new ArrayList<Float>();
                        initilalizePointCloud();
                        setPointCloudViewPage();
                    }
                    Log.d("ListNotification", "NbrPC: " + Integer.toString(nbrPC));
                }
            }
        });

        // -Setup values on Evalutation screen-
        progressValue = findViewById(R.id.progressSeekbar);
        evaluationTitle = findViewById(R.id.textEvaluation);
        maxValue = findViewById(R.id.maxSeekbar);

        // -Setup Seekbar for evaluation
        seekBar = findViewById(R.id.seekBar);
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                progressValue.setText(Integer.toString(progress));
                score = progress;
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        // Set up renderer.
        surfaceView.setPreserveEGLContextOnPause(true);
        surfaceView.setEGLContextClientVersion(2);
        surfaceView.setEGLConfigChooser(8, 8, 8, 8, 16, 0); // Alpha used for plane blending.
        surfaceView.setRenderer(this);
        surfaceView.setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);
    }

    // -sets view content VISIBLE or GONE-
    void setEvaluationaPage(){
        newPointCloud.setVisibility(View.GONE);
        forwardBtn.setVisibility(View.GONE);
        backwardsBtn.setVisibility(View.GONE);
        surfaceView.setVisibility(View.GONE);
        seekBar.setVisibility(View.VISIBLE);
        scoretext1.setVisibility(View.VISIBLE);
        scoretext2.setVisibility(View.VISIBLE);
        scoretext3.setVisibility(View.VISIBLE);
        scoretext4.setVisibility(View.VISIBLE);
        scoretext5.setVisibility(View.VISIBLE);
        scoreBtn.setVisibility(View.VISIBLE);
        progressValue.setVisibility(View.VISIBLE);
        evaluationTitle.setVisibility(View.VISIBLE);
        maxValue.setVisibility(View.VISIBLE);
    }

    void setPointCloudViewPage(){
        forwardBtn.setVisibility(View.VISIBLE);
        backwardsBtn.setVisibility(View.VISIBLE);
        surfaceView.setVisibility(View.VISIBLE);
        scoretext1.setVisibility(View.GONE);
        scoretext2.setVisibility(View.GONE);
        scoretext3.setVisibility(View.GONE);
        scoretext4.setVisibility(View.GONE);
        scoretext5.setVisibility(View.GONE);
        seekBar.setVisibility(View.GONE);
        scoreBtn.setVisibility(View.GONE);
        progressValue.setVisibility(View.GONE);
        evaluationTitle.setVisibility(View.GONE);
        maxValue.setVisibility(View.GONE);
    }

    /* Checks if external storage is available for read and write */
    public boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        return Environment.MEDIA_MOUNTED.equals(state);
    }
    // -writes final scores after experiment is concluded-
    void writeFileEnd(String fileName){
        if(isExternalStorageWritable()) {
            File file;
            FileOutputStream outputStream;
            try {
                file = new File(Environment.getExternalStorageDirectory(), fileName);
                outputStream = new FileOutputStream(file);
                outputStream.write("The Scores: \n".getBytes());
                for(int x: scores){
                    outputStream.write(Integer.toString(x).getBytes());
                    outputStream.write("\n".getBytes());
                }
                outputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }else{
            Toast.makeText(this, "Cannot write to external storage", Toast.LENGTH_LONG).show();
        }
    }

    // -upon finishing of on point cloud, stores location data of viewing experiment for
    //  reconstruction-
    void writeFileLocation(String fileName){
        if(isExternalStorageWritable()) {
            File file;
            FileOutputStream outputStream;
            try {
                int j = 0;
                file = new File(Environment.getExternalStorageDirectory(), fileName);
                outputStream = new FileOutputStream(file);
                outputStream.write("The Location Data: \n".getBytes());
                outputStream.write("Format: location float[3], rotation quaterion float[4] \n".getBytes());
                outputStream.write(("Format: time, " +
                        " x," +
                        " y," +
                        " z," +
                        " x = k.x * sin(theta/2)," +
                        " y = k.y * sin(theta/2)," +
                        " z = k.z * sin(theta/2)," +
                        " w = cos(theta/2) \n").getBytes());
                for(Iterator<Float> iter = data_loc.iterator(); iter.hasNext();){
                    Float nextI = iter.next();
                    if(nextI != SWITCH) {
                        String value = Float.toString(nextI) + ", ";
                        outputStream.write(value.getBytes());
                        j++;
                        if (j % 8 == 0) outputStream.write("\n".getBytes());
                    }else{
                        outputStream.write("------------SWITCH------------\n".getBytes());
                    }
                }
                outputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }else{
            Toast.makeText(this, "Cannot write to external storage", Toast.LENGTH_LONG).show();
        }
    }

    // -finds next Point Clouds out of all saved by name-
    PointCloudReader checkReaderNumber(int nbr){
        PointCloudReader temp_reader = new PointCloudReader();

        String nameFile = batchList[nbr];
        for(PointCloudReader rdr : readers){
            if(rdr.getName().equals(nameFile)){
                temp_reader = rdr;
            }
        }

        return temp_reader;
    }

    // -updates point cloud to be displayed-
    void initilalizePointCloud() {
        first_time = true;
        storedPoints = null;
        int lines;
        float[] array;
        float[] color_array;
        PointCloudReader temp_reader = checkReaderNumber(nbrPC);
        lines = temp_reader.getNumLine();
        storedPoints = FloatBuffer.allocate(lines * 4);
        coloredPoints = FloatBuffer.allocate(lines * 4);
        array = temp_reader.getPoints();
        color_array = temp_reader.getColor();
        storedPoints = storedPoints.put(array);
        storedPoints = (FloatBuffer) storedPoints.position(0);
        coloredPoints = coloredPoints.put(color_array);
        coloredPoints = (FloatBuffer) coloredPoints.position(0);
        startTime = System.nanoTime();

        first_time = false;

    }

    @Override
    protected void onResume() {
        super.onResume();

        if (session == null) {
            Exception exception = null;
            String message = null;
            try {
                switch (ArCoreApk.getInstance().requestInstall(this, !installRequested)) {
                    case INSTALL_REQUESTED:
                        installRequested = true;
                        return;
                    case INSTALLED:
                        break;
                }

                // ARCore requires camera permissions to operate. If we did not yet obtain runtime
                // permission on Android M and above, now is a good time to ask the user for it.
                if (!com.google.ar.core.examples.java.helloar.CameraPermissionHelper.hasCameraPermission(this)) {
                    com.google.ar.core.examples.java.helloar.CameraPermissionHelper.requestCameraPermission(this);
                    return;
                }

                session = new Session(/* context= */ this);
            } catch (UnavailableArcoreNotInstalledException
                    | UnavailableUserDeclinedInstallationException e) {
                message = "Please install ARCore";
                exception = e;
            } catch (UnavailableApkTooOldException e) {
                message = "Please update ARCore";
                exception = e;
            } catch (UnavailableSdkTooOldException e) {
                message = "Please update this app";
                exception = e;
            } catch (Exception e) {
                message = "This device does not support AR";
                exception = e;
            }

            if (message != null) {
                showSnackbarMessage(message, true);
                Log.e(TAG, "Exception creating session", exception);
                return;
            }

            // Create default config and check if supported.
            Config config = new Config(session);
            if (!session.isSupported(config)) {
                showSnackbarMessage("This device does not support AR", true);
            }
            session.configure(config);
        }

        showLoadingMessage();
        // Note that order matters - see the note in onPause(), the reverse applies here.
        session.resume();
        surfaceView.onResume();
        displayRotationHelper.onResume();

    }

    @Override
    public void onPause() {
        super.onPause();
        if (session != null) {
            // Note that the order matters - GLSurfaceView is paused first so that it does not try
            // to query the session. If Session is paused before GLSurfaceView, GLSurfaceView may
            // still call session.update() and get a SessionPausedException.
            displayRotationHelper.onPause();
            surfaceView.onPause();
            session.pause();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] results) {
        if (!com.google.ar.core.examples.java.helloar.CameraPermissionHelper.hasCameraPermission(this)) {
            Toast.makeText(this, "Camera permission is needed to run this application", Toast.LENGTH_LONG)
                    .show();
            if (!com.google.ar.core.examples.java.helloar.CameraPermissionHelper.shouldShowRequestPermissionRationale(this)) {
                // Permission denied with checking "Do not ask again".
                com.google.ar.core.examples.java.helloar.CameraPermissionHelper.launchPermissionSettings(this);
            }
            finish();
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            // Standard Android full-screen functionality.
            getWindow()
                    .getDecorView()
                    .setSystemUiVisibility(
                            View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                                    | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                                    | View.SYSTEM_UI_FLAG_FULLSCREEN
                                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }
    }

    private void onSingleTap(MotionEvent e) {
        // Queue tap if there is space. Tap is lost if queue is full.
        queuedSingleTaps.offer(e);
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        GLES20.glClearColor(0.1f, 0.1f, 0.1f, 1.0f);

        // -Create the texture and pass it to ARCore session to be filled during update().-
        backgroundRenderer.createOnGlThread(/*context=*/ this);

        // -initialize point cloud renderer-
        pointClass.createOnGlThread(/*context=*/ this);

    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        displayRotationHelper.onSurfaceChanged(width, height);
        GLES20.glViewport(0, 0, width, height);
    }

    @Override
    public void onDrawFrame(GL10 gl) {

        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);

        if (home_screen || storedPoints == null) return;
        if (session == null) {
            return;
        }
        // Notify ARCore session that the view size changed so that the perspective matrix and
        // the video background can be properly adjusted.
        displayRotationHelper.updateSessionIfNeeded(session);

        try {
            session.setCameraTextureName(backgroundRenderer.getTextureId());

            // Obtain the current frame from ARSession. When the configuration is set to
            // UpdateMode.BLOCKING (it is by default), this will throttle the rendering to the
            // camera framerate.
            Frame frame = session.update();
            Camera camera = frame.getCamera();

            // -save location data-
            Camera cam = frame.getCamera();
            Pose mypose = cam.getPose();
            float[] position = new float[3];
            float[] rotation = new float[4];
            mypose.getTranslation(position, 0);
            mypose.getRotationQuaternion(rotation, 0);

            if(switch_on == true) data_loc.add(SWITCH);
            float endTime = System.nanoTime();
            data_loc.add((endTime-startTime)/1000000); //in milliseconds
            for(float x: position)data_loc.add(x);
            for(float x: rotation)data_loc.add(x);
            switch_on = false;

            // Draw background.
            backgroundRenderer.draw(frame);

            // If not tracking, don't draw 3d objects.
            if (camera.getTrackingState() == TrackingState.PAUSED) {
                return;
            }

            // Get projection matrix.
            float[] projmtx = new float[16];
            camera.getProjectionMatrix(projmtx, 0, 0.1f, 100.0f);

            // Get camera matrix and draw.
            float[] viewmtx = new float[16];
            camera.getViewMatrix(viewmtx, 0);

            // -draw point clouds-
            pointClass.update(storedPoints, coloredPoints);
            pointClass.draw(viewmtx, projmtx, sizePoints);

        } catch (Throwable t) {
            // Avoid crashing the application due to unhandled exceptions.
            Log.e("§", "Exception on the OpenGL thread", t);
        }

    }

    private void showSnackbarMessage(String message, boolean finishOnDismiss) {
        messageSnackbar =
                Snackbar.make(
                        com.example.okhof.pointcloudsardouble.MainActivity.this.findViewById(android.R.id.content),
                        message,
                        Snackbar.LENGTH_INDEFINITE);
        messageSnackbar.getView().setBackgroundColor(0xbf323232);
        if (finishOnDismiss) {
            messageSnackbar.setAction(
                    "Dismiss",
                    new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            messageSnackbar.dismiss();
                        }
                    });
            messageSnackbar.addCallback(
                    new BaseTransientBottomBar.BaseCallback<Snackbar>() {
                        @Override
                        public void onDismissed(Snackbar transientBottomBar, int event) {
                            super.onDismissed(transientBottomBar, event);
                            finish();
                        }
                    });
        }
        messageSnackbar.show();
    }

    private void showLoadingMessage() {
        runOnUiThread(
                new Runnable() {
                    @Override
                    public void run() {
                        //showSnackbarMessage("Searching for surfaces...", false);
                    }
                });
    }

    private void hideLoadingMessage() {
        runOnUiThread(
                new Runnable() {
                    @Override
                    public void run() {
                        if (messageSnackbar != null) {
                            messageSnackbar.dismiss();
                        }
                        messageSnackbar = null;
                    }
                });
    }
}
