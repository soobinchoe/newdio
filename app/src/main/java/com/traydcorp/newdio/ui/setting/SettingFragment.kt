package com.traydcorp.newdio.ui.setting

import android.content.ActivityNotFoundException
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.traydcorp.newdio.databinding.FragmentSettingBinding
import android.content.Intent
import android.content.res.Resources
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.os.ConfigurationCompat
import com.google.firebase.analytics.FirebaseAnalytics
import com.traydcorp.newdio.BuildConfig
import com.traydcorp.newdio.MainActivity
import com.traydcorp.newdio.R
import com.traydcorp.newdio.dataModel.User
import com.traydcorp.newdio.ui.home.HomeActivity
import com.traydcorp.newdio.ui.player.PlayerFragment
import com.traydcorp.newdio.utils.SharedPreference
import com.traydcorp.newdio.utils.retofitService.RetrofitService
import com.traydcorp.newdio.utils.retrofitAPI.RetrofitClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import java.util.*


class SettingFragment : Fragment() {

    companion object {
        fun newInstance() = SettingFragment()
    }

    private var viewBinding : FragmentSettingBinding? = null
    private val bind get() = viewBinding!!

    val sharedPreferences = SharedPreference()

    private lateinit var language : String
    private var access : String? = null
    private var refresh : String? = null

    private var userEmail : String? = null
    private var provider : String? = null

    private lateinit var retrofit: Retrofit
    private lateinit var supplementService: RetrofitService

    private lateinit var activityResultLauncher: ActivityResultLauncher<Intent>

    private lateinit var firebaseAnalytics: FirebaseAnalytics

    private var parameters = Bundle()
    private var currentTime : Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        firebaseAnalytics = (activity as HomeActivity).firebaseAnalytics

        language = (activity as HomeActivity).getLanguagePreference()

        access = sharedPreferences.getShared(context, "access_token")
        refresh = sharedPreferences.getShared(context, "refresh_token")

        userEmail = sharedPreferences.getShared(context, "userEmail")
        provider = sharedPreferences.getShared(context, "provider")

        retrofit = RetrofitClient.getInstance()
        supplementService = retrofit.create(RetrofitService::class.java)

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        viewBinding = FragmentSettingBinding.inflate(inflater, container, false)

        if (access != null && refresh != null){ // 회원일 때
            if (userEmail == null && provider == null) { // user 정보가 없을 때
                getUserInfo(supplementService.getUser(access, refresh, language))
            }
            if (userEmail != null && provider != null) { // user 정보가 있으면 업데이트
                updateUserInfo(provider!!, userEmail!!)
            }
            bind.manageAccount.visibility = View.VISIBLE
            bind.stored.visibility = View.VISIBLE
            bind.loginText.visibility = View.GONE
            bind.User.visibility = View.VISIBLE
        } else { // 비회원일 때
            bind.manageAccount.visibility = View.GONE
            bind.loginBtn.visibility = View.VISIBLE
            bind.autoPlaySetting.visibility = View.GONE
        }


        // 사용 환경 설정 - 설정 값
        if (language == "ko"){
            bind.languageSettingText.text = getString(R.string.settings_language_ko)
        } else {
            bind.languageSettingText.text = getString(R.string.settings_language_en)
        }

        updateAutoplaySetting()

        updateTextSizeSetting()

        return bind.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val bundle = Bundle()

        // 버전정보
        bind.versionInfo.setOnClickListener {
            requireActivity().supportFragmentManager.beginTransaction().addToBackStack(null)
                .add(R.id.homeView, VersionFragment(), "settingChild").commit()
        }

        // 공지사항
        bind.notice.setOnClickListener {
            parameters = Bundle().apply {
                this.putString("action", "click")
                this.putString("type", "notice")
            }

            if (language == "ko"){
                parameters.putString("language", "ko")
                openBrowser("https://www.traydcorp.com/newdio-notice-kor")
            } else {
                parameters.putString("language", "en")
                openBrowser("https://www.traydcorp.com/newdio-notice-eng")
            }

            sendFirebaseLog(parameters)

        }

        // 버그 또는 문의
        bind.bug.setOnClickListener {
            val mailSubject = "[" + getString(R.string.appname) + "]" + getString(R.string.settings_bug)

            val model = Build.MODEL
            val manufacturer = Build.MANUFACTURER
            val osVersion = Build.VERSION.RELEASE
            val appVersion = BuildConfig.VERSION_NAME

            parameters = Bundle().apply {
                this.putString("action", "click")
                this.putString("type", "bug")
                this.putString("version", appVersion)
            }
            sendFirebaseLog(parameters)

            Log.d("bud",
                "model : $model, manufacurer : $manufacturer, osVersion : $osVersion, appVersion : $appVersion"
            )
            val mailBody = getString(R.string.settings_phone_info) + " : " + manufacturer + " " + model + "\n" + getString(R.string.settings_os_version) + " : " + osVersion + "\n" + getString(R.string.settings_app_version) + " : " + appVersion
            val intent = Intent(Intent.ACTION_SEND)
            intent.type = "plain/text"
            intent.putExtra(Intent.EXTRA_EMAIL, arrayOf("newdioglobal@traydcorp.com"))
            intent.putExtra(Intent.EXTRA_SUBJECT, mailSubject)
            intent.putExtra(Intent.EXTRA_TEXT, mailBody)
            startActivity(Intent.createChooser(intent, ""))
        }

        // 앱 평가하기
        bind.feedback.setOnClickListener {
            parameters = Bundle().apply {
                this.putString("action", "click")
                this.putString("type", "feedback")
                this.putString("version", BuildConfig.VERSION_NAME)
            }
            sendFirebaseLog(parameters)

            val uri: Uri = Uri.parse("market://details?id=com.traydcorp.newdio")
            val goToMarket = Intent(Intent.ACTION_VIEW, uri)
            // To count with Play market backstack, After pressing back button,
            // to taken back to our application, we need to add following flags to intent.
            goToMarket.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY or
                    Intent.FLAG_ACTIVITY_NEW_DOCUMENT or
                    Intent.FLAG_ACTIVITY_MULTIPLE_TASK)
            try {
                startActivity(goToMarket)
            } catch (e: ActivityNotFoundException) {
                startActivity(Intent(Intent.ACTION_VIEW,
                    Uri.parse("http://play.google.com/store/apps/details?id=com.traydcorp.newdio")))
            }
        }

        // 앱 공유하기
        bind.share.setOnClickListener {
            parameters = Bundle().apply {
                this.putString("action", "click")
                this.putString("type", "share")
                this.putString("version", BuildConfig.VERSION_NAME)
                if (language == "ko") {
                    parameters.putString("language", "ko")
                } else {
                    parameters.putString("language", "en")
                }
            }
            sendFirebaseLog(parameters)

            val intentInvite = Intent(Intent.ACTION_SEND)
            intentInvite.type = "text/plain"
            val body = "https://play.google.com/store/apps/details?id=com.traydcorp.newdio"
            val subject = getString(R.string.appname) + " - " + getString(R.string.app_slogan)
            try {
                intentInvite.putExtra(Intent.EXTRA_SUBJECT, subject)
                intentInvite.putExtra(Intent.EXTRA_TEXT, body)
                intentInvite.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

                activityResultLauncher.launch(Intent.createChooser(intentInvite, "Share using"))

            } catch (e: ActivityNotFoundException) {
            }

        }


        // 자주 묻는 질문
        bind.faq.setOnClickListener {
            parameters = Bundle().apply {
                this.putString("action", "click")
                this.putString("type", "faq")
            }

            if (language == "ko"){
                parameters.putString("language", "ko")
                openBrowser("https://www.traydcorp.com/newdio-faq-kor")
            } else {
                parameters.putString("language", "en")
                openBrowser("https://www.traydcorp.com/newdio-faq-eng")
            }
            sendFirebaseLog(parameters)
        }

        // 서비스 이용약관
        bind.tncService.setOnClickListener {
            parameters = Bundle().apply {
                this.putString("action", "click")
                this.putString("type", "service")
            }
            if (language == "ko"){
                parameters.putString("language", "ko")
                openBrowser("https://www.traydcorp.com/newdio-termsofservice-kor")
            } else {
                parameters.putString("language", "en")
                openBrowser("https://www.traydcorp.com/newdio-termsofservice-eng")
            }
            sendFirebaseLog(parameters)
        }

        // 개인정보 보호약관
        bind.tncPrivacy.setOnClickListener {
            parameters = Bundle().apply {
                this.putString("action", "click")
                this.putString("type", "privacy")
            }
            if (language == "ko"){
                parameters.putString("language", "ko")
                openBrowser("https://www.traydcorp.com/newdio-privacypolicy-kor")
            } else {
                parameters.putString("language", "en")
                openBrowser("https://www.traydcorp.com/newdio-privacypolicy-eng")
            }
            sendFirebaseLog(parameters)
        }

        // 유료 서비스 이용약관
        bind.tncSubscription.setOnClickListener {
            parameters = Bundle().apply {
                this.putString("action", "click")
                this.putString("type", "subscriptions")
            }
            if (language == "ko"){
                parameters.putString("language", "ko")
                openBrowser("https://www.traydcorp.com/newdio-termsandconditionsoflocationservice-kor")
            } else {
                parameters.putString("language", "en")
                openBrowser("https://www.traydcorp.com/newdio-termsandconditionsoflocationservice-eng")
            }
            sendFirebaseLog(parameters)
        }

        // 언어 설정
        bind.languageSetting.setOnClickListener {
            requireActivity().supportFragmentManager.beginTransaction().addToBackStack(null)
                .add(R.id.homeView, LanguageFragment(), "settingChild").commit()
        }

        // 로그아웃
        bind.logout.setOnClickListener {
            if (access != null && refresh != null){
                val dialog = DialogFragment()
                bundle.putString("key", "Logout")
                dialog.arguments = bundle
                dialog.show(parentFragmentManager, "dialog")

                //DialogFragment("Logout").show(rquireFragment, "asd")
            }
        }

        // 회원 탈퇴
        bind.deleteAccount.setOnClickListener {
            requireActivity().supportFragmentManager.beginTransaction().addToBackStack(null)
                .add(R.id.homeView, DeleteFragment(), "settingChild").commit()
        }

        // 로그인
        bind.loginBtn.setOnClickListener {
            val intent = Intent(context, MainActivity::class.java)
            startActivity(intent)
        }

        // 텍스트 크기
        bind.textSizeSetting.setOnClickListener {
            requireActivity().supportFragmentManager.beginTransaction().addToBackStack(null)
                .add(R.id.homeView, TextSizeFragment(), "settingChild").commit()
        }

        // 보관함
        bind.stored.setOnClickListener {
            requireActivity().supportFragmentManager.beginTransaction().addToBackStack(null)
                .setCustomAnimations(R.animator.player_slide_up, R.animator.player_slide_down, R.animator.player_slide_up, R.animator.player_slide_down)
                .add(R.id.homeView, FavotiteFragment(), "settingChild").commit()
        }

        // 음성 자동 재생
        bind.autoPlaySetting.setOnClickListener {
            requireActivity().supportFragmentManager.beginTransaction().addToBackStack(null)
                .add(R.id.homeView, AutoPlayFragment(), "settingChild").commit()
        }

    }

    private fun openBrowser(url : String) {
        val browserIntent = Intent(
            Intent.ACTION_VIEW,
            Uri.parse(url)
        )
        startActivity(browserIntent)
    }

    // 회원 정보 api
    private fun getUserInfo(service: Call<User>) {
        Handler(Looper.getMainLooper()).postDelayed({
            service.enqueue(object : Callback<User> {
                override fun onResponse(call: Call<User>, response: Response<User>) {
                    if (response.code() == 200){
                        val result = response.body()

                        if (result != null) {
                            sharedPreferences.setUserInfo(result, requireContext())
                            updateUserInfo(result.provider, result.email)
                        }

                        // 액세스 토큰 만료시 새로 받은 토큰으로 업데이트
                        val newAccess = response.headers()["Authorization"]
                        if (newAccess != null) {
                            sharedPreferences.setShared("access_token", newAccess, requireContext())
                        }
                    }
                }

                override fun onFailure(call: Call<User>, t: Throwable) {
                    Log.d("error message", t.message.toString())
                    /*val intent = Intent(context, HomeActivity::class.java)
                    startActivity(intent)*/
                }

            })
        }, 1000)
    }

    // 음성 자동 재생 업데이트
    fun updateAutoplaySetting() {
        val autoPlay = sharedPreferences.getShared(requireContext(), "autoPlay")
        if (autoPlay == null){
            bind.autoPlayText.text = getString(R.string.settings_auto_play_on)
        } else {
            when (autoPlay) {
                "true" -> getString(R.string.settings_auto_play_on)
                "false" -> getString(R.string.settings_auto_play_off)
                else -> null
            }?.let {
                bind.autoPlayText.text = it
            }
        }
    }

    // 텍스트 크기 업데이트
    fun updateTextSizeSetting() {
        val savedTextSize = sharedPreferences.getShared(requireContext(), "textSize")
        if (savedTextSize == null){
            bind.textSizeText.text = getString(R.string.settings_text_original)
        } else {
            when (savedTextSize) {
                "small" -> getString(R.string.settings_text_small)
                "original" -> getString(R.string.settings_text_original)
                "large" -> getString(R.string.settings_text_large)
                else -> null
            }?.let {
                bind.textSizeText.text = it
            }
        }
    }

    // 회원 정보 업데이트
    private fun updateUserInfo(provider: String, userEmail: String) {
        when (provider) {
            "google" -> {
                bind.socialImage.setImageResource(R.drawable.img_general_google_square)
                bind.socialName.text = getString(R.string.social_account_google)
            }
            "kakao" -> {
                bind.socialImage.setImageResource(R.drawable.img_general_kakao_square)
                bind.socialName.text = getString(R.string.social_account_kakao)
            }
            "naver" -> {
                bind.socialImage.setImageResource(R.drawable.img_general_naver_square)
                bind.socialName.text = getString(R.string.social_account_naver)
            }
        }
        bind.socialUserId.text = userEmail
    }

    private fun sendFirebaseLog(param: Bundle) {
        currentTime = (Calendar.getInstance().timeInMillis/1000).toInt()
        param.apply {
            this.putString("screen", "setting")
            this.putInt("time", currentTime)
        }
        firebaseAnalytics.logEvent("newdio", param)
    }


}