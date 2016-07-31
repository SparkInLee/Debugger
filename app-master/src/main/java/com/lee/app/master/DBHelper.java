package com.lee.app.master;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * Created by jianglee on 7/30/16.
 */
public class DBHelper extends SQLiteOpenHelper {

    private static final String DB_NAME = "debugger.db";

    private static final int DB_VERSION = 1;

    public static final String ACCOUNT_TABLE_NAME = "Account";

    public static class AccountFields {
        public static final String ID = "id";
        public static final String NAME = "name";
    }

    public static final String CREATE_ACCOUNT_TABLE = "(" + AccountFields.ID
            + " UNSIGNED BIG INT PRIMARY KEY, " + AccountFields.NAME + " TEXT)";

    public DBHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS " + ACCOUNT_TABLE_NAME + " " + CREATE_ACCOUNT_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        try {
            db.beginTransaction();
            db.execSQL("DROP TABLE IF EXISTS " + ACCOUNT_TABLE_NAME);
            db.execSQL("CREATE TABLE IF NOT EXISTS " + ACCOUNT_TABLE_NAME + " " + CREATE_ACCOUNT_TABLE);
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }
}
