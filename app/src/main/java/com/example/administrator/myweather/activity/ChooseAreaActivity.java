package com.example.administrator.myweather.activity;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.administrator.myweather.R;
import com.example.administrator.myweather.model.City;
import com.example.administrator.myweather.model.County;
import com.example.administrator.myweather.model.MyWeatherDB;
import com.example.administrator.myweather.model.Province;
import com.example.administrator.myweather.util.HttpCallbackListener;
import com.example.administrator.myweather.util.HttpUtil;
import com.example.administrator.myweather.util.Utility;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Administrator on 2016/4/10.
 */
public class ChooseAreaActivity extends Activity{
    public static final int LEVEL_PROVINCE=0;
    public static final int LEVEL_CITY=1;
    public static final int LEVEL_COUNTY=2;
    private ProgressDialog progressDialog;
    private TextView titleText;
    private ListView listView;
    private ArrayAdapter<String> adapter;
    private MyWeatherDB mMyWeatherDB;
    private List<String> dataList=new ArrayList<String>();
    private List<Province> provinceList;//省列表
    private List<City> cityList;//市列表
    private List<County> countyList;//县列表
    private Province selectedProvincce;//选中的省
    private City selectedCity;//选中的市
   // private County selectedCounty;//选中的县,————————————不需要选中县这个对象，因为上面创建选中的对象是为了引出下一级的
    private int currentLevel;
    private boolean isFromWeatherActivity;//是否从WeatherActivity中跳转过来
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        isFromWeatherActivity=getIntent().getBooleanExtra("from_weather_activity",false);
        super.onCreate(savedInstanceState);
        SharedPreferences prefs= PreferenceManager.getDefaultSharedPreferences(this);
        if(prefs.getBoolean("city_selected",false)&&isFromWeatherActivity){
            //这个判断就是当重新选择城市的时候所满足的条件
        //读取city_selected标注为，
        // 如果是true就说明当前已经选择过城市了，直接跳转到WeatherActivity
            //&&表示：已经选择了城市且不是从WeatherActivity跳转过来的，才会直接跳到WeatherActivity
            //声明这个是因为重新选择城市的时候会跳转过来，所以又新增加了一个判断对象来区别
            Intent intent=new Intent(this,WeatherActivity.class);
            startActivity(intent);
            finish();
            return;
        }
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.choose_area);
        listView=(ListView)findViewById(R.id.list_view);
        titleText=(TextView)findViewById(R.id.title_text);
        adapter=new ArrayAdapter<String>(this,android.R.layout.simple_list_item_1,dataList);
        listView.setAdapter(adapter);
        mMyWeatherDB=MyWeatherDB.getInstance(this);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int index, long id) {
                //四个参数分别为：arg0=parent, arg1=view, arg2=position, arg3=id
               // parent是父视图，view为当前视图，position是当前视图在adpter中位置，id是当前视图View的ID.
                //position值一般是和list中位置的值是对应的，只要获取list中该position上的值就可以跳转下一个activity
                if(currentLevel==LEVEL_PROVINCE){
                    selectedProvincce=provinceList.get(index);//该行即为选中省份的操作，同时获得位置
                    queryCities();//加载数据
                }else if(currentLevel==LEVEL_CITY){
                    selectedCity=cityList.get(index);
                    queryCounties();
                }else if(currentLevel==LEVEL_COUNTY){
                    String countyCode=countyList.get(index).getCountyCode();
                    Intent intent=new Intent(ChooseAreaActivity.this,WeatherActivity.class);
                    intent.putExtra("county_code",countyCode);
                    startActivity(intent);
                    finish();
                }
            }
        });
        queryProvinces();
    }
    private void queryProvinces(){//查询全国所有的省，优先从数据库查询，如果没有查询到再去服务器上查询
        provinceList=mMyWeatherDB.loadProvinces();
        if(provinceList.size()>0){
            dataList.clear();
            for(Province province:provinceList){
                dataList.add(province.getProvinceName());
            }
            adapter.notifyDataSetChanged();
            listView.setSelection(0);
            titleText.setText("中国");
            currentLevel=LEVEL_PROVINCE;
        }else {
            queryFromServer(null, "province");
        }
    }
    private void queryCities() {//查询所有的市，优先从数据库查询，如果没有查询到再去服务器上查询
        cityList = mMyWeatherDB.loadCities(selectedProvincce.getId());
        if (cityList.size() > 0) {
            dataList.clear();
            for (City city : cityList) {
                dataList.add(city.getCityName());
            }
            adapter.notifyDataSetChanged();
            listView.setSelection(0);
            titleText.setText(selectedProvincce.getProvinceName());
            currentLevel = LEVEL_CITY;
        } else {
            queryFromServer(selectedProvincce.getProvinceCode(), "city");
        }
    }
    private void queryCounties(){//查询所有的县，优先从数据库查询，如果没有查询到再去服务器上查询
        countyList=mMyWeatherDB.loadCounties(selectedCity.getId());
        if(countyList.size()>0){
            dataList.clear();
            for(County county:countyList){
                dataList.add(county.getCountyName());
            }
            adapter.notifyDataSetChanged();
            listView.setSelection(0);
            titleText.setText(selectedCity.getCityName());
            currentLevel=LEVEL_COUNTY;
        }else {
            queryFromServer(selectedCity.getCityCode(),"county");
        }
    }
    private void queryFromServer(final String code,final String type){
        //code来确定访问的地址，type来确定Utility中的方法，从而来对应的解析相应的数据类型
        String address;
        if(!TextUtils.isEmpty(code)){
            address="http://www.weather.com.cn/data/list3/city"+code+".xml";
        }else{
            address="http://www.weather.com.cn/data/list3/city.xml";
        }
        showProgressDialog();
        HttpUtil.sendHttpRequest(address, new HttpCallbackListener() {
            @Override
            public void onFinish(String response) {
                boolean result=false;
                //下面三种情况分别调用handle...response方法，该方法中会自动将解析的数据存储在数据库中
                if("province".equals(type)){
                    result= Utility.handleProvincesResponse(mMyWeatherDB,response);
                }else if("city".equals(type)){
                    result=Utility.handleCitiesResponse(mMyWeatherDB,response,selectedProvincce.getId());
                    //特别注意很多方法province和city，county有点不同
                }else  if("county".equals(type)){
                    result=Utility.handleCountiesResponse(mMyWeatherDB,response,selectedCity.getId());
                }
                if(result){
                    runOnUiThread(new Runnable() {//通过renOnUiTread（）方法来回到主线程处理逻辑
                        @Override
                        public void run() {
                            closeProgressDialog();
                            if("province".equals(type)){
                                queryProvinces();//再解析和处理完数据之后，handle...response方法自动将数据保存在数据库中
                                //所以这里再一次加载queryProvinces方法，这次就可以直接从数据库中读取数据
                                //由于此方法牵扯到了UI操作，所以必须要在主线程中调用，这里
                                // 借助了runOnTiThread方法来实现从子线程到主线程的切换
                            }else if("city".equals(type)){
                                queryCities();
                            }else if("county".equals(type)){
                                queryCounties();
                            }
                        }
                    });
                }
            }
            @Override
            public void onError(Exception e) {//和上面的onfinish方法一样，同样是重写的方法，同样是回到主线程处理逻辑
                runOnUiThread(new Runnable() {
                    //由于此方法牵扯到了UI操作，所以必须要在主线程中调用，这里
                    // 借助了runOnTiThread方法来实现从子线程到主线程的切换
                    @Override
                    public void run() {
                        closeProgressDialog();
                        Toast.makeText(ChooseAreaActivity.this,"加载失败",Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
    }
    private void showProgressDialog(){
        if(progressDialog==null){
            progressDialog=new ProgressDialog(this);
            progressDialog.setMessage("正在加载...");
            progressDialog.setCanceledOnTouchOutside(false);//???????????????????
        }
        progressDialog.show();
    }
    private void closeProgressDialog(){
        if(progressDialog!=null){
            progressDialog.dismiss();
        }
    }

    @Override
    public void onBackPressed() {//覆盖默认back健的行为，根据当前级别来判断返回的列表
        super.onBackPressed();
        if(currentLevel==LEVEL_COUNTY){
            queryCities();
        }else if(currentLevel==LEVEL_CITY){
            queryProvinces();
        }else {
            if(isFromWeatherActivity){//如果是重新选择城市的时候按返回健，即返回天气界面
                Intent intent=new Intent(this,WeatherActivity.class);
                startActivity(intent);
            }
            finish();
        }
    }
}
