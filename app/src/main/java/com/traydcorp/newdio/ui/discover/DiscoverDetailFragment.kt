package com.traydcorp.newdio.ui.discover

import android.content.Context
import android.content.res.ColorStateList
import android.content.res.Resources
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
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import androidx.core.content.ContextCompat
import androidx.core.os.ConfigurationCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.gson.JsonObject
import com.traydcorp.newdio.R
import com.traydcorp.newdio.dataModel.CompanyDetail
import com.traydcorp.newdio.dataModel.IndustryDetail
import com.traydcorp.newdio.dataModel.NewsDetail
import com.traydcorp.newdio.databinding.FragmentDiscoverDetailBinding
import com.traydcorp.newdio.ui.home.HomeActivity
import com.traydcorp.newdio.ui.player.LoginPopUpFragment
import com.traydcorp.newdio.ui.player.PlayerFragment
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


class DiscoverDetailFragment : Fragment() {

    private var viewBinding : FragmentDiscoverDetailBinding? = null
    private val bind get() = viewBinding!!

    private lateinit var adapter : DiscoverDetailAdapter
    private var result = ArrayList<NewsDetail>()
    private var playAllList = ArrayList<NewsDetail>()

    private val sharedPreferences = SharedPreference()
    private lateinit var retrofit: Retrofit
    private lateinit var supplementService: RetrofitService

    private lateinit var language : String
    private var access : String? = null
    private var refresh : String? = null

    private lateinit var companyInfo : String

    private var shortAnimationDuration: Int = 0

    private var isAllSuccess = false

    private var bundle = Bundle()

    private var isHeartOn = false
    private lateinit var index : String

    private var loadingDialog = LoadingFragment()

    private lateinit var firebaseAnalytics: FirebaseAnalytics

    private var parameters = Bundle()
    private var currentTime : Int = 0
    private var id : String? = null
    private var key : String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        firebaseAnalytics = (activity as HomeActivity).firebaseAnalytics

        // 저장된 선호 언어가 있으면 해당 언어 불러오기, 없으면 시스템 설정 언어
        val savedLanguage = sharedPreferences.getShared(requireContext(), "language")
        if (savedLanguage == null){
            language = ConfigurationCompat.getLocales(Resources.getSystem().configuration)[0].language
        } else {
            language = savedLanguage
        }

        // access token, refresh token
        access = sharedPreferences.getShared(context, "access_token")
        refresh = sharedPreferences.getShared(context, "refresh_token")

        retrofit = RetrofitClient.getInstance()
        supplementService = retrofit.create(RetrofitService::class.java)

        shortAnimationDuration = resources.getInteger(android.R.integer.config_shortAnimTime)


    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        viewBinding = FragmentDiscoverDetailBinding.inflate(inflater, container, false)

        loadingDialog.show((context as AppCompatActivity).supportFragmentManager, "loading")

        // bottom navi 없애기
        val bottomNavi = activity?.findViewById<BottomNavigationView>(R.id.bottomNavigation)
        bottomNavi?.visibility = View.GONE

        id = arguments?.getString("id")
        key = arguments?.getString("key")

        if (key == "industry") {
            bind.infoBtn.visibility = View.GONE
            getDiscoverIndustryDetail(supplementService.getDiscoverDetailIndustryData(access, refresh, id!!, language))
            getDiscoverNewsDetail(supplementService.getDiscoverDetailIndustryNewsData(id!!, language, null))
        } else {
            Log.d("id", id.toString())
            getDiscoverCompanyDetail(supplementService.getDiscoverDetailCompanyData(access, refresh, id!!, language))
            getDiscoverNewsDetail(supplementService.getDiscoverDetailCompanyNewsData(id!!, language, null))
        }

        return bind.root
    }

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 뒤로가기
        bind.backBtn.setOnClickListener {
            if (requireActivity().supportFragmentManager.backStackEntryCount != 0){
                requireActivity().supportFragmentManager.popBackStack()
            }
        }

        // textView 애니메이션
        val fadeInAnimation : Animation = AlphaAnimation(0.0f, 1.0f)
        fadeInAnimation.duration = 300
        val fadeOutAnimation : Animation = AlphaAnimation(1.0f, 0.0f)
        fadeOutAnimation.duration = 300


        // 상단 스크롤 고정
        bind.scrollView.run {
            header = bind.heading
            stickListener = { _ ->
                Log.d("LOGGER_TAG", "stickListener")
                bind.toolbarDetailText.visibility = View.VISIBLE
                bind.toolbarDetailText.animation = fadeInAnimation
                bind.toolbarDetailText.animation.start()
                bind.headingPlayAll.visibility = View.VISIBLE
                bind.headingPlayAll.animation = fadeInAnimation
                bind.headingPlayAll.animation.start()

            }
            freeListener = { _ ->
                Log.d("LOGGER_TAG", "freeListener")
                bind.toolbarDetailText.visibility = View.GONE
                bind.toolbarDetailText.animation = fadeOutAnimation
                bind.toolbarDetailText.animation.start()
                bind.headingPlayAll.visibility = View.GONE
                bind.headingPlayAll.animation = fadeOutAnimation
                bind.headingPlayAll.animation.start()
            }
        }

        // info 버튼
        bind.infoBtn.setOnClickListener {
            currentTime = (Calendar.getInstance().timeInMillis/1000).toInt()
            parameters = Bundle().apply {
                this.putString("screen", "detail")
                this.putString("action", "click")
                this.putInt("time", currentTime)
                this.putString("type", "info")
                if (key == "industry") {
                    this.putString("in_id", id)
                } else {
                    this.putString("co_id", id)
                }
            }
            firebaseAnalytics.logEvent("newdio", parameters)

            bind.infoBtn.setColorFilter(ContextCompat.getColor(requireContext(), R.color.light_green))
            var dialog = DiscoverInfoFragment()

            if (dialog.isAdded){
                activity?.supportFragmentManager?.beginTransaction()?.remove(dialog)!!.commit()
                dialog = DiscoverInfoFragment()
            }

            bundle.putString("key", companyInfo)
            bundle.putString("from", "discover")
            dialog.arguments = bundle
            dialog.show(parentFragmentManager, "dialog")

            // dismiss 될 때 아이콘 다시 변경
            activity?.supportFragmentManager?.executePendingTransactions()
            dialog.dialog?.setOnDismissListener {
                activity?.supportFragmentManager?.beginTransaction()?.remove(dialog)!!.commit()
                bind.infoBtn.setColorFilter(ContextCompat.getColor(requireContext(), R.color.white))
            }

        }

        // 전체듣기 버튼
        bind.playAllBtn.setOnClickListener {
            getPlayAll()
        }

        bind.headingPlayAll.setOnClickListener {
            getPlayAll()
        }


        // 관련 뉴스 페이징 처리, 스크롤 끝까지 내려가면 다음 리스트 호출
        bind.scrollView.setOnScrollChangeListener { _, _, _, _, _ ->
            if (!bind.scrollView.canScrollVertically(1)) {

                val lastPosition =
                    (bind.relatedNewsRcy.layoutManager as LinearLayoutManager?)!!.findLastCompletelyVisibleItemPosition()

                val id = arguments?.getString("id")
                val key = arguments?.getString("key")
                val lastid = result[lastPosition].crawlingdata

                currentTime = (Calendar.getInstance().timeInMillis/1000).toInt()
                parameters = Bundle().apply {
                    this.putString("screen", "detail")
                    this.putString("action", "drag")
                    this.putInt("time", currentTime)
                    this.putString("type", "next_page")
                    if (key == "industry") {
                        this.putString("in_id", id)
                    } else {
                        this.putString("co_id", id)
                    }
                    this.putInt("id", lastid)
                }
                firebaseAnalytics.logEvent("newdio", parameters)

                if (key == "industry") {
                    getMoreDiscoverNewsDetail(supplementService.getDiscoverDetailIndustryNewsData(id!!, language, lastid), lastid)
                } else {
                    getMoreDiscoverNewsDetail(supplementService.getDiscoverDetailCompanyNewsData(id!!, language, lastid), lastid)
                }

            }
        }

        // 좋아요 버튼
        bind.heartBtn.setOnClickListener {
            if (access != null && refresh != null){
                bind.heartBtn.backgroundTintList = when (isHeartOn) {
                    true -> ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.white))
                    else -> ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.light_green))
                }
                if (isHeartOn){
                    bind.heartBtn.setBackgroundResource(R.drawable.ic_general_heart_off)
                    isHeartOn = false
                } else if (!isHeartOn) {
                    bind.heartBtn.setBackgroundResource(R.drawable.ic_general_heart_on)
                    isHeartOn = true
                }
                discoverDetailLikes(supplementService.discoverDetailLikes(access, refresh, index))
            } else {
                // 로그인 팝업
                val dialog = LoginPopUpFragment()
                dialog.show(parentFragmentManager, "dialog")
            }
        }

    }

    override fun onDetach() {
        super.onDetach()
        if (arguments?.getString("from") != "favorite") {
            val bottomNavi = activity?.findViewById<BottomNavigationView>(R.id.bottomNavigation)
            bottomNavi?.visibility = View.VISIBLE
        }

        val toolbarText : TextView? = activity?.findViewById(R.id.discoverToolbarText)
        val toolbarTextLive : TextView? = activity?.findViewById(R.id.liveToolbarText)
        if (arguments?.getString("from") == "live"){
            Log.d("from", "live")
            toolbarTextLive?.visibility = View.VISIBLE
            toolbarTextLive?.text = getString(R.string.menu_live)
        } else {
            toolbarText?.text = getString(R.string.menu_discover)
        }

        arguments?.clear()

    }

    // 전체듣기
    private fun getPlayAll(){
        currentTime = (Calendar.getInstance().timeInMillis/1000).toInt()
        parameters = Bundle().apply {
            this.putString("screen", "detail")
            this.putString("action", "click")
            this.putInt("time", currentTime)
            this.putString("type", "play_all")
            if (key == "industry") {
                this.putString("in_id", id)
            } else {
                this.putString("co_id", id)
            }
        }
        firebaseAnalytics.logEvent("newdio", parameters)

        // 로드된 리스트가 100개가 넘어가면 100개까지 잘라서 플레이 리스트로
        val bundle = Bundle()
        bundle.putString("from", "discover")
        if(playAllList.size > 100){
            val subList: ArrayList<NewsDetail> = ArrayList(playAllList.subList(0, 100))
            bundle.putSerializable("playAllList", subList)
        } else {
            bundle.putSerializable("playAllList", playAllList)
        }

        // player로 이동
        if (activity?.supportFragmentManager?.findFragmentByTag("player") != null){
            val player = activity?.supportFragmentManager?.findFragmentByTag("player") as PlayerFragment
            player.disconnectMedia()
            player.dialog?.dismiss()
        }
        val playerFragment = PlayerFragment()
        playerFragment.arguments = bundle
        playerFragment.show((context as AppCompatActivity).supportFragmentManager, "player")
    }

    // 상세보기 company
    private fun getDiscoverCompanyDetail(service: Call<CompanyDetail>) {

        Handler(Looper.getMainLooper()).postDelayed({
            service.enqueue(object : Callback<CompanyDetail> {
                override fun onResponse(
                    call: Call<CompanyDetail>,
                    response: Response<CompanyDetail>
                ) {
                    if (response.code() == 200){
                        val result = response.body()!!

                        if (result.logo_url != null){
                            view?.let {
                                Glide.with(it)
                                    .load(result.logo_url).centerCrop()
                                    .into(bind.companyImage)
                            }
                        }
                        bind.companyName.text = result.company
                        bind.Industry.text = result.related_industry
                        bind.toolbarDetailText.text = result.company
                        //bind.likesNo.text = result.likes.toString()
                        companyInfo = result.description.toString()
                        index = result.index.toString()
                        if (result.user_likes) {
                            bind.heartBtn.setBackgroundResource(R.drawable.ic_general_heart_on)
                            bind.heartBtn.backgroundTintList =  ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.light_green))
                            isHeartOn = true
                        }
                        isAllSuccess = true

                        // 액세스 토큰 만료시 새로 받은 토큰으로 업데이트
                        val newAccess = response.headers()["Authorization"]
                        if (newAccess != null) {
                            sharedPreferences.setShared("access_token", newAccess, requireContext())
                        }
                    } else {
                        errorFragment("serverError")
                        isAllSuccess = false
                    }
                }

                override fun onFailure(call: Call<CompanyDetail>, t: Throwable) {
                    errorFragment(null)
                    isAllSuccess = false
                }

            })
        }, 1000)

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
                        result = response.body() as ArrayList<NewsDetail>
                        playAllList = response.body() as ArrayList<NewsDetail>
                        discoverDetailRecyclerView(result)
                        bind.discoverDetailToolbar.visibility = View.VISIBLE
                        bind.scrollView.visibility = View.VISIBLE
                        isAllSuccess = true
                    } else {
                        errorFragment("serverError")
                        isAllSuccess = false
                    }
                }

                override fun onFailure(call: Call<List<NewsDetail>>, t: Throwable) {
                    errorFragment(null)
                    isAllSuccess = false
                }

            })
        }, 1000)

    }

    // 상세보기 company 관련 뉴스 페이징
    private fun getMoreDiscoverNewsDetail(service: Call<List<NewsDetail>>, lastid : Int) {

        Handler(Looper.getMainLooper()).postDelayed({
            service.enqueue(object : Callback<List<NewsDetail>> {
                override fun onResponse(
                    call: Call<List<NewsDetail>>,
                    response: Response<List<NewsDetail>>
                ) {


                    if (response.code() == 200){

                        val relatedNewsList = response.body() as ArrayList<NewsDetail>

                        for (i in relatedNewsList.indices) {
                            result.add(relatedNewsList[i])
                        }

                        adapter.notifyItemChanged(lastid)
                        adapter.notifyItemRangeChanged(lastid, relatedNewsList.size)
                        adapter.notifyItemRangeInserted(lastid, relatedNewsList.size)

                        val crawlingdata = relatedNewsList[relatedNewsList.lastIndex].crawlingdata

                        currentTime = (Calendar.getInstance().timeInMillis/1000).toInt()
                        parameters = Bundle().apply {
                            this.putString("screen", "detail")
                            this.putString("action", "view")
                            this.putInt("time", currentTime)
                            this.putString("type", "next_page")
                            if (key == "industry") {
                                this.putString("in_id", id)
                            } else {
                                this.putString("co_id", id)
                            }
                            this.putInt("pre_id", lastid)
                            this.putInt("id", crawlingdata)
                        }
                        firebaseAnalytics.logEvent("newdio", parameters)

                        isAllSuccess = true
                    } else {
                        isAllSuccess = false
                    }
                }

                override fun onFailure(call: Call<List<NewsDetail>>, t: Throwable) {
                    isAllSuccess = false
                }

            })
        }, 1000)

    }

    // 상세보기 industry
    private fun getDiscoverIndustryDetail(service: Call<IndustryDetail>) {

        Handler(Looper.getMainLooper()).postDelayed({
            service.enqueue(object : Callback<IndustryDetail> {
                override fun onResponse(
                    call: Call<IndustryDetail>,
                    response: Response<IndustryDetail>
                ) {

                    if (response.code() == 200){
                        Log.d("DETAIL CALL", "200")
                        val result = response.body()!!

                        if (result.logo_url != null){
                            view?.let {
                                Glide.with(it)
                                    .load(result.logo_url).centerCrop()
                                    .into(bind.companyImage)
                            }
                        }
                        bind.companyName.text = result.industry
                        bind.toolbarDetailText.text = result.industry
                        //bind.likesNo.text = result.likes.toString()
                        if (result.user_likes) {
                            bind.heartBtn.setBackgroundResource(R.drawable.ic_general_heart_on)
                            bind.heartBtn.backgroundTintList =  ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.light_green))
                            isHeartOn = true
                        }
                        index = result.index.toString()
                        isAllSuccess = true

                        // 액세스 토큰 만료시 새로 받은 토큰으로 업데이트
                        val newAccess = response.headers()["Authorization"]
                        if (newAccess != null) {
                            sharedPreferences.setShared("access_token", newAccess, requireContext())
                        }
                    } else {
                        errorFragment("serverError")
                        Log.d("DETAIL CALL", "ELSE")
                        isAllSuccess = false
                    }
                }

                override fun onFailure(call: Call<IndustryDetail>, t: Throwable) {
                    Log.d("DETAIL CALL", "FAIL")
                    errorFragment(null)
                    isAllSuccess = false
                }

            })
        }, 1000)

    }


    // 상세보기 관련 뉴스 recyclerView
    private fun discoverDetailRecyclerView(result: ArrayList<NewsDetail>) {
        adapter = DiscoverDetailAdapter(result)
        bind.relatedNewsRcy.adapter = adapter
        val layoutManager = LinearLayoutManager(context)
        bind.relatedNewsRcy.layoutManager = layoutManager

        if (loadingDialog.isVisible){
            loadingDialog.dismiss()
        }

        currentTime = (Calendar.getInstance().timeInMillis/1000).toInt()
        parameters = Bundle().apply {
            this.putString("screen", "detail")
            this.putString("action", "view")
            this.putInt("time", currentTime)
            if (key == "industry") {
                this.putString("type", "industry")
                this.putString("in_id", id)
            } else {
                this.putString("type", "company")
                this.putString("co_id", id)
            }
        }
        firebaseAnalytics.logEvent("newdio", parameters)

        adapter.setItemClickListener(object : DiscoverDetailAdapter.OnItemClickListener{
            override fun onClick(v: View, position: Int, crawlingdata: Int) {
                currentTime = (Calendar.getInstance().timeInMillis/1000).toInt()
                parameters = Bundle().apply {
                    this.putString("screen", "detail")
                    this.putString("action", "click")
                    this.putInt("time", currentTime)
                    this.putString("type", "news")
                    if (key == "industry") {
                        this.putString("in_id", id)
                    } else {
                        this.putString("co_id", id)
                    }
                    this.putInt("id", result[position].crawlingdata)
                    this.putInt("last_id", result[result.lastIndex].crawlingdata)
                }
                firebaseAnalytics.logEvent("newdio", parameters)

                val bundle = Bundle()
                bundle.putInt("crawlingdata", crawlingdata)
                bundle.putString("from", "discover")

                // player로 이동
                if (activity?.supportFragmentManager?.findFragmentByTag("player") != null){
                    val player = activity?.supportFragmentManager?.findFragmentByTag("player") as PlayerFragment
                    player.disconnectMedia()
                    player.dialog?.dismiss()
                }
                val playerFragment = PlayerFragment()
                playerFragment.arguments = bundle
                playerFragment.show((context as AppCompatActivity).supportFragmentManager, "player")

                /*requireActivity().supportFragmentManager.beginTransaction()
                    .setCustomAnimations(R.animator.player_slide_up, R.animator.player_slide_down, R.animator.player_slide_up, R.animator.player_slide_down)
                    .addToBackStack(null)
                    .add(R.id.homeView, playerFragment).commit()*/

            }

        })

    }

    // 기업 산업 likes api 호출
    private fun discoverDetailLikes(service: Call<JsonObject>) {

        Handler(Looper.getMainLooper()).postDelayed({
            service.enqueue(object : Callback<JsonObject> {
                override fun onResponse(call: Call<JsonObject>, response: Response<JsonObject>) {
                    if (response.code() == 200){
                        currentTime = (Calendar.getInstance().timeInMillis/1000).toInt()
                        parameters = Bundle().apply {
                            this.putString("screen", "detail")
                            this.putString("action", "click")
                            this.putInt("time", currentTime)
                            this.putString("type", "like")
                            if (key == "industry") {
                                this.putString("in_id", id)
                            } else {
                                this.putString("co_id", id)
                            }
                        }

                        if (response.body()?.get("code").toString() == "0"){
                            parameters.putInt("like", 0)
                            isHeartOn = false
                        }
                        if (response.body()?.get("code").toString() == "1"){
                            parameters.putInt("like", 1)
                            isHeartOn = true
                        }
                        firebaseAnalytics.logEvent("newdio", parameters)

                        // 액세스 토큰 만료시 새로 받은 토큰으로 업데이트
                        val newAccess = response.headers()["Authorization"]
                        if (newAccess != null) {
                            sharedPreferences.setShared("access_token", newAccess, requireContext())
                        }
                    }
                }

                override fun onFailure(call: Call<JsonObject>, t: Throwable) {
                    Log.d("onFailure", t.message.toString())
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
            val id = arguments?.getString("id")
            getDiscoverNewsDetail(supplementService.getDiscoverDetailCompanyNewsData(id!!, language, null))
            getDiscoverCompanyDetail(supplementService.getDiscoverDetailCompanyData(access, refresh, id, language))
            loadingDialog.show((context as AppCompatActivity).supportFragmentManager, "loading")
            refreshView()
        }

    }


    // 에러 페이지 다시보기 후 view refresh
    private fun refreshView() {
        requireActivity().supportFragmentManager
            .beginTransaction()
            .detach(this)
            .attach(this)
            .commit()

        bind.errorFragment.visibility = View.GONE
    }




}


