package com.example.administrator.myweather.db;

import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * Created by Administrator on 2016/4/10.
 */
public class MyWeatherOpenHelper extends SQLiteOpenHelper{
    public static final String CREATE_PROVICE="create table Provice("
            +"id integer primary key autoincrement,"
            +"province_name text,"
            +"provice_code text)";

    @Override
    public void onCreate(SQLiteDatabase db) {

    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    }
}
