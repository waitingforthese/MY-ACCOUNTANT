package rahul.jagtap.dmas

import android.app.ProgressDialog
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.view.WindowManager
import com.afollestad.materialdialogs.MaterialDialog
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import rahul.jagtap.dmas.databinding.ActivityAddGovtSchemeInfoBinding
import rahul.jagtap.dmas.extensions.toast
import rahul.jagtap.dmas.model.GovtSchemeInfo
import rahul.jagtap.dmas.utils.Utils
import java.text.SimpleDateFormat
import java.util.ArrayList
import java.util.Date
import java.util.Locale
import androidx.core.widget.doAfterTextChanged

class AddGovtSchemeInfoActivity : BaseActivity() {
    private val TAG = AddGovtSchemeInfoActivity::class.java.simpleName
    private var email: String? = ""
    private var username: String? = ""
    private var uid: String? = ""
    private var name: String? = ""
    var cpd: ProgressDialog? = null
    var govtSchemeInfo: GovtSchemeInfo? = null
    lateinit var binding: ActivityAddGovtSchemeInfoBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddGovtSchemeInfoBinding.inflate(layoutInflater)
        if (Utils.disableScreenshot) this.window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)
        setContentView(binding.root)
        // input-field migration: clear errors on edit
        listOf(binding.tilTitle, binding.tilTitleBgColor, binding.tilSchemeInfo)
            .forEach { til -> til.editText?.doAfterTextChanged { til.error = null } }
        setSupportActionBar(binding.toolbarLayout.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowTitleEnabled(false)
        binding.toolbarLayout.toolbarTitle?.text = if (govtSchemeInfo != null) "Edit नवनवीन माहिती" else "Create नवनवीन माहिती"
        email = app?.preferences?.loggedInUser?.email
        uid = app?.preferences?.loggedInUser?.uid
        username = app?.preferences?.loggedInUser?.username
        name = app?.preferences?.loggedInUser?.name
        govtSchemeInfo = intent.getParcelableExtra("govtSchemeInfo")

        binding.etTitleBgColor.setOnClickListener {
            // orange, yellow, blue, pink, golden
            val list = ArrayList<String>()
            list.add("Orange")
            list.add("Yellow")
            list.add("Blue")
            list.add("Pink")
            list.add("Golden")
            mContext?.let { it1 ->
                MaterialDialog.Builder(it1).items(list).itemsCallback { dialog: MaterialDialog?, itemView: View?, position: Int, text: CharSequence ->
                    run {
                        dialog?.dismiss()
                        binding.etTitleBgColor.setText(list[position])
                    }
                }.show()
            }
        }

        if (govtSchemeInfo != null) {
            binding.etTitle?.setText(govtSchemeInfo?.title)
            binding.etSchemeInfo?.setText(govtSchemeInfo?.schemeInfo)
            binding.etTitleBgColor?.setText(govtSchemeInfo?.titleColor)
        }

        binding.btnSubmit?.setOnClickListener {
            val strTitle = binding.etTitle.text.toString()
            val strTitleBgColor = binding.etTitleBgColor.text.toString()
            val strSchemeInfo = binding.etSchemeInfo.text.toString()
            if (TextUtils.isEmpty(strTitle)) {
                binding.tilTitle?.error = binding.tilTitle.hint.toString()
                binding.etTitle?.requestFocus()
                return@setOnClickListener
            }
            if (TextUtils.isEmpty(strTitleBgColor)) {
                binding.tilTitleBgColor?.error = binding.tilTitleBgColor.hint.toString()
                binding.etTitleBgColor?.requestFocus()
                return@setOnClickListener
            }
            if (TextUtils.isEmpty(strSchemeInfo)) {
                binding.tilSchemeInfo?.error = binding.tilSchemeInfo.hint.toString()
                binding.etSchemeInfo?.requestFocus()
                return@setOnClickListener
            }
            createDbRecord()
        }
    }

    private fun createDbRecord() {
        cpd = ProgressDialog(mContext)
        cpd?.setCancelable(false)
        cpd?.show()
        if (govtSchemeInfo == null) {
            val createdDateTime = SimpleDateFormat("dd-MM-yyyy HH:mm:ss", Locale.ENGLISH).format(Date())
            val billMap = HashMap<String, Any?>()
            billMap["email"] = email
            billMap["createdBy"] = name
            billMap["createdByUserName"] = username
            billMap["uid"] = uid
            billMap["createdDateTime"] = createdDateTime
            billMap["title"] = binding.etTitle.text.toString()
            billMap["titleColor"] = binding.etTitleBgColor.text.toString()
            billMap["schemeInfo"] = binding.etSchemeInfo.text.toString()
            billMap["timeStamp"] = System.currentTimeMillis()

            val pushKey = database.child(Utils.GOVT_SCHEMES_TABLE).push().key
            billMap["pushKey"] = pushKey

            val messageUserMap = HashMap<String, Any?>()
            messageUserMap["${Utils.GOVT_SCHEMES_TABLE}/$pushKey"] = billMap

            database.updateChildren(messageUserMap) { databaseError: DatabaseError?, databaseReference: DatabaseReference? ->
                if (databaseError != null) {
                    Log.e("db error", databaseError.message)
                }
            }
            if (cpd != null && cpd?.isShowing == true) cpd?.dismiss()
            toast("Entry added successfully")
        } else {
            govtSchemeInfo?.title = binding.etTitle.text.toString()
            govtSchemeInfo?.titleColor = binding.etTitleBgColor.text.toString()
            govtSchemeInfo?.schemeInfo = binding.etSchemeInfo.text.toString()
            database.child(Utils.GOVT_SCHEMES_TABLE).child(govtSchemeInfo?.pushKey!!).setValue(govtSchemeInfo)
            if (cpd != null && cpd?.isShowing == true) cpd?.dismiss()
            toast("Entry updated successfully")
        }
        setResult(RESULT_OK)
        Utils.hideSoftKeyboard(this)
        finish()
    }

    fun getTodayDate(): String {
        return SimpleDateFormat("dd-MM-yy", Locale.ENGLISH).format(Date())
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                Utils.hideSoftKeyboard(this)
                finish()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    companion object {}
}
