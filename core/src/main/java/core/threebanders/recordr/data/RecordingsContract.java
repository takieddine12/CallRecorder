package core.threebanders.recordr.data;

import android.provider.BaseColumns;

class RecordingsContract {
    private RecordingsContract() {
    }

    public static class Recordings implements BaseColumns {
        public static final String TABLE_NAME = "recordings";
        public static final String COLUMN_NAME_CONTACT_ID = "contact_id";
        public static final String COLUMN_NAME_INCOMING = "incoming";
        public static final String COLUMN_NAME_PATH = "path";
        public static final String COLUMN_NAME_START_TIMESTAMP = "start_timestamp";
        public static final String COLUMN_NAME_END_TIMESTAMP = "end_timestamp";
        public static final String COLUMN_NAME_IS_NAME_SET = "is_name_set";
        public static final String COLUMN_NAME_FORMAT = "format";
        public static final String COLUMN_NAME_MODE = "mode";
        static final String COLUMN_NAME_SOURCE = "source";
    }
}
