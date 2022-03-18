package com.traydcorp.newdio.ui.search

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.traydcorp.newdio.databinding.RecyclerSearchRecentBinding

class SearchAdapter(private val recentSearchList: ArrayList<String>) : RecyclerView.Adapter<SearchAdapter.SearchViewHolder>(){

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SearchViewHolder {
        val binding = RecyclerSearchRecentBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return SearchViewHolder(binding)
    }

    override fun onBindViewHolder(holder: SearchViewHolder, position: Int) {
        recentSearchList[position].let { holder.bindRecentSearchList(it) }
    }

    override fun getItemCount(): Int {
        return recentSearchList.size
    }

    // 스크롤 시에 형태 유지
    override fun getItemViewType(position: Int): Int {
        return position
    }

    // 리스너 인터페이스
    interface OnItemClickListener {
        fun onClick(v: View, position: Int, search: String?)
    }

    // setItemClickListener로 설정한 함수 실행
    private lateinit var itemClickListener : OnItemClickListener

    // 외부에서 클릭 시 이벤트 설정
    fun setItemClickListener(onItemClickListener: OnItemClickListener) {
        this.itemClickListener = onItemClickListener
    }

    inner class SearchViewHolder(private val binding: RecyclerSearchRecentBinding) : RecyclerView.ViewHolder(binding.root) {

        fun bindRecentSearchList(recentSearch: String) {
            binding.searchWord.text = recentSearch
            binding.deleteBtn.setOnClickListener {
                Log.d("delete", "call $recentSearch")
                itemClickListener.onClick(it, adapterPosition, recentSearch)
            }

            binding.searchWord.setOnClickListener {
                itemClickListener.onClick(it, adapterPosition, null)
            }
        }



    }
}