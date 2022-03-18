package com.traydcorp.newdio.ui.register

import android.annotation.SuppressLint
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.res.Resources
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import androidx.appcompat.widget.SearchView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.os.ConfigurationCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.traydcorp.newdio.databinding.FragmentRegister3Binding
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import com.google.gson.JsonObject
import com.traydcorp.newdio.MainActivity
import com.traydcorp.newdio.R
import com.traydcorp.newdio.dataModel.CompanyDetail
import com.traydcorp.newdio.dataModel.IndustryDetail
import com.traydcorp.newdio.model.MemberVO
import com.traydcorp.newdio.ui.setting.DialogFragment
import com.traydcorp.newdio.utils.LoadingFragment
import com.traydcorp.newdio.utils.SharedPreference
import com.traydcorp.newdio.utils.retofitService.RetrofitService
import com.traydcorp.newdio.utils.retrofitAPI.RetrofitClient
import retrofit2.Retrofit
import java.util.*
import kotlin.collections.ArrayList


class Register3Fragment : Fragment() {

    private var viewBinding : FragmentRegister3Binding? = null
    private val bind get() = viewBinding!!

    val sharedPreferences = SharedPreference()

    private lateinit var language : String

    private lateinit var retrofit: Retrofit
    private lateinit var supplementService: RetrofitService

    // 관심 기업, 산업 선택 업데이트
    private var companyQuantity : Int = 0
    private var industryQuantity : Int = 0
    private var totalQuantity : Int = 0
    private var selectedIndustry = arrayListOf<String>()
    private var selectedCompany = arrayListOf<String>()

    private var companies : ArrayList<CompanyDetail>? = ArrayList()
    private var industries : ArrayList<IndustryDetail>? = ArrayList()
    private var sharedTotalList : ArrayList<IndustryDetail> = ArrayList()

    private var firstCompanies : ArrayList<CompanyDetail> = ArrayList()
    private var totalCompanyList : ArrayList<CompanyDetail> = ArrayList()
    private var tempCompanyList : ArrayList<CompanyDetail> = ArrayList()

    private lateinit var totalListAdapter : TotalListAdapter

    private lateinit var searchView : ConstraintLayout
    private lateinit var searchBar : SearchView
    private lateinit var topConstraintLayout : ConstraintLayout

    private lateinit var fadeInAnimation : Animation
    private lateinit var fadeOutAnimation : Animation

    private lateinit var searchBtn : ImageView

    private var loadingDialog = LoadingFragment()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

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
    ): View {
        viewBinding = FragmentRegister3Binding.inflate(inflater, container, false)

        // 기업 산업 api call
        getList(supplementService.interestedCompanyList(language))
        loadingDialog.show((context as AppCompatActivity).supportFragmentManager, "loading")


        // 선택된 개수
        val text: String = String.format(resources.getString(R.string.social_add_count), "0")
        bind.quantityText.text = text

        // 기업 검색 view
        searchBtn = requireActivity().findViewById(R.id.searchBtn)
        val cancelBtn : TextView = requireActivity().findViewById(R.id.cancelBtn)
        searchView = requireActivity().findViewById(R.id.searchView)
        searchBar = requireActivity().findViewById(R.id.searchBar)
        topConstraintLayout = requireActivity().findViewById(R.id.topConstraintLayout)

        // 뒤로가기
        val backBtn : ImageView? = requireActivity().findViewById(R.id.backBtn)
        backBtn?.setOnClickListener {
            if (requireActivity().supportFragmentManager.backStackEntryCount != 0) {
                requireActivity().supportFragmentManager.popBackStack()
            }
        }

        // view change 애니메이션
        fadeInAnimation = AlphaAnimation(0.0f, 1.0f)
        fadeInAnimation.duration = 200
        fadeOutAnimation = AlphaAnimation(1.0f, 0.0f)
        fadeOutAnimation.duration = 200

        searchBtn.visibility = View.VISIBLE

        // 검색 버튼
        searchBtn.setOnClickListener {
            topConstraintLayout.visibility = View.INVISIBLE

            searchView.visibility = View.VISIBLE
            searchView.animation = fadeInAnimation
            searchView.animation.start()

            bind.mainView.visibility = View.GONE

            bind.companySearchRecyclerView.visibility = View.VISIBLE
            bind.companySearchRecyclerView.animation = fadeInAnimation
            bind.companySearchRecyclerView.animation.start()
        }

        // 검색 취소 버튼
        cancelBtn.setOnClickListener {
            searchBarOff()
        }

        searchBar.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
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

        // 데이터 담아서 회원가입
        bind.quantityText.setOnClickListener {
            if (companyQuantity >= 3 && industryQuantity >= 1){


                val dialog = DialogFragment()
                val bundle = Bundle()
                bundle.putString("key", "Register")
                dialog.arguments = bundle
                dialog.show(parentFragmentManager, "dialog")

            } else if (companyQuantity < 3) {
                Toast.makeText(context, getString(R.string.social_login_three_more_company), Toast.LENGTH_SHORT).show()

            } else if (industryQuantity < 1) {
                Toast.makeText(context, getString(R.string.social_login_one_more_company), Toast.LENGTH_SHORT).show()
            }
        }


        bind.industryRecyclerView.visibility = View.GONE

        //기업 버튼
        bind.companyBtn.setOnClickListener {
            searchBtn.visibility = View.VISIBLE
            bind.industryRecyclerView.visibility = View.GONE
            bind.companyRecyclerView.visibility = View.VISIBLE
            bind.subtext1.text = getString(R.string.social_login_interested_company)
            bind.subtext2.text = getString(R.string.social_login_three_more_company)
            bind.companyBtn.setBackgroundResource(R.drawable.custom_register_box)
            bind.companyBtn.setTextColor(resources.getColor(R.color.white))
            bind.industryBtn.setBackgroundResource(R.drawable.custom_register_box_grey)
            bind.industryBtn.setTextColor(resources.getColor(R.color.text_light_grey))
        }

        // 산업 버튼
        bind.industryBtn.setOnClickListener {
            searchBtn.visibility = View.GONE
            bind.industryRecyclerView.visibility = View.VISIBLE
            bind.companyRecyclerView.visibility = View.GONE
            bind.subtext1.text = getString(R.string.social_login_interested_industry)
            bind.subtext2.text = getString(R.string.social_login_one_more_company)
            bind.industryBtn.setBackgroundResource(R.drawable.custom_register_box)
            bind.industryBtn.setTextColor(resources.getColor(R.color.white))
            bind.companyBtn.setBackgroundResource(R.drawable.custom_register_box_grey)
            bind.companyBtn.setTextColor(resources.getColor(R.color.text_light_grey))
        }

        // 상단 스크롤 고정
        bind.scrollView.run {
            header = bind.headerView
            stickListener = { _ ->
                Log.d("LOGGER_TAG", "stickListener")
            }
            freeListener = { _ ->
                Log.d("LOGGER_TAG", "freeListener")
            }
        }

        return bind.root
    }

    // 회원가입 api 호출
    fun callRegister() {
        val accessToken = arguments?.getString("accessToken")
        val socialName = arguments?.getString("socialName")
        val birthday = arguments?.getString("birthday")
        val gender = arguments?.getString("gender")

        val memberVO = MemberVO()
        memberVO.access_token = accessToken!!
        memberVO.interested_industries = selectedIndustry
        memberVO.interested_companies = selectedCompany
        memberVO.birthday = birthday!!
        memberVO.gender = gender!!

        register(supplementService.register(memberVO, socialName))
    }

    // 뒤로가기시 검색 아이콘 감추기
    override fun onDetach() {
        super.onDetach()
        searchBtn.visibility = View.GONE
    }

    // 검색 화면 off
    private fun searchBarOff() {
        searchBar.setQuery("", false)
        searchBar.clearFocus()
        searchView.visibility = View.INVISIBLE
        topConstraintLayout.visibility = View.VISIBLE
        topConstraintLayout.animation = fadeInAnimation
        topConstraintLayout.animation.start()
        bind.mainView.visibility = View.VISIBLE
        bind.mainView.animation = fadeInAnimation
        bind.mainView.animation.start()
        bind.companySearchRecyclerView.visibility = View.GONE
    }

    // 산업 리스트 recyclerView
    private fun industryRecyclerView(industryList: ArrayList<IndustryDetail>) {
        // 산업만 recyclerView에 binding
        val industries: ArrayList<IndustryDetail>? = getIndustryList(industryList)

        val adapter = TotalListAdapter(industries, null)
        bind.industryRecyclerView.adapter = adapter
        val gridLayoutManager = GridLayoutManager(context, 3)
        bind.industryRecyclerView.layoutManager = gridLayoutManager

        // 아이템 선택시 개수 업데이트 후 arrayList 추가
        adapter.setItemClickListener(object : TotalListAdapter.OnItemClickListener {
            override fun onClick(v: View, position: Int, industry: String?) {
                // recyclerView 안의 view 접근 - 하트 표시
                val recyclerViewAccess = bind.industryRecyclerView.findContainingItemView(v)
                val selectedItemColor =
                    recyclerViewAccess?.findViewById<ImageView>(R.id.selectedColor)
                val heart = recyclerViewAccess?.findViewById<ImageView>(R.id.heart)

                // 선택된 industry name view에서 가져오기
                val industryNameRcy = recyclerViewAccess?.findViewById<TextView>(R.id.name)
                Log.d("industryNameRcy", industryNameRcy?.text.toString())

                // 선택 -> 하트표시 & 개수 업데이트
                if (!industries!![position].isSelected) {
                    industryQuantity ++
                    totalQuantity ++
                    selectedItemColor!!.visibility = View.VISIBLE
                    heart!!.visibility = View.VISIBLE
                    industries[position].isSelected = true
                    industries[position].index?.let { selectedIndustry.add(it) }
                    Log.d("selected industry index", industries[position].index.toString())
                } else {
                    industryQuantity --
                    totalQuantity --
                    selectedItemColor!!.visibility = View.GONE
                    heart!!.visibility = View.GONE
                    industries[position].isSelected = false
                    selectedIndustry.remove(industries[position].index)
                    Log.d("selected industry index", industries[position].index.toString())
                }
                // 관심 산업 & 기업 총 개수
                val text: String = String.format(resources.getString(R.string.social_add_count), totalQuantity)

                if (totalQuantity > 0){
                    bind.quantityText.text = text
                    bind.quantityText.setTextColor(Color.parseColor("#69DB7C"))
                } else {
                    bind.quantityText.text = text
                    bind.quantityText.setTextColor(Color.parseColor("#BBBBBB"))
                }
                Log.d("selected industry list", selectedIndustry.toString())
            }
        })

    }

    // 기업 리스트 recyclerView
    private fun companyRecyclerView(totalList: ArrayList<IndustryDetail>) {
        sharedTotalList = totalList
        // 산업별로 첫번째 기업 recyclerView에 binding
        firstCompanies = getFirstCompany(totalList)

        totalListAdapter = TotalListAdapter(null, firstCompanies)
        bind.companyRecyclerView.adapter = totalListAdapter
        val gridLayoutManager = GridLayoutManager(context, 3)
        bind.companyRecyclerView.layoutManager = gridLayoutManager


        // 아이템 선택시 개수 업데이트 후 arrayList 추가
        totalListAdapter.setItemClickListener(object : TotalListAdapter.OnItemClickListener {
            override fun onClick(v: View, position: Int, industry: String?) {
                // recyclerView view
                val recyclerViewAccess = bind.companyRecyclerView.findContainingItemView(v)
                val selectedItemColor =
                    recyclerViewAccess?.findViewById<ImageView>(R.id.selectedColor)
                val heart = recyclerViewAccess?.findViewById<ImageView>(R.id.heart)

                // 선택된 item industry index
                var industryIndex = 0
                for (i in sharedTotalList.indices) {
                    if (sharedTotalList[i].industry == industry){
                        industryIndex = i
                    }
                }

                // 새로 그려져야할 company index
                var newItemStartCompanyIndex = 0

                // 선택된 industry안의 company list
                val selectedCompanyList = sharedTotalList[industryIndex].company_list

                // 선택 -> 하트표시 & 개수 업데이트
                if (!firstCompanies[position].isSelected) {
                    companyQuantity ++
                    totalQuantity ++
                    selectedItemColor!!.visibility = View.VISIBLE
                    heart!!.visibility = View.VISIBLE
                    firstCompanies[position].isSelected = true
                    selectedCompany.add(firstCompanies[position].index.toString())

                    // 선택된 산업안의 기업 리스트 for문 - 몇번째 company까지 view가 만들어 졌는지 체크
                    loop@ for (i in selectedCompanyList!!.indices){
                        if (!selectedCompanyList[i].isCreated){
                            newItemStartCompanyIndex = i
                            break@loop
                        }
                    }
                } else {
                    companyQuantity --
                    totalQuantity --
                    selectedItemColor!!.visibility = View.GONE
                    heart!!.visibility = View.GONE
                    firstCompanies[position].isSelected = false
                    selectedCompany.remove(firstCompanies[position].index)
                }
                val text: String = String.format(resources.getString(R.string.social_add_count), totalQuantity)
                if (totalQuantity > 0){ // n개 추가일때 글씨색 변경
                    bind.quantityText.text = text
                    bind.quantityText.setTextColor(Color.parseColor("#69DB7C"))
                } else { // 0개 추가일때 글씨색 변경
                    bind.quantityText.text = text
                    bind.quantityText.setTextColor(Color.parseColor("#BBBBBB"))
                }
                Log.d("selected company list", selectedCompany.toString())

                // 선택된 company가 리스트의 마지막이 아니면
                if (newItemStartCompanyIndex != 0){

                    // 추가할 아이템 개수
                    val newItemQuantity = 3

                    // 추가할 아이템 index start & end
                    val start = newItemStartCompanyIndex
                    val end = newItemStartCompanyIndex + newItemQuantity - 1
                    val companies : ArrayList<CompanyDetail> = ArrayList()
                    var itemCount = 0

                    // 남은 기업 개수가 추가할 아이템 개수보다 많을때
                    if (selectedCompanyList!!.size - newItemStartCompanyIndex > newItemQuantity){
                        for (i in start..end){
                            val companyName = sharedTotalList[industryIndex].company_list!![i].company
                            val companyIndex = sharedTotalList[industryIndex].company_list!![i].index
                            val companyUrl = sharedTotalList[industryIndex].company_list!![i].logo_url
                            val relatedIndustry = sharedTotalList[industryIndex].industry

                            val companyList = CompanyDetail()
                            companyList.company = companyName
                            companyList.index = companyIndex
                            companyList.logo_url = companyUrl
                            companyList.related_industry = relatedIndustry
                            companyList.isCreated = true
                            selectedCompanyList[i].isCreated = true

                            companies.add(companyList)
                            itemCount++
                        }
                    } else {
                        // 남은 기업 개수가 추가할 아이템 개수보다 적을때 마지막 기업까지 bind
                        for (i in start..selectedCompanyList.lastIndex){
                            val companyName = sharedTotalList[industryIndex].company_list!![i].company
                            val companyIndex = sharedTotalList[industryIndex].company_list!![i].index
                            val companyUrl = sharedTotalList[industryIndex].company_list!![i].logo_url
                            val relatedIndustry = sharedTotalList[industryIndex].industry

                            val companyList = CompanyDetail()
                            companyList.company = companyName
                            companyList.index = companyIndex
                            companyList.logo_url = companyUrl
                            companyList.related_industry = relatedIndustry
                            companyList.isCreated = true
                            selectedCompanyList[i].isCreated = true

                            companies.add(companyList)
                            itemCount++
                        }
                    }
                    // insert position : 현재 아이템 position + gap
                    firstCompanies.addAll(position + 1, companies)

                    totalListAdapter.notifyItemChanged(position + 1)
                    totalListAdapter.notifyItemRangeChanged(position + 1, itemCount)
                    totalListAdapter.notifyItemRangeInserted(position + 1, itemCount)
                }

            }
        })

    }

    // 전체 목록에서 첫번째 기업만
    private fun getFirstCompany(companyList: ArrayList<IndustryDetail>) : ArrayList<CompanyDetail> {

        for (i in companyList.indices){

            val companyName = companyList[i].company_list!![0].company
            val index = companyList[i].company_list!![0].index
            val companyUrl = companyList[i].company_list!![0].logo_url
            val relatedIndustry = companyList[i].industry

            val companyDetails = CompanyDetail()
            companyDetails.company = companyName
            companyDetails.index = index
            companyDetails.logo_url = companyUrl
            companyDetails.related_industry = relatedIndustry

            companies!!.add(companyDetails)
        }
        return companies!!
    }

    // 전체 목록에서 기업만
    private fun getCompany(companyList: ArrayList<IndustryDetail>) : ArrayList<CompanyDetail> {

        for (i in companyList.indices){
            for (j in companyList[i].company_list!!.indices) {
                val companyName = companyList[i].company_list!![j].company
                val index = companyList[i].company_list!![j].index
                val companyUrl = companyList[i].company_list!![j].logo_url
                val relatedIndustry = companyList[i].industry

                val companyDetails = CompanyDetail()
                companyDetails.company = companyName
                companyDetails.index = index
                companyDetails.logo_url = companyUrl
                companyDetails.related_industry = relatedIndustry

                totalCompanyList.add(companyDetails)
                tempCompanyList.add(companyDetails)
            }
        }
        totalCompanyList.sortBy { it.company }
        tempCompanyList.sortBy { it.company }
        return totalCompanyList
    }

    // 전체 목록에서 산업만
    private fun getIndustryList(companyList: ArrayList<IndustryDetail>) : ArrayList<IndustryDetail>?{

        for (i in companyList.indices){

            if (!companyList[i].index.equals("in0")){
                val industry = companyList[i].industry
                val index = companyList[i].index
                val industryUrl = companyList[i].logo_url

                val industryDetails = IndustryDetail()
                industryDetails.industry = industry
                industryDetails.index = index
                industryDetails.logo_url = industryUrl

                industries!!.add(industryDetails)
            }
        }
        return industries
    }

    // 전체 리스트 api
    private fun getList(service: Call<List<IndustryDetail>>) {
        Handler(Looper.getMainLooper()).postDelayed({
            service.enqueue(object : Callback<List<IndustryDetail>>{
                override fun onResponse(call: Call<List<IndustryDetail>>, response: Response<List<IndustryDetail>>) {
                    if (response.code() == 200){
                        val result = response.body()!!

                        // 리스트 전체
                        for (i in result.indices){
                            for (j in result[i].company_list!!.indices) {
                                // 첫번째 기업 만들어진 상태 true
                                if (j == 0){
                                    result[i].company_list!![j].isCreated = true
                                }
                            }
                        }

                        // recyclerview로
                        companyRecyclerView(result as ArrayList<IndustryDetail>)
                        industryRecyclerView(result)
                        searchRecyclerView(result)
                        searchBtn.isClickable = true
                        if (loadingDialog.isVisible){
                            loadingDialog.dismiss()
                        }
                        bind.mainView.visibility = View.VISIBLE
                    } else {
                        errorFragment("serverError")
                    }

                }
                override fun onFailure(call: Call<List<IndustryDetail>>, t: Throwable) {
                    errorFragment(null)
                }

            })
        }, 1000)
    }

    // 검색 recyclerView
    private fun searchRecyclerView(totalList: ArrayList<IndustryDetail>) {

        // 전체 기업 리스트
        totalCompanyList = getCompany(totalList)

        // 기업명 순으로 정렬
        tempCompanyList.sortBy { it.company }

        val adapter = SearchListAdapter(tempCompanyList)
        bind.companySearchRecyclerView.adapter = adapter
        val linearLayoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
        bind.companySearchRecyclerView.layoutManager = linearLayoutManager

        adapter.setItemClickListener(object : SearchListAdapter.OnItemClickListener{
            @RequiresApi(Build.VERSION_CODES.N)
            @SuppressLint("NotifyDataSetChanged")
            override fun onClick(v: View, position: Int, index: String) {
                // 검색창 초기화
                searchBar.setQuery("", false)
                searchBar.clearFocus()

                var scrollPosition = 0
                // 이미 view에 그려진 company
                for (i in firstCompanies.indices){
                    if (firstCompanies[i].index == index) {

                        firstCompanies[i].isSelected = true
                        selectedCompany.add(firstCompanies[i].index.toString())
                        updateTotalList(index)
                        scrollPosition = i
                    }
                }

                var selectedIndustry : String? = null
                var selectedCompanyIndex = CompanyDetail()
                // view에 그려지지 않은 company
                for (i in totalCompanyList.indices) {
                    if (totalCompanyList[i].index == index && !totalCompanyList[i].isCreated) {
                        firstCompanies.add(0, getSelectedCompany(index))
                        selectedIndustry = getSelectedCompany(index).related_industry
                        selectedCompanyIndex = getSelectedCompany(index)
                        selectedCompany.add(index)
                    }
                }
                // 만들어질 list에서 삭제
                for (i in sharedTotalList.indices) {
                    if (sharedTotalList[i].industry == selectedIndustry) {
                        sharedTotalList[i].company_list?.removeIf {
                            it.index == selectedCompanyIndex.index
                        }
                    }
                }

                searchBarOff()

                companyQuantity ++
                totalQuantity ++

                val text: String = String.format(resources.getString(R.string.social_add_count), totalQuantity)
                if (totalQuantity > 0){ // n개 추가일때 글씨색 변경
                    bind.quantityText.text = text
                    bind.quantityText.setTextColor(Color.parseColor("#69DB7C"))
                } else { // 0개 추가일때 글씨색 변경
                    bind.quantityText.text = text
                    bind.quantityText.setTextColor(Color.parseColor("#BBBBBB"))
                }
                Log.d("selected company list", selectedCompany.toString())

                totalListAdapter.notifyDataSetChanged()

                // 선택 후 해당 item 위치로 스크롤변경
                if (scrollPosition != 0){
                    Handler(Looper.getMainLooper()).postDelayed({
                        val positionY = bind.companyRecyclerView.getChildAt(scrollPosition).y
                        bind.scrollView.smoothScrollTo(0, positionY.toInt(), 1000)
                    }, 200)
                } else {
                    bind.scrollView.smoothScrollTo(0, 0, 1000)
                }


            }

        })

    }

    // total list 업데이트
    private fun updateTotalList(index: String) {
        for (i in totalCompanyList.indices) {
            if (totalCompanyList[i].index == index) {
                totalCompanyList[i].isSelected = true
                totalCompanyList[i].isCreated = true
            }
        }
    }

    // 선택된 company
    private fun getSelectedCompany(index: String): CompanyDetail {
        val company = CompanyDetail()
        for (i in totalCompanyList.indices) {
            if (totalCompanyList[i].index == index) {
                company.company = totalCompanyList[i].company
                company.index = totalCompanyList[i].index
                company.related_industry = totalCompanyList[i].related_industry
                company.logo_url = totalCompanyList[i].logo_url
                company.isCreated = true
                company.isSelected = true
            }
        }
        return company
    }


    // 회원가입 api call
    fun register(service: Call<JsonObject>) {
        service.enqueue(object : Callback<JsonObject> {
            override fun onResponse(call: Call<JsonObject>, response: Response<JsonObject>) {
                if (response.code() == 200){
                    Log.d("register response body", response.body().toString())
                    val access1 = response.body()?.get("Authorization").toString()
                    val refresh1 = response.body()?.get("Refresh-Token").toString()

                    val access = access1.substring(1, access1.length-1)
                    val refresh = refresh1.substring(1, refresh1.length-1)

                    sharedPreferences.setShared("access_token", access, requireContext())
                    sharedPreferences.setShared("refresh_token", refresh, requireContext())

                    requireActivity().supportFragmentManager.clearBackStack("register")
                    requireActivity().supportFragmentManager.beginTransaction()
                        .replace(R.id.fragmentViewLayout, Register4Fragment()).commit()
                } else {
                    Log.d("Fail response code", response.code().toString())
                    // 잘못된 토큰 팝업 메세지 후 로그인 페이지로 이동
                    val builder = AlertDialog.Builder(requireContext())
                    builder.setMessage(getString(R.string.error_401_content))
                        .setTitle(getString(R.string.error_401_title))
                        .setPositiveButton(getString(R.string.popup_confirm),
                            DialogInterface.OnClickListener { dialog, id ->
                            })
                    val alertDialog = builder.create()
                    alertDialog.show()
                    alertDialog.setOnDismissListener {
                        val intent = Intent(context, MainActivity::class.java)
                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        startActivity(intent)
                    }

                }

            }
            override fun onFailure(call: Call<JsonObject>, t: Throwable) {
                //Toast.makeText(context, "가입 실패", Toast.LENGTH_SHORT).show()
                Log.d("ExceptionTAG", t.message.toString())
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
        if (loadingDialog.isVisible){
            loadingDialog.dismiss()
        }
        bind.errorFragment.visibility = View.VISIBLE
        bind.mainView.visibility = View.INVISIBLE
        searchBtn.isClickable = false
        val errorText = bind.errorFragment.findViewById<TextView>(R.id.errorText)
        val errorText2 = bind.errorFragment.findViewById<TextView>(R.id.errorText2)
        val retryBtn = bind.errorFragment.findViewById<AppCompatButton>(R.id.retryBtn)


        if (serverError == "serverError") {
            errorText.text = getString(R.string.error_server_title)
            errorText2.text = getString(R.string.error_server_subtitle)
        }

        retryBtn.setOnClickListener {
            bind.mainView.visibility = View.INVISIBLE
            getList(supplementService.interestedCompanyList(language))
            loadingDialog.show((context as AppCompatActivity).supportFragmentManager, "loading")
            refreshView()
        }
    }




}