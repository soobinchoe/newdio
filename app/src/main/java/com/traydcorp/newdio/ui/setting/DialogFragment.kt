package com.traydcorp.newdio.ui.setting

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Point
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.annotation.RequiresApi
import androidx.fragment.app.DialogFragment
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.gson.JsonObject
import com.traydcorp.newdio.R
import com.traydcorp.newdio.databinding.FragmentDialogBinding
import com.traydcorp.newdio.ui.home.HomeActivity
import com.traydcorp.newdio.ui.register.RegisterActivity
import com.traydcorp.newdio.utils.SharedPreference
import com.traydcorp.newdio.utils.retofitService.RetrofitService
import com.traydcorp.newdio.utils.retrofitAPI.RetrofitClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import java.util.*


class DialogFragment : DialogFragment(){
// DialogFragment(private val key) : DialogFragment()
    private var viewBinding : FragmentDialogBinding? = null
    private val bind get() = viewBinding!!

    val sharedPreferences = SharedPreference()

    private lateinit var access : String
    private lateinit var refresh : String

    private lateinit var retrofit: Retrofit
    private lateinit var supplementService: RetrofitService

    private lateinit var listener: NoticeDialogListener

    private lateinit var firebaseAnalytics: FirebaseAnalytics

    private var parameters = Bundle()
    private var currentTime : Int = 0

    interface NoticeDialogListener {
        fun onDialogPositiveClick(dialog: DialogFragment)
        fun onDialogNegativeClick(dialog: DialogFragment)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        try {
            listener = context as NoticeDialogListener
        } catch (e: ClassCastException) {
            throw ClassCastException((context.toString() +
                    " must implement NoticeDialogListener"))
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (arguments?.getString("key") != "Register") {
            firebaseAnalytics = (activity as HomeActivity).firebaseAnalytics
        }

        access = sharedPreferences.getShared(context, "access_token").toString()
        refresh = sharedPreferences.getShared(context, "refresh_token").toString()

        retrofit = RetrofitClient.getInstance()
        supplementService = retrofit.create(RetrofitService::class.java)

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        viewBinding = FragmentDialogBinding.inflate(inflater, container, false)
        dialog?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        // x
        val key = arguments?.getString("key")

        when(key) {
            "Delete" -> { // 탈퇴
                bind.dialogContent.text = getString(R.string.popup_secession)
                bind.dialogConfirm.setOnClickListener {
                    parameters = Bundle().apply {
                        this.putString("action", "click")
                        this.putString("type", "delete")
                    }
                    sendFirebaseLog(parameters)

                    deleteAccount(supplementService.deleteUser(access, refresh))
                    activity?.window?.setFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
                        WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
                }
                bind.dialogCancel.setOnClickListener {
                    dismiss()
                }
            }
            "Logout" -> { // 로그아웃
                bind.dialogContent.text = getString(R.string.popup_logout)
                bind.dialogConfirm.setOnClickListener {
                    parameters = Bundle().apply {
                        this.putString("action", "click")
                        this.putString("type", "logout")
                    }
                    sendFirebaseLog(parameters)

                    sharedPreferences.sharedClear(requireContext(), "access_token")
                    sharedPreferences.sharedClear(requireContext(), "refresh_token")
                    sharedPreferences.sharedClear(requireContext(), "language")
                    sharedPreferences.sharedClear(requireContext(), "textSize")
                    sharedPreferences.sharedClear(requireContext(), "userEmail")
                    sharedPreferences.sharedClear(requireContext(), "provider")
                    sharedPreferences.sharedClear(requireContext(), "userId")

                    if (requireActivity().supportFragmentManager.findFragmentByTag("5") != null) {
                        val setting : SettingFragment = requireActivity().supportFragmentManager.findFragmentByTag("setting") as SettingFragment
                        requireActivity().supportFragmentManager.beginTransaction().remove(setting)
                    }

                    val intent = requireContext().packageManager.getLaunchIntentForPackage(context!!.packageName)
                    intent?.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    intent?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    intent?.putExtra("reset", "reset")
                    startActivity(intent)
                    activity?.finish()



                }
                bind.dialogCancel.setOnClickListener {
                    dismiss()
                }
            }
            "Register" -> { // 회원가입
                bind.dialogContent.text = getString(R.string.social_login_notification)
                bind.dialogConfirm.setOnClickListener {
                    listener.onDialogPositiveClick(this)
                    dismiss()
                }
                bind.dialogCancel.setOnClickListener {
                    dismiss()
                }
            }
            "DeleteAll" -> { // 전체 삭제
                bind.dialogContent.text = getString(R.string.search_delete_all_question)
                bind.dialogConfirm.setOnClickListener {
                    listener.onDialogPositiveClick(this)
                    dismiss()
                }
                bind.dialogCancel.setOnClickListener {
                    dismiss()
                }
            }
        }


        return bind.root
    }


    @RequiresApi(Build.VERSION_CODES.R)
    override fun onResume() {
        super.onResume()

        val display = context?.display
        val size = Point()
        display!!.getSize(size)

        val params: ViewGroup.LayoutParams? = dialog?.window?.attributes
        val deviceWidth = size.x
        params?.width = (deviceWidth * 0.76).toInt()
        dialog?.window?.attributes = params as WindowManager.LayoutParams
    }

    // 회원 탈퇴 api
    private fun deleteAccount(service: Call<JsonObject>) {
        service.enqueue(object : Callback<JsonObject> {
            override fun onResponse(call: Call<JsonObject>, response: Response<JsonObject>) {
                if (response.isSuccessful) {
                    context?.let { sharedPreferences.sharedClear(it, "access_token") }
                    context?.let { sharedPreferences.sharedClear(it, "refresh_token") }
                    context?.let { sharedPreferences.sharedClear(it, "language") }
                    context?.let { sharedPreferences.sharedClear(it, "textSize") }
                    context?.let { sharedPreferences.sharedClear(it, "userEmail") }
                    context?.let { sharedPreferences.sharedClear(it, "provider") }
                    context?.let { sharedPreferences.sharedClear(it, "userId") }

                    val intent = requireContext().packageManager.getLaunchIntentForPackage(context!!.packageName)
                    intent?.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    intent?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    startActivity(intent)
                    activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
                    activity?.finish()
                } else if (response.code() == 204) {
                    // no content
                } else if (response.code() == 403) {
                    // 토큰 검증 실패시
                } else if (response.code() == 404) {
                    // 유저가 없을시
                }
            }

            override fun onFailure(call: Call<JsonObject>, t: Throwable) {
                Log.d("delete ErrorMessage", t.message.toString())
            }

        })
    }

    private fun sendFirebaseLog(param: Bundle) {
        currentTime = (Calendar.getInstance().timeInMillis/1000).toInt()
        param.apply {
            this.putString("screen", "setting")
            this.putInt("time", currentTime)
        }
        firebaseAnalytics.logEvent("newdio", param)
    }



}