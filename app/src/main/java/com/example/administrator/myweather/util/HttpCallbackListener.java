package com.example.administrator.myweather.util;

/**
 * Created by Administrator on 2016/4/10.
 */
public interface HttpCallbackListener {
    void onFinish(String response);
    void onError(Exception e);
}
