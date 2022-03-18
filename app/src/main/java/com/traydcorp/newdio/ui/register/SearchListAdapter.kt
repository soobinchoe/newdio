package com.traydcorp.newdio.ui.register

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.traydcorp.newdio.dataModel.CompanyDetail
import com.traydcorp.newdio.databinding.RecyclerRegisterSearchItemBinding

class SearchListAdapter(val company: ArrayList<CompanyDetail>) : RecyclerView.Adapter<SearchListAdapter.SearchListViewHolder>()  {



    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): SearchListViewHolder {
        val binding = RecyclerRegisterSearchItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)

        return SearchListViewHolder(binding)
    }

    override fun onBindViewHolder(holder: SearchListViewHolder, position: Int) {
        company[position].let { holder.bindCompanies(it) }
    }

    override fun getItemCount(): Int {
        return company.size
    }

    // 스크롤 시에 형태 유지
    override fun getItemViewType(position: Int): Int {
        return position
    }

    // 리스너 인터페이스
    interface OnItemClickListener {
        fun onClick(v: View, position: Int, index: String)
    }

    // setItemClickListener로 설정한 함수 실행
    private lateinit var itemClickListener : OnItemClickListener

    // 외부에서 클릭 시 이벤트 설정
    fun setItemClickListener(onItemClickListener: OnItemClickListener) {
        this.itemClickListener = onItemClickListener
    }

    inner class SearchListViewHolder(private val binding: RecyclerRegisterSearchItemBinding) : RecyclerView.ViewHolder(binding.root) {

        fun bindCompanies(company: CompanyDetail) {

            if (company.logo_url != null) {
                Glide.with(itemView).load(company.logo_url).into(binding.circleImage)
            }

            binding.companyName.text = company.company
            binding.industry.text = company.related_industry

            binding.searchCompanyRcy.setOnClickListener {
                company.index?.let { it1 -> itemClickListener.onClick(it, adapterPosition, it1) }
            }

        }

    }


}