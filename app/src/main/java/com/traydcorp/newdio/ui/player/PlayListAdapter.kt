package com.traydcorp.newdio.ui.player

import android.content.Context
import android.graphics.Color
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.TranslateAnimation
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.traydcorp.newdio.R
import com.traydcorp.newdio.dataModel.NewsDetail
import com.traydcorp.newdio.databinding.RecyclerPlayerListBinding
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

import android.content.res.ColorStateList
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.traydcorp.newdio.ui.home.HomeActivity


class PlayListAdapter(private val playList: ArrayList<NewsDetail>?) : RecyclerView.Adapter<PlayListAdapter.PlayListViewHolder>(){


    private lateinit var context : Context

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlayListViewHolder {
        val binding = RecyclerPlayerListBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return PlayListViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PlayListViewHolder, position: Int) {
        playList?.get(position)?.let { holder.bindPlayList(it) }
    }

    override fun getItemCount(): Int {
        return playList!!.size
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

    inner class PlayListViewHolder(private val binding: RecyclerPlayerListBinding) : RecyclerView.ViewHolder(binding.root) {


        var px1: Float = binding.root.resources.getDimension(R.dimen.playlist_news_radius)

        fun bindPlayList(relatedNews: NewsDetail){
            val formatterFrom = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSS'Z'", Locale.getDefault())
            val formatterTo = SimpleDateFormat("yyyy.MM.dd", Locale.getDefault())
            formatterFrom.timeZone = TimeZone.getTimeZone("UTC")
            val date = formatterFrom.parse(relatedNews.post_date)

            binding.title.text = relatedNews.title
            binding.newsSite.text = relatedNews.news_site
            binding.newsDate.text = formatterTo.format(date!!)
            if (relatedNews.isPlaying){
                binding.selectedBackground.visibility = View.VISIBLE
                val color = ContextCompat.getColor(context, R.color.light_green)
                val stateList = ColorStateList.valueOf(ContextCompat.getColor(context, R.color.light_green))
                binding.title.setTextColor(color)
                binding.dot.backgroundTintList = stateList
                binding.newsSite.setTextColor(color)
                binding.newsDate.setTextColor(color)
            } else {
                val filename = "readNews"
                val fileContents = relatedNews.crawlingdata.toString()
                val readNews = File(context.filesDir, filename)
                val readNewsList = ArrayList<String>()
                if (readNews.exists()){
                    readNews.readLines().forEach {
                        readNewsList.add(it)
                    }
                }

                if (readNewsList.contains(fileContents)){
                    binding.selectedBackground.visibility = View.GONE
                    val color = ContextCompat.getColor(context, R.color.hint_grey)
                    val stateList = ColorStateList.valueOf(ContextCompat.getColor(context, R.color.hint_grey))
                    binding.dot.backgroundTintList = stateList
                    binding.title.setTextColor(color)
                    binding.newsSite.setTextColor(color)
                    binding.newsDate.setTextColor(color)
                } else {
                    binding.selectedBackground.visibility = View.GONE
                    val color = ContextCompat.getColor(context, R.color.white)
                    val stateList = ColorStateList.valueOf(ContextCompat.getColor(context, R.color.white))
                    binding.dot.backgroundTintList = stateList
                    binding.title.setTextColor(color)
                    binding.newsSite.setTextColor(color)
                    binding.newsDate.setTextColor(color)
                }
            }



            if (relatedNews.image_url != null) {
                Glide.with(itemView)
                    .load(relatedNews.image_url)
                    .transform(CenterCrop(), RoundedCorners(px1.toInt()))
                    .into(binding.newsImage)
            }

            binding.newsContainer.setOnClickListener {
                itemClickListener.onClick(it, adapterPosition, relatedNews.crawlingdata)
            }

            binding.deleteBtn.setOnClickListener {
                itemClickListener.onClick(it, adapterPosition, 0)
            }



        }
    }



}