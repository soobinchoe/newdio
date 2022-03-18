package com.traydcorp.newdio


import android.content.ContentValues
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.kakao.sdk.auth.AuthApiClient
import com.kakao.sdk.common.util.Utility
import com.kakao.sdk.user.UserApiClient
import com.nhn.android.naverlogin.OAuthLogin
import com.nhn.android.naverlogin.data.OAuthLoginState
import com.traydcorp.newdio.ui.register.LoginFragment
import kotlin.concurrent.thread

class MainActivity : AppCompatActivity() {


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main_activity)


        // 카카오 로그인 해시
        val keyHash = Utility.getKeyHash(this)
        Log.d("Hash", keyHash)

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.container, LoginFragment.newInstance())
                .commitNow()
        }

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
                    Log.e(ContentValues.TAG, "카카오 연결 끊기 실패")
                }
                else {
                    Log.i(ContentValues.TAG, "카카오 연결 끊기 성공. SDK에서 토큰 삭제 됨")
                }
            }
        }
        if(OAuthLoginState.OK == mOAuthLoginModule.getState(this)) { // 네이버 연결
            Log.d(ContentValues.TAG, "네이버 연결 끊기")
            thread {
                val isSuccessDeleteToken: Boolean = mOAuthLoginModule.logoutAndDeleteToken(this)
                if (isSuccessDeleteToken) {
                    Log.i(ContentValues.TAG, "네이버 연결 끊기 성공. SDK에서 토큰 삭제 됨")
                }
            }
        }

        // 구글 연결
        client.revokeAccess()
        Log.i(ContentValues.TAG, "구글 연결 끊기 성공. SDK에서 토큰 삭제 됨")
    }



}