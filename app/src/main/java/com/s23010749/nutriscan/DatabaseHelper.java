package com.s23010749.nutriscan;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

// This class handles all the database stuff, like saving users and now, scan history.
public class DatabaseHelper extends SQLiteOpenHelper {

    // Name and version for our database file.
    private static final String DATABASE_NAME = "NutriScan.db";
    private static final int DATABASE_VERSION = 3; // Version 3 for full scan_history details

    // Info for our 'users' table.
    private static final String TABLE_USERS = "users";
    private static final String COL_USER_ID = "id";
    private static final String COL_USERNAME = "username";
    private static final String COL_PASSWORD = "password";

    // Info for our 'scan_history' table.
    private static final String TABLE_SCAN_HISTORY = "scan_history";
    public static final String COL_SCAN_ID = "scan_id"; // Made public for HistoryActivity to use
    public static final String COL_FOOD_NAME = "food_name"; // Made public
    public static final String COL_HEALTH_STATUS = "health_status"; // Made public
    public static final String COL_SUGAR = "sugar_level"; // Made public
    public static final String COL_SALT = "salt_level"; // Made public
    public static final String COL_FAT = "fat_level"; // Made public
    public static final String COL_TIPS = "tips_message"; // Made public
    public static final String COL_DATE = "scan_date"; // Made public
    public static final String COL_LOCATION = "scan_location"; // Made public

    // This command creates the 'users' table when the app first runs.
    private static final String CREATE_USERS_TABLE = "CREATE TABLE " + TABLE_USERS + "("
            + COL_USER_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
            + COL_USERNAME + " TEXT UNIQUE,"
            + COL_PASSWORD + " TEXT"
            + ")";

    // This command creates the 'scan_history' table.
    private static final String CREATE_SCAN_HISTORY_TABLE = "CREATE TABLE " + TABLE_SCAN_HISTORY + "("
            + COL_SCAN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
            + COL_FOOD_NAME + " TEXT,"
            + COL_HEALTH_STATUS + " TEXT,"
            + COL_SUGAR + " TEXT,"
            + COL_SALT + " TEXT,"
            + COL_FAT + " TEXT,"
            + COL_TIPS + " TEXT,"
            + COL_DATE + " TEXT,"
            + COL_LOCATION + " TEXT"
            + ")";

    // Constructor, just sets up the database.
    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    // This runs when the database is first created (only once).
    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_USERS_TABLE); // Create the users table.
        db.execSQL(CREATE_SCAN_HISTORY_TABLE); // Create the scan history table.
    }

    // This runs if we ever update the database version.
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // This will delete existing data if tables are dropped!
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_USERS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_SCAN_HISTORY); // Drop the old history table
        onCreate(db); // Recreate all tables.
    }

    // --- User database actions ---

    // Adds a new user to the database.
    public boolean addUser(String username, String password) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues contentValues = new ContentValues();

        contentValues.put(COL_USERNAME, username);
        contentValues.put(COL_PASSWORD, password);

        long result = db.insert(TABLE_USERS, null, contentValues);
        db.close();
        return result != -1;
    }

    // Checks if a username already exists in the database.
    public boolean checkUsername(String username) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = null;
        String[] columns = { COL_USERNAME };
        String selection = COL_USERNAME + " = ?";
        String[] selectionArgs = { username };

        try {
            cursor = db.query(TABLE_USERS, columns, selection, selectionArgs,
                    null, null, null);
            return cursor.moveToFirst();
        } finally {
            if (cursor != null) {
                cursor.close();
            }
            db.close();
        }
    }

    // Checks if both username and password match for logging in.
    public boolean checkUser(String username, String password) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = null;
        String[] columns = { COL_USER_ID };
        String selection = COL_USERNAME + " = ?" + " AND " + COL_PASSWORD + " = ?";
        String[] selectionArgs = { username, password };

        try {
            cursor = db.query(TABLE_USERS, columns, selection, selectionArgs,
                    null, null, null);
            return cursor.moveToFirst();
        } finally {
            if (cursor != null) {
                cursor.close();
            }
            db.close();
        }
    }

    // --- Scan History database actions ---

    // Adds a new scan result to the history table.
    public long addScanResult(String foodName, String healthStatus, String sugar, String salt, String fat, String tips, String date, String location) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues contentValues = new ContentValues();

        contentValues.put(COL_FOOD_NAME, foodName);
        contentValues.put(COL_HEALTH_STATUS, healthStatus);
        contentValues.put(COL_SUGAR, sugar);
        contentValues.put(COL_SALT, salt);
        contentValues.put(COL_FAT, fat);
        contentValues.put(COL_TIPS, tips);
        contentValues.put(COL_DATE, date);
        contentValues.put(COL_LOCATION, location);

        long result = db.insert(TABLE_SCAN_HISTORY, null, contentValues);
        db.close();
        return result;
    }

    // Gets all scan history results.
    public Cursor getAllScanHistory() {
        SQLiteDatabase db = this.getReadableDatabase();
        // Order by date, newest first
        return db.query(TABLE_SCAN_HISTORY, null, null, null, null, null, COL_DATE + " DESC");
    }

    // Deletes a specific scan history item by its ID
    public boolean deleteScanResult(long scanId) {
        SQLiteDatabase db = this.getWritableDatabase();
        // Delete rows where COL_SCAN_ID matches the given scanId
        int rowsAffected = db.delete(TABLE_SCAN_HISTORY, COL_SCAN_ID + " = ?",
                new String[]{String.valueOf(scanId)});
        db.close();
        return rowsAffected > 0; // Returns true if at least one row was deleted
    }

    // (Conceptual) Example of updating a user's password
    // This is just an example; need a UI for this.
    public boolean updateUserPassword(String username, String newPassword) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put(COL_PASSWORD, newPassword);

        int rowsAffected = db.update(TABLE_USERS, contentValues, COL_USERNAME + " = ?",
                new String[]{username});
        db.close();
        return rowsAffected > 0;
    }
}