package com.traydcorp.newdio.ui.discover

import android.content.Context
import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.GranularRoundedCorners
import com.traydcorp.newdio.R
import com.traydcorp.newdio.dataModel.NewsDetail
import com.traydcorp.newdio.databinding.RecyclerDiscoverDetailBinding
import com.traydcorp.newdio.ui.home.HomeActivity
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

class DiscoverDetailAdapter(private val discoverDetailRelatedNewsList: ArrayList<NewsDetail>?) : RecyclerView.Adapter<DiscoverDetailAdapter.DiscoverDetailViewHolder>()  {

    private lateinit var context : Context

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DiscoverDetailViewHolder {
        val binding = RecyclerDiscoverDetailBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return DiscoverDetailViewHolder(binding)
    }

    override fun onBindViewHolder(holder: DiscoverDetailViewHolder, position: Int) {
        discoverDetailRelatedNewsList?.get(position)?.let { holder.bindDetailList(it)}
    }

    override fun getItemCount(): Int {
        return discoverDetailRelatedNewsList!!.size
    }

    // 스크롤 시에 형태 유지
    override fun getItemViewType(position: Int): Int {
        return position
    }

    // 리스너 인터페이스
    interface OnItemClickListener {
        fun onClick(v: View, position: Int, id: Int)
    }

    // setItemClickListener로 설정한 함수 실행
    private lateinit var itemClickListener : OnItemClickListener

    // 외부에서 클릭 시 이벤트 설정
    fun setItemClickListener(onItemClickListener: OnItemClickListener) {
        this.itemClickListener = onItemClickListener
    }

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)
        context = recyclerView.context
    }

    inner class DiscoverDetailViewHolder(private val binding: RecyclerDiscoverDetailBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bindDetailList(it: NewsDetail) {

            val px1: Float = binding.root.resources.getDimension(com.traydcorp.newdio.R.dimen.discover_detail_radius)

            if (it.image_url != null){
                Glide.with(itemView)
                    .load(it.image_url)
                    .transform(CenterCrop(), GranularRoundedCorners(px1, px1, px1,px1))
                    .into(binding.newsImage)
            }
            binding.detailDate.text = ((context as FragmentActivity) as HomeActivity).getDateFormatter("yyyy.MM.dd", it.post_date!!)
            binding.detailNewsTitle.text = it.title
            binding.siteName.text = it.news_site

            val crawlingdata = it.crawlingdata

            val filename = "readNews"
            val fileContents = it.crawlingdata.toString()
            val readNews = File(context.filesDir, filename)
            val readNewsList = ArrayList<String>()
            if (readNews.exists()){
                readNews.readLines().forEach {
                    readNewsList.add(it)
                }
            }

            if (readNewsList.contains(fileContents)){
                changeTextColor()
            }

            binding.discoverDetailCont.setOnClickListener {
                itemClickListener.onClick(it, adapterPosition, crawlingdata)
                changeTextColor()
            }
        }

        fun changeTextColor() {
            val color = ContextCompat.getColor(context, R.color.hint_grey)
            val stateList = ColorStateList.valueOf(ContextCompat.getColor(context, R.color.hint_grey))
            binding.dot.backgroundTintList = stateList
            binding.detailDate.setTextColor(color)
            binding.detailNewsTitle.setTextColor(color)
            binding.siteName.setTextColor(color)
        }

    }
}