package core.threebanders.recordr.data;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;


public class CallRecorderDbHelper extends SQLiteOpenHelper {
    public static final String SQL_CREATE_RECORDINGS = "CREATE TABLE " +
            RecordingsContract.Recordings.TABLE_NAME + " (" + RecordingsContract.Recordings._ID + " INTEGER NOT NULL PRIMARY KEY, " +
            RecordingsContract.Recordings.COLUMN_NAME_CONTACT_ID + " INTEGER , " +
            RecordingsContract.Recordings.COLUMN_NAME_INCOMING + " INTEGER , " +
            RecordingsContract.Recordings.COLUMN_NAME_PATH + " TEXT , " +
            RecordingsContract.Recordings.COLUMN_NAME_START_TIMESTAMP + " INTEGER , " +
            RecordingsContract.Recordings.COLUMN_NAME_END_TIMESTAMP + " INTEGER , " +
            RecordingsContract.Recordings.COLUMN_NAME_FORMAT + " TEXT , " +
            RecordingsContract.Recordings.COLUMN_NAME_IS_NAME_SET + " INTEGER  DEFAULT 0, " +
            RecordingsContract.Recordings.COLUMN_NAME_MODE + " TEXT , " +
            RecordingsContract.Recordings.COLUMN_NAME_SOURCE + " TEXT DEFAULT 'unknown')";

    public static final String SQL_CREATE_CONTACTS = "CREATE TABLE " + ContactsContractLocal.Contacts.TABLE_NAME +
            " (" + ContactsContractLocal.Contacts._ID + " INTEGER NOT NULL PRIMARY KEY, " +
            ContactsContractLocal.Contacts.COLUMN_NAME_NUMBER + " TEXT, " +
            ContactsContractLocal.Contacts.COLUMN_NAME_CONTACT_NAME + " TEXT, " +
            ContactsContractLocal.Contacts.COLUMN_NAME_PHOTO_URI + " TEXT, " +
            ContactsContractLocal.Contacts.COLUMN_NAME_PHONE_TYPE + " INTEGER NOT NULL, " +
            "CONSTRAINT no_duplicates UNIQUE(" + ContactsContractLocal.Contacts.COLUMN_NAME_NUMBER + ") )";


    private static final int DATABASE_VERSION = 3;

    CallRecorderDbHelper(Context context, String dbName) {
        super(context, dbName, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(SQL_CREATE_RECORDINGS);
        db.execSQL(SQL_CREATE_CONTACTS);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        String version2 = "ALTER TABLE " + RecordingsContract.Recordings.TABLE_NAME + " ADD COLUMN " + RecordingsContract.Recordings.COLUMN_NAME_SOURCE +
                " TEXT NOT NULL DEFAULT 'unknown'";
        String version3a = "ALTER TABLE " + ContactsContractLocal.Contacts.TABLE_NAME + " RENAME TO " + ContactsContractLocal.Contacts.TABLE_NAME + "_old";
        String version3b = "INSERT INTO " + ContactsContractLocal.Contacts.TABLE_NAME + "(_id, phone_number, contact_name, photo_uri, phone_type) SELECT _id, phone_number, contact_name, photo_uri, phone_type FROM "
                + ContactsContractLocal.Contacts.TABLE_NAME + "_old";
        String version3c = "DROP TABLE " + ContactsContractLocal.Contacts.TABLE_NAME + "_old";
        if (oldVersion == 1) {
            db.execSQL(version2);
            db.execSQL(version3a);
            db.execSQL(SQL_CREATE_CONTACTS);
            db.execSQL(version3b);
            db.execSQL(version3c);
        }
        if (oldVersion == 2) {
            db.execSQL(version3a);
            db.execSQL(SQL_CREATE_CONTACTS);
            db.execSQL(version3b);
            db.execSQL(version3c);
        }
    }
}
