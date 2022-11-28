package com.threebanders.recordr.ui.contact

import android.animation.Animator
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.provider.CallLog
import android.text.InputType
import android.view.*
import android.view.ContextMenu.ContextMenuInfo
import android.view.View.OnLongClickListener
import android.widget.*
import androidx.appcompat.widget.Toolbar
import androidx.core.app.NavUtils
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.afollestad.materialdialogs.DialogAction
import com.afollestad.materialdialogs.MaterialDialog
import com.threebanders.recordr.R
import com.threebanders.recordr.ui.BaseActivity
import com.threebanders.recordr.ui.BaseActivity.LayoutType
import com.threebanders.recordr.ui.BaseFragment
import com.threebanders.recordr.ui.MainViewModel
import com.threebanders.recordr.ui.player.PlayerActivity
import core.threebanders.recordr.CoreUtil
import core.threebanders.recordr.CrLog
import core.threebanders.recordr.data.Contact
import core.threebanders.recordr.data.Recording
import core.threebanders.recordr.recorder.Recorder
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

open class ContactDetailFragment : BaseFragment() {
    protected var adapter: RecordingAdapter? = null
    protected var recordingsRecycler: RecyclerView? = null
    private var detailView: RelativeLayout? = null
    protected var selectMode = false
    lateinit var mainViewModel: MainViewModel
    protected var selectedItems: MutableList<Int>? = ArrayList()

    private var selectedItemsDeleted = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        adapter = RecordingAdapter(ArrayList(0))
        mainViewModel = ViewModelProvider(mainActivity!!).get(
            MainViewModel::class.java
        )

        if (savedInstanceState != null) {
            selectMode = savedInstanceState.getBoolean(SELECT_MODE_KEY)
            selectedItems = savedInstanceState.getIntegerArrayList(SELECTED_ITEMS_KEY)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        detailView =
            inflater.inflate(
                R.layout.contact_detail_fragment, container,
                false
            ) as RelativeLayout
        recordingsRecycler = detailView!!.findViewById(R.id.recordings)
        recordingsRecycler!!.setLayoutManager(
            LinearLayoutManager(
                mainActivity
            )
        )
        recordingsRecycler!!.addItemDecoration(
            DividerItemDecoration(
                context,
                DividerItemDecoration.VERTICAL
            )
        )
        recordingsRecycler!!.adapter = adapter

        return detailView
    }

    override fun onResume() {
        super.onResume()

        val mainViewModel = ViewModelProvider(mainActivity!!).get(
            MainViewModel::class.java
        )
        mainViewModel.loadRecordings()
    }

    protected fun onDeleteSelectedRecordings() {
        MaterialDialog.Builder(mainActivity!!)
            .title(R.string.delete_recording_confirm_title)
            .content(
                String.format(
                    resources.getString(
                        R.string.delete_recording_confirm_message
                    ),
                    selectedItems!!.size
                )
            )
            .positiveText(android.R.string.ok)
            .negativeText(android.R.string.cancel)
            .icon(mainActivity!!.resources.getDrawable(R.drawable.warning))
            .onPositive { dialog: MaterialDialog, _: DialogAction ->
                val result = mainViewModel!!.deleteRecordings(
                    selectedRecordings
                )
                if (result != null) MaterialDialog.Builder(mainActivity!!)
                    .title(result.title)
                    .content(result.message)
                    .icon(resources.getDrawable(result.icon))
                    .positiveText(android.R.string.ok)
                    .show() else {
                    if (adapter!!.itemCount == 0) {
                        val noContent = mainActivity!!.findViewById<View>(R.id.no_content_detail)
                        if (noContent != null) noContent.visibility = View.VISIBLE
                    }
                    clearSelectMode()
                }
            }
            .show()
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        setDetailsButtonsListeners()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putBoolean(SELECT_MODE_KEY, selectMode)
        outState.putIntegerArrayList(SELECTED_ITEMS_KEY, selectedItems as ArrayList<Int>?)
    }

    open fun paintViews() {
        adapter!!.replaceData(mainViewModel.records.value!!, callDetails)
        if (selectMode) putInSelectMode(false) else toggleSelectModeActionBar(false)
        val noContent = detailView!!.findViewById<TextView>(R.id.no_content_detail)
        if (mainViewModel.records.value!!.size > 0) noContent.visibility =
            View.GONE else noContent.visibility = View.VISIBLE
    }

    fun removeRecording() {
        adapter!!.removeItem(mainViewModel.deletedRecording.value)
    }

    protected fun putInSelectMode(animate: Boolean) {
        selectMode = true
        toggleSelectModeActionBar(animate)
        redrawRecordings()
    }

    protected open fun toggleSelectModeActionBar(animate: Boolean) {
        val navigateBackBtn = mainActivity!!.findViewById<ImageButton>(R.id.navigate_back)
        val closeBtn = mainActivity!!.findViewById<ImageButton>(R.id.close_select_mode)
        val moveBtn = mainActivity!!.findViewById<ImageButton>(R.id.actionbar_select_move)
        val selectAllBtn = mainActivity!!.findViewById<ImageButton>(R.id.actionbar_select_all)
        val infoBtn = mainActivity!!.findViewById<ImageButton>(R.id.actionbar_info)
        val menuRightBtn = mainActivity!!.findViewById<ImageButton>(R.id.contact_detail_menu)
        val menuRightSelectedBtn =
            mainActivity!!.findViewById<ImageButton>(R.id.contact_detail_selected_menu)
        toggleTitle()
        if (mainActivity!!.layoutType == LayoutType.SINGLE_PANE) if (selectMode) hideView(
            navigateBackBtn,
            animate
        ) else showView(navigateBackBtn, animate)
        if (selectMode) showView(closeBtn, animate) else hideView(closeBtn, animate)
        if (selectMode) showView(moveBtn, animate) else hideView(moveBtn, animate)
        if (selectMode) {
            if (checkIfSelectedRecordingsDeleted()) disableMoveBtn() else enableMoveBtn()
        }
        if (selectMode) showView(selectAllBtn, animate) else hideView(selectAllBtn, animate)
        if (selectMode) showView(infoBtn, animate) else hideView(infoBtn, animate)
        if (selectMode) showView(menuRightSelectedBtn, animate) else hideView(
            menuRightSelectedBtn,
            animate
        )
        if (selectMode) hideView(menuRightBtn, animate) else showView(menuRightBtn, animate)
        if (mainActivity!!.layoutType == LayoutType.DOUBLE_PANE) {
            val hamburger = mainActivity!!.findViewById<ImageButton>(R.id.hamburger)
            if (selectMode) hideView(hamburger, animate) else showView(hamburger, animate)
        }
    }

    protected open fun shareRecorde(path: String?) {
        try {
            val f = File(path!!)
            val uri = context?.let {
                FileProvider.getUriForFile(
                    it,
                    "com.threebanders.recordr.CrApp.provider",
                    f
                )
            }
            val share = Intent(Intent.ACTION_SEND)
            share.putExtra(Intent.EXTRA_STREAM, uri)
            share.type = "audio/*"
            share.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            startActivity(Intent.createChooser(share, "Share audio File"))
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        }
    }

    protected open fun toggleTitle() {
        val title = mainActivity!!.findViewById<TextView>(R.id.actionbar_title)
        if (mainActivity!!.layoutType == LayoutType.DOUBLE_PANE) {
            val params = title.layoutParams as Toolbar.LayoutParams
            params.gravity = if (selectMode) Gravity.START else Gravity.CENTER
            title.layoutParams = params
        }
        if (selectMode) title.text = selectedItems!!.size.toString() else {
            if (mainActivity!!.layoutType == LayoutType.SINGLE_PANE) title.text =
                mainViewModel.contact.value!!.contactName else title.setText(R.string.app_name)
        }
    }

    private fun fadeEffect(view: View, finalAlpha: Float, finalVisibility: Int) {
        view.animate()
            .alpha(finalAlpha)
            .setDuration(EFFECT_TIME.toLong())
            .setListener(object : Animator.AnimatorListener {
                override fun onAnimationStart(animator: Animator) {}
                override fun onAnimationEnd(animator: Animator) {
                    view.visibility = finalVisibility
                }

                override fun onAnimationCancel(animator: Animator) {}
                override fun onAnimationRepeat(animator: Animator) {}
            })
    }

    protected fun hideView(v: View?, animate: Boolean) {
        if (animate) fadeEffect(v!!, 0.0f, View.GONE) else {
            v!!.alpha = 0.0f //poate lipsi?
            v.visibility = View.GONE
        }
    }

    protected fun showView(vw: View?, animate: Boolean) {
        if (animate) fadeEffect(vw!!, 1f, View.VISIBLE) else {
            vw?.alpha = 1f //poate lipsi?
            vw?.visibility = View.VISIBLE
        }
    }

    protected fun clearSelectMode() {
        selectMode = false
        toggleSelectModeActionBar(true)
        redrawRecordings()
        selectedItems!!.clear()
    }

    private fun modifyMargins(recording: View) {
        val checkBox = recording.findViewById<CheckBox>(R.id.recording_checkbox)
        val res = requireContext().resources
        checkBox.visibility = if (selectMode) View.VISIBLE else View.GONE
        val lpCheckBox = checkBox.layoutParams as RelativeLayout.LayoutParams
        lpCheckBox.marginStart =
            if (selectMode) res.getDimension(R.dimen.recording_checkbox_visible_start_margin)
                .toInt() else res.getDimension(R.dimen.recording_checkbox_gone_start_margin).toInt()
        checkBox.layoutParams = lpCheckBox
        val recordingAdorn = recording.findViewById<ImageView>(R.id.recording_adorn)
        val lpRecAdorn = recordingAdorn.layoutParams as RelativeLayout.LayoutParams
        lpRecAdorn.marginStart =
            if (selectMode) res.getDimension(R.dimen.recording_adorn_selected_margin_start)
                .toInt() else res.getDimension(R.dimen.recording_adorn_unselected_margin_start)
                .toInt()
        recordingAdorn.layoutParams = lpRecAdorn
        val title = recording.findViewById<TextView>(R.id.recording_title)
        val lpTitle = title.layoutParams as RelativeLayout.LayoutParams
        lpTitle.marginStart =
            if (selectMode) res.getDimension(R.dimen.recording_title_selected_margin_start)
                .toInt() else res.getDimension(R.dimen.recording_title_unselected_margin_start)
                .toInt()
        title.layoutParams = lpTitle
    }

    private fun selectRecording(recording: View) {
        val checkBox = recording.findViewById<CheckBox>(R.id.recording_checkbox)
        checkBox.isChecked = true
    }

    private fun deselectRecording(recording: View) {
        val checkBox = recording.findViewById<CheckBox>(R.id.recording_checkbox)
        checkBox.isChecked = false
    }

    protected fun enableMoveBtn() {
        val moveBtn = mainActivity!!.findViewById<ImageButton>(R.id.actionbar_select_move)
        moveBtn.isEnabled = true
        moveBtn.imageAlpha = 255
    }

    protected fun disableMoveBtn() {
        val moveBtn = mainActivity!!.findViewById<ImageButton>(R.id.actionbar_select_move)
        moveBtn.isEnabled = false
        moveBtn.imageAlpha = 75
    }

    private fun redrawRecordings() {
        for (i in 0 until adapter!!.itemCount) adapter!!.notifyItemChanged(i)
    }

    private fun manageSelectRecording(recording: View, adapterPosition: Int, exists: Boolean) {
        if (!removeIfPresentInSelectedItems(adapterPosition)) {
            selectedItems!!.add(adapterPosition)
            selectRecording(recording)
            if (!exists) {
                selectedItemsDeleted++
                disableMoveBtn()
            }
        } else {
            deselectRecording(recording)
            if (!exists) selectedItemsDeleted--
            if (selectedItemsDeleted == 0) enableMoveBtn()
        }
        if (selectedItems!!.isEmpty()) clearSelectMode() else toggleTitle()
    }

    private val selectedRecordings: List<Recording?>
        get() {
            val list: MutableList<Recording?> = ArrayList()
            for (adapterPosition in selectedItems!!) list.add(adapter!!.getItem(adapterPosition))
            return list
        }

    private fun removeIfPresentInSelectedItems(adapterPosition: Int): Boolean {
        return if (selectedItems!!.contains(adapterPosition)) {
            selectedItems!!.remove(adapterPosition) //fără casting îl interpretează ca poziție
            //în selectedItems
            true
        } else false
    }

    protected fun checkIfSelectedRecordingsDeleted(): Boolean {
        for (recording in selectedRecordings) if (!recording!!.exists()) return true
        return false
    }

    private fun onShowStorageInfo() {
        var sizePrivate: Long = 0
        var sizePublic: Long = 0
        for (recording in adapter!!.getRecordings()!!) {
            val size = File(recording!!.path).length()
            if (recording.isSavedInPrivateSpace(mainActivity)) sizePrivate += size else sizePublic += size
        }
        val dialog = MaterialDialog.Builder(mainActivity!!)
            .title(R.string.storage_info)
            .customView(R.layout.info_storage_dialog, false)
            .positiveText(android.R.string.ok).build()
        val privateStorage = dialog.view.findViewById<TextView>(R.id.info_storage_private_data)
        privateStorage.text = CoreUtil.getFileSizeHuman(sizePrivate)
        val publicStorage = dialog.view.findViewById<TextView>(R.id.info_storage_public_data)
        publicStorage.text = CoreUtil.getFileSizeHuman(sizePublic)
        dialog.show()
    }

    protected fun onRenameRecording() {
        MaterialDialog.Builder(mainActivity!!)
            .title(R.string.rename_recording_title)
            .inputType(InputType.TYPE_CLASS_TEXT)
            .input(
                mainActivity!!.resources.getString(R.string.rename_recording_input_text),
                null, false
            ) { dialog: MaterialDialog, input: CharSequence ->
                if (selectedItems!!.size != 1) {
                    CrLog.log(
                        CrLog.WARN,
                        "Calling onRenameClick when multiple recordings are selected"
                    )
                    return@input
                }
                val result = mainViewModel!!.renameRecording(input, selectedRecordings[0])
                if (result != null) MaterialDialog.Builder(mainActivity!!)
                    .title(result.title)
                    .content(result.message)
                    .icon(resources.getDrawable(result.icon))
                    .positiveText(android.R.string.ok)
                    .show() else adapter!!.notifyItemChanged(selectedItems!![0])
            }.show()
    }

    protected fun onSelectAll() {
        val notSelected: MutableList<Int> = ArrayList()
        for (i in 0 until adapter!!.itemCount) notSelected.add(i)
        notSelected.removeAll(selectedItems!!)
        for (position in notSelected) {
            selectedItems!!.add(position)
            adapter!!.notifyItemChanged(position)
            //https://stackoverflow.com/questions/33784369/recyclerview-get-view-at-particular-position
            val selectedRecording = recordingsRecycler!!.layoutManager!!
                .findViewByPosition(position)
            selectedRecording?.let { selectRecording(it) }
        }
        toggleTitle()
    }

    protected fun onRecordingInfo() {
        if (selectedItems!!.size > 1) {
            var totalSize: Long = 0
            for (position in selectedItems!!) {
                val recording = adapter!!.getItem(position)
                totalSize += recording!!.size
            }
            MaterialDialog.Builder(mainActivity!!)
                .title(R.string.recordings_info_title)
                .content(
                    String.format(
                        requireContext().resources.getString(R.string.recordings_info_text),
                        CoreUtil.getFileSizeHuman(totalSize)
                    )
                )
                .positiveText(android.R.string.ok)
                .show()
            return
        }
        val dialog = MaterialDialog.Builder(mainActivity!!)
            .title(R.string.recording_info_title)
            .customView(R.layout.info_dialog, false)
            .positiveText(android.R.string.ok).build()

        //There should be only one if we are here:
        if (selectedItems!!.size != 1) {
            CrLog.log(CrLog.WARN, "Calling onInfoClick when multiple recordings are selected")
            return
        }
        val recording = adapter!!.getItem(
            selectedItems!![0]
        )
        val date = dialog.view.findViewById<TextView>(R.id.info_date_data)
        date.text = String.format("%s %s", recording!!.date, recording.time)
        val size = dialog.view.findViewById<TextView>(R.id.info_size_data)
        size.text = CoreUtil.getFileSizeHuman(recording.size)
        val source = dialog.view.findViewById<TextView>(R.id.info_source_data)
        source.text = recording.source
        val format = dialog.view.findViewById<TextView>(R.id.info_format_data)
        format.text = recording.getHumanReadingFormat(mainActivity)
        val length = dialog.view.findViewById<TextView>(R.id.info_length_data)
        length.text = CoreUtil.getDurationHuman(recording.length, true)
        val path = dialog.view.findViewById<TextView>(R.id.info_path_data)
        path.text =
            if (recording.isSavedInPrivateSpace(mainActivity)) mainActivity!!.resources.getString(R.string.private_storage) else recording.path
        if (!recording.exists()) {
            path.text = String.format(
                "%s%s",
                path.text,
                mainActivity!!.resources.getString(R.string.nonexistent_file)
            )
            path.setTextColor(requireContext().resources.getColor(android.R.color.holo_red_light))
        }
        dialog.show()
    }


    protected open fun setDetailsButtonsListeners() {
        val navigateBack = mainActivity!!.findViewById<ImageButton>(R.id.navigate_back)
        navigateBack.setOnClickListener { view: View? ->
            NavUtils.navigateUpFromSameTask(
                mainActivity!!
            )
        }
        val menuButtonSelectOff = mainActivity!!.findViewById<ImageButton>(R.id.contact_detail_menu)
        menuButtonSelectOff.setOnClickListener { vw: View? ->
            val popupMenu = PopupMenu(
                mainActivity, vw!!
            )
            popupMenu.setOnMenuItemClickListener { item: MenuItem ->
                when (item.itemId) {
                    R.id.storage_info -> {
                        onShowStorageInfo()
                        return@setOnMenuItemClickListener false
                    }
                    else -> return@setOnMenuItemClickListener false
                }
            }
            val inflater = popupMenu.menuInflater
            inflater.inflate(R.menu.contact_popup, popupMenu.menu)
            popupMenu.show()
        }
        val closeBtn = mainActivity!!.findViewById<ImageButton>(R.id.close_select_mode)
        closeBtn.setOnClickListener { v: View? -> clearSelectMode() }
        val menuButtonSelectOn =
            mainActivity!!.findViewById<ImageButton>(R.id.contact_detail_selected_menu)
        menuButtonSelectOn.setOnClickListener { v: View ->
            val popupMenu = PopupMenu(
                mainActivity, v
            )
            popupMenu.setOnMenuItemClickListener { item: MenuItem ->
                when (item.itemId) {
                    R.id.rename_recording -> {
                        onRenameRecording()
                        return@setOnMenuItemClickListener true
                    }
                    else -> return@setOnMenuItemClickListener false
                }
            }
            val inflater = popupMenu.menuInflater
            inflater.inflate(R.menu.recording_selected_popup, popupMenu.menu)
            val renameMenuItem = popupMenu.menu.findItem(R.id.rename_recording)
            val recording = (recordingsRecycler!!.adapter as RecordingAdapter?)!!.getItem(
                selectedItems!![0]
            )
            if (selectedItems!!.size > 1 || !recording!!.exists()) renameMenuItem.isEnabled = false
            popupMenu.show()
        }
        val moveBtn = mainActivity!!.findViewById<ImageButton>(R.id.actionbar_select_move)
        registerForContextMenu(moveBtn)
        //foarte necesar. Altfel meniul contextual va fi arătat numai la long click.
        moveBtn.setOnClickListener { obj: View -> obj.showContextMenu() }
        val selectAllBtn = mainActivity!!.findViewById<ImageButton>(R.id.actionbar_select_all)
        selectAllBtn.setOnClickListener { v: View? -> onSelectAll() }
        val deleteBtn = mainActivity!!.findViewById<ImageButton>(R.id.delete_recording)
        deleteBtn.setOnClickListener { v: View? -> onDeleteSelectedRecordings() }
        val infoBtn = mainActivity!!.findViewById<ImageButton>(R.id.actionbar_info)
        infoBtn.setOnClickListener { view: View? -> onRecordingInfo() }


    }


    override fun onCreateContextMenu(menu: ContextMenu, v: View, menuInfo: ContextMenuInfo?) {

    }

    override fun onContextItemSelected(item: MenuItem): Boolean {
        return false
    }

    inner class RecordingHolder(inflater: LayoutInflater, parent: ViewGroup?) :
        RecyclerView.ViewHolder(inflater.inflate(R.layout.recording, parent, false)),
        View.OnClickListener, OnLongClickListener {
        var title: TextView
        var recordingType: ImageView
        var recordingAdorn: ImageView
        val recordingshare: ImageView
        var exclamation: ImageView
        var checkBox: CheckBox

        init {
            recordingType = itemView.findViewById(R.id.recording_type)
            recordingshare = itemView.findViewById(R.id.recording_share)
            title = itemView.findViewById(R.id.recording_title)
            checkBox = itemView.findViewById(R.id.recording_checkbox)
            recordingAdorn = itemView.findViewById(R.id.recording_adorn)
            exclamation = itemView.findViewById(R.id.recording_exclamation)
            itemView.setOnClickListener(this)
            itemView.setOnLongClickListener(this)
        }

        override fun onLongClick(v: View): Boolean {
            if (!selectMode) putInSelectMode(true)
            val recording = adapter!!.getItem(adapterPosition)
            manageSelectRecording(v, this.adapterPosition, recording!!.exists())
            return true
        }

        override fun onClick(v: View) {
            val recording = adapter!!.getItem(adapterPosition)
            if (selectMode) manageSelectRecording(
                v,
                this.adapterPosition,
                recording!!.exists()
            ) else { //usual short click
                if (recording!!.exists()) {
                    val playIntent = Intent(mainActivity, PlayerActivity::class.java)
                    playIntent.putExtra(RECORDING_EXTRA, recording)
                    startActivity(playIntent)
                } else Toast.makeText(mainActivity, R.string.audio_file_missing, Toast.LENGTH_SHORT)
                    .show()
            }
        }
    }

    //A devenit public pentru a funcționa adapter.replaceData() în UnassignedRecordingsFragment
    inner class RecordingAdapter internal constructor(private var recordings: MutableList<Recording>) :
        RecyclerView.Adapter<RecordingHolder>() {
        private var contactList: List<Contact?>? = null
        fun getRecordings(): List<Recording?>? {
            return recordings
        }

        //A devenit public pentru a funcționa adapter.replaceData() în UnassignedRecordingsFragment
        fun replaceData(recordings: MutableList<Recording>, contactList: List<Contact>) {
            this.recordings = recordings
            this.contactList = contactList
            notifyDataSetChanged()
        }

        fun removeItem(recording: Recording?) {
            val position = recordings!!.indexOf(recording)
            recordings.remove(recording)
            notifyItemRemoved(position)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecordingHolder {
            val layoutInflater = LayoutInflater.from(mainActivity)
            return RecordingHolder(layoutInflater, parent)
        }

        //A devenit public pentru a funcționa UnassignedRecordingsFragment
        fun getItem(position: Int): Recording? {
            return recordings[position]
        }

        override fun onBindViewHolder(holder: RecordingHolder, position: Int) {
            val recording = recordings[position]
            val adornRes: Int
            adornRes = when (recording.format) {
                Recorder.WAV_FORMAT -> if (mainActivity!!.settledTheme == BaseActivity.LIGHT_THEME) R.drawable.sound_symbol_wav_light else R.drawable.sound_symbol_wav_dark
                Recorder.AAC_HIGH_FORMAT -> if (mainActivity!!.settledTheme == BaseActivity.LIGHT_THEME) R.drawable.sound_symbol_aac128_light else R.drawable.sound_symbol_aac128_dark
                Recorder.AAC_BASIC_FORMAT -> if (mainActivity!!.settledTheme == BaseActivity.LIGHT_THEME) R.drawable.sound_symbol_aac32_light else R.drawable.sound_symbol_aac32_dark
                else -> if (mainActivity!!.settledTheme == BaseActivity.LIGHT_THEME) R.drawable.sound_symbol_aac64_light else R.drawable.sound_symbol_aac64_dark
            }
            for (i in contactList!!.indices) {
                if (contactList!![i]!!
                        .daytime.equals(recording.dateRecord, ignoreCase = true)
                ) {
                    if (contactList!![i]!!
                            .isMissed
                    ) holder.title.text =
                        getString(R.string.missed_by) + " " + contactList!![i]!!.phoneNumber else holder.title.text =
                        contactList!![i]!!.phoneNumber
                }
            }
            if (mainViewModel.contact.value == null || !mainViewModel.contact.value!!
                    .isPrivateNumber
            ) holder.recordingType.setImageResource(if (recording.isIncoming) R.drawable.incoming else if (mainActivity!!.settledTheme == BaseActivity.Companion.LIGHT_THEME) R.drawable.outgoing_light else R.drawable.outgoing_dark)
            holder.recordingAdorn.setImageResource(adornRes)
            holder.checkBox.setOnClickListener { view: View ->
                manageSelectRecording(
                    view,
                    position,
                    recording.exists()
                )
            }

            holder.recordingshare.setOnClickListener { view: View? ->
                shareRecorde(
                    recording.path
                )
            }

            if (!recording.exists()) markNonexistent(holder)
            modifyMargins(holder.itemView)
            if (selectedItems!!.contains(position)) selectRecording(holder.itemView) else deselectRecording(
                holder.itemView
            )
        }

        private fun markNonexistent(holder: RecordingHolder) {
            holder.exclamation.visibility = View.VISIBLE
            val filter =
                if (mainActivity!!.settledTheme == BaseActivity.Companion.LIGHT_THEME) Color.argb(
                    255,
                    0,
                    0,
                    0
                ) else Color.argb(255, 255, 255, 255)
            holder.recordingAdorn.setColorFilter(filter)
            holder.recordingType.setColorFilter(filter)
            holder.recordingAdorn.imageAlpha = 100
            holder.recordingType.imageAlpha = 100
            holder.title.alpha = 0.5f
        }

        private fun unMarkNonexistent(holder: RecordingHolder) {
            holder.exclamation.visibility = View.GONE
            holder.recordingAdorn.colorFilter = null
            holder.recordingType.colorFilter = null
            holder.recordingType.imageAlpha = 255
            holder.recordingAdorn.imageAlpha = 255
            holder.title.alpha = 1f
        }

        override fun onViewRecycled(holder: RecordingHolder) {
            super.onViewRecycled(holder)
            unMarkNonexistent(holder)
        }

        override fun getItemCount(): Int {
            return recordings.size
        }
    }

    protected val callDetails: List<Contact>
        get() = try {
            val contactList: MutableList<Contact> = ArrayList()
            val managedCursor =
                requireContext().contentResolver.query(
                    CallLog.Calls.CONTENT_URI,
                    null,
                    null,
                    null,
                    null
                )
            val number = managedCursor!!.getColumnIndex(CallLog.Calls.NUMBER)
            val name = managedCursor.getColumnIndex(CallLog.Calls.CACHED_NAME)
            val type = managedCursor.getColumnIndex(CallLog.Calls.TYPE)
            val date = managedCursor.getColumnIndex(CallLog.Calls.DATE)
            val duration = managedCursor.getColumnIndex(CallLog.Calls.DURATION)

            while (managedCursor.moveToNext()) {
                val phNumber = managedCursor.getString(number) // mobile number
                val phName = managedCursor.getString(name) // name
                val callType = managedCursor.getString(type) // call type
                val callDate = managedCursor.getString(date) // call date
                val callDayTime = Date(java.lang.Long.valueOf(callDate))
                val callDuration = managedCursor.getString(duration)
                var dir: String? = null

                when (callType.toInt()) {
                    CallLog.Calls.OUTGOING_TYPE -> dir = "OUTGOING"
                    CallLog.Calls.INCOMING_TYPE -> dir = "INCOMING"
                    CallLog.Calls.MISSED_TYPE -> dir = "MISSED"
                }

                var value: String? = ""
                value = phName
                if (phName == null || phName.isEmpty())
                    value = phNumber
                val contact = Contact()
                if (dir != null && dir.equals("MISSED", ignoreCase = true)) contact.isMissed = true
                contact.phoneNumber = value
                contact.contactName = callDuration
                contact.daytime =
                    SimpleDateFormat("dd/MM/yyyy hh:mm a", Locale.US).format(callDayTime)
                contactList.add(contact)
            }
            managedCursor.close()

            contactList
        } catch (ex: Exception) {
            ex.printStackTrace()
            ArrayList()
        }

    companion object {
        private const val SELECT_MODE_KEY = "select_mode_key"
        private const val SELECTED_ITEMS_KEY = "selected_items_key"
        private const val EFFECT_TIME = 250
        const val RECORDING_EXTRA = "recording_extra"

        fun newInstance(contact: Contact?): ContactDetailFragment {
            val args = Bundle()
            args.putParcelable(ContactsListFragment.Companion.ARG_CONTACT, contact)
            val fragment = ContactDetailFragment()
            fragment.arguments = args

            return fragment
        }
    }
}
