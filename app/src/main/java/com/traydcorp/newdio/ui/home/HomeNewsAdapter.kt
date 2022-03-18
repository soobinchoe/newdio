package com.traydcorp.newdio.ui.home

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.traydcorp.newdio.dataModel.HomeNewsList
import com.traydcorp.newdio.databinding.RecyclerHomeTopNewsBinding
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.GranularRoundedCorners
import com.traydcorp.newdio.databinding.RecyclerHomeSubNewsBinding
import kotlin.math.roundToInt
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.traydcorp.newdio.R
import com.traydcorp.newdio.dataModel.NewsDetail
import com.traydcorp.newdio.ui.player.PlayerFragment
import java.util.*

import androidx.fragment.app.FragmentActivity
import com.google.firebase.analytics.FirebaseAnalytics


class HomeNewsAdapter(private val homeTopNewsDetails: ArrayList<NewsDetail>?, private val homeSubNewsDetails: ArrayList<HomeNewsList>?) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var hasInitParentDimensions = false
    private var maxImageWidth: Int = 0
    private var maxImageHeight: Int = 0
    private var maxImageAspectRatio: Float = 1f

    private lateinit var context : Context

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int):
            RecyclerView.ViewHolder {

        // TOP news 화면에 보여지는 크기
        if (!hasInitParentDimensions) {
            maxImageWidth = (parent.width * 0.9f).roundToInt()
            maxImageHeight = parent.height
            maxImageAspectRatio = 0.96f
            hasInitParentDimensions = true
        }

        if (homeTopNewsDetails != null){
            val binding = RecyclerHomeTopNewsBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return HomeNewsListViewHolder(binding)
        } else {
            val binding = RecyclerHomeSubNewsBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return HomeSubNewsListViewHolder(binding)
        }


    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val targetImageWidth: Int = (maxImageWidth * maxImageAspectRatio).roundToInt()

        homeTopNewsDetails?.get(position)?.let {
            holder as HomeNewsListViewHolder
            holder.itemView.layoutParams = RecyclerView.LayoutParams(
                targetImageWidth,
                RecyclerView.LayoutParams.MATCH_PARENT
            )
            holder.bindHomeTopNews(it)}

        homeSubNewsDetails?.get(position)?.let {
            holder as HomeSubNewsListViewHolder
            holder.bindHomeSubNews(it)}
    }

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)
        context = recyclerView.context
    }


    override fun getItemCount(): Int {
        var itemCount = 0
        if (homeTopNewsDetails != null) {
            itemCount = homeTopNewsDetails.size
        } else {
            itemCount = homeSubNewsDetails!!.size
        }
        return itemCount
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

    // TOP news
    inner class HomeNewsListViewHolder(private val binding: RecyclerHomeTopNewsBinding) : RecyclerView.ViewHolder(binding.root) {

        var px1: Float = binding.root.resources.getDimension(R.dimen.top_news_radius)

        fun bindHomeTopNews(homeNewsDetails: NewsDetail) {
            binding.topNewsContents.text= homeNewsDetails.title
            if (homeNewsDetails.image_url != null){
                Glide.with(itemView)
                    .load(homeNewsDetails.image_url)
                    .transform(CenterCrop(), GranularRoundedCorners(0F, px1, px1,px1))
                    .into(binding.topNewsImage)
            }

            binding.topNewsContainer.setOnClickListener {
                itemClickListener.onClick(it, adapterPosition, false)
            }

            binding.topNewsPlayBtn.setOnClickListener {
                itemClickListener.onClick(it, adapterPosition, true)
            }

        }

    }

    // 하위 news adapter로 연결
    inner class HomeSubNewsListViewHolder(private val binding: RecyclerHomeSubNewsBinding) : RecyclerView.ViewHolder(binding.root) {


        fun bindHomeSubNews(homeSubNewsDetails: HomeNewsList) {
            val SubNewsDetails : ArrayList<NewsDetail> =  ArrayList()
            for (i in homeSubNewsDetails.newsList!!.indices){
                val subNewsTitle = homeSubNewsDetails.newsList!![i].title
                val subNewsUrl = homeSubNewsDetails.newsList!![i].image_url
                val subNewsCrawlingData = homeSubNewsDetails.newsList!![i].crawlingdata

                val subNews = NewsDetail()
                subNews.title = subNewsTitle
                subNews.crawlingdata = subNewsCrawlingData
                subNews.image_url = subNewsUrl

                SubNewsDetails.add(subNews)
            }

            val adapter = HomeNewsHorizontalAdapter(SubNewsDetails)
            binding.subNewsDetailRcy.adapter = adapter

            // 선택 시 player로 이동
            adapter.setItemClickListener(object : HomeNewsHorizontalAdapter.OnItemClickListener{
                override fun onClick(v: View, position: Int, playBtn: Boolean) {
                    val currentTime = (Calendar.getInstance().timeInMillis/1000).toInt()
                    val parameters = Bundle().apply {
                        this.putString("screen", "newdio")
                        this.putString("action", "click")
                        this.putInt("time", currentTime)
                        this.putString("type", "topic")
                        this.putString("category", homeSubNewsDetails.category_name)
                        this.putString("keyword", homeSubNewsDetails.index)
                        this.putInt("position", position)
                        this.putInt("max_position", SubNewsDetails.size-1)
                    }
                    val firebaseAnalytics = (context as HomeActivity).firebaseAnalytics
                    firebaseAnalytics.logEvent("newdio", parameters)

                    val crawlingdata = homeSubNewsDetails.newsList!![position].crawlingdata
                    val bundle = Bundle()
                    bundle.putInt("crawlingdata", crawlingdata)
                    bundle.putString("from", "home")

                    if (playBtn) {
                        bundle.putString("playBtn", "true")
                    }

                    ((context as FragmentActivity) as HomeActivity).getPlayer(bundle)
                }
            })

            binding.subNewsTitle.text = homeSubNewsDetails.category

            binding.playAllBtn.setOnClickListener {
                val currentTime = (Calendar.getInstance().timeInMillis/1000).toInt()
                val parameters = Bundle().apply {
                    this.putString("screen", "newdio")
                    this.putString("action", "click")
                    this.putInt("time", currentTime)
                    this.putString("type", "play_all")
                    this.putString("category", homeSubNewsDetails.category_name)
                    this.putString("keyword", homeSubNewsDetails.index)
                }
                val firebaseAnalytics = (context as HomeActivity).firebaseAnalytics
                firebaseAnalytics.logEvent("newdio", parameters)
                itemClickListener.onClick(it, adapterPosition, false)
            }
        }



    }

}



