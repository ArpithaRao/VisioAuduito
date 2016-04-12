package com.visio;

import android.app.Activity;
import android.content.Intent;
import android.content.IntentSender;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.nearby.Nearby;
import com.google.android.gms.nearby.messages.Message;
import com.google.android.gms.nearby.messages.MessageListener;
import com.google.android.gms.nearby.messages.Strategy;
import com.google.android.gms.nearby.messages.SubscribeCallback;
import com.google.android.gms.nearby.messages.SubscribeOptions;

public class LauncherActivity
        extends AppCompatActivity implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {
    public static final String TAG = MainActivity.class.getSimpleName();
    private LauncherActivity object;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        object = this;
        setContentView(R.layout.activity_main);
        Toolbar myToolbar = (Toolbar) findViewById(R.id.application_toolbar);
        setSupportActionBar(myToolbar);

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Nearby.MESSAGES_API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this).build();
        mMessageListener = new MessageListener() {
            @Override
            public void onFound(Message message) {
                //Nearby.Messages.unsubscribe(mGoogleApiClient,mMessageListener);
                Intent mainIntentLauncher = new Intent(object, com.visio.MainActivity.class);
                mainIntentLauncher.putExtra(com.visio.MainActivity.extraName,new String(message.getContent()));
                mainIntentLauncher.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(mainIntentLauncher);
            }
        };
        //messageSubscribe();

        ///Intent intent = new Intent(this,SettingsActivity.class);
        //startActivity(intent);


    }

    private void messageSubscribe(){
        Log.d(TAG,"Trying to subscribe");
        if(!mGoogleApiClient.isConnected()){
            if(!mGoogleApiClient.isConnecting()){
                mGoogleApiClient.connect();
            }
        }else{
            SubscribeOptions localOptions = new SubscribeOptions.Builder()
                    .setStrategy(Strategy.BLE_ONLY)
                    .setCallback(new SubscribeCallback() {
                        @Override
                        public void onExpired() {
                            Log.d(TAG,"No longer subscribed");
                        }
                    }).build();

            Nearby.Messages.subscribe(mGoogleApiClient,mMessageListener,localOptions)
                    .setResultCallback(new ResultCallback<Status>() {
                        @Override
                        public void onResult(@NonNull Status status) {
                            if(status.isSuccess()){
                                Log.d(TAG,"Subscribed");
                            }else{
                                Log.d(TAG,"Subscription Failed");
                                handleFailure(status);
                            }


                        }
                    });
        }

    }

    private void handleFailure(Status status){
        if(mResolvingIssue){
            Log.d(TAG,"Already resolving issue");
        }else if(status.hasResolution()){
            try{
                mResolvingIssue = true;
                status.startResolutionForResult(this,1);
            }catch (IntentSender.SendIntentException e){
                mResolvingIssue = false;
                Log.i(TAG, "Failed to resolve error status.", e);
            }
        }
    }



    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1) {
            // User was presented with the Nearby opt-in dialog and pressed "Allow".
            mResolvingIssue = false;
            if (resultCode == Activity.RESULT_OK) {
                // Execute the pending subscription and publication tasks here.
                this.messageSubscribe();
            } else if (resultCode == Activity.RESULT_CANCELED) {
                // User declined to opt-in. Reset application state here.
            } else {
                Toast.makeText(this, "Failed to resolve error with code " + resultCode,
                        Toast.LENGTH_LONG).show();
            }
        }
    }


    private GoogleApiClient mGoogleApiClient;

    private boolean         mResolvingIssue = false;
    private MessageListener mMessageListener;
    @Override
    public void onConnected(@Nullable Bundle bundle) {
        Log.d(TAG,"Trying to connect");
        messageSubscribe();
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Log.d(TAG,"Connection Failed");
    }

   
}
