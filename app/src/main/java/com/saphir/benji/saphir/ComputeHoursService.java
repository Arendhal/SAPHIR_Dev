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

import java.text.SimpleDateFormat;

/**
 * Created by Benji on 04/09/2017.
 * Compute the work hours of the agent and returns his status
 */
public class ComputeHoursService extends Service {

        String TAG="ComputeHours";

        private final static int NOTIFICATION_ID=2;
        //24 & 35H in millis
        long mInterval24 = 86400000;
        long mInterval35 = 126000000;
        //Time from Friday 12h00 PM to Monday 7h AM
        long mIntervalWeekend = 241200000;

        //Useful attribute
        long elapsedTimeinMillis;

        //The agent's status
        boolean canWork, shouldRest, cantWork;

        private final IBinder mServiceBinder = new ComputeHoursService.RunServiceBinder();
        public Context mContext;

        public class RunServiceBinder extends Binder {
            ComputeHoursService getService(){
                return ComputeHoursService.this;
            }
        }


    @Nullable
        @Override
    public IBinder onBind(Intent intent) {
            Log.i(TAG,"Binding Service");
            return mServiceBinder;
        }

    @Override
    public void onCreate(){
            super.onCreate();
            /*Code here*/
            Log.i(TAG,"Service created");
            mContext = ComputeHoursService.this;
            canWork=false;
            shouldRest=false;
            cantWork=false;
            elapsedTimeinMillis=0;
        }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId){
            Log.i(TAG,"Starting Service");
            return Service.START_STICKY;
        }

    @Override
    public void onDestroy(){
            super.onDestroy();
            Log.i(TAG,"Service destroyed");
        }

    /**
     * Place the service into foreground
     * @return Place the service into foreground
     */
    public void foreground(){
        startForeground(NOTIFICATION_ID, CreateNotification());
    }

    /**
     * Place the service into background
     * @return Place the service into background
     */
    public void background(){
        stopForeground(true);
    }

    public long getElapsedTimeInMillis(long elapsedTime){
            elapsedTimeinMillis += elapsedTime * 1000;
            Log.i(TAG,"Elapsed time in millis: "+elapsedTimeinMillis);
            return elapsedTimeinMillis;
        }

    public long getElapsedTimeinSec(){
        return elapsedTimeinMillis/1000;
    }

    public long computeWorkTime(long elapsedTime){
          return  mIntervalWeekend - getElapsedTimeInMillis(elapsedTime);
        }

    public void getAgentStatus(long elapsedTime){
            long compare = computeWorkTime(elapsedTime);
            //Si temps de repos > 35H
            if(compare > mInterval35){
                canWork = true;
                Log.i(TAG,"Cet agent est disponible");
                Log.i(TAG,"compare : "+compare);
                printWorkTime();
            }
            //Si temps de repos  entre 24 et 35H
            if(compare > mInterval24 && compare < mInterval35){
                shouldRest = true;
                Log.i(TAG,"Cet agent devrait se reposer");
                Log.i(TAG,"compare : "+compare);
                printWorkTime();
            }
            //Si temps de repos inferieur a 24H
            if(compare < mInterval24){
                cantWork = true;
                Log.i(TAG,"Cet agent doit ce reposer immédiatement");
                Log.i(TAG,"compare : "+compare);
                printWorkTime();
            }
        }

    public String printWorkTime(){
            long sec = getElapsedTimeinSec();
            long mins = sec/60;
            long hours = mins/60;
            String WorkTime = "Temps travaillé : " +hours%24+"H "+mins%60+"m "+sec%60+"s";
            Log.i(TAG,WorkTime);
        return WorkTime;
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

