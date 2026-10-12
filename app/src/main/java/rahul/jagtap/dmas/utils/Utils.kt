package rahul.jagtap.dmas.utils

import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Environment
import android.text.TextUtils
import android.util.Log
import android.view.PixelCopy
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.inputmethod.InputMethodManager
import androidx.annotation.RequiresApi
import com.afollestad.materialdialogs.DialogAction
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.MaterialDialog.SingleButtonCallback
import org.joda.time.DateTime
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.URLConnection
import java.text.DecimalFormat
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*
import java.util.regex.Pattern

class Utils {
    companion object {
        var EMAIL_PATTERN = "^[_A-Za-z0-9-\\+]+(\\.[_A-Za-z0-9-]+)*@[A-Za-z0-9-]+(\\.[A-Za-z0-9]+)*(\\.[A-Za-z]{2,})$"
        var FCM_TOKEN_TABLE = "fcm_tokens"
        var USERS_TABLE = "users"
        var BILLS_TABLE = "bills"
        var ESUVIDHA_TABLE = "esuvidha"
        var DAILY_ENTRY_TABLE = "daily_entries"
        var REPORTS_TABLE = "reports"
        var NOTIFICATIONS_TABLE = "notifications"
        var PAYMENT_DETAILS_TABLE = "payment_details"
        var REFERRAL_PROGRAM_TABLE = "referral_program"
        var BANNER_IMAGE_TABLE = "banner_image"
        var TALUKA_SUVIDHA_LIST_IMAGE_TABLE = "taluka_suvidha_list_image"
        var ABOUT_TEAM_IMAGE_TABLE = "about_team_image"
        var ACCOUNTING_MENU_IMAGE_TABLE = "accounting_menu_image"
        var REGISTER_SCREEN_IMAGE_TABLE = "register_screen_image"
        var DAY_BOOK_TABLE = "day_books"
        var TEXT_MSG_TABLE = "text_messages"
        var GOVT_SCHEMES_TABLE = "govt_schemes"
        var SUCHNA_TABLE = "esuvidha_suchna"
        var DYNAMIC_TYPES_TABLE = "dynamic_types"
        var ESUVIDHA_DYNAMIC_TYPES_TABLE = "esuvidha_dynamic_types"
        var CONTACT_US_TABLE = "contact_us"

        // Admin-managed training videos screen (screen title + list of YouTube videos). See TrainingVideoActivity.
        var TRAINING_VIDEOS_TABLE = "training_videos"

        /**
         * Extract the 11-char YouTube video id from any URL form the admin might paste:
         * watch?v=ID, youtu.be/ID, shorts/ID, embed/ID, live/ID, or a bare id.
         * Returns null if nothing usable is found.
         */
        fun extractYoutubeId(url: String?): String? {
            val u = url?.trim().orEmpty()
            if (u.isEmpty()) return null
            // Already a bare id
            if (Regex("^[A-Za-z0-9_-]{11}$").matches(u)) return u
            val patterns = listOf(
                "v=([A-Za-z0-9_-]{11})",
                "youtu\\.be/([A-Za-z0-9_-]{11})",
                "shorts/([A-Za-z0-9_-]{11})",
                "embed/([A-Za-z0-9_-]{11})",
                "live/([A-Za-z0-9_-]{11})",
                "/([A-Za-z0-9_-]{11})(?:[?&/]|$)"
            )
            for (p in patterns) {
                Regex(p).find(u)?.groupValues?.getOrNull(1)?.let { return it }
            }
            return null
        }

        /** hqdefault.jpg always exists for a valid public video (unlike maxresdefault), so no gray boxes. */
        fun youtubeThumbUrl(videoId: String?): String? =
            videoId?.takeIf { it.isNotBlank() }?.let { "https://img.youtube.com/vi/$it/hqdefault.jpg" }

        // Admin-managed e-suvidha services grid (flattened services + subtypes). See EsuvidhaServiceRegistry.
        var ESUVIDHA_GRID_TABLE = "esuvidha_grid"
        var ESUVIDHA_GRID_BACKUP_TABLE = "esuvidha_grid_backup"

        var PAN_CARDS_SUCHNA = "pan_cards_suchna"
        var SHOP_ACTS_SUCHNA = "shop_acts_suchna"
        var UDYAM_AADHAR_SUCHNA = "udyam_aadhar_suchna"
        var FOOD_LICENSE_SUCHNA = "food_license_suchna"
        var PROVIDENT_FUND_SUCHNA = "provident_fund_suchna"
        var NEPAL_MONEY_TRANSFER_SUCHNA = "nepal_money_transfer_suchna"
        var RAILWAY_TICKET_BOOKING_SUCHNA = "railway_ticket_booking_suchna"
        var BUSINESS_PAN_CARDS_SUCHNA = "business_pan_cards_suchna"
        var ELECTION_CARDS_SUCHNA = "election_cards_suchna"
        var PASSPORTS_SUCHNA = "passports_suchna"
        var DRIVING_LEARNING_LICENSES_SUCHNA = "driving_learning_licenses_suchna"
        var AADHAR_CARD_UPDATE_SUCHNA = "aadhar_card_update_suchna"
        var EDIT_PAN_AADHAR_CARDS_SUCHNA = "edit_pan_aadhar_cards_suchna"
        var ACHUK_JANMA_KUNDLI_SUCHNA = "achuk_janma_kundli_suchna"
        var AADHAR_PAN_LINK_SUCHNA = "aadhar_pan_link_suchna"
        var POLICE_VERIFICATIONS_SUCHNA = "police_verifications_suchna"
        var INCOME_CERTIFICATES_SUCHNA = "income_certificates_suchna"
        var AGE_CERTIFICATES_SUCHNA = "age_certificates_suchna"
        var GAZZETS_SUCHNA = "gazzets_suchna"
        var JYOTISH_SHASTRA_SUCHNA = "jyotish_shastra_suchna"
        var CIBIL_REPORTS_SUCHNA = "cibil_reports_suchna"
        var FREE_CREDIT_CARDS_SUCHNA = "free_credit_cards_suchna"
        var GST_REGS_SUCHNA = "gst_regs_suchna"
        var ALL_GOVT_CARDS_SUCHNA = "all_govt_cards_suchna"
        var FARMER_POLICIES_SUCHNA = "farmer_policies_suchna"
        var ALL_OTHER_DOCS_SUCHNA = "all_other_docs_suchna"
        var DEMAT_ACCOUNTS_SUCHNA = "demat_accounts_suchna"
        var GOVT_SCHEMES_SUCHNA = "govt_schemes_suchna"
        var VERIFICATION_SUCHNA = "verification_suchna"
        var disableScreenshot = true
        var closingUpdateList = listOf("Completed", "Completed fee Brought Down", "Cancel Fee Carried Forword", "Cancel Wrong Datails Upload",
            "Full Refund Server Problem", "Full Refund OTP not Received", "Govt Rejected Govt Fee Refund", "Govt Rejected No Refund",
            "Full Refund Customer No Reply", "Customer Decline Full Refund", "Customer Decline Govt Fee Refund", "Refund Arrogant Customer & Client",
            "Cancel Fraud Customer", "Incomplete Documents Full Refund", "Cancel Fee Not Received", "Testing Admin", "Admin Rejected Full Refund")

        // Function to get a key from the value
        fun getKeyFromValue(map: HashMap<String, String>, value: String): String? {
            return map.entries.firstOrNull { it.value == value }?.key
        }

        fun getKeyFromNestedHashMap(map: HashMap<String, HashMap<String, String>>, value: String): String? {
            return map.entries.firstOrNull { it.value["type_title"] == value }?.key
        }

        @Throws(Exception::class)
        fun downloadScreenshot(view: View, window: Window, isCreateFolderClicked: Boolean, createFolderPath: String): Boolean {
            val name = "screenshot_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.ENGLISH).format(Date())}.jpg"
            try {
                val file = File(Environment.getExternalStorageDirectory(), if (isCreateFolderClicked) createFolderPath else "DCIM/Camera")
                if (!file.exists()) {
                    file.mkdirs()
                }
                val resultFile = File(file.absolutePath + File.separator.toString() + name)
                Utils.captureScreenshot(view, window) { bitmap ->
                    if (bitmap != null) {
                        // Use the bitmap (e.g., save it to storage, share it, etc.)
                        var fileOutputStream: FileOutputStream? = null
                        try {
                            fileOutputStream = FileOutputStream(resultFile)
                            fileOutputStream.use { out ->
                                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                            } ?: false
                            Log.e("TAG", "Writing file $resultFile")
                            //                        isSuccess = true
                        } catch (e: IOException) {
                            Log.e("TAG", "Error writing Exception: ", e)
                            //                        isSuccess = false
                        } catch (e: Exception) {
                            Log.e("TAG", "Failed to save file due to Exception: ", e)
                            //                        isSuccess = false
                        } finally {
                            try {
                                fileOutputStream?.close()
                            } catch (ex: Exception) {
                                ex.printStackTrace()
                            }
                        }
                    } else {
                        // Handle the error case
                    }
                }
            } catch (e: Exception) {
                Log.e(">>>>>", e.toString()) //mToast(this, e.toString());
                return false
            }
            return true
        }

        fun captureScreenshot(view: View, window: Window, callback: (Bitmap?) -> Unit) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                // For API level 26 and above
                captureScreenshotAboveApi26(view, window, callback)
            } else {
                // For API level below 26
                captureScreenshotBelowApi26(view, callback)
            }
        }

        @RequiresApi(Build.VERSION_CODES.O)
        private fun captureScreenshotAboveApi26(view: View, window: Window, callback: (Bitmap?) -> Unit) {
            // Create a bitmap with the same dimensions as the view
            val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
            val location = IntArray(2)
            view.getLocationInWindow(location)

            try {
                // Use PixelCopy to capture the view's content
                PixelCopy.request(window, android.graphics.Rect(
                    location[0], location[1],
                    location[0] + view.width, location[1] + view.height), bitmap,
                    { copyResult ->
                        if (copyResult == PixelCopy.SUCCESS) {
                            callback(bitmap)
                        } else {
                            callback(null)
                        }
                    },
                    view.handler
                )
            } catch (e: Exception) {
                e.printStackTrace()
                callback(null)
            }
        }

        private fun captureScreenshotBelowApi26(view: View, callback: (Bitmap?) -> Unit) {
            try {
                // Create a bitmap with the same dimensions as the view
                val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
                // Draw the view onto the canvas
                val canvas = Canvas(bitmap)
                view.draw(canvas)
                callback(bitmap)
            } catch (e: Exception) {
                e.printStackTrace()
                callback(null)
            }
        }

        fun formatAmount(x: Double): String {
            val format = DecimalFormat("#.##")
            try {
                return format.format(x)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            return x.toString()
        }

        fun checkBetween(dateToCheck: String?, startDate: String?, endDate: String?): Boolean {
            var res = false
            val fmt1 = SimpleDateFormat("dd/MM/yyyy") //2013-05-20
            try {
                val fromDate = fmt1.parse(startDate)
                val requestDate = fmt1.parse(dateToCheck)
                val toDate = fmt1.parse(endDate)
                res = requestDate.compareTo(fromDate) >= 0 && requestDate.compareTo(toDate) <= 0
            } catch (pex: ParseException) {
                pex.printStackTrace()
            }
            return res
        }

        fun isDateBetweenTwoDates(date1: String, date2: String, dateToCheck: String): Boolean {
            try { // Create a SimpleDateFormat reference with different formats
                val sdf = SimpleDateFormat("dd-MM-yy")

                //Create the Lower and Upper Bound Date object
                val startDate: Date? = sdf.parse(date1)
                val endDate: Date? = sdf.parse(date2)

                //Create the date object to check
                val dateToValidate: Date? = sdf.parse(dateToCheck)

                // Create Joda Datetime instance using Date objects
                val dateTime1 = DateTime(startDate)
                val dateTime2 = DateTime(endDate)
                val dateTime3 = DateTime(dateToValidate)

                // compare datetime3 with datetime1 and datetime2 using the methods
                if ((dateTime3.isAfter(dateTime1) && dateTime3.isBefore(dateTime2)) || dateTime3.isEqual(dateTime1)|| dateTime3.isEqual(dateTime2)) {
                    System.out.println("""${"The date " + sdf.format(dateToValidate)} lies between the two dates $startDate & $endDate""")
                    return true
                } else {
                    println(sdf.format(dateToValidate) + " does not lie between the two dates $startDate & $endDate\n")
                    return false
                }
            } catch (e: Exception) {
                e.printStackTrace()
                return false
            }
        }

        fun showCustomAlertDialog(mContext: Context?, layoutResID: Int): Dialog? {
            if (mContext == null) return null
            val mDialog = Dialog(mContext)
            mDialog.setContentView(layoutResID)
            if (mDialog.window != null) {
                mDialog.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
                mDialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            }
            mDialog.setCancelable(true)
            mDialog.setCanceledOnTouchOutside(true)
            mDialog.show()
            return mDialog
        }

        fun hideSoftKeyboard(activity: Activity) {
            if (activity.currentFocus != null) {
                val inputMethodManager = activity
                    .getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager?
                inputMethodManager?.hideSoftInputFromWindow(activity.currentFocus!!.windowToken, 0)
            }
        }

        // validating email id
        fun isValidEmail(email: String?): Boolean {
            val pattern = Pattern.compile(EMAIL_PATTERN)
            val matcher = pattern.matcher(email)
            return matcher.matches()
        }

        fun usernameFromEmail(email: String): String {
            return if (email.contains("@")) {
                email.split("@".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()[0]
            } else {
                email
            }
        }

        fun dmyHmsTodmy(str_date: String?): String? {
            if (!TextUtils.isEmpty(str_date)) {
                val originalFormat = SimpleDateFormat("dd-MM-yyyy HH:mm:ss", Locale.ENGLISH)
                val targetFormat = SimpleDateFormat("dd-MM-yy")
                var date: Date? = null
                try {
                    date = originalFormat.parse(str_date) //2017-07-27
                } catch (e: ParseException) {
                    e.printStackTrace()
                }
                return targetFormat.format(date)
            }
            return ""
        }

        fun dmyHmsTodmyWoEng(str_date: String?): String? {
            if (!TextUtils.isEmpty(str_date)) {
                val originalFormat = SimpleDateFormat("dd-MM-yyyy HH:mm:ss")
                val targetFormat = SimpleDateFormat("dd-MM-yy")
                var date: Date? = null
                try {
                    date = originalFormat.parse(str_date) //2017-07-27
                } catch (e: ParseException) {
                    e.printStackTrace()
                }
                return targetFormat.format(date)
            }
            return ""
        }

        fun ymdTodmy(str_date: String?): String? {
            if (!TextUtils.isEmpty(str_date)) {
                val originalFormat = SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH)
                val targetFormat = SimpleDateFormat("dd-MM-yy")
                var date: Date? = null
                try {
                    date = originalFormat.parse(str_date) //2017-07-27
                } catch (e: ParseException) {
                    e.printStackTrace()
                }
                return targetFormat.format(date)
            }
            return ""
        }

        fun ymdHmsTodmyHms(str_date: String?): String? {
            if (!TextUtils.isEmpty(str_date)) {
                val originalFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ENGLISH)
                val targetFormat = SimpleDateFormat("EEE dd MMM yyyy hh:mm aa")
                var date: Date? = null
                try {
                    date = originalFormat.parse(str_date) //2017-07-27
                } catch (e: ParseException) {
                    e.printStackTrace()
                }
                return targetFormat.format(date)
            }
            return ""
        }

        fun showDialog(mContext: Context?, content: String?, isNegativeEnabled: Boolean, positiveButtonCallback: SingleButtonCallback?) {
            if (mContext == null) return
            MaterialDialog.Builder(mContext).content(content!!).positiveText("Ok").negativeText(if (isNegativeEnabled) "Cancel" else "").cancelable(false).onPositive(positiveButtonCallback!!).onNegative { dialog: MaterialDialog, which: DialogAction? -> dialog.dismiss() }.show()
        }

        fun showDialog(mContext: Context?, title: String?, content: String?, isNegativeEnabled: Boolean, positiveButtonText: String?, positiveButtonCallback: SingleButtonCallback?) {
            if (mContext == null) return
            MaterialDialog.Builder(mContext).title(title.toString()).content(content!!).positiveText(positiveButtonText.toString())
                .negativeText(if (isNegativeEnabled) "Cancel" else "")
                .cancelable(false)
                .onPositive(positiveButtonCallback!!)
                .onNegative { dialog: MaterialDialog, which: DialogAction? -> dialog.dismiss() }.show()
        }

        fun showDialog(mContext: Context?, title: String?, content: String?, isNegativeEnabled: Boolean, positiveButtonText: String?,
            positiveButtonTextColor: Int, positiveButtonCallback: SingleButtonCallback?) {
            if (mContext == null) return
            MaterialDialog.Builder(mContext).title(title.toString()).content(content!!).positiveText(positiveButtonText.toString())
                .positiveColor(positiveButtonTextColor)
                .negativeText(if (isNegativeEnabled) "Cancel" else "")
                .cancelable(false)
                .onPositive(positiveButtonCallback!!)
                .onNegative { dialog: MaterialDialog, which: DialogAction? -> dialog.dismiss() }.show()
        }

        fun getMimeType(path: String?): String? {
            val fileNameMap = URLConnection.getFileNameMap()
            var contentTypeFor = fileNameMap.getContentTypeFor(path)
            if (contentTypeFor == null) {
                contentTypeFor = "application/octet-stream"
            }
            return contentTypeFor
        }
    }
}
