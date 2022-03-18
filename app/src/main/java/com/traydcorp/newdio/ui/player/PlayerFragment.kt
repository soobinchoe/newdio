package com.traydcorp.newdio.ui.player

import android.app.Dialog
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.res.ColorStateList
import android.content.res.Resources
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.bumptech.glide.Glide
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.traydcorp.newdio.R
import com.traydcorp.newdio.databinding.FragmentPlayerBinding
import com.traydcorp.newdio.utils.SharedPreference
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.*
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.KeyEvent
import java.util.concurrent.TimeUnit
import kotlin.collections.ArrayList
import androidx.appcompat.app.AlertDialog
import androidx.core.os.ConfigurationCompat
import com.traydcorp.newdio.utils.retofitService.RetrofitService
import retrofit2.Retrofit
import android.widget.ImageView
import android.widget.Toast
import androidx.annotation.Dimension
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.content.ContextCompat
import androidx.fragment.app.setFragmentResultListener
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.gson.JsonObject
import com.traydcorp.newdio.dataModel.NewsDetail
import com.traydcorp.newdio.ui.home.HomeActivity
import com.traydcorp.newdio.utils.retrofitAPI.RetrofitClient
import java.io.File
import android.content.ComponentName
import android.content.ContentValues
import android.os.IBinder
import android.content.ServiceConnection
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.media.MediaMetadata
import android.media.MediaMetadataRetriever
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.core.app.NotificationCompat
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition

import android.graphics.BitmapFactory
import android.media.MediaPlayer
import android.text.format.DateUtils
import androidx.fragment.app.FragmentActivity
import com.google.firebase.analytics.FirebaseAnalytics
import com.traydcorp.newdio.ui.home.HomeFragment
import com.traydcorp.newdio.ui.player.PlayerService.Companion.ACTION_DELETE
import com.traydcorp.newdio.ui.player.PlayerService.Companion.ACTION_NEXT
import com.traydcorp.newdio.ui.player.PlayerService.Companion.ACTION_PLAY
import com.traydcorp.newdio.ui.player.PlayerService.Companion.ACTION_PREV
import com.traydcorp.newdio.ui.setting.SettingFragment
import com.traydcorp.newdio.utils.LoadingFragment
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.SequenceInputStream


class PlayerFragment : BottomSheetDialogFragment(), ActionPlaying, ServiceConnection {

    private var viewBinding : FragmentPlayerBinding? = null
    private val bind get() = viewBinding!!

    private val sharedPreferences = SharedPreference()
    private lateinit var retrofit: Retrofit
    private lateinit var supplementService: RetrofitService

    private lateinit var language : String
    private var access : String? = null
    private var refresh : String? = null

    private var sTime: Int = 0
    private var eTime: Int = 0
    private var oTime: Int = 0
    private val hdlr: Handler = Handler(Looper.getMainLooper())

    private lateinit var newsUrl : String
    private var playerListFragment = PlayerListFragment()
    private var playAllList : ArrayList<NewsDetail> = ArrayList()
    var relatedNewsList : ArrayList<NewsDetail> = ArrayList()
    var result = NewsDetail()
    var crawlingdata : Int? = null
    var index = 0

    private var isHeartOn = false
    private var isReturned = false

    private lateinit var bottomSheetBehavior : BottomSheetBehavior<*>
    private var bottomNavi : BottomNavigationView? = null

    private lateinit var playBtnActivity : ImageView

    private var playService: PlayerService? = null
    private var mediaSession: MediaSessionCompat? = null

    var autoPlay : Boolean = false
    var isPlay = false
    var isResume = false
    var isPlayed = false
    var isHide = false
    var playBtnClicked = false

    private lateinit var firebaseAnalytics: FirebaseAnalytics

    private var parameters = Bundle()
    private var currentTime : Int = 0
    private var preId : Int = 0

    private var loadingDialog = LoadingFragment()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        firebaseAnalytics = (activity as HomeActivity).firebaseAnalytics

        // 저장된 선호 언어가 있으면 해당 언어 불러오기, 없으면 시스템 설정 언어
        language = (activity as HomeActivity).getLanguagePreference()

        access = sharedPreferences.getShared(context, "access_token")
        refresh = sharedPreferences.getShared(context, "refresh_token")

        // access token, refresh token
        if (access != null && refresh != null){
            updateAutoPlay()
        }

        retrofit = RetrofitClient.getInstance()
        supplementService = retrofit.create(RetrofitService::class.java)

        bottomNavi = activity?.findViewById(R.id.bottomNavigation)

        // 플레이어 켜지지 않게 재생 버튼만 눌렀을 때
        if (arguments?.getString("playBtn") == null){
            bottomNavi?.visibility = View.GONE
        }

        // 플레이리스트에서 뉴스 선택 시
        setFragmentResultListener("requestKey") { requestKey, bundle ->
            playerListFragment.dismiss()
            index = bundle.getInt("index")
            val id = bundle.getInt("id")

            getPlayerDetail(supplementService.getPlayerData(access, refresh, id, language))
        }

    }

    // 자동 재생 체크
    fun updateAutoPlay() {
        access = sharedPreferences.getShared(context, "access_token")
        refresh = sharedPreferences.getShared(context, "refresh_token")

        val autoPlayResult = sharedPreferences.getShared(requireContext(), "autoPlay")
        if (autoPlayResult == null){
            autoPlay = access != null && refresh != null
        } else {
            when (autoPlayResult) {
                "true" -> autoPlay = true
                "false" -> autoPlay = false
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.R)
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        viewBinding = FragmentPlayerBinding.inflate(inflater, container, false)

        playBtnActivity = activity?.findViewById(R.id.playBtnBottom)!!

        loadingDialog.show((context as AppCompatActivity).supportFragmentManager, "loading")

        crawlingdata = arguments?.getInt("crawlingdata")

        // 라이브 플레이 리스트
        if (arguments?.getString("from") == "live") {
            if (access != null && refresh != null){
                getPlayerList(supplementService.getLivePlayerListData(access, refresh, crawlingdata!!, language))
            } else {
                getPlayerList(supplementService.getLivePlayerListData(null, null, crawlingdata!!, language))
            }
        }

        // 전체 듣기로 넘어왔을때
        if (arguments?.getSerializable("playAllList") != null){
            playAllList = arguments?.getSerializable("playAllList") as ArrayList<NewsDetail>
        }

        if (playAllList.isEmpty() && arguments?.getString("from") != "live"){
            if (access != null && refresh != null){
                getPlayerList(supplementService.getPlayerListData(access, refresh, crawlingdata!!, language))
            } else {
                getPlayerList(supplementService.getPlayerListData(null, null, crawlingdata!!, language))
            }
        }

        // 기업,산업에서 전체듣기로 넘어왔을때
        if (playAllList.isNotEmpty()){
            for (i in playAllList.indices){
                val crawlingdata = playAllList[i].crawlingdata
                val title = playAllList[i].title
                val image_url = playAllList[i].image_url
                val postDate = playAllList[i].post_date
                val newsSite = playAllList[i].news_site

                val newsDetail = NewsDetail()
                newsDetail.crawlingdata = crawlingdata
                newsDetail.title = title
                newsDetail.image_url = image_url
                newsDetail.post_date = postDate
                newsDetail.news_site = newsSite
                relatedNewsList.add(newsDetail)
            }
            if (access != null && refresh != null){
                getPlayerDetail(supplementService.getPlayerData(access, refresh, relatedNewsList[index].crawlingdata, language))
            } else {
                getPlayerDetail(supplementService.getPlayerData(null, null, relatedNewsList[index].crawlingdata, language))
            }
        }

        // 선호 글씨 크기 설정 있으면 해당 글씨로
        updateTextSize()

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
                playerLikes(supplementService.playerLikes(access, refresh, crawlingdata!!))
            } else {
                val dialog = LoginPopUpFragment()
                dialog.show(parentFragmentManager, "dialog")
            }
        }
        return bind.root
    }

    // 선호 글씨 크기 설정 있으면 해당 글씨로
    fun updateTextSize() {
        val textSize = sharedPreferences.getShared(requireContext(), "textSize")
        if (textSize != null) {
            when (textSize) {
                "small" ->  13F
                "original" ->  15F
                "large" ->  18F
                else -> null
            }?.let {
                bind.newsContent.setTextSize(Dimension.SP, it)
            }
        }
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 플레이 리스트 버튼 클릭
        bind.listBtn.setOnClickListener {
            getPlayList()
        }

        // 플레이 버튼 클릭
        bind.playBtn.setOnClickListener {

            if (access != null && refresh != null){
                playClicked()
            } else {
                val dialog = LoginPopUpFragment()
                dialog.show(parentFragmentManager, "dialog")
            }

        }

        // 다음버튼 클릭
        bind.playRight.setOnClickListener {
            nextClicked(null)
        }

        // 이전버튼 클릭
        bind.playLeft.setOnClickListener {
            prevClicked(null)
        }

        // 원문보기 버튼
        bind.engBtn.setOnClickListener {
            parameters = Bundle().apply {
                this.putString("action", "click")
                this.putString("type", "original")
                this.putInt("id", result.crawlingdata)
            }
            sendFirebaseLog(parameters)

            val playerBottomFragment = PlayerBottomFragment()

            val bundle = Bundle()
            bundle.putString("engTitle", result.eng_title)
            bundle.putString("engContent", result.eng_content)

            playerBottomFragment.arguments = bundle
            playerBottomFragment.show((context as AppCompatActivity).supportFragmentManager, "playerOriginal")
        }

        // 더보기 버튼
        bind.moreBtn.setOnClickListener {
            val moreDialog = BottomSheetDialog(requireContext())
            val view = layoutInflater.inflate(R.layout.fragment_player_more_bottom, null)
            moreDialog.setContentView(view)
            moreDialog.show()

            val shareBtn = view.findViewById<ConstraintLayout>(R.id.share)
            val reportBtn = view.findViewById<ConstraintLayout>(R.id.report)
            val moveBtn = view.findViewById<ConstraintLayout>(R.id.move)
            val cancelBtn = view.findViewById<AppCompatButton>(R.id.cancelBtn)

            // 공유하기
            shareBtn.setOnClickListener {
                parameters = Bundle().apply {
                    this.putString("action", "click")
                    this.putString("type", "more")
                    this.putInt("id", result.crawlingdata)
                    this.putInt("position", 0)
                }
                sendFirebaseLog(parameters)

                val intentInvite = Intent(Intent.ACTION_SEND)
                intentInvite.type = "text/plain"
                val subject = "[" + getString(R.string.appname) + "] " + bind.newsTitle.text.toString()
                val body = bind.newsSite.text.toString() + " | " + bind.publishTime.text.toString() + "\n\n" +
                        bind.newsContent.text.toString() + "\n\n" + "https://www.traydcorp.com/newdio"


                intentInvite.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY or
                        Intent.FLAG_ACTIVITY_NEW_DOCUMENT or
                        Intent.FLAG_ACTIVITY_MULTIPLE_TASK)
                try {
                    intentInvite.putExtra(Intent.EXTRA_SUBJECT, subject)
                    intentInvite.putExtra(Intent.EXTRA_TEXT, body)
                    startActivity(Intent.createChooser(intentInvite, "Share using"))
                } catch (e: ActivityNotFoundException) {
                }
                moreDialog.dismiss()
            }

            // 신고하기
            reportBtn.setOnClickListener {
                parameters = Bundle().apply {
                    this.putString("action", "click")
                    this.putString("type", "more")
                    this.putInt("id", result.crawlingdata)
                    this.putInt("position", 1)
                }
                sendFirebaseLog(parameters)

                moreDialog.dismiss()

                val reportFragment = ReportFragment()
                val bundle = Bundle()
                bundle.putInt("crawlingdata", crawlingdata!!)
                reportFragment.arguments = bundle
                reportFragment.show((context as AppCompatActivity).supportFragmentManager, "report")

            }

            // 사이트로 이동하기
            moveBtn.setOnClickListener {
                parameters = Bundle().apply {
                    this.putString("action", "click")
                    this.putString("type", "more")
                    this.putInt("id", result.crawlingdata)
                    this.putInt("position", 2)
                }
                sendFirebaseLog(parameters)

                moreDialog.dismiss()
                openBrowser(newsUrl)
            }

            // 취소
            cancelBtn.setOnClickListener { moreDialog.dismiss() }
        }

        // 플레기이어 view 뒤로가기 버튼
        bind.playerBackBtn.setOnClickListener {
            dialog?.hide()
            isHide = true

            setBottomNav()

            if (autoPlay || isPlayed) {
                setPlayerBar()
            }
        }

    }

    private fun sendFirebaseLog(param: Bundle) {
        currentTime = (Calendar.getInstance().timeInMillis/1000).toInt()
        param.apply {
            this.putString("screen", "player")
            this.putInt("time", currentTime)
        }
        firebaseAnalytics.logEvent("newdio", param)
    }


    // 재생목록
    fun getPlayList() {
        if(!playerListFragment.isVisible && !playerListFragment.isAdded){
            parameters = Bundle().apply {
                this.putString("action", "click")
                this.putString("type", "playlist")
                this.putInt("id", result.crawlingdata)
            }
            sendFirebaseLog(parameters)

            val bundle = Bundle()
            bundle.putSerializable("list", relatedNewsList)
            bundle.putInt("index", index)
            bundle.putInt("crawlingdata", result.crawlingdata)

            if (arguments?.getString("from") == "live"){
                bundle.putString("from", "live")
            } else if (arguments?.getString("from") == "discover") {
                bundle.putString("from", "discover")
                bundle.putString("id", arguments?.getString("id"))
            }

            playerListFragment.arguments = bundle
            playerListFragment.show((context as AppCompatActivity).supportFragmentManager, "playerList")
        }
    }

    // 외부 브라우저
    private fun openBrowser(url : String) {
        val browserIntent = Intent(
            Intent.ACTION_VIEW,
            Uri.parse(url)
        )
        startActivity(browserIntent)
    }


    // 플레이어 바 셋팅
    fun setPlayerBar() {
        parameters = Bundle().apply {
            this.putString("action", "foreground")
            this.putString("type", "player")
            this.putInt("id", result.crawlingdata)
        }
        sendFirebaseLog(parameters)

        val playerBar = activity?.findViewById<ConstraintLayout>(R.id.playerBar)
        val playerBarNewsImage = activity?.findViewById<ImageView>(R.id.newsImageBottom)
        val playerBarNewsSite = activity?.findViewById<TextView>(R.id.newsSiteBottom)
        val playerBarNewsTitle = activity?.findViewById<TextView>(R.id.newsTitleBottom)

        playerBarNewsTitle?.text = result.title
        playerBarNewsSite?.text = result.news_site
        if (playerBarNewsImage != null) {
            Glide.with(requireContext())
                .load(result.image_url).centerCrop()
                .into(playerBarNewsImage)
        }

        if (arguments?.getString("key") != "search") {
            playerBar?.visibility = View.VISIBLE
        }

        setBottomNav()
    }


    // bottom sheet dialog 에서 BACK 처리
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return super.onCreateDialog(savedInstanceState).apply {
            setOnKeyListener { _: DialogInterface, keyCode: Int, keyEvent: KeyEvent ->
                if (keyCode == KeyEvent.KEYCODE_BACK && keyEvent.action == KeyEvent.ACTION_UP && bind.playerBackBtn.isClickable) {
                    dialog?.hide()
                    isHide = true
                    if (autoPlay || isPlayed) {
                        setPlayerBar()
                    }
                    setBottomNav()


                    return@setOnKeyListener true
                }
                return@setOnKeyListener false
            }
        }
    }


    override fun onDetach() {
        super.onDetach()
        setBottomNav()

    }

    // 플레이어 창 내려갈때 bottom navi
    private fun setBottomNav() {
        val key = arguments?.getString("from")
        if (key != "discover" && key != "favorite"){
            bottomNavi?.visibility = View.VISIBLE
        }
    }


    // player likes api 호출
    private fun playerLikes(service: Call<JsonObject>) {

        Handler(Looper.getMainLooper()).postDelayed({
            service.enqueue(object : Callback<JsonObject> {
                override fun onResponse(call: Call<JsonObject>, response: Response<JsonObject>) {
                    Log.d("response body", response.body().toString())
                    if (response.code() == 200){

                        currentTime = (Calendar.getInstance().timeInMillis/1000).toInt()
                        parameters = Bundle().apply {
                            this.putString("action", "click")
                            this.putString("type", "like")
                            this.putInt("id", id)
                        }

                        if (response.body()?.get("code").toString() == "0"){
                            relatedNewsList[index].user_likes = false
                            isHeartOn = false
                            parameters.putInt("like", 0)
                        }

                        if (response.body()?.get("code").toString() == "1") {
                            relatedNewsList[index].user_likes = true
                            isHeartOn = true
                            parameters.putInt("like", 1)
                        }

                        sendFirebaseLog(parameters)

                        // 액세스 토큰 만료시 새로 받은 토큰으로 업데이트
                        val newAccess = response.headers()["Authorization"]
                        if (newAccess != null) {
                            sharedPreferences.setShared("access_token", newAccess, requireContext())
                        }
                    }
                }

                override fun onFailure(call: Call<JsonObject>, t: Throwable) {

                    bind.heartBtn.backgroundTintList = when (isHeartOn) {
                        true -> ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.white))
                        else -> ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.light_green))
                    }

                    if(relatedNewsList[index].user_likes) {
                        relatedNewsList[index].user_likes = true
                        isHeartOn = true
                        bind.heartBtn.setBackgroundResource(R.drawable.ic_general_heart_on)
                    } else {
                        relatedNewsList[index].user_likes = false
                        isHeartOn = false
                        bind.heartBtn.setBackgroundResource(R.drawable.ic_general_heart_off)
                    }
                    val builder = AlertDialog.Builder(requireContext())
                    builder.setMessage(getString(R.string.error_network_subtitle))
                        .setTitle(getString(R.string.error_network_title))
                        .setPositiveButton(getString(R.string.popup_confirm),
                            DialogInterface.OnClickListener { dialog, id ->

                            })
                    val alertDialog = builder.create()
                    alertDialog.show()
                }

            })
        }, 1000)
    }


    // player list api 호출
    private fun getPlayerList(service: Call<NewsDetail>) {

        Handler(Looper.getMainLooper()).postDelayed({
            service.enqueue(object : Callback<NewsDetail> {
                override fun onResponse(
                    call: Call<NewsDetail>,
                    response: Response<NewsDetail>
                ) {
                    if (response.code() == 200){
                        //result.isPlaying = true
                        result = response.body()!!

                        // 선택된 뉴스를 리스트 제일 첫번째로
                        val selectedNews = NewsDetail()
                        relatedNewsList = result.related_news_list!!

                        selectedNews.crawlingdata = result.crawlingdata
                        selectedNews.news_site = result.news_site
                        selectedNews.eng_title = result.eng_title
                        selectedNews.news_url = result.news_url
                        selectedNews.eng_content = result.eng_content
                        selectedNews.post_date = result.post_date
                        selectedNews.title = result.title
                        selectedNews.long_summarized_content = result.long_summarized_content
                        selectedNews.audio_file = result.audio_file
                        selectedNews.image_url = result.image_url
                        selectedNews.isPlaying = true
                        selectedNews.user_likes = result.user_likes
                        relatedNewsList.add(0, selectedNews)

                        bindRelatedNewsList(0, selectedNews)

                        // 토큰 검증 후 에러시 로그아웃 처리
                        val header = response.headers()["Token-Error"]
                        if (header != null) {
                            sharedPreferences.sharedClear(requireContext(), "access_token")
                            sharedPreferences.sharedClear(requireContext(), "refresh_token")
                            sharedPreferences.sharedClear(requireContext(), "language")
                            sharedPreferences.sharedClear(requireContext(), "textSize")
                            sharedPreferences.sharedClear(requireContext(), "userEmail")
                            sharedPreferences.sharedClear(requireContext(), "provider")
                            Toast.makeText(context, getString(R.string.common_logout), Toast.LENGTH_SHORT).show()
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

                override fun onFailure(call: Call<NewsDetail>, t: Throwable) {
                    errorFragment(null)
                }

            })
        }, 1000)

    }

    // player detail api 호출
    private fun getPlayerDetail(service: Call<NewsDetail>) {

        Handler(Looper.getMainLooper()).postDelayed({
            service.enqueue(object : Callback<NewsDetail> {
                override fun onResponse(
                    call: Call<NewsDetail>,
                    response: Response<NewsDetail>
                ) {
                    if (response.code() == 200){
                        result.isPlaying = true
                        result = response.body()!!
                        if (playAllList.isNotEmpty()){
                            relatedNewsList[index].audio_file = result.audio_file
                        }

                        bindRelatedNewsList(index, result)

                        // 토큰 검증 후 에러시 로그아웃 처리
                        val header = response.headers()["Token-Error"]
                        if (header != null) {
                            sharedPreferences.sharedClear(requireContext(), "access_token")
                            sharedPreferences.sharedClear(requireContext(), "refresh_token")
                            sharedPreferences.sharedClear(requireContext(), "language")
                            sharedPreferences.sharedClear(requireContext(), "textSize")
                            sharedPreferences.sharedClear(requireContext(), "userEmail")
                            sharedPreferences.sharedClear(requireContext(), "provider")
                            Toast.makeText(context, getString(R.string.common_logout), Toast.LENGTH_SHORT).show()
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

                override fun onFailure(call: Call<NewsDetail>, t: Throwable) {
                    errorFragment(null)
                }

            })
        }, 1000)

    }

    // 현재 재생 시간 업데이트
    private val updatePlayTime: Runnable = object : Runnable {
        override fun run() {
            if (playService?.mediaPlayer?.currentPosition != null) {
                sTime = playService?.mediaPlayer?.currentPosition!!
            } else {
                sTime = 0
            }
            bind.progressTime.text = timerFormat(sTime)

            hdlr.postDelayed(this, 100)
        }
    }

    // 플레이어 view 업데이트
    fun bindRelatedNewsList(i: Int, result: NewsDetail){
        Log.d("bindRelatedNewsList", "call")

        // 실행중인 뉴스가 있으면 media stop
        if (playService?.mediaPlayer != null) {
            if (playService!!.mediaPlayer!!.isPlaying) {
                playService?.stopMedia()
                showNotification(android.R.drawable.ic_media_play, 0F)

                currentTime = (Calendar.getInstance().timeInMillis/1000).toInt()
                parameters = Bundle().apply {
                    this.putString("action", "auto_click")
                    this.putString("type", "pause")
                    this.putInt("id", preId)
                    this.putInt("cur_time", sTime)
                    this.putInt("max_time", eTime)
                }
                sendFirebaseLog(parameters)
            }
        }

        val filename = "readNews"
        val fileContents = result.crawlingdata.toString()
        val readNews = File(context?.filesDir, filename)
        //readNews.delete()

        if (!readNews.exists()) { // 읽었던 뉴스 파일이 없을 때 새로 생성
            context?.openFileOutput(filename, Context.MODE_PRIVATE).use {
                it?.write(fileContents.toByteArray())
            }
        } else {
            // 읽었던 뉴스 리스트 가져오기
            val readNewsList = ArrayList<String>()
            readNews.readLines().forEach {
                readNewsList.add(it)
            }
            // 현재 뉴스가 읽었던 뉴스 리스트에 없으면 추가
            if (!readNewsList.contains(fileContents)){
                context?.openFileOutput(filename, Context.MODE_APPEND).use {
                    it?.write(System.getProperty("line.separator").toByteArray())
                    it?.write(fileContents.toByteArray())
                }
            }
        }

        hdlr.removeCallbacks(updatePlayTime)
        bind.endTime.text = "00:00"
        bind.progressTime.text = "00:00"

        bind.playBtn.setImageResource(R.drawable.ic_player_stop)
        isPlay = false
        isResume = false


        // 음성파일 시간 업데이트
        Handler(Looper.getMainLooper()).postDelayed({
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val retriever = MediaMetadataRetriever()
                    retriever.setDataSource(result.audio_file)
                    val time = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)

                    eTime = time!!.toLong().toInt()
                } else {
                    Log.d(ContentValues.TAG, "get duration Build.VERSION: " + Build.VERSION.SDK_INT)
                    val mp = MediaPlayer.create(activity?.applicationContext, Uri.parse(result.audio_file))
                    eTime = mp.duration.toLong().toInt()
                }


                if (oTime == 0) {
                    oTime = 1
                }

                bind.endTime.text = timerFormat(eTime)
            } catch (e: Exception) {
                Log.d("eTime", e.stackTraceToString() )
            }
            bind.progressTime.text = "00:00"}, 100)

        // 다음 플레이어로 넘어갈 때 원문보기 켜져 있으면 종료
        if (activity?.supportFragmentManager?.findFragmentByTag("playerOriginal") != null){
            val playerBottomFragment = requireActivity().supportFragmentManager.findFragmentByTag("playerOriginal") as PlayerBottomFragment
            playerBottomFragment.dismiss()
        }

        bind.engBtn.visibility = View.VISIBLE
        bind.toolbar.visibility = View.VISIBLE
        bind.newsPlayer.visibility = View.VISIBLE

        bind.newsSite.text = result.news_site
        bind.publishTime.text = (activity as HomeActivity).getDateFormatter("yyyy.MM.dd aa HH:mm", result.post_date!!)
        bind.newsTitle.text = result.title
        bind.newsContent.text = result.long_summarized_content
        if (result.image_url != null){
            view?.let {
                Glide.with(it)
                    .load(result.image_url).centerCrop()
                    .into(bind.newsImage)
            }
        }

        // 현재 재생 중인 뉴스 업데이트
        for (i in relatedNewsList.indices) {
            relatedNewsList[i].isPlaying = false
        }
        relatedNewsList[i].isPlaying = true

        index = i
        crawlingdata = result.crawlingdata
        newsUrl = result.news_url.toString()

        // 좋아요
        if (result.user_likes){
            bind.heartBtn.setBackgroundResource(R.drawable.ic_general_heart_on)
            isHeartOn = true
        } else if (!result.user_likes) {
            bind.heartBtn.setBackgroundResource(R.drawable.ic_general_heart_off)
            isHeartOn = false
        }

        // 좋아요 색상 변경
        bind.heartBtn.backgroundTintList = when (isHeartOn) {
            true -> activity?.applicationContext?.let {
                ContextCompat.getColor(
                    it, R.color.light_green)
            }?.let { ColorStateList.valueOf(it) }
            else -> activity?.applicationContext?.let {
                ContextCompat.getColor(
                    it, R.color.white)
            }?.let { ColorStateList.valueOf(it) }
        }

        bind.background.visibility = View.GONE

        currentTime = (Calendar.getInstance().timeInMillis/1000).toInt()
        parameters = Bundle().apply {
            this.putString("action", "view")
            this.putString("type", "player")
            this.putInt("id", result.crawlingdata)
        }
        sendFirebaseLog(parameters)

        if (loadingDialog.isVisible){
            loadingDialog.dismiss()
        }


        // 로그인 체크
        if (access != null && refresh != null){
            if (playService == null) {
                connectMediaService()
            } else if (playService != null) {
                playMedia(result.audio_file!!)
            }
        }

        // 현재 목록의 마지막 뉴스면 다음 20개 불러오기
        if (index == relatedNewsList.size-1 && index != 0) {
            val bundle = Bundle()
            bundle.putSerializable("list", relatedNewsList)
            bundle.putInt("index", index)
            if (arguments?.getString("from") == "live"){
               bundle.putString("from", "live")
                playerListFragment.arguments = bundle
                playerListFragment.loadMoreNews()
            } else {
                playerListFragment.arguments = bundle
                playerListFragment.loadMoreNews()
            }
        }

    }


    // 타이머 포맷 변환
    private fun timerFormat(time: Int) : String {
        return String.format("%02d:%02d", TimeUnit.MILLISECONDS.toMinutes(time.toLong()),
            TimeUnit.MILLISECONDS.toSeconds(time.toLong()) - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(time.toLong())))

    }


    // 에러 페이지
    private fun errorFragment(serverError : String?) {
        isReturned = false
        Handler(Looper.getMainLooper()).postDelayed({
            if (loadingDialog.isVisible) {
                bind.errorFragment.visibility = View.VISIBLE
                bind.toolbar.visibility = View.GONE
                bind.newsPlayer.visibility = View.GONE
                bind.engBtn.visibility = View.GONE
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
            loadingDialog.show((context as AppCompatActivity).supportFragmentManager, "loading")

            if (access != null && refresh != null){
                getPlayerList(supplementService.getPlayerListData(access, refresh, crawlingdata!!, language))
            } else {
                getPlayerList(supplementService.getPlayerListData(null, null, crawlingdata!!, language))
            }
            refreshView()
        }
    }


    // 에러뷰에서 다시 시도 후 view refresh
    private fun refreshView() {
        requireActivity().supportFragmentManager
            .beginTransaction()
            .detach(this)
            .attach(this)
            .commit()

        bind.background.visibility = View.VISIBLE
        bind.errorFragment.visibility = View.GONE
    }

    fun disconnectMedia() {
        parameters = Bundle().apply {
            this.putString("action", "auto_click")
            this.putString("type", "pause")
            this.putInt("id", result.crawlingdata)
            this.putInt("cur_time", sTime)
            this.putInt("max_time", eTime)
        }
        sendFirebaseLog(parameters)

        // 재생 시간 업데이트 handler & 플레이어 서비스 연결 해제
        hdlr.removeCallbacks(updatePlayTime)
        playService?.onDestroy()
        removeNotification()

        parameters = Bundle().apply {
            this.putString("action", "end")
            this.putString("type", "player")
            this.putInt("id", result.crawlingdata)
        }
        sendFirebaseLog(parameters)
    }

    // 다음 버튼
    override fun nextClicked(background : String?) {
        super.nextClicked(background)
        hdlr.removeCallbacks(updatePlayTime)

        // 재생 버튼 초기화
        //bind.playBtn.setImageResource(R.drawable.ic_player_stop)
        isPlay = false
        isResume = false

        if (index < relatedNewsList.size-1){
            index ++
        }

        if (background == "onComplete") {
            if (!autoPlay) {

                bind.playBtn.setImageResource(R.drawable.ic_player_stop)
                playBtnActivity.setImageResource(R.drawable.ic_general_play)
                isPlay = false
                showNotification(android.R.drawable.ic_media_play, 0F)
                return
            }
        }


        parameters = Bundle().apply {
            if (autoPlay) {
                this.putString("action", "auto_click")
                this.putString("type", "player")
            } else {
                this.putString("action", "click")
                this.putString("type", "next")
            }
            this.putInt("id", result.crawlingdata)
            this.putInt("to_id", relatedNewsList[index].crawlingdata)
        }
        sendFirebaseLog(parameters)

        preId = result.crawlingdata

        // 다음 뉴스 데이터 불러오기
        getPlayerDetail(supplementService.getPlayerData(access, refresh, relatedNewsList[index].crawlingdata, language))

    }

    // 이전 버튼
    override fun prevClicked(background : String?) {
        super.prevClicked(background)

        if (index > 0){
            hdlr.removeCallbacks(updatePlayTime)

            index --

            currentTime = (Calendar.getInstance().timeInMillis/1000).toInt()
            parameters = Bundle().apply {
                this.putString("action", "click")
                this.putString("type", "previous")
                this.putInt("id", result.crawlingdata)
                this.putInt("to_id", relatedNewsList[index].crawlingdata)
            }
            sendFirebaseLog(parameters)

            preId = result.crawlingdata
            // 재생 버튼 초기화
            bind.playBtn.setImageResource(R.drawable.ic_player_stop)
            isPlay = false
            isResume = false

            // 이전 뉴스 데이터 불러오기
            getPlayerDetail(supplementService.getPlayerData(access, refresh, relatedNewsList[index].crawlingdata, language))

        }
    }

    // 재생 버튼
    override fun playClicked() {
        super.playClicked()

        // 플레이어 재생
        if (!isPlay && !isResume) { // 첫 재생
            Log.d("첫재생", "call")
            playBtnClicked = true
            if (playService == null) {
                connectMediaService()
            } else {
                playMedia(relatedNewsList[index].audio_file!!)
            }
        } else if(!isPlay && isResume) { // 일시정지 후 재생
            Log.d("일시정지 후 재생", "call")
            playService?.resumeMedia()

            currentTime = (Calendar.getInstance().timeInMillis/1000).toInt()
            parameters = Bundle().apply {
                this.putString("action", "click")
                this.putString("type", "play")
                this.putInt("id", result.crawlingdata)
                this.putInt("cur_time", sTime)
                this.putInt("max_time", eTime)
            }
            sendFirebaseLog(parameters)

            bind.playBtn.setImageResource(R.drawable.ic_player_play)
            playBtnActivity.setImageResource(R.drawable.ic_general_stop_white)
            isPlay = true
            showNotification(android.R.drawable.ic_media_pause, 1F)
            hdlr.postDelayed(updatePlayTime, 100)
        } else if (isPlay){ // 일시정지
            Log.d("일시정지", "call")
            playService?.pauseMedia()

            currentTime = (Calendar.getInstance().timeInMillis/1000).toInt()
            parameters = Bundle().apply {
                this.putString("action", "click")
                this.putString("type", "pause")
                this.putInt("id", result.crawlingdata)
                this.putInt("cur_time", sTime)
                this.putInt("max_time", eTime)
            }
            sendFirebaseLog(parameters)

            bind.playBtn.setImageResource(R.drawable.ic_player_stop)
            playBtnActivity.setImageResource(R.drawable.ic_general_play)
            isPlay = false
            showNotification(android.R.drawable.ic_media_play, 0F)
            hdlr.removeCallbacks(updatePlayTime)
        }
    }

    // 플레이어 정지 (알림바 취소)
    override fun stopClicked() {
        super.stopClicked()
        hdlr.removeCallbacks(updatePlayTime)
        disconnectMedia()
        playService?.mediaPlayer = null
        bind.playBtn.setImageResource(R.drawable.ic_player_stop)
        playBtnActivity.setImageResource(R.drawable.ic_general_play)
        isPlay = false
        isResume = false
        bind.progressTime.text = "00:00"
    }

    // player service와 연결
    fun connectMediaService() {
        val playerIntent = activity?.applicationContext?.let {Intent(it, PlayerService::class.java)}

        if (playService == null) {
            activity?.let {(it as HomeActivity).startService(playerIntent)}
        }

        activity?.let {(it as HomeActivity).bindService(playerIntent, this, Context.BIND_AUTO_CREATE)}
        mediaSession = activity?.applicationContext?.let {MediaSessionCompat(it, "AudioPlayer")}
    }

    fun playMedia(mediaFile: String) {

        playService?.createMediaPlayer(mediaFile)

        if (autoPlay && playService?.isPlaying == true) {
            bind.playBtn.setImageResource(R.drawable.ic_player_play)
            playBtnActivity.setImageResource(R.drawable.ic_general_stop_white)
            hdlr.postDelayed(updatePlayTime, 100)
            isPlay = true
            isResume = true

            // 홈화면 플레이어 탭바
            setPlayerBar()
            isPlayed = true
        } else if (!autoPlay) {
            if (playBtnClicked) {
                playService?.playBtn = true
                playBtnClicked = false
                bind.playBtn.setImageResource(R.drawable.ic_player_play)
                playBtnActivity.setImageResource(R.drawable.ic_general_stop_white)
                hdlr.postDelayed(updatePlayTime, 100)
                isPlay = true
                isResume = true

                // 홈화면 플레이어 탭바
                setPlayerBar()
                isPlayed = true
            }
        }
    }

    override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
        val binder = service as PlayerService.LocalBinder
        playService = binder.service
        playService!!.setCallBack(this)

        relatedNewsList[index].audio_file?.let { playMedia(it) }
    }

    override fun onServiceDisconnected(name: ComponentName?) {
        playService = null
    }

    override fun playPrepared(play : Boolean) {
        super.playPrepared(play)
        if (play) {
            showNotification(android.R.drawable.ic_media_pause, 1F)
            currentTime = (Calendar.getInstance().timeInMillis/1000).toInt()
            parameters = Bundle().apply {
                this.putString("action", "auto_click")
                this.putString("type", "play")
                this.putInt("id", result.crawlingdata)
                this.putInt("cur_time", 0)
                this.putInt("max_time", eTime)
            }
            sendFirebaseLog(parameters)
        } else {
            showNotification(android.R.drawable.ic_media_play, 0F)
        }
        setPlayerBar()
    }

    val CHANNEL_ID = "NEWDIO"
    val NOTIFICATION_ID = 111

    // notification 이미지 center crop
    fun centerCropBitmap(src : Bitmap): Bitmap? {
        val width = src.width
        val height = src.height

        var x = 0
        var y = 0

        if(width >= height) {
            x = width/2 - height/2
            return Bitmap.createBitmap(src, x, y, height, height)
        } else {
            y = height/2 - width/2
            return Bitmap.createBitmap(src, x, y, width, width)
        }
    }

    fun showNotification(playPauseBtn : Int, playBackSpeed: Float) {
        // 뉴디오 기본 아이콘
        val icon = BitmapFactory.decodeResource(activity?.applicationContext?.resources, R.drawable.ic_general_newdio)

        Glide.with(activity?.applicationContext!!).asBitmap()
            .load(relatedNewsList[index].image_url).centerCrop()
            .into(object: CustomTarget<Bitmap>() {
                override fun onLoadCleared(placeholder: Drawable?) {
                }

                override fun onLoadFailed(errorDrawable: Drawable?) {
                    super.onLoadFailed(errorDrawable)
                    // 이미지 불러오기 실패 시 기본 이미지로 설정
                    buildNotification(icon, playPauseBtn)
                }
                override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                    buildNotification(centerCropBitmap(resource)!!, playPauseBtn)
                }
            })

        mediaSession!!.setMetadata(
            MediaMetadataCompat.Builder()
                .putLong(
                    MediaMetadata.METADATA_KEY_DURATION,
                    playService?.mediaPlayer?.duration!!.toLong()
                )
                .build()
        )
        mediaSession!!.setPlaybackState(PlaybackStateCompat.Builder()
            .setState(
                PlaybackStateCompat.STATE_PLAYING,
                playService?.mediaPlayer?.currentPosition!!.toLong(),
                playBackSpeed
            )
            .build()
        )

    }

    private fun buildNotification(bmp : Bitmap, playPauseBtn : Int) {

        // notification 채널 생성
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name: CharSequence = "newdio"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val mChannel = NotificationChannel(CHANNEL_ID, name, importance)
            activity?.applicationContext?.let {(it.getSystemService(Service.NOTIFICATION_SERVICE) as NotificationManager).createNotificationChannel(mChannel)}
        }

        val intent = Intent(context, HomeActivity::class.java)
        intent.flags = (Intent.FLAG_ACTIVITY_NEW_TASK)


        val contentIntent : PendingIntent? = PendingIntent.getActivity(activity?.applicationContext, 0, intent, PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)


        val prevIntent = Intent(activity?.applicationContext, NotificationReceiver::class.java).setAction(ACTION_PREV)
        val prevPendingIntent = PendingIntent.getBroadcast(requireContext(), 0, prevIntent, PendingIntent.FLAG_UPDATE_CURRENT)

        val playIntent = Intent(activity?.applicationContext, NotificationReceiver::class.java).setAction(ACTION_PLAY)
        val playPendingIntent = PendingIntent.getBroadcast(requireContext(), 0, playIntent, PendingIntent.FLAG_UPDATE_CURRENT)

        val nextIntent = Intent(activity?.applicationContext, NotificationReceiver::class.java).setAction(ACTION_NEXT)
        val nextPendingIntent = PendingIntent.getBroadcast(requireContext(), 0, nextIntent, PendingIntent.FLAG_UPDATE_CURRENT)


        val notificationBuilder: NotificationCompat.Builder = NotificationCompat.Builder(activity!!.applicationContext, CHANNEL_ID)
            .setShowWhen(true) // Set the Notification style
            .setStyle(
                androidx.media.app.NotificationCompat.MediaStyle()
                    .setMediaSession(mediaSession!!.sessionToken) // Show our playback controls in the compact notification view.
                    .setShowActionsInCompactView(0, 1, 2)
            )
            .setLargeIcon(bmp)
            .setSmallIcon(R.drawable.ic_stat_ic_general_newdio) // Set Notification content information
            .setContentText(relatedNewsList[index].news_site)
            .setContentTitle(relatedNewsList[index].title)
            .setOnlyAlertOnce(true)
            .setContentIntent(contentIntent)
            .addAction(android.R.drawable.ic_media_previous, "previous", prevPendingIntent)
            .addAction(playPauseBtn, "pause", playPendingIntent)
            .addAction(android.R.drawable.ic_media_next, "next", nextPendingIntent)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val deleteIntent = Intent(activity?.applicationContext, NotificationReceiver::class.java).setAction(ACTION_DELETE)
            val deletePendingIntent = PendingIntent.getBroadcast(requireContext(), 0, deleteIntent, PendingIntent.FLAG_UPDATE_CURRENT)
            notificationBuilder.setDeleteIntent(deletePendingIntent)
        }

        (activity?.applicationContext?.getSystemService(Service.NOTIFICATION_SERVICE) as NotificationManager).notify(
            NOTIFICATION_ID, notificationBuilder.build()
        )


    }

    private fun removeNotification() {
        val notificationManager = activity?.getSystemService(Service.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(NOTIFICATION_ID)
    }

    override fun onResume() {
        super.onResume()

        if (arguments?.getString("playBtn") != null && isPlayed){
            bottomNavi?.visibility = View.VISIBLE
        }

    }

    // 플레이어 전체화면으로 보이게
    override fun onStart() {
        super.onStart()

        if(isHide) {
            dialog?.hide()
        }

        if (arguments?.getString("playBtn") != null){
            dialog?.hide()
            isHide = true
        }

        dialog?.let {
            val bottomSheet: View = dialog!!.findViewById(R.id.design_bottom_sheet)
            bottomSheet.layoutParams.height = ViewGroup.LayoutParams.MATCH_PARENT
            isCancelable = false
        }

        val view = view
        view!!.post{
            val parent = view.parent as View
            val params = parent.layoutParams as CoordinatorLayout.LayoutParams
            val behavior = params.behavior
            bottomSheetBehavior = (behavior as BottomSheetBehavior<*>?)!!
            bottomSheetBehavior.peekHeight = view.measuredHeight

            parent.setBackgroundColor(Color.TRANSPARENT)
        }
    }





}