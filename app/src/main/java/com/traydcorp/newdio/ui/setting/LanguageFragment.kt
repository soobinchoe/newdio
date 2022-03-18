package com.traydcorp.newdio.ui.setting

import android.content.Intent
import android.content.res.Configuration
import android.content.res.Resources
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.AppCompatButton
import androidx.constraintlayout.widget.ConstraintLayout
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.firebase.analytics.FirebaseAnalytics
import com.traydcorp.newdio.MainActivity
import com.traydcorp.newdio.R
import com.traydcorp.newdio.databinding.FragmentLanguageBinding
import com.traydcorp.newdio.ui.home.HomeActivity
import com.traydcorp.newdio.ui.player.PlayerFragment
import com.traydcorp.newdio.utils.SharedPreference
import java.util.*


class LanguageFragment : Fragment() {

    private var viewBinding : FragmentLanguageBinding? = null
    private val bind get() = viewBinding!!

    private val sharedPreferences = SharedPreference()

    private lateinit var language : String

    private lateinit var firebaseAnalytics: FirebaseAnalytics

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        firebaseAnalytics = (activity as HomeActivity).firebaseAnalytics

        language = (activity as HomeActivity).getLanguagePreference()

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        viewBinding = FragmentLanguageBinding.inflate(inflater, container, false)

        if (language == "ko"){
            bind.languageText.text = getString(R.string.settings_language_ko)
        } else {
            bind.languageText.text = getString(R.string.settings_language_en)
        }

        return bind.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 뒤로가기
        bind.backBtn.setOnClickListener {
            if (requireActivity().supportFragmentManager.backStackEntryCount != 0){
                requireActivity().supportFragmentManager.popBackStack()
            } else {
                val intent = Intent(context, MainActivity::class.java)
                startActivity(intent)
            }
        }

        bind.languageTextBox.setOnClickListener {
            // 언어 선택 dialog
            val dialog = BottomSheetDialog(requireContext())
            val view = layoutInflater.inflate(R.layout.fragment_language_bottom, null)
            dialog.setContentView(view)
            dialog.show()

            val korean = view.findViewById<ConstraintLayout>(R.id.korean)
            val english = view.findViewById<ConstraintLayout>(R.id.english)
            val cancelBtn = view.findViewById<AppCompatButton>(R.id.cancelBtn)


            // 한국어
            korean.setOnClickListener {
                bind.languageText.text = getString(R.string.settings_language_ko)
                language = "ko"
                dialog.dismiss()
            }

            // 영어
            english.setOnClickListener {
                bind.languageText.text = getString(R.string.settings_language_en)
                language = "en"
                dialog.dismiss()
            }

            // 취소
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
                this.putString("language", language)
            }
            firebaseAnalytics.logEvent("newdio", parameters)

            sharedPreferences.setShared("language", language, context!!)

            // 재생중인 플레이어가 있으면 종료
            if (requireActivity().supportFragmentManager.findFragmentByTag("player") != null){
                val player = requireActivity().supportFragmentManager.findFragmentByTag("player") as PlayerFragment
                player.disconnectMedia()
                player.dismiss()
            }

            // 앱 언어 설정 변경
            val locale = Locale(language)
            Locale.setDefault(locale)
            val resources: Resources = activity!!.resources
            val config: Configuration = resources.configuration
            config.setLocale(locale)
            resources.updateConfiguration(config, resources.displayMetrics)

            // 앱 새로 시작
            val intent = requireContext().packageManager.getLaunchIntentForPackage(context!!.packageName)
            intent!!.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
            intent.addFlags(Intent.	FLAG_ACTIVITY_TASK_ON_HOME)
            startActivity(intent)
            activity?.finish()

        }

    }


}