package rahul.jagtap.dmas

import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.view.MenuItem
import android.view.WindowManager
import androidx.recyclerview.widget.GridLayoutManager
import com.bumptech.glide.Glide
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import okhttp3.ResponseBody
import rahul.jagtap.dmas.databinding.ActivityAccountingBinding
import rahul.jagtap.dmas.extensions.gone
import rahul.jagtap.dmas.extensions.visible
import rahul.jagtap.dmas.model.DayBook
import rahul.jagtap.dmas.model.ImageDetails
import rahul.jagtap.dmas.utils.GridDividerDecoration
import rahul.jagtap.dmas.utils.Utils
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.lang.reflect.Type


class ESuvidhaMenuActivity : BaseActivity() {
    var dayBook: DayBook? = null
    private lateinit var eSuvidhaMenuAdapter: ESuvidhaMenuAdapter
    private lateinit var menuList: java.util.ArrayList<String>
    private val TAG = ESuvidhaMenuActivity::class.java.simpleName
    lateinit var binding: ActivityAccountingBinding
    var jyotish_shastra_suchna = ""
    var typesMap: HashMap<String, HashMap<String, HashMap<String, String>>>? = null
    private var renderedFromCache = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAccountingBinding.inflate(layoutInflater)
        if (Utils.disableScreenshot) this.window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbarLayout.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowTitleEnabled(false)
        binding.toolbarLayout.toolbarTitle?.text = "महा ई सुविधा - सुविधा संग्रह"

        setMenuGrid()
        setESuvidhaImage()
        renderFromCache()
        fetchDynamicTypes()
    }

    /**
     * Paint the menu instantly from the last-known snapshot so the screen never opens blank while the
     * two network calls run. [fetchDynamicTypes] still refreshes in the background and re-renders.
     */
    private fun renderFromCache() {
        val prefs = app?.preferences
        val cachedTypes = rahul.jagtap.dmas.utils.EsuvidhaCache.getDynamicTypes(prefs)
        val cachedSuchna = rahul.jagtap.dmas.utils.EsuvidhaCache.getSuchna(prefs)
        if (cachedTypes != null || cachedSuchna != null) {
            typesMap = cachedTypes
            renderedFromCache = true
            buildMenuAdapter(cachedSuchna)
        }
    }

    /** Append grouped-service tiles for the current [typesMap] and (re)bind the adapter. Idempotent. */
    private fun buildMenuAdapter(suchnaMap: HashMap<String, String>?) {
        jyotish_shastra_suchna = suchnaMap?.get(Utils.JYOTISH_SHASTRA_SUCHNA) ?: jyotish_shastra_suchna
        eSuvidhaMenuAdapter = ESuvidhaMenuAdapter(mContext, menuList, jyotish_shastra_suchna, typesMap, suchnaMap)
        binding.rvMenu.adapter = eSuvidhaMenuAdapter
    }

    private fun setMenuGrid() {
        binding.rvMenu?.layoutManager = GridLayoutManager(mContext, 3)
        binding.rvMenu?.addItemDecoration(GridDividerDecoration(mContext))
        menuList = ArrayList<String>()

        // Keep only the Khate Book option on this screen.
        menuList.add("खाते बुक\n(स्वतःचा हिशोब स्वतः करा)")
    }

    private fun setESuvidhaImage() {
        database.child(Utils.ESUVIDHA_IMAGE_TABLE).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onCancelled(p0: DatabaseError) {
            }

            override fun onDataChange(dataSnapshot: DataSnapshot) {
                val imageDetails = dataSnapshot.getValue(ImageDetails::class.java)
                if (imageDetails != null) {
                    if (!TextUtils.isEmpty(imageDetails.imageDownloadUrl)) {
                        mContext?.let { Glide.with(it).load(imageDetails.imageDownloadUrl).into(binding.imageView1) }
                        binding.imageView1?.visible()
                    }
                }
            }
        })
    }

    private fun fetchDynamicTypes() {
        // Only block the screen with a spinner when we had nothing cached to show.
        if (!renderedFromCache) binding.progressBar?.visible()
        app?.apiRequestHelper?.apiService?.esuvidhaDynamicTypes?.enqueue(object : Callback<ResponseBody> {
            override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                binding.progressBar?.gone()
                if (response.isSuccessful) {
                    val json = response.body()?.string()
                    if (json == null || json == "null") {
                        return
                    }
                    val type: Type = object : TypeToken<HashMap<String, HashMap<String, HashMap<String, String>>>?>() {}.type
                    typesMap = Gson().fromJson(json, type)
                    rahul.jagtap.dmas.utils.EsuvidhaCache.saveDynamicTypesJson(app?.preferences, json)
                }
                fetchSuchna()
            }

            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                binding.progressBar?.gone()
                Log.e("in", "failure")
                fetchSuchna()
            }
        })
    }

    private fun fetchSuchna() {
        if (!renderedFromCache) binding.progressBar?.visible()
        app?.apiRequestHelper?.apiService?.suchna?.enqueue(object : Callback<ResponseBody> {
            override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                binding.progressBar?.gone()
                if (response.isSuccessful) {
                    val json = response.body()?.string()
                    if (json == null || json == "null") {
                        return
                    }
                    val type: Type = object : TypeToken<HashMap<String, String>?>() {}.type
                    val map: HashMap<String, String>? = Gson().fromJson(json, type)
                    rahul.jagtap.dmas.utils.EsuvidhaCache.saveSuchnaJson(app?.preferences, json)
                    // Grouped services: append one tile per subtype-service (with visible subtypes) after the
                    // category tiles. Tapping opens the parent screen where the user picks the subtype.
                    buildMenuAdapter(map)
                }
            }

            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                binding.progressBar?.gone()
                Log.e("in", "failure")
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
}
