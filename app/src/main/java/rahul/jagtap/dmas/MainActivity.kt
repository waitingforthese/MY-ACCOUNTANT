package rahul.jagtap.dmas

import android.Manifest
import android.annotation.SuppressLint
import android.app.ProgressDialog
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.TextUtils
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.WindowManager
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.afollestad.materialdialogs.DialogAction
import com.afollestad.materialdialogs.MaterialDialog
import com.bumptech.glide.Glide
import com.google.android.material.navigation.NavigationView
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.google.firebase.remoteconfig.ConfigUpdate
import com.google.firebase.remoteconfig.ConfigUpdateListener
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigException
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import id.zelory.compressor.Compressor
import kotlinx.coroutines.launch
import okhttp3.ResponseBody
import pl.aprilapps.easyphotopicker.ChooserType
import pl.aprilapps.easyphotopicker.DefaultCallback
import pl.aprilapps.easyphotopicker.EasyImage
import pl.aprilapps.easyphotopicker.MediaFile
import pl.aprilapps.easyphotopicker.MediaSource
import rahul.jagtap.dmas.adapter.MenuListAdapter
import rahul.jagtap.dmas.databinding.ActivityMainBinding
import rahul.jagtap.dmas.databinding.NavHeaderBinding
import rahul.jagtap.dmas.extensions.gone
import rahul.jagtap.dmas.extensions.longToast
import rahul.jagtap.dmas.extensions.toast
import rahul.jagtap.dmas.extensions.visible
import rahul.jagtap.dmas.model.ContactConfig
import rahul.jagtap.dmas.model.DayBook
import rahul.jagtap.dmas.model.ImageDetails
import rahul.jagtap.dmas.model.User
import rahul.jagtap.dmas.utils.GridDividerDecoration
import rahul.jagtap.dmas.utils.HomeGridDividerDecoration
import rahul.jagtap.dmas.utils.Utils
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.lang.reflect.Type
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit
import androidx.core.net.toUri


class MainActivity : BaseActivity(), NavigationView.OnNavigationItemSelectedListener {
    var dayBook: DayBook? = null
    private lateinit var menuListAdapter: MenuListAdapter
    private lateinit var menuList: ArrayList<String>
    private var userInfo: User? = null
    private val TAG = MainActivity::class.java.simpleName
    private var imageFileName: String = ""
    private var imageDownloadUrl: String = ""
    var imageUri: Uri? = null
    var userList = ArrayList<User>()
    var imageType = -1
    private lateinit var easyImage: EasyImage
    //    var geocoder: Geocoder? = null
    var remoteConfig: FirebaseRemoteConfig? = null
    private lateinit var binding: ActivityMainBinding
    private var contactConfig = ContactConfig()
    private var dayBookListenerAttached = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        val loggedInEmail = app?.preferences?.loggedInUser?.email
        if (screenshotAllowedEmailList.contains(loggedInEmail))
            Utils.disableScreenshot = false
        if (Utils.disableScreenshot) this.window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)
        val view = binding.root
        setContentView(view) //        MobileAds.initialize(this) {}
        setSupportActionBar(binding.toolbarLayout.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(false)
        supportActionBar?.setDisplayShowTitleEnabled(false)
        supportActionBar?.setHomeAsUpIndicator(mContext?.let { ContextCompat.getDrawable(it, R.drawable.ic_menu_white) })
        binding.toolbarLayout.toolbarTitle?.text = "माय अकाउंटंट"//getString(R.string.app_name)
        easyImage = EasyImage.Builder(this).setChooserType(ChooserType.CAMERA_AND_GALLERY).allowMultiple(false) // Setting to true will cause taken pictures to show up in the device gallery, DEFAULT false
            .setCopyImagesToPublicGalleryFolder(false).build()

        val toggle = ActionBarDrawerToggle(this, binding.drawerLayout, binding.toolbarLayout.toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close)
        binding.drawerLayout?.addDrawerListener(toggle)
        toggle.syncState()
        binding.navView?.setNavigationItemSelectedListener(this)
        binding.progressBar?.visible() //                val loggedInUser = app?.preferences?.loggedInUser
        //                loggedInUser?.isAdmin = "1"
        //        loggedInUser?.userType = "1"
        //                app?.preferences?.loggedInUser = loggedInUser
        //        geocoder = Geocoder(this, Locale.getDefault())
        renderHomeFromCache()
        fetchUserInfo() //        initAd()
        setBannerImage()
        setAdminData()

        remoteConfig = FirebaseRemoteConfig.getInstance()
        val builder = FirebaseRemoteConfigSettings.Builder()
        builder.minimumFetchIntervalInSeconds = 3600
        val settings = builder.build()
        remoteConfig?.setConfigSettingsAsync(settings)
        remoteConfig?.fetchAndActivate()?.addOnCompleteListener(this) { task ->
            if (task.isSuccessful) {
                val updated = task.result
                Log.d(TAG, "Config params updated: $updated") //                    Toast.makeText(
                //                        this,
                //                        "Fetch and activate succeeded",
                //                        Toast.LENGTH_SHORT,
                //                    ).show()
            } else { //                    Toast.makeText(
                //                        this,
                //                        "Fetch failed",
                //                        Toast.LENGTH_SHORT,
                //                    ).show()
            } //                displayWelcomeMessage()
        }
        remoteConfig?.addOnConfigUpdateListener(object : ConfigUpdateListener {
            override fun onUpdate(configUpdate: ConfigUpdate) {
                Log.e(TAG, "Updated keys: " + configUpdate.updatedKeys);

                if (configUpdate.updatedKeys.contains("appVersionCode")) {
                    remoteConfig?.activate()?.addOnCompleteListener {
                        val appVersionCode = remoteConfig?.getLong("appVersionCode") ?: 0
                        Log.e(TAG, "appVersionCode: $appVersionCode")
                        if (appVersionCode > BuildConfig.VERSION_CODE) {
                            showUpdateAppPopup()
                        }
                    }
                }
            }

            override fun onError(error: FirebaseRemoteConfigException) {
                Log.w(TAG, "Config update error with code: " + error.code, error)
            }
        })
        val appVersionCode = remoteConfig?.getLong("appVersionCode") ?: 0
        Log.e(TAG, "appVersionCode: $appVersionCode")
        if (appVersionCode > BuildConfig.VERSION_CODE) {
            showUpdateAppPopup()
        } //        remoteConfig?.add(object : ConfigUpdateListener {
        //            override fun onUpdate(configUpdate : ConfigUpdate) {
        //                Log.d(TAG, "Updated keys: " + configUpdate.updatedKeys);
        //
        //                if (configUpdate.updatedKeys.contains("welcome_message")) {
        //                    remoteConfig.activate().addOnCompleteListener {
        //                        displayWelcomeMessage()
        //                    }
        //                }
        //            }
        //
        //            override fun onError(error : FirebaseRemoteConfigException) {
        //                Log.w(TAG, "Config update error with code: " + error.code, error)
        //            }
        //        })
        //        showUpdateAppPopup()
        if (app?.preferences?.shouldShowPopup() == true && app?.preferences?.loggedInUser?.isAdmin != "1") loadContactConfigAndShowPopup()
        scheduleSubscribeReminder()
        setButtons()
    }

    private fun setButtons() {
        binding.tvContact?.setOnClickListener {
            callOrSms("+919552789899")
        }
        binding.tvAppVersionBottom.text = "Version: ${BuildConfig.VERSION_NAME}"
    }

    private fun scheduleSubscribeReminder() {
        val workRequest = PeriodicWorkRequestBuilder<SubscribeReminderWorker>(2, TimeUnit.DAYS).build()

        WorkManager.getInstance(applicationContext).enqueueUniquePeriodicWork("SubscribeReminderWork", ExistingPeriodicWorkPolicy.KEEP, workRequest)
    }

    /** Loads the admin-configurable contact config, then shows the subscribe popup with its links. */
    private fun loadContactConfigAndShowPopup() {
        database.child(Utils.CONTACT_US_TABLE).child("admin").addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (checkIfActivityDestroying()) return
                snapshot.getValue(ContactConfig::class.java)?.let { contactConfig = it }
                showSubscribePopup()
            }

            override fun onCancelled(error: DatabaseError) {
                if (checkIfActivityDestroying()) return
                showSubscribePopup()
            }
        })
    }

    private fun showSubscribePopup() {
        if (mContext == null) return
        app?.preferences?.setShouldShowPopup(false)
        val youtubeUrl = contactConfig.youtubeUrl.orEmpty()
        val instagramUrl = contactConfig.instagramUrl.orEmpty()
        val dialog = MaterialDialog.Builder(mContext!!)
            .customView(R.layout.dialog_subscribe, false)
            .cancelable(true)
            .build()
        val view = dialog.customView ?: return
        view.findViewById<TextView>(R.id.tvYoutubeHandle).text = handleFromUrl(youtubeUrl)
        view.findViewById<TextView>(R.id.tvInstagramHandle).text = handleFromUrl(instagramUrl)
        view.findViewById<View>(R.id.btnYoutube).setOnClickListener {
            openExternalLink(youtubeUrl)
            dialog.dismiss()
        }
        view.findViewById<View>(R.id.btnInstagram).setOnClickListener {
            openExternalLink(instagramUrl)
            dialog.dismiss()
        }
        view.findViewById<View>(R.id.tvLater).setOnClickListener { dialog.dismiss() }
        dialog.show()
    }

    /** Derives a "@handle" display string from a channel/profile URL (last path segment). */
    private fun handleFromUrl(url: String): String {
        if (url.isBlank()) return ""
        return try {
            val seg = Uri.parse(url).lastPathSegment?.takeIf { it.isNotBlank() } ?: return ""
            if (seg.startsWith("@")) seg else "@$seg"
        } catch (e: Exception) {
            ""
        }
    }

    /** Opens a social/web link, ignoring the rare case where no browser/app can handle it. */
    private fun openExternalLink(url: String) {
        if (url.isBlank()) return
        try {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        } catch (e: ActivityNotFoundException) {
            toast("No app found to open this link")
        }
    }

    private fun showUpdateAppPopup() {
        if (mContext == null) return
        MaterialDialog.Builder(mContext!!).title("New version available!")
            .titleColor(ContextCompat.getColor(this, R.color.red))
            .content("नवीन वैशिष्ट्यांचा लाभ घेण्यासाठी कृपया तुमचे ॲप अपडेट करा.")
            .contentColor(ContextCompat.getColor(this, R.color.red))
            .positiveText("Update App")
            .positiveColor(ContextCompat.getColor(this, R.color.red))
            .negativeText("Cancel")
            .negativeColor(ContextCompat.getColor(this, R.color.font_black_6))
            .cancelable(false)
            .onPositive { dialog, which ->
                run {
                    dialog.dismiss()
                    redirectToPlayStore()
                    finish()
                }
            }.onNegative { dialog: MaterialDialog, which: DialogAction? -> dialog.dismiss() }.show()
//        Utils.showDialog(mContext, "New version available!", "नवीन वैशिष्ट्यांचा लाभ घेण्यासाठी कृपया तुमचे ॲप अपडेट करा.", true, "Update App",
//            ContextCompat.getColor(this, R.color.red)) { dialog, which ->
//            run {
//                dialog.dismiss()
//                val appPackageName = packageName // getPackageName() from Context or Activity object
//
//                try {
//                    startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$appPackageName")))
//                } catch (anfe: ActivityNotFoundException) {
//                    startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=$appPackageName")))
//                }
//                finish()
//            }
//        }
    }

    private fun redirectToPlayStore() {
        val appPackageName = packageName // getPackageName() from Context or Activity object

        try {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$appPackageName")))
        } catch (anfe: ActivityNotFoundException) {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=$appPackageName")))
        }
    }

    private fun setAdminData() {
        if (app?.preferences?.loggedInUser?.isAdmin == "1") {
            binding.tvTitle?.visible() //        Log.e("token", "" + app.getPreferences().getToken());
            Handler(Looper.getMainLooper()).postDelayed({
                val menu: Menu = binding.navView.menu
                val nav_banner_image = menu.findItem(R.id.nav_banner_image)
                nav_banner_image?.isVisible = true
                val nav_esuvidha_image = menu.findItem(R.id.nav_esuvidha_image)
                nav_esuvidha_image?.isVisible = true
                val nav_esuvidha_list_image = menu.findItem(R.id.nav_esuvidha_list_image)
                nav_esuvidha_list_image?.isVisible = true
                val nav_taluka_suvidha_list_image = menu.findItem(R.id.nav_taluka_suvidha_list_image)
                nav_taluka_suvidha_list_image?.isVisible = true
                val nav_accounting_menu_image = menu.findItem(R.id.nav_accounting_menu_image)
                nav_accounting_menu_image?.isVisible = true
                val nav_register_image = menu.findItem(R.id.nav_register_image)
                nav_register_image?.isVisible = true
            }, 1000)
        }
    }

    private fun setBannerImage() {
        database.child(Utils.BANNER_IMAGE_TABLE).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onCancelled(p0: DatabaseError) {
            }

            override fun onDataChange(dataSnapshot: DataSnapshot) {
                val imageDetails = dataSnapshot.getValue(ImageDetails::class.java)
                if (imageDetails != null) { // Check if the activity is being destroyed
                    if (checkIfActivityDestroying()) return
                    if (mContext != null && !TextUtils.isEmpty(imageDetails.imageDownloadUrl)) mContext?.let { Glide.with(it).load(imageDetails.imageDownloadUrl).into(binding.ivAd) }
                }
            }
        })
    }

    //    private fun initAd() {
    //        val adRequest = AdRequest.Builder().build()
    //        adView?.loadAd(adRequest)
    //    }

    /**
     * The home grid is a static, role-based list; the role is already in the cached user. Render it
     * immediately so the screen doesn't sit on a spinner waiting for the users/{uid} read. [fetchUserInfo]
     * still runs to enforce the block-check and refresh the user, re-rendering if the role changed.
     */
    private fun renderHomeFromCache() {
        val cached = app?.preferences?.loggedInUser ?: return
        val headerBinding = NavHeaderBinding.bind(binding.navView.getHeaderView(0))
        headerBinding.tvName?.text = "Welcome, ${cached.name}"
        headerBinding.tvAppVersion?.text = "Version ${BuildConfig.VERSION_NAME}"
        setMenuGrid()
        binding.progressBar?.gone()
    }

    private fun fetchUserInfo() {
        app?.preferences?.loggedInUserId?.let { uid ->
            database.child(Utils.USERS_TABLE).child(uid).addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onCancelled(databaseError: DatabaseError) {
                    Log.e(TAG, "getUser:onCancelled", databaseError.toException())
                }

                override fun onDataChange(dataSnapshot: DataSnapshot) { // Get user value
                    userInfo = dataSnapshot.getValue(User::class.java)
                    if (userInfo?.isBlocked == "1") {
                        logout()
                    } else { //                                                        val loggedInUser = app?.preferences?.loggedInUser
                        //                                                        userInfo?.isAdmin = "0"
                        //                        userInfo?.userType = "0"
                        //                                                        app?.preferences?.loggedInUser = loggedInUser
                        app?.preferences?.loggedInUser = userInfo //                        Log.e("uid", app?.preferences?.loggedInUser?.uid.toString())
                        val headerBinding = NavHeaderBinding.bind(binding.navView.getHeaderView(0))
                        headerBinding.tvName?.text = "Welcome, ${userInfo?.name}"
                        headerBinding.tvAppVersion?.text = "Version ${BuildConfig.VERSION_NAME}"
                        setMenuGrid()
                        binding.progressBar?.gone()
                    }
                }
            })
        }
    }

    private fun setMenuGrid() {
        val gridLayoutManager = GridLayoutManager(mContext, 3)
        menuList = ArrayList<String>()

        val loggedInUser = app?.preferences?.loggedInUser

        if (loggedInUser?.isAdmin == "1") {
            // Admin menu: keep only the requested features.
            menuList.add("हे अँप कसे वापरावे")
            menuList.add("ट्रेनिंग व्हिडिओ")
            menuList.add("येथून फी भरावी")
            menuList.add("नियम व अटी")
            menuList.add("Day Book")
            menuList.add("Accounting Services")
            menuList.add("Reports")
            menuList.add("Send Report")
            menuList.add("Notifications")
            menuList.add("All Users")
            menuList.add("Text Msg")
            menuList.add("नवनवीन माहिती")
        } else if (loggedInUser?.userType == "2") {
            // Employee menu: remove fee/charges and unrelated administration entries.
            menuList.add("हे अँप कसे वापरावे")
            menuList.add("ट्रेनिंग व्हिडिओ")
            menuList.add("येथून फी भरावी")
            menuList.add("नियम व अटी")
            menuList.add("Day Book")
            menuList.add("Accounting Services")
            menuList.add("Reports")
            menuList.add("Send Report")
            menuList.add("Notifications")
            menuList.add("All Users")
            menuList.add("Text Msg")
            menuList.add("नवनवीन माहिती")
        } else {
            // Regular user menu.
            menuList.add("My Accountant")
            menuList.add("खाते बुक\n(स्वतःचा हिशोब स्वतः करा)")
            menuList.add("नवनवीन माहिती")
            menuList.add("येथून फी भरावी")
            menuList.add("नियम व अटी")
            menuList.add("हे अँप कसे वापरावे")
            menuList.add("ट्रेनिंग व्हिडिओ")
            menuList.add("contact us")
        }

        menuListAdapter = MenuListAdapter(mContext, menuList)
        gridLayoutManager.spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
            override fun getSpanSize(position: Int): Int {
                return if (menuListAdapter.isFullWidth(menuList.getOrNull(position))) 3 else 1
            }
        }
        binding.rvMenu?.layoutManager = gridLayoutManager
        // setMenuGrid() runs more than once (cache render + network refresh); add the divider only once,
        // otherwise each pass stacks another decoration and the inter-tile spacing keeps growing.
        if (binding.rvMenu?.itemDecorationCount == 0) {
            binding.rvMenu?.addItemDecoration(HomeGridDividerDecoration(mContext))
        }
        binding.rvMenu.adapter = menuListAdapter
        if (app?.preferences?.loggedInUser?.isAdmin != "1" || app?.preferences?.loggedInUser?.isAdmin != "2") setDayBookData()
    }

    private fun setDayBookData() {
        // setMenuGrid() can run twice (cache render + network refresh); attach this persistent listener once.
        if (dayBookListenerAttached) return
        dayBookListenerAttached = true
        app?.preferences?.loggedInUser?.uid?.let {
            Firebase.database.getReference(Utils.DAY_BOOK_TABLE).child(it).addValueEventListener(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    dayBook = dataSnapshot.getValue(DayBook::class.java) //                    if (dayBook != null) {
                    //                        menuList.add("Day Book")
                    //                        menuListAdapter = MenuListAdapter(mContext, menuList)
                    //                        rvMenu.adapter = menuListAdapter
                    //                    }
                }

                override fun onCancelled(dataSnapshot: DatabaseError) {
                }
            })
        }
    }

    /**
  
       

    /** Fire-and-forget refresh of the e-suvidha cache blobs so the next open reflects any server change. */
    private fun refreshEsuvidhaCacheSilently() {
        val prefs = app?.preferences ?: return
        app?.apiRequestHelper?.apiService?.esuvidhaDynamicTypes?.enqueue(object : Callback<ResponseBody> {
            override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                if (response.isSuccessful) EsuvidhaCache.saveDynamicTypesJson(prefs, response.body()?.string())
            }

            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {}
        })
        app?.apiRequestHelper?.apiService?.suchna?.enqueue(object : Callback<ResponseBody> {
            override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                if (response.isSuccessful) EsuvidhaCache.saveSuchnaJson(prefs, response.body()?.string())
            }

            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {}
        })
    }

   

    override fun onNavigationItemSelected(
        item: MenuItem
    ): Boolean { // Handle navigation view item clicks here.
        val id = item.itemId
        if (id == R.id.nav_logout) {
            logout()
        }
        if (id == R.id.nav_privacy_policy) {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("http://play.google.com/store/apps/details?id=" + BuildConfig.APPLICATION_ID)))
        }
        if (id == R.id.nav_contact_us) {
            startActivity(Intent(mContext, ContactActivity::class.java))
        }
        if (id == R.id.nav_payment_details) {
            startActivity(Intent(mContext, PaymentDetailsActivity::class.java))
        }
        if (id == R.id.nav_share) {
            shareApp()
        }
        if (id == R.id.nav_about_team) {
            startActivity(Intent(mContext, AboutTeamActivity::class.java))
        }
        if (id == R.id.nav_banner_image) {
            imageType = 1
            choosePhotoWithPermissions()
        }
        if (id == R.id.nav_esuvidha_image) {
            imageType = 2
            choosePhotoWithPermissions()
        }
        if (id == R.id.nav_esuvidha_list_image) {
            imageType = 3
            choosePhotoWithPermissions()
        }
        if (id == R.id.nav_taluka_suvidha_list_image) {
            imageType = 4
            choosePhotoWithPermissions()
        }
        if (id == R.id.nav_accounting_menu_image) {
            imageType = 5
            choosePhotoWithPermissions()
        }
        if (id == R.id.nav_register_image) {
            imageType = 6
            choosePhotoWithPermissions()
        }
        if (id == R.id.nav_rate_us) {
            rateUsOnGooglePlay()
        }
        if (id == R.id.nav_new_update) {
            redirectToPlayStore()
        }
        binding.drawerLayout?.closeDrawer(GravityCompat.START)
        return true
    }

    private fun shareApp() {
        val shareIntent = Intent(Intent.ACTION_SEND)
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.app_name))
        shareIntent.putExtra(Intent.EXTRA_TEXT, "Download and install this app: https://play.google.com/store/apps/details?id=" + BuildConfig.APPLICATION_ID)
        shareIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_DOCUMENT)
        shareIntent.type = "text/plain"
        startActivity(Intent.createChooser(shareIntent, "Share app with:"))
    }

    fun rateUsOnGooglePlay() {
        val marketUri: Uri = Uri.parse("market://details?id=$packageName")
        try {
            startActivity(Intent(Intent.ACTION_VIEW, marketUri))
        } catch (ex: ActivityNotFoundException) {
            Toast.makeText(this, "Couldn't find Google Play Store on this device", Toast.LENGTH_SHORT).show()
        }
    }

    private fun logout() {
        val token = app?.preferences?.token.toString()
        val isAdmin = app?.preferences?.loggedInUser?.isAdmin == "1"
        Firebase.auth.signOut()
        app?.preferences?.logOutUser()
        if (!TextUtils.isEmpty(token) && isAdmin) app?.preferences?.token = token
        Log.e("token", token)
        startActivity(Intent(mContext, LoginActivity::class.java))
        finish()
    }

    override fun onResume() {
        super.onResume()
        setAdminData()
        getUserList()
    }

    @SuppressLint("CheckResult")
    private fun choosePhotoWithPermissions() {
//        rxPermissions?.requestEachCombined(Manifest.permission.CAMERA)?.subscribe {
//            when {
//                it.granted -> { // get picture
                    easyImage.openChooser(this)
//                }
//
//                it.shouldShowRequestPermissionRationale -> { // At least one denied permission without ask never again
//                }
//
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
            override fun onMediaFilesPicked(
                imageFiles: Array<MediaFile>, source: MediaSource
            ) { //                val dir = getExternalFilesDir(Environment.DIRECTORY_PICTURES)?.absolutePath
                //                val fileName = AccountingServicesActivity.dateFormatForPhoto.format(System.currentTimeMillis()) + ".jpg"
                //                val image = File("$dir/$fileName")

                lifecycleScope.launch {
                    val compressedImageFile = mContext?.let { Compressor.compress(it, imageFiles[0].file) }
                    Log.e(TAG, "${compressedImageFile?.path}")
                    compressedImageFile?.let {
                        imageUri = Uri.fromFile(it)
                        Utils.showDialog(mContext, "Continue adding/changing banner image?", true) { dialog, which ->
                            run {
                                dialog.dismiss()
                                when (imageType) {
                                    1 -> uploadBannerPhoto(imageUri!!)
                                    2 -> uploadESuvidhaPhoto(imageUri!!)
                                    3 -> uploadESuvidhaListPhoto(imageUri!!)
                                    4 -> uploadTalukaSuvidhaListPhoto(imageUri!!)
                                    5 -> uploadAccountingMenuPhoto(imageUri!!)
                                    6 -> uploadRegisterScreenPhoto(imageUri!!)
                                }
                            }
                        }
                    }
                }
            }
        })
//        if (requestCode === 1) {
//            if (resultCode === RESULT_OK || resultCode === 11) { // Result OK or specific to some UPI apps
//                if (data != null) {
//                    val response: String = data.getStringExtra("response").toString()
//                    parseUPIResponse(response)
//                } else {
//                    Toast.makeText(this, "Transaction failed. Try again.", Toast.LENGTH_SHORT).show()
//                }
//            } else {
//                Toast.makeText(this, "Transaction canceled by user.", Toast.LENGTH_SHORT).show()
//            }
//        }
    }

//    private fun parseUPIResponse(response: String) {
//        var response: String? = response
//        if (response == null) {
//            response = "discard"
//        }
//
//        var status = ""
//        val responseArray = response.split("&".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
//        for (res in responseArray) {
//            val keyValue = res.split("=".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
//            if (keyValue.size >= 2) {
//                if (keyValue[0].equals("Status", ignoreCase = true)) {
//                    status = keyValue[1]
//                }
//            }
//        }
//
//        if (status.equals("success", ignoreCase = true)) {
//            Toast.makeText(this, "Transaction successful!", Toast.LENGTH_SHORT).show()
//        } else {
//            Toast.makeText(this, "Transaction failed or canceled.", Toast.LENGTH_SHORT).show()
//        }
//    }

    private fun uploadBannerPhoto(fileUri: Uri) {
        val cpd: ProgressDialog? = ProgressDialog(mContext)
        cpd?.setCancelable(false)
        cpd?.show()
        imageFileName = "banner_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.ENGLISH).format(Date())}.jpg"
        val filepath = storageRef.child(Utils.BANNER_IMAGE_TABLE).child(imageFileName)
        filepath.putFile(fileUri).addOnSuccessListener {
            try {
                if (cpd != null && cpd.isShowing) cpd.dismiss()
                filepath.downloadUrl.addOnSuccessListener { uri: Uri ->
                    imageDownloadUrl = uri.toString()
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

    private fun uploadESuvidhaPhoto(fileUri: Uri) {
        val cpd: ProgressDialog? = ProgressDialog(mContext)
        cpd?.setCancelable(false)
        cpd?.show()
        imageFileName = "esuvidha_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.ENGLISH).format(Date())}.jpg"
        val filepath = storageRef.child(Utils.ESUVIDHA_IMAGE_TABLE).child(imageFileName)
        filepath.putFile(fileUri).addOnSuccessListener {
            try {
                if (cpd != null && cpd.isShowing) cpd.dismiss()
                filepath.downloadUrl.addOnSuccessListener { uri: Uri ->
                    imageDownloadUrl = uri.toString()
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

    private fun uploadESuvidhaListPhoto(fileUri: Uri) {
        val cpd: ProgressDialog? = ProgressDialog(mContext)
        cpd?.setCancelable(false)
        cpd?.show()
        imageFileName = "esuvidha_list_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.ENGLISH).format(Date())}.jpg"
        val filepath = storageRef.child(Utils.ESUVIDHA_LIST_IMAGE_TABLE).child(imageFileName)
        filepath.putFile(fileUri).addOnSuccessListener {
            try {
                if (cpd != null && cpd.isShowing) cpd.dismiss()
                filepath.downloadUrl.addOnSuccessListener { uri: Uri ->
                    imageDownloadUrl = uri.toString()
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

    private fun uploadTalukaSuvidhaListPhoto(fileUri: Uri) {
        val cpd: ProgressDialog? = ProgressDialog(mContext)
        cpd?.setCancelable(false)
        cpd?.show()
        imageFileName = "taluka_suvidha_list_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.ENGLISH).format(Date())}.jpg"
        val filepath = storageRef.child(Utils.TALUKA_SUVIDHA_LIST_IMAGE_TABLE).child(imageFileName)
        filepath.putFile(fileUri).addOnSuccessListener {
            try {
                if (cpd != null && cpd.isShowing) cpd.dismiss()
                filepath.downloadUrl.addOnSuccessListener { uri: Uri ->
                    imageDownloadUrl = uri.toString()
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

    private fun uploadAccountingMenuPhoto(fileUri: Uri) {
        val cpd: ProgressDialog? = ProgressDialog(mContext)
        cpd?.setCancelable(false)
        cpd?.show()
        imageFileName = "accounting_menu_img_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.ENGLISH).format(Date())}.jpg"
        val filepath = storageRef.child(Utils.TALUKA_SUVIDHA_LIST_IMAGE_TABLE).child(imageFileName)
        filepath.putFile(fileUri).addOnSuccessListener {
            try {
                if (cpd != null && cpd.isShowing) cpd.dismiss()
                filepath.downloadUrl.addOnSuccessListener { uri: Uri ->
                    imageDownloadUrl = uri.toString()
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

    private fun uploadRegisterScreenPhoto(fileUri: Uri) {
        val cpd: ProgressDialog? = ProgressDialog(mContext)
        cpd?.setCancelable(false)
        cpd?.show()
        imageFileName = "register_img_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.ENGLISH).format(Date())}.jpg"
        val filepath = storageRef.child(Utils.TALUKA_SUVIDHA_LIST_IMAGE_TABLE).child(imageFileName)
        filepath.putFile(fileUri).addOnSuccessListener {
            try {
                if (cpd != null && cpd.isShowing) cpd.dismiss()
                filepath.downloadUrl.addOnSuccessListener { uri: Uri ->
                    imageDownloadUrl = uri.toString()
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
        billMap["imageFileName"] = imageFileName
        billMap["imageDownloadUrl"] = imageDownloadUrl
        billMap["timeStamp"] = System.currentTimeMillis()

        val messageUserMap = HashMap<String, Any?>()
        when (imageType) {
            1 -> messageUserMap["${Utils.BANNER_IMAGE_TABLE}"] = billMap
            2 -> messageUserMap["${Utils.ESUVIDHA_IMAGE_TABLE}"] = billMap
            3 -> messageUserMap["${Utils.ESUVIDHA_LIST_IMAGE_TABLE}"] = billMap
            4 -> messageUserMap["${Utils.TALUKA_SUVIDHA_LIST_IMAGE_TABLE}"] = billMap
            5 -> messageUserMap["${Utils.ACCOUNTING_MENU_IMAGE_TABLE}"] = billMap
            6 -> messageUserMap["${Utils.REGISTER_SCREEN_IMAGE_TABLE}"] = billMap
        }
        database.updateChildren(messageUserMap) { databaseError: DatabaseError?, _: DatabaseReference? ->
            if (databaseError != null) {
                Log.e("db error", databaseError.message)
            }
        }
        when (imageType) {
            1 -> {
                toast("Updated banner image")
                setBannerImage()
            }

            2 -> toast("Updated E-Suvidha image")
            3 -> toast("Updated E-Suvidha List image")
            4 -> toast("Updated Taluka Suvidha List image")
            5 -> toast("Updated Accounting Menu Image")
            6 -> toast("Updated Register Image")
        }
    }

    private fun getUserList() {
        database.child(Utils.USERS_TABLE).addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                Log.i("firebase", "Got value ${snapshot.value}") // Get user value
                val json = Gson().toJson(snapshot.value)
                val type: Type = object : TypeToken<HashMap<String, User>?>() {}.type
                val map: HashMap<String, User> = Gson().fromJson(json, type)
                userList.clear()
                userList.addAll(map.values.toMutableList())
                binding.tvTotalUsers?.text = "एकूण युजर: ${userList.size}" //                setScrollableRandomUserlist()
            }

            override fun onCancelled(error: DatabaseError) {
            }
        })
    }

    //    private fun setScrollableRandomUserlist() {
    //        val randomUserList = ArrayList<String>()
    //        var list = userList
    //        list.shuffle()
    //        list.subList(0, 50).forEach { user ->
    //            val cityNameFromAddress = getCityNameFromAddress(user.address.toString())
    //            Log.e(TAG, "setScrollableRandomUserlist: "+ cityNameFromAddress.toString())
    //            randomUserList.add(" \u25CF "+user.name + "(${cityNameFromAddress})")
    //        }
    //        randomUserList.shuffle()
    ////        val subList = randomUserList.subList(0, 50)
    ////
    ////        subList.map {s ->
    ////            val t = s
    ////            it
    ////        }
    //
    //        val s: String = list.toString().replace("[", "").replace("]", "").replace(",".toRegex(), "")
    //        // set duration based on text length
    //        val roundDuration: Int = s.length / 10 * 1000 + 10000
    //        tvAd.setRndDuration(roundDuration)
    //        tvAd.visible()
    //        tvAd.text = s
    //        tvAd.isSelected = true
    //        tvAd.startScroll()
    //    }

    //    private fun getCityNameFromAddress(address: String): String? {
    //        if (address.isNullOrEmpty()) return ""
    //
    //        val addresses = geocoder?.getFromLocationName(address, 1)
    //        return if (addresses?.isNotEmpty() == true) {
    //            "("+ addresses[0].locality+")" // This will give you the city name
    //        } else {
    //            ""
    //        }
    //    }

    // Configure sign-in to request the YouTube scope
    //        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
    //            .requestEmail()
    //            .requestScopes(Scope("https://www.googleapis.com/auth/youtube.readonly"))
    //            .build()
    //
    //        mGoogleSignInClient = GoogleSignIn.getClient(this, gso)
    //        signIn()
    //    val googleIdOption: GetGoogleIdOption = GetGoogleIdOption.Builder().setFilterByAuthorizedAccounts(false).setServerClientId("635481756457-ps3hut2sv40acosmnujskak5nh16r9g6.apps.googleusercontent.com").setAutoSelectEnabled(true) //            .setNonce(<nonce string to use when generating a Google ID token>)
    //        .build()
    //
    //    val request: GetCredentialRequest = GetCredentialRequest.Builder().addCredentialOption(googleIdOption).build() //
    //    credentialManager = CredentialManager.create(this) //
    //    //        CoroutineScope(Dispatchers.IO).launch {
    //    //            try {
    //    //                val result = credentialManager.getCredential(
    //    //                    request = request,
    //    //                    context = this@MainActivity,
    //    //                )
    //    //                handleSignIn(result)
    //    //            } catch (e: GetCredentialException) {
    //    //                //                handleFailure(e)
    //    //                e.printStackTrace()
    //    //            }
    //    //        }
    //    binding.btnSubscribeProgrammatically.setOnClickListener {
    //        CoroutineScope(Dispatchers.IO).launch {
    //            try {
    //                val result = credentialManager.getCredential(
    //                    request = request,
    //                    context = this@MainActivity,
    //                )
    //                handleSignIn(result)
    //            } catch (e: GetCredentialException) { //                handleFailure(e)
    //                e.printStackTrace()
    //            }
    //        }
    //    }

    //    private fun handleSignInResult(completedTask: Task<GoogleSignInAccount>) {
    //        try {
    //            val account = completedTask.getResult(ApiException::class.java) // Signed in successfully, proceed to check subscription
    //            checkSubscription(account)
    //        } catch (e: ApiException) { // Handle sign-in failure
    //            e.printStackTrace()
    //            Log.w("SignIn", "signInResult:failed code=" + e.statusCode)
    //        }
    //    }
    //
    //    private fun checkSubscription(
    //        account: GoogleSignInAccount
    //    ) { // Initialize credentials and YouTube API client
    //        val credential = GoogleAccountCredential.usingOAuth2(applicationContext, SCOPES).setBackOff(ExponentialBackOff()).apply {
    //            selectedAccount = account.account
    //        }
    //
    //        val youtube = YouTube.Builder(NetHttpTransport(), GsonFactory(), credential).setApplicationName("YourAppName").build()
    //
    //        CoroutineScope(Dispatchers.IO).launch {
    //            try { // Replace with your channel ID
    //                val targetChannelId = "LoudFindings"
    //
    //                val subscriptionRequest = youtube.subscriptions().list(listOf("snippet")).setMine(true).setMaxResults(50L) // Adjust as needed
    //
    //                var isSubscribed = false
    //                var nextPageToken: String? = null
    //
    //                do {
    //                    subscriptionRequest.pageToken = nextPageToken
    //                    val response = subscriptionRequest.execute()
    //
    //                    for (item in response.items) {
    //                        Log.e(TAG, "checkSubscription: " + item.snippet.resourceId.channelId)
    //                        if (item.snippet.resourceId.channelId == targetChannelId) {
    //                            isSubscribed = true
    //                            break
    //                        }
    //                    }
    //
    //                    nextPageToken = response.nextPageToken
    //                } while (nextPageToken != null && !isSubscribed)
    //
    //                withContext(Dispatchers.Main) {
    //                    if (isSubscribed) { // Allow user to proceed
    //                        proceedToApp()
    //                    } else { // Prompt user to subscribe
    //                        promptSubscription()
    //                    }
    //                }
    //            } catch (e: Exception) {
    //                e.printStackTrace()
    //                withContext(Dispatchers.Main) { // Handle error, possibly retry or show message
    //                }
    //            }
    //        }
    //    }
    //
    //    private fun proceedToApp() {
    //        toast("Already subscribed")
    //    }
    //
    //    private fun promptSubscription() { // Show a dialog or screen prompting the user to subscribe
    //        // You can include a button that opens the YouTube channel
    //
    //        AlertDialog.Builder(this).setTitle("Subscribe Required").setMessage("Please subscribe to our YouTube channel to use the app.").setPositiveButton("Subscribe") { dialog, _ -> // Open YouTube channel
    //            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com/@LoudFindings"))
    //            startActivity(intent)
    //        }.setNegativeButton("Exit") { dialog, _ -> // Exit the app or restrict access
    //            finish()
    //        }.setCancelable(false).show()
    //    }

    //    private fun subscribeToChannel(youtube: YouTube, channelId: String) {
    //        lifecycleScope.launch(Dispatchers.IO) {
    //            try { // Create a subscription object
    //                val subscription = com.google.api.services.youtube.model.Subscription().setSnippet(com.google.api.services.youtube.model.SubscriptionSnippet().setResourceId(com.google.api.services.youtube.model.ResourceId().setKind("youtube#channel").setChannelId(channelId)))
    //
    //                // Insert the subscription
    //                youtube.subscriptions().insert(listOf("snippet"), subscription).execute()
    //
    //                withContext(Dispatchers.Main) {
    //                    Toast.makeText(this@MainActivity, "Successfully subscribed!", Toast.LENGTH_SHORT).show()
    //                }
    //
    //            } catch (e: Exception) {
    //                Log.e(TAG, "Failed to subscribe: ${e.message}", e)
    //                withContext(Dispatchers.Main) {
    //                    Toast.makeText(this@MainActivity, "Failed to subscribe: ${e.message}", Toast.LENGTH_LONG).show()
    //                }
    //            }
    //        }
    //    }
    //
    //    // Start the sign-in intent
    //    //    fun signIn() {
    //    //        val signInIntent = mGoogleSignInClient?.signInIntent
    //    //        startActivityForResult(signInIntent, RC_SIGN_IN)
    //    //    }
    //
    //    fun handleSignIn(
    //        result: GetCredentialResponse
    //    ) { // Handle the successfully returned credential.
    //        val credential = result.credential
    //
    //        when (credential) {
    //
    //            // Passkey credential
    //            is PublicKeyCredential -> { // Share responseJson such as a GetCredentialResponse on your server to
    //                // validate and authenticate
    //                responseJson = credential.authenticationResponseJson
    //                Log.e(TAG, "responseJson: $responseJson")
    //            }
    //
    //            // Password credential
    //            is PasswordCredential -> { // Send ID and password to your server to validate and authenticate.
    //                val username = credential.id
    //                val password = credential.password
    //                Log.e(TAG, "username: $username")
    //                Log.e(TAG, "password: $password")
    //            }
    //
    //            is GoogleIdTokenCredential -> {
    //                val id = credential.id
    //                val idToken = credential.idToken
    //                val type = credential.type
    //                val data = credential.data
    //                val displayName = credential.displayName
    //                val familyName = credential.familyName
    //                val givenName = credential.givenName
    //                val phoneNumber = credential.phoneNumber
    //                val profilePictureUri = credential.profilePictureUri
    //                Log.e(TAG, "id: $id")
    //                Log.e(TAG, "idToken: $idToken")
    //                Log.e(TAG, "type: $type")
    //                Log.e(TAG, "data: $data")
    //                Log.e(TAG, "displayName: $displayName")
    //                Log.e(TAG, "familyName: $familyName")
    //                Log.e(TAG, "givenName: $givenName")
    //                Log.e(TAG, "phoneNumber: $phoneNumber")
    //                Log.e(TAG, "profilePictureUri: $profilePictureUri")
    //                val cred = GoogleAccountCredential.usingOAuth2(applicationContext, SCOPES).setBackOff(ExponentialBackOff()).apply {
    //                    selectedAccount = Account(displayName
    //                        ?: "", type) //                        credential = credential
    //                }
    //                val youtube = YouTube.Builder(NetHttpTransport(), GsonFactory(), cred).setApplicationName("YourAppName").build()
    //                subscribeToChannel(youtube, "LoudFindings")
    //                //                CoroutineScope(Dispatchers.IO).launch {
    //                //                    try { // Replace with your channel ID
    //                //                        val targetChannelId = "LoudFindings"
    //                //
    //                //                        val subscriptionRequest = youtube.subscriptions().list(listOf("snippet")).setMine(true).setMaxResults(50L) // Adjust as needed
    //                //
    //                //                        var isSubscribed = false
    //                //                        var nextPageToken: String? = null
    //                //
    //                //                        do {
    //                //                            subscriptionRequest.pageToken = nextPageToken
    //                //                            val response = subscriptionRequest.execute()
    //                //                            Log.e(TAG, "response: $response")
    //                //                            for (item in response.items) {
    //                //                                Log.e(TAG, "checkSubscription: " + item.snippet.resourceId.channelId)
    //                //                                if (item.snippet.resourceId.channelId == targetChannelId) {
    //                //                                    isSubscribed = true
    //                //                                    break
    //                //                                }
    //                //                            }
    //                //
    //                //                            nextPageToken = response.nextPageToken
    //                //                        } while (nextPageToken != null && !isSubscribed)
    //                //
    //                //                        withContext(Dispatchers.Main) {
    //                //                            if (isSubscribed) { // Allow user to proceed
    //                //                                proceedToApp()
    //                //                            } else { // Prompt user to subscribe
    //                //                                promptSubscription()
    //                //                            }
    //                //                        }
    //                //                    } catch (e: Exception) {
    //                //                        e.printStackTrace()
    //                //                        withContext(Dispatchers.Main) { // Handle error, possibly retry or show message
    //                //                        }
    //                //                    }
    //                //                }
    //            } // GoogleIdToken credential
    //            is CustomCredential -> {
    //                if (credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
    //                    try { // Use googleIdTokenCredential and extract the ID to validate and
    //                        // authenticate on your server.
    //                        val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data) // You can use the members of googleIdTokenCredential directly for UX
    //                        // purposes, but don't use them to store or control access to user
    //                        // data. For that you first need to validate the token:
    //                        // pass googleIdTokenCredential.getIdToken() to the backend server.
    //                        //                        GoogleIdTokenVerifier verifier = ... // see validation instructions
    //                        //                        GoogleIdToken idToken = verifier.verify(idTokenString);
    //                        //                        // To get a stable account identifier (e.g. for storing user data),
    //                        //                        // use the subject ID:
    //                        //                        idToken.getPayload().getSubject()
    //                    } catch (e: GoogleIdTokenParsingException) {
    //                        Log.e(TAG, "Received an invalid google id token response", e)
    //                    }
    //                } else { // Catch any unrecognized custom credential type here.
    //                    Log.e(TAG, "Unexpected type of credential")
    //                }
    //            }
    //
    //            else -> { // Catch any unrecognized credential type here.
    //                Log.e(TAG, "Unexpected type of credential")
    //            }
    //        }
    //    }
}
