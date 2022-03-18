package com.traydcorp.newdio.ui.home

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.traydcorp.newdio.R
import com.traydcorp.newdio.dataModel.NewsDetail
import com.traydcorp.newdio.databinding.RecyclerHomeSubNewsDetailBinding
import java.io.File

class HomeNewsHorizontalAdapter(private val homeSubNewsDetalis: ArrayList<NewsDetail>?) : RecyclerView.Adapter<HomeNewsHorizontalAdapter.HomeNewsListViewHolder>() {

    private lateinit var context : Context

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int):
            HomeNewsListViewHolder {
        val binding = RecyclerHomeSubNewsDetailBinding.inflate(LayoutInflater.from(parent.context), parent, false)

        return HomeNewsListViewHolder(binding)
    }

    override fun onBindViewHolder(holder: HomeNewsListViewHolder, position: Int) {
        homeSubNewsDetalis?.get(position).let { holder.bindHomeSubNews(it!!) }
    }

    override fun getItemCount(): Int {
        return  homeSubNewsDetalis!!.size
    }

    // 스크롤 시에 형태 유지
    override fun getItemViewType(position: Int): Int {
        return position
    }

    // 리스너 인터페이스
    interface OnItemClickListener {
        fun onClick(v: View, position: Int, playBtn: Boolean)
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

    inner class HomeNewsListViewHolder(private val binding: RecyclerHomeSubNewsDetailBinding) : RecyclerView.ViewHolder(binding.root) {

        var px1: Float = binding.root.resources.getDimension(R.dimen.sub_news_radius)

        fun bindHomeSubNews(homeSubNewsDetails: NewsDetail) {

            binding.subNewsText.text = homeSubNewsDetails.title
            if (homeSubNewsDetails.image_url != null){
                Glide.with(itemView)
                    .load(homeSubNewsDetails.image_url)
                    .transform(CenterCrop(),RoundedCorners(px1.toInt()))
                    .into(binding.subNewsImage)
            }

            binding.subNewsImage.setOnClickListener {
                itemClickListener.onClick(it, adapterPosition, false)
                binding.subNewsText.setTextColor(ContextCompat.getColor(context, R.color.hint_grey))
            }

            binding.subNewsPlayBtn.setOnClickListener {
                itemClickListener.onClick(it, adapterPosition, true)
                binding.subNewsText.setTextColor(ContextCompat.getColor(context, R.color.hint_grey))
            }

            val filename = "readNews"
            val fileContents = homeSubNewsDetails.crawlingdata.toString()
            val readNews = File(context.filesDir, filename)
            val readNewsList = ArrayList<String>()
            if (readNews.exists()){
                readNews.readLines().forEach {
                    readNewsList.add(it)
                }
            }
            if (readNewsList.contains(fileContents)){
                binding.subNewsText.setTextColor(ContextCompat.getColor(context, R.color.hint_grey))
            }

        }
    }


}
