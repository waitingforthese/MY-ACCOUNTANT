package rahul.jagtap.dmas

import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.view.MenuItem
import android.view.WindowManager
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import com.bumptech.glide.Glide
import com.google.android.gms.auth.api.identity.GetPhoneNumberHintIntentRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import rahul.jagtap.dmas.databinding.ActivityRegisterBinding
import rahul.jagtap.dmas.extensions.gone
import rahul.jagtap.dmas.extensions.longToast
import rahul.jagtap.dmas.extensions.visible
import rahul.jagtap.dmas.model.ImageDetails
import rahul.jagtap.dmas.model.NotificationItem
import rahul.jagtap.dmas.model.User
import rahul.jagtap.dmas.utils.Utils
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


class RegisterActivity : BaseActivity() {
    private val TAG = RegisterActivity::class.java.simpleName
    lateinit var binding: ActivityRegisterBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        if (Utils.disableScreenshot) this.window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbarLayout.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(false)
        supportActionBar?.setDisplayShowTitleEnabled(false)
        binding.toolbarLayout.toolbarTitle?.text = getString(R.string.txt_registration)
        binding.btnSubmit?.setOnClickListener {
            createAccount()
        }
//        binding.etEmail.setOnClickListener { showBestEmailPicker() }
        val prefill_name = intent.getStringExtra("prefill_name")
        val prefill_email = intent.getStringExtra("prefill_email")
        binding.etName.setText(prefill_name.toString())
        binding.etEmail.setText(prefill_email.toString())
        pickPhoneFromSim()
        binding.etContactNo.setOnClickListener { pickPhoneFromSim() }
        setRegisterScreenImage()
    }

    private fun createAccount() {
        val strName = binding.etName.text.toString().trim()
        val strShopName = binding.etShopName.text.toString().trim()
        val strEmail = binding.etEmail.text.toString().trim()
        val strContactNo = binding.etContactNo.text.toString().trim()
//        val strPassword = binding.etPassword.text.toString().trim()
//        val strConfirmPassword = binding.etConfirmPassword.text.toString().trim()
        val strAddress = binding.etAddress.text.toString().trim()
        val strReferrer = binding.etReferrer.text.toString().trim()
//        val strAadharNo = etAadharNo.text.toString().trim()
        if (TextUtils.isEmpty(strName)) {
            binding.etName.error = binding.etName?.hint.toString()//"Enter Name"
            binding.etName.requestFocus()
            return
        }
        if (TextUtils.isEmpty(strShopName)) {
            binding.etShopName.error = binding.etShopName.hint.toString() //"Enter Shop Name"
            binding.etShopName.requestFocus()
            return
        }
//        if (TextUtils.isEmpty(strEmail)) {
//            binding.etEmail.error = binding.etEmail.hint.toString() //"Enter Email ID"
//            binding.etEmail.requestFocus()
//            return
//        }
//        if (!Utils.isValidEmail(strEmail)) {
//            binding.etEmail.error = "वैध ईमेल आयडी टाका"
//            binding.etEmail.requestFocus()
//            return
//        }
        if (TextUtils.isEmpty(strContactNo)) {
            binding.etContactNo.error = binding.etContactNo?.hint.toString() //"Enter Contact Number"
            binding.etContactNo.requestFocus()
            return
        }
//        if (TextUtils.isEmpty(strPassword)) {
//            binding.etPassword.error = binding.etPassword.hint.toString() //"Enter Password"
//            binding.etPassword.requestFocus()
//            return
//        }
//        if (strPassword.length < 6) {
//            binding.etPassword.error = "पासवर्डची लांबी किमान ६ अक्षरे असणे आवश्यक आहे"
//            binding.etPassword.requestFocus()
//            return
//        }
//        if (TextUtils.isEmpty(strConfirmPassword)) {
//            binding.etConfirmPassword.error = "पुन्हा पासवर्ड टाका"
//            binding.etConfirmPassword.requestFocus()
//            return
//        }
//        if (strPassword != strConfirmPassword) {
//            binding.etConfirmPassword.error = "पासवर्ड कन्फर्म पासवर्डशी जुळत नाही"
//            binding.etConfirmPassword.requestFocus()
//            return
//        }
        if (TextUtils.isEmpty(strAddress)) {
            binding.etAddress.error = binding.etAddress.hint.toString()
            binding.etAddress.requestFocus()
            return
        }
        if (TextUtils.isEmpty(strReferrer)) {
            binding.etReferrer.error = binding.etReferrer.hint.toString()
            binding.etReferrer.requestFocus()
            return
        }
//        if (TextUtils.isEmpty(strAadharNo)) {
//            etAadharNo.error = "Enter Aadhar Number"
//            etAadharNo.requestFocus()
//            return
//        }

        binding.progressBar?.visible()
        onAuthSuccess(strEmail, intent.getStringExtra("uid").toString(), strName, strShopName, strContactNo, strAadharNo = "", strAddress, strReferrer)
//        // [START create_user_with_email]
//        auth.createUserWithEmailAndPassword(strEmail, strPassword)
//            .addOnCompleteListener(this) { task ->
//                if (task.isSuccessful) {
//                    // Sign in success, update UI with the signed-in user's information
//                    Log.d(TAG, "createUserWithEmail:success")
//                    val user = auth.currentUser
//                    user?.let { onAuthSuccess(it, strName, strShopName, strContactNo, "", strAddress, strReferrer) }
//                } else {
//                    // If sign in fails, display a message to the user.
//                    Log.w(TAG, "createUserWithEmail:failure", task.exception)
//                    longToast("Email already exists.")
//                }
//
//                binding.progressBar?.gone()
//            }
    }

    private fun onAuthSuccess(strEmail: String, uid: String, strName: String, strShopName: String, strContactNo: String, strAadharNo: String, strAddress: String, strReferrer: String) {
        val username = Utils.usernameFromEmail(strEmail)
        val createdDateTime = SimpleDateFormat("dd-MM-yyyy HH:mm:ss", Locale.ENGLISH).format(Date())

        // Write new user
        val localUser =
            User(uid = uid, name = strName, username = username, shopName = strShopName, isAdmin = "", email = strEmail,
                contactNo = strContactNo, address = strAddress, referrer = strReferrer, createdAt = createdDateTime, aadharNo = strAadharNo,
                userType = "0")
        database.child(Utils.USERS_TABLE).child(uid).setValue(localUser)
        val pushKey = database.child(Utils.NOTIFICATIONS_TABLE).push().key
        if (pushKey != null) {
//            val notificationMap = HashMap<String, Any?>()
//            notificationMap["email"] = user.email
//            notificationMap["message"] =
//            notificationMap["uid"] = uid
//            notificationMap["notificationType"] = "add_user"
//            notificationMap["createdAt"] = createdDateTime
//            val notificationUserMap = HashMap<String, Any?>()
//            notificationUserMap["Notifications/$pushKey"] = notificationMap
//            database.updateChildren(notificationUserMap)
            val notificationItem = NotificationItem(
                message = "New user(${strName}) registered", createdAt = createdDateTime,
                email = strEmail, uid = uid, notificationType = "add_user"
            )
            database.child(Utils.NOTIFICATIONS_TABLE).child(pushKey).setValue(notificationItem)
            sendNotification("New user(${strName}) registered")
        }
        longToast("Account created successfully")
        app?.preferences?.loggedInUserId = uid
        app?.preferences?.loggedInUserEmail = strEmail
        startActivity(Intent(mContext, MainActivity::class.java))
        finish()
    }

    private fun setRegisterScreenImage() {
        database.child(Utils.REGISTER_SCREEN_IMAGE_TABLE).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onCancelled(p0: DatabaseError) {
            }

            override fun onDataChange(dataSnapshot: DataSnapshot) {
                val imageDetails = dataSnapshot.getValue(ImageDetails::class.java)
                if (imageDetails != null) {
                    if (!TextUtils.isEmpty(imageDetails.imageDownloadUrl)) {
                        mContext?.let { Glide.with(it).load(imageDetails.imageDownloadUrl).into(binding.imageView1) }
                        binding.imageView1.visible()
                    }
                }
            }
        })
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

    public override fun onStop() {
        super.onStop()
        binding.progressBar?.gone()
    }

    // ======== BEGIN: Easy pickers (Phone Number Hint + Account chooser) ========

    // Phone Number Hint launcher (no permissions needed)
    private val phoneHintLauncher =
        registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { res ->
            if (res.resultCode == RESULT_OK) {
                val picked = Identity.getSignInClient(this).getPhoneNumberFromIntent(res.data)
                if (!picked.isNullOrBlank()) {
                    // Remove everything except digits
                    val digits = picked.replace(Regex("\\D"), "")
                    // Get last 10 digits (if present)
                    val mobile = if (digits.length >= 10) digits.takeLast(10) else ""
                    // Validate: Indian numbers should be 10 digits and start with 6-9
                    if (mobile.length == 10 && mobile[0] in '6'..'9') {
                        binding.etContactNo.setText("+91$mobile")
                    } else {
                        // If not valid, show error
                        binding.etContactNo.setText("")
                        binding.etContactNo.error = "Invalid number. Please select a valid mobile number from SIM."
                    }
                } else {
                    binding.etContactNo.setText("")
                }
//                var phone = Identity.getSignInClient(this).getPhoneNumberFromIntent(res.data)
//                if (!phone.isNullOrBlank()) {
//                    // Always force +91 if needed
//                    if (!phone.startsWith("+91")) {
//                        phone = phone.replace(Regex("^\\+\\d+"), "+91")
//                    }
//                    binding.etContactNo.setText(phone)
//                }
            }
        }

    private fun pickPhoneFromSim() {
        val request = GetPhoneNumberHintIntentRequest.builder().build()
        Identity.getSignInClient(this)
            .getPhoneNumberHintIntent(request)
            .addOnSuccessListener { pendingIntent ->
                phoneHintLauncher.launch(IntentSenderRequest.Builder(pendingIntent).build())
            }
            .addOnFailureListener {
                // Optional: silently ignore or show a toast; can fallback to contacts picker if needed
//                binding.etContactNo.error = "Unable to get number from SIM. Please check your SIM."
//                binding.etContactNo.isLongClickable = true
//                binding.etContactNo.isFocusable = true
//                binding.etContactNo.isEnabled = true
//                binding.etContactNo.error = "Unable to get number from SIM. Please enter your number."
            }
    }

    // Call this to show only Google emails
//    private fun showGoogleEmailsDialog() {
//        val perm = Manifest.permission.GET_ACCOUNTS
//        val granted = ContextCompat.checkSelfPermission(this, perm) == PackageManager.PERMISSION_GRANTED
//        if (!granted) {
//            requestPermissions(arrayOf(perm), 1234)
//            return
//        }
//        val accountManager = AccountManager.get(this)
//        val googleAccounts = accountManager.getAccountsByType("com.google")
//        val emails = googleAccounts.map { it.name }.distinct()
//        if (emails.isEmpty()) {
//            // Optional: fallback to manual entry
//            AlertDialog.Builder(this)
//                .setTitle("No Google emails found")
//                .setMessage("No Google account emails found on this device.")
//                .setPositiveButton("OK", null)
//                .show()
//            return
//        }
//        val items = emails.toTypedArray()
//        AlertDialog.Builder(this)
//            .setTitle("Choose email")
//            .setItems(items) { _, which ->
//                binding.etEmail.setText(items[which])
//            }
//            .setNegativeButton("Cancel", null)
//            .show()
//    }

//    private fun showBestEmailPicker() {
//        // Try AccountManager first (Google-only)
//        val permission = Manifest.permission.GET_ACCOUNTS
//        val granted = ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
//        if (!granted) {
//            // Permission not granted; request it at runtime
//            requestPermissions(arrayOf(permission), 5555)
//            return
//        }
//        val am = AccountManager.get(this)
//        val googleAccounts = am.getAccountsByType("com.google")
//        val emails = googleAccounts.map { it.name }
//            .filter { Patterns.EMAIL_ADDRESS.matcher(it).matches() }
//            .distinct()
//
//        if (emails.isNotEmpty()) {
//            // If there are Google accounts, show them in a picker
//            showEmailPickerDialog(emails)
//            return
//        }
//
//        // Fallback: Try the system account picker intent (rarely available)
//        try {
//            val intent = Intent("android.accounts.action.CHOOSE_ACCOUNT").apply {
//                putExtra("allowableAccountTypes", arrayOf("com.google"))
//            }
//            startActivityForResult(intent, 6666)
//        } catch (e: Exception) {
//            // Fallback: manual entry
////            showManualEmailEntry()
//            showManualEntry()
//        }
//    }

    // Dialog to pick from list
//    private fun showEmailPickerDialog(emails: List<String>) {
//        val items = emails.toTypedArray()
//        AlertDialog.Builder(this)
//            .setTitle("Choose your email")
//            .setItems(items) { _, which ->
//                binding.etEmail.setText(items[which])
//            }
//            .setNegativeButton("Cancel", null)
//            .show()
//    }

    // In your onRequestPermissionsResult:
//    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
//        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
//        if (requestCode == 5555 && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
//            showBestEmailPicker()
//        } else if (requestCode == 5555) {
////            showManualEmailEntry()
//            showManualEntry()
//        }
//    }
//
//    // Optional: handle system account picker fallback result
//    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
//        super.onActivityResult(requestCode, resultCode, data)
//        if (requestCode == 6666 && resultCode == Activity.RESULT_OK) {
//            val email = data?.getStringExtra("authAccount")
//            if (!email.isNullOrBlank()) {
//                binding.etEmail.setText(email)
//                return
//            }
//        }
//        // If fallback picker failed, ask user to type
//        if (requestCode == 6666) showManualEntry()
//    }

//    companion object {
//        private const val ACTION_CHOOSE_ACCOUNT = "android.accounts.action.CHOOSE_ACCOUNT"
//        private const val EXTRA_ALLOWABLE_ACCOUNT_TYPES_STRING_ARRAY = "allowableAccountTypes"
//    }
}

//private fun RegisterActivity.showManualEntry() {
//    AlertDialog.Builder(this).setTitle("Manual entry").setMessage("No Google account emails found on this device.").setPositiveButton("OK", null).show()
//}
