package com.fitlog.local;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import android.widget.Toast;
import org.json.JSONObject;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;

public final class MainActivity extends Activity {
    private static final int PICK_PHOTO=110, EXPORT_BACKUP=111, IMPORT_BACKUP=112, EXPORT_CSV=113, TAKE_PHOTO=114;
    private static final String[][] EXERCISES={
        {"胸", "平板卧推", "上斜卧推", "哑铃飞鸟", "俯卧撑", "器械推胸"},
        {"背", "引体向上", "高位下拉", "杠铃划船", "坐姿划船", "硬拉"},
        {"腿", "深蹲", "腿举", "箭步蹲", "罗马尼亚硬拉", "腿弯举", "提踵"},
        {"肩臂", "肩推", "侧平举", "二头弯举", "三头下压", "臂屈伸"},
        {"核心", "平板支撑", "卷腹", "俄罗斯转体", "悬垂举腿"},
        {"有氧", "跑步", "快走", "骑行", "椭圆机", "游泳", "跳绳"},
        {"拉伸", "全身拉伸", "腿部拉伸", "瑜伽"}
    };
    FitDb db;
    private String tab="今日";
    private long workoutId=0, mealId=0;
    private LinearLayout content;
    private long pendingPhotoMeal=0;
    private boolean monthly=false;
    private long selectedDay=dayStart(System.currentTimeMillis());
    private static final SimpleDateFormat DATE_TIME=new SimpleDateFormat("yyyy-MM-dd HH:mm",Locale.CHINA);
    private static final SimpleDateFormat DATE=new SimpleDateFormat("yyyy-MM-dd",Locale.CHINA);

    @Override public void onCreate(Bundle b) { super.onCreate(b); db=new FitDb(this); db.getWritableDatabase(); render(); }
    @Override public void onDestroy() { db.close(); super.onDestroy(); }
    private static String str(JSONObject o,String key) { return o==null?"":o.optString(key,""); }
    private static double num(JSONObject o,String key) { return o==null?0:o.optDouble(key,0); }
    private static String fmt(double x) { return Math.abs(x-Math.rint(x))<0.01?String.format(Locale.CHINA,"%.0f",x):String.format(Locale.CHINA,"%.1f",x); }
    private static String when(long ms) { synchronized(DATE_TIME){return DATE_TIME.format(new Date(ms));} }
    private static String date(long ms) { synchronized(DATE){return DATE.format(new Date(ms));} }
    private static long dayStart(long ms) { Calendar c=Calendar.getInstance();c.setTimeInMillis(ms);c.set(Calendar.HOUR_OF_DAY,0);c.set(Calendar.MINUTE,0);c.set(Calendar.SECOND,0);c.set(Calendar.MILLISECOND,0);return c.getTimeInMillis(); }
    private static long nextDay(long ms) { Calendar c=Calendar.getInstance();c.setTimeInMillis(dayStart(ms));c.add(Calendar.DAY_OF_MONTH,1);return c.getTimeInMillis(); }
    private long selectedTime() { Calendar day=Calendar.getInstance(),clock=Calendar.getInstance();day.setTimeInMillis(selectedDay);day.set(Calendar.HOUR_OF_DAY,clock.get(Calendar.HOUR_OF_DAY));day.set(Calendar.MINUTE,clock.get(Calendar.MINUTE));return day.getTimeInMillis(); }
    private long defaultRecordTime() { return tab.equals("今日")?selectedTime():System.currentTimeMillis(); }
    private static String shortTime(long ms) { return new SimpleDateFormat("HH:mm",Locale.CHINA).format(new Date(ms)); }
    private static ContentValues values(Object... pairs) { ContentValues v=new ContentValues(); for(int i=0;i<pairs.length;i+=2) { Object o=pairs[i+1];String k=(String)pairs[i]; if(o==null)v.putNull(k);else if(o instanceof Integer)v.put(k,(Integer)o);else if(o instanceof Long)v.put(k,(Long)o);else if(o instanceof Double)v.put(k,(Double)o);else v.put(k,String.valueOf(o)); } return v; }
    private void toast(String s) { Toast.makeText(this,s,Toast.LENGTH_LONG).show(); }
    private void title(String s) { Ui.add(content,Ui.text(this,s,23,Ui.INK,true),this,18); }
    private void hint(String s) { Ui.add(content,Ui.text(this,s,13,Ui.SUB,false),this,6); }
    private void action(String label,boolean filled,Runnable run) { Ui.add(content,Ui.button(this,label,filled,run),this,12); }
    private LinearLayout card() { LinearLayout c=Ui.card(this); Ui.add(content,c,this,12); return c; }
    private void line(LinearLayout p,String label,String detail,Runnable click) {
        LinearLayout r=Ui.row(this); TextView left=Ui.text(this,label,16,Ui.INK,true); r.addView(left,new LinearLayout.LayoutParams(0,ViewGroup.LayoutParams.WRAP_CONTENT,1));
        if(detail!=null)r.addView(Ui.text(this,detail,13,Ui.SUB,false)); r.setPadding(0,Ui.dp(this,10),0,Ui.dp(this,10)); if(click!=null)r.setOnClickListener(v->click.run()); Ui.add(p,r,this,2);
    }
    private void render() {
        LinearLayout root=Ui.column(this); root.setBackgroundColor(Ui.BG);
        LinearLayout header=Ui.row(this); header.setPadding(Ui.dp(this,20),Ui.dp(this,12),Ui.dp(this,20),Ui.dp(this,10)); header.setBackgroundColor(Ui.WHITE);
        TextView brand=Ui.text(this,"练刻",22,Ui.TEAL,true); header.addView(brand,new LinearLayout.LayoutParams(0,ViewGroup.LayoutParams.WRAP_CONTENT,1));
        TextView menu=Ui.text(this,"数据与设置",13,Ui.SUB,true); menu.setOnClickListener(v->showSettings()); header.addView(menu); root.addView(header);
        ScrollView sc=new ScrollView(this); sc.setFillViewport(true); content=Ui.column(this); content.setPadding(Ui.dp(this,18),0,Ui.dp(this,18),Ui.dp(this,28)); sc.addView(content); root.addView(sc,new LinearLayout.LayoutParams(-1,0,1));
        if(workoutId!=0)showWorkout(); else if(mealId!=0)showMeal(); else switch(tab) { case "训练": showWorkouts(); break; case "身体": showMetrics(); break; case "饮食": showMeals(); break; case "回顾": showReview(); break; default: showHome(); }
        LinearLayout nav=Ui.row(this); nav.setBackgroundColor(Ui.WHITE); nav.setPadding(Ui.dp(this,6),Ui.dp(this,6),Ui.dp(this,6),Ui.dp(this,6));
        for(String label:new String[]{"今日","训练","身体","饮食","回顾"}) {
            TextView item=Ui.text(this,label,13,tab.equals(label)?Ui.TEAL:Ui.SUB,tab.equals(label)); item.setGravity(Gravity.CENTER); item.setMinHeight(Ui.dp(this,48));
            item.setOnClickListener(v->{tab=label;workoutId=0;mealId=0;render();}); nav.addView(item,new LinearLayout.LayoutParams(0,-2,1));
        }
        root.addView(nav); setContentView(root);
    }

    private static final class DayEntry { final long ts,id;final String kind,title,detail;DayEntry(long t,long i,String k,String a,String b){ts=t;id=i;kind=k;title=a;detail=b;} }
    private void showHome() {
        title("每日记录"); hint("按日期回看或补记；点当天记录可修改时间和内容。");
        LinearLayout dates=Ui.row(this);
        TextView prev=Ui.button(this,"‹",false,()->{Calendar c=Calendar.getInstance();c.setTimeInMillis(selectedDay);c.add(Calendar.DAY_OF_MONTH,-1);selectedDay=dayStart(c.getTimeInMillis());render();});
        dates.addView(prev); TextView dateButton=Ui.button(this,date(selectedDay)+(selectedDay==dayStart(System.currentTimeMillis())?" · 今天":"")+" ▾",false,()->Ui.pickDate(this,selectedDay,d->{selectedDay=dayStart(d);render();}));
        Ui.equalWeight(dates,dateButton); TextView next=Ui.button(this,"›",false,()->{long d=nextDay(selectedDay);if(d<=dayStart(System.currentTimeMillis())){selectedDay=d;render();}});dates.addView(next);
        if(selectedDay==dayStart(System.currentTimeMillis()))next.setAlpha(0.4f);
        Ui.add(content,dates,this,14);
        if(selectedDay!=dayStart(System.currentTimeMillis()))action("回到今天",false,()->{selectedDay=dayStart(System.currentTimeMillis());render();});
        LinearLayout c=card(); long start=selectedDay; long end=nextDay(start);
        int workouts=db.rows("SELECT id FROM workouts WHERE start_ms>=? AND start_ms<?",String.valueOf(start),String.valueOf(end)).size();
        int meals=db.rows("SELECT id FROM meals WHERE ts>=? AND ts<?",String.valueOf(start),String.valueOf(end)).size();
        JSONObject n=db.one("SELECT COALESCE(SUM(i.grams*i.kcal100/100),0) kcal, COALESCE(SUM(i.grams*i.protein100/100),0) protein FROM items i JOIN meals m ON m.id=i.meal_id WHERE m.ts>=? AND m.ts<?",String.valueOf(start),String.valueOf(end));
        Ui.add(c,Ui.text(this,date(start)+" · 当天概览",17,Ui.INK,true),this,0);
        line(c,"训练",workouts+" 次",()->{tab="训练";render();}); line(c,"饮食",meals+" 餐",()->{tab="饮食";render();});
        line(c,"已记录热量",fmt(num(n,"kcal"))+" kcal",()->{tab="饮食";render();}); line(c,"已记录蛋白质",fmt(num(n,"protein"))+" g",null);
        hint("仅汇总已记录的食物；没有饮食记录不代表摄入为零。");
        Ui.add(content,Ui.text(this,"快捷入口",17,Ui.INK,true),this,18);
        LinearLayout quick=Ui.row(this);Ui.equalWeight(quick,Ui.quickTile(this,"🏋️","记训练",0xFFE6F3EF,()->editWorkout(null)));
        quick.addView(new View(this),new LinearLayout.LayoutParams(Ui.dp(this,7),1));Ui.equalWeight(quick,Ui.quickTile(this,"🥗","记饮食",0xFFFFF0DD,()->editMeal(null)));
        quick.addView(new View(this),new LinearLayout.LayoutParams(Ui.dp(this,7),1));Ui.equalWeight(quick,Ui.quickTile(this,"⚖️","记身体",0xFFEEEAF8,()->editMetric(null)));
        quick.addView(new View(this),new LinearLayout.LayoutParams(Ui.dp(this,7),1));Ui.equalWeight(quick,Ui.quickTile(this,"📊","看回顾",0xFFE8F1FC,()->{tab="回顾";render();}));Ui.add(content,quick,this,10);
        Ui.add(content,Ui.text(this,"当天记录 · 点击修改",17,Ui.INK,true),this,20);
        ArrayList<DayEntry> entries=new ArrayList<>();String a=String.valueOf(start),b=String.valueOf(end);
        for(JSONObject w:db.rows("SELECT w.*,COUNT(DISTINCT e.id) exercise_count,COUNT(s.id) set_count FROM workouts w LEFT JOIN exercises e ON e.workout_id=w.id LEFT JOIN sets s ON s.exercise_id=e.id WHERE w.start_ms>=? AND w.start_ms<? GROUP BY w.id",a,b))
            entries.add(new DayEntry(w.optLong("start_ms"),w.optLong("id"),"训练",str(w,"type")+"训练",w.optInt("exercise_count")+" 个项目 · "+w.optInt("set_count")+" 组"));
        for(JSONObject m:db.rows("SELECT * FROM metrics WHERE ts>=? AND ts<?",a,b))
            entries.add(new DayEntry(m.optLong("ts"),m.optLong("id"),"身体",str(m,"type"),metricValue(m)));
        for(JSONObject m:db.rows("SELECT m.*,COUNT(i.id) item_count,COALESCE(SUM(i.grams*i.kcal100/100),0) kcal FROM meals m LEFT JOIN items i ON i.meal_id=m.id WHERE m.ts>=? AND m.ts<? GROUP BY m.id",a,b))
            entries.add(new DayEntry(m.optLong("ts"),m.optLong("id"),"饮食",str(m,"slot"),m.optInt("item_count")+" 种 · "+fmt(num(m,"kcal"))+" kcal"));
        Collections.sort(entries,(x,y)->Long.compare(y.ts,x.ts));
        if(entries.isEmpty())hint("这一天还没有记录。");
        for(DayEntry e:entries){LinearLayout entry=card();Runnable open=()->{
            if(e.kind.equals("训练"))editWorkout(db.one("SELECT * FROM workouts WHERE id=?",String.valueOf(e.id)));
            else if(e.kind.equals("饮食"))editMeal(db.one("SELECT * FROM meals WHERE id=?",String.valueOf(e.id)));
            else editMetric(db.one("SELECT * FROM metrics WHERE id=?",String.valueOf(e.id)));
        };line(entry,shortTime(e.ts)+"  "+e.title,"修改 ›",open);
            Ui.add(entry,Ui.text(this,e.detail,13,Ui.SUB,false),this,2);entry.setOnClickListener(v->open.run());}
    }
    private void showWorkouts() {
        title("训练记录"); hint("力量训练逐组记录；有氧记录时长、距离或强度。"); action("＋ 新建训练",true,()->editWorkout(null));
        List<JSONObject> rows=db.rows("SELECT w.*, COUNT(DISTINCT e.id) exercises, COUNT(s.id) sets_count FROM workouts w LEFT JOIN exercises e ON e.workout_id=w.id LEFT JOIN sets s ON s.exercise_id=e.id GROUP BY w.id ORDER BY w.start_ms DESC LIMIT 100");
        if(rows.isEmpty())hint("还没有训练记录。开始一条训练，再添加项目和组数。");
        for(JSONObject w:rows) { LinearLayout c=card(); long id=w.optLong("id"); line(c,str(w,"type")+" · "+when(w.optLong("start_ms")),"查看 ›",()->{workoutId=id;render();});
            Ui.add(c,Ui.text(this,w.optInt("exercises")+" 个项目 · "+w.optInt("sets_count")+" 组 · "+Math.max(0,(w.optLong("end_ms")-w.optLong("start_ms"))/60000)+" 分钟",13,Ui.SUB,false),this,2); }
    }
    private void editWorkout(JSONObject old) {
        LinearLayout body=Ui.column(this); long endDefault=old==null?defaultRecordTime():old.optLong("end_ms");
        long startDefault=old==null?endDefault:old.optLong("start_ms");
        Ui.TimeChoice start=new Ui.TimeChoice(this,"开始时间",startDefault,false);
        Ui.TimeChoice end=new Ui.TimeChoice(this,"结束时间",endDefault,true);
        Spinner type=spinner(new String[]{"力量","有氧","柔韧","混合"},str(old,"type")); EditText note=Ui.input(this,"可选备注",str(old,"note"),false);
        Ui.add(body,Ui.text(this,"点击时间选择日期和时分；保存前核对下方时长。",12,Ui.SUB,false),this,8);
        Ui.add(body,start.view,this,0);Ui.add(body,end.view,this,0);
        Ui.add(body,Ui.text(this,"按实际训练时长回填开始时间（可选）",12,Ui.SUB,true),this,12);
        HorizontalScrollView durationScroll=new HorizontalScrollView(this);durationScroll.setHorizontalScrollBarEnabled(false);LinearLayout durations=Ui.row(this);durationScroll.addView(durations);
        for(int minutes:new int[]{30,45,60,90}){TextView chip=Ui.button(this,minutes+" 分钟",false,()->start.set(end.get()-minutes*60000L));
            LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-2,-2);p.rightMargin=Ui.dp(this,6);durations.addView(chip,p);}
        Ui.add(body,durationScroll,this,6);
        TextView summary=Ui.text(this,"",13,Ui.TEAL,true);Ui.add(body,summary,this,12);
        Runnable update=()->{long duration=(end.get()-start.get())/60000;summary.setText(duration<=0?"请设置实际开始时间；结束须晚于开始。":"将保存："+when(start.get())+" 至 "+when(end.get())+" · "+duration+" 分钟");};
        start.onChange(update);end.onChange(update);
        Ui.field(body,this,"训练类型",type); Ui.field(body,this,"备注",note);
        Ui.form(this,old==null?"新建训练":"编辑训练",body,"保存",()->{
            long s=start.get(),e=end.get(); if(e<=s)throw new IllegalArgumentException("结束时间必须晚于开始时间");
            if(e>System.currentTimeMillis()+60000)throw new IllegalArgumentException("结束时间不能晚于现在");
            ContentValues v=values("start_ms",s,"end_ms",e,"type",type.getSelectedItem().toString(),"note",Ui.trim(note));
            if(old==null)workoutId=db.add("workouts",v);else {db.update("workouts",old.optLong("id"),v);workoutId=old.optLong("id");} tab="训练";render();
        });
    }
    private Spinner spinner(String[] options,String selected) { Spinner s=new Spinner(this); ArrayAdapter<String> adapter=new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,options); s.setAdapter(adapter); int i=Arrays.asList(options).indexOf(selected); if(i>=0)s.setSelection(i); return s; }
    private void showWorkout() {
        JSONObject w=db.one("SELECT * FROM workouts WHERE id=?",String.valueOf(workoutId)); if(w==null){workoutId=0;render();return;}
        action("‹ 返回训练列表",false,()->{workoutId=0;render();}); title(str(w,"type")+"训练"); hint(when(w.optLong("start_ms"))+"  至  "+when(w.optLong("end_ms")));
        if(!str(w,"note").isEmpty())hint(str(w,"note"));
        LinearLayout tools=card(); line(tools,"编辑时间与备注","编辑",()->editWorkout(w)); line(tools,"删除本次训练","删除",()->Ui.confirm(this,"删除本次训练及全部项目、组数？",()->{db.delete("workouts",workoutId);workoutId=0;render();}));
        action("＋ 添加训练项目",true,this::chooseExercise);
        List<JSONObject> exercises=db.rows("SELECT * FROM exercises WHERE workout_id=? ORDER BY position,id",String.valueOf(workoutId));
        if(exercises.isEmpty())hint("尚无项目。可从分类中滑动选择，也可以打字搜索。");
        for(JSONObject e:exercises) {
            long eid=e.optLong("id"); LinearLayout c=card();
            line(c,str(e,"name")+" · "+str(e,"category"),"删除",()->Ui.confirm(this,"删除项目及其全部组数？",()->{db.delete("exercises",eid);render();}));
            List<JSONObject> sets=db.rows("SELECT * FROM sets WHERE exercise_id=? ORDER BY position,id",String.valueOf(eid));
            int n=1; for(JSONObject set:sets) { long sid=set.optLong("id"); String detail=setDescription(set);
                line(c,"第 "+(n++)+" 组",detail,()->editSet(eid,set));
                TextView del=Ui.text(this,"删除此组",12,Ui.RED,false); del.setOnClickListener(v->Ui.confirm(this,"删除这一组？",()->{db.delete("sets",sid);render();})); Ui.add(c,del,this,0);
            }
            Ui.add(c,Ui.button(this,"＋ 添加一组",false,()->editSet(eid,null)),this,10);
        }
    }
    private String setDescription(JSONObject o) {
        ArrayList<String> parts=new ArrayList<>(); if(o.optInt("reps")>0)parts.add(o.optInt("reps")+" 次"); if(num(o,"weight")>0)parts.add(fmt(num(o,"weight"))+" kg");
        if(o.optInt("duration_sec")>0)parts.add(fmt(o.optInt("duration_sec")/60.0)+" 分钟"); if(num(o,"distance_km")>0)parts.add(fmt(num(o,"distance_km"))+" km"); if(num(o,"rpe")>0)parts.add("RPE "+fmt(num(o,"rpe")));
        return android.text.TextUtils.join(" · ",parts);
    }
    private void editSet(long exerciseId,JSONObject old) {
        editSet(exerciseId,old,null);
    }
    private void editSet(long exerciseId,JSONObject old,Runnable cancel) {
        LinearLayout body=Ui.column(this); EditText reps=Ui.input(this,"次数",old==null?"":String.valueOf(old.optInt("reps")),true);
        EditText weight=Ui.input(this,"kg",old==null?"":fmt(num(old,"weight")),true);
        EditText minutes=Ui.input(this,"分钟",old==null?"":fmt(old.optInt("duration_sec")/60.0),true);
        EditText distance=Ui.input(this,"km",old==null?"":fmt(num(old,"distance_km")),true);
        EditText rpe=Ui.input(this,"1–10，可留空",old==null?"":fmt(num(old,"rpe")),true);
        Ui.field(body,this,"次数",reps); Ui.field(body,this,"重量",weight); Ui.field(body,this,"持续时间",minutes); Ui.field(body,this,"距离",distance); Ui.field(body,this,"主观强度 RPE",rpe);
        Ui.form(this,old==null?"添加一组":"编辑一组",body,"保存",()->{
            double repsValue=Ui.trim(reps).isEmpty()?0:Ui.nonnegative(reps,"次数");if(repsValue!=Math.rint(repsValue)||repsValue>100000)throw new IllegalArgumentException("次数必须是合理的整数");int r=(int)repsValue; double w=Ui.trim(weight).isEmpty()?0:Ui.nonnegative(weight,"重量");
            double m=Ui.trim(minutes).isEmpty()?0:Ui.nonnegative(minutes,"时间"); double d=Ui.trim(distance).isEmpty()?0:Ui.nonnegative(distance,"距离");
            double rp=Ui.trim(rpe).isEmpty()?0:Ui.nonnegative(rpe,"RPE"); if(r==0&&m==0&&d==0)throw new IllegalArgumentException("请填写次数、时间或距离"); if(rp>10)throw new IllegalArgumentException("RPE 不能超过 10");if(m>100000||w>100000||d>100000)throw new IllegalArgumentException("请核对输入数值");
            ContentValues v=values("exercise_id",exerciseId,"reps",r,"weight",w,"duration_sec",(int)Math.round(m*60),"distance_km",d,"rpe",rp,"position",old==null?db.count("sets"):old.optInt("position"));
            if(old==null)db.add("sets",v);else db.update("sets",old.optLong("id"),v);render();
        },cancel);
    }
    private void chooseExercise() {
        LinearLayout layout=Ui.column(this); layout.setPadding(Ui.dp(this,16),Ui.dp(this,8),Ui.dp(this,16),Ui.dp(this,8));
        Ui.add(layout,Ui.text(this,"搜名称，或左右滑动分类；本次已有的项目可直接加组。",12,Ui.SUB,false),this,2);
        EditText search=Ui.input(this,"搜索训练项目，包括历史自定义项目","",false); Ui.add(layout,search,this,5);
        HorizontalScrollView hs=new HorizontalScrollView(this); hs.setHorizontalScrollBarEnabled(false); LinearLayout chips=Ui.row(this); hs.addView(chips); Ui.add(layout,hs,this,10);
        ScrollView scroll=new ScrollView(this); LinearLayout list=Ui.column(this); scroll.addView(list); layout.addView(scroll,new LinearLayout.LayoutParams(-1,Ui.dp(this,360)));
        LinkedHashMap<String,String[]> catalog=new LinkedHashMap<>();
        for(String[] group:EXERCISES)for(int i=1;i<group.length;i++)catalog.put(group[i].toLowerCase(Locale.ROOT),new String[]{group[0],group[i]});
        List<JSONObject> recent=db.rows("SELECT name,category,MAX(id) last_id,COUNT(*) uses FROM exercises GROUP BY LOWER(name) ORDER BY last_id DESC LIMIT 100");
        for(JSONObject o:recent){String name=str(o,"name");catalog.putIfAbsent(name.toLowerCase(Locale.ROOT),new String[]{str(o,"category"),name});}
        List<JSONObject> current=db.rows("SELECT id,name,category FROM exercises WHERE workout_id=? ORDER BY position,id",String.valueOf(workoutId));
        HashMap<String,Long> currentIds=new HashMap<>();for(JSONObject e:current)currentIds.put(str(e,"name").toLowerCase(Locale.ROOT),e.optLong("id"));
        String[] category={recent.isEmpty()?"全部":"最近"}; final Runnable[] refreshRef=new Runnable[1];
        AlertDialog dialog=new AlertDialog.Builder(this).setTitle("选择训练项目").setView(layout).setNegativeButton("取消",null).create();
        Runnable refresh=()->{
            chips.removeAllViews(); for(String cat:new String[]{"最近","常用","本次","全部","胸","背","腿","肩臂","核心","有氧","拉伸","自定义"}) {
                if(cat.equals("本次")&&current.isEmpty())continue;
                TextView chip=Ui.button(this,cat,cat.equals(category[0]),()->{category[0]=cat;if(Ui.trim(search).isEmpty())refreshRef[0].run();else search.setText("");});
                LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(-2,-2); cp.rightMargin=Ui.dp(this,6);chips.addView(chip,cp);
            }
            list.removeAllViews(); String q=Ui.trim(search).toLowerCase(Locale.ROOT); LinkedHashMap<String,String[]> found=new LinkedHashMap<>();
            if(!q.isEmpty()){for(String[] option:catalog.values())if(option[1].toLowerCase(Locale.ROOT).contains(q))found.put(option[1].toLowerCase(Locale.ROOT),option);}
            else if(category[0].equals("最近")){for(int i=0;i<Math.min(12,recent.size());i++){JSONObject e=recent.get(i);String name=str(e,"name");found.put(name.toLowerCase(Locale.ROOT),catalog.get(name.toLowerCase(Locale.ROOT)));}}
            else if(category[0].equals("常用")){
                ArrayList<JSONObject> frequent=new ArrayList<>(recent);Collections.sort(frequent,(a,b)->{int n=Integer.compare(b.optInt("uses"),a.optInt("uses"));return n!=0?n:Long.compare(b.optLong("last_id"),a.optLong("last_id"));});
                for(int i=0;i<Math.min(12,frequent.size());i++){String name=str(frequent.get(i),"name");found.put(name.toLowerCase(Locale.ROOT),catalog.get(name.toLowerCase(Locale.ROOT)));}
            }else if(category[0].equals("本次")){for(JSONObject e:current){String name=str(e,"name");found.put(name.toLowerCase(Locale.ROOT),catalog.get(name.toLowerCase(Locale.ROOT)));}}
            else {for(String[] option:catalog.values())if(category[0].equals("全部")||category[0].equals(option[0]))found.put(option[1].toLowerCase(Locale.ROOT),option);}
            for(String[] option:found.values()) { if(option==null)continue;String key=option[1].toLowerCase(Locale.ROOT);boolean inCurrent=currentIds.containsKey(key);
                LinearLayout row=Ui.column(this);row.setPadding(Ui.dp(this,10),Ui.dp(this,10),Ui.dp(this,10),Ui.dp(this,10));row.setBackground(Ui.box(this,Ui.WHITE,12));
                Ui.add(row,Ui.text(this,option[1],16,Ui.INK,true),this,0);
                Ui.add(row,Ui.text(this,option[0]+(inCurrent?" · 本次已有，点击加组":" · 点击添加项目"),12,inCurrent?Ui.TEAL:Ui.SUB,false),this,3);
                row.setOnClickListener(v->{dialog.dismiss();if(inCurrent)editSet(currentIds.get(key),null);else addExercise(option[1],option[0]);});Ui.add(list,row,this,5);
            }
            if(!q.isEmpty()&&!catalog.containsKey(q))Ui.add(list,Ui.button(this,"＋ 新建自定义项目："+Ui.trim(search),false,()->{dialog.dismiss();addExercise(Ui.trim(search),"自定义");}),this,10);
            if(found.isEmpty()&&q.isEmpty())Ui.add(list,Ui.text(this,"这里还没有项目。可切换分类或输入名称搜索。",14,Ui.SUB,false),this,12);
            int rowCount=found.size()+(!q.isEmpty()&&!catalog.containsKey(q)?1:0);
            scroll.getLayoutParams().height=Ui.dp(this,Math.min(360,Math.max(110,rowCount*72+18)));scroll.requestLayout();
        };
        refreshRef[0]=refresh;
        search.addTextChangedListener(watch(refresh)); refresh.run(); dialog.show();
    }
    private TextWatcher watch(Runnable action) { return new TextWatcher(){public void beforeTextChanged(CharSequence s,int st,int c,int a){}public void onTextChanged(CharSequence s,int st,int before,int count){action.run();}public void afterTextChanged(Editable e){}}; }
    private void addExercise(String name,String category) {
        if(name.trim().isEmpty())return; long n=db.rows("SELECT id FROM exercises WHERE workout_id=?",String.valueOf(workoutId)).size();
        long id=db.add("exercises",values("workout_id",workoutId,"name",name.trim(),"category",category,"position",n));render();
        editSet(id,null,()->{if(db.rows("SELECT id FROM sets WHERE exercise_id=? LIMIT 1",String.valueOf(id)).isEmpty()){db.delete("exercises",id);render();}});
    }

    private void showMetrics() {
        title("身体记录"); hint("保留每次测量的时间、数值和设备；图表不作医学诊断。"); action("＋ 添加指标",true,()->editMetric(null));
        List<JSONObject> records=db.rows("SELECT * FROM metrics ORDER BY ts DESC,id DESC LIMIT 150"); if(records.isEmpty())hint("还没有身体记录。");
        for(JSONObject o:records){ long id=o.optLong("id"); LinearLayout c=card();
            line(c,str(o,"type")+" · "+when(o.optLong("ts")),metricValue(o),()->editMetric(o));
            if(!str(o,"source").isEmpty())Ui.add(c,Ui.text(this,"设备："+str(o,"source"),12,Ui.SUB,false),this,2);
            if(!str(o,"note").isEmpty())Ui.add(c,Ui.text(this,str(o,"note"),12,Ui.SUB,false),this,2);
            TextView del=Ui.text(this,"删除",12,Ui.RED,false);del.setOnClickListener(v->Ui.confirm(this,"删除这条测量记录？",()->{db.delete("metrics",id);render();}));Ui.add(c,del,this,8);
        }
    }
    private String metricValue(JSONObject o) {
        String type=str(o,"type"); if(type.equals("血压"))return fmt(num(o,"sys"))+" / "+fmt(num(o,"dia"))+" mmHg";
        return fmt(num(o,"value"))+(type.equals("体重")?" kg":" %");
    }
    private void editMetric(JSONObject old) {
        LinearLayout body=Ui.column(this); Spinner type=spinner(new String[]{"体重","体脂率","血压","血氧"},str(old,"type"));
        Ui.TimeChoice time=new Ui.TimeChoice(this,"测量时间",old==null?defaultRecordTime():old.optLong("ts"),true);
        EditText value=Ui.input(this,"数值",old==null||old.isNull("value")?"":fmt(num(old,"value")),true);
        EditText sys=Ui.input(this,"收缩压 mmHg",old==null||old.isNull("sys")?"":fmt(num(old,"sys")),true);
        EditText dia=Ui.input(this,"舒张压 mmHg",old==null||old.isNull("dia")?"":fmt(num(old,"dia")),true);
        EditText source=Ui.input(this,"如：上臂血压计 / 体脂秤",str(old,"source"),false); EditText note=Ui.input(this,"如：晨起 / 饭后 / 运动后",str(old,"note"),false);
        Ui.field(body,this,"指标",type);Ui.add(body,time.view,this,0);
        Ui.add(body,Ui.text(this,"点击时间修改日期和时分；保存时以显示的时间为准。",12,Ui.SUB,false),this,7);
        LinearLayout single=Ui.column(this);Ui.field(single,this,"数值（体重 kg，其他 %）",value); Ui.add(body,single,this,0);
        LinearLayout pressure=Ui.column(this);Ui.field(pressure,this,"收缩压",sys);Ui.field(pressure,this,"舒张压",dia);Ui.add(body,pressure,this,0);
        type.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener(){
            public void onItemSelected(android.widget.AdapterView<?> p,View v,int pos,long id){boolean bp=type.getSelectedItem().toString().equals("血压");single.setVisibility(bp?View.GONE:View.VISIBLE);pressure.setVisibility(bp?View.VISIBLE:View.GONE);}
            public void onNothingSelected(android.widget.AdapterView<?> p){}
        });
        Ui.field(body,this,"测量设备（可选）",source);Ui.field(body,this,"测量条件 / 备注（可选）",note);
        Ui.form(this,old==null?"添加身体指标":"编辑身体指标",body,"保存",()->{
            String t=type.getSelectedItem().toString();double x=0,s=0,d=0;
            if(t.equals("血压")){s=Ui.positive(sys,"收缩压");d=Ui.positive(dia,"舒张压");if(s<=d)throw new IllegalArgumentException("请核对收缩压与舒张压");}
            else {x=Ui.positive(value,"数值");if((t.equals("血氧")||t.equals("体脂率"))&&x>100)throw new IllegalArgumentException("百分比不能超过 100");}
            if(time.get()>System.currentTimeMillis()+60000)throw new IllegalArgumentException("测量时间不能晚于现在");
            ContentValues v=values("ts",time.get(),"type",t,"sys",t.equals("血压")?s:null,"dia",t.equals("血压")?d:null,"value",t.equals("血压")?null:x,"source",Ui.trim(source),"note",Ui.trim(note));
            if(old==null)db.add("metrics",v);else db.update("metrics",old.optLong("id"),v);tab="身体";render();
        });
    }
    private void showMeals() {
        title("饮食记录");hint("照片帮助回想，热量按选中的食物与份量计算。");action("＋ 记录一餐",true,()->editMeal(null));
        List<JSONObject> meals=db.rows("SELECT m.*, COUNT(i.id) item_count, COALESCE(SUM(i.grams*i.kcal100/100),0) kcal FROM meals m LEFT JOIN items i ON i.meal_id=m.id GROUP BY m.id ORDER BY m.ts DESC LIMIT 150");
        if(meals.isEmpty())hint("还没有餐食记录。");
        for(JSONObject m:meals){long id=m.optLong("id");LinearLayout c=card();line(c,str(m,"slot")+" · "+when(m.optLong("ts")),"查看 ›",()->{mealId=id;render();});
            Ui.add(c,Ui.text(this,m.optInt("item_count")+" 种食物 · "+fmt(num(m,"kcal"))+" kcal"+(str(m,"photo").isEmpty()?"":" · 有照片"),13,Ui.SUB,false),this,2);
        }
    }
    private void editMeal(JSONObject old) {
        LinearLayout body=Ui.column(this);Spinner slot=spinner(new String[]{"早餐","午餐","晚餐","加餐","其他"},str(old,"slot"));
        Ui.TimeChoice time=new Ui.TimeChoice(this,"用餐时间",old==null?defaultRecordTime():old.optLong("ts"),true);
        EditText note=Ui.input(this,"如：食堂 / 自制 / 外卖",str(old,"note"),false);
        Ui.field(body,this,"餐次",slot);Ui.add(body,time.view,this,0);Ui.add(body,Ui.text(this,"点击时间修改日期和时分；保存时以显示的时间为准。",12,Ui.SUB,false),this,7);Ui.field(body,this,"文字备注（可选）",note);
        Ui.form(this,old==null?"记录一餐":"编辑餐食",body,"保存",()->{
            if(time.get()>System.currentTimeMillis()+60000)throw new IllegalArgumentException("用餐时间不能晚于现在");
            ContentValues v=values("ts",time.get(),"slot",slot.getSelectedItem().toString(),"note",Ui.trim(note));
            if(old==null)mealId=db.add("meals",v);else {mealId=old.optLong("id");db.update("meals",mealId,v);} tab="饮食";render();
        });
    }
    private void showMeal() {
        JSONObject m=db.one("SELECT * FROM meals WHERE id=?",String.valueOf(mealId));if(m==null){mealId=0;render();return;}
        action("‹ 返回饮食列表",false,()->{mealId=0;render();});title(str(m,"slot")+" · "+date(m.optLong("ts")));hint(when(m.optLong("ts")));
        if(!str(m,"note").isEmpty())hint(str(m,"note"));
        LinearLayout options=card();line(options,"编辑餐次、时间与文字","编辑",()->editMeal(m));
        line(options,"添加或更换餐食照片","选取",()->pickPhoto(mealId));
        line(options,"删除整餐","删除",()->Ui.confirm(this,"删除这餐及其食物和照片？",()->{File f=photoFile(str(m,"photo"));db.delete("meals",mealId);if(f!=null)f.delete();mealId=0;render();}));
        String photo=str(m,"photo"); File imageFile=photoFile(photo); if(imageFile!=null&&imageFile.exists()){
            ImageView iv=new ImageView(this); iv.setImageBitmap(BitmapFactory.decodeFile(imageFile.getAbsolutePath()));iv.setAdjustViewBounds(true);iv.setMaxHeight(Ui.dp(this,260));iv.setScaleType(ImageView.ScaleType.CENTER_CROP);Ui.add(content,iv,this,12);
        }
        action("＋ 添加食物",true,this::chooseFood);
        List<JSONObject> items=db.rows("SELECT * FROM items WHERE meal_id=? ORDER BY id",String.valueOf(mealId));
        if(items.isEmpty())hint("这餐还没有食物。可以搜索、滑动分类或新建食物。");
        double kcal=0,protein=0,carb=0,fat=0;
        for(JSONObject i:items){long id=i.optLong("id");double grams=num(i,"grams");kcal+=FoodMath.scaled(num(i,"kcal100"),grams);protein+=FoodMath.scaled(num(i,"protein100"),grams);carb+=FoodMath.scaled(num(i,"carb100"),grams);fat+=FoodMath.scaled(num(i,"fat100"),grams);
            LinearLayout c=card();line(c,str(i,"name")+" · "+fmt(grams)+" g",fmt(FoodMath.scaled(num(i,"kcal100"),grams))+" kcal",()->editItem(i));
            Ui.add(c,Ui.text(this,"蛋白 "+fmt(FoodMath.scaled(num(i,"protein100"),grams))+" g · 碳水 "+fmt(FoodMath.scaled(num(i,"carb100"),grams))+" g · 脂肪 "+fmt(FoodMath.scaled(num(i,"fat100"),grams))+" g",12,Ui.SUB,false),this,2);
            TextView del=Ui.text(this,"删除",12,Ui.RED,false);del.setOnClickListener(v->Ui.confirm(this,"删除这项食物？",()->{db.delete("items",id);render();}));Ui.add(c,del,this,6);
        }
        LinearLayout sum=card();Ui.add(sum,Ui.text(this,"本餐已记录合计",16,Ui.INK,true),this,0);
        Ui.add(sum,Ui.text(this,fmt(kcal)+" kcal   蛋白 "+fmt(protein)+" g   碳水 "+fmt(carb)+" g   脂肪 "+fmt(fat)+" g",14,Ui.TEAL,true),this,8);
        hint("这是食物库或手动营养值 × 录入份量的结果；未知配料与用油不会自动计入。");
        final double totalKcal=kcal,totalProtein=protein,totalCarb=carb,totalFat=fat;
        if(!items.isEmpty())action("保存为可复用食谱",false,()->saveRecipe(items,totalKcal,totalProtein,totalCarb,totalFat));
    }
    private File photoFile(String name){if(name==null||name.isEmpty()||name.contains("/")||name.contains("\\")||name.contains(".."))return null;return new File(new File(getFilesDir(),"photos"),name);}
    private void pickPhoto(long id){pendingPhotoMeal=id;new AlertDialog.Builder(this).setTitle("添加餐食照片").setItems(new String[]{"从手机选取","用相机拍摄"},(d,n)->{
        if(n==0){Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);i.setType("image/*");i.addCategory(Intent.CATEGORY_OPENABLE);startActivityForResult(i,PICK_PHOTO);}
        else {Intent i=new Intent(MediaStore.ACTION_IMAGE_CAPTURE);if(i.resolveActivity(getPackageManager())!=null)startActivityForResult(i,TAKE_PHOTO);else toast("未找到可用相机");}
    }).show();}
    @Override protected void onActivityResult(int requestCode,int resultCode,Intent data){super.onActivityResult(requestCode,resultCode,data);if(resultCode!=RESULT_OK||data==null)return;
        try{
            if(requestCode==PICK_PHOTO||requestCode==TAKE_PHOTO){
                Bitmap image;if(requestCode==TAKE_PHOTO){image=(Bitmap)data.getExtras().get("data");}else{
                    Uri uri=data.getData();BitmapFactory.Options bounds=new BitmapFactory.Options();bounds.inJustDecodeBounds=true;
                    try(InputStream in=getContentResolver().openInputStream(uri)){BitmapFactory.decodeStream(in,null,bounds);}
                    if(bounds.outWidth<=0||bounds.outHeight<=0)throw new IllegalArgumentException("不能读取这张图片");
                    BitmapFactory.Options options=new BitmapFactory.Options();options.inSampleSize=1;while(Math.max(bounds.outWidth,bounds.outHeight)/options.inSampleSize>1600)options.inSampleSize*=2;
                    try(InputStream in=getContentResolver().openInputStream(uri)){image=BitmapFactory.decodeStream(in,null,options);}
                }
                if(image==null)throw new IllegalArgumentException("不能读取这张图片");
                File dir=new File(getFilesDir(),"photos");if(!dir.exists()&&!dir.mkdirs())throw new IllegalStateException("不能创建照片目录");
                String name="meal_"+pendingPhotoMeal+"_"+System.currentTimeMillis()+".jpg";File target=new File(dir,name);
                try(FileOutputStream out=new FileOutputStream(target)){if(!image.compress(Bitmap.CompressFormat.JPEG,82,out))throw new IllegalStateException("照片保存失败");}
                image.recycle();JSONObject meal=db.one("SELECT photo FROM meals WHERE id=?",String.valueOf(pendingPhotoMeal));
                if(meal==null){target.delete();return;}File old=photoFile(str(meal,"photo"));db.update("meals",pendingPhotoMeal,values("photo",name));if(old!=null)old.delete();render();
            } else if(requestCode==EXPORT_BACKUP){Backup.exportZip(this,db,data.getData());toast("完整备份已导出到你选择的位置");}
            else if(requestCode==IMPORT_BACKUP){Uri uri=data.getData();new AlertDialog.Builder(this).setTitle("恢复备份").setMessage("这会用备份文件替换本机全部训练、身体、饮食、照片和自定义食物。建议先导出当前备份。")
                    .setNegativeButton("取消",null).setPositiveButton("确认恢复",(d,w)->{try{Backup.importZip(this,db,uri);workoutId=0;mealId=0;render();toast("备份已恢复");}catch(Exception e){error(e);}}).show();}
            else if(requestCode==EXPORT_CSV){Backup.exportCsv(this,db,data.getData());toast("CSV 已导出到你选择的位置");}
        }catch(Exception e){error(e);}
    }
    private void error(Exception e){new AlertDialog.Builder(this).setTitle("操作未完成").setMessage(e.getMessage()==null?e.toString():e.getMessage()).setPositiveButton("知道了",null).show();}
    private void chooseFood() {
        LinearLayout layout=Ui.column(this);layout.setPadding(Ui.dp(this,16),Ui.dp(this,8),Ui.dp(this,16),Ui.dp(this,8));
        EditText search=Ui.input(this,"搜索食物中文名或英文名","",false);Ui.add(layout,search,this,2);
        HorizontalScrollView hs=new HorizontalScrollView(this);hs.setHorizontalScrollBarEnabled(false);LinearLayout chips=Ui.row(this);hs.addView(chips);Ui.add(layout,hs,this,10);
        ScrollView scroll=new ScrollView(this);LinearLayout list=Ui.column(this);scroll.addView(list);layout.addView(scroll,new LinearLayout.LayoutParams(-1,Ui.dp(this,360)));
        AlertDialog dialog=new AlertDialog.Builder(this).setTitle("选择食物").setView(layout).setNegativeButton("关闭",null).create();
        String[] category={db.count("items")==0?"全部":"最近"}; final Runnable[] refreshRef=new Runnable[1]; Runnable refresh=()->{
            chips.removeAllViews();for(String cat:new String[]{"最近","收藏","全部","主食","蛋奶","肉鱼豆","水果","蔬菜","坚果油脂","调味饮料","自定义"}){
                TextView chip=Ui.button(this,cat,cat.equals(category[0]),()->{category[0]=cat;search.setText("");});LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(-2,-2);cp.rightMargin=Ui.dp(this,6);chips.addView(chip,cp);
            }
            list.removeAllViews();String q=Ui.trim(search);List<JSONObject> foods;
            if(!q.isEmpty())foods=db.rows("SELECT * FROM foods WHERE name LIKE ? OR english LIKE ? ORDER BY favorite DESC,custom DESC,name LIMIT 80","%"+q+"%","%"+q+"%");
            else if(category[0].equals("最近"))foods=db.rows("SELECT f.* FROM foods f JOIN items i ON i.food_id=f.id GROUP BY f.id ORDER BY MAX(i.id) DESC LIMIT 20");
            else if(category[0].equals("收藏"))foods=db.rows("SELECT * FROM foods WHERE favorite=1 ORDER BY name LIMIT 80");
            else if(category[0].equals("全部"))foods=db.rows("SELECT * FROM foods ORDER BY custom DESC,category,name LIMIT 100");
            else if(category[0].equals("自定义"))foods=db.rows("SELECT * FROM foods WHERE custom=1 ORDER BY name LIMIT 80");
            else foods=db.rows("SELECT * FROM foods WHERE category=? ORDER BY name LIMIT 80",category[0]);
            for(JSONObject f:foods){LinearLayout row=Ui.row(this);TextView label=Ui.text(this,str(f,"name")+"\n"+fmt(num(f,"kcal"))+" kcal/100g",14,Ui.INK,false);
                label.setPadding(0,Ui.dp(this,10),Ui.dp(this,8),Ui.dp(this,10));row.addView(label,new LinearLayout.LayoutParams(0,-2,1));label.setOnClickListener(v->{dialog.dismiss();addItem(f);});
                TextView fav=Ui.text(this,f.optInt("favorite")==1?"★":"☆",24,Ui.TEAL,false);fav.setPadding(Ui.dp(this,8),0,Ui.dp(this,8),0);fav.setOnClickListener(v->{db.update("foods",f.optLong("id"),values("favorite",f.optInt("favorite")==1?0:1));refreshRef[0].run();});row.addView(fav);Ui.add(list,row,this,2);
            }
            if(foods.isEmpty())Ui.add(list,Ui.text(this,"暂无条目。试试搜索，或新增自定义食物。",14,Ui.SUB,false),this,12);
            Ui.add(list,Ui.button(this,"＋ 新增自定义食物",false,()->{dialog.dismiss();editCustomFood(null,true);}),this,12);
            scroll.getLayoutParams().height=Ui.dp(this,Math.min(360,Math.max(125,foods.size()*70+65)));scroll.requestLayout();
        };
        refreshRef[0]=refresh;search.addTextChangedListener(watch(refresh));refresh.run();dialog.show();
    }
    private void addItem(JSONObject food){
        if(mealId==0)return;LinearLayout body=Ui.column(this);EditText grams=Ui.input(this,"克", "100",true);Ui.field(body,this,str(food,"name")+" · 份量（克）",grams);
        HorizontalScrollView hs=new HorizontalScrollView(this);LinearLayout quick=Ui.row(this);hs.addView(quick);for(int g:new int[]{50,100,150,200,250}){
            TextView chip=Ui.button(this,g+"g",false,()->grams.setText(String.valueOf(g)));LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-2,-2);p.rightMargin=Ui.dp(this,5);quick.addView(chip,p);
        }Ui.add(body,hs,this,10);
        Ui.add(body,Ui.text(this,"每 100g："+fmt(num(food,"kcal"))+" kcal · 蛋白 "+fmt(num(food,"protein"))+"g · 碳水 "+fmt(num(food,"carb"))+"g · 脂肪 "+fmt(num(food,"fat"))+"g",12,Ui.SUB,false),this,12);
        Ui.add(body,Ui.text(this,"来源："+str(food,"source"),11,Ui.SUB,false),this,6);
        String recipe=str(food,"recipe_json");if(!recipe.isEmpty())Ui.add(body,Ui.text(this,"自制食谱，营养值按保存时的配方快照计算。",11,Ui.SUB,false),this,5);
        Ui.form(this,"添加食物",body,"加入这餐",()->{
            double g=Ui.positive(grams,"份量");if(g>10000)throw new IllegalArgumentException("单项份量请不超过 10000 g");
            db.add("items",values("meal_id",mealId,"food_id",food.optLong("id"),"name",str(food,"name"),"grams",g,
                "kcal100",num(food,"kcal"),"protein100",num(food,"protein"),"carb100",num(food,"carb"),"fat100",num(food,"fat"),"source",str(food,"source")));render();
        });
    }
    private void editItem(JSONObject item){
        LinearLayout body=Ui.column(this);EditText grams=Ui.input(this,"克",fmt(num(item,"grams")),true);Ui.field(body,this,str(item,"name")+" · 份量（克）",grams);
        Ui.form(this,"修改食物份量",body,"保存",()->{double g=Ui.positive(grams,"份量");if(g>10000)throw new IllegalArgumentException("单项份量请不超过 10000 g");db.update("items",item.optLong("id"),values("grams",g));render();});
    }
    private void editCustomFood(JSONObject old,boolean addToCurrentMeal){
        LinearLayout body=Ui.column(this);EditText name=Ui.input(this,"食物名称",str(old,"name"),false);Spinner category=spinner(new String[]{"自定义","主食","蛋奶","肉鱼豆","水果","蔬菜","坚果油脂","调味饮料"},str(old,"category"));
        EditText kcal=Ui.input(this,"kcal",old==null?"":fmt(num(old,"kcal")),true);EditText protein=Ui.input(this,"g",old==null?"":fmt(num(old,"protein")),true);
        EditText carb=Ui.input(this,"g",old==null?"":fmt(num(old,"carb")),true);EditText fat=Ui.input(this,"g",old==null?"":fmt(num(old,"fat")),true);
        EditText source=Ui.input(this,"例如包装营养标签 / 自行估算",str(old,"source"),false);
        Ui.field(body,this,"名称",name);Ui.field(body,this,"分类",category);Ui.add(body,Ui.text(this,"下面数值均按每 100g 食物填写。",12,Ui.SUB,false),this,12);
        Ui.field(body,this,"热量 kcal",kcal);Ui.field(body,this,"蛋白质 g",protein);Ui.field(body,this,"碳水化合物 g",carb);Ui.field(body,this,"脂肪 g",fat);Ui.field(body,this,"数据来源",source);
        Ui.form(this,old==null?"新建自定义食物":"编辑自定义食物",body,"保存",()->{
            if(Ui.trim(name).isEmpty())throw new IllegalArgumentException("请填写食物名称");
            ContentValues v=values("name",Ui.trim(name),"english","","category",category.getSelectedItem().toString(),"kcal",Ui.nonnegative(kcal,"热量"),"protein",Ui.nonnegative(protein,"蛋白质"),"carb",Ui.nonnegative(carb,"碳水"),"fat",Ui.nonnegative(fat,"脂肪"),"source",Ui.trim(source).isEmpty()?"手动录入 · 来源未注明":Ui.trim(source),"custom",1);
            JSONObject f;if(old==null){long id=db.add("foods",v);f=db.one("SELECT * FROM foods WHERE id=?",String.valueOf(id));}else{db.update("foods",old.optLong("id"),v);f=db.one("SELECT * FROM foods WHERE id=?",String.valueOf(old.optLong("id")));}
            if(addToCurrentMeal&&mealId!=0)addItem(f);else render();
        });
    }
    private void saveRecipe(List<JSONObject> items,double kcal,double protein,double carb,double fat){
        LinearLayout body=Ui.column(this);EditText name=Ui.input(this,"如：我的番茄炒蛋","",false);double sumGrams=0;for(JSONObject i:items)sumGrams+=num(i,"grams");
        EditText yield=Ui.input(this,"成品重量 g",fmt(sumGrams),true);Ui.field(body,this,"食谱名称",name);Ui.field(body,this,"成品总重量（克）",yield);
        Ui.add(body,Ui.text(this,"烹调失水、吸水会改变成品重量；请尽量称重。未录入的油、酱料不会计入。",12,Ui.SUB,false),this,12);
        Ui.form(this,"保存为可复用食谱",body,"保存",()->{
            if(Ui.trim(name).isEmpty())throw new IllegalArgumentException("请填写食谱名称");double g=Ui.positive(yield,"成品重量");
            org.json.JSONArray recipe=new org.json.JSONArray();for(JSONObject i:items)recipe.put(i);
            db.add("foods",values("name",Ui.trim(name),"english","","category","自定义","kcal",FoodMath.per100g(kcal,g),"protein",FoodMath.per100g(protein,g),"carb",FoodMath.per100g(carb,g),"fat",FoodMath.per100g(fat,g),"source","自制食谱 · "+date(System.currentTimeMillis()),"custom",1,"recipe_json",recipe.toString()));
            toast("食谱已保存到自定义食物");render();
        });
    }
    private static final class Daily { int workouts,meals;double kcal;Double weight; }
    private void showReview() {
        title("定期回看");hint("先看记录覆盖率，再看趋势；未记录的日子不会被当作零摄入。");
        LinearLayout switches=Ui.row(this);TextView week=Ui.button(this,"本周",!monthly,()->{monthly=false;render();});TextView month=Ui.button(this,"本月",monthly,()->{monthly=true;render();});
        Ui.equalWeight(switches,week);switches.addView(new View(this),new LinearLayout.LayoutParams(Ui.dp(this,8),1));Ui.equalWeight(switches,month);Ui.add(content,switches,this,12);
        Calendar cal=Calendar.getInstance();long now=System.currentTimeMillis();cal.setTimeInMillis(now);
        if(monthly){cal.set(Calendar.DAY_OF_MONTH,1);}else{int dow=cal.get(Calendar.DAY_OF_WEEK);cal.add(Calendar.DAY_OF_MONTH,-((dow+5)%7));}
        long start=dayStart(cal.getTimeInMillis());if(monthly)cal.add(Calendar.MONTH,1);else cal.add(Calendar.DAY_OF_MONTH,7);long end=dayStart(cal.getTimeInMillis());
        String a=String.valueOf(start),b=String.valueOf(end);
        JSONObject training=db.one("SELECT COUNT(*) n, COALESCE(SUM((end_ms-start_ms)/60000.0),0) minutes FROM workouts WHERE start_ms>=? AND start_ms<?",a,b);
        JSONObject diet=db.one("SELECT COUNT(DISTINCT m.id) meals, COALESCE(SUM(i.grams*i.kcal100/100),0) kcal, COALESCE(SUM(i.grams*i.protein100/100),0) protein FROM meals m LEFT JOIN items i ON i.meal_id=m.id WHERE m.ts>=? AND m.ts<?",a,b);
        LinearLayout summary=card();Ui.add(summary,Ui.text(this,date(start)+" 至 "+date(end-1),16,Ui.INK,true),this,0);
        line(summary,"训练",training.optInt("n")+" 次 · "+fmt(num(training,"minutes"))+" 分钟",null);
        line(summary,"已记餐食",diet.optInt("meals")+" 餐",null);
        line(summary,"已记热量",fmt(num(diet,"kcal"))+" kcal",null);
        line(summary,"已记蛋白质",fmt(num(diet,"protein"))+" g",null);
        List<JSONObject> metrics=db.rows("SELECT * FROM metrics WHERE ts>=? AND ts<? ORDER BY ts",a,b);
        for(String type:new String[]{"体重","体脂率","血压","血氧"}){
            ArrayList<JSONObject> series=new ArrayList<>();ArrayList<Double> points=new ArrayList<>();for(JSONObject m:metrics)if(type.equals(str(m,"type"))){series.add(m);points.add(num(m,type.equals("血压")?"sys":"value"));}
            if(series.isEmpty())continue;LinearLayout c=card();Ui.add(c,Ui.text(this,type+" · "+series.size()+" 次测量",16,Ui.INK,true),this,0);
            if(points.size()>1)Ui.add(c,new TrendView(this,points),this,8);
            for(int i=Math.max(0,series.size()-6);i<series.size();i++){JSONObject m=series.get(i);line(c,when(m.optLong("ts")),metricValue(m),null);}
        }
        HashMap<String,Daily> daily=new HashMap<>();
        for(JSONObject w:db.rows("SELECT start_ms FROM workouts WHERE start_ms>=? AND start_ms<?",a,b)){String key=date(w.optLong("start_ms"));Daily d=daily.get(key);if(d==null){d=new Daily();daily.put(key,d);}d.workouts++;}
        for(JSONObject m:db.rows("SELECT m.ts,COALESCE(SUM(i.grams*i.kcal100/100),0) kcal FROM meals m LEFT JOIN items i ON i.meal_id=m.id WHERE m.ts>=? AND m.ts<? GROUP BY m.id",a,b)){
            String key=date(m.optLong("ts"));Daily d=daily.get(key);if(d==null){d=new Daily();daily.put(key,d);}d.meals++;d.kcal+=num(m,"kcal");
        }
        for(JSONObject m:metrics)if(str(m,"type").equals("体重")){String key=date(m.optLong("ts"));Daily d=daily.get(key);if(d==null){d=new Daily();daily.put(key,d);}d.weight=num(m,"value");}
        LinearLayout table=card();Ui.add(table,Ui.text(this,"逐日表格",16,Ui.INK,true),this,0);
        for(long t=start;t<=now&&t<end;){String key=date(t);Daily d=daily.get(key);
            String value=(d==null?"训练 0 · 饮食未记":("训练 "+d.workouts+" · "+(d.meals==0?"饮食未记":("已记 "+fmt(d.kcal)+" kcal"))))+(d!=null&&d.weight!=null?" · "+fmt(d.weight)+" kg":"");
            line(table,key,value,null);Calendar next=Calendar.getInstance();next.setTimeInMillis(t);next.add(Calendar.DAY_OF_MONTH,1);t=dayStart(next.getTimeInMillis());
        }
        hint("训练时长包含训练开始与结束之间的休息；热量仅统计已录入食物。可在“数据与设置”导出原始 CSV。");
    }
    private void showSettings() {
        String[] options={"导出完整本地备份（含照片）","从备份恢复（替换本机数据）","导出原始 CSV","管理自定义食物","关于数据与来源"};
        new AlertDialog.Builder(this).setTitle("数据与设置").setItems(options,(d,which)->{
            if(which==0){Intent i=new Intent(Intent.ACTION_CREATE_DOCUMENT);i.addCategory(Intent.CATEGORY_OPENABLE);i.setType("application/zip");i.putExtra(Intent.EXTRA_TITLE,"练刻备份-"+date(System.currentTimeMillis())+".zip");startActivityForResult(i,EXPORT_BACKUP);}
            else if(which==1){Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);i.addCategory(Intent.CATEGORY_OPENABLE);i.setType("application/zip");startActivityForResult(i,IMPORT_BACKUP);}
            else if(which==2){Intent i=new Intent(Intent.ACTION_CREATE_DOCUMENT);i.addCategory(Intent.CATEGORY_OPENABLE);i.setType("text/csv");i.putExtra(Intent.EXTRA_TITLE,"练刻原始记录-"+date(System.currentTimeMillis())+".csv");startActivityForResult(i,EXPORT_CSV);}
            else if(which==3)manageCustomFoods();
            else new AlertDialog.Builder(this).setTitle("本机数据说明").setMessage("本应用没有账号、云同步或网络权限。食物库是离线的 USDA 基础食物数据和你新增的食物；混合菜、照片与未称重份量会影响营养估算。卸载应用前请主动导出完整备份。身体图表只展示记录，不提供诊断。") .setPositiveButton("知道了",null).show();
        }).show();
    }
    private void manageCustomFoods(){
        List<JSONObject> foods=db.rows("SELECT * FROM foods WHERE custom=1 ORDER BY id DESC LIMIT 100");String[] names=new String[foods.size()+1];names[0]="＋ 新建自定义食物";for(int i=0;i<foods.size();i++)names[i+1]=str(foods.get(i),"name");
        new AlertDialog.Builder(this).setTitle("自定义食物").setItems(names,(d,which)->{
            if(which==0){editCustomFood(null,false);return;}
            JSONObject food=foods.get(which-1);new AlertDialog.Builder(this).setTitle(str(food,"name")).setItems(new String[]{"编辑","查看来源与配方","删除"},(x,action)->{
                if(action==0)editCustomFood(food,false);
                else if(action==1)new AlertDialog.Builder(this).setMessage(str(food,"source")+(str(food,"recipe_json").isEmpty()?"":"\n\n配方快照："+str(food,"recipe_json"))).setPositiveButton("关闭",null).show();
                else Ui.confirm(this,"删除食物库条目？历史餐食快照会保留。",()->{db.delete("foods",food.optLong("id"));render();});
            }).show();
        }).show();
    }
}
