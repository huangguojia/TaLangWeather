package com.example.talangweather.util;


import okhttp3.OkHttpClient;
import okhttp3.Request;

public class HttpUtil {
    public static void sendOkHttpRequest(String address, okhttp3.Callback callback){
        OkHttpClient client = new OkHttpClient(); //建立访问客户端
        Request request = new Request.Builder().url(address).build();  //请求数据
        client.newCall(request).enqueue(callback);  //发送请求和调用回调函数
    }
}
