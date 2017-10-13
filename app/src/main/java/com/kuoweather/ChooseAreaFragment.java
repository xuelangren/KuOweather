package com.kuoweather;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import com.kuoweather.db.City;
import com.kuoweather.db.County;
import com.kuoweather.db.Province;
import com.kuoweather.util.HttpUtil;
import com.kuoweather.util.Utility;

import org.litepal.crud.DataSupport;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

/**
 * Created by Administrator on 2017/9/27.
 */

public class ChooseAreaFragment extends Fragment {
    public static final int LEVEL_PROVINCE = 0;
    public static final int LEVEL_CITY = 1;
    public static final int LEVEL_COUNTY = 2;
    private ProgressDialog progressDialog;
    private TextView textView;
    private Button button;
    private ListView listView;
    private ArrayAdapter<String> adapter;

    private List<String> dataList = new ArrayList<>();
    private List<Province> provinceList;
    private List<City> cityList;
    private List<County> countyList;

    private Province selectedProvince;
    private City selectedCity;
    private County selectedCounty;

    private int currentLevel;
    private String url ="http://guolin.tech/api/weather?cityid=CN101070201&key=bc0418b57b2d4918819d3974ac1285d9";

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.choose_area, container,false);
        textView = (TextView) view.findViewById(R.id.tv_tittle);
        button = (Button) view.findViewById(R.id.back_button);
        listView = (ListView) view.findViewById(R.id.lv);
        adapter = new ArrayAdapter<String>(getContext(),android.R.layout.simple_list_item_1,dataList);
        listView.setAdapter(adapter);
        return view;
    }


    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if(currentLevel == LEVEL_PROVINCE){
                    selectedProvince = provinceList.get(position);
                    queryCities();

                }else if(currentLevel == LEVEL_CITY){
                    selectedCity = cityList.get(position);
                    queryCounties();
                }else if(currentLevel == LEVEL_COUNTY){
                    String weatherId = countyList.get(position).getWeatherId();
                    if(getActivity() instanceof MainActivity){
                        Intent intent =new Intent(getActivity(),WeatherActivity.class);
                        intent.putExtra("weatherId",weatherId);
                        startActivity(intent);
                        getActivity().finish();
                    }else if(getActivity() instanceof WeatherActivity){
                        WeatherActivity weatherActivity = (WeatherActivity) getActivity();
                        weatherActivity.dwLayout.closeDrawers();
                        weatherActivity.swRfLayout.setRefreshing(true);
                        weatherActivity.requestWeather(weatherId);
                    }

                }
            }
        });

        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(currentLevel == LEVEL_COUNTY){
                    queryCities();
                }else if(currentLevel == LEVEL_CITY){
                    queryProvince();
                }
            }
        });
        queryProvince();
    }

    private void queryProvince() {
        textView.setText("中国");
        button.setVisibility(View.INVISIBLE);
        provinceList = DataSupport.findAll(Province.class);
        if(provinceList.size()>0){
            dataList.clear();
            for(Province province:provinceList){
                dataList.add(province.getProvinceName());
            }
            adapter.notifyDataSetChanged();
            listView.setSelection(0);
            currentLevel = LEVEL_PROVINCE;
        }else {
            String address ="http://guolin.tech/api/china";
            queryFormServer(address,"province");
        }
    }
    private void queryCities() {
        textView.setText(selectedProvince.getProvinceName());
        button.setVisibility(View.VISIBLE);
        cityList = DataSupport.where("provinceId = ?",String.valueOf(selectedProvince.getId())).find(City.class);
        if(cityList.size()>0){
            dataList.clear();
            for(City city:cityList){
                dataList.add(city.getCityName());
            }
            adapter.notifyDataSetChanged();
            listView.setSelection(0);
            currentLevel = LEVEL_CITY;
        }else {
            int provinceCode = selectedProvince.getProvinceCode();
            String address ="http://guolin.tech/api/china/"+provinceCode;
            queryFormServer(address,"city");
        }

    }

    private void queryCounties() {
        textView.setText(selectedCity.getCityName());
        button.setVisibility(View.VISIBLE);
        countyList = DataSupport.where("cityId = ?",String.valueOf(selectedCity.getId())).find(County.class);
        if(countyList.size()>0){

            dataList.clear();
            for(County county:countyList){
                dataList.add(county.getCountyName());//
                Log.d("aaaaaa", "queryCounties: "+county.getWeatherId());
            }
            adapter.notifyDataSetChanged();
            listView.setSelection(0);
            currentLevel = LEVEL_COUNTY;
        }else {
            int provinceCode = selectedProvince.getProvinceCode();
            int cityCode = selectedCity.getCityCode();
            String address = "http://guolin.tech/api/china/"+provinceCode+"/"+cityCode;
            queryFormServer(address,"county");
        }
    }

    private void queryFormServer(String address, final String type) {
             showProgressBarDialog();
           HttpUtil.sendOkHttpRequest(address, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                   getActivity().runOnUiThread(new Runnable() {
                       @Override
                       public void run() {
                           closeProgressDialog();
                       }
                   });
            }
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                    String responseText  = response.body().string();
                    boolean result = false;
                    if("province".equals(type)){
                        result = Utility.handleProvinceResponse(responseText);
                    }else if("city".equals(type)){
                        result = Utility.handleCityResponse(responseText,selectedProvince.getId());

                    }else if("county".equals(type)){
                        result = Utility.handleCountyResponse(responseText,selectedCity.getId());
                    }
                    if(result){
                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                closeProgressDialog();
                                if("province".equals(type)){
                                    queryProvince();
                                }else if("city".equals(type)){
                                    queryCities();
                                }else if("county".equals(type)){
                                    queryCounties();
                                }
                            }
                        });
                    }
            }
        });


    }

    private void closeProgressDialog() {
        if(progressDialog!=null){
            progressDialog.dismiss();
        }
    }

    private void showProgressBarDialog() {
        if(progressDialog ==null){
            progressDialog = new ProgressDialog(getActivity());
            progressDialog.setMessage("正在加载...");
            progressDialog.setCanceledOnTouchOutside(false);
        }
        progressDialog.show();
    }


}
