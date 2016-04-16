package com.visio;

import android.content.Intent;
import android.content.SharedPreferences;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

public class SettingsActivity extends AppCompatActivity implements SeekBar.OnSeekBarChangeListener, CheckBox.OnCheckedChangeListener{

    public static final String PREF = "MyPreferences";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        SharedPreferences preferences = getSharedPreferences(PREF, 0);
        Integer thresholdVal=preferences.getInt("threshold", 100);
        Integer degreesVal = preferences.getInt("degrees",35);
        boolean[] gender = new boolean[3];
        gender[0]=preferences.getBoolean("men",false);
        gender[1]=preferences.getBoolean("women",false);
        gender[2]=preferences.getBoolean("unisex",false);



        Toolbar myToolbar = (Toolbar) findViewById(R.id.application_toolbar);
        setSupportActionBar(myToolbar);


        SeekBar threshold = (SeekBar)findViewById(R.id.threshold);
        SeekBar degrees = (SeekBar)findViewById(R.id.degrees);

        threshold.setOnSeekBarChangeListener(this);
        degrees.setOnSeekBarChangeListener(this);

        threshold.incrementProgressBy(250);
        degrees.incrementProgressBy(5);

        threshold.setProgress(thresholdVal);
        degrees.setProgress(degreesVal);

        CheckBox men = (CheckBox)findViewById(R.id.checkBox);
        CheckBox women = (CheckBox)findViewById(R.id.checkBox2);
        CheckBox unisex = (CheckBox)findViewById(R.id.checkBox3);

        men.setChecked(gender[0]);
        women.setChecked(gender[1]);
        unisex.setChecked(gender[2]);

        men.setOnCheckedChangeListener(this);
        women.setOnCheckedChangeListener(this);
        unisex.setOnCheckedChangeListener(this);
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        TextView degrees_text=(TextView)findViewById(R.id.degrees_text);
        TextView threshold_text=(TextView)findViewById(R.id.threshold_text);

        SharedPreferences settings =  getApplicationContext().getSharedPreferences(PREF,0);
        SharedPreferences.Editor editor = settings.edit();


        if (seekBar.getTag().equals("degrees_text")){
            progress = progress/5;
            progress=progress*5;
            degrees_text.setText(String.valueOf(progress));
            editor.putInt("degrees", progress);
            editor.commit();

        }else if(seekBar.getTag().equals("threshold_text")) {
            progress=progress/5;
            progress =progress*5;
            threshold_text.setText(String.valueOf(progress));
            editor.putInt("threshold",progress);
            editor.commit();

        }
    }



    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {

    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {

    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        SharedPreferences settings = getApplicationContext().getSharedPreferences(PREF,0);
        SharedPreferences.Editor editor = settings.edit();
        Log.d("TAG",buttonView.getTag().toString()+" "+isChecked);
        switch (buttonView.getTag().toString()){
            case "men": editor.putBoolean("men", isChecked);
                        editor.commit();
                        break;
            case "women":editor.putBoolean("women", isChecked);
                        editor.commit();
                        break;
            case "unisex":editor.putBoolean("unisex", isChecked);
                        editor.commit();
                        break;
        }
    }
}
