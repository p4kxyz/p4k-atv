package com.files.codes.service;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import com.files.codes.AppConfig;
import com.files.codes.database.DatabaseHelper;
import com.files.codes.model.api.ApiService;
import com.files.codes.model.subscription.ActiveStatus;
import com.files.codes.utils.RetrofitClient;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;

public class SubscriptionStatusUpdateTask extends AsyncTask<Void, Void, Void> {
    private String userId;
    private Context context;

    public interface AsyncResponse {
        void processFinish(Boolean output);
    }
    AsyncResponse asyncResponse = null;

    public SubscriptionStatusUpdateTask(Context context, String userId){
        super();
        this.context = context;
        this.userId = userId;
    }

    public SubscriptionStatusUpdateTask(String userId, Context context, AsyncResponse asyncResponse) {
        this.userId = userId;
        this.context = context;
        this.asyncResponse = asyncResponse;
    }

    @Override
    protected Void doInBackground(Void... voids) {
        Retrofit retrofit = RetrofitClient.getRetrofitInstance();
        ApiService subscriptionApi = retrofit.create(ApiService.class);
        Call<ActiveStatus> call = subscriptionApi.getActiveStatus(AppConfig.API_KEY, userId);
        call.enqueue(new Callback<ActiveStatus>() {
            @Override
            public void onResponse(Call<ActiveStatus> call, Response<ActiveStatus> response) {
                if (response.code() == 200) {
                    ActiveStatus activeStatus = response.body();
                    DatabaseHelper db = new DatabaseHelper(context);
                    db.deleteAllActiveStatusData();
                    db.insertActiveStatusData(activeStatus);
                    Log.e("SubscriptionTask", "onResponse: subscription status updated" );
                }
            }

            @Override
            public void onFailure(Call<ActiveStatus> call, Throwable t) {
                //Toast.makeText(context, context.getString(R.string.something_went_text), Toast.LENGTH_SHORT).show();
            }
        });
        return null;
    }

    @Override
    protected void onPostExecute(Void unused) {
        super.onPostExecute(unused);
        if (asyncResponse != null){
            asyncResponse.processFinish(true);
        }
    }
}
