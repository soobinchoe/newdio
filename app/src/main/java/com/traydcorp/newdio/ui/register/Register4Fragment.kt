package com.traydcorp.newdio.ui.register

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.activity.OnBackPressedCallback
import com.traydcorp.newdio.R
import com.traydcorp.newdio.databinding.FragmentRegister4Binding
import com.traydcorp.newdio.ui.home.HomeActivity
import com.traydcorp.newdio.utils.SharedPreference
import com.traydcorp.newdio.utils.retofitService.RetrofitService
import com.traydcorp.newdio.utils.retrofitAPI.RetrofitClient
import retrofit2.Retrofit


class Register4Fragment : Fragment() {

    private var viewBinding : FragmentRegister4Binding? = null
    private val bind get() = viewBinding!!

    val sharedPreferences = SharedPreference()

    private lateinit var retrofit: Retrofit
    private lateinit var supplementService: RetrofitService

    private lateinit var callback: OnBackPressedCallback

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        retrofit = RetrofitClient.getInstance()
        supplementService = retrofit.create(RetrofitService::class.java)

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        viewBinding = FragmentRegister4Binding.inflate(inflater, container, false)

        val backBtn : ImageView? = requireActivity().findViewById(R.id.backBtn)
        val searchBtn : ImageView? = requireActivity().findViewById(R.id.searchBtn)
        backBtn?.visibility = View.GONE
        searchBtn?.visibility = View.GONE

        val access = arguments?.getString("access")
        val refresh = arguments?.getString("refresh")
        if (access != null) {
            sharedPreferences.setShared("access_token", access, requireContext())
        }
        if (refresh != null) {
            sharedPreferences.setShared("refresh_token", refresh, requireContext())
        }

        bind.startBtn.setOnClickListener {
            val intent = Intent(context, HomeActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            intent.putExtra("reset", "reset")
            startActivity(intent)
            activity?.finish()
        }


        return bind.root
    }

    // 뒤로가기 막기
    override fun onAttach(context: Context) {
        super.onAttach(context)
        callback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
            }
        }
        requireActivity().onBackPressedDispatcher.addCallback(this, callback)
    }

}