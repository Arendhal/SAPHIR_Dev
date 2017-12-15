package com.saphir.astreinte.saphir;

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
import java.util.Date;

import static java.util.Calendar.AM;
import static java.util.Calendar.PM;

/**
 * Created by Benjamin on 04/09/2017.
 * Compute the work hours of the agent and returns his status
 * Write his status to a file in external storage
 */
public class ComputeHoursService extends Service {

    String TAG="ComputeHours";
    public String mStartDate;
    public String mEndDate;
    public String mType;

    //String containing the selected Agent in the menu
    public String mSelectedAgent;

    //Foreground notification ID
    private final static int NOTIFICATION_ID=1;

    public boolean isSaturday;
    public boolean isWeekend;

    //24 & 35H in millis
    long mInterval24 = 86400000;
    long mInterval35 = 126000000;
    long m35h=126000000;
    //Time from Friday 12h00 PM to Monday 7h AM (67 Hours)
    long mIntervalWeekend = 241200000;

    long elapsedTimeinMillis;

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
    public String getStartDate(){
        Calendar calendar = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy--HH:mm:ss");
        mStartDate = "Horaires: "+sdf.format(calendar.getTime());
        return mStartDate;
    }

    /**
     * Used on MainActivity to get the date and time on which the user pressed
     * the end button
     * @return date and time on which the user pressed the end button
     */
    public String getEndDate(){
        Calendar calendar = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy--HH:mm:ss");
        mEndDate = " à "+sdf.format(calendar.getTime());
        return mEndDate;
    }

    /**
     * Get the type of activity entered by the agent
     * @param type
     * @return the type of activity entered by the agent
     */
    public String getType(String type){
        Log.i(TAG,"Type:"+type);
        mType=type;
        return type;
    }

    /**
     * Return true if day is saturday
     * @return isSaturday
     */
    public boolean isItSaturday(){
        Calendar calendar = Calendar.getInstance();
        weekendUsage();
        if(calendar.get(Calendar.DAY_OF_WEEK) == Calendar.SATURDAY){
            isSaturday = true;
            Log.i(TAG,"Is it saturday : " + isSaturday);
        }
        else{
            isSaturday = false;
            Log.i(TAG,"Is it saturday : " + isSaturday);
        }
        return isSaturday;
    }

    public boolean weekendUsage(){
        Calendar debutAstreinte= Calendar.getInstance();
        Calendar finAstreinte = Calendar.getInstance();
        Calendar phoneTime = Calendar.getInstance();

        //Création début de l'astreinte
        debutAstreinte.set(Calendar.AM_PM,1); //Sets calendar to PM
        debutAstreinte.set(Calendar.DAY_OF_WEEK,Calendar.FRIDAY);
        debutAstreinte.set(Calendar.HOUR_OF_DAY,12);
        Log.i(TAG,"Debut astreinte: " + debutAstreinte.toString());

        //Création fin de l'astreinte
        finAstreinte.set(Calendar.AM_PM,0); //Sets calendar to AM
        finAstreinte.set(Calendar.DAY_OF_WEEK,Calendar.MONDAY);
        finAstreinte.set(Calendar.HOUR_OF_DAY,7);
        finAstreinte.set(Calendar.MINUTE,15);
        Log.i(TAG,"Fin astreinte: " + finAstreinte.toString());

        //Comparaison avec date du tel;
        if(phoneTime.compareTo(debutAstreinte) * phoneTime.compareTo(finAstreinte) >=0 ){
            isWeekend = true;
            Log.i(TAG,"Phone time :" + phoneTime.toString());
            return isWeekend;
        }
        Log.i(TAG,"isWeekend : " + isWeekend);
        isWeekend=false;
        return false;
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
    public void getAgentStatus(long elapsedTime) {
        long compare = computeWorkTime(elapsedTime);
        Log.i(TAG,"Compare : "+compare);
        isItSaturday();
        String SaturdayString = "Eviter de faire appel à cet agent ce dimanche.";
        //Si temps de repos > 35H
        if (compare > mInterval35) {
            if(isWeekend){ Write(getSelectedAgent() +"\n" + mEndDate + "\n" + printWorkTime() + "Cet agent est disponible\n\n"); } //For the weekend
            if(isSaturday){Write(SaturdayString); } //To warn not to use the agent on sunday
            if(!isWeekend){ Write(getSelectedAgent() +"\n" + mStartDate+ mEndDate + "\n" + printWorkTime()); } //For the week
        }
        //Si temps de repos  entre 24 et 35H
        if (compare >= mInterval24 && compare <= mInterval35) {
            if(isWeekend){ Write(getSelectedAgent() +"\n" + mStartDate+ mEndDate + "\n" + printWorkTime() + "Attention seuil de repos des 35H franchi\n\n"); }
            if(isSaturday){ Write(SaturdayString); }
            if(!isWeekend){ Write(getSelectedAgent() +"\n" + mStartDate+ mEndDate + "\n" + printWorkTime()); }
        }
        //Si temps de repos inferieur a 24H
        if (compare < mInterval24) {
            if(isWeekend){ Write(getSelectedAgent() +"\n" + mStartDate+ mEndDate + "\n" + printWorkTime() + "Cet agent doit ce reposer immédiatement\n\n"); }
            if(isSaturday){ Write(SaturdayString); }
            if(!isWeekend){ Write(getSelectedAgent() +"\n" + mStartDate+ mEndDate + "\n" + printWorkTime()); }
        }
    }

    /**
     * Used to do calculations
     * @param elapsedTime in sec
     * @return mIntervalWeekend - worked time
     */
    public long computeWorkTime(long elapsedTime){
          return  m35h - getElapsedTimeInMillis(elapsedTime);
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
    public void Write(String toWrite){
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

