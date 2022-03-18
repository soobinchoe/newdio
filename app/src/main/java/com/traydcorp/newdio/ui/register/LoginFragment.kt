package com.traydcorp.newdio.ui.register

import android.annotation.SuppressLint
import android.app.Activity
import android.content.ContentValues
import android.content.ContentValues.TAG
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.res.Resources
import android.graphics.Paint
import android.net.Uri
import android.os.Bundle
import android.telephony.TelephonyManager
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult
import androidx.appcompat.app.AlertDialog
import androidx.core.os.ConfigurationCompat
import androidx.fragment.app.Fragment
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.ktx.Firebase
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.kakao.sdk.auth.AuthApiClient
import com.kakao.sdk.auth.model.OAuthToken
import com.kakao.sdk.user.UserApiClient
import com.nhn.android.naverlogin.OAuthLogin
import com.nhn.android.naverlogin.OAuthLoginHandler
import com.nhn.android.naverlogin.data.OAuthLoginState
import com.traydcorp.newdio.R
import com.traydcorp.newdio.dataModel.ResponseBody
import com.traydcorp.newdio.dataModel.SocialToken
import com.traydcorp.newdio.databinding.FragmentLoginBinding
import com.traydcorp.newdio.ui.home.HomeActivity
import com.traydcorp.newdio.utils.SharedPreference
import com.traydcorp.newdio.utils.retofitService.RetrofitService
import com.traydcorp.newdio.utils.retrofitAPI.RetrofitClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import java.io.IOException
import java.util.*
import kotlin.concurrent.thread


class LoginFragment : Fragment() {

    companion object {
        fun newInstance() = LoginFragment()
    }

    private var _binding: FragmentLoginBinding? = null
    var mOAuthLoginModule: OAuthLogin = OAuthLogin.getInstance()
    private lateinit var googleSignInClient: GoogleSignInClient


    private val binding get() = _binding!!
    private lateinit var language : String

    private val shared = SharedPreference()
    private val gson = Gson()
    private var socialName : String? = null
    private var accessToken = SocialToken()

    private lateinit var retrofit: Retrofit
    private lateinit var supplementService: RetrofitService

    private lateinit var firebaseAnalytics: FirebaseAnalytics

    private var parameters = Bundle()
    private var currentTime : Int = 0


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        firebaseAnalytics = Firebase.analytics

        val savedLanguage = shared.getShared(requireContext(), "language")
        if (savedLanguage == null){
            language = ConfigurationCompat.getLocales(Resources.getSystem().configuration)[0].language
        } else {
            language = savedLanguage
        }

        retrofit = RetrofitClient.getInstance()
        supplementService = retrofit.create(RetrofitService::class.java)

        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        return binding.root

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val telephoneManager = context?.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        val countryCode = telephoneManager.networkCountryIso

        // countryCode가 한국일 때 카카오, 네이버 로그인 보이게
        if (countryCode == "kr") {
            binding.kakaoLoginButton.visibility = View.VISIBLE
            binding.naverLoginButton.visibility = View.VISIBLE
        }

        // 텍스트에 밑줄
        binding.tnc.paintFlags = Paint.UNDERLINE_TEXT_FLAG
        binding.privacy.paintFlags = Paint.UNDERLINE_TEXT_FLAG

        // 이용 약관
        binding.tnc.setOnClickListener {
            if (language == "ko"){
                openBrowser("https://www.traydcorp.com/newdio-termsofservice-kor")
            } else {
                openBrowser("https://www.traydcorp.com/newdio-termsofservice-eng")
            }
        }

        // 개인정보처리방침
        binding.privacy.setOnClickListener {
            if (language == "ko"){
                openBrowser("https://www.traydcorp.com/newdio-privacypolicy-kor")
            } else {
                openBrowser("https://www.traydcorp.com/newdio-privacypolicy-eng")
            }
        }

        parameters = Bundle().apply {
            this.putString("action", "click")
            this.putString("type", "login")
        }

        // 카카오 로그인
        val kakaoLoginButton = binding.kakaoLoginButton
        kakaoLoginButton.setOnClickListener {
            parameters.putString("social", "kakao")
            sendFirebaseLog(parameters)

            // 카카오톡이 설치되어 있으면 카카오톡으로 로그인, 아니면 카카오계정으로 로그인
            if (UserApiClient.instance.isKakaoTalkLoginAvailable(requireContext())) {
                UserApiClient.instance.loginWithKakaoTalk(requireContext(), callback = callback)
            } else {
                UserApiClient.instance.loginWithKakaoAccount(requireContext(), callback = callback)
            }
        }


        // 네이버 로그인
        val naverLoginButton = binding.naverLoginButton
        naverLoginButton.setOnClickListener {
            parameters.putString("social", "naver")
            sendFirebaseLog(parameters)

            // 네이버 초기화
            val OAUTH_CLIENT_ID = "nyx2geVDIUInkFgSoivT"
            val OAUTH_CLIENT_SECRET = "Rk52RrsByI"
            val OAUTH_CLIENT_NAME = "NEWDIO"
            mOAuthLoginModule.init(
                context, OAUTH_CLIENT_ID, OAUTH_CLIENT_SECRET, OAUTH_CLIENT_NAME
            )
            mOAuthLoginModule.startOauthLoginActivity(requireActivity(), mOAuthLoginHandler)
        }

        //구글 로그인
        // app\bulid\generated\res\google-services\debug\values\values.xml
        // default_web_client_id
        // google API OAuth 2.0 클라이언트 ID

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))  // OK
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(context, gso)
        val signInIntent = googleSignInClient.signInIntent

        val googleLoginButton = binding.googleLoginButton
        googleLoginButton.setOnClickListener {
            parameters.putString("social", "google")
            sendFirebaseLog(parameters)

            Log.d("구글 로그인", "구글 로그인 버튼 클릭")
            googleLoginLauncher.launch(signInIntent)
        }

        disconnect()


    }


    // 구글 로그인 Launcher
    private val googleLoginLauncher =
        registerForActivityResult(StartActivityForResult()) { result ->
            Log.d("구글 로그인", result.resultCode.toString()) // resultCode가 0이면 연결 실패

            if (result.resultCode == Activity.RESULT_OK) {
                Log.d("구글 로그인", "구글 로그인 성공")
                val data: Intent? = result.data
                val task = GoogleSignIn.getSignedInAccountFromIntent(data)
                try {
                    val account = task.getResult(ApiException::class.java)!!
                    Log.d(TAG, "구글 idToken:" + account.idToken)
                    socialName = "google"
                    accessToken.access_token = account.idToken
                    gson.toJson(account.idToken).toString()
                    gson.toJson(socialName).toString()
                    loginCall(supplementService.login(accessToken, socialName))
                } catch (e: ApiException) {
                    // Google Sign In failed, update UI appropriately
                    Log.w(TAG, "구글 로그인 실패", e)
                }
            }
        }

    // 카카오 로그인 공통 callback 구성
    val callback: (OAuthToken?, Throwable?) -> Unit = { token, error ->
        Log.d(TAG, "카카오 callback")
        if (error != null) {
            Log.e(TAG, "카카오 로그인 실패", error)
        } else if (token != null) {
            Log.i(TAG, "카카오 로그인 성공 ${token.accessToken}")
            socialName = "kakao"
            accessToken.access_token = token.accessToken
            gson.toJson(token.accessToken).toString()
            gson.toJson(socialName).toString()
            loginCall(supplementService.login(accessToken, socialName))
        }
    }


    // 네이버 로그인 핸들러
    private val mOAuthLoginHandler: OAuthLoginHandler = @SuppressLint("HandlerLeak")
    object : OAuthLoginHandler() {
        override fun run(success: Boolean) {
            if (success) {
                val naverAccessToken = mOAuthLoginModule.getAccessToken(context)
                val refreshToken = mOAuthLoginModule.getRefreshToken(context)
                val expiresAt = mOAuthLoginModule.getExpiresAt(context)
                Log.i(TAG, "네이버 로그인 성공" + "\n토큰: ${naverAccessToken}" + "\n만료시간: ${expiresAt} 초")

                socialName = "naver"
                accessToken.access_token = naverAccessToken
                gson.toJson(naverAccessToken).toString()
                gson.toJson(socialName).toString()

                loginCall(supplementService.login(accessToken, socialName))

            }
        }
    }

    // 소셜 로그인 api call
    private fun loginCall(service: Call<JsonObject>) {
        service.enqueue(object : Callback<JsonObject> {
            override fun onResponse(call: Call<JsonObject>, response: Response<JsonObject>) {
                if (response.code() == 200){
                    // 회원인 유저가 로그인 시
                    // Authorization, Refresh 토큰 저장
                    val access1 = response.body()?.get("Authorization").toString()
                    val refresh1 = response.body()?.get("Refresh-Token").toString()

                    val body = response.body()?.toString()
                    if (body != null) {
                        Log.d("responseBody", body)
                    }

                    val access = access1.substring(1, access1.length-1)
                    val refresh = refresh1.substring(1, refresh1.length-1)

                    shared.setShared("access_token", access, requireContext())
                    shared.setShared("refresh_token", refresh, requireContext())



                    //loginSuccess() // 로그인 완료 페이지
                    val intent = Intent(context, HomeActivity::class.java)
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    intent.putExtra("reset", "reset")
                    startActivity(intent)
                    activity?.finish()


                } else if (response.code() == 412){
                    // {"detail":"An email does not exist in the token."}
                    val detail = response.errorBody()?.let {
                        Gson().fromJson(it.charStream(), ResponseBody::class.java)
                    }

                    if (detail?.detail == null) {
                        val intent = Intent(context, RegisterActivity::class.java)
                        intent.putExtra("accessToken", accessToken.access_token)
                        intent.putExtra("socialName", socialName)
                        startActivity(intent)
                    } else {
                        disconnect()
                    }

                } else if(response.code() == 401){ // 토큰 만료 또는 토큰이 맞지 않을시
                    // 잘못된 토큰 팝업 메세지
                    val builder = AlertDialog.Builder(requireContext())
                    builder.setMessage(getString(R.string.error_401_content))
                        .setTitle(getString(R.string.error_401_title))
                        .setPositiveButton(getString(R.string.popup_confirm),
                            DialogInterface.OnClickListener { dialog, id ->
                            })
                    val alertDialog = builder.create()
                    alertDialog.show()
                    disconnect()
                }else if(response.code() == 400){
                    disconnect()
                } else {
                    try {
                        Log.d("errorMessageTAG", response.errorBody().toString())
                    } catch (e: IOException) {
                        Log.d("ExceptionTAG", e.message.toString())
                    }
                }
            }
            override fun onFailure(call: Call<JsonObject>, t: Throwable) {
                Log.d("LoginErrormessage", t.message.toString())
                disconnect()
            }
        })
    }

    private fun openBrowser(url : String) {
        val browserIntent = Intent(
            Intent.ACTION_VIEW,
            Uri.parse(url)
        )
        startActivity(browserIntent)
    }


    private fun disconnect() {
        val gso : GoogleSignInOptions = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        val client = GoogleSignIn.getClient(context, gso)

        val mOAuthLoginModule : OAuthLogin = OAuthLogin.getInstance()

        if(AuthApiClient.instance.hasToken()){ // 카카오 연결
            UserApiClient.instance.unlink { error ->
                if (error != null) {
                    Log.e(ContentValues.TAG, "카카오 연결 끊기 실패")
                }
                else {
                    Log.i(ContentValues.TAG, "카카오 연결 끊기 성공. SDK에서 토큰 삭제 됨")
                }
            }
        }
        if(OAuthLoginState.OK == mOAuthLoginModule.getState(context)) { // 네이버 연결
            Log.d(ContentValues.TAG, "네이버 연결 끊기")
            thread {
                val isSuccessDeleteToken: Boolean = mOAuthLoginModule.logoutAndDeleteToken(context)
                if (isSuccessDeleteToken) {
                    Log.i(ContentValues.TAG, "네이버 연결 끊기 성공. SDK에서 토큰 삭제 됨")
                }
            }
        }

        // 구글 연결
        client.revokeAccess()
        Log.i(ContentValues.TAG, "구글 연결 끊기 성공. SDK에서 토큰 삭제 됨")
    }

    private fun sendFirebaseLog(param: Bundle) {
        currentTime = (Calendar.getInstance().timeInMillis/1000).toInt()
        param.apply {
            this.putString("screen", "sign")
            this.putInt("time", currentTime)
        }
        firebaseAnalytics.logEvent("newdio", param)
    }

}