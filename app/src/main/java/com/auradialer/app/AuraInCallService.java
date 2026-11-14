package com.auradialer.app;

import android.app.*;
import android.content.Intent;
import android.os.Build;
import android.telecom.Call;
import android.telecom.InCallService;

public class AuraInCallService extends InCallService {
    static volatile Call activeCall;
    static volatile AuraInCallService service;
    private static final String CHANNEL="aura_calls";
    @Override public void onCreate(){
        super.onCreate();
        service=this;
        NotificationManager nm=getSystemService(NotificationManager.class);
        nm.createNotificationChannel(new NotificationChannel(CHANNEL,"Phone calls",NotificationManager.IMPORTANCE_HIGH));
    }
    @Override public void onCallAdded(Call call){
        super.onCallAdded(call);
        activeCall=call;
        Intent intent=new Intent(this,CallActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_SINGLE_TOP|Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pi=PendingIntent.getActivity(this,400,intent,PendingIntent.FLAG_IMMUTABLE|PendingIntent.FLAG_UPDATE_CURRENT);
        NotificationManager nm=getSystemService(NotificationManager.class);
        Notification.Builder n=new Notification.Builder(this,CHANNEL)
           .setSmallIcon(android.R.drawable.sym_action_call).setContentTitle("Aura Dialer")
           .setContentText("Tap to manage your call").setCategory(Notification.CATEGORY_CALL)
           .setContentIntent(pi).setOngoing(true).setVisibility(Notification.VISIBILITY_PUBLIC);
        if(Build.VERSION.SDK_INT<34 || nm.canUseFullScreenIntent())n.setFullScreenIntent(pi,true);
        try{nm.notify(400,n.build());}catch(SecurityException ignored){}
    }
    @Override public void onCallRemoved(Call call){
        super.onCallRemoved(call);
        if(activeCall==call){activeCall=null;getSystemService(NotificationManager.class).cancel(400);}
    }
    @Override public void onDestroy(){
        service=null;activeCall=null;
        getSystemService(NotificationManager.class).cancel(400);
        super.onDestroy();
    }
}
