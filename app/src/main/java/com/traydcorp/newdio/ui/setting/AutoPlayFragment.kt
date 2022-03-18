package com.traydcorp.newdio.ui.setting

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.widget.AppCompatButton
import androidx.constraintlayout.widget.ConstraintLayout
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.firebase.analytics.FirebaseAnalytics
import com.traydcorp.newdio.MainActivity
import com.traydcorp.newdio.R
import com.traydcorp.newdio.dataModel.NewsDetail
import com.traydcorp.newdio.databinding.FragmentAutoPlayBinding
import com.traydcorp.newdio.ui.home.HomeActivity
import com.traydcorp.newdio.ui.player.PlayerFragment
import com.traydcorp.newdio.ui.player.PlayerService
import com.traydcorp.newdio.utils.SharedPreference
import java.util.*


class AutoPlayFragment : Fragment() {

    private var viewBinding : FragmentAutoPlayBinding? = null
    private val bind get() = viewBinding!!

    private val sharedPreferences = SharedPreference()

    private var autoPlay : String? = null
    private var autoPlayBoolean : Boolean = false

    private lateinit var firebaseAnalytics: FirebaseAnalytics

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        firebaseAnalytics = (activity as HomeActivity).firebaseAnalytics

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        viewBinding = FragmentAutoPlayBinding.inflate(inflater, container, false)

        autoPlay = sharedPreferences.getShared(requireContext(), "autoPlay")


        return bind.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (autoPlay == null){ // 설정 된 값이 없을 때 기본설정 켜짐
            bind.autoPlayText.text = getString(R.string.settings_auto_play_on)
            autoPlay = "true"
        } else {
            when (autoPlay) {
                "true" -> getString(R.string.settings_auto_play_on)
                "false" -> getString(R.string.settings_auto_play_off)
                else -> null
            }?.let {
                bind.autoPlayText.text = it
            }
        }

        bind.autoPlayTextBox.setOnClickListener {
            // 언어 선택 dialog
            val dialog = BottomSheetDialog(requireContext())
            val view = layoutInflater.inflate(R.layout.fragment_language_bottom, null)
            dialog.setContentView(view)
            dialog.show()

            val autoPlayOn = view.findViewById<ConstraintLayout>(R.id.korean)
            val autoPlayOff = view.findViewById<ConstraintLayout>(R.id.english)
            val titleText = view.findViewById<TextView>(R.id.titleText)
            val autoPlayOnText = view.findViewById<TextView>(R.id.koreanText)
            val autoPlayOffText = view.findViewById<TextView>(R.id.notSelectText)
            val cancelBtn = view.findViewById<AppCompatButton>(R.id.cancelBtn)

            titleText.text = getString(R.string.settings_auto_play_please)
            autoPlayOnText.text = getString(R.string.settings_auto_play_on)
            autoPlayOffText.text = getString(R.string.settings_auto_play_off)

            autoPlayOn.setOnClickListener { // 켜짐
                bind.autoPlayText.text = getString(R.string.settings_auto_play_on)
                autoPlay = "true"
                autoPlayBoolean = true
                dialog.dismiss()
            }

            autoPlayOff.setOnClickListener { // 꺼짐
                bind.autoPlayText.text = getString(R.string.settings_auto_play_off)
                autoPlay = "false"
                autoPlayBoolean = false
                dialog.dismiss()
            }

            cancelBtn.setOnClickListener { // 취소
                dialog.dismiss()
            }

        }

        bind.nextBtn.setOnClickListener { // 완료 버튼
            // firebase log
            val currentTime = (Calendar.getInstance().timeInMillis/1000).toInt()
            val parameters = Bundle().apply {
                this.putString("screen", "setting")
                this.putString("action", "click")
                this.putInt("time", currentTime)
                this.putString("type", "setting")
                when (autoPlay) {
                    "true" -> this.putInt("auto_play", 1)
                    "false" -> this.putInt("auto_play", 0)
                }
            }
            firebaseAnalytics.logEvent("newdio", parameters)

            sharedPreferences.setShared("autoPlay", autoPlay!!, requireContext())

            if (requireActivity().supportFragmentManager.backStackEntryCount != 0){
                // 설정 - 음성 자동재생 업데이트
                val settingFragment : SettingFragment = requireActivity().supportFragmentManager.findFragmentByTag("setting") as SettingFragment
                settingFragment.updateAutoplaySetting()
                // 재생중인 플레이어가 있으면 음성 자동재생 설정 업데이트
                if (requireActivity().supportFragmentManager.findFragmentByTag("player") != null) {
                    val player : PlayerFragment = requireActivity().supportFragmentManager.findFragmentByTag("player") as PlayerFragment
                    player.autoPlay = autoPlayBoolean
                }
                requireActivity().supportFragmentManager.popBackStack()
            }

        }

        // 뒤로가기
        bind.backBtn.setOnClickListener {
            if (requireActivity().supportFragmentManager.backStackEntryCount != 0){
                requireActivity().supportFragmentManager.popBackStack()
            } else {
                val intent = Intent(context, MainActivity::class.java)
                startActivity(intent)
            }
        }

    }


}