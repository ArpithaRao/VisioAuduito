package com.visio;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.apps.weave.apis.appaccess.AppAccessRequest;
import com.google.android.apps.weave.apis.data.Command;
import com.google.android.apps.weave.apis.data.CommandResult;
import com.google.android.apps.weave.apis.data.DeviceState;
import com.google.android.apps.weave.apis.data.WeaveApiClient;
import com.google.android.apps.weave.apis.data.WeaveDevice;
import com.google.android.apps.weave.apis.data.responses.Response;
import com.google.android.apps.weave.apis.data.responses.ResultCode;
import com.google.android.apps.weave.apis.device.DeviceLoaderCallbacks;
import com.google.android.apps.weave.framework.apis.Weave;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class EdisonLauncher extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Log.d("Inside Edison","oncreate");
        /*if (hasRuntimePermissions()) {
            //initializeUi();
        } else {
            getRuntimePermissions();
        }
*/


       initializeApiClient();
    }
//    public void initializeUi() {
//        setContentView(R.layout.activity_edison_launcher);
//        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
//        setSupportActionBar(toolbar);
//    }


    @Override
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

    public void getRuntimePermissions() {
        Log.d("Inside Edison","get permission");

        String [] permissionIds = {
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.READ_CONTACTS
        };
        ActivityCompat.requestPermissions(this, permissionIds, 0);
    }

    public boolean hasRuntimePermissions() {
        Log.d("Inside Edison","request permission");

        return ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this,
                        Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this,
                        Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED;
    }

    /*@Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }*/

   /* @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
*/

//    @Override
//    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
//        Log.d("Inside Edison Fragment","On create View");
//
//        initializeApiClient();
//        return inflater.inflate(R.layout.activity_main, container, false);
//    }

    /*public void requestDeviceAccess() {

        Log.d("edison ", "requestDevice Access");
        AppAccessRequest request = new AppAccessRequest.Builder(
                AppAccessRequest.APP_ACCESS_ROLE_USER,
                "developmentBoard", "465320465428")
                .build();

        Response<Intent> accessResponse = Weave
                .APP_ACCESS_API.getRequestAccessIntent(mApiClient,
                        request);
       *//* if (accessResponse.isSuccess()) {
            startActivityForResult(accessResponse.getSuccess(), 1);
        } else if (accessResponse.getError().getErrorCode() == ResultCode.RESOLUTION_REQUIRED) {
            // This is usually when the Weave Management app is
            // not installed. Firing the resolution Intent will
            // send the user to the Weave entry in Google Play Store.
            startActivityForResult(accessResponse.getError()
                    .getResolutionIntent(), 2);
        } else {
            Log.e("codelab", "Could not create RequestAccessIntent. " +
                    "Error: " + accessResponse.getError());
        }*//*
    }*/

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
    private void initializeApiClient() {
        Log.d("Inside Edison ","Initialiaze api");
        // Initialize the actual API client
        mApiClient = new WeaveApiClient(this);
        // In a real world app, only request device access
        // when the user requests.
       // requestDeviceAccess();
    }

    /** Begins a scan for Weave-accessible devices. Searches for both cloud devices associated with
     * the user's account, and provisioned weave devices sitting on the same network.
     */
    private void startDiscovery() {
        Weave.DEVICE_API.startLoading(mApiClient, mDiscoveryListener);
    }

    /**
     * Stops device scan
     */
    private void stopDiscovery() {
        Weave.DEVICE_API.stopLoading(mApiClient, mDiscoveryListener);
    }


    @Override
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
    }

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
                return Weave.COMMAND_API.getState(mApiClient, device.getId());
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
                        }
                    }
                }
            }
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

//    private void addSwitch(final int index, boolean initialValue) {
//        final int ledIndex = index + 1;
//        Log.i("codelab", "LED " + ledIndex + " state: " + initialValue);
//        Switch ledSwitch = new Switch(getActivity());
//        ledSwitch.setChecked(initialValue);
//        ledSwitch.setText("LED " + ledIndex);
//        ((ViewGroup)getView()).addView(ledSwitch);
//
//        ledSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
//            @Override
//            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
//                setLightState(ledIndex, isChecked);
//            }
//        });
//    }

    /**
     * Creates a Weave command for adjusting a single LED.
     * @param ledIndex The index of the LED to adjust.
     * @param lightOn Whether the LED should be on or not.
     * @return an executable weave command to toggle the LED to the desired state.
     */
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
                return Weave.COMMAND_API.execute(
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
