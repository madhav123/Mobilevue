package com.mobilevue.database;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import com.mobilevue.data.Reminder;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DatabaseHandler extends SQLiteOpenHelper {

	private static final int DATABASE_VERSION = 1;

	private static final String DATABASE_NAME = "VueDatabase";

	private static final String TABLE_PROGRAM_REMINDER = "program_reminder";

	private static final String KEY_ID = "id";
	private static final String KEY_PROG_NAME = "program_name";
	private static final String KEY_TIME = "time";

	public DatabaseHandler(Context context) {
		super(context, DATABASE_NAME, null, DATABASE_VERSION);
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		String CREATE_CONTACTS_TABLE = "CREATE TABLE " + TABLE_PROGRAM_REMINDER
				+ "(" + KEY_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
				+ KEY_PROG_NAME + " TEXT," + KEY_TIME + " INTEGER" + ")";
		db.execSQL(CREATE_CONTACTS_TABLE);
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		db.execSQL("DROP TABLE IF EXISTS " + TABLE_PROGRAM_REMINDER);
		onCreate(db);
	}

	public void addReminder(Reminder reminder) {
		SQLiteDatabase db = this.getWritableDatabase();
		ContentValues values = new ContentValues();
		values.put(KEY_PROG_NAME, reminder.get_prog_name());
		values.put(KEY_TIME, reminder.get_time());
		db.insert(TABLE_PROGRAM_REMINDER, null, values);
		db.close();
	}

	public List<Reminder> getAllReminder() {
		List<Reminder> reminderList = new ArrayList<Reminder>();
		// Select All Query
		String selectQuery = "SELECT  * FROM " + TABLE_PROGRAM_REMINDER;

		SQLiteDatabase db = this.getWritableDatabase();
		Cursor cursor = db.rawQuery(selectQuery, null);

		// looping through all rows and adding to list
		if (cursor.moveToFirst()) {
			do {
				Reminder reminder = new Reminder();
				reminder.set_id((Integer.parseInt(cursor.getString(0))));
				reminder.set_prog_name(cursor.getString(1));
				reminder.set_time(cursor.getLong(2));
				reminderList.add(reminder);
			} while (cursor.moveToNext());
		}

		return reminderList;
	}

	public void deleteOldReminders() {
		SQLiteDatabase db = this.getWritableDatabase();
		Calendar c = Calendar.getInstance();
		long currentMillies = c.getTimeInMillis();

		db.delete(TABLE_PROGRAM_REMINDER, KEY_TIME + " < " + currentMillies,
				null);
		db.close();
	}

	public void deleteAllReminders() {
		SQLiteDatabase db = this.getWritableDatabase();
		db.execSQL("delete from " + TABLE_PROGRAM_REMINDER);
		db.close();
	}
}
