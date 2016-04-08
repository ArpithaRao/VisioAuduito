package com.visio;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.speech.RecognizerIntent;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTransaction;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import com.customlbs.library.Indoors;
import com.customlbs.library.IndoorsException;
import com.customlbs.library.IndoorsFactory;
import com.customlbs.library.IndoorsLocationListener;
import com.customlbs.library.callbacks.IndoorsServiceCallback;
import com.customlbs.library.callbacks.LoadingBuildingStatus;
import com.customlbs.library.callbacks.OnlineBuildingCallback;
import com.customlbs.library.callbacks.ZoneCallback;
import com.customlbs.library.model.Building;
import com.customlbs.library.model.Zone;
import com.customlbs.shared.Coordinate;
import com.customlbs.surface.library.IndoorsSurfaceFactory;
import com.customlbs.surface.library.IndoorsSurfaceFragment;

import java.util.ArrayList;
import java.util.List;


public class MainActivity extends FragmentActivity implements  IndoorsServiceCallback, IndoorsLocationListener {
    public static String TAG = MainActivity.class.getSimpleName();
    public ZoneCallback zonecallback;
    public static OnlineBuildingCallback onlinebuildingcallback;
    public static IndoorsSurfaceFragment indoorsFragment;
    public static IndoorsFactory.Builder indoorsBuilder;
    public static ArrayList<Building> buildingList;
    public static ArrayList<Zone> zonelist;
    public static final String extraName = "BUILDINGID";
    public final static int REQ_CODE_SPEECH_INPUT = 100;
    public MainActivity thisObject;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        thisObject = this;
        indoorsBuilder = new IndoorsFactory.Builder();
        indoorsBuilder.setContext(this);
        indoorsBuilder.setPassiveServiceCallback(this);
        indoorsBuilder.setUserInteractionListener(this);
        indoorsBuilder.setApiKey("34420529-47cf-4e4f-a3b6-79f1e0948aab");
        indoorsBuilder.setBuildingId(Long.parseLong(getIntent().getStringExtra(extraName)));
        IndoorsSurfaceFactory.Builder indoorsSurface = new IndoorsSurfaceFactory.Builder();
        indoorsSurface.setIndoorsBuilder(indoorsBuilder);

        indoorsFragment = indoorsSurface.build();

        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.add(android.R.id.content, indoorsFragment, "indoors");
        transaction.commit();




    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode==REQ_CODE_SPEECH_INPUT){
            if(resultCode==RESULT_OK&&data!=null){
                ArrayList<String> inputLocationCommand = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                TextView localTextView = (TextView) findViewById(R.id.SpeechOp);
                Log.d(TAG,inputLocationCommand.get(0));
            }
        }
    }

    @Override
    public void connected() {



}

    @Override
    public void onError (IndoorsException e){

    }

    public MainActivity() {
        super();
    }

    @Override
    public void loadingBuilding(LoadingBuildingStatus loadingBuildingStatus) {
    }

    @Override
    public void buildingLoaded(Building building) {

        indoorsFragment.getIndoors().getZones(building, new ZoneCallback() {
            @Override
            public void setZones(ArrayList<Zone> arrayList) {
                Zone temp = arrayList.get(0);
                List<Coordinate> tempCordinate = temp.getZonePoints();
                Log.d(TAG,temp.getName());
                for(Coordinate tempVal : tempCordinate){
                    Log.d(TAG,tempVal.x+","+tempVal.y);
                }

            }
        });




    }

    @Override
    public void leftBuilding(Building building) {

    }

    @Override
    public void positionUpdated(Coordinate coordinate, int i) {

    }

    @Override
    public void orientationUpdated(float v) {

    }

    @Override
    public void changedFloor(int i, String s) {

    }

    @Override
    public void enteredZones(List<Zone> list) {

    }

    @Override
    public void buildingLoadingCanceled() {

    }
}

class VoiceCommandInput{
    private Activity callingActivity;

    public VoiceCommandInput(Activity callingActivity) {
        this.callingActivity = callingActivity;
    }

    public void takeSpeechInput(){
        Intent intentHandle = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intentHandle.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intentHandle.putExtra(RecognizerIntent.EXTRA_PROMPT,"Where would you like to go?");
        try{
            callingActivity.startActivityForResult(intentHandle,MainActivity.REQ_CODE_SPEECH_INPUT);
        }catch(ActivityNotFoundException e){
            Toast.makeText(callingActivity,"Speech Input not supported on this device",Toast.LENGTH_LONG).show();
        }
    }
}