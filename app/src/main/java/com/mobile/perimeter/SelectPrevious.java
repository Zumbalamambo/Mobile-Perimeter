package com.mobile.perimeter;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;

import com.mobile.perimeter.database.InfoDbHelper;
import com.mobile.perimeter.database.ResultsIntoDatabase;
import com.mobile.perimeter.util.Consts;

public class SelectPrevious extends Activity {
	private ResultsIntoDatabase mResultDatabase;
	private SimpleCursorAdapter mDataAdapter;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_select_previous);
		mResultDatabase = new ResultsIntoDatabase(this);
		mResultDatabase.open();
		Cursor cursor = mResultDatabase.getAllResults();

		mDataAdapter = new SimpleCursorAdapter(this,
				android.R.layout.simple_list_item_2, cursor, new String[] {
						"username", "date" }, new int[] { android.R.id.text1,
						android.R.id.text2 }, 0);

		ListView listView = (ListView) findViewById(R.id.previousResultsView);
		// Assign adapter to ListView
		listView.setAdapter(mDataAdapter);

		listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> adapterView, View view,
					int pos, long l) {
				Cursor c = (Cursor) mDataAdapter.getItem(pos);
				c.moveToPosition(pos);
				// String date =
				// c.getString(c.getColumnIndexOrThrow(InfoDbHelper.COLUMN_DATE));
				String mode = c.getString(c
						.getColumnIndexOrThrow(InfoDbHelper.COLUMN_MODE));
				String results = c.getString(c
						.getColumnIndexOrThrow(InfoDbHelper.COLUMN_RESULTS));
				int eye = c.getInt(c
						.getColumnIndexOrThrow(InfoDbHelper.COLUMN_EYE));

				Bundle b = new Bundle();
				b.putString(InfoDbHelper.COLUMN_MODE, mode);
				b.putString(InfoDbHelper.COLUMN_RESULTS, results);
				b.putInt(InfoDbHelper.COLUMN_EYE, eye);

				selectViewOrLoadData(b);

			}
		});

		listView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
			@Override
			public boolean onItemLongClick(AdapterView<?> adapterView,
					View view, int pos, long l) {
				Cursor c = (Cursor) mDataAdapter.getItem(pos);
				c.moveToPosition(pos);
				String date = c.getString(c
						.getColumnIndexOrThrow(InfoDbHelper.COLUMN_DATE));
				mResultDatabase.deleteResult(date);

				Cursor c2 = mResultDatabase.getAllResults();
				mDataAdapter.changeCursor(c2);

				mDataAdapter.notifyDataSetChanged();
				// finish();
				return true;
			}
		});
	}

	protected void selectViewOrLoadData(final Bundle b) {
		AlertDialog.Builder ViewOrLoadDialogBox = new AlertDialog.Builder(
				SelectPrevious.this);
		ViewOrLoadDialogBox.setMessage("Display data or load data?");
		ViewOrLoadDialogBox.setCancelable(false);
		ViewOrLoadDialogBox.setPositiveButton("Load",
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {
						dialog.cancel();

						Intent i = new Intent();
						i.putExtras(b);
						setResult(Consts.CONT_PREVIOUS, i);
						finish();

					}
				});

		ViewOrLoadDialogBox.setNeutralButton("Display",
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {
						dialog.cancel();

						Intent i = new Intent();
						i.putExtras(b);
						setResult(Consts.DISP_RESULTS, i);
						finish();

					}
				});

		AlertDialog alert = ViewOrLoadDialogBox.create();
		alert.show();
	}

	@Override
	protected void onPause() {
		super.onPause();
		mResultDatabase.close();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.start_screen, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();

		// noinspection SimplifiableIfStatement
		if (id == R.id.action_settings) {
			return true;
		}

		return super.onOptionsItemSelected(item);
	}
}
