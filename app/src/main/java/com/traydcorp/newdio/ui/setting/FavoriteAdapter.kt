package com.traydcorp.newdio.ui.setting

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.GranularRoundedCorners
import com.traydcorp.newdio.dataModel.CompanyDetail
import com.traydcorp.newdio.dataModel.IndustryDetail
import com.traydcorp.newdio.dataModel.NewsDetail
import com.traydcorp.newdio.databinding.RecyclerSettingFavoriteBinding
import com.traydcorp.newdio.databinding.RecyclerSettingFavoriteNewsBinding
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

class FavoriteAdapter(private val favoriteCompanies: ArrayList<CompanyDetail>?,
                      private val favoriteIndustries: ArrayList<IndustryDetail>?,
                      private val favoriteNews: ArrayList<NewsDetail>?)
    : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {

        // 기업 산업
        if (favoriteCompanies != null || favoriteIndustries != null) {
            val binding = RecyclerSettingFavoriteBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return FavoriteViewHolder(binding)
        } else { // 뉴스
            val binding = RecyclerSettingFavoriteNewsBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return FavoriteNewsViewHolder(binding)
        }

    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {

        favoriteCompanies?.get(position)?.let {
            holder as FavoriteViewHolder
            holder.bindFavoriteCompany(it)}

        favoriteIndustries?.get(position)?.let {
            holder as FavoriteViewHolder
            holder.bindFavoriteIndustry(it)}

        favoriteNews?.get(position)?.let {
            holder as FavoriteNewsViewHolder
            holder.bindFavoriteNews(it)}

    }

    override fun getItemCount(): Int {
        var itemCount = 0
        if (favoriteCompanies != null) {
            itemCount = favoriteCompanies.size
        }
        if (favoriteIndustries != null) {
            itemCount = favoriteIndustries.size
        }
        if (favoriteNews != null) {
            itemCount = favoriteNews.size
        }
        return itemCount
    }

    // 스크롤 시에 형태 유지
    override fun getItemViewType(position: Int): Int {
        return position
    }

    // 리스너 인터페이스
    interface OnItemClickListener {
        fun onClick(v: View, position: Int, id: String?, heart: String?, industry: String?)
    }

    // setItemClickListener로 설정한 함수 실행
    private lateinit var itemClickListener : OnItemClickListener

    // 외부에서 클릭 시 이벤트 설정
    fun setItemClickListener(onItemClickListener: OnItemClickListener) {
        this.itemClickListener = onItemClickListener
    }

    // 기업 산업 viewHolder
    inner class FavoriteViewHolder(private val binding: RecyclerSettingFavoriteBinding) : RecyclerView.ViewHolder(binding.root) {
        // 기업
        fun bindFavoriteCompany(company: CompanyDetail) {
            binding.name.text = company.company
            if (company.logo_url != null){
                Glide.with(itemView)
                    .load(company.logo_url)
                    .into(binding.circleImage)
            }

            val id = company.index
            binding.favoriteCon.setOnClickListener {
                itemClickListener.onClick(it, adapterPosition, id!!, null, null)
            }

            binding.heartBtn.setOnClickListener {
                itemClickListener.onClick(it, adapterPosition, id!!, "heart", null)
                favoriteCompanies?.remove(company)
                notifyItemRemoved(adapterPosition)
            }

        }

        // 산업
        fun bindFavoriteIndustry(industry: IndustryDetail) {
            binding.name.text = industry.industry
            if (industry.logo_url != null){
                Glide.with(itemView)
                    .load(industry.logo_url)
                    .into(binding.circleImage)
            }

            val id = industry.index
            binding.favoriteCon.setOnClickListener {
                itemClickListener.onClick(it, adapterPosition, id!!, null, "industry")
            }

            binding.heartBtn.setOnClickListener {
                itemClickListener.onClick(it, adapterPosition, id!!, "heart", "industry")
                favoriteIndustries?.remove(industry)
                notifyItemRemoved(adapterPosition)
            }

        }
    }

    // 뉴스 viewHolder
    inner class FavoriteNewsViewHolder(private val binding: RecyclerSettingFavoriteNewsBinding) : RecyclerView.ViewHolder(binding.root) {

        fun bindFavoriteNews(likedNews: NewsDetail) {
            val px1: Float = binding.root.resources.getDimension(com.traydcorp.newdio.R.dimen.discover_detail_radius)

            if (likedNews.image_url != null){
                Glide.with(itemView)
                    .load(likedNews.image_url)
                    .transform(CenterCrop(), GranularRoundedCorners(px1, px1, px1,px1))
                    .into(binding.newsImage)
            }
            val formatterFrom = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSS'Z'", Locale.getDefault())
            val formatterTo = SimpleDateFormat("yyyy.MM.dd", Locale.getDefault())
            formatterFrom.timeZone = TimeZone.getTimeZone("UTC")
            val date = formatterFrom.parse(likedNews.post_date!!)
            binding.detailDate.text = formatterTo.format(date!!)
            binding.detailNewsTitle.text = likedNews.title
            binding.siteName.text = likedNews.news_site

            val id = likedNews.crawlingdata.toString()
            binding.favoriteNewsCont.setOnClickListener {
                itemClickListener.onClick(it, adapterPosition, id, null, null)
            }

            binding.heartBtn.setOnClickListener {
                itemClickListener.onClick(it, adapterPosition, id, "heart", null)
                favoriteNews?.remove(likedNews)
                notifyItemRemoved(adapterPosition)
            }
        }
    }

}