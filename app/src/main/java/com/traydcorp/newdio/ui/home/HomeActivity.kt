package com.traydcorp.newdio.ui.home


import android.content.Intent
import android.content.res.Configuration
import android.content.res.Resources
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.support.v4.media.session.MediaSessionCompat
import android.util.Log
import android.view.Menu
import android.view.View
import android.widget.Toast
import androidx.core.os.ConfigurationCompat
import androidx.core.widget.NestedScrollView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.ktx.Firebase
import com.traydcorp.newdio.R
import com.traydcorp.newdio.databinding.ActivityHomeBinding
import com.traydcorp.newdio.ui.discover.DiscoverDetailFragment
import com.traydcorp.newdio.ui.discover.DiscoverFragment
import com.traydcorp.newdio.ui.live.LiveFragment
import com.traydcorp.newdio.ui.player.PlayerFragment
import com.traydcorp.newdio.ui.player.PlayerService
import com.traydcorp.newdio.ui.search.SearchFragment
import com.traydcorp.newdio.ui.setting.DialogFragment
import com.traydcorp.newdio.ui.setting.SettingFragment
import com.traydcorp.newdio.utils.SharedPreference
import java.text.SimpleDateFormat
import java.util.*

class HomeActivity : AppCompatActivity(), DialogFragment.NoticeDialogListener {

    private lateinit var viewBinding : ActivityHomeBinding

    private val home = HomeFragment()
    private val live = LiveFragment()
    private val discover = DiscoverFragment()
    private val search = SearchFragment()
    private val setting = SettingFragment()
    private var player : PlayerFragment? = null
    private var active : Fragment = home
    private var searchChild : Fragment? = null
    private var discoverSearch : Fragment? = null

    private val sharedPreferences = SharedPreference()

    private lateinit var language : String

    lateinit var firebaseAnalytics: FirebaseAnalytics

    var userId : String? = null

    var selectedItem : Int = 0

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        val reset = intent?.getStringExtra("reset")
        if(reset == "reset") {
            val intent = packageManager.getLaunchIntentForPackage(packageName)
            intent!!.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
            intent.addFlags(Intent.	FLAG_ACTIVITY_TASK_ON_HOME)
            startActivity(intent)
            finish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        firebaseAnalytics = Firebase.analytics

        userId = sharedPreferences.getShared(applicationContext, "userId")
        if (userId == null){
            userId = sharedPreferences.getShared(applicationContext, "userUUID")
        }

        // 저장된 선호 언어가 있으면 해당 언어 불러오기, 없으면 시스템 설정 언어
        val savedLanguage = sharedPreferences.getShared(applicationContext, "language")
        if (savedLanguage == null){
            language = ConfigurationCompat.getLocales(Resources.getSystem().configuration)[0].language
        } else {
            language = savedLanguage
        }

        // 앱 언어로 설정
        val locale = Locale(language)
        val config = Configuration(this.resources.configuration)
        Locale.setDefault(locale)
        config.setLocale(locale)
        this.baseContext.resources.updateConfiguration(
            config,
            this.baseContext.resources.displayMetrics
        )

        viewBinding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(viewBinding.root)

        // 첫 화면 홈으로 설정
        supportFragmentManager.beginTransaction().add(R.id.homeView, home, "newdio").show(home).commit()
        active = home
        selectedItem = R.id.homeFragment

        navigation()

        // 플레이어바
        viewBinding.playerBar.setOnClickListener {
            if (supportFragmentManager.findFragmentByTag("player") != null){
                Log.d("playbarTab", supportFragmentManager.findFragmentByTag("player").toString())
                player = supportFragmentManager.findFragmentByTag("player") as PlayerFragment
                player!!.isHide = false
                player!!.dialog?.show()
                //player.dialog?.setCancelable(true)
            }
        }

        // 플레이어바 재생목록 버튼
        viewBinding.playListBtn.setOnClickListener {
            if (supportFragmentManager.findFragmentByTag("player") != null){
                player = supportFragmentManager.findFragmentByTag("player") as PlayerFragment
                player!!.getPlayList()
                //player.dialog?.setCancelable(true)
            }
        }

        // 플레이어바 삭제 버튼
        viewBinding.playDeleteBtn.setOnClickListener {
            if (supportFragmentManager.findFragmentByTag("player") != null){
                player = supportFragmentManager.findFragmentByTag("player") as PlayerFragment
                player!!.disconnectMedia()
                player!!.dismiss()
                viewBinding.playerBar.visibility = View.GONE
            }
        }

        // 플레이어바 재생 & 일시정지 버튼
        viewBinding.playBtnBottom.setOnClickListener {
            if (supportFragmentManager.findFragmentByTag("player") != null){
                player = supportFragmentManager.findFragmentByTag("player") as PlayerFragment
                player!!.playClicked()
            }
        }

    }



    // bottom navigation
    private fun navigation(){
        val navView: BottomNavigationView = viewBinding.bottomNavigation



        navView.run {
            setOnItemSelectedListener { item ->
                val currentTime = (Calendar.getInstance().timeInMillis/1000).toInt()

                // 현재 fragment 불러오기
                val fragments: List<Fragment> = supportFragmentManager.fragments
                var currentFragment = Fragment()
                for (fragment in fragments) {
                    if (fragment.isVisible)
                        currentFragment = fragment
                }

                val parameters = Bundle()
                parameters.apply {
                    this.putString("screen", "newdio")
                    this.putString("action", "click")
                    this.putString("type", "menu")
                    this.putInt("time", currentTime)
                }

                searchChild = supportFragmentManager.findFragmentByTag("search")
                discoverSearch = supportFragmentManager.findFragmentByTag("discoverSearch")
                if (discoverSearch != null) {
                    supportFragmentManager.beginTransaction().hide(discoverSearch!!).commit()
                }

                if (currentFragment == discover){
                    val discoverFragment : DiscoverFragment = supportFragmentManager.findFragmentByTag("discover") as DiscoverFragment
                    discoverFragment.removeInfo()
                }

                // 활성화 된 fragment에서 bottom navi 눌렀을 때 위로 스크롤
                if (item.itemId == selectedItem){
                    // discover, home, setting -> NestedScrollView, live -> RecyclerView
                    if (item.itemId == R.id.liveFragment){
                        currentFragment.view?.findViewById<RecyclerView>(R.id.liveListRcy)
                            ?.smoothScrollToPosition(0)
                    } else if (item.itemId == R.id.discoverFragment || item.itemId == R.id.homeFragment || item.itemId == R.id.settingFragment){
                        currentFragment.view?.findViewById<NestedScrollView>(R.id.homeScrollView)?.smoothScrollTo(0,0)
                    }
                } else {
                    when (item.itemId) {
                        R.id.homeFragment -> {
                            parameters.putString("id", "home")
                            // bottom navi 선택 된 아이콘 변경 처리
                            iconOff(menu)
                            menu.findItem(R.id.homeFragment).setIcon(R.drawable.ic_bar_home_on)
                            selectedItem = R.id.homeFragment // 선택시 selectedItem에 저장
                            // 현재 active 탭 감추고 선택한 탭 보여주기
                            if (supportFragmentManager.findFragmentByTag("newdio") == null){
                                supportFragmentManager.beginTransaction().add(R.id.homeView, home, "newdio").hide(currentFragment).hide(active).show(home).commit()
                            } else {
                                if (supportFragmentManager.findFragmentByTag("player") != null && searchChild != null) {
                                    supportFragmentManager.beginTransaction().hide(currentFragment).hide(searchChild!!).hide(active).show(home).commit()
                                } else if (supportFragmentManager.findFragmentByTag("player") != null && discoverSearch != null) {
                                    supportFragmentManager.beginTransaction().hide(currentFragment).hide(discoverSearch!!).hide(active).show(home).commit()
                                }else {
                                    supportFragmentManager.beginTransaction().hide(currentFragment).hide(active).show(home).commit()
                                }

                            }
                            // active를 현재 탭으로 변경
                            active = home
                        }
                        R.id.liveFragment -> {
                            parameters.putString("id", "live")
                            iconOff(menu)
                            menu.findItem(R.id.liveFragment).setIcon(R.drawable.ic_bar_live_on)
                            selectedItem = R.id.liveFragment
                            // 이미 만들어진 뷰가 있으면 해당 뷰 보여주고 아니면 새로 만들기
                            if (supportFragmentManager.findFragmentByTag("live") == null){
                                supportFragmentManager.beginTransaction().add(R.id.homeView, live, "live").hide(active).show(live).commit()
                            } else {
                                supportFragmentManager.beginTransaction().hide(currentFragment).hide(active).show(live).commit()
                            }
                            active = live
                        }
                        R.id.discoverFragment -> {
                            parameters.putString("id", "discover")
                            iconOff(menu)
                            menu.findItem(R.id.discoverFragment).setIcon(R.drawable.ic_bar_discover_on)
                            selectedItem = R.id.discoverFragment
                            if (supportFragmentManager.findFragmentByTag("discover") == null){
                                supportFragmentManager.beginTransaction().add(R.id.homeView, discover, "discover").hide(active).show(discover).commit()
                            } else {
                                if (discoverSearch != null) {
                                    Log.d("discoverSearch", "not null")
                                    supportFragmentManager.beginTransaction().hide(active).show(discover).show(discoverSearch!!).commit()
                                } else {
                                    Log.d("discoverSearch", "null")
                                    supportFragmentManager.beginTransaction().hide(active).show(discover).commit()
                                }
                            }

                            active = discover
                        }
                        R.id.searchFragment -> {
                            parameters.putString("id", "search")
                            iconOff(menu)
                            menu.findItem(R.id.searchFragment).setIcon(R.drawable.ic_bar_search_on)
                            selectedItem = R.id.searchFragment
                            if (supportFragmentManager.findFragmentByTag("search") == null){
                                supportFragmentManager.beginTransaction().add(R.id.homeView, search, "search").hide(active).show(search).commit()
                            } else {
                                if (searchChild != null) {
                                    supportFragmentManager.beginTransaction().hide(active).show(search).show(searchChild!!).commit()
                                } else {
                                    supportFragmentManager.beginTransaction().hide(active).show(search).commit()
                                }
                            }

                            active = search
                        }
                        R.id.settingFragment -> {
                            parameters.putString("id", "setting")
                            iconOff(menu)
                            menu.findItem(R.id.settingFragment).setIcon(R.drawable.ic_bar_setting_on)
                            selectedItem = R.id.settingFragment
                            if (supportFragmentManager.findFragmentByTag("setting") == null){
                                supportFragmentManager.beginTransaction().add(R.id.homeView, setting, "setting").hide(active).show(setting).commit()
                            } else {
                                if (supportFragmentManager.findFragmentByTag("player") != null){
                                    player = supportFragmentManager.findFragmentByTag("player") as PlayerFragment
                                    supportFragmentManager.beginTransaction().hide(currentFragment).hide(player!!).hide(active).show(setting).commit()
                                } else {
                                    supportFragmentManager.beginTransaction().hide(currentFragment).hide(active).show(setting).commit()
                                }

                            }
                            active = setting
                        }

                    }
                }
                firebaseAnalytics.logEvent("newdio", parameters)
                true
            }
        }
    }


    // 뒤로가기 두번시 종료
    private var doubleBackToExitPressedOnce = false
    override fun onBackPressed() {
        // back stack이 있을때 뒤로가기 누르면 종료 x
        if (doubleBackToExitPressedOnce|| supportFragmentManager.backStackEntryCount != 0) {
            super.onBackPressed()
            return
        }
        this.doubleBackToExitPressedOnce = true

        // 검색에서 검색화면이 켜져있을때 뒤로가기로 검색화면 종료
        val searchFragment : SearchFragment? = supportFragmentManager.findFragmentByTag("search") as SearchFragment?
        if (searchFragment != null && searchFragment.isSearchOn) {
            searchFragment.searchBarOff()
            val navView: BottomNavigationView = viewBinding.bottomNavigation
            navView.visibility = View.VISIBLE
            this.doubleBackToExitPressedOnce = false
        }

        Handler(Looper.getMainLooper()).postDelayed({ doubleBackToExitPressedOnce = false }, 2000)
    }

    // 비활성화 아이콘
    private fun iconOff(menu: Menu) {
        menu.findItem(R.id.homeFragment).setIcon(R.drawable.ic_bar_home_off)
        menu.findItem(R.id.liveFragment).setIcon(R.drawable.ic_bar_live_off)
        menu.findItem(R.id.settingFragment).setIcon(R.drawable.ic_bar_setting_off)
        menu.findItem(R.id.discoverFragment).setIcon(R.drawable.ic_bar_discover_off)
        menu.findItem(R.id.searchFragment).setIcon(R.drawable.ic_bar_search_off)
    }

    override fun onDialogPositiveClick(dialog: androidx.fragment.app.DialogFragment) {
        val searchFragment : SearchFragment = supportFragmentManager.findFragmentByTag("search") as SearchFragment
        searchFragment.deleteAll()
    }

    override fun onDialogNegativeClick(dialog: androidx.fragment.app.DialogFragment) {}


    override fun onDestroy() {
        super.onDestroy()

        if (supportFragmentManager.findFragmentByTag("player") != null){
            player = supportFragmentManager.findFragmentByTag("player") as PlayerFragment
            player!!.disconnectMedia()
        }

        val parameters = Bundle().apply{
            this.putString("action", "kill")
        }
        sendFirebaseLog(parameters)

    }


    override fun onStop() {
        super.onStop()

        val parameters = Bundle().apply{
            this.putString("action", "background")
        }
        sendFirebaseLog(parameters)
    }

    override fun onRestart() {
        super.onRestart()

        val parameters = Bundle().apply{
            this.putString("action", "foreground")
        }
        sendFirebaseLog(parameters)

    }

    // firebase Log
    fun sendFirebaseLog(param : Bundle) {
        val currentTime = (Calendar.getInstance().timeInMillis/1000).toInt()
        // 현재 fragment 불러오기
        val fragments: List<Fragment> = supportFragmentManager.fragments
        var currentFragment = Fragment()
        for (fragment in fragments) {
            if (fragment.isVisible)
                currentFragment = fragment
        }

        param.apply {
            this.putString("screen", currentFragment.tag.toString())
            this.putInt("time", currentTime)
        }
        firebaseAnalytics.logEvent("newdio", param)
    }

    // 로그아웃
    fun logout(access: String, refresh: String, header: String) {
        sharedPreferences.sharedClear(applicationContext, "access_token")
        sharedPreferences.sharedClear(applicationContext, "refresh_token")
        sharedPreferences.sharedClear(applicationContext, "language")
        sharedPreferences.sharedClear(applicationContext, "textSize")
        sharedPreferences.sharedClear(applicationContext, "autoPlay")
        sharedPreferences.sharedClear(applicationContext, "userEmail")
        sharedPreferences.sharedClear(applicationContext, "provider")
        sharedPreferences.sharedClear(applicationContext, "userId")
        Toast.makeText(applicationContext, getString(R.string.common_logout), Toast.LENGTH_SHORT).show()

        if (header == "Expired") {
            val parameters = Bundle().apply{
                this.putString("screen", null)
                this.putString("action", "token")
                this.putString("type", "expired_token")
                this.putString("token_access", access)
                this.putString("token_refresh", refresh)
            }
            sendFirebaseLog(parameters)
        }

    }

    // 언어 설정
    fun getLanguagePreference() : String {
        // 저장된 선호 언어가 있으면 해당 언어 불러오기, 없으면 시스템 설정 언어
        val savedLanguage = sharedPreferences.getShared(this, "language")
        if (savedLanguage == null){
            language = ConfigurationCompat.getLocales(Resources.getSystem().configuration)[0].language
        } else {
            language = savedLanguage
        }
        return language
    }

    // player로 이동
    fun getPlayer(bundle: Bundle) {
        if (supportFragmentManager.findFragmentByTag("player") != null){
            val player = supportFragmentManager.findFragmentByTag("player") as PlayerFragment
            player.disconnectMedia()
            player.dialog?.dismiss()
        }
        val playerFragment = PlayerFragment()
        playerFragment.arguments = bundle
        playerFragment.show(supportFragmentManager, "player")
    }

    // 상세보기
    fun getDiscoverDetail(bundle: Bundle) {
        val discoverDetailFragment = DiscoverDetailFragment()
        discoverDetailFragment.arguments = bundle

        supportFragmentManager.beginTransaction()
            .setCustomAnimations(R.animator.discover_detail_slide_right, R.animator.discover_detail_slide_left, R.animator.discover_detail_slide_right, R.animator.discover_detail_slide_left)
            .addToBackStack(null)
            .add(R.id.homeView, discoverDetailFragment).commit()
    }

    fun getDateFormatter(formatTo: String, date: String) : String {
        val formatterFrom = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSS'Z'", Locale.getDefault())
        val formatterTo = SimpleDateFormat(formatTo, Locale.getDefault())
        formatterFrom.timeZone = TimeZone.getTimeZone("UTC")
        val date = formatterFrom.parse(date)
        return formatterTo.format(date)
    }



}