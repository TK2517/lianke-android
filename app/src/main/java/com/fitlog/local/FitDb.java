package com.fitlog.local;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public final class FitDb extends SQLiteOpenHelper {
    public static final String[] TABLES = {"workouts", "exercises", "sets", "metrics", "meals", "items", "foods"};
    private final Context context;
    public FitDb(Context context) { super(context, "fitlog.db", null, 1); this.context=context.getApplicationContext(); }

    @Override public void onConfigure(SQLiteDatabase db) { db.setForeignKeyConstraintsEnabled(true); }
    @Override public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE workouts(id INTEGER PRIMARY KEY, start_ms INTEGER NOT NULL, end_ms INTEGER NOT NULL, type TEXT NOT NULL, note TEXT NOT NULL DEFAULT '')");
        db.execSQL("CREATE TABLE exercises(id INTEGER PRIMARY KEY, workout_id INTEGER NOT NULL REFERENCES workouts(id) ON DELETE CASCADE, name TEXT NOT NULL, category TEXT NOT NULL, position INTEGER NOT NULL DEFAULT 0)");
        db.execSQL("CREATE TABLE sets(id INTEGER PRIMARY KEY, exercise_id INTEGER NOT NULL REFERENCES exercises(id) ON DELETE CASCADE, reps INTEGER NOT NULL DEFAULT 0, weight REAL NOT NULL DEFAULT 0, duration_sec INTEGER NOT NULL DEFAULT 0, distance_km REAL NOT NULL DEFAULT 0, rpe REAL NOT NULL DEFAULT 0, position INTEGER NOT NULL DEFAULT 0)");
        db.execSQL("CREATE TABLE metrics(id INTEGER PRIMARY KEY, ts INTEGER NOT NULL, type TEXT NOT NULL, sys REAL, dia REAL, value REAL, source TEXT NOT NULL DEFAULT '', note TEXT NOT NULL DEFAULT '')");
        db.execSQL("CREATE TABLE meals(id INTEGER PRIMARY KEY, ts INTEGER NOT NULL, slot TEXT NOT NULL, note TEXT NOT NULL DEFAULT '', photo TEXT NOT NULL DEFAULT '')");
        db.execSQL("CREATE TABLE foods(id INTEGER PRIMARY KEY, fdc_id INTEGER, name TEXT NOT NULL, english TEXT NOT NULL DEFAULT '', category TEXT NOT NULL, kcal REAL NOT NULL, protein REAL NOT NULL, carb REAL NOT NULL, fat REAL NOT NULL, source TEXT NOT NULL, favorite INTEGER NOT NULL DEFAULT 0, custom INTEGER NOT NULL DEFAULT 0, recipe_json TEXT NOT NULL DEFAULT '')");
        db.execSQL("CREATE TABLE items(id INTEGER PRIMARY KEY, meal_id INTEGER NOT NULL REFERENCES meals(id) ON DELETE CASCADE, food_id INTEGER REFERENCES foods(id) ON DELETE SET NULL, name TEXT NOT NULL, grams REAL NOT NULL, kcal100 REAL NOT NULL, protein100 REAL NOT NULL, carb100 REAL NOT NULL, fat100 REAL NOT NULL, source TEXT NOT NULL)");
        db.execSQL("CREATE INDEX idx_workouts_start ON workouts(start_ms)");
        db.execSQL("CREATE INDEX idx_metrics_ts ON metrics(ts)");
        db.execSQL("CREATE INDEX idx_meals_ts ON meals(ts)");
        seedFoods(db);
    }
    @Override public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) { throw new IllegalStateException("Unsupported DB version: " + oldVersion); }

    private void seedFoods(SQLiteDatabase db) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(context.getAssets().open("foods.tsv"), StandardCharsets.UTF_8))) {
            reader.readLine();
            String line;
            while ((line=reader.readLine()) != null) {
                String[] c=line.split("\t", -1);
                if (c.length != 9) throw new IllegalStateException("Bad catalog row: " + line);
                ContentValues v=new ContentValues();
                v.put("fdc_id", Long.parseLong(c[0])); v.put("name", c[1]); v.put("english", c[2]); v.put("category", c[3]);
                v.put("kcal", Double.parseDouble(c[4])); v.put("protein", Double.parseDouble(c[5])); v.put("carb", Double.parseDouble(c[6])); v.put("fat", Double.parseDouble(c[7])); v.put("source", c[8]);
                db.insertOrThrow("foods", null, v);
            }
        } catch (Exception e) { throw new IllegalStateException("Food catalog cannot be loaded", e); }
    }

    public long add(String table, ContentValues values) { return getWritableDatabase().insertOrThrow(table, null, values); }
    public void update(String table, long id, ContentValues values) { getWritableDatabase().update(table, values, "id=?", new String[]{String.valueOf(id)}); }
    public void delete(String table, long id) { getWritableDatabase().delete(table, "id=?", new String[]{String.valueOf(id)}); }
    public List<JSONObject> rows(String sql, String... args) {
        ArrayList<JSONObject> out=new ArrayList<>();
        try (Cursor c=getReadableDatabase().rawQuery(sql,args)) {
            while (c.moveToNext()) {
                JSONObject o=new JSONObject();
                for (int i=0;i<c.getColumnCount();i++) {
                    String key=c.getColumnName(i);
                    switch(c.getType(i)) {
                        case Cursor.FIELD_TYPE_INTEGER: o.put(key,c.getLong(i)); break;
                        case Cursor.FIELD_TYPE_FLOAT: o.put(key,c.getDouble(i)); break;
                        case Cursor.FIELD_TYPE_STRING: o.put(key,c.getString(i)); break;
                        default: o.put(key,JSONObject.NULL);
                    }
                }
                out.add(o);
            }
        } catch(Exception e) { throw new IllegalStateException("Database query failed",e); }
        return out;
    }
    public JSONObject one(String sql, String... args) {
        List<JSONObject> list=rows(sql,args); return list.isEmpty()?null:list.get(0);
    }
    public int count(String table) { JSONObject o=one("SELECT COUNT(*) n FROM "+table); return o==null?0:o.optInt("n"); }
}
