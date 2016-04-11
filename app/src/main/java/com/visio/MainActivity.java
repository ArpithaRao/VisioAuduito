package com.visio;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.graphics.CornerPathEffect;
import android.speech.RecognizerIntent;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTransaction;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.customlbs.library.Indoors;
import com.customlbs.library.IndoorsException;
import com.customlbs.library.IndoorsFactory;
import com.customlbs.library.IndoorsLocationAdapter;
import com.customlbs.library.IndoorsLocationListener;
import com.customlbs.library.callbacks.IndoorsServiceCallback;
import com.customlbs.library.callbacks.LoadingBuildingStatus;
import com.customlbs.library.callbacks.OnlineBuildingCallback;
import com.customlbs.library.callbacks.RoutingCallback;
import com.customlbs.library.callbacks.ZoneCallback;
import com.customlbs.library.model.Building;
import com.customlbs.library.model.Zone;
import com.customlbs.library.util.IndoorsCoordinateUtil;
import com.customlbs.shared.Coordinate;
import com.customlbs.surface.library.IndoorsSurface;
import com.customlbs.surface.library.IndoorsSurfaceFactory;
import com.customlbs.surface.library.IndoorsSurfaceFragment;
import com.customlbs.surface.library.IndoorsSurfaceInteractionCallback;
import com.customlbs.surface.library.SurfaceState;

import java.lang.reflect.Array;
import java.nio.charset.MalformedInputException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;


public class MainActivity extends FragmentActivity implements  IndoorsServiceCallback, IndoorsLocationListener, IndoorsSurface.OnSurfaceClickListener{
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

    private static boolean inRoutingMode = false;
    private static List<RouterInterface> registeredObjects = new ArrayList<RouterInterface>(); //These objects need to be notified
    //in case of user change of position
    //The whole loop will be controlled
    // only when in routing mode.

    public boolean initializedZonesAndPosition = false; //This needs to be true before we do anything/

    public VoiceCommandInput mInputVoiceCommand;
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
        indoorsBuilder.setEvaluationMode(false);

        IndoorsSurfaceFactory.Builder indoorsSurface = new IndoorsSurfaceFactory.Builder();
        indoorsSurface.setIndoorsBuilder(indoorsBuilder);

        indoorsFragment = indoorsSurface.build();
        indoorsFragment.registerOnSurfaceClickListener(this);
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.add(android.R.id.content, indoorsFragment, "indoors");
        transaction.commit();
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
                if(currentUserCoordinates!=null&&!initializedZonesAndPosition){
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
        currentUserCoordinates = coordinate;
        currentAccuracy = i;
        if(zonesList!=null&&!initializedZonesAndPosition){
            initializedZonesAndPosition = true;
        }
        if(inRoutingMode)
            notifyAllObservers();
    }

    private void notifyAllObservers() {
        for(RouterInterface localInterfaceObject:this.registeredObjects){
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


    @Override
    public void onClick(Coordinate coordinate) {
        if(initializedZonesAndPosition) {
            if (mInputVoiceCommand == null)
                mInputVoiceCommand = new VoiceCommandInput(this);
            mInputVoiceCommand.takeSpeechInput();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode==MainActivity.REQ_CODE_SPEECH_INPUT){
            if(resultCode==RESULT_OK){
                if(data!=null){
                    List<String> inputCommand = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
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

    public void routeToZone(String zoneId){
        Coordinate sourceCoordinate = MainActivity.currentUserCoordinates;
        Coordinate destinationCoordinate = null;
        if(MainActivity.zoneEntrance.containsKey(zoneId)){
            destinationCoordinate = MainActivity.zoneEntrance.get(zoneId);
            MainActivity.indoorsFragment.getIndoors().getRouteAToB(sourceCoordinate, destinationCoordinate, new RoutingCallback() {
                @Override
                public void setRoute(ArrayList<Coordinate> arrayList) {
                    MainActivity.indoorsFragment.getIndoors().enableRouteSnapping(arrayList);
                    MainActivity.indoorsFragment.getSurfaceState().setRoutingPath(arrayList);
                    MainActivity.indoorsFragment.updateSurface();
                    MainActivity.registerForNextChange(new RouterImplementation(arrayList));

                }

                @Override
                public void onError(IndoorsException e) {

                }
            });
        }

    }
}

interface RouterInterface{


    int THRESHOLD = 1000;

    void getDistance(Coordinate currentPosition, float currentOrientation);
}

class RouterImplementation implements RouterInterface{
    private List<Coordinate> routerCoordinate;
    private Iterator<Coordinate> iterCoordinate;
    private Activity callingActivity;
    Coordinate expectedCoordinate = null;
    public RouterImplementation(List<Coordinate> routerCoordinate) {
        this.routerCoordinate = routerCoordinate;
        iterCoordinate = routerCoordinate.listIterator();
        if(!iterCoordinate.hasNext()){
            Toast.makeText(callingActivity, "There is no route defined", Toast.LENGTH_SHORT).show();
            throw new IllegalStateException("Route not initialized yet");
        }
        firstRegister();
    }

    private void firstRegister(){
        this.expectedCoordinate = iterCoordinate.next();


    }

    public void sayNextRoute(Coordinate userCurrentPosition, float userCurrentOrientation) {
        this.expectedCoordinate = iterCoordinate.next();
        double direction = getDirection(userCurrentPosition,expectedCoordinate,userCurrentOrientation);
        String turnDirection = getTurnDirection(direction);
        if(turnDirection!=null){
            Toast.makeText(callingActivity,turnDirection,Toast.LENGTH_LONG).show();
        }
    }

    private String getTurnDirection(double direction) {
        if(direction==0){
            return(new String("Straight").toUpperCase());
        }else if(direction<0&&Math.abs(direction)<=120){
            return(new String("Left").toUpperCase());
        }else if(direction>0&&Math.abs(direction)<=120){
            return(new String("Right").toUpperCase());
        }else if(Math.abs(direction)>120){
            return(new String("Around").toUpperCase());
        }
        return null;
    }

    private double getDirection(Coordinate userCurrentPosition, Coordinate nextPosition, float userCurrentOrientation){
        int dx = userCurrentPosition.x - expectedCoordinate.x;
        int dy = userCurrentPosition.y - expectedCoordinate.y;
        double bearing = (180/Math.PI) * Math.atan2(dy,dx);
        bearing = 360-bearing;
        return (userCurrentOrientation-bearing);

    }

    @Override
    public void getDistance(Coordinate currentPosition, float currentOrientation) {
        int dx = Math.abs(currentPosition.x-expectedCoordinate.x);
        int dy = Math.abs(currentPosition.y-expectedCoordinate.y);

        if(Math.sqrt(dy-dx)<=THRESHOLD){
            sayNextRoute(currentPosition,currentOrientation);
        }
    }
}