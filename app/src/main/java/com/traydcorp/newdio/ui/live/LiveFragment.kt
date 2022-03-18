package com.traydcorp.newdio.ui.live


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
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import androidx.core.os.ConfigurationCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.analytics.FirebaseAnalytics
import com.traydcorp.newdio.R
import com.traydcorp.newdio.databinding.FragmentLiveBinding
import com.traydcorp.newdio.ui.player.PlayerFragment
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import com.traydcorp.newdio.dataModel.NewsDetail
import com.traydcorp.newdio.ui.discover.DiscoverDetailFragment
import com.traydcorp.newdio.ui.home.HomeActivity
import com.traydcorp.newdio.utils.LoadingFragment
import com.traydcorp.newdio.utils.SharedPreference
import com.traydcorp.newdio.utils.retofitService.RetrofitService
import com.traydcorp.newdio.utils.retrofitAPI.RetrofitClient
import retrofit2.Retrofit
import java.util.*
import kotlin.collections.ArrayList


class LiveFragment : Fragment() {

    companion object {
        fun newInstance() = LiveFragment()
    }

    private var viewBinding: FragmentLiveBinding? = null
    private val bind get() = viewBinding!!

    private val sharedPreferences = SharedPreference()
    private lateinit var retrofit: Retrofit
    private lateinit var supplementService: RetrofitService

    private lateinit var language : String

    private lateinit var adapter : LiveListAdapter
    private var result = ArrayList<NewsDetail>()

    private var bundle = Bundle()

    private var loadingDialog = LoadingFragment()

    private lateinit var firebaseAnalytics: FirebaseAnalytics

    private var parameters = Bundle()
    private var currentTime : Int = 0

    private var isRefresh = false
    private var preId = 0
    private var lastId = 0

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
    ): View? {
        viewBinding = FragmentLiveBinding.inflate(inflater, container, false)
        bind.liveToolbar.visibility = View.VISIBLE
        bind.liveToolbarText.text = getString(R.string.menu_live)

        loadingDialog.show((context as AppCompatActivity).supportFragmentManager, "loading")

        getNewLiveList(null)

        val liveInfoText = getString(R.string.live_info_1) + "\n" + getString(R.string.live_info_2)

        // help button 눌렀을 때
        var dialog = InfoDialogFragment()
        bind.toolbarHelp.setOnClickListener {
            bind.toolbarHelp.setImageResource(R.drawable.ic_general_help_on)
            if (dialog.isAdded){
                activity?.supportFragmentManager?.beginTransaction()?.remove(dialog)!!.commit()
                dialog = InfoDialogFragment()
            }

            bundle.putString("key", liveInfoText)
            bundle.putString("from", "live")
            dialog.arguments = bundle
            dialog.show(parentFragmentManager, "dialog")

            // dismiss 될 때 아이콘 다시 변경
            activity?.supportFragmentManager?.executePendingTransactions()
            dialog.dialog?.setOnDismissListener {
                activity?.supportFragmentManager?.beginTransaction()?.remove(dialog)!!.commit()
                bind.toolbarHelp.setImageResource(R.drawable.ic_general_help_off)
            }
        }

        // live 리스트 맨 끝까지 갔을 때 last id로 다음 리스트 불러오기
        bind.liveListRcy.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                val lastPosition =
                    (recyclerView.layoutManager as LinearLayoutManager).findLastCompletelyVisibleItemPosition()
                // 스크롤이 더이상 내려가지 않을 때 마지막 아이템 = lastid
                if (!bind.liveListRcy.canScrollVertically(1)){
                    val lastid = result[lastPosition].crawlingdata
                    currentTime = (Calendar.getInstance().timeInMillis/1000).toInt()
                    parameters = Bundle().apply {
                        this.putString("screen", "live")
                        this.putString("action", "drag")
                        this.putInt("time", currentTime)
                        this.putString("type", "next_page")
                        this.putInt("id", lastid)
                    }
                    firebaseAnalytics.logEvent("newdio", parameters)
                    getNewLiveList(lastid)
                }
            }
        })

        return bind.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        // 새로 고침
        bind.swipeRefresh.setOnRefreshListener {
            currentTime = (Calendar.getInstance().timeInMillis/1000).toInt()
            parameters = Bundle().apply {
                this.putString("screen", "live")
                this.putString("action", "drag")
                this.putInt("time", currentTime)
                this.putString("type", "relode")
            }
            firebaseAnalytics.logEvent("newdio", parameters)
            isRefresh = true

            getNewLiveList(null)
            bind.swipeRefresh.isRefreshing = false

        }
    }

    fun getNewLiveList(lastid: Int?) {
        loadLiveList(supplementService.getLiveData(lastid, language), lastid)
    }

    // live api 요청
    private fun loadLiveList(service: Call<List<NewsDetail>>, lastid : Int?) {

        Handler(Looper.getMainLooper()).postDelayed({
            service.enqueue(object : Callback<List<NewsDetail>> {
                override fun onResponse(
                    call: Call<List<NewsDetail>>,
                    response: Response<List<NewsDetail>>
                ) {

                    if (response.code() == 200){
                        if (lastid == null){ // 첫 로드
                            result = response.body() as ArrayList<NewsDetail>
                            liveListRecyclerView(result)
                            bind.swipeRefresh.visibility = View.VISIBLE

                            lastId = result[result.lastIndex].crawlingdata
                            return
                        }

                        // 추가 로드
                        val newsList = response.body() as ArrayList<NewsDetail>
                        for (i in newsList.indices){
                            result.add(newsList[i])
                        }

                        adapter.notifyItemChanged(lastid)
                        adapter.notifyItemRangeChanged(lastid, newsList.size)
                        adapter.notifyItemRangeInserted(lastid, newsList.size)

                        currentTime = (Calendar.getInstance().timeInMillis/1000).toInt()
                        parameters = Bundle().apply {
                            this.putString("screen", "live")
                            this.putString("action", "view")
                            this.putInt("time", currentTime)
                            this.putString("type", "next_page")
                            this.putInt("pre_id", lastid)
                            this.putInt("id", newsList[newsList.lastIndex].crawlingdata)
                        }
                        firebaseAnalytics.logEvent("newdio", parameters)

                        lastId = newsList[newsList.lastIndex].crawlingdata

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

    private fun liveListRecyclerView(result: List<NewsDetail>?) {
        // 선호 글자 크기 가져오기
        var textSize = sharedPreferences.getShared(requireContext(), "textSize")
        if (textSize == null) {
            textSize = "original"
        }

        val layoutManager = LinearLayoutManager(context)
        adapter = LiveListAdapter(result as ArrayList<NewsDetail>?, textSize)
        bind.liveListRcy.adapter = adapter
        bind.liveListRcy.layoutManager = layoutManager

        loadingDialog.dismiss()

        currentTime = (Calendar.getInstance().timeInMillis/1000).toInt()

        parameters = Bundle().apply {
            this.putString("screen", "live")
            this.putString("action", "view")
            this.putInt("time", currentTime)
            if (isRefresh) {
                this.putString("type", "reload")
                this.putInt("id", result!![0].crawlingdata)
                this.putInt("pre_id", preId)
            }
        }
        firebaseAnalytics.logEvent("newdio", parameters)

        preId = result!![0].crawlingdata

        // live item 선택 시 player로 이동
        adapter.setItemClickListener(object : LiveListAdapter.OnItemClickListener{
            override fun onClick(v: View, position: Int, index: String?) {
                currentTime = (Calendar.getInstance().timeInMillis/1000).toInt()
                parameters = Bundle().apply {
                    this.putString("screen", "live")
                    this.putString("action", "click")
                    this.putInt("time", currentTime)
                    this.putInt("id", result[position].crawlingdata)
                }

                val bundle = Bundle()
                // adapter에서 live news인지 기업 정보인지 판단
                if (index == null) { // live news
                    parameters.putString("type", "live")
                    parameters.putInt("last_id", lastId)

                    val crawlingdata = result[position].crawlingdata
                    bundle.putInt("crawlingdata", crawlingdata)
                    bundle.putString("from", "live")

                    (activity as HomeActivity).getPlayer(bundle)
                } else { // 기업 상세정보
                    parameters.putString("type", "company")
                    parameters.putString("co_id", index)

                    bundle.putString("id", index)
                    bundle.putString("from", "live")
                    (activity as HomeActivity).getDiscoverDetail(bundle)
                }
                firebaseAnalytics.logEvent("newdio", parameters)
            }
        })

    }

    // 에러 페이지
    private fun errorFragment(serverError : String?) {
        if (loadingDialog.isVisible){
            loadingDialog.dismiss()
        }
        bind.errorFragment.visibility = View.VISIBLE
        bind.swipeRefresh.visibility = View.GONE

        val errorText = bind.errorFragment.findViewById<TextView>(R.id.errorText)
        val errorText2 = bind.errorFragment.findViewById<TextView>(R.id.errorText2)
        val retryBtn = bind.errorFragment.findViewById<AppCompatButton>(R.id.retryBtn)

        if (serverError == "serverError") {
            errorText.text = getString(R.string.error_server_title)
            errorText2.text = getString(R.string.error_server_subtitle)
        }

        retryBtn.setOnClickListener {
            getNewLiveList(null)
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

        bind.errorFragment.visibility = View.GONE
    }
}