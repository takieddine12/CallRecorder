package core.threebanders.recordr;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Const {

    public static final String DATABASE_NAME = "callrecorder.db";
    public static final String SPEAKER_USE = "put_on_speaker";
    public static final String GOOGLE_DRIVE_USE = "put_on_drive";
    public static final List<CoreUtil.PhoneTypeContainer> PHONE_TYPES = new ArrayList<>(Arrays.asList(
            new CoreUtil.PhoneTypeContainer(1, "Home"),
            new CoreUtil.PhoneTypeContainer(2, "Mobile"),
            new CoreUtil.PhoneTypeContainer(3, "Work"),
            new CoreUtil.PhoneTypeContainer(-1, "Unknown"),
            new CoreUtil.PhoneTypeContainer(7, "Other")
    ));
}
