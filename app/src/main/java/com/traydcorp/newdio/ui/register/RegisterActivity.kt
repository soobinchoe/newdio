package com.traydcorp.newdio.ui.register

import android.content.ContentValues.TAG
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import com.kakao.sdk.auth.AuthApiClient
import com.kakao.sdk.user.UserApiClient
import com.nhn.android.naverlogin.OAuthLogin
import com.nhn.android.naverlogin.data.OAuthLoginState
import com.traydcorp.newdio.R
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.traydcorp.newdio.ui.setting.DialogFragment
import kotlin.concurrent.thread


class RegisterActivity : AppCompatActivity(), DialogFragment.NoticeDialogListener {


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 로그인 페이지에서 넘어온 데이터
        val bundle = Bundle()
        bundle.putString("accessToken", intent.getStringExtra("accessToken"))
        bundle.putString("socialName", intent.getStringExtra("socialName"))

        // 데이터 담아서 다음페이지로
        val register1Fragment = Register1Fragment()
        register1Fragment.arguments = bundle
        supportFragmentManager.beginTransaction().
        replace(R.id.fragmentViewLayout, register1Fragment).commit()

        setContentView(R.layout.activity_register)
    }


    // 소셜 로그인 가입 중단시 소셜 토큰 삭제
    override fun onStop() {
        super.onStop()
        disconnect()

    }

    override fun onDestroy() {
        super.onDestroy()
        disconnect()
    }


    private fun disconnect() {
        val gso : GoogleSignInOptions = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        val client = GoogleSignIn.getClient(this, gso)

        val mOAuthLoginModule : OAuthLogin = OAuthLogin.getInstance()

        if(AuthApiClient.instance.hasToken()){ // 카카오 연결
            UserApiClient.instance.unlink { error ->
                if (error != null) {
                    Log.e(TAG, "카카오 연결 끊기 실패")
                }
                else {
                    Log.i(TAG, "카카오 연결 끊기 성공. SDK에서 토큰 삭제 됨")
                }
            }
        }
        if(OAuthLoginState.OK == mOAuthLoginModule.getState(this)) { // 네이버 연결
            Log.d(TAG, "네이버 연결 끊기")
            thread {
                val isSuccessDeleteToken: Boolean = mOAuthLoginModule.logoutAndDeleteToken(this)
                if (isSuccessDeleteToken) {
                    Log.i(TAG, "네이버 연결 끊기 성공. SDK에서 토큰 삭제 됨")
                }
            }
        }

        // 구글 연결
        client.revokeAccess()
        Log.i(TAG, "구글 연결 끊기 성공. SDK에서 토큰 삭제 됨")
    }

    override fun onDialogPositiveClick(dialog: androidx.fragment.app.DialogFragment) {
        val registerFragment : Register3Fragment = supportFragmentManager.findFragmentByTag("register3") as Register3Fragment
        registerFragment.callRegister()
    }

    override fun onDialogNegativeClick(dialog: androidx.fragment.app.DialogFragment) {
        TODO("Not yet implemented")
    }


}