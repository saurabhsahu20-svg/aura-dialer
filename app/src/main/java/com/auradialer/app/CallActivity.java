package com.auradialer.app;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.telecom.Call;
import android.telecom.CallAudioState;
import android.telecom.VideoProfile;
import android.view.Gravity;
import android.view.View;
import android.widget.*;
import android.view.WindowManager;

public class CallActivity extends Activity {
    private Call call;
    private TextView status,answer,end,hold,mute,speaker;
    private boolean isMuted=false,isSpeaker=false;
    private final Call.Callback callback=new Call.Callback(){
        @Override public void onStateChanged(Call changed,int state){runOnUiThread(()->refresh(state));}
    };
    private TextView label(String s,int size,int color){
        TextView t=new TextView(this);t.setText(s);t.setTextSize(size);t.setTextColor(color);t.setGravity(Gravity.CENTER);return t;
    }
    private TextView control(String s,int color){
        TextView t=label(s,17,Color.WHITE);
        GradientDrawable bg=new GradientDrawable();bg.setColor(color);bg.setCornerRadius(34);
        t.setBackground(bg);t.setPadding(12,16,12,16);return t;
    }
    @Override public void onCreate(Bundle bundle){
        super.onCreate(bundle);
        setShowWhenLocked(true);setTurnScreenOn(true);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        call=AuraInCallService.activeCall;
        if(call==null){finish();return;}
        String phone=call.getDetails().getHandle()==null?"":call.getDetails().getHandle().getSchemeSpecificPart();
        if(phone==null)phone="";
        String name=phone.isEmpty()?"Unknown caller":phone;
        Bitmap photo=null;
        if(!phone.isEmpty() && checkSelfPermission(Manifest.permission.READ_CONTACTS)==PackageManager.PERMISSION_GRANTED){
            try(Cursor c=getContentResolver().query(Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI,Uri.encode(phone)),
                new String[]{ContactsContract.PhoneLookup.DISPLAY_NAME,ContactsContract.PhoneLookup.PHOTO_URI},null,null,null)){
                if(c!=null && c.moveToFirst()){
                    if(c.getString(0)!=null)name=c.getString(0);
                    String src=c.getString(1);
                    if(src!=null)try(java.io.InputStream stream=getContentResolver().openInputStream(Uri.parse(src))){
                        photo=BitmapFactory.decodeStream(stream);
                    }
                }
            }catch(Exception ignored){}
        }
        FrameLayout root=new FrameLayout(this);
        GradientDrawable background=new GradientDrawable(GradientDrawable.Orientation.TL_BR,new int[]{0xff202b58,0xff050819});
        root.setBackground(background);
        if(photo!=null){
            ImageView image=new ImageView(this);image.setImageBitmap(photo);
            image.setScaleType(ImageView.ScaleType.CENTER_CROP);
            root.addView(image,new FrameLayout.LayoutParams(-1,-1));
        }
        View overlay=new View(this);
        overlay.setBackground(new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,new int[]{0xbb000000,0x33000000,0xee000000}));
        root.addView(overlay,new FrameLayout.LayoutParams(-1,-1));
        LinearLayout body=new LinearLayout(this);body.setOrientation(LinearLayout.VERTICAL);
        body.setPadding(18,65,18,38);body.setGravity(Gravity.CENTER_HORIZONTAL);
        root.addView(body,new FrameLayout.LayoutParams(-1,-1));
        body.addView(label("A U R A   D I A L E R",14,0xffbaabff));
        TextView title=label(name,33,Color.WHITE);title.setPadding(0,35,0,5);body.addView(title);
        body.addView(label(phone,17,0xffd9dbe6));
        status=label("Calling…",16,0xffe2e4f2);status.setPadding(0,20,0,20);body.addView(status);
        body.addView(new View(this),new LinearLayout.LayoutParams(1,0,1));
        LinearLayout row=new LinearLayout(this);row.setGravity(Gravity.CENTER);
        mute=control("Mute",0xaa354365);speaker=control("Speaker",0xaa354365);
        TextView keypad=control("Keypad",0xaa354365);
        row.addView(mute,new LinearLayout.LayoutParams(0,65,1));
        row.addView(speaker,new LinearLayout.LayoutParams(0,65,1));
        row.addView(keypad,new LinearLayout.LayoutParams(0,65,1));body.addView(row);
        LinearLayout pad=new LinearLayout(this);pad.setOrientation(LinearLayout.VERTICAL);
        pad.setVisibility(View.GONE);body.addView(pad);
        for(String digits:new String[]{"123","456","789","*0#"}){
            LinearLayout line=new LinearLayout(this);
            for(char digit:digits.toCharArray()){
                TextView key=control(String.valueOf(digit),0x99505a7c);
                line.addView(key,new LinearLayout.LayoutParams(0,52,1));
                key.setOnClickListener(v->{if(call!=null){call.playDtmfTone(digit);v.postDelayed(()->{if(call!=null)call.stopDtmfTone();},160);}});
            }
            pad.addView(line);
        }
        keypad.setOnClickListener(v->pad.setVisibility(pad.getVisibility()==View.VISIBLE?View.GONE:View.VISIBLE));
        mute.setOnClickListener(v->{isMuted=!isMuted;if(AuraInCallService.service!=null)AuraInCallService.service.setMuted(isMuted);mute.setText(isMuted?"Unmute":"Mute");});
        speaker.setOnClickListener(v->{isSpeaker=!isSpeaker;if(AuraInCallService.service!=null)AuraInCallService.service.setAudioRoute(isSpeaker?CallAudioState.ROUTE_SPEAKER:CallAudioState.ROUTE_EARPIECE);speaker.setText(isSpeaker?"Earpiece":"Speaker");});
        hold=control("Hold / Resume",0xaa354365);body.addView(hold,new LinearLayout.LayoutParams(-1,62));
        hold.setOnClickListener(v->{if(call!=null){if(call.getState()==Call.STATE_HOLDING)call.unhold();else if(call.getState()==Call.STATE_ACTIVE && (call.getDetails().getCallCapabilities()&Call.Details.CAPABILITY_HOLD)!=0)call.hold();}});
        LinearLayout actions=new LinearLayout(this);actions.setPadding(0,30,0,0);
        answer=control("☎  Answer",0xff22bb78);
        end=control("✕  Decline",0xffe64c68);
        actions.addView(answer,new LinearLayout.LayoutParams(0,75,1));
        actions.addView(end,new LinearLayout.LayoutParams(0,75,1));
        body.addView(actions);
        answer.setOnClickListener(v->{if(call!=null)call.answer(VideoProfile.STATE_AUDIO_ONLY);});
        end.setOnClickListener(v->{if(call!=null){if(call.getState()==Call.STATE_RINGING)call.reject(false,null);else call.disconnect();}finish();});
        setContentView(root);
        call.registerCallback(callback);
        refresh(call.getState());
    }
    private void refresh(int state){
        if(state==Call.STATE_DISCONNECTED||state==Call.STATE_DISCONNECTING){finish();return;}
        status.setText(state==Call.STATE_RINGING?"Incoming call":state==Call.STATE_ACTIVE?"Connected":state==Call.STATE_HOLDING?"On hold":state==Call.STATE_DIALING?"Dialing…":"Calling…");
        answer.setVisibility(state==Call.STATE_RINGING?View.VISIBLE:View.GONE);
        end.setText(state==Call.STATE_RINGING?"✕  Decline":"✕  End call");
        hold.setVisibility(state==Call.STATE_ACTIVE||state==Call.STATE_HOLDING?View.VISIBLE:View.GONE);
    }
    @Override public void onDestroy(){if(call!=null)call.unregisterCallback(callback);super.onDestroy();}
}
