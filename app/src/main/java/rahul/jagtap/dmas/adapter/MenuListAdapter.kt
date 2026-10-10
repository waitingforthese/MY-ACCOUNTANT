        private val BANNER_TITLES = setOf("My Accountant")
    }

    /** Items that should span the full grid width (rendered as banners). */
    fun isFullWidth(title: String?): Boolean = title in BANNER_TITLES

    override fun getItemViewType(position: Int): Int =
        if (isFullWidth(itemList?.get(position))) TYPE_BANNER else TYPE_TILE

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return if (viewType == TYPE_BANNER) {
            BannerViewHolder(ItemHomeBannerBinding.inflate(inflater, parent, false))
        } else {
            TileViewHolder(ItemHomeMenuBinding.inflate(inflater, parent, false))
        }
    }

    override fun getItemCount(): Int = itemList?.size ?: 0

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val title = itemList?.get(position)
        when (holder) {
            is BannerViewHolder -> bindBanner(holder, title)
            is TileViewHolder -> bindTile(holder, title)
        }
        holder.itemView.setOnClickListener { onItemClick(title) }
    }

    private fun bindBanner(holder: BannerViewHolder, title: String?) {
        when (title) {
            "My Accountant" -> {
                holder.binding.bannerIconBg.setBackgroundResource(R.drawable.bg_banner_logo_blue)
                holder.binding.ivBanner.setImageResource(R.drawable.ic_accounting)
                holder.binding.tvBannerTitle.text = "My Accountant"
                holder.binding.tvBannerSubtitle.text = "बिलाचे फोटो पाठवा - आम्ही अकाउंटिंग करू - येथे रिपोर्ट दिसतील"
            }
        }
    }

    private fun bindTile(holder: TileViewHolder, title: String?) {
        holder.binding.tvMenuTitle.text = title
        when (title) {
            "Day Book" -> holder.binding.ivMenu.setImageResource(R.drawable.ic_view_reports)
            "Accounting Services" -> holder.binding.ivMenu.setImageResource(R.drawable.ic_scan_bill)
            "Reports" -> holder.binding.ivMenu.setImageResource(R.drawable.ic_view_reports)
            "Send Report" -> holder.binding.ivMenu.setImageResource(R.drawable.ic_view_reports)
            "Notifications" -> holder.binding.ivMenu.setImageResource(R.drawable.ic_notifications)
            "Daily Entries", "खाते बुक\n(स्वतःचा हिशोब स्वतः करा)" -> holder.binding.ivMenu.setImageResource(R.drawable.ic_khate_book)
            "येथून फी भरावी" -> holder.binding.ivMenu.setImageResource(R.drawable.ic_make_payment)
            "हे अँप कसे वापरावे" -> holder.binding.ivMenu.setImageResource(R.drawable.ic_tutorial)
            "Text Msg" -> holder.binding.ivMenu.setImageResource(R.drawable.ic_view_reports)
            "नियम व अटी" -> holder.binding.ivMenu.setImageResource(R.drawable.ic_terms_conditions)
            "contact us" -> holder.binding.ivMenu.setImageResource(R.drawable.ic_headset)
            "ट्रेनिंग व्हिडिओ" -> holder.binding.ivMenu.setImageResource(R.drawable.ic_training_video)
        }
    }

    private fun onItemClick(title: String?) {
        when (title) {
            "Day Book" -> {
                if (isAdmin || isEmployee) context?.startActivity(Intent(context, DayBookListActivity::class.java))
                else {
                    context?.startActivity(Intent(context, DayBookActivity::class.java).putExtra("userUid", (context as? MainActivity)?.dayBook?.userUid))
                }
            }
            "Accounting Services" -> {
                if (isAdmin || isEmployee) context?.startActivity(Intent(context, BillDatesActivity::class.java))
                else context?.startActivity(Intent(context, AccountingServicesActivity::class.java))
            }
            "Accounting", "My Accountant", "My Accountant\n(Outsourcing)" -> {
                context?.startActivity(Intent(context, AccountingMenuActivity::class.java))
            }
            "Reports" -> {
                if (isAdmin || isEmployee) context?.startActivity(Intent(context, ReportTypesActivity::class.java))
                else context?.startActivity(Intent(context, UserReportTypesActivity::class.java))
            }
            "Notifications" -> {
                context?.startActivity(Intent(context, NotificationsActivity::class.java))
            }
            "Send Report" -> {
                context?.startActivity(Intent(context, SendReportActivity::class.java))
            }
            "Daily Entries", "खाते बुक\n(स्वतःचा हिशोब स्वतः करा)" -> {
                context?.startActivity(Intent(context, DailyEntriesActivity::class.java))
            }
            "येथून फी भरावी" -> {
                context?.startActivity(Intent(context, PaymentDetailsActivity::class.java))
            }
            "हे अँप कसे वापरावे" -> {
                context?.startActivity(Intent(context, TutorialActivity::class.java))
            }
            "ट्रेनिंग व्हिडिओ" -> {
                context?.startActivity(Intent(context, TrainingVideoActivity::class.java))
            }
            "Text Msg" -> {
                context?.startActivity(Intent(context, TextMsgActivity::class.java))
            }
            "All Users" -> {
                context?.startActivity(Intent(context, UsersActivity::class.java))
            }
            "नियम व अटी" -> {
                context?.startActivity(Intent(context, TermsConditionsActivity::class.java))
            }
            "contact us" -> {
                context?.startActivity(Intent(context, ContactActivity::class.java))
            }
        }
    }

    inner class TileViewHolder @SuppressLint("RestrictedApi") constructor(val binding: ItemHomeMenuBinding) : RecyclerView.ViewHolder(binding.root)

    inner class BannerViewHolder @SuppressLint("RestrictedApi") constructor(val binding: ItemHomeBannerBinding) : RecyclerView.ViewHolder(binding.root)
}
