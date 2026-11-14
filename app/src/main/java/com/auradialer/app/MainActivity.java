package com.auradialer.app;

import android.Manifest;
import android.app.Activity;
import android.app.role.RoleManager;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.CallLog;
import android.provider.ContactsContract;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.telecom.TelecomManager;
import android.view.Gravity;
import android.view.View;
import android.widget.*;
import java.util.ArrayList;

public class MainActivity extends Activity {
    private static final int BG=0xff080c1c, PANEL=0xff1b2540, WHITE=Color.WHITE, ACCENT=0xffaa94ff, GREEN=0xff20c788;
    private String number="";
    private int tab=0;
    private EditText digits;
    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        if (getIntent()!=null && getIntent().getData()!=null) number=getIntent().getData().getSchemeSpecificPart();
        render();
        String[] ps={Manifest.permission.READ_CONTACTS,Manifest.permission.READ_CALL_LOG,Manifest.permission.CALL_PHONE,Manifest.permission.POST_NOTIFICATIONS};
        ArrayList<String> missing=new ArrayList<>();
        for(String p:ps) if(checkSelfPermission(p)!=PackageManager.PERMISSION_GRANTED) missing.add(p);
        if(!missing.isEmpty()) requestPermissions(missing.toArray(new String[0]),17);
    }
    @Override public void onRequestPermissionsResult(int r,String[] p,int[] g){super.onRequestPermissionsResult(r,p,g);render();}
    @Override protected void onNewIntent(Intent i){super.onNewIntent(i);if(i.getData()!=null)number=i.getData().getSchemeSpecificPart();tab=0;render();}
    private TextView text(String s,int size,int color){TextView v=new TextView(this);v.setText(s);v.setTextColor(color);v.setTextSize(size);v.setGravity(Gravity.CENTER);return v;}
    private GradientDrawable shape(int color,int radius){GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(radius);return d;}
    private LinearLayout vertical(){LinearLayout l=new LinearLayout(this);l.setOrientation(1);return l;}
    private void button(LinearLayout parent,String label,Runnable action,int color){
        TextView b=text(label,17,WHITE);b.setBackground(shape(color,28));b.setPadding(16,16,16,16);
        LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.setMargins(5,7,5,7);parent.addView(b,p);b.setOnClickListener(v->action.run());
    }
    private void dial(String s){
        if(s.trim().isEmpty())return;
        Uri uri=Uri.fromParts("tel",s.trim(),null);
        Intent i=new Intent(checkSelfPermission(Manifest.permission.CALL_PHONE)==PackageManager.PERMISSION_GRANTED?Intent.ACTION_CALL:Intent.ACTION_DIAL,uri);
        try{startActivity(i);}catch(Exception e){startActivity(new Intent(Intent.ACTION_DIAL,uri));}
    }
    private void requestDefault(){
        RoleManager r=getSystemService(RoleManager.class);
        if(r!=null && r.isRoleAvailable(RoleManager.ROLE_DIALER) && !r.isRoleHeld(RoleManager.ROLE_DIALER))
            startActivityForResult(r.createRequestRoleIntent(RoleManager.ROLE_DIALER),24);
    }
    private void render(){
        LinearLayout root=vertical();root.setPadding(22,38,22,16);root.setBackgroundColor(BG);setContentView(root);
        TextView brand=text("A U R A  D I A L E R",13,ACCENT);root.addView(brand);
        TextView heading=text(tab==0?"Make a call":tab==1?"Contacts":"Recent calls",28,WHITE);
        heading.setTypeface(Typeface.DEFAULT,Typeface.BOLD);heading.setPadding(0,16,0,20);root.addView(heading);
        if(tab==0) showDialpad(root); else showList(root);
        LinearLayout nav=new LinearLayout(this);nav.setGravity(Gravity.CENTER);
        for(int j=0;j<3;j++){final int chosen=j;TextView t=text(new String[]{"Keypad","Contacts","Recents"}[j],14,j==tab?ACCENT:WHITE);nav.addView(t,new LinearLayout.LayoutParams(0,58,1));t.setOnClickListener(v->{tab=chosen;render();});}
        root.addView(nav);
        button(root,"Set as default phone app",this::requestDefault,PANEL);
    }
    private void showDialpad(LinearLayout root){
        LinearLayout space=vertical();root.addView(space,new LinearLayout.LayoutParams(-1,0,1));
        digits=new EditText(this);digits.setTextColor(WHITE);digits.setHintTextColor(0xffa0aec9);digits.setTextSize(28);
        digits.setSingleLine(true);digits.setGravity(Gravity.CENTER);digits.setInputType(3);digits.setText(number);digits.setHint("Enter number");
        space.addView(digits,new LinearLayout.LayoutParams(-1,84));
        String[] keys={"1","2","3","4","5","6","7","8","9","*","0","#"};
        for(int row=0;row<4;row++){
            LinearLayout line=new LinearLayout(this);space.addView(line,new LinearLayout.LayoutParams(-1,0,1));
            for(int col=0;col<3;col++){
                String key=keys[row*3+col];
                TextView b=text(key,26,WHITE);b.setBackground(shape(PANEL,32));
                LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,-1,1);lp.setMargins(5,5,5,5);line.addView(b,lp);
                b.setOnClickListener(v->{number=digits.getText().toString()+key;digits.setText(number);digits.setSelection(number.length());});
                if(key.equals("0"))b.setOnLongClickListener(v->{number=digits.getText().toString()+"+";digits.setText(number);return true;});
            }
        }
        button(space,"⌫  Delete",()->{number=digits.getText().toString();if(!number.isEmpty())number=number.substring(0,number.length()-1);digits.setText(number);},PANEL);
        button(space,"☎  CALL",()->{number=digits.getText().toString();dial(number);},GREEN);
    }
    private void showList(LinearLayout root){
        ScrollView scroll=new ScrollView(this);root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        LinearLayout box=vertical();scroll.addView(box);
        try{
            if(tab==1 && checkSelfPermission(Manifest.permission.READ_CONTACTS)==PackageManager.PERMISSION_GRANTED){
                String[] fields={ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,ContactsContract.CommonDataKinds.Phone.NUMBER};
                try(Cursor c=getContentResolver().query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI,fields,null,null,fields[0]+" ASC")){
                    int count=0;while(c!=null && c.moveToNext() && count++<150){String name=c.getString(0),phone=c.getString(1);button(box,(name==null?phone:name)+"\n"+phone,()->dial(phone),PANEL);}
                }
            }else if(tab==2 && checkSelfPermission(Manifest.permission.READ_CALL_LOG)==PackageManager.PERMISSION_GRANTED){
                try(Cursor c=getContentResolver().query(CallLog.Calls.CONTENT_URI,new String[]{CallLog.Calls.NUMBER},null,null,CallLog.Calls.DATE+" DESC")){
                    int count=0;while(c!=null && c.moveToNext() && count++<80){String phone=c.getString(0);button(box,phone,()->dial(phone),PANEL);}
                }
            }
        }catch(Exception e){box.addView(text("Unable to load. Check permissions.",16,WHITE));}
    }
}
