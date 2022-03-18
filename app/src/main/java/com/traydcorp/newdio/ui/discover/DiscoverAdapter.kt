package com.traydcorp.newdio.ui.discover

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.GranularRoundedCorners
import com.traydcorp.newdio.R
import com.traydcorp.newdio.dataModel.CompanyDetail
import com.traydcorp.newdio.dataModel.IndustryDetail
import com.traydcorp.newdio.databinding.RecyclerDiscoverRankingCompanyBinding
import com.traydcorp.newdio.databinding.RecyclerDiscoverRankingIndustryBinding
import com.traydcorp.newdio.databinding.RecyclerDiscoverRecommendedCompanyBinding
import com.traydcorp.newdio.databinding.RecyclerDiscoverTotalIndustyBinding
import kotlin.math.roundToInt


class DiscoverAdapter(private val recommendedCompany: ArrayList<CompanyDetail>?,
                      private val rankingCompany: ArrayList<CompanyDetail>?,
                      private val rankingIndustry: ArrayList<IndustryDetail>?,
                      private val totalIndustry: List<IndustryDetail>?
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var hasInitParentDimensions = false
    private var maxImageWidth: Int = 0
    private var maxImageHeight: Int = 0
    private var maxImageAspectRatio: Float = 1f

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int):
            RecyclerView.ViewHolder {

        // 실시간 산업 recyclerview 화면에 보여질 width
        if (!hasInitParentDimensions) {
            maxImageWidth = (parent.width * 0.5f).roundToInt()
            maxImageHeight = parent.height
            maxImageAspectRatio = 0.96f
            hasInitParentDimensions = true
        }

        if (recommendedCompany != null) {
            val binding = RecyclerDiscoverRecommendedCompanyBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return RecommendedCompanyViewHolder(binding)
        } else if (rankingCompany != null) {
            val binding = RecyclerDiscoverRankingCompanyBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return RankingCompanyViewHolder(binding)
        } else if (totalIndustry != null){
            val binding = RecyclerDiscoverTotalIndustyBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return TotalIndustryViewHolder(binding)
        } else {
            val binding = RecyclerDiscoverRankingIndustryBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return RankingIndustryViewHolder(binding)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {

        val targetImageWidth: Int = (maxImageWidth * maxImageAspectRatio).roundToInt()

        recommendedCompany?.get(position)?.let {
            holder as RecommendedCompanyViewHolder
            holder.bindRecommendedCompany(it)}

        rankingCompany?.get(position)?.let {
            holder as RankingCompanyViewHolder
            holder.itemView.layoutParams = RecyclerView.LayoutParams(
                RecyclerView.LayoutParams.MATCH_PARENT,
                RecyclerView.LayoutParams.MATCH_PARENT
            )
            holder.bindRankingCompany(it)}

        rankingIndustry?.get(position)?.let {
            holder as RankingIndustryViewHolder
            holder.itemView.layoutParams = RecyclerView.LayoutParams(
                targetImageWidth,
                RecyclerView.LayoutParams.MATCH_PARENT
            )
            holder.bindRankingIndustry(it)}

        totalIndustry?.get(position)?.let {
            holder as TotalIndustryViewHolder
            holder.bindTotalIndustry(it)}


    }

    override fun getItemCount(): Int {
        var itemCount = 0
        if (recommendedCompany != null){
            itemCount = recommendedCompany.size
        } else if (rankingCompany != null){
            itemCount = rankingCompany.size
        } else if (rankingIndustry != null){
            itemCount = rankingIndustry.size
        } else if (totalIndustry != null) {
            itemCount = totalIndustry.size
        }
        return itemCount
    }

    // 스크롤 시에 형태 유지
    override fun getItemViewType(position: Int): Int {
        return position
    }

    // 리스너 인터페이스
    interface OnItemClickListener {
        fun onClick(v: View, position: Int, id: String, playBtn: Boolean?)
    }

    // setItemClickListener로 설정한 함수 실행
    private lateinit var itemClickListener : OnItemClickListener

    // 외부에서 클릭 시 이벤트 설정
    fun setItemClickListener(onItemClickListener: OnItemClickListener) {
        this.itemClickListener = onItemClickListener
    }

    // 추천 기업 view holder
    inner class RecommendedCompanyViewHolder(private val binding: RecyclerDiscoverRecommendedCompanyBinding) : RecyclerView.ViewHolder(binding.root) {

        fun bindRecommendedCompany(it: CompanyDetail) {
            binding.recommendCompanyText.text = it.company
            if (it.logo_url != null){
                Glide.with(itemView)
                    .load(it.logo_url)
                    .into(binding.circleImage)
            }

            val id = it.index

            binding.circleImage.setOnClickListener {
                itemClickListener.onClick(it, adapterPosition, id!!, null)
            }
        }

    }

    // 실시간 기업 view holder
    inner class RankingCompanyViewHolder(private val binding: RecyclerDiscoverRankingCompanyBinding) : RecyclerView.ViewHolder(binding.root) {


        fun bindRankingCompany(it: CompanyDetail) {
            binding.rankingCompanyName.text = it.company
            binding.rankingCompanyIndustry.text = it.related_industry
            binding.rankingCompanyNo.text = it.rank.toString()

            if (it.logo_url != null){
                Glide.with(itemView)
                    .load(it.logo_url)
                    .into(binding.circleImage)
            }

            // 실시간 기업 순위 변화
            if (it.rank_change.equals("new")) {
                binding.rankingNew.visibility = View.VISIBLE
            } else if (it.rank_change!!.toInt() == 0) {
                binding.noChange.visibility = View.VISIBLE
            } else if (it.rank_change!!.toInt() > 0) {
                binding.rankingChange.visibility = View.VISIBLE
                binding.rankingArrow.setBackgroundResource(R.drawable.ic_discover_triangle_up)
                binding.changedRange.text = it.rank_change.toString()
            } else if (it.rank_change!!.toInt() < 0) {
                binding.rankingChange.visibility = View.VISIBLE
                binding.rankingArrow.setBackgroundResource(R.drawable.ic_discover_triangle_down)
                binding.changedRange.text = (-it.rank_change!!.toInt()).toString()
            }

            val id = it.index

            binding.rankingCompanyView.setOnClickListener {
                itemClickListener.onClick(it, adapterPosition, id!!, null)
            }
        }


    }

    // 실시간 산업 view holder
    inner class RankingIndustryViewHolder(private val binding: RecyclerDiscoverRankingIndustryBinding) : RecyclerView.ViewHolder(binding.root) {


        fun bindRankingIndustry(it: IndustryDetail) {
            binding.rankingIndustryName.text = it.industry
            binding.rankingIndustryNo.text = it.rank.toString()

            val id = it.index

            binding.rankingIndustry.setOnClickListener {
                itemClickListener.onClick(it, adapterPosition, id!!, null)
            }

        }


    }

    // 산업별 뉴스 view holder
    inner class TotalIndustryViewHolder(private val binding: RecyclerDiscoverTotalIndustyBinding) : RecyclerView.ViewHolder(binding.root) {

        var px1: Float = binding.root.resources.getDimension(com.traydcorp.newdio.R.dimen.sub_news_radius)
        fun bindTotalIndustry(it: IndustryDetail) {
            binding.totalListText.text = it.industry

            if (it.logo_url != null){
                Glide.with(itemView)
                    .load(it.logo_url)
                    .transform(CenterCrop(), GranularRoundedCorners(px1, px1, px1, px1))
                    .into(binding.totalListImage)
            }

            val id = it.index

            binding.totalListImage.setOnClickListener {
                itemClickListener.onClick(it, adapterPosition, id!!, null)
            }

            binding.totalListBtn.setOnClickListener {
                itemClickListener.onClick(it, adapterPosition, id!!, true)
            }
        }

    }
}



