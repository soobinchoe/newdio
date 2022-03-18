package com.traydcorp.newdio.ui.discover

import android.content.res.Resources
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.os.ConfigurationCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.PagerSnapHelper
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.analytics.FirebaseAnalytics
import com.traydcorp.newdio.R
import com.traydcorp.newdio.dataModel.CompanyDetail
import com.traydcorp.newdio.dataModel.IndustryDetail
import com.traydcorp.newdio.dataModel.NewsDetail
import com.traydcorp.newdio.dataModel.RankingList
import com.traydcorp.newdio.databinding.FragmentDiscoverBinding
import com.traydcorp.newdio.ui.home.HomeActivity
import com.traydcorp.newdio.ui.player.PlayerFragment
import com.traydcorp.newdio.utils.LoadingFragment
import com.traydcorp.newdio.utils.SharedPreference
import com.traydcorp.newdio.utils.retofitService.RetrofitService
import com.traydcorp.newdio.utils.retrofitAPI.RetrofitClient
import me.relex.circleindicator.CircleIndicator2
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import java.util.*
import kotlin.collections.ArrayList


class DiscoverFragment : Fragment() {

    private var viewBinding : FragmentDiscoverBinding? = null
    private val bind get() = viewBinding!!

    private val sharedPreferences = SharedPreference()

    private lateinit var adapter : DiscoverAdapter

    private lateinit var retrofit: Retrofit
    private lateinit var supplementService: RetrofitService

    private lateinit var language : String

    private var bundle = Bundle()

    private var isAllSuccess = false
    private var isDailyInfoBtnOn = false

    private val fadeInAnimation : Animation = AlphaAnimation(0.0f, 1.0f)
    private val fadeOutAnimation : Animation = AlphaAnimation(1.0f, 0.0f)

    private lateinit var industryIndex : String

    private var loadingDialog = LoadingFragment()

    private lateinit var firebaseAnalytics: FirebaseAnalytics

    private var parameters = Bundle()
    private var currentTime : Int = 0

    private var isRefresh = false


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        firebaseAnalytics = (activity as HomeActivity).firebaseAnalytics

        language = (activity as HomeActivity).getLanguagePreference()

        retrofit = RetrofitClient.getInstance()
        supplementService = retrofit.create(RetrofitService::class.java)

    }



    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        viewBinding = FragmentDiscoverBinding.inflate(inflater, container, false)

        bind.discoverToolbarText.text = getString(R.string.menu_discover)

        loadingDialog.show((context as AppCompatActivity).supportFragmentManager, "loading")

        Handler(Looper.getMainLooper()).post {
            getDiscover()
        }

        return bind.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 검색 버튼
        bind.searchBtn.setOnClickListener {
            removeInfo()

            requireActivity().supportFragmentManager.beginTransaction()
                .addToBackStack(null)
                .add(R.id.homeView, DiscoverSearchFragment(), "discoverSearch").commit()
        }

        // 새로 고침
        bind.swipeRefresh.setOnRefreshListener {
            parameters = Bundle().apply {
                this.putString("action", "drag")
                this.putString("type", "reload")
            }
            sendFirebaseLog(parameters)
            isRefresh = true

            bind.discoverContents.visibility = View.VISIBLE
            getDiscover()
            bind.swipeRefresh.isRefreshing = false

            bind.discoverContents.visibility = View.VISIBLE
        }

        // 실시간 기업 순위 indicator
        val indicator : CircleIndicator2 = bind.discoverIndicator
        bind.companyRankingRcy.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                if (bind.companyRankingRcy.canScrollHorizontally(-1)){
                    indicator.animatePageSelected(1)
                }
            }
        })

        // 일간 기업 순위 indicator
        val indicatorDaily : CircleIndicator2 = bind.dailyDiscoverIndicator
        bind.dailyCompanyRankingRcy.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                if (bind.dailyCompanyRankingRcy.canScrollHorizontally(-1)){
                    indicatorDaily.animatePageSelected(1)
                }
            }
        })

        val gridLayoutManager = GridLayoutManager(context, 5, GridLayoutManager.HORIZONTAL, false)
        bind.industryRankingRcy.layoutManager = gridLayoutManager


        // textView 애니메이션
        fadeInAnimation.duration = 150
        fadeOutAnimation.duration = 150

        // info text
        val dailyInfoText = viewBinding!!.infoDailyDialog.findViewById<TextView>(R.id.liveInfoText)
        val realTimeCompanyInfoText = viewBinding!!.infoRealTimeCompanyDialog.findViewById<TextView>(R.id.liveInfoText)
        val realTimeIndustryInfoText = viewBinding!!.infoIndustryDialog.findViewById<TextView>(R.id.liveInfoText)


        // 일간 기업 순위 info 버튼
        bind.infoBtnDailyCompany.setOnClickListener {
            if (!isDailyInfoBtnOn) {
                dailyInfoText.text = getString(R.string.discover_daily_rank_company_description)
                setInfoBtnOn(bind.infoBtnDailyCompany, bind.infoDailyDialog)
            } else {
                setInfoBtnOff(bind.infoBtnDailyCompany, bind.infoDailyDialog)
            }
        }

        // 실시간 핫이슈 기업 info 버튼
        bind.infoBtnRealTimeCompany.setOnClickListener {
            if (!isDailyInfoBtnOn) {
                realTimeCompanyInfoText.text = getString(R.string.discover_hot_rank_company_description)
                setInfoBtnOn(bind.infoBtnRealTimeCompany, bind.infoRealTimeCompanyDialog)
            } else {
                setInfoBtnOff(bind.infoBtnRealTimeCompany, bind.infoRealTimeCompanyDialog)
            }
        }

        // 실시간 핫이슈 산업 info 버튼
        bind.infoBtnDailyindustry.setOnClickListener {
            if (!isDailyInfoBtnOn) {
                realTimeIndustryInfoText.text = getString(R.string.discover_hot_rank_industry_description)
                setInfoBtnOn(bind.infoBtnDailyindustry, bind.infoIndustryDialog)
            } else {
                setInfoBtnOff(bind.infoBtnDailyindustry, bind.infoIndustryDialog)
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            bind.homeScrollView.setOnScrollChangeListener { v, scrollX, scrollY, oldScrollX, oldScrollY ->
                removeInfo()
            }
        }
    }

    private fun setInfoBtnOn(infoBtn: ImageView, dialog: ConstraintLayout) {
        infoBtn.setColorFilter(ContextCompat.getColor(requireContext(), R.color.light_green))
        dialog.visibility = View.VISIBLE
        dialog.animation = fadeInAnimation
        dialog.animation.start()
        isDailyInfoBtnOn = true
    }

    private fun setInfoBtnOff(infoBtn: ImageView, dialog: ConstraintLayout) {
        infoBtn.setColorFilter(ContextCompat.getColor(requireContext(), R.color.text_light_grey))
        dialog.visibility = View.GONE
        dialog.animation = fadeOutAnimation
        dialog.animation.start()
        isDailyInfoBtnOn = false
    }

    private fun getDiscover() {
        getRecommendedCompany(supplementService.getRecommendedCompanyData(language))
        getTotalIndustry(supplementService.getTotalIndustryData(language))
        getRankingList(supplementService.getRankingListData(language))
        getDailyCompanyRankingList(supplementService.getDailyCompanyRankingData(language))
    }

    fun removeInfo() {
        // textView 애니메이션
        if (bind.infoDailyDialog.isShown) {
            bind.infoBtnDailyCompany.setColorFilter(ContextCompat.getColor(requireContext(), R.color.white))
            setInfoBtnOff(bind.infoBtnDailyCompany, bind.infoDailyDialog)
        } else if (bind.infoRealTimeCompanyDialog.isShown) {
            bind.infoBtnRealTimeCompany.setColorFilter(ContextCompat.getColor(requireContext(), R.color.white))
            setInfoBtnOff(bind.infoBtnRealTimeCompany, bind.infoRealTimeCompanyDialog)
        } else if (bind.infoIndustryDialog.isShown) {
            bind.infoBtnDailyindustry.setColorFilter(ContextCompat.getColor(requireContext(), R.color.white))
            setInfoBtnOff(bind.infoBtnDailyindustry, bind.infoIndustryDialog)
        }
    }


    // 추천 기업
    private fun getRecommendedCompany(service: Call<List<CompanyDetail>>) {

        Handler(Looper.getMainLooper()).postDelayed({
            service.enqueue(object : Callback<List<CompanyDetail>> {
                override fun onResponse(
                    call: Call<List<CompanyDetail>>,
                    response: Response<List<CompanyDetail>>
                ) {

                    if (response.code() == 200){
                        val result = response.body() as ArrayList<CompanyDetail>
                        recommendedCompanyRecyclerView(result)
                        isAllSuccess = true
                    } else {
                        errorFragment("serverError")
                        isAllSuccess = false
                    }
                }

                override fun onFailure(call: Call<List<CompanyDetail>>, t: Throwable) {
                    errorFragment(null)
                    isAllSuccess = false
                }

            })
        }, 1000)

    }

    // 추천 기업 RecyclerView
    private fun recommendedCompanyRecyclerView(result: java.util.ArrayList<CompanyDetail>) {

        val layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)

        adapter = DiscoverAdapter(result, null, null, null)
        bind.recommendCompanyRcy.adapter = adapter
        bind.recommendCompanyRcy.layoutManager = layoutManager

        adapter.setItemClickListener(object : DiscoverAdapter.OnItemClickListener{
            override fun onClick(v: View, position: Int, id: String, playBtn: Boolean?) {
                parameters = Bundle().apply {
                    this.putString("action", "click")
                    this.putString("type", "recommend_co")
                    this.putString("co_id", id)
                    this.putInt("position", position)
                    this.putInt("max_position", result.size-1)
                }
                sendFirebaseLog(parameters)

                // 기업 상세정보로 이동
                removeInfo()
                bundle.putString("id", id)
                (activity as HomeActivity).getDiscoverDetail(bundle)
            }
        })

    }

    // 일간 기업 순위
    private fun getDailyCompanyRankingList(service: Call<RankingList>) {

        Handler(Looper.getMainLooper()).postDelayed({
            service.enqueue(object : Callback<RankingList> {
                override fun onResponse(
                    call: Call<RankingList>,
                    response: Response<RankingList>
                ) {

                    if (response.code() == 200){
                        val result = response.body()
                        val asOfTimeText = (activity as HomeActivity).getDateFormatter("HH:mm", result?.created!!)
                        val text: String = String.format(resources.getString(R.string.search_criteria), asOfTimeText)
                        bind.asOfTimeDailyCompany.text = text

                        dailyRankingCompanyRecyclerView(result.company_ranking)
                        isAllSuccess = true
                    } else {
                        errorFragment("serverError")
                        isAllSuccess = false
                    }
                }

                override fun onFailure(call: Call<RankingList>, t: Throwable) {
                    errorFragment(null)
                    isAllSuccess = false
                }

            })
        }, 1000)

    }

    // 일간 기업 순위 recyclerView
    private fun dailyRankingCompanyRecyclerView(companyList: ArrayList<CompanyDetail>?) {
        val gridLayoutManager = GridLayoutManager(context, 5, GridLayoutManager.HORIZONTAL, false)
        val snapHelper = PagerSnapHelper()
        adapter = DiscoverAdapter(null, companyList, null, null)
        bind.dailyCompanyRankingRcy.adapter = adapter
        bind.dailyCompanyRankingRcy.layoutManager = gridLayoutManager

        // 실시간 기업 페이징 처리
        bind.dailyCompanyRankingRcy.onFlingListener = null
        snapHelper.attachToRecyclerView(bind.dailyCompanyRankingRcy)
        bind.dailyCompanyRankingRcy.onFlingListener = snapHelper
        val indicator : CircleIndicator2 = bind.dailyDiscoverIndicator
        indicator.attachToRecyclerView(bind.dailyCompanyRankingRcy, snapHelper)
        indicator.createIndicators(2,0)

        bind.discoverContents.visibility = View.VISIBLE
        if (loadingDialog.isVisible){
            loadingDialog.dismiss()
        }

        parameters = Bundle().apply {
            this.putString("action", "view")
            if (isRefresh) {
                this.putString("type", "reload")
            }
        }
        sendFirebaseLog(parameters)


        adapter.setItemClickListener(object : DiscoverAdapter.OnItemClickListener{
            override fun onClick(v: View, position: Int, id: String, playBtn: Boolean?) {
                parameters = Bundle().apply {
                    this.putString("action", "click")
                    this.putString("type", "daily_co")
                    this.putString("co_id", id)
                    this.putInt("position", position)
                    this.putInt("max_position", companyList!!.size-1)
                }
                sendFirebaseLog(parameters)
                // 기업 상세정보로 이동
                removeInfo()
                bundle.putString("id", id)
                (activity as HomeActivity).getDiscoverDetail(bundle)
            }
        })

    }

    // 실시간 기업, 산업
    private fun getRankingList(service: Call<RankingList>) {

        Handler(Looper.getMainLooper()).postDelayed({
            service.enqueue(object : Callback<RankingList> {
                override fun onResponse(
                    call: Call<RankingList>,
                    response: Response<RankingList>
                ) {

                    if (response.code() == 200){
                        val result = response.body()

                        val asOfTimeText = (activity as HomeActivity).getDateFormatter("HH:mm", result?.created!!)
                        val text: String = String.format(resources.getString(R.string.search_criteria), asOfTimeText)
                        bind.asOfTimeRealTimeCompany.text = text
                        bind.asOfTimeindustry.text = text

                        rankingCompanyRecyclerView(result.company_list)
                        rankingIndustryRecyclerView(result.industry_list)
                        isAllSuccess = true
                    } else {
                        errorFragment("serverError")
                        isAllSuccess = false
                    }
                }

                override fun onFailure(call: Call<RankingList>, t: Throwable) {
                    errorFragment(null)
                    isAllSuccess = false
                }

            })
        }, 1000)

    }

    // 실시간 기업 recyclerView
    private fun rankingCompanyRecyclerView(companyList: ArrayList<CompanyDetail>?) {
        val gridLayoutManager = GridLayoutManager(context, 5, GridLayoutManager.HORIZONTAL, false)
        val snapHelper = PagerSnapHelper()
        adapter = DiscoverAdapter(null, companyList, null, null)
        bind.companyRankingRcy.adapter = adapter
        bind.companyRankingRcy.layoutManager = gridLayoutManager

        // 실시간 기업 페이징 처리
        bind.companyRankingRcy.onFlingListener = null
        snapHelper.attachToRecyclerView(bind.companyRankingRcy)
        bind.companyRankingRcy.onFlingListener = snapHelper
        val indicator : CircleIndicator2 = bind.discoverIndicator
        indicator.attachToRecyclerView(bind.companyRankingRcy, snapHelper)
        indicator.createIndicators(2,0)


        adapter.setItemClickListener(object : DiscoverAdapter.OnItemClickListener{
            override fun onClick(v: View, position: Int, id: String, playBtn: Boolean?) {
                parameters = Bundle().apply {
                    this.putString("action", "click")
                    this.putString("type", "rank_co")
                    this.putString("co_id", id)
                    this.putInt("position", position)
                    this.putInt("max_position", companyList!!.size-1)
                }
                sendFirebaseLog(parameters)
                // 기업 상세정보로 이동
                removeInfo()
                bundle.putString("id", id)
                (activity as HomeActivity).getDiscoverDetail(bundle)
            }
        })

    }



    // 실시간 산업 recyclerView
    private fun rankingIndustryRecyclerView(industryList: ArrayList<IndustryDetail>?) {
        val gridLayoutManager = GridLayoutManager(context, 5, GridLayoutManager.HORIZONTAL, false)
        adapter = DiscoverAdapter(null, null, industryList, null)
        bind.industryRankingRcy.adapter = adapter
        bind.industryRankingRcy.layoutManager = gridLayoutManager
        adapter.setItemClickListener(object : DiscoverAdapter.OnItemClickListener{
            override fun onClick(v: View, position: Int, id: String, playBtn: Boolean?) {
                parameters = Bundle().apply {
                    this.putString("action", "click")
                    this.putString("type", "rank_in")
                    this.putString("in_id", id)
                    this.putInt("position", position)
                    this.putInt("max_position", industryList!!.size-1)
                }
                sendFirebaseLog(parameters)
                // 산업 상세정보로 이동
                removeInfo()
                bundle.putString("id", id)
                bundle.putString("key", "industry")
                (activity as HomeActivity).getDiscoverDetail(bundle)
            }
        })
    }

    // 산업별 뉴스
    private fun getTotalIndustry(service: Call<List<IndustryDetail>>) {

        Handler(Looper.getMainLooper()).postDelayed({
            service.enqueue(object : Callback<List<IndustryDetail>> {
                override fun onResponse(
                    call: Call<List<IndustryDetail>>,
                    response: Response<List<IndustryDetail>>
                ) {
                    val result = response.body()

                    if (response.code() == 200){
                        if (result != null) {
                            totalIndustryRecyclerView(result)
                        }
                        isAllSuccess = true
                    } else {
                        errorFragment("serverError")
                        isAllSuccess = false
                    }
                }

                override fun onFailure(call: Call<List<IndustryDetail>>, t: Throwable) {
                    errorFragment(null)
                    isAllSuccess = false
                }

            })
        }, 1000)

    }

    // 산업별 뉴스 recyclerView
    private fun totalIndustryRecyclerView(result: List<IndustryDetail>) {
        val adapter = DiscoverAdapter(null, null, null, result)
        bind.totalIndustryRcy.adapter = adapter

        adapter.setItemClickListener(object : DiscoverAdapter.OnItemClickListener{
            override fun onClick(v: View, position: Int, id: String, playBtn: Boolean?) {
                parameters = Bundle().apply {
                    this.putString("action", "click")
                    this.putString("type", "list_in")
                    this.putString("in_id", id)
                    this.putInt("position", position)
                    this.putInt("max_position", result.size-1)
                }
                sendFirebaseLog(parameters)

                // 산업 상세정보로 이동
                removeInfo()

                if (playBtn == true) {
                    industryIndex = id
                    getDiscoverNewsDetail(supplementService.getDiscoverDetailIndustryNewsData(id, language, null))
                } else {
                    bundle.putString("id", id)
                    bundle.putString("key", "industry")
                    (activity as HomeActivity).getDiscoverDetail(bundle)
                }

            }
        })
    }

    // 상세보기 관련 뉴스
    private fun getDiscoverNewsDetail(service: Call<List<NewsDetail>>) {

        Handler(Looper.getMainLooper()).postDelayed({
            service.enqueue(object : Callback<List<NewsDetail>> {
                override fun onResponse(
                    call: Call<List<NewsDetail>>,
                    response: Response<List<NewsDetail>>
                ) {

                    if (response.code() == 200){
                        val result = response.body() as ArrayList<NewsDetail>

                        val bundle = Bundle()
                        bundle.putString("id", industryIndex)
                        bundle.putString("from", "discover")
                        bundle.putSerializable("playAllList", result)
                        bundle.putString("playBtn", "true")

                        (activity as HomeActivity).getPlayer(bundle)
                    } else {
                        errorFragment("serverError")
                    }
                }

                override fun onFailure(call: Call<List<NewsDetail>>, t: Throwable) {
                    errorFragment(null)
                }

            })
        }, 1000)

    }

    // 서버 에러 페이지
    private fun errorFragment(serverError : String?) {
        if (loadingDialog.isVisible){
            loadingDialog.dismiss()
        }
        bind.errorFragment.visibility = View.VISIBLE

        val errorText = bind.errorFragment.findViewById<TextView>(R.id.errorText)
        val errorText2 = bind.errorFragment.findViewById<TextView>(R.id.errorText2)
        val retryBtn = bind.errorFragment.findViewById<AppCompatButton>(R.id.retryBtn)

        if (serverError == "serverError") {
            errorText.text = getString(R.string.error_server_title)
            errorText2.text = getString(R.string.error_server_subtitle)
        }

        retryBtn.setOnClickListener {
            getRecommendedCompany(supplementService.getRecommendedCompanyData(language))
            getTotalIndustry(supplementService.getTotalIndustryData(language))
            getRankingList(supplementService.getRankingListData(language))
            getDailyCompanyRankingList(supplementService.getDailyCompanyRankingData(language))
            bind.discoverContents.visibility = View.VISIBLE
            loadingDialog.show((context as AppCompatActivity).supportFragmentManager, "loading")
            refreshView()
        }

    }

    private fun refreshView() {
        requireActivity().supportFragmentManager
            .beginTransaction()
            .detach(this)
            .attach(this)
            .commit()
        bind.discoverContents.visibility = View.GONE
        bind.errorFragment.visibility = View.GONE
    }

    private fun sendFirebaseLog(param: Bundle) {
        currentTime = (Calendar.getInstance().timeInMillis/1000).toInt()
        param.apply {
            this.putString("screen", "discover")
            this.putInt("time", currentTime)
        }
        firebaseAnalytics.logEvent("newdio", param)
    }


}