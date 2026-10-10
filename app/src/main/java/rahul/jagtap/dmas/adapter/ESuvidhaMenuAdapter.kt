package rahul.jagtap.dmas.adapter

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import rahul.jagtap.dmas.App
import rahul.jagtap.dmas.R
import rahul.jagtap.dmas.databinding.ItemHomeMenuBinding
import rahul.jagtap.dmas.user.DailyEntriesActivity

/**
 * Minimal menu adapter retained for compatibility with ESuvidhaMenuActivity.
 * ESuvidha, Jyotish, and Govt Scheme entries are intentionally disabled.
 */
class ESuvidhaMenuAdapter(
    private val context: Context?,
    private val itemList: List<String?>? = null,
    @Suppress("UNUSED_PARAMETER") private val jyotish_shastra_suchna: String = "",
    @Suppress("UNUSED_PARAMETER") private val typesMap: HashMap<String, HashMap<String, HashMap<String, String>>>? = null,
    @Suppress("UNUSED_PARAMETER") private val suchnaMap: HashMap<String, String>? = null
) : RecyclerView.Adapter<ESuvidhaMenuAdapter.RecyclerViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerViewHolder {
        val binding = ItemHomeMenuBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return RecyclerViewHolder(binding)
    }

    override fun getItemCount(): Int = itemList?.size ?: 0

    override fun onBindViewHolder(holder: RecyclerViewHolder, position: Int) {
        val title = itemList?.get(position).orEmpty()
        holder.binding.tvMenuTitle.text = title
        if (title == "खाते बुक\n(स्वतःचा हिशोब स्वतः करा)") {
            holder.binding.ivMenu.setImageResource(R.drawable.ic_khate_book)
            holder.itemView.setOnClickListener {
                context?.let { it.startActivity(Intent(it, DailyEntriesActivity::class.java)) }
            }
        } else {
            // Any legacy ESuvidha/Jyotish/Govt Scheme tile is intentionally non-actionable.
            holder.itemView.setOnClickListener(null)
        }
    }

    inner class RecyclerViewHolder @SuppressLint("RestrictedApi") constructor(
        val binding: ItemHomeMenuBinding
    ) : RecyclerView.ViewHolder(binding.root)
}
