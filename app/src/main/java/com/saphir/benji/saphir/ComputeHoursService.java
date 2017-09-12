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
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;
import java.util.Calendar;

/**
 * Created by Benji on 04/09/2017.
 * Compute the work hours of the agent and returns his status
 * Write his status to a file in external storage
 */
public class ComputeHoursService extends Service {

    String TAG="ComputeHours";
    public String mStartDate;

    //String containing the selected Agent in the menu
    public String mSelectedAgent;

    private final static int NOTIFICATION_ID=1;
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
            Log.v(TAG,"Starting Service");
            return Service.START_STICKY;
        }

    @Override
    public void onDestroy(){
            super.onDestroy();
            Log.v(TAG,"Service destroyed");
        }

    /**
     * Place the service into foreground
     */
    public void foreground(){
        startForeground(NOTIFICATION_ID, CreateNotification());
    }

    /**
     * Place the service into background
     */
    public void background(){
        stopForeground(true);
    }

    /**
     * TimerService.elapsedTime is passed in argument, and multiplied by 1000
     * to get the time back in millis
     * Milliseconds are better to use with the comparison time
     * @param elapsedTime in seconds
     * @return elapsedTime in millis
     */
    public long getElapsedTimeInMillis(long elapsedTime){
            elapsedTimeinMillis += elapsedTime * 1000;
            Log.i(TAG,"Elapsed time in millis: "+elapsedTimeinMillis);
            return elapsedTimeinMillis;
        }

    /**
     * Get the elapsed time in sec
     * @return get elapsed time in sec
     */
    public long getElapsedTimeinSec(){
        return elapsedTimeinMillis/1000;
    }

    /**
     * Used on MainActivity to get the date and time on which the user pressed
     * the start button
     * @return date and time on which the user pressed the start button
     */
    public String getDate(){
        Calendar calendar = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy--HH:mm:ss");
        mStartDate = "Date de début: "+sdf.format(calendar.getTime());
        return mStartDate;
    }

    /**
     * Get the currently selected agent in the menu
     * @return name of selected agent
     */
    public String getSelectedAgent(){
        mSelectedAgent = MainActivity.SELECTED_AGENT;
        //Si la chaine est vide
        if(MainActivity.SELECTED_AGENT.equals("")){
            mSelectedAgent="Pas d'agent selectionné";
        }
        return mSelectedAgent;
    }

    /**
     * Return the agent's status , the date he started to work and how long has he been working
     * @param elapsedTime in millis
     */
    public void getAgentStatus(long elapsedTime) { //TODO verify calculation & remove Compare in the Write() method
        long compare = computeWorkTime(elapsedTime);
        Log.i(TAG,"Compare : "+compare);
        //Si temps de repos > 35H
        if (compare > mInterval35) {
            canWork = true;
            Write(getSelectedAgent() +"\n" + mStartDate+"\n" + printWorkTime() + "Cet agent est disponible\n\n");
            printWorkTime();
        }
        //Si temps de repos  entre 24 et 35H
        if (compare >= mInterval24 && compare <= mInterval35) {
            shouldRest = true;
            Write(getSelectedAgent() +"\n" + mStartDate+"\n" + printWorkTime() + "Cet agent devrait se reposer\n\n" );
            printWorkTime();
        }
        //Si temps de repos inferieur a 24H
        if (compare < mInterval24) {
            cantWork = true;
            Write(getSelectedAgent() +"\n" + mStartDate+"\n" + printWorkTime() + "Cet agent doit ce reposer immédiatement\n\n");
            printWorkTime();
        }
    }

    /**
     * Used to do calculations
     * @param elapsedTime in sec
     * @return mIntervalWeekend - worked time
     */
    public long computeWorkTime(long elapsedTime){
          return  mIntervalWeekend - getElapsedTimeInMillis(elapsedTime);
        }

    /**
     * Prints the work time in a fancy way
     * @return the work time in a fancy way
     */
    public String printWorkTime(){
        long sec = getElapsedTimeinSec();
        long mins = sec/60;
        long hours = mins/60;
        return "Temps total de travail : " +hours%24+"H "+mins%60+"m "+sec%60+"s"+"\n";
    }

    /**
     * Create a Saphir folder in the external storage and write to the
     * 'agents.txt' file in it
     * @param toWrite data to write
     */
    private void Write(String toWrite){
        File folder = new File(getExternalCacheDir(),"Saphir");
        folder.mkdirs();
        File file = new File(folder,"Rapport.txt");
        try {
            file.createNewFile();

            FileOutputStream fOut = new FileOutputStream(file,true);
            OutputStreamWriter myOutWriter = new OutputStreamWriter(fOut);

            myOutWriter.append(toWrite);
            myOutWriter.close();

            fOut.flush();
            fOut.close();
            Log.i(TAG,"Write Successful");
        }
        catch (IOException e){
            Log.e(TAG,"File write failed : " + e.toString());
        }
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
                .setLargeIcon(Bitmap.createScaledBitmap(icon, 128, 128, false))
                .setPriority(Notification.PRIORITY_LOW);
        Intent resultIntent = new Intent(this,MainActivity.class);
        PendingIntent resultPendingIntent = PendingIntent.getActivity(this,0,resultIntent,PendingIntent.FLAG_UPDATE_CURRENT);
        builder.setContentIntent(resultPendingIntent);
        return builder.build();
    }
}

