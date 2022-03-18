package com.traydcorp.newdio.ui.player

import android.content.Context
import android.content.res.Resources
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.traydcorp.newdio.R
import com.traydcorp.newdio.databinding.FragmentPlayerListBinding
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.fragment.app.setFragmentResult
import androidx.recyclerview.widget.ItemTouchHelper
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.traydcorp.newdio.dataModel.NewsDetail

import androidx.core.os.ConfigurationCompat
import com.google.firebase.analytics.FirebaseAnalytics
import com.traydcorp.newdio.ui.home.HomeActivity
import com.traydcorp.newdio.utils.SharedPreference
import com.traydcorp.newdio.utils.retofitService.RetrofitService
import com.traydcorp.newdio.utils.retrofitAPI.RetrofitClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashSet


class PlayerListFragment : BottomSheetDialogFragment() {

    companion object {
        fun newInstance() = PlayerListFragment()
    }

    private var viewBinding : FragmentPlayerListBinding? = null
    private val bind get() = viewBinding!!

    private val excludeList = HashSet<Int>()
    private val relatedList = HashSet<Int>()

    private val sharedPreferences = SharedPreference()
    private lateinit var retrofit: Retrofit
    private lateinit var supplementService: RetrofitService

    private lateinit var language : String
    private var access : String? = null
    private var refresh : String? = null

    private lateinit var adapter : PlayListAdapter
    private var result = ArrayList<NewsDetail>()
    private var relatedNewsList = ArrayList<NewsDetail>()
    private var lastIndex : Int = 0

    private lateinit var firebaseAnalytics: FirebaseAnalytics

    private var parameters = Bundle()
    private var currentTime : Int = 0

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
        viewBinding = FragmentPlayerListBinding.inflate(inflater, container, false)

        val playerData = arguments?.getSerializable("list")

        relatedNewsList = playerData as ArrayList<NewsDetail>
        relatedListRecyclerView(relatedNewsList)

        // 더보기 버튼
        bind.loadMoreBtn.setOnClickListener {
            loadMoreNews()
        }

        return bind.root
    }

    // bottom sheet 전체 화면크기
    override fun onStart() {
        super.onStart()
        if (dialog != null) {
            val bottomSheet: View = dialog!!.findViewById(R.id.design_bottom_sheet)
            bottomSheet.layoutParams.height = ViewGroup.LayoutParams.MATCH_PARENT
        }

        val view = view
        view!!.post{
            val parent = view.parent as View
            val params = parent.layoutParams as CoordinatorLayout.LayoutParams
            val behavior = params.behavior
            val bottomSheetBehavior = behavior as BottomSheetBehavior<*>?
            bottomSheetBehavior!!.peekHeight = view.measuredHeight
            parent.setBackgroundColor(Color.TRANSPARENT)
        }
    }

    fun dpToPx(context: Context, dp: Float): Float {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, context.resources.displayMetrics)
    }

    // 플레이 리스트 recyclerView
    private fun relatedListRecyclerView(relatedNewsList: ArrayList<NewsDetail>) {

        for (i in relatedNewsList.indices) {
            excludeList.add(relatedNewsList[i].crawlingdata)
            relatedList.add(relatedNewsList[i].crawlingdata)
        }

        // firebase log
        parameters = Bundle().apply {
            this.putString("action", "view")
            this.putString("type", "playlist")
            this.putInt("id", arguments?.getInt("crawlingdata")!!)
        }
        sendFirebaseLog(parameters)

        adapter = PlayListAdapter(relatedNewsList)

        // swipe로 삭제 SwipeHelperCallback
        val swipeHelperCallback = SwipeHelperCallback().apply {
            setClamp(dpToPx(requireContext(), 72f))
        }
        val itemTouchHelper = ItemTouchHelper(swipeHelperCallback)
        itemTouchHelper.attachToRecyclerView(bind.listRcy)

        bind.listRcy.adapter = adapter
        bind.listRcy.apply {
            setOnTouchListener { view, motionEvent ->
                swipeHelperCallback.removePreviousClamp(this, false)
                false
            }
        }

        adapter.setItemClickListener(object : PlayListAdapter.OnItemClickListener{
            override fun onClick(v: View, position: Int, id: Int) {
                parameters = Bundle().apply {
                    this.putString("action", "click")
                    this.putString("type", "playlist")
                    this.putInt("id", arguments?.getInt("crawlingdata")!!)
                    this.putInt("to_id", relatedNewsList[position].crawlingdata)
                }
                sendFirebaseLog(parameters)

                if (id != 0) { // 해당 플레이어로 이동
                    val bundle = Bundle()
                    bundle.putInt("index", position)
                    bundle.putInt("id", id)
                    setFragmentResult("requestKey", bundle)

                    val player = requireActivity().supportFragmentManager.findFragmentByTag("player") as PlayerFragment
                    player.dialog?.show()

                } else { // swipe로 삭제
                    if (!relatedNewsList[position].isPlaying) {
                        relatedNewsList.removeAt(position)
                        swipeHelperCallback.removePreviousClamp(bind.listRcy, true)
                        adapter.notifyDataSetChanged()
                        adapter.notifyItemRangeRemoved(position, 1)
                    }
                }

            }
        })

        lastIndex = relatedNewsList.lastIndex

    }

    // 플레이 리스트 추가 로드
    fun loadMoreNews() {
        val excludeListTemp = excludeList.toString()
        val excludeListQuery = excludeListTemp.substring(1, excludeListTemp.length-1).replace(" ","")
        val relatedListTemp = relatedList.toString()
        val relatedListQuery = relatedListTemp.substring(1, relatedListTemp.length-1).replace(" ","")
        if (arguments?.getString("from") == "live"){ // 라이브 플레이 리스트 추가 로드
            val lastIndex = relatedNewsList.lastIndex
            val crawlingdata = relatedNewsList[lastIndex].crawlingdata
            if (access != null && refresh != null){
                loadMoreLiveNewsList(supplementService.getLivePlayerListData(access, refresh, crawlingdata, language))
            } else {
                loadMoreLiveNewsList(supplementService.getLivePlayerListData(null, null, crawlingdata, language))
            }
        } else if (arguments?.getString("from") == "discover") { // 상세보기 전체듣기 추가 로드
            val id = arguments?.getString("id")
            val lastIndex = relatedNewsList.lastIndex
            getMoreDiscoverNewsDetail(supplementService.getDiscoverDetailIndustryNewsData(id!!, language, relatedNewsList[lastIndex].crawlingdata), relatedNewsList[lastIndex].crawlingdata)
        } else { // 플레이어 플레이 리스트 추가 로드
            loadMoreNewsList(supplementService.getPlayerNextListData(access, refresh, language, excludeListQuery, relatedListQuery))

        }

    }

    private fun loadMoreNewsList(service: Call<List<NewsDetail>>) {
        Handler(Looper.getMainLooper()).postDelayed({
            service.enqueue(object : Callback<List<NewsDetail>> {
                override fun onResponse(call: Call<List<NewsDetail>>, response: Response<List<NewsDetail>>) {
                    if (response.code() == 200){
                        result = response.body() as ArrayList<NewsDetail>
                        for (i in result.indices) {
                            excludeList.add(result[i].crawlingdata)
                            relatedNewsList.add(result[i])
                        }
                        adapter.notifyItemChanged(lastIndex+1)
                        adapter.notifyItemRangeChanged(lastIndex+1, relatedNewsList.size)
                        adapter.notifyItemRangeInserted(lastIndex+1, relatedNewsList.size)
                        lastIndex = relatedNewsList.lastIndex

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
                    }
                }

                override fun onFailure(call: Call<List<NewsDetail>>, t: Throwable) {
                    Log.d("errorMessage", t.message.toString())
                }

            })
        }, 1000)
    }


    // 라이브 플레이 리스트 추가 로드
    private fun loadMoreLiveNewsList(service: Call<NewsDetail>) {

        Handler(Looper.getMainLooper()).postDelayed({
            service.enqueue(object : Callback<NewsDetail> {
                override fun onResponse(
                    call: Call<NewsDetail>,
                    response: Response<NewsDetail>
                ) {
                    if (response.code() == 200){
                        val result = response.body()!!

                        for (i in result.related_news_list!!.indices) {
                            relatedNewsList.add(lastIndex + i + 1, result.related_news_list!![i])
                        }

                        adapter.notifyItemChanged(lastIndex+1)
                        adapter.notifyItemRangeChanged(lastIndex+1, relatedNewsList.size)
                        adapter.notifyItemRangeInserted(lastIndex+1, relatedNewsList.size)

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
                    }
                }

                override fun onFailure(call: Call<NewsDetail>, t: Throwable) {
                    Log.d("errorMessage", t.message.toString())
                }

            })
        }, 1000)

    }

    // 상세보기 company 관련 뉴스 추가 로드
    private fun getMoreDiscoverNewsDetail(service: Call<List<NewsDetail>>, lastid : Int) {

        Handler(Looper.getMainLooper()).postDelayed({
            service.enqueue(object : Callback<List<NewsDetail>> {
                override fun onResponse(
                    call: Call<List<NewsDetail>>,
                    response: Response<List<NewsDetail>>
                ) {


                    if (response.code() == 200){
                        val result = response.body() as ArrayList<NewsDetail>

                        for (i in result.indices) {
                            relatedNewsList.add(lastIndex + i + 1, result[i])
                        }

                        adapter.notifyItemChanged(lastIndex+1)
                        adapter.notifyItemRangeChanged(lastIndex+1, relatedNewsList.size)
                        adapter.notifyItemRangeInserted(lastIndex+1, relatedNewsList.size)

                        adapter.notifyItemChanged(lastid)
                        adapter.notifyItemRangeChanged(lastid, relatedNewsList.size)
                        adapter.notifyItemRangeInserted(lastid, relatedNewsList.size)
                    } else {
                    }
                }

                override fun onFailure(call: Call<List<NewsDetail>>, t: Throwable) {
                    Log.d("errorMessage", t.message.toString())
                }

            })
        }, 1000)

    }

    private fun sendFirebaseLog(param: Bundle) {
        currentTime = (Calendar.getInstance().timeInMillis/1000).toInt()
        param.apply {
            this.putString("screen", "player")
            this.putInt("time", currentTime)
        }
        firebaseAnalytics.logEvent("newdio", param)
    }



}