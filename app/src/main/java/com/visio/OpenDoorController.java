package com.visio;

import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.util.Log;

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

public class OpenDoorController {
    private WeaveDevice mDevice;
    private WeaveApiClient mApiClient;
    private Activity callingActivity;

    public OpenDoorController(Activity callingActivity) {
        this.callingActivity = callingActivity;
    }

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
    private void getInitialLightStates(final WeaveDevice device) {
        // Clear current switches, if any.
        // ((ViewGroup) getView()).removeAllViews();

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
                            /*for (int i = 0; i < ledStates.size(); i++) {
                                addSwitch(i, ledStates.get(i));
                            }*/
                           // addSwitch(0, ledStates.get(0));
                        }
                    }
                }
            }
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }


    /* Creates a Weave command for adjusting a single LED.
     * @param ledIndex The index of the LED to adjust.
     * @param lightOn Whether the LED should be on or not.
     * @return an executable weave command to toggle the LED to the desired state.
     */
    public Command getSetLightStateCommand(int ledIndex, boolean lightOn) {
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

    /** Begins a scan for Weave-accessible devices. Searches for both cloud devices associated with
    * the user's account, and provisioned weave devices sitting on the same network.
            */
    public void startDiscovery() {
        Weave.DEVICE_API.startLoading(mApiClient, mDiscoveryListener);
    }

    /**
     * Stops device scan
     */
    public void stopDiscovery() {
        Weave.DEVICE_API.stopLoading(mApiClient, mDiscoveryListener);
    }

    /**
     * Generates an Api client instance, sets up a listener to react to devices being either
     * discovered or lost.
     */
    private void initializeApiClient() {
        // Initialize the actual API client
        Log.i("codelab", "inside initializeapiclient");

        mApiClient = new WeaveApiClient(callingActivity);
        // In a real world app, only request device access
        // when the user requests.
        requestDeviceAccess();
    }

    public void requestDeviceAccess() {
        Log.i("codelab", "inside requestdevice access");

        AppAccessRequest request = new AppAccessRequest.Builder(
                AppAccessRequest.APP_ACCESS_ROLE_USER,
                "developmentBoard", "465320465428")
                .build();

        Response<Intent> accessResponse = Weave
                .APP_ACCESS_API.getRequestAccessIntent(mApiClient,
                        request);
        if (accessResponse.isSuccess()) {
            callingActivity.startActivityForResult(accessResponse.getSuccess(), 1);
        } else if (accessResponse.getError().getErrorCode() == ResultCode.RESOLUTION_REQUIRED) {
            // This is usually when the Weave Management app is
            // not installed. Firing the resolution Intent will
            // send the user to the Weave entry in Google Play Store.
            callingActivity.startActivityForResult(accessResponse.getError()
                    .getResolutionIntent(), 2);
        } else {
            Log.e("codelab", "Could not create RequestAccessIntent. " +
                    "Error: " + accessResponse.getError());
        }
    }

}
