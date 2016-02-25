package com.mobile.perimeter.database;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class InfoDbHelper extends SQLiteOpenHelper {
	// If you change the database schema, you must increment the database
	// version.
	public static final int DATABASE_VERSION = 2;
	public static final String DATABASE_NAME = "UserInfo.db";
	private static final String TEXT_TYPE = " TEXT";
	private static final String INTEGER_TYPE = " INTEGER";

	public static final String TABLE_NAME = "PerimetryResultsInfo";
	public static final String COLUMN_USERNAME = "username";
	public static final String COLUMN_DATE = "date";
	public static final String COLUMN_MODE = "mode";
	public static final String COLUMN_EYE = "eye";
	public static final String COLUMN_RESULTS = "results";
	public static final String COLUMN_ID = "_id";

	private static final String SQL_CREATE_ENTRIES = "CREATE TABLE IF NOT EXISTS "
			+ TABLE_NAME
			+ " ("
			+ COLUMN_ID
			+ " INTEGER PRIMARY KEY AUTOINCREMENT,"
			+ COLUMN_USERNAME
			+ TEXT_TYPE
			+ ", "
			+ COLUMN_DATE
			+ TEXT_TYPE
			+ ", "
			+ COLUMN_MODE
			+ TEXT_TYPE
			+ ", "
			+ COLUMN_RESULTS
			+ TEXT_TYPE
			+ ", "
			+ COLUMN_EYE
			+ TEXT_TYPE + " )";

	private static final String SQL_DELETE_ACTIVITY_ENTRIES = "DROP TABLE IF EXISTS "
			+ TABLE_NAME;

	public InfoDbHelper(Context context) {

		super(context, DATABASE_NAME, null, DATABASE_VERSION);
		Log.d("database", "infohelper");

	}

	public void onCreate(SQLiteDatabase db) {
		Log.d("database", "database oncreate");

		db.execSQL(SQL_CREATE_ENTRIES);
		Log.d("database", "database oncreatedone");
	}

	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		// This database is only a cache for online data, so its upgrade policy
		// is
		// to simply to discard the data and start over
		// db.execSQL(SQL_DELETE_ACTIVITY_ENTRIES);
		if (newVersion > oldVersion) {
			db.execSQL("ALTER TABLE " + TABLE_NAME + " ADD COLUMN "
					+ COLUMN_EYE + " INTEGER DEFAULT 1");
		}
		onCreate(db);
	}

	public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		onUpgrade(db, oldVersion, newVersion);
	}

}
