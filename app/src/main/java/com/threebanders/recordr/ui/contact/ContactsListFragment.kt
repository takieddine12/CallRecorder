package com.threebanders.recordr.ui.contact

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.os.Bundle
import android.view.*
import android.widget.*
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.threebanders.recordr.R
import com.threebanders.recordr.ui.BaseActivity
import com.threebanders.recordr.ui.BaseActivity.LayoutType
import com.threebanders.recordr.ui.BaseFragment
import com.threebanders.recordr.ui.MainViewModel
import core.threebanders.recordr.CoreUtil
import core.threebanders.recordr.data.Contact

class ContactsListFragment : BaseFragment() {
    private var adapter: ContactsAdapter? = null
    private var contactsRecycler: RecyclerView? = null
    lateinit var mainViewModel: MainViewModel

    /**
     * Poziția curentă în adapter. Necesară doar în DOUBLE_PANE. Este setată din
     * ViewHolder.getAdapterPosition() cînd se clichează și e folosită la setarea detaliului curent și la
     * selectarea contactului curent în onBindViewHolder().
     */
    private var currentPos = 0
    private var parentActivity: BaseActivity? = null
    private var colorPointer = 0

    @SuppressLint("UseSparseArrays")
    private val contactsColors: MutableMap<Long, Int> = HashMap()

    override fun onAttach(context: Context) {
        super.onAttach(context)
        parentActivity = baseActivity
    }

    override fun onDetach() {
        super.onDetach()
        parentActivity = null
    }

    /**
     * Apelată din ContactDetailFragment după ștergerea unui contact.
     */
    //    @Override
    //    public void resetCurrentPosition() {
    //        currentPos = 0;
    //    }
    override fun onResume() {
        super.onResume()
        //        if (ContactsListActivityMain.PERMS_NOT_GRANTED == 0)
//            presenter.loadContacts();
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putInt(CURRENT_POS_KEY, currentPos)
    }

    private fun manageDetail() {
        val detailFragment =
            parentActivity!!.supportFragmentManager.findFragmentById(R.id.contact_detail_fragment_container)
        //        if (adapter.getItemCount() > 0) {
//            if (detailFragment == null || ((ContactDetailContract.View) detailFragment).isInvalid())
//                mainViewModel.contact.setValue(adapter.getItem(currentPos));
//                setCurrentDetail();
//        } else
        setCurrentDetail()
    }

    fun showContacts() {
        contactsRecycler!!.adapter = adapter
        adapter!!.notifyDataSetChanged()
        if (baseActivity!!.layoutType == LayoutType.DOUBLE_PANE) manageDetail()
        val noContent = parentActivity!!.findViewById<TextView>(R.id.no_content_list)
        if (adapter!!.itemCount > 0) noContent.visibility = View.GONE else noContent.visibility =
            View.VISIBLE
    }

    /**
     * Necesară deoarece există 2 variante de selectContact().
     */
    private fun realSelectContact(contactSlot: View?) {
        if (contactSlot != null) {
            contactSlot.findViewById<View>(R.id.tablet_current_selection).visibility =
                View.VISIBLE
            if (parentActivity!!.settledTheme == BaseActivity.Companion.LIGHT_THEME) contactSlot.setBackgroundColor(
                resources.getColor(R.color.slotLightSelected)
            ) else contactSlot.setBackgroundColor(
                resources.getColor(R.color.slotDarkSelected)
            )
        }
    }

    /**
     * Apelată la clicarea pe un contact cu poziția în adapter.
     */
    private fun selectContact(position: Int) {
        val contactSlot = contactsRecycler!!.layoutManager!!.findViewByPosition(position)
        realSelectContact(contactSlot)
    }

    /**
     * Apelată în onBindViewHolder() cu View-ul corespunzător.
     */
    private fun selectContactWithView(contactSlot: View) {
        realSelectContact(contactSlot)
    }

    private fun deselectContact(position: Int) {
        val contactSlot = contactsRecycler!!.layoutManager!!.findViewByPosition(position)
        if (contactSlot != null) {
            contactSlot.findViewById<View>(R.id.tablet_current_selection).visibility = View.GONE
            if (parentActivity!!.settledTheme == BaseActivity.Companion.LIGHT_THEME) contactSlot.setBackgroundColor(
                resources.getColor(R.color.slotLight)
            ) else contactSlot.setBackgroundColor(resources.getColor(R.color.slotAndDetailHeaderDark))
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState != null) {
            currentPos = savedInstanceState.getInt(CURRENT_POS_KEY)
        }
        adapter = ContactsAdapter(mainViewModel!!.contacts.value)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val fragmentRoot = inflater.inflate(R.layout.list_contacts_fragment, container, false)
        contactsRecycler = fragmentRoot.findViewById(R.id.listened_phones)
        contactsRecycler!!.setLayoutManager(LinearLayoutManager(parentActivity))
        mainViewModel = ViewModelProvider(mainActivity!!).get(
            MainViewModel::class.java
        )
        mainViewModel!!.contacts.observe(viewLifecycleOwner) { contacts: List<Contact?>? -> showContacts() }
        showContacts()
        return fragmentRoot
    }

    inner class ContactHolder internal constructor(inflater: LayoutInflater, parent: ViewGroup?) :
        RecyclerView.ViewHolder(inflater.inflate(R.layout.contact, parent, false)),
        View.OnClickListener {
        var contactPhoto: ImageView
        var mContactName: TextView
        var mPhoneNumber: TextView
        var contact: Contact? = null

        init {
            itemView.setOnClickListener(this)
            contactPhoto = itemView.findViewById(R.id.contact_photo)
            mContactName = itemView.findViewById(R.id.contact_name)
            mPhoneNumber = itemView.findViewById(R.id.phone_number)
        }

        override fun onClick(view: View) {
            val previousPos = currentPos
            currentPos = adapterPosition
            if (baseActivity!!.layoutType == LayoutType.SINGLE_PANE) {
                val detailIntent = Intent(context, ContactDetailActivity::class.java)
                detailIntent.putExtra(ARG_CONTACT, contact)
                parentActivity!!.startActivity(detailIntent)
            } else {
                setCurrentDetail()
                selectContact(currentPos)
                deselectContact(previousPos)
            }
        }
    }

    private fun setCurrentDetail() {
        val detailMenu = parentActivity!!.findViewById<ImageButton>(R.id.contact_detail_menu)
        if (mainViewModel!!.contact.value != null) {
            val contactDetail: ContactDetailFragment =
                ContactDetailFragment.Companion.newInstance(mainViewModel!!.contact.value)
            parentActivity!!.supportFragmentManager.beginTransaction()
                .replace(R.id.contact_detail_fragment_container, contactDetail)
                .commitAllowingStateLoss() //fără chestia asta îmi dă un Caused by:
            // java.lang.IllegalStateException: Can not perform this action after onSaveInstanceState cînd înlocuiesc fragmentul detail după adăugarea unui
            //contact nou. Soluția: https://stackoverflow.com/questions/7575921/illegalstateexception-can-not-perform-this-action-after-onsaveinstancestate-wit
        } else {
            //celelalte butoane nu pot să fie vizibile deoarece ștergerea unui contact nu se poate face cu selectMode on
            detailMenu.visibility = View.GONE
            val detailFragment =
                parentActivity!!.supportFragmentManager.findFragmentById(R.id.contact_detail_fragment_container)
            if (detailFragment != null) //dacă aplicația începe fără niciun contact detailFragment va fi null
                parentActivity!!.supportFragmentManager.beginTransaction().remove(detailFragment)
                    .commit()
        }
    }

    internal inner class ContactsAdapter(contactList: List<Contact?>?) :
        RecyclerView.Adapter<ContactHolder>() {
        private val contacts: List<Contact?>

        init {
            val updatedList: MutableList<Contact?> = ArrayList()
            for (contact in contactList!!) {
                var color: Int
                if (contactsColors.containsKey(contact!!.id)) color =
                    contactsColors[contact.id]!! else {
                    if (colorPointer == CoreUtil.colorList.size - 1) colorPointer = 0
                    color = CoreUtil.colorList[colorPointer++]
                    contactsColors[contact.id] = color
                }
                contact.color = color
                updatedList.add(contact)
            }
            contacts = updatedList
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ContactHolder {
            val layoutInflater = LayoutInflater.from(parentActivity)
            return ContactHolder(layoutInflater, parent)
        }

        override fun onBindViewHolder(holder: ContactHolder, position: Int) {
            val contact = contacts[position]
            if (contact!!.photoUri != null) {
                holder.contactPhoto.setImageURI(null) //cînd se schimbă succesiv 2 poze făcute de cameră se folosește același fișier și optimizările android fac necesar acest hack pentru a obține refresh-ul pozei
                holder.contactPhoto.setImageURI(contact.photoUri)
            } else {
                if (contact.isPrivateNumber) {
                    holder.contactPhoto.setImageResource(R.drawable.incognito)
                    holder.mPhoneNumber.visibility = View.GONE
                } else {
                    holder.contactPhoto.setImageResource(R.drawable.user_contact)
                    //PorteDuffColorFilter ia întotdeauna un aRGB. modificarea culorii funcționează în felul
                    //următor: user_contact.xml are în centru culoarea #E6E6E6 (luminozitate 230), mai mare decît
                    //toate culorile din listă. La margine are negru,luminozitate mai mică decît toate culorile
                    //din listă. Aplicînd modul LIGHTEN (https://developer.android.com/reference/android/graphics/PorterDuff.Mode.html#LIGHTEN)
                    // se inlocuiește totdeauna culoarea de pe margine și este păstrată culoarea din centru.
                    //Alternativa ar fi https://github.com/harjot-oberai/VectorMaster .
                    holder.contactPhoto.colorFilter =
                        PorterDuffColorFilter(contact.color, PorterDuff.Mode.LIGHTEN)
                }
            }
            holder.mContactName.text = contact.contactName
            holder.contact = contact
            if (!contact.isPrivateNumber) holder.mPhoneNumber.text = contact.phoneNumber
            if (parentActivity!!.layoutType == LayoutType.DOUBLE_PANE) holder.mPhoneNumber.visibility =
                View.GONE

            //codul de mai jos este necesar pentru că în momentul în care fragmentul pornește sau repornește
            // trebuie să marcheze contactul activ: primul la pornire, currentPos la repornire. Folosește o versiune
            //privată a selectContact() care ia ca parametru un View deaorece la pornire lista cu contacte a
            // adapterului nu este disponibilă.
            if (parentActivity!!.layoutType == LayoutType.DOUBLE_PANE) {
                if (position == currentPos) selectContactWithView(holder.itemView)
            }
        }

        fun getItem(position: Int): Contact? {
            return contacts[position]
        }

        override fun getItemCount(): Int {
            return contacts.size
        }
    }

    companion object {
        private const val CURRENT_POS_KEY = "current_pos"
        const val ARG_CONTACT = "arg_contact"
    }
}