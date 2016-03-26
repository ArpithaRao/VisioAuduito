package com.visio;

import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import com.customlbs.library.IndoorsFactory;
import com.customlbs.surface.library.IndoorsSurfaceFactory;
import com.customlbs.surface.library.IndoorsSurfaceFragment;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        IndoorsFactory.Builder indoorsBuilder = new IndoorsFactory.Builder();
        IndoorsSurfaceFactory.Builder surfaceBuilder = new IndoorsSurfaceFactory.Builder();
        indoorsBuilder.setContext(this);

        indoorsBuilder.setApiKey("34420529-47cf-4e4f-a3b6- 79f 1e0948 aab");
        indoorsBuilder.setBuildingId((long) 680800560);
        surfaceBuilder.setIndoorsBuilder(indoorsBuilder);

        IndoorsSurfaceFragment indoorsFragment = surfaceBuilder.build();

        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.add(android.R.id.content, indoorsFragment, "indoors");
        transaction.commit();
    }
}
