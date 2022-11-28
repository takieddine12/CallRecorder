package com.threebanders.recordr.ui

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.threebanders.recordr.R
import com.threebanders.recordr.common.DialogInfo
import core.threebanders.recordr.Core
import core.threebanders.recordr.CrLog
import core.threebanders.recordr.data.Contact
import core.threebanders.recordr.data.Recording
import core.threebanders.recordr.data.Repository
import java.io.File

class MainViewModel : ViewModel() {
    private val repository: Repository
    var contact = MutableLiveData<Contact?>()
    private var contactList: List<Contact> = ArrayList()
    var contacts = MutableLiveData(contactList)

    private val recordList: MutableList<Recording> = ArrayList()
    var records = MutableLiveData(recordList)

    var deletedRecording = MutableLiveData<Recording?>()

    init {
        repository = Core.getRepository()
        setupAllContacts()
    }

    private fun setupAllContacts() {
        contactList = repository.allContacts
        contacts.value = contactList
    }

    fun loadRecordings() {
        repository.getRecordings(contact.value) { recordings: List<Recording>? ->
            recordList.clear()
            recordList.addAll(recordings!!)
            records.postValue(recordList)
        }
    }

    fun deleteRecordings(recordings: List<Recording?>): DialogInfo? {
        for (recording in recordings) {
            try {
                recording!!.delete(repository)
                deletedRecording.postValue(recording)
            } catch (exc: Exception) {
                CrLog.log(CrLog.ERROR, "Error deleting the selected recording(s): " + exc.message)
                return DialogInfo(
                    R.string.error_title,
                    R.string.error_deleting_recordings,
                    R.drawable.error
                )
            }
        }
        return null
    }

    fun renameRecording(input: CharSequence, recording: Recording?): DialogInfo? {
        if (Recording.hasIllegalChar(input)) return DialogInfo(
            R.string.information_title,
            R.string.rename_illegal_chars,
            R.drawable.info
        )
        val parent = File(recording!!.path).parent
        val oldFileName = File(recording.path).name
        val ext = oldFileName.substring(oldFileName.length - 3)
        val newFileName = "$input.$ext"
        if (File(parent, newFileName).exists()) return DialogInfo(
            R.string.information_title,
            R.string.rename_already_used,
            R.drawable.info
        )
        try {
            if (File(recording.path).renameTo(File(parent, newFileName))) {
                recording.path = File(parent, newFileName).absolutePath
                recording.isNameSet = true
                recording.update(repository)
            } else throw Exception("File.renameTo() has returned false.")
        } catch (e: Exception) {
            CrLog.log(CrLog.ERROR, "Error renaming the recording:" + e.message)
            return DialogInfo(R.string.error_title, R.string.rename_error, R.drawable.error)
        }
        return null
    }
}