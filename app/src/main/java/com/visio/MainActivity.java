package com.visio;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
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
import com.google.android.apps.weave.apis.appaccess.AppAccessRequest;
import com.google.android.apps.weave.apis.data.Command;
import com.google.android.apps.weave.apis.data.CommandResult;
import com.google.android.apps.weave.apis.data.DeviceState;
import com.google.android.apps.weave.apis.data.WeaveApiClient;
import com.google.android.apps.weave.apis.data.WeaveDevice;
import com.google.android.apps.weave.apis.data.responses.Response;
import com.google.android.apps.weave.apis.data.responses.ResultCode;
import com.google.android.apps.weave.apis.device.DeviceLoaderCallbacks;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.MalformedInputException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;


public class MainActivity extends AppCompatActivity implements  IndoorsServiceCallback, IndoorsLocationListener, IndoorsSurface.OnSurfaceClickListener{
    public static String TAG = MainActivity.class.getSimpleName();
    public static final String extraName = "BUILDINGID";
    public final static int REQ_CODE_SPEECH_INPUT = 100;
    //public MainActivity thisObject;
    public static List<com.visio.Zone> zoneProperties;


    Weave weave = new Weave();
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
    public static String destinationEnd;
    public static int threshold;
    public static float offset;
    public static MainActivity thisObject;

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
        FirebaseZoneInfo localFirebaseZone = new FirebaseZoneInfo(this);
        localFirebaseZone.initZoneInfo();
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
        weave.initializeApiClient();
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

        currentUserOrientation=v;
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
                    MainActivity.destinationEnd = mDestinationZone;
                    mInputVoiceCommand.routeToZone(inputCommand.get(0).toUpperCase());

                }
            }
        }
    }

    public static void appendLog(String text){
        File logFile = new File("sdcard/log.file");
        if(!logFile.exists()) {
            try {
                logFile.createNewFile();

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        try{
            BufferedWriter buf = new BufferedWriter(new FileWriter(logFile,true));
            buf.append(text);
            buf.newLine();
            buf.flush();
            buf.close();
        }catch (IOException e){
            e.printStackTrace();
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
//                  MainActivity.indoorsFragment.getIndoors().enableRouteSnapping(arrayList);
                    MainActivity.indoorsFragment.getIndoors().enablePredefinedRouteSnapping();
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
            Log.d("PtOrientation",String.valueOf((userCurrentOrientation - MainActivity.offset + 360) % 360));
            Log.d("PtBearing", String.valueOf(finalDirection));

            Log.d("PtDirection",String.valueOf(finalDirection -((userCurrentOrientation - MainActivity.offset + 360) % 360)))
            ;

            if (nextCoordinateIndex != this.routerCoordinate.size() - 1) {

                Log.d(MainActivity.TAG + " route", turnDirection == null ? "Null" : enhanceDirection(finalDirection, turnDirection));
                //System.out.println(enhanceDirection(finalDirection,turnDirection)+" "+finalDirection);

                speechEngine.speak(enhanceDirection(finalDirection, turnDirection), TextToSpeech.QUEUE_FLUSH, null, "Direction");
            } else {

                Log.d(MainActivity.TAG + " last", "Your destination is on your " + turnDirection + direction);
                //System.out.println("Your destination is on your " + turnDirection +" "+finalDirection);
                speechEngine.speak("Your destination is " + enhanceDestination(finalDirection,turnDirection), TextToSpeech.QUEUE_FLUSH, null, "Destination");
                //MainActivity.inRoutingMode = false;

                speechEngine.speak("Your destination is " + enhanceDestination(finalDirection, turnDirection), TextToSpeech.QUEUE_FLUSH, null, "Destination");
                sleepThread(100);
                if(MainActivity.zoneProperties!=null) {
                    for (com.visio.Zone localZone : MainActivity.zoneProperties) {
                        if (localZone.getId().toUpperCase().equals(MainActivity.destinationEnd.toUpperCase())) {
                            speechEngine.speak("The door opens " + localZone.getDirection(), TextToSpeech.QUEUE_ADD, null, "Direction");
                            if(localZone.getType().toUpperCase().equals("automatic".toUpperCase())){
                                MainActivity.thisObject.weave.startDiscovery();
                            }
                        }
                    }
                }
                MainActivity.inRoutingMode = false;

            }
        }

    }

    public static void sleepThread(int pMilliseconds) {
        try{
            Thread.sleep(pMilliseconds);
        }catch(InterruptedException e){
            e.printStackTrace();
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
        //check this out
        //MainActivity.appendLog("ActualTurnAngle " + actualTurnAngle);

        String a=(actualTurnAngle > 0) ? "left" : "right";
        Log.d("Raw Angle", String.valueOf(actualTurnAngle) + a);

        return actualTurnAngle;

    }

    public String enhanceDestination(double direction, String turnDirection){
        float modDirection = (float) Math.abs(direction);
        Log.d("modDirection",String.valueOf(modDirection));
        if(modDirection < 45.0f){
            return "in slightly to the " + turnDirection + " ahead of you";
        }else if(modDirection >=45.0f && modDirection < 155.0f){
            return "to the " + turnDirection + " of you";
        }else if(modDirection >= 155.0f && modDirection <=180.0f){
            return "behind you on the " + turnDirection ;
        }else{
            return "Direction error";
        }
    }

    public String enhanceDirection(double direction, String turnDirection){
        float modDirection = (float) Math.abs(direction);

        Log.d("Direction",String.valueOf(modDirection)+" "+turnDirection);

        if(modDirection<35.49f){
            return "GO STRAIGHT";
        }else if(modDirection>=35.50f && modDirection <= 44.9f){
            return "TAKE SLIGHT " + turnDirection + " TURN";
        }else if(modDirection>=45f && modDirection<=134.9f){
            return "TAKE " + turnDirection + " TURN";
        }else if(modDirection>=135.0f && modDirection <=154.49f){
            return "TAKE HARD " + turnDirection + " TURN";
        }else if(modDirection >=155.0f && modDirection <=180.0f){
            return "TURN BACK";
        }else return "Direction error";
    }
    public String getTurnDirection(double actualTurnAngle) {
        boolean positive;
        positive = actualTurnAngle>0?true:false;

        return positive?"LEFT":"RIGHT";

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
/*
        try {
            userCurrentOrientation=updateCurrentOrientation();

        } catch (InterruptedException e) {
            e.printStackTrace();
        }

*/

        float correctedOrientation=(userCurrentOrientation-MainActivity.offset+360)%360;
        int dx =   userCurrentPosition.x - nextCoordinate.x;
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
        double dy = Math.abs( targetPosition.y - currentPosition.y);
        return Math.sqrt(dy+dx);
    }

}

class Weave{

    //private Activity callingActivity;
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {

        switch (requestCode) {
            case 0: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    //initializeUi();
                }
                return;
            }
        }
    }

    public void requestDeviceAccess() {

        Log.d("edison ", "requestDevice Access");
        AppAccessRequest request = new AppAccessRequest.Builder(
                AppAccessRequest.APP_ACCESS_ROLE_USER,
                "developmentBoard", "465320465428")
                .build();

        Response<Intent> accessResponse = com.google.android.apps.weave.framework.apis.Weave
                .APP_ACCESS_API.getRequestAccessIntent(mApiClient,
                        request);
        if (accessResponse.isSuccess()) {
            MainActivity.thisObject.startActivityForResult(accessResponse.getSuccess(), 1);
        } else if (accessResponse.getError().getErrorCode() == ResultCode.RESOLUTION_REQUIRED) {
            // This is usually when the Weave Management app is
            // not installed. Firing the resolution Intent will
            // send the user to the Weave entry in Google Play Store.
            MainActivity.thisObject.startActivityForResult(accessResponse.getError()
                    .getResolutionIntent(), 2);
        } else {
            Log.e("codelab", "Could not create RequestAccessIntent. " +
                    "Error: " + accessResponse.getError());
        }
    }

    private WeaveDevice mDevice;
    private WeaveApiClient mApiClient;
    private DeviceLoaderCallbacks mDiscoveryListener = new DeviceLoaderCallbacks() {
        @Override
        public void onDevicesFound(WeaveDevice[] weaveDevices) {
            if (weaveDevices.length > 0) {
                // For simplicity sake, use the first device found:
                mDevice = weaveDevices[0];
                Log.i("codelab", mDevice.getName() + " found");
                stopDiscovery();
                getInitialLightStates(mDevice);
            }
        }

        @Override
        public void onDevicesLost(WeaveDevice[] weaveDevices) {
            Log.i("codelab", "Lost Device(s)!");
            for (WeaveDevice device : weaveDevices) {
                if(mDevice != null && mDevice.getId().equals(device.getId())) {
                    mDevice = null;
                }
            }
        }
    };


    /**
     * Generates an Api client instance, sets up a listener to react to devices being either
     * discovered or lost.
     */

    public void initializeApiClient() {
        Log.d("Inside Edison ","Initialiaze api");
        // Initialize the actual API client
        mApiClient = new WeaveApiClient(MainActivity.thisObject);
        // In a real world app, only request device access
        // when the user requests.
        requestDeviceAccess();
    }


    /** Begins a scan for Weave-accessible devices. Searches for both cloud devices associated with
     * the user's account, and provisioned weave devices sitting on the same network.
     */
    public void startDiscovery() {
        com.google.android.apps.weave.framework.apis.Weave.DEVICE_API.startLoading(mApiClient, mDiscoveryListener);
    }

    /**
     * Stops device scan
     */
    public void stopDiscovery() {
        com.google.android.apps.weave.framework.apis.Weave.DEVICE_API.stopLoading(mApiClient, mDiscoveryListener);
    }



/** Begins a scan for Weave-accessible devices. Searches for both cloud devices associated with
 * the user's account, and provisioned weave devices sitting on the same network.
 *//*
*/
/*
    private void startDiscovery() {
        com.google.android.apps.weave.framework.apis.Weave.DEVICE_API.startLoading(mApiClient, mDiscoveryListener);
    }

    *//*
*/
    /**
     * Stops device scan
     *//*
*/
/*
    private void stopDiscovery() {
        com.google.android.apps.weave.framework.apis.Weave.DEVICE_API.stopLoading(mApiClient, mDiscoveryListener);
    }


    *//*
*/
/*@Override
    public void onResume() {
        super.onResume();
        Log.d("edison ", "onResume");
        startDiscovery();
    }

    @Override
    public void onPause() {
        Log.d("edison ", "onPause");
        stopDiscovery();
        super.onPause();
    }*//*
*/


    private void getInitialLightStates(final WeaveDevice device) {
        // Clear current switches, if any.
        // ((ViewGroup) getActivity().getView()).removeAllViews();

        Log.d("edison ", "geInitialLightStates");

        // Network call, punt off the main thread.
        new AsyncTask<Void, Void, Response<DeviceState>>() {

            @Override
            protected Response<DeviceState> doInBackground(Void... params) {
                if (device == null) {
                    return null;
                }
                return com.google.android.apps.weave.framework.apis.Weave.COMMAND_API.getState(mApiClient, device.getId());
            }

            @Override
            protected void onPostExecute(Response<DeviceState> result) {
                super.onPostExecute(result);
                if (result != null) {
                    if (!result.isSuccess() || result.getError() != null) {
                        Log.e("codelab", "Failure querying device state: " + result.getError());
                    } else {
                        Map<String, Object> state = (Map<String, Object>)
                                result.getSuccess().getStateValue("_ledflasher");
                        if (state == null) {
                            Log.e("codelab", "Device does not contain a _ledflasher state trait!");
                        } else {
                            Log.i("codelab", "Success querying device state! Populating now.");

                            ArrayList<Boolean> ledStates = (ArrayList<Boolean>) state.get("_leds");

                            // Here you'll be adding UI switches later on. For now just log the state
                            // of each LED to verify things work.
                            Log.d("edison ", "setting light state now");
                            setLightState(1,true);
                            try{
                                Thread.sleep(30000);
                            }catch(Exception e){
                                e.printStackTrace();
                            }
                            setLightState(1,false);
                        }
                    }
                }
            }
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    public Command getSetLightStateCommand(int ledIndex, boolean lightOn) {
        Log.d("edison", "getSetLightStateCommand");
        HashMap<String, Object> commandParams = new HashMap<>();
        commandParams.put("_led", ledIndex);
        commandParams.put("_on", lightOn);
        return new Command()
                .setName("_ledflasher._set")
                .setParameters(commandParams);
    }

    /**
     * Sets the state of a single LED
     * @param ledIndex The index of the LED to adjust
     * @param lightState Whether the LED should be "on" or not.
     */

    public void setLightState(final int ledIndex, final boolean lightState) {
        // Network call, punt off the main thread.

        Log.d("edison ", "setLightStates");


        new AsyncTask<Void, Void, Response<CommandResult>>() {

            @Override
            protected Response<CommandResult> doInBackground(Void... params) {
                Command command = getSetLightStateCommand(ledIndex, lightState);
                return com.google.android.apps.weave.framework.apis.Weave.COMMAND_API.execute(
                        mApiClient, mDevice.getId(), command);
            }

            @Override
            protected void onPostExecute(Response<CommandResult> result) {
                super.onPostExecute(result);
                if (result.isSuccess()) {
                    Log.i("codelab", "Success setting LED!");
                } else {
                    Log.e("codelab", "Failure setting LED: " +
                            result.getError());
                }
            }
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

}
