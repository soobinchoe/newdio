package com.traydcorp.newdio.ui.live

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.traydcorp.newdio.R
import com.traydcorp.newdio.databinding.RecyclerLiveListBinding
import android.text.format.DateUtils
import android.util.Log
import androidx.annotation.Dimension
import com.traydcorp.newdio.dataModel.NewsDetail
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList


class LiveListAdapter(private val liveList: ArrayList<NewsDetail>?, textSize : String) : RecyclerView.Adapter<LiveListAdapter.LiveListViewHolder>() {

    private val textSize = textSize

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LiveListViewHolder {
        val binding = RecyclerLiveListBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return LiveListViewHolder(binding)
    }

    override fun onBindViewHolder(holder: LiveListViewHolder, position: Int) {
        liveList?.get(position)?.let { holder.bindLiveList(it) }
    }

    override fun getItemCount(): Int {
        return liveList!!.size
    }

    // 스크롤 시에 형태 유지
    override fun getItemViewType(position: Int): Int {
        return position
    }

    // 리스너 인터페이스
    interface OnItemClickListener {
        fun onClick(v: View, position: Int, index: String?)
    }

    // setItemClickListener로 설정한 함수 실행
    private lateinit var itemClickListener : OnItemClickListener

    // 외부에서 클릭 시 이벤트 설정
    fun setItemClickListener(onItemClickListener: OnItemClickListener) {
        this.itemClickListener = onItemClickListener
    }

    inner class LiveListViewHolder(private val binding: RecyclerLiveListBinding) : RecyclerView.ViewHolder(binding.root) {
        @SuppressLint("SetTextI18n")
        fun bindLiveList(result: NewsDetail) {
            binding.liveCategory.text = result.related_category
            binding.liveContent.text = result.title


            if (result.related_company_list != null){
                if (result.related_company_list!!.size == 1){
                    binding.companyName.text = "#"+ result.related_company_list!![0].company
                } else if (result.related_company_list!!.size == 2) {
                    binding.companyName.text = "#"+result.related_company_list!![0].company
                    binding.companyName2.text = "#"+result.related_company_list!![1].company
                } else if (result.related_company_list!!.size == 3) {
                    binding.companyName.text = "#"+result.related_company_list!![0].company
                    binding.companyName2.text = "#"+result.related_company_list!![1].company
                    binding.companyName3.text = "#"+result.related_company_list!![2].company
                }
            }

            // 감정 분석 아이콘 설정
            val sentiment = result.text_sentiment
            if (sentiment!! > 0.6 && sentiment <= 1.0){
                binding.sentiment.setImageResource(R.drawable.ic_live_green)
            } else if (sentiment in 0.4..0.6) {
                binding.sentiment.setImageResource(R.drawable.ic_live_yellow)
            } else {
                binding.sentiment.setImageResource(R.drawable.ic_live_red)
            }
            val formatter = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSS'Z'", Locale.getDefault())
            formatter.timeZone = TimeZone.getTimeZone("UTC")

            //
            try {
                val now = System.currentTimeMillis()
                val ago = DateUtils.getRelativeTimeSpanString(formatter.parse(result.post_date).time, now, DateUtils.SECOND_IN_MILLIS)
                binding.liveTime.text = ago
            } catch (e: Exception) {
                e.printStackTrace()
            }


            binding.liveContentView.setOnClickListener {
                itemClickListener.onClick(it, adapterPosition, null)
            }

            binding.companyName.setOnClickListener {
                itemClickListener.onClick(it, adapterPosition, result.related_company_list!![0].index)
            }

            binding.companyName2.setOnClickListener {
                itemClickListener.onClick(it, adapterPosition, result.related_company_list!![1].index)
            }

            binding.companyName3.setOnClickListener {
                itemClickListener.onClick(it, adapterPosition, result.related_company_list!![2].index)
            }

            // 라이브 내용 텍스트 size
            when (textSize) {
                "small" ->  13F
                "original" ->  15F
                "large" ->  18F
                else -> null
            }?.let {
                binding.liveContent.setTextSize(Dimension.SP, it)
            }

        }

    }
}