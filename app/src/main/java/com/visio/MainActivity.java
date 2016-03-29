package com.visio;

import android.content.Intent;
import android.speech.RecognizerIntent;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.customlbs.library.Indoors;
import com.customlbs.library.IndoorsException;
import com.customlbs.library.IndoorsFactory;
import com.customlbs.library.callbacks.IndoorsServiceCallback;
import com.customlbs.library.callbacks.LoadingBuildingCallback;
import com.customlbs.library.callbacks.LoadingBuildingStatus;
import com.customlbs.library.callbacks.OnlineBuildingCallback;
import com.customlbs.library.callbacks.ZoneCallback;
import com.customlbs.library.model.Building;
import com.customlbs.library.model.Zone;
import com.customlbs.model.WayPoint;
import com.customlbs.shared.Coordinate;
import com.customlbs.surface.library.IndoorsSurfaceFactory;
import com.customlbs.surface.library.IndoorsSurfaceFragment;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

public class MainActivity extends AppCompatActivity {

    private static final int SPEECH_REQUEST_CODE = 1;
    public static ArrayList<Building> buildingList;
    public static ArrayList<Zone> zonelist;

    public static Indoors indoors;
    public static OnlineBuildingCallback onlinebuildingcallback;
    public static ZoneCallback zonecallback;
    public static LoadingBuildingCallback loadingbuildingcallback;
    public static IndoorsServiceCallback serviceCallback;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        IndoorsFactory.createInstance(this, "34420529-47cf-4e4f-a3b6-79f1e0948aab", new IndoorsServiceCallback() {
            @Override
            public void connected() {
                IndoorsFactory.getInstance().getOnlineBuildings(MainActivity.onlinebuildingcallback);
            }

            @Override
            public void onError(IndoorsException e) {

            }
        });



        MainActivity.onlinebuildingcallback= new OnlineBuildingCallback() {
            @Override
            public void setOnlineBuildings(ArrayList<Building> arrayList) {
                MainActivity.buildingList=arrayList;
                IndoorsFactory.getInstance().getZones(buildingList.get(0),MainActivity.zonecallback);
            }
        };

        MainActivity.zonecallback = new ZoneCallback() {
            @Override
            public void setZones(ArrayList<Zone> arrayList) {
                zonelist=arrayList;
            }
        };

        MainActivity.loadingbuildingcallback = new LoadingBuildingCallback() {
            @Override
            public void loadingBuilding(LoadingBuildingStatus loadingBuildingStatus) {

            }

            @Override
            public void buildingLoaded(Building building) {

            }

            @Override
            public void buildingLoadingCanceled() {

            }

            @Override
            public void onError(IndoorsException e) {

            }
        };

    }

















/*
        IndoorsFactory.Builder indoorsBuilder = new IndoorsFactory.Builder();
        IndoorsSurfaceFactory.Builder surfaceBuilder = new IndoorsSurfaceFactory.Builder();
        indoorsBuilder.setContext(this);
        indoorsBuilder.setApiKey("34420529-47cf-4e4f-a3b6-79f1e0948aab");


        indoorsBuilder.setBuildingId((long) 680800560);

        surfaceBuilder.setIndoorsBuilder(indoorsBuilder);

        IndoorsSurfaceFragment indoorsFragment = surfaceBuilder.build();
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.add(android.R.id.content, indoorsFragment, "indoors");
        transaction.commit();
*/






/* Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        startActivityForResult(intent, SPEECH_REQUEST_CODE);*/


    /*@Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data){
        if (requestCode == SPEECH_REQUEST_CODE && resultCode == RESULT_OK) {
            List<String> results = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
            String spokenText = results.get(0);
            Toast.makeText(this, results.get(0), Toast.LENGTH_LONG).show();
        }

        super.onActivityResult(requestCode, resultCode, data);
    }
*/

}
