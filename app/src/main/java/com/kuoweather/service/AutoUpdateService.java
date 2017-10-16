package com.kuoweather.service;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.os.SystemClock;
import android.preference.PreferenceManager;

import com.kuoweather.Gson.Weather;
import com.kuoweather.util.HttpUtil;
import com.kuoweather.util.Utility;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class AutoUpdateService extends Service {
    public AutoUpdateService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
     return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        updateWeather();
        updateBingPic();
        AlarmManager manger = (AlarmManager) getSystemService(ALARM_SERVICE);
        int anNother = 1000*60*8*8;
        long trigertime = SystemClock.elapsedRealtime()+anNother;
     Intent i = new Intent(this,AutoUpdateService.class);
        PendingIntent pi =PendingIntent.getService(this,0,i,0);
        manger.cancel(pi);
        manger.set(AlarmManager.ELAPSED_REALTIME_WAKEUP,trigertime,pi);

        return super.onStartCommand(intent, flags, startId);
    }

    private void updateBingPic() {
        String requestBingPic = "http://guolin.tech/api/bing_pic";
        HttpUtil.sendOkHttpRequest(requestBingPic, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {

            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String responsText = response.body().string();
                 SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(AutoUpdateService.this).edit();
                editor.putString("bing_pic",responsText);
                editor.apply();

            }
        });


    }

    private void updateWeather() {
        final SharedPreferences preference = PreferenceManager.getDefaultSharedPreferences(this);
        String weatherString = preference.getString("weather", null);
        if(weatherString!=null ){
            Weather weather = Utility.handleWeatherResponse(weatherString);
            String weatherId = weather.getBasic().getId();
            String weatherUrl = "http://guolin.tech/api/weather?cityid=" + weatherId + "&key=bc0418b57b2d4918819d3974ac1285d9";
            HttpUtil.sendOkHttpRequest(weatherUrl, new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {

                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    String responseText = response.body().string();
                    Weather weather1 = Utility.handleWeatherResponse(responseText);
                    if(weather1!=null && "ok".equals(weather1.getStatus())){
                        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(AutoUpdateService.this).edit();
                        editor.putString("weather",responseText);
                        editor.apply();
                    }
                }
            });

        }


    }
}
