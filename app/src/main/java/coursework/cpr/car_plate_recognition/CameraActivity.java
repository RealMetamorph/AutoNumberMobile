package coursework.cpr.car_plate_recognition;

import android.content.Context;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.SurfaceView;
import android.widget.Button;

import org.opencv.android.CameraBridgeViewBase;
import org.opencv.core.Mat;
import org.opencv.core.MatOfRect;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;
import org.opencv.objdetect.Objdetect;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;


public class CameraActivity extends AppCompatActivity implements CameraBridgeViewBase.CvCameraViewListener {

    private int absolutePlateSize;
    private Button btnCapture;
    private CameraBridgeViewBase cameraBridgeViewBase;
    private MatOfRect plates;
    private int currentFrame;
    private int maxFrame = 3;

    //Save to FILE
    private CascadeClassifier cascadeClassifier;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.setContentView(R.layout.activity_camera);
        cameraBridgeViewBase = findViewById(R.id.camera2View);
        cameraBridgeViewBase.setVisibility(SurfaceView.VISIBLE);
        cameraBridgeViewBase.setCvCameraViewListener(this);
        //cameraBridgeViewBase.setRotation(180);
        initializeOpenCVDependencies();
    }

    private void initializeOpenCVDependencies() {

        try {
            absolutePlateSize = 0;
            // Copy the resource into a temp file so OpenCV can load it
            InputStream is = getResources().openRawResource(R.raw.cascade);
            File cascadeDir = getDir("cascade", Context.MODE_PRIVATE);
            File cascadeFile = new File(cascadeDir, "cascade.xml");
            FileOutputStream os = new FileOutputStream(cascadeFile);

            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = is.read(buffer)) != -1) {
                os.write(buffer, 0, bytesRead);
            }
            is.close();
            os.close();
            currentFrame = maxFrame + 1;

            // Load the cascade classifier
            cascadeClassifier = new CascadeClassifier(cascadeFile.getAbsolutePath());

        } catch (Exception e) {
            Log.e("OpenCVActivity", "Error loading cascade", e);
        }

        // Initialise the camera view
        cameraBridgeViewBase.enableView();
    }

    @Override
    protected void onResume() {
        super.onResume();
        initializeOpenCVDependencies();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

    }

    @Override
    public void onCameraViewStarted(int width, int height) {
        int max = width > height ? width : height;
        System.out.println(width);
        System.out.println(height);
        cameraBridgeViewBase.setMaxFrameSize(max, max);
    }

    @Override
    public void onCameraViewStopped() {

    }

    @Override
    public Mat onCameraFrame(Mat inputFrame) {
        if (currentFrame >= maxFrame) {
            plates = new MatOfRect();
            Mat grayFrame = new Mat();
            Imgproc.cvtColor(inputFrame, grayFrame, Imgproc.COLOR_BGR2GRAY);
            Imgproc.equalizeHist(grayFrame, grayFrame);
            if (absolutePlateSize == 0) {
                int height = grayFrame.rows();
                if (Math.round(height * 0.05f) > 0) {
                    absolutePlateSize = Math.round(height * 0.05f);
                }
            }

            cascadeClassifier.detectMultiScale(grayFrame, plates, 1.8, 6, Objdetect.CASCADE_SCALE_IMAGE, new org.opencv.core.Size(absolutePlateSize, absolutePlateSize));
            //Рисуем квадратики,ееей!
            Rect[] platesArray = plates.toArray();
            for (int i = 0; i < platesArray.length; i++)
                Imgproc.rectangle(inputFrame, platesArray[i].tl()/*top-left*/, platesArray[i].br()/*bottom-right*/, new Scalar(229, 40, 64), 3); //new Scalar(229, 40, 64)
            currentFrame = 0;
        } else {
            Rect[] platesArray = plates.toArray();
            for (int i = 0; i < platesArray.length; i++)
                Imgproc.rectangle(inputFrame, platesArray[i].tl()/*top-left*/, platesArray[i].br()/*bottom-right*/, new Scalar(229, 40, 64), 3); //new Scalar(229, 40, 64)
            currentFrame++;
        }
//        Mat something = new Mat(inputFrame.cols(), inputFrame.rows(), inputFrame.type());
//        Core.flip(inputFrame,something, (int) (System.currentTimeMillis()/1000w)%10);
//        //Imgproc.resize(something,something,inputFrame.size());
        //Mat dest = inputFrame.clone();
//        System.out.println(inputFrame.rows());
//        Mat dest = inputFrame.clone();
//        dest.reshape(inputFrame.rows(), inputFrame.cols());
//        System.out.println(dest.rows());
//        Point center = new Point(dest.cols() / 2, dest.rows() / 2);
//        Mat rot = Imgproc.getRotationMatrix2D(center, -90, 1);
//        Imgproc.warpAffine(inputFrame, dest, rot, dest.size());

        //Core.flip(inputFrame.t(), dest, 1);
        return inputFrame;//something;
    }
    //  public native String stringFromJNI();

}