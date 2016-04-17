package com.visio;

import android.app.Activity;

import com.firebase.client.DataSnapshot;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;
import com.firebase.client.ValueEventListener;

import java.util.List;

class FirebaseZoneInfo{
    Activity callingActivity;
    com.visio.Zones valuesFromFirebase;
    public FirebaseZoneInfo(Activity callingActivity) {
        this.callingActivity = callingActivity;
        Firebase.setAndroidContext(callingActivity);
    }
    public void initZoneInfo(){
        Firebase localFirebaseReference = new Firebase("https://visioaduito.firebaseio.com/");
        localFirebaseReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                valuesFromFirebase = dataSnapshot.getValue(Zones.class);
                MainActivity.zoneProperties = getAllZones();

            }

            @Override
            public void onCancelled(FirebaseError firebaseError) {

            }
        });

    }


    public List<com.visio.Zone> getAllZones(){
        return valuesFromFirebase.getZones();
    }

}