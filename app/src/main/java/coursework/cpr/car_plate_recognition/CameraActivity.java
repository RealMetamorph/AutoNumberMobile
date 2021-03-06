package coursework.cpr.car_plate_recognition;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Toast;

import com.googlecode.leptonica.android.Box;
import com.googlecode.leptonica.android.Clip;
import com.googlecode.leptonica.android.Convert;
import com.googlecode.leptonica.android.GrayQuant;
import com.googlecode.leptonica.android.Pix;
import com.googlecode.leptonica.android.ReadFile;
import com.googlecode.leptonica.android.WriteFile;
import com.googlecode.tesseract.android.TessBaseAPI;

import org.opencv.android.CameraBridgeViewBase;
import org.opencv.core.Mat;
import org.opencv.core.MatOfRect;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;
import org.opencv.objdetect.Objdetect;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

import static coursework.cpr.car_plate_recognition.MainActivity.APP_PREFERENCES;


public class CameraActivity extends AppCompatActivity implements CameraBridgeViewBase.CvCameraViewListener {

    private int absolutePlateSize;
    private CameraBridgeViewBase cameraBridgeViewBase;
    private TessBaseAPI tesseract;
    private MatOfRect plates;
    private Mat lastFrame;
    private Point lastPoint;
    private int currentFrame;
    private int maxFrame = 3;
    private int camHeight;
    private int camWidth;
    private CameraActivity self;
    private File imageDirDebug;
    private long timeout = 1000;
    private long time;
    //settings
    SharedPreferences sPref;

    //Cascade
    private CascadeClassifier cascadeClassifier;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.setContentView(R.layout.activity_camera);
        self = this;
        sPref = getSharedPreferences(APP_PREFERENCES, Context.MODE_PRIVATE);
        imageDirDebug = new File(Environment.getExternalStorageDirectory(), "CarPlateRecognition");
        //noinspection ResultOfMethodCallIgnored
        imageDirDebug.mkdir();
        time = 0;
        cameraBridgeViewBase = findViewById(R.id.camera2View);
        maxFrame = Math.round(Float.parseFloat(loadData(getString(R.string.frequency))));
        cameraBridgeViewBase.setVisibility(SurfaceView.VISIBLE);
        cameraBridgeViewBase.setCvCameraViewListener(this);
        cameraBridgeViewBase.setOnTouchListener(new View.OnTouchListener() {
            @SuppressLint("ClickableViewAccessibility")
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getActionButton() == MotionEvent.ACTION_DOWN) {
                    int viewHeight = v.getHeight();
                    int viewWidth = v.getWidth();
                    System.out.println(viewHeight);
                    System.out.println(viewWidth);
                    System.out.println();
                    double x = converter(event.getY(), 0, viewHeight, 0, camWidth);
                    double y = camHeight - converter(event.getX(), 0, viewWidth, 0, 0.86 * camHeight);
                    Log.d("Touch activate", "X = " + x);
                    Log.d("Touch activate", "Y = " + y);
                    if (plates == null)
                        return false;
                    lastPoint = new Point(x, y);
                    if (time > 0) {
                        time -= System.currentTimeMillis();
                        Toast.makeText(self, "Подождите", Toast.LENGTH_LONG).show();
                        return false;
                    }
                    Rect[] platesArray = plates.toArray();
                    for (Rect rect : platesArray) {
                        if (rect.contains(lastPoint)) {
                            Log.d("Check", "Containing x = " + x + ", y = " + y + ".");
                            Toast.makeText(self, "Распознавание", Toast.LENGTH_LONG).show();
                            Mat findRect = lastFrame.submat(rect);

                            File imageFileDebug = new File(imageDirDebug, "beforeImage.png");
                            if (Boolean.parseBoolean(loadData(getString(R.string.debugMode))))
                                Imgcodecs.imwrite(imageFileDebug.getAbsolutePath(), findRect);

                            File imageDir = getDir("imageToOCR", Context.MODE_PRIVATE);
                            File imageFile = new File(imageDir, "image.png");
                            Imgcodecs.imwrite(imageFile.getAbsolutePath(), findRect);

                            Pix pix = ReadFile.readFile(imageFile);
                            pix = Convert.convertTo8(pix);
                            pix = GrayQuant.pixThresholdToBinary(pix, Integer.parseInt(loadData(getString(R.string.grayThresh))));
                            imageFileDebug = new File(imageDirDebug, "binariesImage.png");
                            if (Boolean.parseBoolean(loadData(getString(R.string.debugMode))))
                                WriteFile.writeImpliedFormat(pix, imageFileDebug);

                            String result = OCR(pix).replaceAll("[/s]*", "");
                            Log.d("Final result: ", result);
                            if (Boolean.parseBoolean(loadData(getString(R.string.key_checkbox)))) {
                                if (!result.isEmpty()) {
                                    Intent intent = new Intent(self, WebActivity.class);
                                    intent.putExtra("href", "https://avtocod.ru/proverkaavto/" + result + "?rd=GRZ");
                                    intent.putExtra("number", result);
                                    startActivity(intent);
                                } else
                                    Toast.makeText(self, "Ничего не найдено", Toast.LENGTH_LONG).show();
                            } else {
                                Toast.makeText(self, result.isEmpty() ? "Ничего не найдено" : ("Результат распознавания: " + result), Toast.LENGTH_LONG).show();
                            }
                            time = timeout;
                        }
                    }
                }
                return true;
            }
        });
        tesseract = new TessBaseAPI();
        initializeOpenCVDependencies();
        initializeTessAPIBase();
    }

    private String checkSymbol(String symbol1) {
        if (symbol1.isEmpty())
            return "";
        symbol1 = symbol1.toUpperCase();
        for (int i = 0; i < symbol1.length(); i++) {
            char sym = symbol1.charAt(i);
            String symbol = symbol1.substring(i, i + 1);
            if (symbol.equals("А") || symbol.equals("В") || symbol.equals("С") || symbol.equals("Е") || symbol.equals("Н") || symbol.equals("К")
                    || symbol.equals("М") || symbol.equals("О") || symbol.equals("Р") || symbol.equals("Т") || symbol.equals("Х") || symbol.equals("У")
                    || (sym >= '0' && sym <= '9'))
                return symbol;
        }
        return "";
    }

    String loadData(String data) {
        return sPref.getString(data, "");
    }

    //OCR
    private String OCR(Pix origImage) {

        Pix work = origImage.clone();
        File debugDir = new File(imageDirDebug, "" + origImage.getHeight() + "X" + origImage.getWidth());
        if (Boolean.parseBoolean(loadData(getString(R.string.debugMode))))
            //noinspection ResultOfMethodCallIgnored
            debugDir.mkdir();

        int width = work.getWidth();
        int height = work.getHeight();

        int maxX = 0;
        int maxY = 0;
        boolean set = false;

        //Проверенные точки
        boolean[][] checkedPoint = new boolean[height][width];

        StringBuilder stringBuilder = new StringBuilder();
        //Зонированная обработка
        for (int i = height / 2 - (int) (0.01 * height); i < height / 2 + (int) (0.01 * height) + 1; i++) {
            for (int j = 0; j < width; j++) {
                if (!checkedPoint[i][j] && work.getPixel(j, i) == -1) {
                    checkedPoint[i][j] = true;

                    //Черные пиксели найденной зоны
                    boolean[][] ourBlack = new boolean[height][width];
                    ourBlack[i][j] = true;

                    //Нахождение пикселей, принадлежащих зоне.
                    int direction = 1;
                    int minH = i;
                    int maxH = i;
                    int minW = j;
                    int maxW = j;
                    int blackCount = 1;
                    boolean second = false;
                    //Обработка строки, где найден первй пиксель.
                    for (int l = j + 1; l < width; l += direction) {
                        if (!checkedPoint[i][l] && ((l > 0 && ourBlack[i][l - 1]) || (l < width - 1 && ourBlack[i][l + 1]))) {
                            if (work.getPixel(l, i) == -1) {
                                ourBlack[i][l] = true;
                                minW = Math.min(minW, l);
                                maxW = Math.max(maxW, l);
                                blackCount++;
                                checkedPoint[i][l] = true;
                            }
                        }
                        if (!second && l == width - 1) {
                            direction = -1;
                            second = true;
                        } else if (second && l == 0) {
                            break;
                        }
                    }
                    boolean has = false;
                    boolean has2 = false;
                    boolean second2 = false;
                    int direction2 = 1;

                    //Поиск других строк
                    for (int k = i + 1; k < height; k += direction2) {
                        second = false;
                        direction = 1;
                        for (int l = 0; l < width; l += direction) {

                            if (!checkedPoint[k][l] && ((l > 0 && ourBlack[k][l - 1]) || (l < width - 1 && ourBlack[k][l + 1]) || (k > 0 && ourBlack[k - 1][l]) || (l > 0 && k > 0 && ourBlack[k - 1][l - 1]) || (l < width - 1 && k > 0 && ourBlack[k - 1][l + 1]) || (k < height - 1 && ourBlack[k + 1][l]) || (l > 0 && k < height - 1 && ourBlack[k + 1][l - 1]) || (l < width - 1 && k < height - 1 && ourBlack[k + 1][l + 1]))) {
                                if (work.getPixel(l, k) == -1) {
                                    ourBlack[k][l] = true;
                                    minW = Math.min(minW, l);
                                    maxW = Math.max(maxW, l);
                                    maxH = Math.max(maxH, k);
                                    minH = Math.min(minH, k);
                                    has = true;
                                    has2 = true;
                                    blackCount++;
                                    checkedPoint[k][l] = true;
                                }
                            }
                            if (!second && l == width - 1) {
                                direction = -1;
                                second = true;
                                has = false;
                            } else if (second && l == 0) {
                                if (has) {
                                    second = false;
                                    direction = 1;
                                    has = false;
                                } else
                                    break;
                            }
                        }
                        if (!second2 && k == height - 1) {
                            direction2 = -1;
                            second2 = true;
                            has2 = false;
                        } else if (second2 && k == 0) {
                            if (has2) {
                                second2 = false;
                                direction2 = 1;
                                has2 = false;
                            } else
                                break;
                        }
                    }

                    //Вычисление размеров зоны
                    int lenW = maxW - minW + 1;
                    int lenH = maxH - minH + 1;
                    //Вычисления параметров соотношения
                    int len = lenH - lenW;
                    int lenPercent = (int) Math.round(((double) lenW / lenH) * 100);
                    int percent = (int) Math.round(((double) blackCount / (lenW * lenH)) * 100);

                    //Если это не угловой элемент, то проверить параметры соотношений, иначе просто стереть объект
                    if ((len > 0 && lenPercent > 30 && lenPercent < 88 && percent > 20 && percent < 67)) {
                        boolean skip = false;
                        /*if (set) {
                            if ((double) maxX / (lenW - 1) > 1.6f)
                                skip = true;
                        } else {
                            set = true;
                            maxX = lenW - 1;
                            maxY = lenH - 1;
                        }*/
                        if (!skip) {
                            Pix cutPix = Clip.clipRectangle(origImage, new Box(minW, minH, lenW - 1, lenH - 1));
                            System.out.println("w=" + (lenW - 1) + ", h=" + (lenH - 1));
                            tesseract.setImage(cutPix);
                            String res = tesseract.getUTF8Text().replace("C", "С").replace("E", "Е").replace("H", "Н");
                            if (!res.isEmpty() && res.length() > 4)
                                res = res.substring(0, 1) + res.substring(1, 4).replace("O", "0") + res.substring(4);
                            File imageFileDebug = new File(debugDir, "" + i + "x" + j + " == " + res + ".png");
                            if (Boolean.parseBoolean(loadData(getString(R.string.debugMode))))
                                WriteFile.writeImpliedFormat(cutPix, imageFileDebug);
                            System.out.println("RESULT: " + res);
                            System.out.println("KEY: " + checkSymbol(res));
                            if (!checkSymbol(res).isEmpty()) {
                                stringBuilder.append(res);
                            }
                        }
                    }

                    //Удаление (обесчвечивание) черного цвета зоны
                    for (int k = minH; k <= maxH; k++) {
                        for (int l = minW; l <= maxW; l++) {
                            if (ourBlack[k][l])
                                work.setPixel(l, k, 0);
                        }
                    }
                } else {
                    checkedPoint[i][j] = true;
                }
            }
        }
        return stringBuilder.toString().

                toUpperCase();

    }

    //чистка шумов на изображении, обрабатывает изображение и удаляет всё, что не похоже на символы и мешает распознавать номер.
    private Pix noiseCleaner(Pix origImage) {

        Pix work = origImage.clone();

        int width = work.getWidth();
        int height = work.getHeight();

        int maxX = 0;
        int maxY = 0;
        boolean set = false;

        //Проверенные точки
        boolean[][] checkedPoint = new boolean[height][width];

        //Зонированная обработка
        for (int i = height / 2; i < height; i++) {
            for (int j = 0; j < width; j++) {
                if (!checkedPoint[i][j] && work.getPixel(j, i) == -1) {
                    checkedPoint[i][j] = true;

                    //Черные пиксели найденной зоны
                    boolean[][] ourBlack = new boolean[height][width];
                    ourBlack[i][j] = true;

                    //Нахождение пикселей, принадлежащих зоне.
                    int direction = 1;
                    int minH = i;
                    int maxH = i;
                    int minW = j;
                    int maxW = j;
                    int blackCount = 1;
                    boolean second = false;
                    //Обработка строки, где найден первй пиксель.
                    for (int l = j + 1; l < width; l += direction) {
                        if (!checkedPoint[i][l] && ((l > 0 && ourBlack[i][l - 1]) || (l < width - 1 && ourBlack[i][l + 1]))) {
                            if (work.getPixel(l, i) == -1) {
                                ourBlack[i][l] = true;
                                minW = Math.min(minW, l);
                                maxW = Math.max(maxW, l);
                                blackCount++;
                                checkedPoint[i][l] = true;
                            }
                        }
                        if (!second && l == width - 1) {
                            direction = -1;
                            second = true;
                        } else if (second && l == 0) {
                            break;
                        }
                    }
                    boolean has = false;
                    boolean has2 = false;
                    boolean second2 = false;
                    int direction2 = 1;

                    //Поиск других строк
                    for (int k = i + 1; k < height; k += direction2) {
                        second = false;
                        direction = 1;
                        for (int l = 0; l < width; l += direction) {

                            if (!checkedPoint[k][l] && ((l > 0 && ourBlack[k][l - 1]) || (l < width - 1 && ourBlack[k][l + 1]) || (k > 0 && ourBlack[k - 1][l]) || (l > 0 && k > 0 && ourBlack[k - 1][l - 1]) || (l < width - 1 && k > 0 && ourBlack[k - 1][l + 1]) || (k < height - 1 && ourBlack[k + 1][l]) || (l > 0 && k < height - 1 && ourBlack[k + 1][l - 1]) || (l < width - 1 && k < height - 1 && ourBlack[k + 1][l + 1]))) {
                                if (work.getPixel(l, k) == -1) {
                                    ourBlack[k][l] = true;
                                    minW = Math.min(minW, l);
                                    maxW = Math.max(maxW, l);
                                    maxH = Math.max(maxH, k);
                                    minH = Math.min(minH, k);
                                    has = true;
                                    has2 = true;
                                    blackCount++;
                                    checkedPoint[k][l] = true;
                                }
                            }
                            if (!second && l == width - 1) {
                                direction = -1;
                                second = true;
                                has = false;
                            } else if (second && l == 0) {
                                if (has) {
                                    second = false;
                                    direction = 1;
                                    has = false;
                                } else
                                    break;
                            }
                        }
                        if (!second2 && k == height - 1) {
                            direction2 = -1;
                            second2 = true;
                            has2 = false;
                        } else if (second2 && k == 0) {
                            if (has2) {
                                second2 = false;
                                direction2 = 1;
                                has2 = false;
                            } else
                                break;
                        }
                    }

                    //Вычисление размеров зоны
                    int lenW = maxW - minW + 1;
                    int lenH = maxH - minH + 1;
                    //Вычисления параметров соотношения
                    int len = lenH - lenW;
                    int lenPercent = (int) Math.round(((double) lenW / lenH) * 100);
                    int percent = (int) Math.round(((double) blackCount / (lenW * lenH)) * 100);


                    /*//Проверка на вертикальные границы
                    int count1 = 0;
                    int count2 = 0;
                    has = false;
                    has2 = false;
                    boolean left = false;
                    boolean right = false;
                    for (int k = minH; k <= maxH; k++) {
                        if (ourBlack[k][minW] || (minW < width - 1 && ourBlack[k][minW + 1])) {
                            if (!left) {
                                count1++;
                                has = true;
                            }
                        } else if (has)
                            left = true;
                        if (ourBlack[k][maxW] || (maxW > 0 && ourBlack[k][maxW - 1])) {
                            if (!right) {
                                count2++;
                                has2 = true;
                            }
                        } else if (has2)
                            right = true;
                        if (left && right)
                            break;
                    }
                    left = false;
                    right = false;
                    if (lenH - count1 <= lenH / 4)
                        left = true;
                    if (lenH - count2 <= lenH / 4)
                        right = true;

                    *//*if (right && !left)
                        if (count1 > lenH / 4)
                            left = true;*//*
                    if (!right && left)
                        if (count2 > lenH / 4)
                            right = true;

                    *//*if (count1 > lenH / 4)
                        left = true;
                    if (count2 > lenH / 4)
                        right = true;*//*

                    //Проверка на горизонтальные границы
                    count1 = 0;
                    count2 = 0;
                    has = false;
                    has2 = false;
                    boolean up = false;
                    boolean down = false;
                    for (int k = minW; k <= maxW; k++) {
                        if (ourBlack[minH][k] || (minH < width - 1 && ourBlack[minH + 1][k])) {
                            if (!up) {
                                count1++;
                                has = true;
                            }
                        } else if (has)
                            up = true;
                        if (ourBlack[maxH][k] || (maxH > 0 && ourBlack[maxH - 1][k])) {
                            if (!down) {
                                count2++;
                                has2 = true;
                            }
                        } else if (has2) {
                            down = true;
                        }
                        if (up && down)
                            break;
                    }
                    up = false;
                    down = false;
                    if (lenW - count1 <= lenW / 4)
                        up = true;
                    if (lenW - count2 <= lenW / 4)
                        down = true;
                    if (left) {
                        if (count1 > lenW / 4)
                            up = true;
                        if (count2 > lenW / 4)
                            down = true;
                    }*/


                    //Если это не угловой элемент, то проверить параметры соотношений, иначе просто стереть объект
                    /*if (!(up && left && !down && !right) && !(up && !left && !down && right) && !(!up && left && down && !right) && !(!up && !left && down && right))*/
                    if ((len > 0 && lenPercent > 30 && lenPercent < 87 && percent > 20 && percent < 67)) {
                        boolean skip = false;
                        if (set) {
                            if ((double) maxX / (lenW - 1) > 1.5f)
                                skip = true;
                        } else {
                            set = true;
                            maxX = lenW - 1;
                            maxY = lenH - 1;
                        }
                        if (!skip) {
                            Pix cutPix = Clip.clipRectangle(origImage, new Box(minW, minH, lenW - 1, lenH - 1));
                            System.out.println("w=" + (lenW - 1) + ", h=" + (lenH - 1));
                            tesseract.setImage(cutPix);
                            String res = tesseract.getUTF8Text();
                            System.out.println("RESULT: " + res);
                            if (!checkSymbol(res).isEmpty())
                                continue;
                        }
                    }

                    //Удаление (обесчвечивание) черного цвета зоны
                    for (int k = minH; k <= maxH; k++) {
                        for (int l = minW; l <= maxW; l++) {
                            if (ourBlack[k][l])
                                work.setPixel(l, k, 0);
                        }
                    }
                } else {
                    checkedPoint[i][j] = true;
                }
            }
        }
        return work;
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

    private void initializeTessAPIBase() {

        try {
            InputStream is = getResources().openRawResource(R.raw.ru);
            File tesserractDir = getDir("tesserract", Context.MODE_PRIVATE);
            File tessdataDir = new File(tesserractDir, "tessdata");
            //noinspection ResultOfMethodCallIgnored
            tessdataDir.mkdir();
            File tessFile = new File(tessdataDir, "ru.traineddata");
            FileOutputStream os = new FileOutputStream(tessFile);

            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = is.read(buffer)) != -1) {
                os.write(buffer, 0, bytesRead);
            }
            is.close();
            os.close();

            // Initialise the TessAPIBase
            tesseract.init(tesserractDir.getAbsolutePath(), "ru");
        } catch (Exception e) {
            Log.e("OCR_Tess", "Error loading tessdata", e);
        }
    }

    double converter(double value, double fromA, double fromB, double toA, double toB) {
        return (value - fromA) / (fromB - fromA) * (toB - toA) + toA;
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
        tesseract.end();
    }

    @Override
    public void onCameraViewStarted(int width, int height) {
        camWidth = width;
        camHeight = height;
        Log.d("Image size", "width = " + width);
        Log.d("Image size", "height = " + height);
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

            lastFrame = new Mat();
            inputFrame.copyTo(lastFrame);
            cascadeClassifier.detectMultiScale(grayFrame, plates, Float.parseFloat(loadData(getString(R.string.scalefactor))), (int) Math.floor(Float.parseFloat(loadData(getString(R.string.minNeighbors)))), Objdetect.CASCADE_SCALE_IMAGE, new org.opencv.core.Size(absolutePlateSize, absolutePlateSize));
            //cascadeClassifier.detectMultiScale(grayFrame, plates, 1.1, 6, Objdetect.CASCADE_SCALE_IMAGE, new org.opencv.core.Size(absolutePlateSize, absolutePlateSize));
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
        if (Boolean.parseBoolean(loadData(getString(R.string.debugMode))) && lastPoint != null)
            Imgproc.circle(inputFrame, lastPoint, 3, new Scalar(200, 100, 64), 3);
        return inputFrame;//something;
    }
}