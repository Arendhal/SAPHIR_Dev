package com.saphir.benji.saphir;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Binder;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

/**
 * Created by Benji on 04/09/17
 * Timer Service tracks the start and end time of timer; Service can be placed
 * into foreground to prevent it being killed when activity runs away
 */

public class TimerService extends Service {

    /** PseudoCode
     OnStart: launch a timer
     make the timer visible
     OnStop: returns for how long it has been running
     The result should be accessible everywhere
     */

    //Foreground notification ID
    private final static int NOTIFICATION_ID = 1;

    //Tag to identify service
    private final static String TAG="TimerService";

    //Is the service running?
    private boolean isTimerRunning;

    //Start and end times in millis
    private long mStartTime,mEndTime;

    //Service binder
    private final IBinder mServiceBinder = new RunServiceBinder();

    public Context mContext;


    public class RunServiceBinder extends Binder {
        TimerService getService(){
            return TimerService.this;
        }
    }


    @Override
    public void onCreate(){
        super.onCreate();
        /*Code here*/
        Log.i(TAG,"Service created");
        mContext = TimerService.this;
        mStartTime = 0;
        mEndTime = 0;
        isTimerRunning = false;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId){
        Log.v(TAG,"Starting Service");

        return Service.START_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
      Log.v(TAG,"Binding Service");
        return mServiceBinder;
    }

    @Override
    public void onDestroy(){
        super.onDestroy();
        Log.i(TAG,"Service destroyed");
    }

    /**
     * Starts the timer
     * @return Starts the timer
    **/
    public void startTimer(){
        if(!isTimerRunning){
            mStartTime= System.currentTimeMillis();
            isTimerRunning=true;
        }
        else {
            Log.e(TAG,"startTimer() request for an already running service");
        }
    }

    /**
     * Stops the timer
     * @return Stops the timer
     */
    public void stopTimer(){
        if(isTimerRunning){
            mEndTime=System.currentTimeMillis();
            isTimerRunning=false;
        }
        else{
            Log.e(TAG,"stopTimer() request for a timer that is not running");
        }

    }

    /**
     * Returns whether the timer is running or not
     * @return Returns whether the timer is running or not
     */
    public boolean isTimerRunning(){
        return isTimerRunning;
    }

    /**
     * Returns the elapsed time in seconds
     * @return Returns the elapsed time in seconds
     */
    public long elapsedTime(){
        return mEndTime > mStartTime ?
                (mEndTime-mStartTime)/1000 :
                (System.currentTimeMillis() - mStartTime)/1000;
    }

    /**
     * Place the service into foreground
     * @return Place the service into foreground
     */
    public void foreground(){
        startForeground(NOTIFICATION_ID,CreateNotification());
    }

    /**
     * Place the service into background
     * @return Place the service into background
     */
    public void background(){
        stopForeground(true);
    }

    /**Create a notification for the service when in foreground
     *
     * @return Create a notification for the service when in foreground
     */
    private Notification CreateNotification(){
        Bitmap icon = BitmapFactory.decodeResource(getResources(), R.drawable.logo);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this)
                    .setContentTitle("Saphir-Astreinte")
                    .setContentText("Touchez pour revenir a l'application")
                    .setSmallIcon(R.drawable.logo_small)
                    .setLargeIcon(Bitmap.createScaledBitmap(icon, 128, 128, false));
        Intent resultIntent = new Intent(this,MainActivity.class);
        PendingIntent resultPendingIntent = PendingIntent.getActivity(this,0,resultIntent,PendingIntent.FLAG_UPDATE_CURRENT);
        builder.setContentIntent(resultPendingIntent);
        return builder.build();
    }
}
