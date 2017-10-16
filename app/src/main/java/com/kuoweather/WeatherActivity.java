package com.kuoweather;

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
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.kuoweather.Gson.Weather;
import com.kuoweather.service.AutoUpdateService;
import com.kuoweather.util.HttpUtil;
import com.kuoweather.util.Utility;

import java.io.IOException;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class WeatherActivity extends AppCompatActivity {

    @BindView(R.id.tittle_city)
    TextView tittleCity;
    @BindView(R.id.title_update_time)
    TextView titleUpdateTime;
    @BindView(R.id.tv_degree)
    TextView tvDegree;
    @BindView(R.id.tv_weather_info)
    TextView tvWeatherInfo;
    @BindView(R.id.forecast_layout)
    LinearLayout forecastLayout;
    @BindView(R.id.tv_aqi)
    TextView tvAqi;
    @BindView(R.id.tv_pm)
    TextView tvPm;
    @BindView(R.id.tv_comfort)
    TextView tvComfort;
    @BindView(R.id.tv_wash)
    TextView tvWash;
    @BindView(R.id.tv_sport)
    TextView tvSport;
    @BindView(R.id.sv_weather_layout)
    ScrollView svWeatherLayout;
    @BindView(R.id.ig_background)
    ImageView igBackground;
    @BindView(R.id.sw_rf_layout)
    SwipeRefreshLayout swRfLayout;
    @BindView(R.id.dw_layout)
    DrawerLayout dwLayout;
    @BindView(R.id.nav_button)
    Button navButton;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (Build.VERSION.SDK_INT > 21) {
            View decorView = getWindow().getDecorView();
            decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
            getWindow().setStatusBarColor(Color.TRANSPARENT);
        }
        setContentView(R.layout.activity_weather);
        ButterKnife.bind(this);
        swRfLayout.setColorSchemeResources(R.color.colorPrimary);
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        String weatherString = preferences.getString("weather", null);
        final String weatherId;
        if (weatherString != null) {
            Weather weather1 = Utility.handleWeatherResponse(weatherString);
            weatherId = weather1.getBasic().getId();
            showWeatherInfo(weather1);
        } else {
            weatherId = getIntent().getStringExtra("weatherId");
            svWeatherLayout.setVisibility(View.INVISIBLE);
            requestWeather(weatherId);
        }
        swRfLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                requestWeather(weatherId);
            }
        });
        String bingPic = preferences.getString("bing_pic", null);
        if (bingPic != null) {
            Glide.with(this).load(bingPic).into(igBackground);
        } else {
            loadImag();
        }

    }

    private void loadImag() {
        String requestBingPic = "http://guolin.tech/api/bing_pic";
        HttpUtil.sendOkHttpRequest(requestBingPic, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {

            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                final String string = response.body().string();
                SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(WeatherActivity.this).edit();
                editor.putString("bing_pic", string);
                editor.apply();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Glide.with(WeatherActivity.this).load(string).into(igBackground);
                    }
                });
            }
        });
    }

    public void requestWeather(String weatherId) {
        String weatherAddress = "http://guolin.tech/api/weather?cityid=" + weatherId + "&key=bc0418b57b2d4918819d3974ac1285d9";
        HttpUtil.sendOkHttpRequest(weatherAddress, new Callback() {
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                final String responseText = response.body().string();
                final Weather weather = Utility.handleWeatherResponse(responseText);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (weather != null) {
                            SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(WeatherActivity.this).edit();
                            editor.putString("weather", responseText);
                            editor.apply();
                            showWeatherInfo(weather);
                        } else {
                            Toast.makeText(WeatherActivity.this, "获取天气信息失败", Toast.LENGTH_SHORT).show();

                        }
                        swRfLayout.setRefreshing(false);
                    }
                });

            }

            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(WeatherActivity.this, "获取天气信息失败", Toast.LENGTH_SHORT).show();
                        swRfLayout.setRefreshing(false);
                    }
                });

            }
        });
        loadImag();
    }

    private void showWeatherInfo(Weather weather) {
        if (weather != null && "ok".equals(weather.getStatus())) {
            String city = weather.getBasic().getCity();
            String time = weather.getBasic().getUpdate().getLoc().split(" ")[1];
            String degree = weather.getNow().getTmp() + "℃";
            String info = weather.getNow().getCond().getTxt();
            tittleCity.setText(city);
            titleUpdateTime.setText(time);
            tvDegree.setText(degree);
            tvWeatherInfo.setText(info);
            forecastLayout.removeAllViews();
            for (Weather.DailyForecastBean forecast : weather.getDaily_forecast()) {
                View view = LayoutInflater.from(this).inflate(R.layout.forecast_item, forecastLayout, false);
                TextView tvData = (TextView) view.findViewById(R.id.tv_data);
                TextView tvInfo = (TextView) view.findViewById(R.id.tv_info);
                TextView tvMin = (TextView) view.findViewById(R.id.tv_min);
                TextView tvMax = (TextView) view.findViewById(R.id.tv_max);
                tvData.setText(forecast.getDate());
                tvInfo.setText(forecast.getCond().getTxt_d());
                tvMin.setText(forecast.getTmp().getMin());
                tvMax.setText(forecast.getTmp().getMax());
                forecastLayout.addView(view);
            }
            if (weather.getAqi() != null) {
                tvAqi.setText(weather.getAqi().getCity().getAqi());
                tvPm.setText(weather.getAqi().getCity().getPm25());
            }
            String comf = weather.getSuggestion().getComf().getTxt();
            String sprot = weather.getSuggestion().getSport().getTxt();
            String wash = weather.getSuggestion().getCw().getTxt();
            tvComfort.setText("舒适度：" + comf);
            tvSport.setText("运动建议：" + sprot);
            tvWash.setText("洗车度：" + wash);
            svWeatherLayout.setVisibility(View.VISIBLE);
            Intent intent = new Intent(this, AutoUpdateService.class);
            startService(intent);
        } else {
            Toast.makeText(this, "获取天气失败", Toast.LENGTH_SHORT).show();
        }


    }

    @OnClick(R.id.nav_button)
    public void onViewClicked() {
        dwLayout.openDrawer(GravityCompat.START);
    }
}
