package com.traydcorp.newdio.ui.discover

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Resources
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import androidx.appcompat.widget.SearchView
import androidx.core.os.ConfigurationCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.analytics.FirebaseAnalytics
import com.traydcorp.newdio.R
import com.traydcorp.newdio.dataModel.CompanyDetail
import com.traydcorp.newdio.dataModel.IndustryDetail
import com.traydcorp.newdio.databinding.FragmentDiscoverSearchBinding
import com.traydcorp.newdio.ui.home.HomeActivity
import com.traydcorp.newdio.ui.register.HangulUtils
import com.traydcorp.newdio.ui.register.SearchListAdapter
import com.traydcorp.newdio.utils.LoadingFragment
import com.traydcorp.newdio.utils.SharedPreference
import com.traydcorp.newdio.utils.retofitService.RetrofitService
import com.traydcorp.newdio.utils.retrofitAPI.RetrofitClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import java.util.*
import kotlin.collections.ArrayList


class DiscoverSearchFragment : Fragment() {

    private var viewBinding : FragmentDiscoverSearchBinding? = null
    private val bind get() = viewBinding!!

    val sharedPreferences = SharedPreference()

    private lateinit var language : String

    private lateinit var retrofit: Retrofit
    private lateinit var supplementService: RetrofitService

    private lateinit var callback: OnBackPressedCallback
    private var isLoadingFinish = false

    private var totalCompanyList : ArrayList<CompanyDetail> = ArrayList()
    private var tempCompanyList : ArrayList<CompanyDetail> = ArrayList()

    private var loadingDialog = LoadingFragment()

    private lateinit var firebaseAnalytics: FirebaseAnalytics

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        firebaseAnalytics = (activity as HomeActivity).firebaseAnalytics

        val savedLanguage = sharedPreferences.getShared(requireContext(), "language")
        if (savedLanguage == null){
            language = ConfigurationCompat.getLocales(Resources.getSystem().configuration)[0].language
        } else {
            language = savedLanguage
        }

        retrofit = RetrofitClient.getInstance()
        supplementService = retrofit.create(RetrofitService::class.java)

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        viewBinding = FragmentDiscoverSearchBinding.inflate(inflater, container, false)


        loadingDialog.show((context as AppCompatActivity).supportFragmentManager, "loading")

        getList(supplementService.interestedCompanyList(language))


        // 뒤로가기
        bind.cancelBtn.setOnClickListener {
            if (requireActivity().supportFragmentManager.backStackEntryCount != 0) {
                requireActivity().supportFragmentManager.popBackStack()
            }
        }

        bind.searchBar.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return true
            }

            @SuppressLint("NotifyDataSetChanged")
            override fun onQueryTextChange(newText: String?): Boolean {

                tempCompanyList.clear()

                val searchText = newText!!.lowercase(Locale.getDefault())
                if (searchText.isNotEmpty()) {
                    totalCompanyList.forEach {

                        val hangulUtils = HangulUtils.newInstance()
                        val korean = hangulUtils.getHangulInitialSound(it.company, searchText)

                        if (it.company?.lowercase(Locale.getDefault())?.contains(searchText) == true) {
                            tempCompanyList.add(it)
                        } else if (korean?.indexOf(searchText)!! >= 0){ // 한글 초성 검색 추가
                            tempCompanyList.add(it)
                        }
                    }
                    bind.companySearchRecyclerView.adapter?.notifyDataSetChanged()
                } else {
                    tempCompanyList.clear()
                    tempCompanyList.addAll(totalCompanyList)
                    bind.companySearchRecyclerView.adapter?.notifyDataSetChanged()
                }
                return false
            }

        })

        return bind.root
    }

    // 전체 리스트 api
    private fun getList(service: Call<List<IndustryDetail>>) {
        Handler(Looper.getMainLooper()).postDelayed({
            service.enqueue(object : Callback<List<IndustryDetail>> {
                override fun onResponse(call: Call<List<IndustryDetail>>, response: Response<List<IndustryDetail>>) {
                    if (response.code() == 200){
                        val result = response.body()!!

                        // 리스트 전체
                        for (i in result.indices){
                            for (j in result[i].company_list!!.indices) {
                                // 산업별 기업 이름 & url
                                val companyName = result[i].company_list!![j].company.toString()
                                val index = result[i].company_list!![j].index.toString()
                                val companyUrl = result[i].company_list!![j].logo_url.toString()
                                val relatedIndustry = result[i].industry

                                // i번째 산업의 기업 정보
                                val companyList = CompanyDetail()
                                companyList.company = companyName
                                companyList.index = index
                                companyList.logo_url = companyUrl
                                companyList.related_industry = relatedIndustry

                                // i번째 산업의 전체 기업
                                tempCompanyList.add(companyList)
                                totalCompanyList.add(companyList)
                            }
                        }

                        // recyclerview로
                        // 기업명 순으로 정렬
                        tempCompanyList.sortBy { it.company }
                        totalCompanyList.sortBy { it.company }
                        searchRecyclerView(tempCompanyList)
                    } else {
                        errorFragment("serverError")
                    }

                }
                override fun onFailure(call: Call<List<IndustryDetail>>, t: Throwable) {
                    Log.d("interested list", "실패 : ${t}")
                    errorFragment(null)
                }

            })
        }, 1000)
    }

    // 검색 recyclerView
    private fun searchRecyclerView(totalList: ArrayList<CompanyDetail>) {

        val adapter = SearchListAdapter(totalList)
        bind.companySearchRecyclerView.adapter = adapter
        val linearLayoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
        bind.companySearchRecyclerView.layoutManager = linearLayoutManager

        loadingDialog.dismiss()

        adapter.setItemClickListener(object : SearchListAdapter.OnItemClickListener{
            override fun onClick(v: View, position: Int, index: String) {
                val currentTime = (Calendar.getInstance().timeInMillis/1000).toInt()
                val parameters = Bundle().apply {
                    this.putString("screen", "discover")
                    this.putString("action", "click")
                    this.putInt("time", currentTime)
                    this.putString("type", "search_co")
                    this.putString("co_id", index)
                }
                firebaseAnalytics.logEvent("newdio", parameters)

                val discoverDetailFragment = DiscoverDetailFragment()
                val bundle = Bundle()
                bundle.putString("id", index)
                bundle.putString("from", "companySearch")
                discoverDetailFragment.arguments = bundle

                requireActivity().supportFragmentManager.beginTransaction()
                    .setCustomAnimations(R.animator.discover_detail_slide_right, R.animator.discover_detail_slide_left, R.animator.discover_detail_slide_right, R.animator.discover_detail_slide_left)
                    .addToBackStack(null)
                    .add(R.id.homeView, discoverDetailFragment).commit()

                // 검색창 초기화
                bind.searchBar.setQuery("", false)
                bind.searchBar.clearFocus()
            }

        })

    }

    // 에러페이지에서 새로고침 후 뷰 다시 만들기
    private fun refreshView() {
        requireActivity().supportFragmentManager
            .beginTransaction()
            .detach(this)
            .attach(this)
            .commit()
        bind.errorFragment.visibility = View.GONE
    }

    // 서버 에러 페이지
    private fun errorFragment(serverError : String?) {
        bind.errorFragment.visibility = View.VISIBLE
        bind.companySearchRecyclerView.visibility = View.INVISIBLE
        val errorText = bind.errorFragment.findViewById<TextView>(R.id.errorText)
        val errorText2 = bind.errorFragment.findViewById<TextView>(R.id.errorText2)
        val retryBtn = bind.errorFragment.findViewById<AppCompatButton>(R.id.retryBtn)


        if (serverError == "serverError") {
            errorText.text = getString(R.string.error_server_title)
            errorText2.text = getString(R.string.error_server_subtitle)
        }

        retryBtn.setOnClickListener {
            bind.companySearchRecyclerView.visibility = View.INVISIBLE
            getList(supplementService.interestedCompanyList(language))
            refreshView()
            loadingDialog.show((context as AppCompatActivity).supportFragmentManager, "loading")
        }
    }


}