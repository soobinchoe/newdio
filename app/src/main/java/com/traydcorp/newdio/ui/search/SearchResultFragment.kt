package com.traydcorp.newdio.ui.search

import android.content.Context
import android.content.res.Resources
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Spannable
import android.text.style.ForegroundColorSpan
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.animation.AlphaAnimation
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import androidx.core.os.ConfigurationCompat
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.analytics.FirebaseAnalytics
import com.traydcorp.newdio.R
import com.traydcorp.newdio.dataModel.NewsDetail
import com.traydcorp.newdio.databinding.FragmentSearchResultBinding
import com.traydcorp.newdio.ui.discover.DiscoverDetailAdapter
import com.traydcorp.newdio.ui.home.HomeActivity
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


class SearchResultFragment : Fragment() {

    private var viewBinding: FragmentSearchResultBinding? = null
    private val bind get() = viewBinding!!

    private val sharedPreferences = SharedPreference()
    private lateinit var retrofit: Retrofit
    private lateinit var supplementService: RetrofitService

    private lateinit var language: String
    private var access: String? = null
    private var refresh: String? = null

    private lateinit var adapter: DiscoverDetailAdapter

    private lateinit var searchWord: String
    private lateinit var relatedNews: ArrayList<NewsDetail>
    private var lastid = 0

    private var recentSearchList: ArrayList<String>? = null
    private var playAllList = ArrayList<NewsDetail>()

    private lateinit var firebaseAnalytics: FirebaseAnalytics

    private var parameters = Bundle()
    private var currentTime: Int = 0


    private var loadingDialog = LoadingFragment()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        firebaseAnalytics = (activity as HomeActivity).firebaseAnalytics

        loadingDialog.show((context as AppCompatActivity).supportFragmentManager, "loading")

        // 저장된 선호 언어가 있으면 해당 언어 불러오기, 없으면 시스템 설정 언어
        val savedLanguage = sharedPreferences.getShared(requireContext(), "language")
        if (savedLanguage == null) {
            language =
                ConfigurationCompat.getLocales(Resources.getSystem().configuration)[0].language
        } else {
            language = savedLanguage
        }


        // access token, refresh token
        access = sharedPreferences.getShared(context, "access_token")
        refresh = sharedPreferences.getShared(context, "refresh_token")

        retrofit = RetrofitClient.getInstance()
        supplementService = retrofit.create(RetrofitService::class.java)

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        viewBinding = FragmentSearchResultBinding.inflate(inflater, container, false)

        // 최근 검색어가 있으면
        if (sharedPreferences.getRecentSearch(requireContext(), "recentSearch")
                ?.isNullOrEmpty() == false
        ) {
            recentSearchList = sharedPreferences.getRecentSearch(requireContext(), "recentSearch")
        }

        searchWord = arguments?.getString("searchWord").toString()
        getSearchResult(supplementService.getSearchResult(searchWord, language, null), false)

        bind.toolbarDetailText.text = searchWord

        // 검색 결과 스크롤 listener
        bind.relatedNewsRcy.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                val layoutManager =
                    LinearLayoutManager::class.java.cast(bind.relatedNewsRcy.layoutManager)
                val totalItemCount = layoutManager.itemCount
                val lastVisible = layoutManager.findLastCompletelyVisibleItemPosition()

                // 추가 로드
                if (lastVisible >= totalItemCount - 1) {
                    parameters = Bundle().apply {
                        this.putString("action", "drag")
                        this.putString("type", "next_page")
                        this.putString("word", searchWord)
                    }
                    sendFirebaseLog(parameters)
                    lastid = relatedNews[lastVisible].crawlingdata
                    getSearchResult(
                        supplementService.getSearchResult(searchWord, language, lastid),
                        true
                    )
                }
            }
        })

        // 뒤로가기
        bind.backBtn.setOnClickListener {
            if (requireActivity().supportFragmentManager.backStackEntryCount != 0) {
                requireActivity().supportFragmentManager.popBackStack()
            }
        }

        // 전체 듣기
        bind.headingPlayAll.setOnClickListener {

            parameters = Bundle().apply {
                this.putString("action", "click")
                this.putString("type", "play_all")
                this.putString("word", searchWord)
            }
            sendFirebaseLog(parameters)

            val bundle = Bundle()
            if (playAllList.size > 100) { // 로드 된 기사가 100개 이상일 때 100개까지 자르기
                val subList: ArrayList<NewsDetail> = ArrayList(playAllList.subList(0, 100))
                bundle.putSerializable("playAllList", subList)
            } else {
                bundle.putSerializable("playAllList", playAllList)
            }

            // player로 이동
            (activity as HomeActivity).getPlayer(bundle)
        }

        return bind.root
    }

    // 관련 뉴스 recyclerView
    private fun relatedNewsRecyclerView(result: ArrayList<NewsDetail>) {
        adapter = DiscoverDetailAdapter(result)
        bind.relatedNewsRcy.adapter = adapter
        val layoutManager = LinearLayoutManager(context)
        bind.relatedNewsRcy.layoutManager = layoutManager

        parameters = Bundle().apply {
            this.putString("action", "view")
            this.putString("type", "search")
            this.putString("word", searchWord)
            this.putInt("count", result.size)
        }
        sendFirebaseLog(parameters)

        if (loadingDialog.isVisible) {
            loadingDialog.dismiss()
        }

        adapter.setItemClickListener(object : DiscoverDetailAdapter.OnItemClickListener {
            override fun onClick(v: View, position: Int, id: Int) {

                parameters = Bundle().apply {
                    this.putString("action", "click")
                    this.putString("type", "result")
                    this.putString("word", searchWord)
                    this.putInt("id", id)
                    this.putInt("position", position)
                    this.putInt("count", result.size)
                }
                sendFirebaseLog(parameters)

                val bundle = Bundle()
                bundle.putInt("crawlingdata", id)

                (activity as HomeActivity).getPlayer(bundle)
            }
        })
    }

    // 검색 결과 api
    private fun getSearchResult(service: Call<List<NewsDetail>>, reload: Boolean) {

        Handler(Looper.getMainLooper()).postDelayed({
            service.enqueue(object : Callback<List<NewsDetail>> {
                override fun onResponse(
                    call: Call<List<NewsDetail>>,
                    response: Response<List<NewsDetail>>
                ) {
                    if (response.code() == 200) {
                        if (!reload) { // 추가 로드가 아닐 때
                            val result = response.body()!! as ArrayList<NewsDetail>
                            relatedNews = result
                            playAllList = result
                            relatedNewsRecyclerView(result)
                            bind.resultView.visibility = View.VISIBLE

                            if (recentSearchList == null) { // 최근 검색어가 없을 때
                                val recentSearch = ArrayList<String>()
                                recentSearch.add(searchWord)
                                recentSearchList = recentSearch
                            } else { // 검색어가 있을 때
                                // 이미 있던 검색어 리스트 맨 위로 올리기
                                if (recentSearchList!!.contains(searchWord)) recentSearchList!!.remove(
                                    searchWord
                                )
                                if (recentSearchList!!.lastIndex == 15) {
                                    recentSearchList!!.removeLast()
                                }
                                recentSearchList!!.add(0, searchWord)

                            }
                            val searchFragment: SearchFragment =
                                requireActivity().supportFragmentManager.findFragmentByTag("search") as SearchFragment
                            searchFragment.refreshRecyclerView(recentSearchList!!)
                            // 최근 검색어 저장
                            sharedPreferences.setRecentSearch(
                                "recentSearch",
                                recentSearchList!!,
                                requireContext()
                            )
                            return
                        }

                        if (reload) { // 추가 로드일 때
                            val result = response.body()!! as ArrayList<NewsDetail>
                            bind.resultView.visibility = View.VISIBLE

                            for (i in result.indices) {
                                relatedNews.add(result[i])
                            }

                            adapter.notifyItemChanged(lastid)
                            adapter.notifyItemRangeChanged(lastid, result.size)
                            adapter.notifyItemRangeInserted(lastid, result.size)

                            parameters = Bundle().apply {
                                this.putString("action", "view")
                                this.putString("type", "next_page")
                                this.putString("word", searchWord)
                                this.putInt("count", relatedNews.size)
                            }
                            sendFirebaseLog(parameters)
                        }
                    }
                    if (response.code() == 204) { // 검색 결과 없을 때
                        val text: String =
                            String.format(getString(R.string.search_no_word), searchWord)

                        // text 부분적으로 다른 색상 지정
                        bind.noSearchResult.setText(text, TextView.BufferType.SPANNABLE)
                        val word: Spannable = bind.noSearchResult.text as Spannable
                        if (language == "ko") { // 언어에 따라서 검색어 색상 위치 다르게 지정
                            word.setSpan(
                                ForegroundColorSpan(resources.getColor(R.color.light_green)),
                                1,
                                searchWord.length + 1,
                                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                            )
                        } else {
                            word.setSpan(
                                ForegroundColorSpan(resources.getColor(R.color.light_green)),
                                word.lastIndex - searchWord.length,
                                word.lastIndex,
                                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                            )
                        }

                        val fadeInAnimation = AlphaAnimation(0.0f, 1.0f)
                        fadeInAnimation.duration = 200

                        bind.noSearchResult.visibility = View.VISIBLE
                        bind.noSearchResult.animation = fadeInAnimation
                        bind.noSearchResult.animation.start()
                    }
                }

                override fun onFailure(call: Call<List<NewsDetail>>, t: Throwable) {
                    errorFragment(null)
                }

            })
        }, 1000)
    }

    // 서버 에러 페이지
    private fun errorFragment(serverError: String?) {

        Handler(Looper.getMainLooper()).postDelayed({
            if (loadingDialog.isVisible) {
                bind.errorFragment.visibility = View.VISIBLE

            }

        }, 1000)
        val errorText = bind.errorFragment.findViewById<TextView>(R.id.errorText)
        val errorText2 = bind.errorFragment.findViewById<TextView>(R.id.errorText2)
        val retryBtn = bind.errorFragment.findViewById<AppCompatButton>(R.id.retryBtn)

        if (serverError == "serverError") {
            errorText.text = getString(R.string.error_server_title)
            errorText2.text = getString(R.string.error_server_subtitle)
        }

        retryBtn.setOnClickListener {
            getSearchResult(supplementService.getSearchResult(searchWord, language, null), false)
            refreshView()
            loadingDialog.show((context as AppCompatActivity).supportFragmentManager, "loading")
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

    override fun onDetach() {
        super.onDetach()
        val bottomNavi: BottomNavigationView = activity?.findViewById(R.id.bottomNavigation)!!
        bottomNavi.visibility = View.VISIBLE
    }

    private fun sendFirebaseLog(param: Bundle) {
        currentTime = (Calendar.getInstance().timeInMillis / 1000).toInt()
        param.apply {
            this.putString("screen", "search")
            this.putInt("time", currentTime)
        }
        firebaseAnalytics.logEvent("newdio", param)
    }


}