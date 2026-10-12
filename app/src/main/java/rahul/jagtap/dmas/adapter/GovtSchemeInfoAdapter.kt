package rahul.jagtap.dmas.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import rahul.jagtap.dmas.R
import rahul.jagtap.dmas.databinding.ItemGovtSchemeInfoBinding
import rahul.jagtap.dmas.model.GovtSchemeInfo

class GovtSchemeInfoAdapter(
    private val context: Context?,
    private val items: ArrayList<GovtSchemeInfo>,
    private val canManage: Boolean
) : RecyclerView.Adapter<GovtSchemeInfoAdapter.SchemeViewHolder>() {

    interface ItemClickListener {
        fun onDeleteClick(position: Int)
        fun onEditClick(position: Int)
    }

    var itemClickListener: ItemClickListener? = null

    inner class SchemeViewHolder(val binding: ItemGovtSchemeInfoBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SchemeViewHolder {
        val binding = ItemGovtSchemeInfoBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return SchemeViewHolder(binding)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: SchemeViewHolder, position: Int) {
        val item = items[position]
        holder.binding.tvTitle.text = item.title.orEmpty()
        holder.binding.tvSchemeInfo.text = item.schemeInfo.orEmpty()
        holder.binding.tvDateTime.text = item.createdDateTime.orEmpty()
        val color = when (item.titleColor?.lowercase()) {
            "orange" -> R.color.icon_circle_orange
            "yellow" -> R.color.esuvidha_yellow
            "blue" -> R.color.blue
            "pink" -> R.color.esuvidha_pink
            "golden" -> R.color.esuvidha_golden
            else -> null
        }
        if (color != null) {
            runCatching { holder.binding.tvTitle.setBackgroundColor(holder.itemView.context.getColor(color)) }
        }
        holder.itemView.setOnClickListener {
            val p = holder.bindingAdapterPosition
            if (p != RecyclerView.NO_POSITION && canManage) itemClickListener?.onEditClick(p)
        }
        holder.itemView.setOnLongClickListener {
            val p = holder.bindingAdapterPosition
            if (p != RecyclerView.NO_POSITION && canManage) itemClickListener?.onDeleteClick(p)
            canManage
        }
    }
}
