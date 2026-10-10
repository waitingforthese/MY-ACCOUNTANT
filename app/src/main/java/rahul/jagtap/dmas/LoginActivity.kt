package rahul.jagtap.dmas

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.text.SpannableString
import android.text.Spanned
import android.text.TextUtils
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.text.style.ForegroundColorSpan
import android.util.Log
import android.view.View
import android.widget.TextView
import androidx.core.net.toUri
import androidx.credentials.Credential
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.lifecycle.lifecycleScope
import com.afollestad.materialdialogs.DialogAction
import com.afollestad.materialdialogs.MaterialDialog
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.launch
import rahul.jagtap.dmas.databinding.ActivityLoginBinding
import rahul.jagtap.dmas.extensions.gone
import rahul.jagtap.dmas.extensions.longToast
import rahul.jagtap.dmas.extensions.toast
import rahul.jagtap.dmas.extensions.visible
import rahul.jagtap.dmas.model.User
import rahul.jagtap.dmas.utils.Utils


class LoginActivity : BaseActivity() {
    private val TAG = LoginActivity::class.java.simpleName
    lateinit var binding: ActivityLoginBinding
    private lateinit var credMgr: CredentialManager
    private lateinit var googleSignInClient: GoogleSignInClient
    private val RC_GOOGLE_SIGN_IN = 9001

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (app?.preferences?.isLoggedInUser == true) {
            startActivity(Intent(mContext, MainActivity::class.java).putExtra("fromLogin", true))
            finish()
            return
        }
        credMgr = CredentialManager.create(this)

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(BuildConfig.OAUTH_CLIENT_ID)
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, gso)

        binding.btnGoogle.setOnClickListener {
            signInWithGoogle()
        } //        binding.btnRegister?.setOnClickListener {
        //            startActivity(Intent(mContext, RegisterActivity::class.java))
        //        }
        ////        binding.btnLogin?.setOnClickListener {
        ////            login()
        ////        }
        //        binding.tvForgotPassword?.setOnClickListener {
        //            startActivity(Intent(mContext, ForgotPasswordActivity::class.java))
        //        }
        FirebaseMessaging.getInstance().token.addOnSuccessListener(this) { s ->
            if (!TextUtils.isEmpty(s)) app?.preferences?.token = s
            Log.e("Token", s)
        }
        val spanText = SpannableString("9552789899 / 02487 231111")

        spanText.setSpan(object : ClickableSpan() {
            override fun onClick(view: View) {
                callPhone("+91 9552789899")
            }
        }, 0, 10, 0)
        spanText.setSpan(ForegroundColorSpan(Color.BLUE), 0, 10, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        val clickable3 = object : ClickableSpan() {
            override fun onClick(view: View) {
                callPhone("02487 231111")
            }
        }
        spanText.setSpan(clickable3, 13, 25, 0)
        spanText.setSpan(ForegroundColorSpan(Color.BLUE), 13, 25, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        binding.tvPhoneNumbers?.setText(spanText, TextView.BufferType.SPANNABLE)
        binding.tvPhoneNumbers?.movementMethod = LinkMovementMethod.getInstance()
        binding.tvVersion.text = "Version: ${BuildConfig.VERSION_NAME}"
        binding.tvWebsite.setOnClickListener {
            val intent = Intent(Intent.ACTION_VIEW, "https://www.mahaesuvidha.com".toUri())
            startActivity(intent)
        }
        binding.tvEmail.setOnClickListener {
            val emailIntent = Intent(Intent.ACTION_SENDTO).apply {
                data = "mailto:app.mahaesuvidha@gmail.com".toUri()
            }
            startActivity(emailIntent)
        }
    }

    // --- Google sign-in via Credential Manager, with Google Play Services fallback ---
    private fun signInWithGoogle() {
        val googleIdOpt = GetGoogleIdOption.Builder()
            .setServerClientId(BuildConfig.OAUTH_CLIENT_ID)
            .setFilterByAuthorizedAccounts(false)
            .build()

        val req = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOpt)
            .build()

        lifecycleScope.launch {
            try {
                val res = credMgr.getCredential(this@LoginActivity, req)
                handleCredential(res.credential)
            } catch (e: Exception) {
                Log.e(TAG, "Credential Manager sign-in failed; starting Play Services fallback", e)
                startLegacyGoogleSignIn()
            }
        }
    }

    private fun startLegacyGoogleSignIn() {
        googleSignInClient.signOut().addOnCompleteListener {
            try {
                startActivityForResult(googleSignInClient.signInIntent, RC_GOOGLE_SIGN_IN)
            } catch (e: Exception) {
                Log.e(TAG, "Google Play Services sign-in could not start", e)
                longToast("Google Sign-In Error: ${e.javaClass.simpleName}: ${e.message}")
            }
        }
    }

    @Deprecated("Deprecated in Android framework, retained for this existing Activity flow")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode != RC_GOOGLE_SIGN_IN) return

        try {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            val account = task.getResult(ApiException::class.java)
            val idToken = account.idToken

            if (idToken.isNullOrEmpty()) {
                longToast("Google Sign-In Error: ID token not received")
                return
            }

            firebaseAuthWithGoogle(idToken)
        } catch (e: ApiException) {
            Log.e(TAG, "Google Play Services sign-in failed: code=${e.statusCode}", e)
            longToast("Google Sign-In Error: ${e.statusCode}: ${e.message}")
        } catch (e: Exception) {
            Log.e(TAG, "Google Play Services sign-in failed", e)
            longToast("Google Sign-In Error: ${e.javaClass.simpleName}: ${e.message}")
        }
    }

    private fun handleCredential(cred: Credential) {
        if (cred is CustomCredential &&
            cred.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
        ) {
            val idToken = GoogleIdTokenCredential.createFrom(cred.data).idToken
            firebaseAuthWithGoogle(idToken)
        } else {
            longToast("Invalid credential")
        }
    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        val firebaseCred = GoogleAuthProvider.getCredential(idToken, null)

        binding.progressBar?.visible()
        auth.signInWithCredential(firebaseCred).addOnCompleteListener { task ->
            binding.progressBar?.gone()
            if (!task.isSuccessful) {
                Log.e(TAG, "signInWithGoogle:failure", task.exception)
                longToast(
                    "Firebase Error: ${task.exception?.javaClass?.simpleName}: ${task.exception?.message}"
                )
                return@addOnCompleteListener
            }

            val user = auth.currentUser
            val email = user?.email.orEmpty()
            val name = user?.displayName.orEmpty()
            val uid = user?.uid.orEmpty()
            Log.e(TAG, "auth uid success: ${auth.uid}")

            val isGmail = email.endsWith("@gmail.com", true) ||
                email.endsWith("@googlemail.com", true)
            if (!isGmail) {
                toast("Please choose a Gmail account.")
                return@addOnCompleteListener
            }

            findUserAndRedirect(uid, email, name)
        }
    }

    //    private fun signOutWithMessage(msg: String) {
    //        try { credMgr.clearCredentialState(ClearCredentialStateRequest()) } catch (_: Exception) {}
    //        FirebaseAuth.getInstance().signOut()
    //        longToast(msg)
    //    }

    private fun goToRegisterPrefilled(name: String, email: String, uid: String) {
        val i = Intent(this, RegisterActivity::class.java)
        i.putExtra("prefill_name", name)
        i.putExtra("prefill_email", email)
        i.putExtra("uid", uid)
        startActivity(i)
        finish()
    }

    //    private fun login() {
    //        val email = binding.etEmail.text.toString().trim()
    //        val password = binding.etPassword.text.toString().trim()
    //        if (TextUtils.isEmpty(email)) {
    //            binding.etEmail.error = "रजिस्टर केलेला ई-मेल आयडी"
    //            binding.etEmail.requestFocus()
    //            return
    //        }
    //        if (!Utils.isValidEmail(email)) {
    //            binding.etEmail.error = "रजिस्टर केलेला वैध ई-मेल आयडी"
    //            binding.etEmail.requestFocus()
    //            return
    //        }
    //        if (TextUtils.isEmpty(password)) {
    //            binding.etPassword.error = "रजिस्टर केलेला पासवर्ड"
    //            binding.etPassword.requestFocus()
    //            return
    //        }
    //        binding.progressBar?.visible()
    //        auth.signInWithEmailAndPassword(email, password).addOnCompleteListener(this) { task ->
    //            val any = if (task.isSuccessful) { // Sign in success, update UI with the signed-in user's information
    //                Log.e(TAG, "signInWithEmail:success")
    //                val uid = task.result?.user!!.uid
    //                val email = task.result?.user!!.email
    //                findUserAndRedirect(uid, email)
    //            } else { // If sign in fails, display a message to the user.
    //                Log.e(TAG, "signInWithEmail:failure", task.exception)
    //                longToast("You have entered wrong Email ID or password")
    //            }
    //            binding.progressBar?.gone()
    //        }
    //    }

    // --- Gmail normalizer (handles dots and +tags) ---
    private fun normalizeGmail(raw: String): String {
        val email = raw.trim().lowercase()
        val parts = email.split("@")
        if (parts.size != 2) return email
        val (local, domain) = parts
        if (domain != "gmail.com" && domain != "googlemail.com") return email
        val plusCut = local.substringBefore("+")
        val noDots = plusCut.replace(".", "")
        return "$noDots@gmail.com"
    }

    private fun findUserAndRedirect(uid: String, email: String?, name: String) {
        database.child(Utils.USERS_TABLE).child(uid).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onCancelled(databaseError: DatabaseError) {
                Log.e("TAG", "getUser:onCancelled", databaseError.toException())
            }

            override fun onDataChange(dataSnapshot: DataSnapshot) { // Get user value
                val userInfo = dataSnapshot.getValue(User::class.java)
                if (userInfo != null) {
                    if (userInfo.isBlocked == "1") {
                        val ok = mContext?.let { MaterialDialog.Builder(it).content("Your account is inactive. Please, contact with admin").positiveText("Contact Admin").negativeText("No").show() }
                        ok?.getActionButton(DialogAction.POSITIVE)?.setOnClickListener {
                            ok.dismiss()
                            startActivity(Intent(mContext, ContactActivity::class.java))
                        }
                        ok?.getActionButton(DialogAction.NEGATIVE)?.setOnClickListener { ok.dismiss() }
                    } else {
                        if (userInfo.isAdmin == "1") {
                            userInfo.fcmToken = app?.preferences?.token
                            database.child(Utils.USERS_TABLE).child(uid).setValue(userInfo).addOnSuccessListener {
                                database.child(Utils.FCM_TOKEN_TABLE).child("admin").setValue(app?.preferences?.token
                                    ?: "") // Write was successful!
                                app?.preferences?.loggedInUserId = uid
                                app?.preferences?.loggedInUserEmail = email
                                longToast("Login Successfully")
                                startActivity(Intent(mContext, MainActivity::class.java))
                                finish()
                            }.addOnFailureListener { // Write failed
                                app?.preferences?.loggedInUserId = uid
                                app?.preferences?.loggedInUserEmail = email
                                longToast("Login Successfully")
                                startActivity(Intent(mContext, MainActivity::class.java))
                                finish()
                            }
                        } else {
                            app?.preferences?.loggedInUserId = uid
                            app?.preferences?.loggedInUserEmail = email
                            longToast("Login Successfully")
                            startActivity(Intent(mContext, MainActivity::class.java).putExtra("fromLogin", true))
                            finish()
                        }
                    }
                } else {
                    goToRegisterPrefilled(name, email.toString(), uid)
                }
            }
        })
    }
}
