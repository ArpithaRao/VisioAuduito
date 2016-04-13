package com.visio;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.speech.tts.TextToSpeech;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.SeekBar;

import com.customlbs.library.IndoorsException;
import com.customlbs.library.IndoorsFactory;
import com.customlbs.library.IndoorsLocationListener;
import com.customlbs.library.callbacks.IndoorsServiceCallback;
import com.customlbs.library.callbacks.LoadingBuildingStatus;
import com.customlbs.library.callbacks.RoutingCallback;
import com.customlbs.library.callbacks.ZoneCallback;
import com.customlbs.library.model.Building;
import com.customlbs.library.model.Zone;
import com.customlbs.shared.Coordinate;
import com.customlbs.surface.library.IndoorsSurface;
import com.customlbs.surface.library.IndoorsSurfaceFactory;
import com.customlbs.surface.library.IndoorsSurfaceFragment;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;


public class MainActivity extends AppCompatActivity implements  IndoorsServiceCallback, IndoorsLocationListener, IndoorsSurface.OnSurfaceClickListener{
    public static String TAG = MainActivity.class.getSimpleName();
    public static final String extraName = "BUILDINGID";
    public final static int REQ_CODE_SPEECH_INPUT = 100;
    public MainActivity thisObject;

    public static IndoorsSurfaceFragment indoorsFragment;
    public static IndoorsFactory.Builder indoorsBuilder;



    public static Coordinate currentUserCoordinates;
    public static int currentAccuracy;
    public static float currentUserOrientation;

    public static List<Zone> zonesList;
    public static HashMap<String,Coordinate> zoneEntrance;

    public static boolean inRoutingMode = false;
    private static List<RouterInterface> registeredObjects = new ArrayList<RouterInterface>(); //These objects need to be notified
    //in case of user change of position
    //The whole loop will be controlled
    // only when in routing mode.

    public boolean initializedZonesAndPosition = false; //This needs to be true before we do anything/

    public VoiceCommandInput mInputVoiceCommand;
    public String mDestinationZone;

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main,menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()){
            case R.id.action_settings:
                Intent intent = new Intent(this,SettingsActivity.class);
                startActivity(intent);
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        thisObject = this;
        indoorsBuilder = new IndoorsFactory.Builder();
        indoorsBuilder.setContext(this);
        indoorsBuilder.setPassiveServiceCallback(this);
        indoorsBuilder.setUserInteractionListener(this);
        indoorsBuilder.setEvaluationMode(false);

        indoorsBuilder.setApiKey("34420529-47cf-4e4f-a3b6-79f1e0948aab");
        indoorsBuilder.setBuildingId(Long.parseLong(getIntent().getStringExtra(extraName)));

        Toolbar myToolbar = (Toolbar) findViewById(R.id.application_toolbar);
        setSupportActionBar(myToolbar);


        IndoorsSurfaceFactory.Builder indoorsSurface = new IndoorsSurfaceFactory.Builder();
        indoorsSurface.setIndoorsBuilder(indoorsBuilder);

        SpeechEngine.createInstance(this);

        indoorsFragment = indoorsSurface.build();
        indoorsFragment.registerOnSurfaceClickListener(this);
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.wrapper, indoorsFragment, "indoors");
        transaction.commit();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if(SpeechEngine.getInstance()!=null)
            SpeechEngine.getInstance().stop();
    }
    @Override
    protected void onResume(){
        super.onResume();
        SpeechEngine.createInstance(this);
    }

    protected void onStop(){
        super.onStop();
        if(SpeechEngine.getInstance()!=null)
            SpeechEngine.getInstance().stop();
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
        initializeZoneOnBuildingLoad(building);

    }

    private void initializeZoneOnBuildingLoad(Building building) {
        indoorsFragment.getIndoors().getZones(building, new ZoneCallback() {
            @Override
            public void setZones(ArrayList<Zone> arrayList) {
                MainActivity.zonesList = arrayList;
                MainActivity.zoneEntrance = getZonesEntrance(MainActivity.zonesList);
                if (currentUserCoordinates != null && !initializedZonesAndPosition) {
                    initializedZonesAndPosition = true;
                }
            }
        });
    }

    private HashMap<String,Coordinate> getZonesEntrance(List<Zone> zonesList) {
        HashMap<String,Coordinate> mapZoneToCoordinate = new HashMap<>();
        for(Zone eachZone : zonesList){
            Coordinate newCoordinate = new Coordinate((eachZone.getZonePoints().get(0).x+eachZone.getZonePoints().get(1).x)/2,
                    (eachZone.getZonePoints().get(0).y+eachZone.getZonePoints().get(1).y)/2,
                    eachZone.getZonePoints().get(0).z);
            mapZoneToCoordinate.put(eachZone.getName().toUpperCase(),newCoordinate);
        }
        return mapZoneToCoordinate;
    }

    @Override
    public void leftBuilding(Building building) {

    }

    @Override
    public void positionUpdated(Coordinate coordinate, int i) {

        Log.d(TAG,coordinate.toString());

        currentUserCoordinates = coordinate;
        currentAccuracy = i;
        if(zonesList!=null&&!initializedZonesAndPosition){
            initializedZonesAndPosition = true;
        }
        if(inRoutingMode)
            notifyAllObservers();
    }

    private void notifyAllObservers() {
        for(RouterInterface localInterfaceObject:MainActivity.registeredObjects){
            localInterfaceObject.getDistance(currentUserCoordinates,currentUserOrientation);
        }
    }

    @Override
    public void orientationUpdated(float v) {
        currentUserOrientation = v;
        if(zonesList!=null&&currentUserCoordinates!=null&&!initializedZonesAndPosition){
            initializedZonesAndPosition = true;
        }
    }

    @Override
    public void changedFloor(int i, String s) {

    }

    @Override
    public void enteredZones(List<Zone> list) {

        List<String> stringList = new ArrayList<>();
        for(Zone zone : list){
            stringList.add(zone.getName());
        }

        if(inRoutingMode) {
            if(stringList.contains( mDestinationZone)){
                inRoutingMode=false;
                SpeechEngine.getInstance().speak("Destination",SpeechEngine.QUEUE_ADD,null);
            }
        }
    }

    @Override
    public void buildingLoadingCanceled() {

    }

    public Coordinate getCurrentUserCoordinates() {
        return currentUserCoordinates;
    }

    public int getCurrentAccuracy() {
        return currentAccuracy;
    }

    public float getCurrentUserOrientation() {
        return currentUserOrientation;
    }

    public static void registerForNextChange(RouterInterface registeringObject){
        registeredObjects.add(registeringObject);

        inRoutingMode = true;
    }


    @SuppressLint("NewApi")
    @Override
    public void onClick(Coordinate coordinate) {
        if(initializedZonesAndPosition) {
            SpeechEngine speechEngine = SpeechEngine.getInstance();
         //   speechEngine.speak(getString(R.string.initiateNavigationPrompt),TextToSpeech.QUEUE_FLUSH,null,"prompt");
            if (mInputVoiceCommand == null)
                mInputVoiceCommand = new VoiceCommandInput(this);
            mInputVoiceCommand.takeSpeechInput(R.string.initiateNavigationPrompt);
        }
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode==MainActivity.REQ_CODE_SPEECH_INPUT){
            if(resultCode==RESULT_OK){
                if(data!=null){
                    List<String> inputCommand = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                    mDestinationZone=inputCommand.get(0);
                    mInputVoiceCommand.routeToZone(inputCommand.get(0).toUpperCase());

                }
            }
        }
    }
}

class VoiceCommandInput{
    private Activity callingActivity;

    public VoiceCommandInput(Activity callingActivity) {
        this.callingActivity = callingActivity;
    }

    public void takeSpeechInput(int promptID){
        String prompt="Could not initialize";

        Intent intentHandle = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intentHandle.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intentHandle.putExtra(RecognizerIntent.EXTRA_PROMPT,prompt);
        try{
            callingActivity.startActivityForResult(intentHandle,MainActivity.REQ_CODE_SPEECH_INPUT);
         //   prompt = callingActivity.getApplicationContext().getString(promptID);
        }catch(ActivityNotFoundException e){
            // Toast.makeText(callingActivity,"Speech Input not supported on this device",Toast.LENGTH_LONG).show();
        }
    }

    public void routeToZone(String zoneId){
        Coordinate sourceCoordinate = MainActivity.currentUserCoordinates;
        Coordinate destinationCoordinate;
        if(MainActivity.zoneEntrance.containsKey(zoneId.toUpperCase())){
            destinationCoordinate = MainActivity.zoneEntrance.get(zoneId.toUpperCase());
            MainActivity.indoorsFragment.getIndoors().getRouteAToB(sourceCoordinate, destinationCoordinate, new RoutingCallback() {
                @Override
                @SuppressWarnings("deprication")
                public void setRoute(ArrayList<Coordinate> arrayList) {
                    MainActivity.indoorsFragment.getIndoors().enableRouteSnapping(arrayList);
                    MainActivity.indoorsFragment.getSurfaceState().setRoutingPath(arrayList);
                    MainActivity.indoorsFragment.getSurfaceState().orientedNaviArrow = true;
                    MainActivity.indoorsFragment.updateSurface();
                    MainActivity.registerForNextChange(new RouterImplementation(arrayList));
                    Log.d("Route",arrayList.toString());
                }

                @Override
                public void onError(IndoorsException e) {

                }
            });
        }else
            takeSpeechInput(R.string.couldNotFindLocation);


    }


}

interface RouterInterface{

    int THRESHOLD = 1000;
    void getDistance(Coordinate currentPosition, float currentOrientation);
}

class SpeechEngine extends TextToSpeech {

    private static SpeechEngine _instance = null;

    private SpeechEngine(Context context, OnInitListener listener) {
        super(context, listener);
    }

    public static SpeechEngine getInstance(){
        return _instance;
    }



    public static SpeechEngine createInstance(Context context){
        OnInitListener listener = new OnInitListener() {
            @Override
            public void onInit(int status) {

            }
        };
        if(_instance==null)
            _instance= new SpeechEngine(context,listener);

        return _instance;
    }
}


class RouterImplementation implements RouterInterface{
    public List<Coordinate> routerCoordinate;
    public Iterator<Coordinate> iterCoordinate;
    //public Activity callingActivity;
    Coordinate nextCoordinate = null;
    int i = 0;



    public RouterImplementation(List<Coordinate> routerCoordinate) {
        this.routerCoordinate = routerCoordinate;

        firstRegister();
    }

    public void firstRegister(){
        setNextCoordinate();


    }

    private void setNextCoordinate() {
        nextCoordinate = routerCoordinate.get(i);
        routerCoordinate.remove(i);
    }


    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public void sayNextRoute(Coordinate userCurrentPosition, float userCurrentOrientation) {

            SpeechEngine speechengine=SpeechEngine.getInstance();

            Coordinate currentCoordinate = this.nextCoordinate;
            setNextCoordinate();
            if(routerCoordinate.size()>1) {
                double direction = getDirection(currentCoordinate, nextCoordinate, userCurrentOrientation);
                String turnDirection = getTurnDirection(direction);
                Log.d(MainActivity.TAG+" route", turnDirection == null ? "Null" : enhanceDirection(direction, turnDirection));
                speechengine.speak(enhanceDirection(direction, turnDirection),TextToSpeech.QUEUE_ADD,null,"Direction");
            }else{
                double direction = getDirection(currentCoordinate, nextCoordinate, userCurrentOrientation);
                String turnDirection = getTurnDirection(direction);
                Log.d(MainActivity.TAG + " route","Your destination is on your "+turnDirection);
                SpeechEngine.getInstance().speak("Your destination is on your " + turnDirection,TextToSpeech.QUEUE_ADD,null,"Destination");
                MainActivity.inRoutingMode = false;
            }

    }

    public String enhanceDirection(double direction, String turnDirection){
        float modDirection = (float) Math.abs(direction);
        if(modDirection<22.49f){
            return "GO STRAIGHT";
        }else if(modDirection>=22.50f && modDirection <= 44.9f){
            return "TAKE SLIGHT " + turnDirection + " TURN";
        }else if(modDirection>=45f && modDirection<=134.9f){
            return "TAKE" + turnDirection + "TURN";
        }else if(modDirection>=135.0f && modDirection <=147.49f){
            return "TAKE HARD " + turnDirection + " TURN";
        }else{
            return "TURN BACK";
        }
    }
    public String getTurnDirection(double direction) {
        boolean positive;
        boolean invert = Math.abs(direction)>180?true:false;
        double actualTurnAngle = direction;
        if(invert){
            double howMuchMoreThan180 = direction - 180;
            actualTurnAngle = 180 - howMuchMoreThan180;

            int sign = (int)(direction / (Math.abs(direction)));
            actualTurnAngle = (sign * -1) * actualTurnAngle;
        }

        positive = actualTurnAngle>0?true:false;

        return positive?"LEFT":"RIGHT";

    }

    public double getDirection(Coordinate userCurrentPosition, Coordinate nextPosition, float userCurrentOrientation){
        int dx = nextCoordinate.x - userCurrentPosition.x ;
        int dy = nextCoordinate.y - userCurrentPosition.y ;
        double bearing = (180/Math.PI) * Math.atan2(dx,dy);
        bearing = bearing>0?bearing:360-Math.abs(bearing);

        return (userCurrentOrientation-bearing);

    }

    @Override
    public void getDistance(Coordinate currentPosition, float currentOrientation) {
        if(this.routerCoordinate.size()<=0){
            MainActivity.inRoutingMode = false;
            return;
        }
        int dx = Math.abs(currentPosition.x- nextCoordinate.x);
        int dy = Math.abs(currentPosition.y- nextCoordinate.y);

        if(Math.sqrt(dy-dx)<=THRESHOLD){

            sayNextRoute(currentPosition,currentOrientation);
        }
    }
}