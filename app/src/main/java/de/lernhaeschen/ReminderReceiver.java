package de.lernhaeschen;
import android.app.*;
import android.content.*;
import android.os.Build;
import java.text.SimpleDateFormat;
import java.util.*;
public class ReminderReceiver extends BroadcastReceiver {
 static final String CHANNEL="study_reminder";
 @Override public void onReceive(Context c,Intent i){notifyNow(c);schedule(c);}
 static void schedule(Context c){
  Calendar t=Calendar.getInstance();t.set(Calendar.HOUR_OF_DAY,15);t.set(Calendar.MINUTE,0);t.set(Calendar.SECOND,0);t.set(Calendar.MILLISECOND,0);
  if(t.getTimeInMillis()<=System.currentTimeMillis())t.add(Calendar.DATE,1);
  AlarmManager m=(AlarmManager)c.getSystemService(Context.ALARM_SERVICE);
  PendingIntent p=PendingIntent.getBroadcast(c,101,new Intent(c,ReminderReceiver.class),PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
  m.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,t.getTimeInMillis(),p);
 }
 static void notifyNow(Context c){
  String day=new SimpleDateFormat("yyyy-MM-dd",Locale.GERMANY).format(new Date());
  if(c.getSharedPreferences("bunny",0).getInt("min-"+day,0)>=15)return;
  NotificationManager n=(NotificationManager)c.getSystemService(Context.NOTIFICATION_SERVICE);
  if(Build.VERSION.SDK_INT>=26)n.createNotificationChannel(new NotificationChannel(CHANNEL,"Lernerinnerung",NotificationManager.IMPORTANCE_DEFAULT));
  PendingIntent p=PendingIntent.getActivity(c,99,new Intent(c,MainActivity.class),PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
  n.notify(105,new Notification.Builder(c,CHANNEL).setSmallIcon(android.R.drawable.ic_menu_edit).setContentTitle("Dein Lernhäschen wartet!").setContentText("15 Minuten Vokabeln lernen?").setContentIntent(p).setAutoCancel(true).build());
 }
}