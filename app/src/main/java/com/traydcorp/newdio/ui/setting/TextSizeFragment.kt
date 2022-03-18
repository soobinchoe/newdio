package com.traydcorp.newdio.ui.setting

import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.AppCompatButton
import androidx.constraintlayout.widget.ConstraintLayout
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.firebase.analytics.FirebaseAnalytics
import com.traydcorp.newdio.R
import com.traydcorp.newdio.databinding.FragmentTextSizeBinding
import com.traydcorp.newdio.ui.home.HomeActivity
import com.traydcorp.newdio.ui.live.LiveFragment
import com.traydcorp.newdio.ui.player.PlayerFragment
import com.traydcorp.newdio.utils.SharedPreference
import java.util.*


class TextSizeFragment : Fragment() {

    private var viewBinding : FragmentTextSizeBinding? = null
    private val bind get() = viewBinding!!

    private val sharedPreferences = SharedPreference()

    private lateinit var dialog : BottomSheetDialog
    private lateinit var textSize : String

    private lateinit var firebaseAnalytics: FirebaseAnalytics

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        firebaseAnalytics = (activity as HomeActivity).firebaseAnalytics

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        viewBinding = FragmentTextSizeBinding.inflate(inflater, container, false)

        // 이미 선택된 크기가 있으면 해당 크기 표시, 없으면 보통
        val savedTextSize = sharedPreferences.getShared(requireContext(), "textSize")
        if (savedTextSize == null){
            bind.textSizeText.text = getString(R.string.settings_text_original)
            textSize = "original"
        } else {
            when (savedTextSize) {
                "small" -> getString(R.string.settings_text_small)
                "original" -> getString(R.string.settings_text_original)
                "large" -> getString(R.string.settings_text_large)
                else -> null
            }?.let {
                bind.textSizeText.text = it
                if (it == getString(R.string.settings_text_small)){
                    textSize = "small"
                }
                if (it == getString(R.string.settings_text_original)){
                    textSize = "original"
                }
                if (it == getString(R.string.settings_text_large)){
                    textSize = "large"
                }
            }
        }

        // 선호 크기 선택
        bind.textTextBox.setOnClickListener {
            dialog = BottomSheetDialog(requireContext())
            val view = layoutInflater.inflate(R.layout.fragment_text_size_bottom, null)
            dialog.setContentView(view)
            dialog.show()

            val small = view.findViewById<ConstraintLayout>(R.id.small)
            val medium = view.findViewById<ConstraintLayout>(R.id.medium)
            val large = view.findViewById<ConstraintLayout>(R.id.large)
            val cancelBtn = view.findViewById<AppCompatButton>(R.id.cancelBtn)

            small.setOnClickListener {
                bind.textSizeText.text = getString(R.string.settings_text_small)
                textSize = "small"
                dialog.dismiss()
            }

            medium.setOnClickListener {
                bind.textSizeText.text = getString(R.string.settings_text_original)
                textSize = "original"
                dialog.dismiss()
            }

            large.setOnClickListener {
                bind.textSizeText.text = getString(R.string.settings_text_large)
                textSize = "large"
                dialog.dismiss()
            }

            cancelBtn.setOnClickListener {
                dialog.dismiss()
            }

        }

        // 완료 버튼
        bind.nextBtn.setOnClickListener {
            val currentTime = (Calendar.getInstance().timeInMillis/1000).toInt()
            val parameters = Bundle().apply {
                this.putString("screen", "setting")
                this.putString("action", "click")
                this.putInt("time", currentTime)
                this.putString("type", "setting")
                when (textSize) {
                    "small" -> this.putInt("text_size", -1)
                    "original" -> this.putInt("text_size", 0)
                    "large" -> this.putInt("text_size", 1)
                }
            }
            firebaseAnalytics.logEvent("newdio", parameters)

            context?.let { it1 -> sharedPreferences.setShared("textSize", textSize, it1) }

            // 설정 화면의 텍스크 크기 업데이트
            val settingFragment : SettingFragment = requireActivity().supportFragmentManager.findFragmentByTag("setting") as SettingFragment
            settingFragment.updateTextSizeSetting()

            // 그려진 라이브 뷰가 있으면 변경된 텍스트 크기로 refresh
            if (requireActivity().supportFragmentManager.findFragmentByTag("live") != null) {
                val live : LiveFragment = requireActivity().supportFragmentManager.findFragmentByTag("live") as LiveFragment
                live.getNewLiveList(null)
            }

            // 재생중인 플레이어가 있으면 변경된 텍스트 크기로 refresh
            if (requireActivity().supportFragmentManager.findFragmentByTag("player") != null) {
                val player : PlayerFragment = requireActivity().supportFragmentManager.findFragmentByTag("player") as PlayerFragment
                player.updateTextSize()
            }

            requireActivity().supportFragmentManager.popBackStack()
        }

        // 뒤로가기
        bind.backBtn.setOnClickListener {
            if (requireActivity().supportFragmentManager.backStackEntryCount != 0){
                requireActivity().supportFragmentManager.popBackStack()
            } else {
                val intent = Intent(context, HomeActivity::class.java)
                startActivity(intent)
            }
        }

        return bind.root
    }


}