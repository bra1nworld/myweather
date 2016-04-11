package com.example.administrator.myweather.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.text.TextUtils;

import com.example.administrator.myweather.model.City;
import com.example.administrator.myweather.model.County;
import com.example.administrator.myweather.model.MyWeatherDB;
import com.example.administrator.myweather.model.Province;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.logging.SimpleFormatter;

/**
 * Created by Administrator on 2016/4/10.
 */
public class Utility {
    public synchronized static boolean handleProvincesResponse(MyWeatherDB myWeatherDB,String response){
        //解析和处理服务器返回的省级数据
        if(!TextUtils.isEmpty(response)){
            String[] allProvices=response.split(",");
            if(allProvices!=null&&allProvices.length>0){
                for (String p:allProvices){
                    String[] array=p.split("\\|");//这里是按|分割而不是\\|分割，\\是辅助符号
                    //查询笔记————————————————————
                    Province province=new Province();
                    province.setProvinceCode(array[0]);
                    province.setProvinceName(array[1]);
                    myWeatherDB.saveProvince(province);//将解析出来的数据存储到Province表
                }
                return false;
            }
        }
        return false;
    }
    public static boolean handleCitiesResponse(MyWeatherDB myWeatherDB,String response,int provinceId){
        if(!TextUtils.isEmpty(response)){
            String[] allCities=response.split(",");
            if(allCities!=null&&allCities.length>0){
                for (String c:allCities){
                    String[] array=c.split("\\|");//这里是按|分割而不是\\|分割，\\是辅助符号
                    City city=new City();
                    city.setCityCode(array[0]);
                    city.setCityName(array[1]);
                    city.setProvinceId(provinceId);
                    myWeatherDB.saveCity(city);
                }
                return false;
            }
        }
        return false;
    }
    public static boolean handleCountiesResponse(MyWeatherDB myWeatherDB,String responce,int cityId){
        if(!TextUtils.isEmpty(responce)){
            String[] allCounties=responce.split(",");
            if(allCounties!=null&&allCounties.length>0){
                for (String c:allCounties){
                    String[] array=c.split("\\|");//这里是按|分割而不是\\|分割，\\是辅助符号
                    County county=new County();
                    county.setCountyCode(array[0]);
                    county.setCountyName(array[1]);
                    county.setCityId(cityId);
                    myWeatherDB.saveCounty(county);
                }
                return false;
            }
        }
        return false;
    }
    public static void handleWeatherResponse(Context context,String response){
        try{
            JSONObject jsonObject=new JSONObject(response);
            JSONObject weatherInfo=jsonObject.getJSONObject("weatherinfo");
            String cityName=weatherInfo.getString("city");
            String weatherCode=weatherInfo.getString("cityId");
            String temp1=weatherInfo.getString("temp1");
            String temp2=weatherInfo.getString("temp2");
            String weatherDesp=weatherInfo.getString("weather");
            String publishTime=weatherInfo.getString("ptime");
            saveWeatherInfo(context,cityName,weatherCode,temp1,temp2,weatherDesp,publishTime);
        }catch(JSONException e){
            e.printStackTrace();
        }
    }
    public static void saveWeatherInfo(Context context,String cityName,String weatherCode,String temp1,String temp2,String weatherDesp,String publishTime){
        SimpleDateFormat sdf=new SimpleDateFormat("yyyy年M月d日",Locale.CHINA);
        SharedPreferences.Editor editor= PreferenceManager.getDefaultSharedPreferences(context).edit();
        //将JSON解析的数据存储到SharedPreferences中
        editor.putBoolean("city_selected",true);
        editor.putString("city_name", cityName);
        editor.putString("weather_code", weatherCode);
        editor.putString("temp1", temp1);
        editor.putString("temp2", temp2);
        editor.putString("weather_desp", weatherDesp);
        editor.putString("publish_time", publishTime);
        editor.putString("current_date",sdf.format(new Date()));
        editor.commit();
    }
}
