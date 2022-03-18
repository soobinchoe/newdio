package com.traydcorp.newdio.ui.register

import android.app.Application
import com.kakao.sdk.common.KakaoSdk

class GlobalApplication : Application(){
    override fun onCreate() {
        super.onCreate()

        // 카카오 초기화
        KakaoSdk.init(this, "fac38c5bbd682d828f6354784a812f9f")

    }
}