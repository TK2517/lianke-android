package com.fitlog.local;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.text.InputType;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.function.LongConsumer;

public final class Ui {
    public static final int BG=Color.rgb(247,249,248), WHITE=Color.WHITE, INK=Color.rgb(29,47,46), SUB=Color.rgb(98,113,110), TEAL=Color.rgb(33,122,114), PALE=Color.rgb(230,242,239), LINE=Color.rgb(226,233,231), RED=Color.rgb(171,57,57);
    private Ui() {}
    public static int dp(Activity a, float n) { return (int)(n*a.getResources().getDisplayMetrics().density+0.5f); }
    public static GradientDrawable box(Activity a, int fill, int radius) { GradientDrawable d=new GradientDrawable(); d.setColor(fill); d.setCornerRadius(dp(a,radius)); return d; }
    public static LinearLayout column(Activity a) { LinearLayout v=new LinearLayout(a); v.setOrientation(LinearLayout.VERTICAL); return v; }
    public static LinearLayout row(Activity a) { LinearLayout v=new LinearLayout(a); v.setOrientation(LinearLayout.HORIZONTAL); v.setGravity(android.view.Gravity.CENTER_VERTICAL); return v; }
    public static TextView text(Activity a, String s, int sp, int color, boolean bold) { TextView v=new TextView(a); v.setText(s); v.setTextSize(sp); v.setTextColor(color); if(bold)v.setTypeface(Typeface.DEFAULT,Typeface.BOLD); return v; }
    public static TextView button(Activity a, String label, boolean filled, Runnable action) {
        TextView v=text(a,label,14,filled?WHITE:TEAL,true); v.setGravity(android.view.Gravity.CENTER); v.setMinHeight(dp(a,42));
        v.setPadding(dp(a,14),dp(a,8),dp(a,14),dp(a,8)); v.setBackground(box(a,filled?TEAL:PALE,12)); v.setOnClickListener(w->action.run()); return v;
    }
    public static LinearLayout quickTile(Activity a,String icon,String label,int fill,Runnable action) {
        LinearLayout tile=column(a);tile.setGravity(android.view.Gravity.CENTER);tile.setMinimumHeight(dp(a,76));
        tile.setPadding(dp(a,4),dp(a,7),dp(a,4),dp(a,7));
        tile.setBackground(new RippleDrawable(ColorStateList.valueOf(0x26217A72),box(a,fill,16),null));
        TextView symbol=text(a,icon,23,INK,false);symbol.setGravity(android.view.Gravity.CENTER);symbol.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);tile.addView(symbol);
        TextView name=text(a,label,13,INK,true);name.setGravity(android.view.Gravity.CENTER);name.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);tile.addView(name);
        tile.setContentDescription(label);tile.setClickable(true);tile.setFocusable(true);tile.setOnClickListener(v->action.run());
        return tile;
    }
    public static LinearLayout card(Activity a) { LinearLayout v=column(a); v.setPadding(dp(a,16),dp(a,14),dp(a,16),dp(a,14)); v.setBackground(box(a,WHITE,18)); v.setElevation(dp(a,1)); return v; }
    public static void add(LinearLayout parent, View child, Activity a, int top) {
        LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT); p.topMargin=dp(a,top); parent.addView(child,p);
    }
    public static void gap(LinearLayout p, Activity a, int height) { View v=new View(a); p.addView(v,new LinearLayout.LayoutParams(1,dp(a,height))); }
    public static EditText input(Activity a, String label, String value, boolean numeric) {
        EditText e=new EditText(a); e.setSingleLine(true); e.setTextSize(15); e.setTextColor(INK); e.setHintTextColor(SUB); e.setHint(label); e.setText(value==null?"":value);
        if(numeric)e.setInputType(InputType.TYPE_CLASS_NUMBER|InputType.TYPE_NUMBER_FLAG_DECIMAL|InputType.TYPE_NUMBER_FLAG_SIGNED);
        e.setPadding(dp(a,8),dp(a,10),dp(a,8),dp(a,10)); return e;
    }
    public static void field(LinearLayout parent, Activity a, String label, View input) { add(parent,text(a,label,12,SUB,true),a,12); add(parent,input,a,2); }
    public interface Save { void run() throws Exception; }
    public static void form(Activity a, String title, LinearLayout body, String saveLabel, Save save) {
        form(a,title,body,saveLabel,save,null);
    }
    public static void form(Activity a, String title, LinearLayout body, String saveLabel, Save save, Runnable cancel) {
        ScrollView scroll=new ScrollView(a); scroll.setFillViewport(false); body.setPadding(dp(a,20),dp(a,8),dp(a,20),dp(a,16)); scroll.addView(body);
        AlertDialog dialog=new AlertDialog.Builder(a).setTitle(title).setView(scroll).setNegativeButton("取消",(d,w)->{if(cancel!=null)cancel.run();}).setPositiveButton(saveLabel,null).create();
        dialog.setOnCancelListener(d->{if(cancel!=null)cancel.run();});
        dialog.setOnShowListener(x->dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v->{
            try { save.run(); dialog.dismiss(); } catch(Exception ex) { new AlertDialog.Builder(a).setMessage(ex.getMessage()==null?"输入有误":ex.getMessage()).setPositiveButton("知道了",null).show(); }
        })); dialog.show();
    }
    public static void confirm(Activity a,String message,Runnable yes) { new AlertDialog.Builder(a).setMessage(message).setNegativeButton("取消",null).setPositiveButton("删除",(d,w)->yes.run()).show(); }
    public static String trim(EditText e) { return e.getText().toString().trim(); }
    public static double nonnegative(EditText e,String label) { double x=Double.parseDouble(trim(e)); if(!Double.isFinite(x)||x<0)throw new IllegalArgumentException(label+"必须是非负数"); return x; }
    public static double positive(EditText e,String label) { double x=nonnegative(e,label); if(x<=0)throw new IllegalArgumentException(label+"必须大于 0"); return x; }
    public static void equalWeight(LinearLayout row, View view) { row.addView(view,new LinearLayout.LayoutParams(0,ViewGroup.LayoutParams.WRAP_CONTENT,1)); }

    public static void pickDate(Activity a,long initial,LongConsumer chosen) {
        Calendar c=Calendar.getInstance();c.setTimeInMillis(initial);
        DatePickerDialog picker=new DatePickerDialog(a,(v,year,month,day)->{
            Calendar result=Calendar.getInstance();result.clear();result.set(year,month,day);
            chosen.accept(result.getTimeInMillis());
        },c.get(Calendar.YEAR),c.get(Calendar.MONTH),c.get(Calendar.DAY_OF_MONTH));
        picker.getDatePicker().setMaxDate(System.currentTimeMillis());picker.show();
    }

    public static void pickDateTime(Activity a,long initial,LongConsumer chosen) {
        Calendar c=Calendar.getInstance();c.setTimeInMillis(initial);
        DatePickerDialog date=new DatePickerDialog(a,(v,year,month,day)->{
            TimePickerDialog time=new TimePickerDialog(a,(clock,hour,minute)->{
                Calendar result=Calendar.getInstance();result.clear();result.set(year,month,day,hour,minute);
                chosen.accept(result.getTimeInMillis());
            },c.get(Calendar.HOUR_OF_DAY),c.get(Calendar.MINUTE),true);
            time.show();
        },c.get(Calendar.YEAR),c.get(Calendar.MONTH),c.get(Calendar.DAY_OF_MONTH));
        date.getDatePicker().setMaxDate(System.currentTimeMillis());date.show();
    }

    public static final class TimeChoice {
        public final LinearLayout view;
        private final TextView button;
        private long value;
        private Runnable changed=()->{};
        public TimeChoice(Activity a,String label,long initial,boolean showNow) {
            view=column(a);value=initial;
            add(view,text(a,label,12,SUB,true),a,12);
            LinearLayout row=row(a);
            button=button(a,"",false,()->pickDateTime(a,value,this::set));
            row.addView(button,new LinearLayout.LayoutParams(0,ViewGroup.LayoutParams.WRAP_CONTENT,1));
            if(showNow){
                TextView now=button(a,"现在",false,()->set(System.currentTimeMillis()));
                LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,ViewGroup.LayoutParams.WRAP_CONTENT);p.leftMargin=dp(a,8);row.addView(now,p);
            }
            add(view,row,a,5);set(initial);
        }
        public long get(){return value;}
        public void set(long ms){value=ms/60000*60000;button.setText(new SimpleDateFormat("yyyy-MM-dd  HH:mm",Locale.CHINA).format(new Date(value))+"  修改 ›");changed.run();}
        public void onChange(Runnable listener){changed=listener;changed.run();}
    }
}
