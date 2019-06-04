package com.example.talangweather;

import android.support.v4.app.Fragment;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.*;
import butterknife.BindView;
import butterknife.ButterKnife;
import com.bumptech.glide.Glide;
import com.example.talangweather.gson.Weather;
import com.example.talangweather.service.AutoUpdateService;
import com.example.talangweather.util.HttpUtil;
import com.example.talangweather.util.StatusBarUtil;
import com.example.talangweather.util.Utility;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

import java.io.IOException;

public class WeatherActivity extends AppCompatActivity {
    private RelativeLayout titleLayout;
    private  LinearLayout weather_linearlayout;
    public DrawerLayout drawerLayout;
    private Button navButton;
    private String mWeatherId;
    public SwipeRefreshLayout swipeRefreshLayout;
    private ImageView bingPicImg;
    private ScrollView weatherLayout;
    private TextView titleCity;
    private TextView titleUpdateTime;
    private TextView degreeText;
    private TextView weatherInfoText;
    private LinearLayout forecastLayout;
    private TextView comforText;
    private TextView carWashText;
    private TextView suportText;
    @Override
    protected  void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if(Build.VERSION.SDK_INT >= 21){ //只有当版本号大于或等于21时才会执行
            View decorView = getWindow().getDecorView();
            decorView.setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
            getWindow().setStatusBarColor(Color.TRANSPARENT);
        }
        setContentView(R.layout.activity_weather);
        ButterKnife.bind(this);
        drawerLayout = (DrawerLayout)findViewById(R.id.drawyer_layout);
        navButton = (Button)findViewById(R.id.nav_button);
        RelativeLayout titleView = (RelativeLayout) findViewById(R.id.title_layout);
        swipeRefreshLayout = (SwipeRefreshLayout)findViewById(R.id.swiperefresh_layout);
        titleCity = (TextView) titleView.findViewById(R.id.title_city);
        titleUpdateTime = (TextView) findViewById(R.id.title_update_time);
        degreeText = (TextView) findViewById(R.id.degree_text);
        weatherInfoText = (TextView) findViewById(R.id.weather_info_text);
        comforText = (TextView) findViewById(R.id.comfort_text);
        carWashText = (TextView) findViewById(R.id.car_wash_text);
        suportText = (TextView) findViewById(R.id.sport_text);
        forecastLayout = (LinearLayout) findViewById(R.id.forecast_layout);
        weatherLayout = (ScrollView) findViewById(R.id.weather_layout);
        bingPicImg = (ImageView) findViewById(R.id.bing_pic_img);
        weather_linearlayout = (LinearLayout)findViewById(R.id.weather_linearlyaout);
        titleLayout = (RelativeLayout)findViewById(R.id.title_layout) ;

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        String bingPic = prefs.getString("bing_pic", null);
        if (bingPic != null) {
            Glide.with(this).load(bingPic).into(bingPicImg);
        } else {
            loadBingPic();
        }
        navButton.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view){
                drawerLayout.openDrawer(GravityCompat.START);
            }
        });
        String weatherString = prefs.getString("weather", null);
        if (weatherString != null){
            Weather weather = Utility.handleWeatherResponse(weatherString);
            mWeatherId = weather.getHeWeather6().get(0).getBasic().getCid();
            showWeatherInfo(weather);
        }else {
            //无缓存时去服务器查询天气
            mWeatherId = getIntent().getStringExtra("weather_Id");
            weatherLayout.setVisibility(View.INVISIBLE);
            requestWeather(mWeatherId);
        }
        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                Log.d("sendoh","进入");
                requestWeather(mWeatherId);
            }
        });

        }
    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        //设置第一个view距离状态栏的高度；
        LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) titleLayout.getLayoutParams();
        params.setMargins(0,StatusBarUtil.getStatusBarHeight(this),0,0);
        titleLayout.setLayoutParams(params);
    }

    /**
     * 根据天气id请求城市天气信息
     */
    public void requestWeather(final String weatherId){
        String weatherUrl = "https://free-api.heweather.net/s6/weather/now?location="+
                weatherId + "&key=d65273a0eff043b5b600ff91a475adcd";
                HttpUtil.sendOkHttpRequest(weatherUrl, new Callback() {
                    @Override
                    public void onFailure(Call call, IOException e) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(WeatherActivity.this,"获取天气信息失败",Toast.LENGTH_SHORT).show();
                                swipeRefreshLayout.setRefreshing(false);
                            }
                        });
                    }

                    @Override
                    public void onResponse(Call call, Response response) throws IOException {
                        final String responseText = response.body().string();
                        final Weather weather = Utility.handleWeatherResponse(responseText);
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                if (weather != null && "ok".equals(weather.getHeWeather6().get(0).getStatus())){
                                    SharedPreferences.Editor editor = PreferenceManager.
                                            getDefaultSharedPreferences(WeatherActivity.this).
                                            edit();
                                    editor.putString("weather", responseText);
                                    editor.apply();
                                    mWeatherId = weather.getHeWeather6().get(0).getBasic().getCid();
                                    showWeatherInfo(weather);
                                }else {
                                    Toast.makeText(WeatherActivity.this,"获取天气信息失败",Toast.LENGTH_SHORT).show();
                                }
                                swipeRefreshLayout.setRefreshing(false);
                            }
                        });
                    }
                });
    }

    /**
     * 处理并展示Weather实体类中的数据
     */
    private void showWeatherInfo(Weather allWeather){
        Weather.HeWeather6Bean weather = allWeather.getHeWeather6().get(0);
        String cityName = weather.getBasic().getLocation();
        String updateTime =weather.getUpdate().getLoc().split(" ")[1];
        String degree = weather.getNow().getTmp()+"度";
        String weatherInfo = weather.getNow().getCond_txt();
        titleCity.setText(cityName);
        titleUpdateTime.setText(updateTime);
        degreeText.setText(degree);
        weatherInfoText.setText(weatherInfo);
        forecastLayout.removeAllViews();
        if (weather.getDaily_forecast()!=null) {
            for (Weather.HeWeather6Bean.DailyForecastBean foreCast : weather.getDaily_forecast()) {
                View view = LayoutInflater.from(this).inflate(R.layout.forecast_item, forecastLayout, false);
                TextView dateText = (TextView) view.findViewById(R.id.date_text);
                TextView infoText = (TextView) view.findViewById(R.id.info_text);
                TextView maxText = (TextView) view.findViewById(R.id.max_text);
                TextView minText = (TextView) view.findViewById(R.id.min_text);
                dateText.setText(foreCast.getDate());
                infoText.setText(foreCast.getCond_txt_d());
                maxText.setText(foreCast.getTmp_max());
                minText.setText(foreCast.getTmp_min());
                forecastLayout.addView(view);
            }
        }
//        if (weather.aqi != null){
//            aqiText.setText(weather.aqi.city.aqi);
//            aqiText.setText(weather.aqi.city.pm25);
//        }
        if (weather.getLifestyle()!=null) {
            String comfort = "舒适度" + weather.getLifestyle().get(0).getTxt();
            String carWash = "洗车指数" + weather.getLifestyle().get(1).getTxt();
            String suport = "运动建议" + weather.getLifestyle().get(3).getTxt();
            comforText.setText(comfort);
            carWashText.setText(carWash);
            suportText.setText(suport);
        }
        weatherLayout.setVisibility(View.VISIBLE);
        Intent intent = new Intent(this, AutoUpdateService.class);
        startService(intent);
    }



    @Override
    public void onBackPressed() {
        super.onBackPressed();
//        Intent intent = new Intent(WeatherActivity.this, MyMainActivity.class);
//        startActivity(intent);
    }

    private void loadBingPic(){
        String requestBingPic = "http://guolin.tech/api/bing_pic";
        HttpUtil.sendOkHttpRequest(requestBingPic, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                final String bingPic = response.body().string();
                SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(WeatherActivity.this).edit();
                editor.putString("bing_pic",bingPic);
                editor.apply();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Glide.with(WeatherActivity.this).load(bingPic).into(bingPicImg);
                    }
                });
            }
        });
    }


    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {

        if(keyCode == KeyEvent.KEYCODE_BACK ) {
            Fragment current = getSupportFragmentManager().findFragmentById(R.id.choose_area_by_drawer_fragment);
            if(current.isVisible()){
                //当左边的菜单栏是可见的，则关闭
                drawerLayout.closeDrawers();
                Log.d("ss","隐藏滑动菜单");
            } else {
                finish();
            }
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }
}
