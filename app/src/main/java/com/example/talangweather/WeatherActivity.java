package com.example.talangweather;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.*;
import butterknife.BindView;
import butterknife.ButterKnife;
import com.example.talangweather.gson.Weather;
import com.example.talangweather.util.HttpUtil;
import com.example.talangweather.util.Utility;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

import java.io.IOException;

public class WeatherActivity extends AppCompatActivity {





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
        setContentView(R.layout.activity_weather);
        ButterKnife.bind(this);
        RelativeLayout titleView = (RelativeLayout)findViewById(R.id.title_layout);
         titleCity =(TextView)titleView.findViewById(R.id.title_city);
         titleUpdateTime=(TextView)findViewById(R.id.title_update_time);
         degreeText=(TextView)findViewById(R.id.degree_text);
         weatherInfoText=(TextView)findViewById(R.id.weather_info_text);
         comforText=(TextView)findViewById(R.id.comfort_text);
         carWashText=(TextView)findViewById(R.id.car_wash_text);
         suportText=(TextView)findViewById(R.id.sport_text);
         forecastLayout=(LinearLayout)findViewById(R.id.forecast_layout);
         weatherLayout =(ScrollView)findViewById(R.id.weather_layout);
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        String weatherString = prefs.getString("weather", null);
        if (weatherString != null){
            Weather weather = Utility.handleWeatherResponse(weatherString);
            showWeatherInfo(weather);
        }else {
            //无缓存时去服务器查询天气
            String weatherId = getIntent().getStringExtra("weather_Id");
            //可知butterknife的实例化在ncreate之后。
       //     weatherLayout.setVisibility(View.INVISIBLE);
            requestWeather(weatherId);
        }
    }

    /**
     * 根据天气id请求城市天气信息
     */
    public void requestWeather(final String weatherId){
        String weatherUrl = "https://free-api.heweather.net/s6/weather/now?location="+
                weatherId + "&key=f363bfc2ae2f4c46b77de79d1188c78c";
                HttpUtil.sendOkHttpRequest(weatherUrl, new Callback() {
                    @Override
                    public void onFailure(Call call, IOException e) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(WeatherActivity.this,"获取天气信息失败",Toast.LENGTH_SHORT).show();
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
                                    showWeatherInfo(weather);
                                }else {
                                    Toast.makeText(WeatherActivity.this,"获取天气信息失败",Toast.LENGTH_SHORT).show();
                                }
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
        String updateTime =weather.getUpdate().getLoc().split("")[1];
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
    }
}
