package com.s23010749.nutriscan;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "NutriScan.db";
    private static final int DATABASE_VERSION = 7; // Upgraded to 7

    private static final String TABLE_USERS = "users";
    private static final String COL_USER_ID = "id";
    private static final String COL_USERNAME = "username";
    private static final String COL_PASSWORD = "password";

    private static final String TABLE_SCAN_HISTORY = "scan_history";
    public static final String COL_SCAN_ID = "scan_id";
    public static final String COL_FOOD_NAME = "food_name";
    public static final String COL_HEALTH_STATUS = "health_status";
    public static final String COL_NUTRIENTS_JSON = "nutrients_json";
    public static final String COL_INGREDIENTS = "ingredients"; // NEW COLUMN
    public static final String COL_TIPS = "tips_message";
    public static final String COL_DATE = "scan_date";
    public static final String COL_LOCATION = "scan_location";

    private static final String CREATE_USERS_TABLE = "CREATE TABLE " + TABLE_USERS + "("
            + COL_USER_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
            + COL_USERNAME + " TEXT UNIQUE,"
            + COL_PASSWORD + " TEXT"
            + ")";

    private static final String CREATE_SCAN_HISTORY_TABLE = "CREATE TABLE " + TABLE_SCAN_HISTORY + "("
            + COL_SCAN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
            + COL_FOOD_NAME + " TEXT,"
            + COL_HEALTH_STATUS + " TEXT,"
            + COL_NUTRIENTS_JSON + " TEXT,"
            + COL_INGREDIENTS + " TEXT," // NEW COLUMN
            + COL_TIPS + " TEXT,"
            + COL_DATE + " TEXT,"
            + COL_LOCATION + " TEXT"
            + ")";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_USERS_TABLE);
        db.execSQL(CREATE_SCAN_HISTORY_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_USERS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_SCAN_HISTORY);
        onCreate(db);
    }

    public boolean addUser(String username, String password) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put(COL_USERNAME, username);
        contentValues.put(COL_PASSWORD, password);
        long result = db.insert(TABLE_USERS, null, contentValues);
        db.close();
        return result != -1;
    }

    public boolean checkUsername(String username) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_USERS, new String[]{COL_USERNAME}, COL_USERNAME + " = ?", new String[]{username}, null, null, null);
        boolean exists = cursor.moveToFirst();
        cursor.close();
        db.close();
        return exists;
    }

    public boolean checkUser(String username, String password) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_USERS, new String[]{COL_USER_ID}, COL_USERNAME + " = ? AND " + COL_PASSWORD + " = ?", new String[]{username, password}, null, null, null);
        boolean exists = cursor.moveToFirst();
        cursor.close();
        db.close();
        return exists;
    }

    // UPDATED METHOD TO SAVE INGREDIENTS
    public long addScanResult(String foodName, String healthStatus, String nutrientsJson, String ingredients, String tips, String date, String location) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put(COL_FOOD_NAME, foodName);
        contentValues.put(COL_HEALTH_STATUS, healthStatus);
        contentValues.put(COL_NUTRIENTS_JSON, nutrientsJson);
        contentValues.put(COL_INGREDIENTS, ingredients);
        contentValues.put(COL_TIPS, tips);
        contentValues.put(COL_DATE, date);
        contentValues.put(COL_LOCATION, location);

        long result = db.insert(TABLE_SCAN_HISTORY, null, contentValues);
        db.close();
        return result;
    }

    public Cursor getAllScanHistory() {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.query(TABLE_SCAN_HISTORY, null, null, null, null, null, COL_DATE + " DESC");
    }

    public boolean deleteScanResult(long scanId) {
        SQLiteDatabase db = this.getWritableDatabase();
        int rowsAffected = db.delete(TABLE_SCAN_HISTORY, COL_SCAN_ID + " = ?", new String[]{String.valueOf(scanId)});
        db.close();
        return rowsAffected > 0;
    }
}