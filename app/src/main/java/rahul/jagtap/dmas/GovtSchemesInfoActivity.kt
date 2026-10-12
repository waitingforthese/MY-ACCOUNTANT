package rahul.jagtap.dmas

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.WindowManager
import androidx.activity.result.contract.ActivityResultContracts
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import rahul.jagtap.dmas.adapter.GovtSchemeInfoAdapter
import rahul.jagtap.dmas.databinding.ActivityViewBillsBinding
import rahul.jagtap.dmas.extensions.toast
import rahul.jagtap.dmas.extensions.gone
import rahul.jagtap.dmas.extensions.visible
import rahul.jagtap.dmas.model.GovtSchemeInfo
import rahul.jagtap.dmas.utils.Utils
import java.text.DateFormat
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Collections
import java.util.Locale


class GovtSchemesInfoActivity : BaseActivity() {
        private val TAG = "GovtSchemesInfoActivity"
//    private var uid: String? = ""
    var list = ArrayList<GovtSchemeInfo>()
    var adapter: GovtSchemeInfoAdapter? = null

    private lateinit var binding: ActivityViewBillsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityViewBillsBinding.inflate(layoutInflater)
        window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbarLayout.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowTitleEnabled(false)
        binding.toolbarLayout.toolbarTitle?.text = "नवनवीन माहिती"

//        uid = app?.preferences?.loggedInUser?.uid
        adapter = GovtSchemeInfoAdapter(mContext, list, isAdminOrEmployee())

        binding.recyclerView?.layoutManager = LinearLayoutManager(mContext, RecyclerView.VERTICAL, false)
        binding.recyclerView?.adapter = adapter
        adapter?.itemClickListener = object : GovtSchemeInfoAdapter.ItemClickListener {
            override fun onDeleteClick(position: Int) {
                Utils.showDialog(mContext, "Are you sure want to delete the record?", true) { dialog, which ->
                    run {
                        dialog.dismiss()
                        val selItem = list[position]
                        list?.removeAt(position) // Delete DB Record
                        selItem.pushKey?.let {
                            database.child(Utils.GOVT_SCHEMES_TABLE).child(selItem.pushKey!!).removeValue()
                        }
                        notifyAdapter()
                        toast("Record deleted successfully")
                    }
                }
            }

            override fun onEditClick(position: Int) {
                val selItem = list[position]
                val intent = Intent(mContext, AddGovtSchemeInfoActivity::class.java)
                intent.putExtra("govtSchemeInfo", selItem)
                resultLauncher.launch(intent)
            }
        }
        setEntriesData()
    }

    private fun setEntriesData() {
        binding.progressBar?.visible()

        database.child(Utils.GOVT_SCHEMES_TABLE)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    binding.progressBar?.gone()
                    list.clear()

                    for (child in snapshot.children) {
                        val item = child.getValue(GovtSchemeInfo::class.java)
                        if (item != null) {
                            // Keep the Firebase child key for edit/delete operations.
                            if (item.pushKey.isNullOrBlank()) {
                                item.pushKey = child.key
                            }
                            list.add(item)
                        }
                    }

                    try {
                        val df: DateFormat = SimpleDateFormat("dd-MM-yyyy HH:mm:ss", Locale.ENGLISH)
                        Collections.sort(list, Comparator { o1, o2 ->
                            val d1 = o1.createdDateTime
                            val d2 = o2.createdDateTime
                            if (d1.isNullOrEmpty() || d2.isNullOrEmpty()) {
                                return@Comparator 0
                            }
                            try {
                                df.parse(d2)?.compareTo(df.parse(d1)) ?: 0
                            } catch (e: Exception) {
                                Log.e(TAG, "Could not sort scheme dates: $d2 || $d1", e)
                                0
                            }
                        })
                    } catch (e: Exception) {
                        Log.e(TAG, "Could not sort scheme records", e)
                    }

                    notifyAdapter()
                }

                override fun onCancelled(error: DatabaseError) {
                    binding.progressBar?.gone()
                    Log.e(TAG, "Failed to load government scheme information: ${error.message}", error.toException())
                    list.clear()
                    notifyAdapter()
                    toast("माहिती लोड झाली नाही. कृपया पुन्हा प्रयत्न करा.")
                }
            })
    }

    private fun notifyAdapter() {
        if (list != null && list.size > 0) {
            adapter?.notifyDataSetChanged()
            binding.recyclerView?.visible()
            binding.tvError?.gone()
        } else {
            binding.recyclerView?.gone()
            binding.tvError?.visible()
        }
    }

    private fun isAdminOrEmployee(): Boolean = app?.preferences?.loggedInUser?.isAdmin == "1" || app?.preferences?.loggedInUser?.userType == "2"

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_add_govt_scheme, menu)
        val item = menu?.findItem(R.id.action_add)
        item?.isVisible = isAdminOrEmployee()
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_add -> {
                val intent = Intent(mContext, AddGovtSchemeInfoActivity::class.java)
                resultLauncher.launch(intent)
                return true
            }

            android.R.id.home -> {
                Utils.hideSoftKeyboard(this)
                finish()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    var resultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            // There are no request codes
            setEntriesData()
        }
    }

    companion object {

    }
}
