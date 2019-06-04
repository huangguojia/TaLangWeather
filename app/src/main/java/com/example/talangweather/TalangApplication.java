package com.example.talangweather;

import android.app.Application;
import interfaces.heweather.com.interfacesmodule.view.HeConfig;

public class TalangApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        initWhetherCount();
    }

    private void initWhetherCount(){
        try {
            HeConfig.init("HE1906041427301375", "ca7276010b49498c83f357432bd07577");
        }catch (Exception e){
            e.printStackTrace();
        }
    }
}
