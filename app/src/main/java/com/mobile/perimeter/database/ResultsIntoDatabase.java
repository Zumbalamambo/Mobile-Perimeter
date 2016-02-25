package com.mobile.perimeter.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.os.Environment;

import com.mobile.perimeter.util.Consts;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;

public final class ResultsIntoDatabase {

	private SQLiteDatabase database;
	private InfoDbHelper dbHelper;
	private String[] allColumns = { InfoDbHelper.COLUMN_ID,
			InfoDbHelper.COLUMN_DATE, InfoDbHelper.COLUMN_RESULTS };

	public ResultsIntoDatabase(Context context) {
		dbHelper = new InfoDbHelper(context);
	}

	public void open() throws SQLException {
		database = dbHelper.getWritableDatabase();
	}

	public void close() {
		dbHelper.close();
	}

	public void createEntry(String date, String mode, String results) {
		ContentValues values = new ContentValues();
		values.put(InfoDbHelper.COLUMN_USERNAME, Consts.USERNAME_KEY);
		values.put(InfoDbHelper.COLUMN_DATE, date);
		values.put(InfoDbHelper.COLUMN_MODE, mode);
		values.put(InfoDbHelper.COLUMN_RESULTS, results);
		boolean whichEye = false;
		if (Consts.RIGHT_EYE) {
			whichEye = true;
		}
		values.put(InfoDbHelper.COLUMN_EYE, whichEye);
		database.insert(InfoDbHelper.TABLE_NAME, null, values);
		/*
		 * Cursor cursor = database.query(InfoDbHelper.TABLE_NAME, allColumns,
		 * InfoDbHelper.COLUMN_ID + " = " + insertId, null, null, null, null);
		 * cursor.moveToFirst(); cursor.close();
		 */

	}

	public void copyAppDbToExternalStorage(Context context) throws IOException {
		File sd = Environment.getExternalStorageDirectory();
		File currentDB = context.getDatabasePath(InfoDbHelper.DATABASE_NAME); // databaseName=your
																				// current
																				// application
																				// database
																				// name,
																				// for
																				// example
																				// "my_data.db"
		if (sd.canWrite()) {
			File backupDB = new File(sd, "perimetry_backup"); // for example
																// "my_data_backup.db"
			if (currentDB.exists()) {
				FileChannel src = new FileInputStream(currentDB).getChannel();
				FileChannel dst = new FileOutputStream(backupDB).getChannel();
				dst.transferFrom(src, 0, src.size());
				src.close();
				dst.close();
			}
		}
	}

	public Cursor getAllResults() {
		String selectQuery = "SELECT  * FROM " + InfoDbHelper.TABLE_NAME;
		Cursor mCursor = database.rawQuery(selectQuery, null);
		if (mCursor != null) {
			mCursor.moveToFirst();
		}
		return mCursor;
	}

	public Cursor getResult(String date) {
		String selectQuery = "SELECT * FROM " + InfoDbHelper.TABLE_NAME
				+ " WHERE " + InfoDbHelper.COLUMN_DATE + " = \"" + date + "\"";
		Cursor cursor = database.rawQuery(selectQuery, null);
		return cursor;
	}

	public void deleteResult(String date) {
		database.delete(InfoDbHelper.TABLE_NAME, InfoDbHelper.COLUMN_DATE
				+ " = \"" + date + "\"", null);
	}

}