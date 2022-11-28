package core.threebanders.recordr.data;


import java.util.List;

public interface Repository {
    List<Contact> getAllContacts();

    void getAllContacts(LoadContactsCallback callback);

    Long getHiddenNumberContactId();

    void insertContact(Contact contact);

    void updateContact(Contact contact);

    void deleteContact(Contact contact);

    void getRecordings(Contact contact, LoadRecordingsCallback callback);

    List<Recording> getRecordings(Contact contact);

    void insertRecording(Recording recording);

    void updateRecording(Recording recording);

    void deleteRecording(Recording recording);

    //Contacts:
    interface LoadContactsCallback {
        void onContactsLoaded(List<Contact> contacts);
    }

    //Recordings:
    interface LoadRecordingsCallback {
        void onRecordingsLoaded(List<Recording> recordings);
    }
}
