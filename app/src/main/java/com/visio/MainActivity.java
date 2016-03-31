package com.visio;

import android.app.Fragment;
import android.content.Context;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.customlbs.library.Indoors;
import com.customlbs.library.IndoorsException;
import com.customlbs.library.IndoorsFactory;
import com.customlbs.library.IndoorsLocationListener;
import com.customlbs.library.LocalizationParameters;
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
import com.customlbs.surface.library.UserInteractionListenerImpl;

import java.io.Console;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.EventListener;
import java.util.List;
import java.util.PriorityQueue;

import static android.widget.Toast.LENGTH_LONG;

class BuildingAdapter extends ArrayAdapter<Building>{

    static LayoutInflater inflater;
    Building[] data;

    public BuildingAdapter(Context context, int simple_list_item_1, Building[] listViewArray) {
        super(context,simple_list_item_1,listViewArray);
        this.data=listViewArray;
        inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        View view = convertView;
        if(view==null){
            view = inflater.inflate(R.layout.building_list_item,parent,false);
        }

        view.setTag(R.string.tag,data[position]);
        TextView text=(TextView)view.findViewById(R.id.text);
        text.setText(data[position].getName());

        return view;
    }

};




public class MainActivity extends FragmentActivity implements  IndoorsServiceCallback, IndoorsLocationListener {

    public ZoneCallback zonecallback;
    public static OnlineBuildingCallback onlinebuildingcallback;
    public static IndoorsSurfaceFragment indoorsFragment;
    public static IndoorsFactory.Builder indoorsBuilder;
    public static ArrayList<Building> buildingList;
    public static ArrayList<Zone> zonelist;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        indoorsBuilder = new IndoorsFactory.Builder();
        indoorsBuilder.setContext(this);
        indoorsBuilder.setPassiveServiceCallback(this);
        indoorsBuilder.setApiKey("34420529-47cf-4e4f-a3b6-79f1e0948aab");

        IndoorsSurfaceFactory.Builder indoorsSurface = new IndoorsSurfaceFactory.Builder();
        indoorsSurface.setIndoorsBuilder(indoorsBuilder);
        indoorsFragment = indoorsSurface.build();






        zonecallback=new ZoneCallback() {
            @Override
            public void setZones(ArrayList<Zone> arrayList) {

                Log.d("Zone",arrayList.toString());
            }
        };

        MainActivity.onlinebuildingcallback = new OnlineBuildingCallback() {
            @Override
            public void setOnlineBuildings(ArrayList<Building> arrayList) {
                Building[] listViewArray= new Building[arrayList.size()];

                MainActivity.buildingList =  arrayList;

                ListView listView = (ListView)findViewById(R.id.listView);
                int i=0;

                for(Building building:buildingList){
                    listViewArray[i]= building;
                    i++;
                }

                //listViewArray= (String[]) buildingList.toArray();

                listView.setAdapter(new BuildingAdapter(MainActivity.this, android.R.layout.simple_list_item_1, listViewArray));

                listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                        final Indoors temp = MainActivity.indoorsFragment.getIndoors();
                        Building building =(Building)view.getTag(R.string.tag);
                        Log.d("building",building.toString());
                        temp.getBuilding(building, new LoadingBuildingCallback() {
                            @Override
                            public void loadingBuilding(LoadingBuildingStatus loadingBuildingStatus) {

                            }

                            @Override
                            public void buildingLoaded(Building building) {
                                Log.d("Building","Loaded");
                                temp.getZones(building, new ZoneCallback() {
                                    @Override
                                    public void setZones(ArrayList<Zone> arrayList) {
                                        Log.d("this", "find");
                                    }
                                });
                            }

                            @Override
                            public void buildingLoadingCanceled() {

                            }

                            @Override
                            public void onError(IndoorsException e) {
                                e.printStackTrace();
                            }
                        });
                    }
                });

            }
        };

    }

    @Override
    public void connected() {
        Indoors localIndoors=MainActivity.indoorsFragment.getIndoors();

        localIndoors.getOnlineBuildings(onlinebuildingcallback);






        //Log.d("MyActivity",);


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
};