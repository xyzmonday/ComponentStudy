package com.richfit.data.net.http;

import android.util.Log;

import com.jakewharton.retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;
import com.richfit.data.constant.Global;
import com.richfit.data.net.api.IRequestApi;

import java.util.concurrent.TimeUnit;

import okhttp3.HttpUrl;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;


public class RetrofitModule {

    private static IRequestApi sRequest;

    public static IRequestApi getRequestApi(String baseUrl) {
        //注意这里为了增加baseUrl能够修改，不在将IRequestApi设置为单例
        if (sRequest == null) {
            //拦截器
            Interceptor interceptor = chain -> {
                Request oldRequest = chain.request();
                // 添加新的参数
                HttpUrl.Builder authorizedUrlBuilder = oldRequest.url()
                        .newBuilder()
                        .scheme(oldRequest.url().scheme())
                        .host(oldRequest.url().host())
                        .addQueryParameter("macAddress", Global.MAC_ADDRESS)
                        .addQueryParameter("userId", Global.USER_ID)
                        .addQueryParameter("dbSource", Global.DBSOURCE);


                // 新的请求
                Request newRequest = oldRequest.newBuilder()
                        .method(oldRequest.method(), oldRequest.body())
                        .header("registeredChannels", "2")//来自1：iOS,2:Android,3:web
                        .url(authorizedUrlBuilder.build())
                        .build();

                return chain.proceed(newRequest);
            };
            //打印拦截器
            HttpLoggingInterceptor logging = new HttpLoggingInterceptor(message -> Log.i("yff", message));
            logging.setLevel(HttpLoggingInterceptor.Level.BODY);

            OkHttpClient httpClient = new OkHttpClient.Builder()
                    .addInterceptor(interceptor)//添加拦截器
                    .addInterceptor(logging)//添加打印拦截器
                    .connectTimeout(25, TimeUnit.SECONDS)//设置请求超时时间
                    .readTimeout(25, TimeUnit.SECONDS)
                    .writeTimeout(25, TimeUnit.SECONDS)
                    .retryOnConnectionFailure(true)//设置出现错误进行重新连接。
                    .build();

            Retrofit retrofit = new Retrofit.Builder()
                    .baseUrl(baseUrl)
                    .addConverterFactory(GsonConverterFactory.create())
                    .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                    .client(httpClient)
                    .build();
            sRequest = retrofit.create(IRequestApi.class);
        }
        return sRequest;
    }

    public static void setRequestApi(IRequestApi requestApi) {
        sRequest = requestApi;
    }

}

