package com.example.talangweather.util;

import android.text.TextUtils;
import com.example.talangweather.db.City;
import com.example.talangweather.db.County;
import com.example.talangweather.db.Province;
import com.example.talangweather.gson.CitySearch;
import com.example.talangweather.gson.Weather;
import com.google.gson.Gson;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class Utility {
    /**
     * 解析和处理服务器返回的省级数据
     */
    public static boolean handleProvinceResponse(String response) {
        if(!TextUtils.isEmpty(response)){
            try{
                JSONArray allProvinces = new JSONArray(response);
                for (int i = 0; i <allProvinces.length() ; i++) {
                    JSONObject provinceObject = allProvinces.getJSONObject(i);
                    Province province = new Province();
                    province.setId(provinceObject.getInt("id"));
                    province.setProvinceName(provinceObject.getString("name"));
                    province.setProvinceCode(provinceObject.getInt("id"));
                    province.save();
                }
                return true;
            }catch (JSONException e){
                e.printStackTrace();
            }
         }
        return false;
    }
    /**
     * 解析和处理服务器返回的市级数据
     */
    public static boolean handleCityResponse(String response, int provinceId) {
        if(!TextUtils.isEmpty(response)){
            try{
                JSONArray allCities = new JSONArray(response);
                for (int i = 0; i <allCities.length() ; i++) {
                    JSONObject cityObject = allCities.getJSONObject(i);
                    City city = new City();
                    city.setId(cityObject.getInt("id"));
                    city.setCityName(cityObject.getString("name"));
                    city.setCityCode(cityObject.getInt("id"));
                    city.setProvinceId(provinceId);
                    city.save();
                }
                return true;
            }catch (JSONException e){
                e.printStackTrace();
            }
        }
        return false;
    }
    /**
     * 解析和处理服务器返回的省级数据
     */
    public static boolean handleCountyResponse(String response, int cityId) {
        if(!TextUtils.isEmpty(response)){
            try{
                JSONArray allCounties = new JSONArray(response);
                for (int i = 0; i <allCounties.length() ; i++) {
                    JSONObject countyObject = allCounties.getJSONObject(i);
                    County county = new County();
                    county.setCountyName(countyObject.getString("name"));
                    county.setWeatherId(countyObject.getString("weather_id"));
                    county.setCityId(cityId);
                    county.save();
                }
                return true;
            }catch (JSONException e){
                e.printStackTrace();
            }
        }
        return false;
    }

    /**
     * 将返回的JSON数据解析成Weather实体类
     */
    public static Weather handleWeatherResponse(String response){
        try{
       //     String result = JSONTokener(response);
//            JSONObject jsonObject = new JSONObject(response);
//            JSONArray jsonArray = jsonObject.getJSONArray("HeWeather");
//            String weatherContent = jsonArray.getJSONArray(0).toString();
//            return new Gson().fromJson(weatherContent, Weather.class);
            Gson gson = new Gson();
            Weather weather = gson.fromJson(response, Weather.class);
            return  weather;
        }catch (Exception e){
            e.printStackTrace();
        }
        return null;
    }

    public static CitySearch handleCityResponse(String response){
        try{
            Gson gson = new Gson();
            CitySearch Cities = gson.fromJson(response, CitySearch.class);
            return  Cities;
        }catch (Exception e){
            e.printStackTrace();
        }
        return null;
    }


    public static String JSONTokener(String str_json) {
        // consume an optional byte order mark (BOM) if it exists
        if (str_json != null && str_json.startsWith("\ufeff")) {
            str_json = str_json.substring(1);
        }
        return str_json;
    }

}
