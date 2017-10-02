package kii.com.gfapp;

import android.app.Activity;
import android.os.AsyncTask;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.Scopes;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Scope;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.fitness.Fitness;
import com.google.android.gms.fitness.FitnessStatusCodes;
import com.google.android.gms.fitness.data.DataSet;
import com.google.android.gms.fitness.data.DataType;
import com.google.android.gms.fitness.data.Field;
import com.google.android.gms.fitness.request.DataReadRequest;
import com.google.android.gms.fitness.result.DailyTotalResult;
import com.google.android.gms.fitness.result.DataReadResult;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

/**
 * This sample demonstrates combining the Recording API and History API of the Google Fit platform
 * to record steps, and display the daily current step count. It also demonstrates how to
 * authenticate a user with Google Play Services.
 */
public class MainActivity extends AppCompatActivity {
    public static final String TAG = "StepCounter";
    private GoogleApiClient mClient = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        // This method sets up our custom logger, which will print all log messages to the device
        // screen, as well as to adb logcat.
        final Button button = (Button) findViewById(R.id.read_steps_button);
        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                readData();
            }
        });
        buildFitnessClient();
    }

    /**
     * Build a {@link GoogleApiClient} to authenticate the user and allow the application
     * to connect to the Fitness APIs. The included scopes should match the scopes needed
     * by your app (see the documentation for details).
     * Use the {@link GoogleApiClient.OnConnectionFailedListener}
     * to resolve authentication failures (for example, the user has not signed in
     * before, or has multiple accounts and must specify which account to use).
     */
    private void buildFitnessClient() {
        // Create the Google API Client
        mClient = new GoogleApiClient.Builder(this)
                .addApi(Fitness.RECORDING_API)
                .addApi(Fitness.HISTORY_API)
                .addScope(new Scope(Scopes.FITNESS_ACTIVITY_READ))
                .addScope(new Scope(Scopes.FITNESS_BODY_READ))
                .addConnectionCallbacks(
                        new GoogleApiClient.ConnectionCallbacks() {

                            @Override
                            public void onConnected(Bundle bundle) {
                                Log.i(TAG, "Connected!!!");
                                // Now you can make calls to the Fitness APIs.  What to do?
                                // Subscribe to some data sources!
                                subscribe();
                            }

                            @Override
                            public void onConnectionSuspended(int i) {
                                // If your connection to the sensor gets lost at some point,
                                // you'll be able to determine the reason and react to it here.
                                if (i == GoogleApiClient.ConnectionCallbacks.CAUSE_NETWORK_LOST) {
                                    Log.w(TAG, "Connection lost.  Cause: Network Lost.");
                                } else if (i == GoogleApiClient.ConnectionCallbacks.CAUSE_SERVICE_DISCONNECTED) {
                                    Log.w(TAG, "Connection lost.  Reason: Service Disconnected");
                                }
                            }
                        }
                )
                .enableAutoManage(this, 0, new GoogleApiClient.OnConnectionFailedListener() {
                    @Override
                    public void onConnectionFailed(ConnectionResult result) {
                        Log.w(TAG, "Google Play services connection failed. Cause: " +
                                result.toString());
                        Snackbar.make(
                                MainActivity.this.findViewById(R.id.main_activity_view),
                                "Exception while connecting to Google Play services: " +
                                        result.getErrorMessage(),
                                Snackbar.LENGTH_INDEFINITE).show();
                    }
                })
                .build();
    }

    /**
     * Record step data by requesting a subscription to background step data.
     */
    public void subscribe() {
        // To create a subscription, invoke the Recording API. As soon as the subscription is
        // active, fitness data will start recording.

        // subscribe to step count
        Fitness.RecordingApi.subscribe(mClient, DataType.TYPE_STEP_COUNT_CUMULATIVE)
                .setResultCallback(new ResultCallback<Status>() {
                    @Override
                    public void onResult(Status status) {
                        if (status.isSuccess()) {
                            if (status.getStatusCode()
                                    == FitnessStatusCodes.SUCCESS_ALREADY_SUBSCRIBED) {
                                Log.i(TAG, "Existing subscription for activity detected.");
                            } else {
                                Log.i(TAG, "Successfully subscribed!");
                            }
                        } else {
                            Log.w(TAG, "There was a problem to subscribe activity.");
                        }
                    }
                });

        // subscribe to weight
        Fitness.RecordingApi.subscribe(mClient, DataType.TYPE_WEIGHT)
                .setResultCallback(new ResultCallback<Status>() {
                    @Override
                    public void onResult(@NonNull Status status) {
                        if (status.isSuccess()) {
                            if (status.getStatusCode()
                                    == FitnessStatusCodes.SUCCESS_ALREADY_SUBSCRIBED) {
                                Log.i(TAG, "Existing subscription for weight detected");
                            } else {
                                Log.i(TAG, "Successfully subscribed weight!");
                            }
                        } else {
                          Log.w(TAG, "There was a problme to subscribe weight.");
                        }
                    }
                });

        // subscribe to height
        Fitness.RecordingApi.subscribe(mClient, DataType.TYPE_HEIGHT)
                .setResultCallback(new ResultCallback<Status>() {
                    @Override
                    public void onResult(@NonNull Status status) {
                        if (status.isSuccess()) {
                            if (status.getStatusCode()
                                    == FitnessStatusCodes.SUCCESS_ALREADY_SUBSCRIBED) {
                                Log.i(TAG, "Existing subscription for height detected");
                            } else {
                                Log.i(TAG, "Successfully subscribed height!");
                            }
                        } else {
                            Log.w(TAG, "There was a problme to subscribe height.");
                        }
                    }
                });
    }

    private void readData() {
        new VerifyDataTask().execute();
    }

    /**
     * Read the current daily step total, computed from midnight of the current day
     * on the device's current timezone.
     */
    private class VerifyDataTask extends AsyncTask<Void, Void, Void> {
        private Activity parent;
        protected Void doInBackground(final Void... params) {
            this.parent = MainActivity.this;
            final int duration = Toast.LENGTH_LONG;
            ArrayList<String>datas = new ArrayList<>();

            PendingResult<DailyTotalResult> result = Fitness.HistoryApi.readDailyTotalFromLocalDevice(mClient, DataType.TYPE_STEP_COUNT_DELTA);
            DailyTotalResult totalResult = result.await(30, TimeUnit.SECONDS);
            if (totalResult.getStatus().isSuccess()) {
                DataSet totalSet = totalResult.getTotal();
                if (totalSet.isEmpty()){
                    datas.add("No records of step count.");
                }else{
                    datas.add("Today's steps: "+ totalSet.getDataPoints().get(0).getValue(Field.FIELD_STEPS).asInt());
                }
            }

            Calendar cal = Calendar.getInstance();
            Date now = new Date();
            cal.setTime(now);
            DataReadRequest dataReadRequest = new DataReadRequest.Builder()
                    .read(DataType.TYPE_WEIGHT)
                    .setTimeRange(1, cal.getTimeInMillis(), TimeUnit.MILLISECONDS)
                    .setLimit(1)
                    .build();
            DataReadResult dataReadResult =
                    Fitness.HistoryApi.readData(mClient, dataReadRequest).await(1, TimeUnit.MINUTES);
            if (dataReadResult.getStatus().isSuccess()) {
                DataSet set = dataReadResult.getDataSet(DataType.TYPE_WEIGHT);
                if (set.isEmpty()) {
                    datas.add("No records of weight.");
                }else {
                    datas.add("Weight: "+ set.getDataPoints().get(0).getValue(Field.FIELD_WEIGHT).asFloat() + " kg");
                }
            }

            dataReadRequest = new DataReadRequest.Builder()
                    .read(DataType.TYPE_HEIGHT)
                    .setTimeRange(1, cal.getTimeInMillis(), TimeUnit.MILLISECONDS)
                    .setLimit(1)
                    .build();
            dataReadResult =
                    Fitness.HistoryApi.readData(mClient, dataReadRequest).await(1, TimeUnit.MINUTES);
            if (dataReadResult.getStatus().isSuccess()) {
                DataSet set = dataReadResult.getDataSet(DataType.TYPE_HEIGHT);
                if (set.isEmpty()) {
                    datas.add("No records of height.");
                }else {
                    datas.add("Height: "+ set.getDataPoints().get(0).getValue(Field.FIELD_HEIGHT).asFloat() + "m");
                }
            }

            final CharSequence text = TextUtils.join("\n", datas.toArray());
            parent.runOnUiThread(new Runnable() {
                public void run() {
                    Toast.makeText(parent.getBaseContext(), text, duration).show();
                }
            });
            Log.i(TAG, text.toString());
            return null;
        }
    }
}
