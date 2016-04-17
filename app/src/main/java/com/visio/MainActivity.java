package com.visio;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.ActivityNotFoundException;
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

import com.customlbs.library.IndoorsException;
import com.customlbs.library.IndoorsFactory;
import com.customlbs.library.IndoorsLocationListener;
import com.customlbs.library.LocalizationParameters;
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
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;


public class MainActivity extends AppCompatActivity implements  IndoorsServiceCallback, IndoorsLocationListener, IndoorsSurface.OnSurfaceClickListener{
    public static String TAG = MainActivity.class.getSimpleName();
    public static final String extraName = "BUILDINGID";
    public final static int REQ_CODE_SPEECH_INPUT = 100;
    public MainActivity thisObject;

    public static FragmentTransaction transaction;
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

    public static int threshold;
    public static float offset;

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
        LocalizationParameters setupParams = new LocalizationParameters();
        setupParams.setPositionCalculationInterval(1000);
        setupParams.setPositionUpdateInterval(1000);
        setupParams.setTrackingInterval(1000);
        setupParams.setUseStabilizationFilter(false);
        indoorsBuilder = new IndoorsFactory.Builder();
        indoorsBuilder.setContext(this);
        indoorsBuilder.setPassiveServiceCallback(this);
        indoorsBuilder.setUserInteractionListener(this);
        indoorsBuilder.setEvaluationMode(false);
        indoorsBuilder.setLocalizationParameters(setupParams);
        indoorsBuilder.setApiKey("34420529-47cf-4e4f-a3b6-79f1e0948aab");
        indoorsBuilder.setBuildingId(Long.parseLong(getIntent().getStringExtra(extraName)));

        Toolbar myToolbar = (Toolbar) findViewById(R.id.application_toolbar);
        setSupportActionBar(myToolbar);


        IndoorsSurfaceFactory.Builder indoorsSurface = new IndoorsSurfaceFactory.Builder();
        indoorsSurface.setIndoorsBuilder(indoorsBuilder);

        SpeechEngine.createInstance(this);

        indoorsFragment = indoorsSurface.build();
        indoorsFragment.registerOnSurfaceClickListener(this);
        transaction = getSupportFragmentManager().beginTransaction();
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

        //Log.d(TAG,coordinate.toString());


        currentUserCoordinates = coordinate;
        currentAccuracy = i;
        //currentUserOrientation = indoorsFragment.getSurfaceState().userOrientationDegrees;

       /* transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(android.R.id.content,indoorsFragment,"indoors");
        transaction.commit();*/

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
        registeringObject.getDistance(currentUserCoordinates,currentUserOrientation);
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
        String prompt="Where would you like to go ?";

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
        MainActivity.threshold=callingActivity.getSharedPreferences("MyPreferences",0).getInt("threshold",100);
        MainActivity.offset=callingActivity.getSharedPreferences("MyPreferences",0).getInt("degrees",225);
        if(MainActivity.zoneEntrance.containsKey(zoneId.toUpperCase())){
            destinationCoordinate = MainActivity.zoneEntrance.get(zoneId.toUpperCase());
            MainActivity.indoorsFragment.getIndoors().getRouteAToB(sourceCoordinate, destinationCoordinate, new RoutingCallback() {
                @Override
                @SuppressWarnings("deprication")
                public void setRoute(ArrayList<Coordinate> arrayList) {
                    for (Coordinate loc : arrayList) {
                        //Log.d("Route", loc.toString());
                    }
                    MainActivity.indoorsFragment.getIndoors().enableRouteSnapping(arrayList);
                    MainActivity.indoorsFragment.getSurfaceState().setRoutingPath(arrayList);
                    MainActivity.indoorsFragment.getSurfaceState().orientedNaviArrow = true;
                    MainActivity.indoorsFragment.updateSurface();
                    MainActivity.registerForNextChange(new RouterImplementation(arrayList,MainActivity.threshold));
                   // Log.d("Route",arrayList.toString());
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
    int THRESHOLD = 100;
    boolean visited[];
    public RouterImplementation(List<Coordinate> routerCoordinate,int threshold) {
        this.routerCoordinate = routerCoordinate;
        visited = new boolean[this.routerCoordinate.size()];
        firstRegister();
        this.THRESHOLD=threshold;
    }

    public void firstRegister(){

    }


    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public void sayNextRoute(Coordinate userCurrentPosition, float userCurrentOrientation, int nextCoordinateIndex) {

        SpeechEngine speechEngine = SpeechEngine.getInstance();

        if(!visited[nextCoordinateIndex-1]) {
            visited[nextCoordinateIndex-1] = true;
            double distance = calculateDistance(nextCoordinate, userCurrentPosition);
            double direction = getDirection(userCurrentPosition, nextCoordinate, userCurrentOrientation);
            double finalDirection = getFinalDirectionAngle(direction);
            String turnDirection = getTurnDirection(finalDirection);
            Log.d("PtNext", nextCoordinate.toString());
            Log.d("PtUserLoc",userCurrentPosition.toString());
            Log.d("PtOrientation",String.valueOf(userCurrentOrientation));

            if (nextCoordinateIndex != this.routerCoordinate.size() - 1) {

                Log.d(MainActivity.TAG + " route", turnDirection == null ? "Null" : enhanceDirection(finalDirection, turnDirection,(int)distance));
                //System.out.println(enhanceDirection(finalDirection,turnDirection)+" "+finalDirection);

                speechEngine.speak(enhanceDirection(finalDirection, turnDirection,(int)distance), TextToSpeech.QUEUE_FLUSH, null, "Direction");
            } else {

                Log.d(MainActivity.TAG + " last", "Your destination is on your " + turnDirection+direction);
                //System.out.println("Your destination is on your " + turnDirection +" "+finalDirection);
                speechEngine.speak("Your destination is " + enhanceDestination(finalDirection,turnDirection,(int)distance), TextToSpeech.QUEUE_FLUSH, null, "Destination");
                //MainActivity.inRoutingMode = false;
            }
        }

    }

    private double getFinalDirectionAngle(double direction) {
        boolean positive;
        Log.d("Raw direction", String.valueOf(direction));
        boolean invert = Math.abs(direction)>180?true:false;
        double actualTurnAngle = direction;
        if(invert){

            actualTurnAngle = 360 - Math.abs(actualTurnAngle);

            int sign = (int)(direction / (Math.abs(direction)));
            actualTurnAngle = (sign * -1) * actualTurnAngle;
        }

        return actualTurnAngle;

    }

    public String enhanceDestination(double direction, String turnDirection, int distance){
        float modDirection = (float) Math.abs(direction);
        Log.d("modDirection",String.valueOf(modDirection));
        if(modDirection < 45.0f){
            return "in slightly to the " + turnDirection + " ahead of you";
        }else if(modDirection >=45.0f && modDirection < 90.0f){
            return "to the " + turnDirection + " of you";
        }else if(modDirection >= 90.0f && modDirection <=180.0f){
            return "behind you on the " + turnDirection ;
        }else{
            return "Direction error";
        }
    }

    public String enhanceDirection(double direction, String turnDirection, int distance){
        float modDirection = (float) Math.abs(direction);

        Log.d("Direction",String.valueOf(modDirection)+" "+turnDirection);

        if(modDirection<22.49f){
            return "GO STRAIGHT";
        }else if(modDirection>=22.50f && modDirection <= 44.9f){
            return "TAKE SLIGHT " + turnDirection + " TURN for "+distance;
        }else if(modDirection>=45f && modDirection<=134.9f){
            return "TAKE " + turnDirection + " TURN for" + distance;
        }else if(modDirection>=135.0f && modDirection <=147.49f){
            return "TAKE HARD " + turnDirection + " TURN for "+ distance;
        }else if(modDirection >=148 && modDirection <=180){
            return "TURN BACK for "+distance;
        }else return "Direction error";
    }
    public String getTurnDirection(double actualTurnAngle) {
        boolean positive;
        positive = actualTurnAngle>0?true:false;

        return positive?"RIGHT":"LEFT";

    }

    public float updateCurrentOrientation() throws InterruptedException {
        float currentOrientation = 0;
        for(int i=0;i<10;i++){
            currentOrientation=MainActivity.indoorsFragment.getSurfaceState().userOrientationDegrees;
            Thread.sleep(5);
        }
        return currentOrientation;
    }

    public double getDirection(Coordinate userCurrentPosition, Coordinate nextPosition, float userCurrentOrientation){
        try {
            userCurrentOrientation=updateCurrentOrientation();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        float correctedOrientation=(userCurrentOrientation-MainActivity.offset+360)%360;
        int dx =  nextCoordinate.x - userCurrentPosition.x;
        int dy =   nextCoordinate.y - userCurrentPosition.y;
        double bearing = (180/Math.PI) * Math.atan2(dx,dy);
        bearing = bearing>0?bearing:360-Math.abs(bearing);
        return (correctedOrientation-bearing);

    }

    @Override
    public void getDistance(Coordinate currentPosition, float currentOrientation) {
        LinkedList<Coordinate> temp = new LinkedList<>(this.routerCoordinate);
        ListIterator tempIter = temp.listIterator();
        Coordinate setThisToNextCoordinate = null;
        int i = 0;
        int targetIndex = 0;
        double min = Double.MAX_VALUE;
        while(tempIter.hasNext()){
            Coordinate targetCoordinate = (Coordinate) tempIter.next();
            double distance = calculateDistance(currentPosition,targetCoordinate);

            if(distance<=THRESHOLD&&distance<min){
                setThisToNextCoordinate = targetCoordinate;
                min = distance;
                targetIndex = i;
            }
            i++;
        }

        if(setThisToNextCoordinate!=null){
            if(targetIndex!=this.routerCoordinate.size()-1) {
                this.nextCoordinate = temp.get(targetIndex + 1);
                this.sayNextRoute(setThisToNextCoordinate, currentOrientation, targetIndex + 1);
            }
        }

    }

    double calculateDistance(Coordinate currentPosition, Coordinate targetPosition){
        double dx = Math.abs(currentPosition.x - targetPosition.x);
        double dy = Math.abs(currentPosition.y - targetPosition.y);
        return Math.sqrt(dy+dx);
    }

}