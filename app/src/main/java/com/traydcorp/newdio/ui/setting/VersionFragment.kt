package com.traydcorp.newdio.ui.setting

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.traydcorp.newdio.BuildConfig
import com.traydcorp.newdio.databinding.FragmentVersionBinding
import com.traydcorp.newdio.ui.home.HomeActivity
import java.util.*


class VersionFragment : Fragment() {

    private var viewBinding : FragmentVersionBinding? = null
    private val bind get() = viewBinding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        viewBinding = FragmentVersionBinding.inflate(inflater, container, false)

        val appVersion = BuildConfig.VERSION_NAME

        val firebaseAnalytics = (activity as HomeActivity).firebaseAnalytics
        val currentTime = (Calendar.getInstance().timeInMillis/1000).toInt()
        val parameters = Bundle().apply {
            this.putString("screen", "setting")
            this.putString("action", "click")
            this.putInt("time", currentTime)
            this.putString("type", "version")
            this.putString("version", appVersion)
        }
        firebaseAnalytics.logEvent("newdio", parameters)

        bind.versionText2.text = appVersion

        // 뒤로가기
        bind.backBtn.setOnClickListener {
            if (requireActivity().supportFragmentManager.backStackEntryCount != 0){
                requireActivity().supportFragmentManager.popBackStack()
            }
        }
        return bind.root
    }


}