package com.traydcorp.newdio.ui.home

import android.content.Context
import android.content.res.Resources
import android.graphics.Rect
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.Px
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.PagerSnapHelper
import androidx.recyclerview.widget.RecyclerView
import com.traydcorp.newdio.dataModel.HomeNewsList
import com.traydcorp.newdio.databinding.FragmentHomeBinding
import com.traydcorp.newdio.utils.SharedPreference
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.lang.Math.abs
import androidx.core.os.ConfigurationCompat
import com.google.firebase.analytics.FirebaseAnalytics
import com.traydcorp.newdio.ui.player.PlayerFragment
import me.relex.circleindicator.CircleIndicator2
import com.traydcorp.newdio.R
import com.traydcorp.newdio.dataModel.NewsDetail
import com.traydcorp.newdio.utils.LoadingFragment
import com.traydcorp.newdio.utils.retofitService.RetrofitService
import com.traydcorp.newdio.utils.retrofitAPI.RetrofitClient
import retrofit2.Retrofit
import java.util.*
import kotlin.collections.ArrayList


class HomeFragment : Fragment() {

    companion object {
        fun newInstance() = HomeFragment()
    }

    private var viewBinding : FragmentHomeBinding? = null
    private val bind get() = viewBinding!!

    private val sharedPreferences = SharedPreference()

    private lateinit var layoutManager : LinearLayoutManager

    private lateinit var retrofit: Retrofit
    private lateinit var supplementService: RetrofitService

    private lateinit var language : String
    private var access : String? = null
    private var refresh : String? = null

    private var loadingDialog = LoadingFragment()

    private lateinit var firebaseAnalytics: FirebaseAnalytics

    private var parameters = Bundle()
    private var currentTime : Int = 0

    private var isRefresh = false


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        firebaseAnalytics = (activity as HomeActivity).firebaseAnalytics

        language = (activity as HomeActivity).getLanguagePreference()

        // access token, refresh token
        access = sharedPreferences.getShared(context, "access_token")
        refresh = sharedPreferences.getShared(context, "refresh_token")

        retrofit = RetrofitClient.getInstance()
        supplementService = retrofit.create(RetrofitService::class.java)

    }


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        viewBinding = FragmentHomeBinding.inflate(inflater, container, false)

        loadingDialog.show((context as AppCompatActivity).supportFragmentManager, "loading")


        // homeAPI 호출
        getHomeNews()

        // 뒤로가기
        val backBtn : ImageView? = requireActivity().findViewById(R.id.backBtn)
        backBtn?.setOnClickListener {
            if (requireActivity().supportFragmentManager.backStackEntryCount != 0){
                requireActivity().supportFragmentManager.popBackStack()
            }
        }

        return bind.root
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 새로 고침
        bind.swipeRefresh.setOnRefreshListener {
            currentTime = (Calendar.getInstance().timeInMillis/1000).toInt()
            parameters = Bundle().apply {
                this.putString("action", "drag")
                this.putInt("time", currentTime)
                this.putString("type", "relode")
            }
            firebaseAnalytics.logEvent("newdio", parameters)
            isRefresh = true

            loadingDialog.show((context as AppCompatActivity).supportFragmentManager, "loading")
            getHomeNews()
            bind.swipeRefresh.isRefreshing = false
        }

    }

    // home API 호출
    private fun getHomeNews() {
        if (access != null && refresh != null){
            homeAPI(supplementService.getHomeNewsData(access, refresh, language))
        } else {
            homeAPI(supplementService.getHomeNewsData(null, null, language))
        }
    }

    // home news api 호출
    private fun homeAPI(service: Call<List<HomeNewsList>>){

        Handler(Looper.getMainLooper()).postDelayed({
            service.enqueue(object : Callback<List<HomeNewsList>> {
                override fun onResponse(
                    call: Call<List<HomeNewsList>>,
                    response: Response<List<HomeNewsList>>
                ) {
                    if (response.code() == 200){
                        val result = response.body()!!
                        val homeTopNewsDetails: ArrayList<NewsDetail> = getTopNewsList(result)
                        val homeSubNewsDetails: ArrayList<HomeNewsList> = getSubNewsList(result)

                        homeTopNewsDetails.let { homeTopNewsRecyclerView(it) }
                        homeSubNewsDetails.let { homeSubNewsRecyclerView(it) }
                        bind.swipeRefresh.visibility = View.VISIBLE

                        // 토큰 검증 후 에러시 로그아웃 처리
                        val header = response.headers()["Token-Error"]
                        if (header != null) {
                            (activity as HomeActivity).logout(access!!, refresh!!, header)
                        }

                        // 액세스 토큰 만료시 새로 받은 토큰으로 업데이트
                        val newAccess = response.headers()["Authorization"]
                        if (newAccess != null) {
                            sharedPreferences.setShared("access_token", newAccess, requireContext())
                        }

                    } else {
                        errorFragment("serverError")
                    }

                }
                override fun onFailure(call: Call<List<HomeNewsList>>, t: Throwable) {
                    errorFragment(null)
                }
            })
        },1000)

    }


    // top news recyclerview 연결
    private fun homeTopNewsRecyclerView(it: ArrayList<NewsDetail>) {
        layoutManager = activity?.applicationContext?.let { it1 -> ProminentLayoutManager(it1) }!!
        val adapter = HomeNewsAdapter(it, null)

        // recyclerview adapter, layoutmanager 연결
        bind.topNewsListRcy.adapter = adapter
        bind.topNewsListRcy.layoutManager = layoutManager

        if (bind.topNewsListRcy.itemDecorationCount == 0){ // refresh 할때 item decoration이 다시 add 안되게
            // top news snapHelper
            val snapHelper = PagerSnapHelper()
            bind.topNewsListRcy.onFlingListener = null
            snapHelper.attachToRecyclerView(bind.topNewsListRcy)

            // top news item decoration
            val spacing = resources.getDimensionPixelSize(R.dimen.carousel_spacing)
            val indicator : CircleIndicator2 = bind.indicator
            indicator.attachToRecyclerView(bind.topNewsListRcy, snapHelper)
            bind.topNewsListRcy.addItemDecoration(LinearHorizontalSpacingDecoration(spacing))
            bind.topNewsListRcy.addItemDecoration(BoundsOffsetDecoration())
        }

        // loading dialog dismiss
        if (loadingDialog.isVisible){
            loadingDialog.dismiss()
        }

        // firebase log
        currentTime = (Calendar.getInstance().timeInMillis/1000).toInt()
        if(!isRefresh) {
            val defaultParams = Bundle().apply {
                this.putString("userId", (activity as HomeActivity).userId)
                this.putString("screen", "newdio")
            }
            firebaseAnalytics.setDefaultEventParameters(defaultParams)
        }

        parameters = Bundle().apply {
            this.putString("action", "view")
            this.putInt("time", currentTime)
            if (isRefresh) {
                this.putString("type", "reload")
            }
        }
        firebaseAnalytics.logEvent("newdio", parameters)

        // news 누르면 player로 이동
        adapter.setItemClickListener(object : HomeNewsAdapter.OnItemClickListener{
            override fun onClick(v: View, position: Int, playBtn: Boolean) {
                // firebase log
                currentTime = (Calendar.getInstance().timeInMillis/1000).toInt()
                parameters = Bundle().apply {
                    this.putString("action", "click")
                    this.putInt("time", currentTime)
                    this.putString("type", "hot")
                    this.putInt("position", position)
                    this.putInt("max_position", it.size-1)
                }
                firebaseAnalytics.logEvent("newdio", parameters)

                val crawlingdata = it[position].crawlingdata
                val bundle = Bundle()
                bundle.putInt("crawlingdata", crawlingdata)
                bundle.putString("from", "home")

                (activity as HomeActivity).getPlayer(bundle)
            }
        })
    }

    // 하위 news recyclerview 연결
    private fun homeSubNewsRecyclerView(it: ArrayList<HomeNewsList>) {
        val adapter = HomeNewsAdapter(null, it)
        bind.subNewsListRcy.adapter = adapter

        adapter.setItemClickListener(object : HomeNewsAdapter.OnItemClickListener{
            override fun onClick(v: View, position: Int, playBtn: Boolean) {
                val bundle = Bundle()
                bundle.putString("from", "home")
                bundle.putSerializable("playAllList", it[position].newsList)

                (activity as HomeActivity).getPlayer(bundle)
            }

        })
    }

    // top news list - 전체 리스트에서 top news만 가져오기
    private fun getTopNewsList(result: List<HomeNewsList>) : ArrayList<NewsDetail> {
        val homeTopNewsDetails : ArrayList<NewsDetail> = ArrayList()
        for (i in result.indices){
            if (result[i].category == "TOP"){
                for (j in result[i].newsList!!.indices){
                    val topNewsContents = result[i].newsList!![j].title
                    val imgUrl = result[i].newsList!![j].image_url
                    val crawlingdata = result[i].newsList!![j].crawlingdata

                    val topNewsList = NewsDetail()
                    topNewsList.title = topNewsContents
                    topNewsList.image_url = imgUrl
                    topNewsList.crawlingdata = crawlingdata

                    homeTopNewsDetails.add(topNewsList)
                }
            }
        }
        return homeTopNewsDetails
    }

    // top news list - 전체 리스트에서 top news를 제외한 news만 가져오기
    private fun getSubNewsList(result: List<HomeNewsList>) : ArrayList<HomeNewsList> {
        val homeSubNewsList : ArrayList<HomeNewsList> = ArrayList()
        for (i in result.indices) {
            if (result[i].category != "TOP") {
                val subNews: ArrayList<NewsDetail> = ArrayList()
                for (j in result[i].newsList!!.indices) {
                    val topNewsContents = result[i].newsList!![j].title
                    val imgUrl = result[i].newsList!![j].image_url
                    val crawlingdata = result[i].newsList!![j].crawlingdata
                    val postDate = result[i].newsList!![j].post_date
                    val newsSite = result[i].newsList!![j].news_site

                    val subNewsDetails = NewsDetail()
                    subNewsDetails.title = topNewsContents
                    subNewsDetails.image_url = imgUrl
                    subNewsDetails.crawlingdata = crawlingdata
                    subNewsDetails.post_date = postDate
                    subNewsDetails.news_site = newsSite

                    subNews.add(subNewsDetails)
                }
                val category: String? = result[i].category
                val categoryName: String? = result[i].category_name
                val index: String? = result[i].index
                val homeNewsList = HomeNewsList()
                homeNewsList.category = category
                homeNewsList.category_name = categoryName
                homeNewsList.index = index
                homeNewsList.newsList = subNews

                homeSubNewsList.add(homeNewsList)
            }
        }
        return homeSubNewsList
    }

    // 에러 페이지
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

        // 다시 시도 버튼
        retryBtn.setOnClickListener {
            getHomeNews()
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
        bind.swipeRefresh.visibility = View.GONE
        bind.errorFragment.visibility = View.GONE
    }


}


// TOP news decorator
class BoundsOffsetDecoration : RecyclerView.ItemDecoration() {
    override fun getItemOffsets(outRect: Rect,
                                view: View,
                                parent: RecyclerView,
                                state: RecyclerView.State) {
        super.getItemOffsets(outRect, view, parent, state)

        val itemPosition = parent.getChildAdapterPosition(view)
        val itemWidth = view.layoutParams.width
        val offset = (parent.width - itemWidth) / 2

        if (itemPosition == 0) {
            outRect.left = offset
        } else if (itemPosition == state.itemCount - 1) {
            outRect.right = offset
        }
    }
}

// TOP news decorator
internal class ProminentLayoutManager(
    context: Context,

    private val minScaleDistanceFactor: Float = 1.5f,

    // 선택된 news 옆 news들 화면의 90%
    private val scaleDownBy: Float = 0.1f
) : LinearLayoutManager(context, HORIZONTAL, false) {

    override fun onLayoutCompleted(state: RecyclerView.State?) =
        super.onLayoutCompleted(state).also { scaleChildren() }

    override fun scrollHorizontallyBy(
        dx: Int,
        recycler: RecyclerView.Recycler,
        state: RecyclerView.State
    ) = super.scrollHorizontallyBy(dx, recycler, state).also {
        if (orientation == HORIZONTAL) scaleChildren()
    }

    private fun scaleChildren() {
        val containerCenter = width / 2f

        val scaleDistanceThreshold = minScaleDistanceFactor * containerCenter

        for (i in 0 until childCount) {
            val child = getChildAt(i)!!

            val childCenter = (child.left + child.right) / 2f
            val distanceToCenter = abs(childCenter - containerCenter)

            val scaleDownAmount = (distanceToCenter / scaleDistanceThreshold).coerceAtMost(1f)
            val scale = 1f - scaleDownBy * scaleDownAmount

            child.scaleX = scale
            child.scaleY = scale

            val translationDirection = if (childCenter > containerCenter) -1 else 1
            val translationXFromScale = translationDirection * child.width * (1 - scale) / 2f
            child.translationX = translationXFromScale
        }
    }
}

// TOP news decorator - 사이 간격
class LinearHorizontalSpacingDecoration(@Px private val innerSpacing: Int) :
    RecyclerView.ItemDecoration() {

    override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State) {
        super.getItemOffsets(outRect, view, parent, state)

        val itemPosition = parent.getChildAdapterPosition(view)

        outRect.left = if (itemPosition == 0) 0 else innerSpacing / 2
        outRect.right = if (itemPosition == state.itemCount - 1) 0 else innerSpacing / 2
    }
}