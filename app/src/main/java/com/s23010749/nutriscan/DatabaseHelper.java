package com.s23010749.nutriscan;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DatabaseHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "NutriScanFinal.db";
    private static final int DATABASE_VERSION = 1;

    public static final String TABLE_HISTORY = "scan_history";
    public static final String COL_SCAN_ID = "id";
    public static final String COL_FOOD_NAME = "food_name";
    public static final String COL_HEALTH_STATUS = "health_status";
    public static final String COL_NUTRIENTS_JSON = "nutrients_json";
    public static final String COL_TIPS = "tips";
    public static final String COL_DATE = "date";
    public static final String COL_LOCATION = "location";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String createHistory = "CREATE TABLE " + TABLE_HISTORY + "("
                + COL_SCAN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + COL_FOOD_NAME + " TEXT,"
                + COL_HEALTH_STATUS + " TEXT,"
                + COL_NUTRIENTS_JSON + " TEXT,"
                + COL_TIPS + " TEXT,"
                + COL_DATE + " TEXT,"
                + COL_LOCATION + " TEXT)";
        db.execSQL(createHistory);

        String createUsers = "CREATE TABLE users (id INTEGER PRIMARY KEY AUTOINCREMENT, username TEXT UNIQUE, password TEXT)";
        db.execSQL(createUsers);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_HISTORY);
        db.execSQL("DROP TABLE IF EXISTS users");
        onCreate(db);
    }

    public boolean addUser(String username, String password) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("username", username);
        values.put("password", password);
        long result = db.insert("users", null, values);
        db.close();
        return result != -1;
    }

    public boolean checkUsername(String username) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query("users", new String[]{"id"}, "username = ?", new String[]{username}, null, null, null);
        boolean exists = cursor.moveToFirst();
        cursor.close();
        db.close();
        return exists;
    }

    public boolean checkUser(String username, String password) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query("users", new String[]{"id"}, "username = ? AND password = ?", new String[]{username, password}, null, null, null);
        boolean exists = cursor.moveToFirst();
        cursor.close();
        db.close();
        return exists;
    }

    public long addScanResult(String name, String health, String json, String tips, String date, String location) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_FOOD_NAME, name);
        values.put(COL_HEALTH_STATUS, health);
        values.put(COL_NUTRIENTS_JSON, json);
        values.put(COL_TIPS, tips);
        values.put(COL_DATE, date);
        values.put(COL_LOCATION, location);
        long id = db.insert(TABLE_HISTORY, null, values);
        db.close();
        return id;
    }

    public Cursor getAllScanHistory() {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.query(TABLE_HISTORY, null, null, null, null, null, COL_SCAN_ID + " DESC");
    }

    // --- ADDED THE MISSING METHOD HERE ---
    public Cursor searchLocalHistory(String query) {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.query(TABLE_HISTORY, null, COL_FOOD_NAME + " LIKE ?", new String[]{"%" + query + "%"}, null, null, COL_SCAN_ID + " DESC");
    }

    public Cursor getTodayScans() {
        SQLiteDatabase db = this.getReadableDatabase();
        String today = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(new java.util.Date());
        return db.query(TABLE_HISTORY, null, COL_DATE + " LIKE ?", new String[]{today + "%"}, null, null, COL_SCAN_ID + " DESC");
    }

    public void deleteScanResult(long id) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_HISTORY, COL_SCAN_ID + " = ?", new String[]{String.valueOf(id)});
        db.close();
    }
}