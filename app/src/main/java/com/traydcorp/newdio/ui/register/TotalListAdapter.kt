package com.traydcorp.newdio.ui.register

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.traydcorp.newdio.dataModel.CompanyDetail
import com.traydcorp.newdio.dataModel.IndustryDetail
import com.traydcorp.newdio.databinding.RecyclerInterestedListBinding

class TotalListAdapter(private val industryList: ArrayList<IndustryDetail>?, val company: ArrayList<CompanyDetail>?) : RecyclerView.Adapter<TotalListAdapter.InterestedListViewHolder>() {


    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): InterestedListViewHolder {
        val binding = RecyclerInterestedListBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        // 페이지에 맞춰서 한줄에 세개 보여주기
        binding.root.post {
            binding.root.minWidth = parent.width/3
            binding.root.requestLayout()
        }
        // 관심 표시 숨기기
        binding.selectedColor.visibility = View.GONE
        binding.heart.visibility = View.GONE

        return InterestedListViewHolder(binding)
    }


    override fun onBindViewHolder(holder: InterestedListViewHolder, position: Int) {
        company?.get(position)?.let { holder.bindCompanies(it) }
        industryList?.get(position)?.let { holder.bindIndustries(it) }
    }

    override fun getItemCount(): Int {
        var itemCount = 0
        if (industryList != null) {
            itemCount = industryList.size
        } else {
            itemCount = company!!.size
        }

        return itemCount
    }

    // 스크롤 시에 형태 유지
    override fun getItemViewType(position: Int): Int {
        return position
    }

    // 리스너 인터페이스
    interface OnItemClickListener {
        fun onClick(v: View, position: Int, industry: String?)
    }

    // setItemClickListener로 설정한 함수 실행
    private lateinit var itemClickListener : OnItemClickListener

    // 외부에서 클릭 시 이벤트 설정
    fun setItemClickListener(onItemClickListener: OnItemClickListener) {
        this.itemClickListener = onItemClickListener
    }

    // 뷰홀더
    inner class InterestedListViewHolder(private val binding: RecyclerInterestedListBinding) : RecyclerView.ViewHolder(binding.root) {

        // 기업 bind
        fun bindCompanies(companies: CompanyDetail) {
            binding.name.text = companies.company
            if (companies.logo_url != null) {
                Glide.with(itemView).load(companies.logo_url).into(binding.circleImage)
            }

            // 아이템 업데이트시 view 다시 그려질때 기존에 있던 하트 표시 유지
            if (companies.isSelected){
                interestedSelect()
            } else {
                interestedDeselect()
            }

            // 아이템 선택 click listener
            binding.circleImage.setOnClickListener {
                itemClickListener.onClick(it, adapterPosition, companies.related_industry)
            }

        }

        // 산업 bind
        fun bindIndustries(industries: IndustryDetail) {

            binding.name.text = industries.industry

            if (industries.logo_url != null) {
                Glide.with(itemView).load(industries.logo_url).into(binding.circleImage)
            }

            // 아이템 업데이트시 view 다시 그려질때 기존에 있던 하트 표시 유지
            if (industries.isSelected){
                interestedSelect()
            } else {
                interestedDeselect()
            }

            // 아이템 선택 click listener
            binding.circleImage.setOnClickListener {
                itemClickListener.onClick(it, adapterPosition, null)
            }

        }

        // 선택시 하트표시 + 어둡게
        fun interestedSelect() {
            binding.selectedColor.visibility = View.VISIBLE
            binding.heart.visibility = View.VISIBLE
        }

        // 선택 취소
        fun interestedDeselect() {
            binding.selectedColor.visibility = View.GONE
            binding.heart.visibility = View.GONE
        }

    }


}




