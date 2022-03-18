package com.traydcorp.newdio.ui.register

import android.content.Context
import android.content.Intent
import android.content.res.Resources
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.telephony.TelephonyManager
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.core.os.ConfigurationCompat
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.ktx.Firebase
import com.traydcorp.newdio.R
import com.traydcorp.newdio.dataModel.User
import com.traydcorp.newdio.ui.home.HomeActivity
import com.traydcorp.newdio.utils.SharedPreference
import com.traydcorp.newdio.utils.retofitService.RetrofitService
import com.traydcorp.newdio.utils.retrofitAPI.RetrofitClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import java.sql.Timestamp
import java.util.*


class SplashActivity : AppCompatActivity() {

    val sharedPreferences = SharedPreference()

    private var access : String? = null
    private var refresh : String? = null

    private lateinit var retrofit: Retrofit
    private lateinit var supplementService: RetrofitService

    private lateinit var firebaseAnalytics: FirebaseAnalytics

    private lateinit var language : String

    private var userId : String? = null
    private var parameters = Bundle()
    private var currentTime : Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        val savedLanguage = sharedPreferences.getShared(applicationContext, "language")
        if (savedLanguage == null){
            language = ConfigurationCompat.getLocales(Resources.getSystem().configuration)[0].language
        } else {
            language = savedLanguage
        }

        // access token, refresh token
        access = sharedPreferences.getShared(applicationContext, "access_token")
        refresh = sharedPreferences.getShared(applicationContext, "refresh_token")

        retrofit = RetrofitClient.getInstance()
        supplementService = retrofit.create(RetrofitService::class.java)

        firebaseAnalytics = Firebase.analytics

        userId = sharedPreferences.getShared(applicationContext, "userId")

        if (access != null && refresh != null && userId == null) {
            getUserInfo(supplementService.getUser(access, refresh, language))
        }

        currentTime = (Calendar.getInstance().timeInMillis/1000).toInt()
        if (userId == null){
            userId = Settings.Secure.getString(applicationContext.contentResolver, Settings.Secure.ANDROID_ID)
            sharedPreferences.setShared("userUUID", userId!!, applicationContext)
        }

        parameters.apply {
            this.putString("userId", userId)
            this.putString("screen", "launch")
            this.putString("language", language)
            this.putInt("time", currentTime)
        }

        firebaseAnalytics.logEvent("newdio", parameters)


        val intentHome = Intent(this, HomeActivity::class.java)

        // 홈으로 이동
        Handler(Looper.getMainLooper()).postDelayed({
            startActivity(intentHome)
            finish()
        }, 2000)


    }



    private fun getUserInfo(service: Call<User>) {
        Handler(Looper.getMainLooper()).postDelayed({
            service.enqueue(object : Callback<User> {
                override fun onResponse(call: Call<User>, response: Response<User>) {
                    if (response.code() == 200){
                        val result = response.body()

                        if (result != null) {
                            sharedPreferences.setUserInfo(result, applicationContext)
                            userId = sharedPreferences.getShared(applicationContext, "userId")
                        }

                        // 액세스 토큰 만료시 새로 받은 토큰으로 업데이트
                        val newAccess = response.headers()["Authorization"]
                        if (newAccess != null) {
                            sharedPreferences.setShared("access_token", newAccess, applicationContext)
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




}



