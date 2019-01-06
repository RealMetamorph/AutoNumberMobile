package coursework.cpr.car_plate_recognition;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.CardView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import org.opencv.android.OpenCVLoader;

public class MainActivity extends AppCompatActivity {


    // Used to load the 'native-lib' library on application startup.
    static {
        System.loadLibrary("native-lib");
        System.loadLibrary("opencv_java3");
        System.loadLibrary("tess");
    }
    public static final String APP_PREFERENCES = "settings";
    private Button button;
    private CardView start;
    private CardView info;
    SharedPreferences sPref;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        sPref = getSharedPreferences(APP_PREFERENCES, Context.MODE_PRIVATE);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        saveData(getString(R.string.scalefactor), getString(R.string.scalefactorDefaultValue));
        saveData(getString(R.string.frequency), getString(R.string.frequencyDefaultValue));
        saveData(getString(R.string.smoothY), getString(R.string.smoothyDefaultValue));
        saveData(getString(R.string.smoothX), getString(R.string.smoothxDefaultValue));
        saveData(getString(R.string.binarizeFactor), getString(R.string.binarizeFactorDefaultValue));
        saveData(getString(R.string.minNeighbors), getString(R.string.minNeighborsDefaultValue));


        if (!OpenCVLoader.initDebug()) {
            Log.e(this.getClass().getSimpleName(), "  OpenCVLoader.initDebug(), not working. !!!!!!!!!!!!!!!!!!!!");
        } else {
            Log.d(this.getClass().getSimpleName(), "  OpenCVLoader.initDebug(), working. !!!!!!!!!!!!!!!!!!!!!");
        }


      //  TextView tv = (TextView) findViewById(R.id.sample_text);
      //  tv.setText(stringFromJNI());
        start = findViewById(R.id.start);
        start.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startCameraActivity();
            }
        });
        info = findViewById(R.id.info);
        info.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startWebActivity();
            }
        });

        }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            startActivity(new Intent(MainActivity.this, SettingsActivity.class));
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public void startCameraActivity() {
        Intent intent = new Intent(this, CameraActivity.class);
        startActivity(intent);
    }

    public void startWebActivity() {
        Intent intent = new Intent(this, WebActivity.class);
        intent.putExtra("carNumber", "M052ET63");
        startActivity(intent);
    }

    void saveData(String data, String etText) {
        SharedPreferences.Editor ed = sPref.edit();
        ed.putString(data, etText);
        ed.apply();
        //   Toast.makeText(this, "Data saved", Toast.LENGTH_SHORT).show();

    }

    void loadData(String data, EditText etText) {
        String savedText = sPref.getString(data, "");
        etText.setText(savedText);
        Toast.makeText(this, "Data loaded", Toast.LENGTH_SHORT).show();
    }

    /**
     * A native method that is implemented by the 'native-lib' native library,
     * which is packaged with this application.
     */
    public native String stringFromJNI();



}
