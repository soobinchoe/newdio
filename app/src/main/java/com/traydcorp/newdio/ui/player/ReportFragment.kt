package com.traydcorp.newdio.ui.player

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Toast
import androidx.appcompat.widget.AppCompatButton
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.coordinatorlayout.widget.CoordinatorLayout
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.gson.JsonObject
import com.traydcorp.newdio.R
import com.traydcorp.newdio.dataModel.ReportBody
import com.traydcorp.newdio.databinding.FragmentReportBinding
import com.traydcorp.newdio.utils.SharedPreference
import com.traydcorp.newdio.utils.retofitService.RetrofitService
import com.traydcorp.newdio.utils.retrofitAPI.RetrofitClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit


class ReportFragment : BottomSheetDialogFragment() {

    private var viewBinding : FragmentReportBinding? = null
    private val bind get() = viewBinding!!

    private lateinit var bottomSheetBehavior : BottomSheetBehavior<*>

    private val sharedPreferences = SharedPreference()
    private lateinit var retrofit: Retrofit
    private lateinit var supplementService: RetrofitService

    private var access : String? = null
    private var refresh : String? = null

    private lateinit var reportType : String
    private var crawlingdata: Int = 0


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        access = sharedPreferences.getShared(context, "access_token")
        refresh = sharedPreferences.getShared(context, "refresh_token")

        retrofit = RetrofitClient.getInstance()
        supplementService = retrofit.create(RetrofitService::class.java)
    }

    // 전체화면으로 보이게
    override fun onStart() {
        super.onStart()

        dialog?.let {
            val bottomSheet: View = dialog!!.findViewById(R.id.design_bottom_sheet)
            bottomSheet.layoutParams.height = ViewGroup.LayoutParams.MATCH_PARENT
        }

        val view = view
        view!!.post{
            val parent = view.parent as View
            val params = parent.layoutParams as CoordinatorLayout.LayoutParams
            val behavior = params.behavior
            bottomSheetBehavior = (behavior as BottomSheetBehavior<*>?)!!
            bottomSheetBehavior.peekHeight = view.measuredHeight

        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        viewBinding = FragmentReportBinding.inflate(inflater, container, false)

        crawlingdata = arguments?.getInt("crawlingdata")!!

        // 초기값 기타
        reportType = "Etc"
        bind.reportText.text = getString(R.string.player_report_etc)

        // 뒤로가기
        bind.backBtn.setOnClickListener {
            dialog?.dismiss()
        }

        bind.reportTextBox.setOnClickListener {
            val dialog = BottomSheetDialog(requireContext())
            val view = layoutInflater.inflate(R.layout.fragment_report_bottom, null)
            dialog.setContentView(view)
            dialog.show()

            // dialog 버튼
            val trans = view.findViewById<ConstraintLayout>(R.id.trans)
            val summary = view.findViewById<ConstraintLayout>(R.id.summary)
            val sound = view.findViewById<ConstraintLayout>(R.id.sound)
            val etc = view.findViewById<ConstraintLayout>(R.id.etc)
            val cancelBtn = view.findViewById<AppCompatButton>(R.id.cancelBtn)

            cancelBtn.setOnClickListener { // 취소
                dialog.dismiss()
            }

            trans.setOnClickListener { // 번역 오류
                bind.reportText.text = getString(R.string.player_report_translation)
                reportType = "Translate"
                dialog.dismiss()
            }

            summary.setOnClickListener { // 요약 오류
                bind.reportText.text = getString(R.string.player_report_summary)
                reportType = "Summarize"
                dialog.dismiss()
            }

            sound.setOnClickListener { // 음성 오류
                bind.reportText.text = getString(R.string.player_report_sound)
                reportType = "Voice"
                dialog.dismiss()
            }

            etc.setOnClickListener { // 기타
                bind.reportText.text = getString(R.string.player_report_etc)
                reportType = "Etc"
                dialog.dismiss()
            }

        }

        // 신고 text box
        bind.reportEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            }

            override fun afterTextChanged(s: Editable?) {
                val editText = bind.reportEditText
                if (editText.text.length in 5..500) { // 5자에서 500자까지
                    // 신고 버튼 활성화
                    bind.nextBtn.setBackgroundResource(R.drawable.custom_register_box)
                    bind.nextBtn.setOnClickListener {
                        bind.nextBtn.isClickable = false
                        val reportBody = ReportBody(reportType, editText.text.toString())
                        if (access != null && refresh != null) {
                            reportNews(supplementService.reportNews(access, refresh, crawlingdata, reportBody))
                        } else {
                            reportNews(supplementService.reportNews(null, null, crawlingdata, reportBody))
                        }

                    }
                } else {
                    bind.nextBtn.setBackgroundResource(R.drawable.custom_register_box_grey)
                }
            }
        })
        return bind.root
    }

    // 신고 api
    private fun reportNews(service: Call<JsonObject>) {

        Handler(Looper.getMainLooper()).postDelayed({
            service.enqueue(object : Callback<JsonObject> {
                override fun onResponse(call: Call<JsonObject>, response: Response<JsonObject>) {
                    if (response.code() == 201) {
                        dialog?.dismiss()
                        activity?.window?.clearFlags(
                            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)

                        // 액세스 토큰 만료시 새로 받은 토큰으로 업데이트
                        val newAccess = response.headers()["Authorization"]
                        if (newAccess != null) {
                            sharedPreferences.setShared("access_token", newAccess, requireContext())
                        }
                    } else {
                        Toast.makeText(context, getString(R.string.error_server_title), Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<JsonObject>, t: Throwable) {
                    Toast.makeText(context, getString(R.string.error_server_title), Toast.LENGTH_SHORT).show()
                }

            })
        }, 1000)
    }



}