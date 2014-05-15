package com.scurab.android.anuitor.sample;

import android.app.Activity;
import android.os.Bundle;

/**
 * Created by jbruchanov on 15.5.14.
 */
public class StartActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
    }

    @Override
    protected void onResume() {
        super.onResume();
        ((SampleApplication) getApplication()).setCurrentActivity(this);
    }

    @Override
    protected void onPause() {
//        ((SampleApplication) getApplication()).setCurrentActivity(null);
        super.onPause();
    }
}
