package com.fitlog.local;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.BufferedWriter;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public final class Backup {
    private Backup() {}
    private static final String FORMAT="fitlog-local-backup";
    private static final String[] IMPORT_ORDER={"foods","workouts","exercises","sets","metrics","meals","items"};
    private static final String[] DELETE_ORDER={"items","sets","exercises","meals","workouts","metrics","foods"};

    public static void exportZip(Context context,FitDb db,Uri uri) throws Exception {
        JSONObject root=new JSONObject();root.put("format",FORMAT);root.put("version",1);JSONObject tables=new JSONObject();
        for(String table:FitDb.TABLES){JSONArray array=new JSONArray();for(JSONObject row:db.rows("SELECT * FROM "+table+" ORDER BY id"))array.put(row);tables.put(table,array);}root.put("tables",tables);
        Set<String> names=new HashSet<>();for(JSONObject meal:db.rows("SELECT photo FROM meals WHERE photo<>''"))names.add(meal.optString("photo"));
        OutputStream destination=context.getContentResolver().openOutputStream(uri);if(destination==null)throw new IllegalStateException("无法打开备份目标");
        try(OutputStream raw=destination;ZipOutputStream zip=new ZipOutputStream(raw)){
            zip.putNextEntry(new ZipEntry("data.json"));zip.write(root.toString().getBytes(StandardCharsets.UTF_8));zip.closeEntry();
            byte[] buffer=new byte[65536];for(String name:names){File f=safePhoto(context,name);if(f==null||!f.isFile())throw new IllegalStateException("照片缺失，未完成完整备份："+name);
                zip.putNextEntry(new ZipEntry("photos/"+name));try(InputStream in=new FileInputStream(f)){int n;while((n=in.read(buffer))!=-1)zip.write(buffer,0,n);}zip.closeEntry();
            }
        }
    }
    public static void importZip(Context context,FitDb db,Uri uri) throws Exception {
        File stage=new File(context.getCacheDir(),"fitlog_restore_"+System.nanoTime());if(!stage.mkdirs())throw new IllegalStateException("不能准备临时目录");
        JSONObject root=null;HashMap<String,File> staged=new HashMap<>();long total=0;
        try {
            InputStream backupStream=context.getContentResolver().openInputStream(uri);if(backupStream==null)throw new IllegalStateException("无法打开备份文件");
            try(InputStream raw=backupStream;ZipInputStream zip=new ZipInputStream(raw)){
                ZipEntry entry;byte[] buffer=new byte[65536];while((entry=zip.getNextEntry())!=null){
                    String path=entry.getName();if(entry.isDirectory()){zip.closeEntry();continue;}
                    if(!path.equals("data.json")&&!path.matches("photos/[A-Za-z0-9._-]+\\.jpg"))throw new IllegalArgumentException("备份包含不支持的文件："+path);
                    long size=0;ByteArrayOutputStream json=path.equals("data.json")?new ByteArrayOutputStream():null;
                    File output=json==null?new File(stage,path.substring(7)):null;
                    try(OutputStream out=json==null?new FileOutputStream(output):json){int n;while((n=zip.read(buffer))!=-1){size+=n;total+=n;if(size>(json==null?20_000_000:50_000_000)||total>500_000_000)throw new IllegalArgumentException("备份文件过大");out.write(buffer,0,n);}}
                    if(json!=null){if(root!=null)throw new IllegalArgumentException("重复的数据文件");root=new JSONObject(new String(json.toByteArray(),StandardCharsets.UTF_8));}
                    else {String name=output.getName();if(staged.put(name,output)!=null)throw new IllegalArgumentException("重复的照片文件");}
                    zip.closeEntry();
                }
            }
            if(root==null||!FORMAT.equals(root.optString("format"))||root.optInt("version")!=1)throw new IllegalArgumentException("这不是受支持的练刻备份");
            JSONObject tables=root.getJSONObject("tables");for(String table:IMPORT_ORDER)tables.getJSONArray(table);
            File photos=new File(context.getFilesDir(),"photos");if(!photos.exists()&&!photos.mkdirs())throw new IllegalStateException("不能创建照片目录");
            ArrayList<File> created=new ArrayList<>();ArrayList<File> oldPhotos=new ArrayList<>();
            try {
                HashMap<String,String> renamed=new HashMap<>();JSONArray meals=tables.getJSONArray("meals");
                for(int i=0;i<meals.length();i++){JSONObject meal=meals.getJSONObject(i);String old=meal.optString("photo","");if(old.isEmpty())continue;
                    File source=staged.get(old);if(source==null)throw new IllegalArgumentException("备份缺少照片："+old);
                    String fresh=renamed.get(old);if(fresh==null){fresh="restored_"+System.nanoTime()+"_"+i+".jpg";File target=new File(photos,fresh);copy(source,target);created.add(target);renamed.put(old,fresh);}meal.put("photo",fresh);
                }
                for(JSONObject meal:db.rows("SELECT photo FROM meals WHERE photo<>''")){File f=safePhoto(context,meal.optString("photo"));if(f!=null)oldPhotos.add(f);}
                SQLiteDatabase sql=db.getWritableDatabase();sql.beginTransaction();try{
                    for(String table:DELETE_ORDER)sql.delete(table,null,null);
                    for(String table:IMPORT_ORDER){JSONArray array=tables.getJSONArray(table);for(int i=0;i<array.length();i++)sql.insertOrThrow(table,null,toValues(array.getJSONObject(i)));}
                    sql.setTransactionSuccessful();
                }finally{sql.endTransaction();}
                for(File f:oldPhotos)f.delete();
            }catch(Exception e){for(File f:created)f.delete();throw e;}
        }finally{File[] files=stage.listFiles();if(files!=null)for(File f:files)f.delete();stage.delete();}
    }
    private static ContentValues toValues(JSONObject object) throws Exception {ContentValues values=new ContentValues();java.util.Iterator<String> keys=object.keys();while(keys.hasNext()){String key=keys.next();Object v=object.get(key);
        if(v==JSONObject.NULL)values.putNull(key);else if(v instanceof Integer)values.put(key,(Integer)v);else if(v instanceof Long)values.put(key,(Long)v);else if(v instanceof Double)values.put(key,(Double)v);else if(v instanceof Number)values.put(key,((Number)v).doubleValue());else values.put(key,String.valueOf(v));
    }return values;}
    private static File safePhoto(Context c,String name){if(name==null||!name.matches("[A-Za-z0-9._-]+\\.jpg"))return null;return new File(new File(c.getFilesDir(),"photos"),name);}
    private static void copy(File in,File out) throws Exception {try(InputStream source=new FileInputStream(in);OutputStream target=new FileOutputStream(out)){byte[] b=new byte[65536];int n;while((n=source.read(b))!=-1)target.write(b,0,n);}}

    public static void exportCsv(Context context,FitDb db,Uri uri) throws Exception {
        OutputStream destination=context.getContentResolver().openOutputStream(uri);if(destination==null)throw new IllegalStateException("无法打开 CSV 目标");
        try(OutputStream raw=destination;BufferedWriter writer=new BufferedWriter(new OutputStreamWriter(raw,StandardCharsets.UTF_8))){
            writer.write('\uFEFF');write(writer,"类型","时间","分类","项目","详情","数值","单位","热量kcal","蛋白质g","碳水g","脂肪g","来源","备注");
            for(JSONObject o:db.rows("SELECT * FROM workouts ORDER BY start_ms"))write(writer,"训练",dt(o.optLong("start_ms")),o.optString("type"),"训练时段",dt(o.optLong("start_ms"))+" 至 "+dt(o.optLong("end_ms")),String.valueOf((o.optLong("end_ms")-o.optLong("start_ms"))/60000),"分钟","","","","","",o.optString("note"));
            for(JSONObject o:db.rows("SELECT s.*,e.name,e.category,w.start_ms FROM sets s JOIN exercises e ON e.id=s.exercise_id JOIN workouts w ON w.id=e.workout_id ORDER BY w.start_ms,e.position,s.position"))
                write(writer,"训练组",dt(o.optLong("start_ms")),o.optString("category"),o.optString("name"),"次数 "+o.optInt("reps")+" · 重量 "+o.optDouble("weight")+"kg · 时长 "+o.optInt("duration_sec")+"秒 · 距离 "+o.optDouble("distance_km")+"km · RPE "+o.optDouble("rpe"),"","","","","","","","");
            for(JSONObject o:db.rows("SELECT * FROM metrics ORDER BY ts"))write(writer,"身体",dt(o.optLong("ts")),o.optString("type"),o.optString("type"),o.optString("type").equals("血压")?o.optDouble("sys")+"/"+o.optDouble("dia"):"",o.optString("type").equals("血压")?"":String.valueOf(o.optDouble("value")),o.optString("type").equals("体重")?"kg":o.optString("type").equals("血压")?"mmHg":"%","","","","",o.optString("source"),o.optString("note"));
            for(JSONObject o:db.rows("SELECT m.ts,m.slot,m.note,i.* FROM meals m LEFT JOIN items i ON i.meal_id=m.id ORDER BY m.ts,i.id")){
                boolean has=o.has("grams")&&!o.isNull("grams");double grams=has?o.optDouble("grams"):0;
                write(writer,"饮食",dt(o.optLong("ts")),o.optString("slot"),has?o.optString("name"):"未添加食物",has?"份量 "+grams+"g":"",has?String.valueOf(grams):"",has?"g":"",
                    has?f(FoodMath.scaled(o.optDouble("kcal100"),grams)):"",has?f(FoodMath.scaled(o.optDouble("protein100"),grams)):"",has?f(FoodMath.scaled(o.optDouble("carb100"),grams)):"",has?f(FoodMath.scaled(o.optDouble("fat100"),grams)):"",has?o.optString("source"):"",o.optString("note"));
            }
        }
    }
    private static String dt(long ms){return new SimpleDateFormat("yyyy-MM-dd HH:mm",Locale.CHINA).format(new Date(ms));}
    private static String f(double x){return String.format(Locale.ROOT,"%.2f",x);}
    private static void write(BufferedWriter w,String... fields) throws Exception {for(int i=0;i<fields.length;i++){if(i>0)w.write(',');String s=fields[i]==null?"":fields[i];String trimmed=s.trim();
        if(!trimmed.isEmpty()&&(trimmed.charAt(0)=='='||trimmed.charAt(0)=='+'||trimmed.charAt(0)=='@'||(trimmed.charAt(0)=='-'&&!trimmed.matches("-?[0-9]+(\\.[0-9]+)?"))))s="'"+s;
        w.write('"');w.write(s.replace("\"","\"\""));w.write('"');}w.write("\r\n");}
}
