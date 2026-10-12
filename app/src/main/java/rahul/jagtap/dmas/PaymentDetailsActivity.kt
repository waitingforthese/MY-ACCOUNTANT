package rahul.jagtap.dmas

import android.Manifest
import android.annotation.SuppressLint
import android.app.ProgressDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.view.MenuItem
import android.view.WindowManager
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ValueEventListener
import id.zelory.compressor.Compressor
import kotlinx.coroutines.launch
import rahul.jagtap.dmas.extensions.longToast
import rahul.jagtap.dmas.extensions.toast
import pl.aprilapps.easyphotopicker.ChooserType
import pl.aprilapps.easyphotopicker.DefaultCallback
import pl.aprilapps.easyphotopicker.EasyImage
import pl.aprilapps.easyphotopicker.MediaFile
import pl.aprilapps.easyphotopicker.MediaSource
import rahul.jagtap.dmas.databinding.ActivityPaymentDetailsBinding
import rahul.jagtap.dmas.extensions.gone
import rahul.jagtap.dmas.extensions.visible
import rahul.jagtap.dmas.model.PaymentDetails
import rahul.jagtap.dmas.utils.Utils
import java.text.SimpleDateFormat
import java.util.*


class PaymentDetailsActivity : BaseActivity() {
    private var paymentPhotoFileName: String = ""
    private var paymentPhotoDownloadUrl: String = ""
    var imageUri: Uri? = null
    private lateinit var easyImage: EasyImage
    lateinit var binding: ActivityPaymentDetailsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPaymentDetailsBinding.inflate(layoutInflater)
        if (Utils.disableScreenshot) this.window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbarLayout.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowTitleEnabled(false)
        binding.toolbarLayout.toolbarTitle?.text = getString(R.string.txt_payment_details)
        easyImage = EasyImage.Builder(this)
            .setChooserType(ChooserType.CAMERA_AND_GALLERY)
            .allowMultiple(false) // Setting to true will cause taken pictures to show up in the device gallery, DEFAULT false
            .setCopyImagesToPublicGalleryFolder(false)
            .build()

        database.child(Utils.PAYMENT_DETAILS_TABLE).child("admin").addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onCancelled(p0: DatabaseError) {
            }

            override fun onDataChange(dataSnapshot: DataSnapshot) {
                val paymentDetails = dataSnapshot.getValue(PaymentDetails::class.java)
                if (paymentDetails != null) {
                    if (checkIfActivityDestroying()) return
                    if (!TextUtils.isEmpty(paymentDetails.paymentPhotoDownloadUrl)) mContext?.let { Glide.with(it).load(paymentDetails.paymentPhotoDownloadUrl).into(binding.imageView) }
                }
            }
        })
        if (app?.preferences?.loggedInUser?.isAdmin == "1") {
            binding.btnAddChangeImage?.visible()
        } else {
            binding.btnAddChangeImage?.gone()
        }
        binding.btnAddChangeImage?.setOnClickListener {
            choosePhotoWithPermissions()
        }
        binding.btnPayByUpiId?.setOnClickListener {
            startActivity(Intent(mContext, PayByUpiActivity::class.java))
        }
        binding.btnPay.setOnClickListener {
            payUsingUPI("")
        }
    }

    @SuppressLint("CheckResult")
    private fun choosePhotoWithPermissions() {
//        rxPermissions?.requestEachCombined(Manifest.permission.CAMERA)?.subscribe {
//            when {
//                it.granted -> { // get picture
                    easyImage.openChooser(this)
//                }
//                it.shouldShowRequestPermissionRationale -> { // At least one denied permission without ask never again
//                }
//                else -> { // At least one denied permission with ask never again
//                    // Need to go to the settings
//                }
//            }
//        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        easyImage.handleActivityResult(requestCode, resultCode, data, this, object : DefaultCallback() {
            override fun onImagePickerError(error: Throwable, source: MediaSource) {
                longToast(getString(R.string.txt_try_later))
            }

            override fun onCanceled(source: MediaSource) {}
            override fun onMediaFilesPicked(imageFiles: Array<MediaFile>, source: MediaSource) {
                //                val dir = getExternalFilesDir(Environment.DIRECTORY_PICTURES)?.absolutePath
                //                val fileName = AccountingServicesActivity.dateFormatForPhoto.format(System.currentTimeMillis()) + ".jpg"
                //                val image = File("$dir/$fileName")

                lifecycleScope.launch {
                    val compressedImageFile = mContext?.let { Compressor.compress(it, imageFiles[0].file) }
                    Log.e("TAG", "${compressedImageFile?.path}")
                    compressedImageFile?.let {
                        imageUri = Uri.fromFile(it)
                        Utils.showDialog(mContext, "Continue adding/changing payment details image?", true) { dialog, which ->
                            run {
                                dialog.dismiss()
                                uploadPaymentPhoto(imageUri!!)
                            }
                        }
                    }
                }
            }
        })
    }

    private fun uploadPaymentPhoto(fileUri: Uri) {
        val cpd: ProgressDialog? = ProgressDialog(mContext)
        cpd?.setCancelable(false)
        cpd?.show()
        paymentPhotoFileName = "payment_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.ENGLISH).format(Date())}.jpg"
        val filepath = storageRef.child(Utils.PAYMENT_DETAILS_TABLE).child(paymentPhotoFileName)
        filepath.putFile(fileUri).addOnSuccessListener {
            try {
                if (cpd != null && cpd.isShowing) cpd.dismiss()
                filepath.downloadUrl.addOnSuccessListener { uri: Uri ->
                    paymentPhotoDownloadUrl = uri.toString()
                    createDbRecord()
                }.addOnFailureListener {
                    it.printStackTrace()
                }
            } catch (e: java.lang.Exception) {
                e.printStackTrace()
            }
        }.addOnFailureListener {
            if (cpd?.isShowing == true) cpd.dismiss()
            it.message?.let { it1 -> toast(it1) }
        }.addOnProgressListener { //displaying the upload progress
            val progress: Double = 100.0 * it.bytesTransferred / it.totalByteCount
            cpd?.setMessage("Please wait.. ")
        }
    }

    private fun createDbRecord() {
        val createdDateTime = SimpleDateFormat("dd-MM-yyyy HH:mm:ss", Locale.ENGLISH).format(Date())
        val billMap = HashMap<String, Any?>()
        billMap["createdBy"] = "admin"
        billMap["createdDateTime"] = createdDateTime
        billMap["paymentPhotoFileName"] = paymentPhotoFileName
        billMap["paymentPhotoDownloadUrl"] = paymentPhotoDownloadUrl
        billMap["timeStamp"] = System.currentTimeMillis()

        //        val pushKey = database.child(Utils.PAYMENT_DETAILS_TABLE).push().key
        //        billMap["pushKey"] = pushKey

        val messageUserMap = HashMap<String, Any?>()
        messageUserMap["${Utils.PAYMENT_DETAILS_TABLE}/admin"] = billMap
        database.updateChildren(messageUserMap) { databaseError: DatabaseError?, _: DatabaseReference? ->
            if (databaseError != null) {
                Log.e("db error", databaseError.message)
            }
        }

        val backupMap = HashMap<String, Any?>()
        backupMap["${Utils.PAYMENT_DETAILS_TABLE}_backup/admin"] = billMap
        database.updateChildren(backupMap)
        toast("Updated payment details")
        finish()
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
