package core.threebanders.recordr.data;

import android.provider.BaseColumns;

public class ContactsContractLocal {
    private ContactsContractLocal() {
    }

    public static class Contacts implements BaseColumns {
        public static final String TABLE_NAME = "contacts";

        public static final String COLUMN_NAME_NUMBER = "phone_number";
        public static final String COLUMN_NAME_CONTACT_NAME = "contact_name";
        public static final String COLUMN_NAME_PHOTO_URI = "photo_uri";
        public static final String COLUMN_NAME_PHONE_TYPE = "phone_type";
    }
}
