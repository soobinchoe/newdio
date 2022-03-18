package com.traydcorp.newdio.ui.player

import android.content.Intent
import android.graphics.Color
import android.graphics.Point
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager
import androidx.annotation.RequiresApi
import androidx.fragment.app.DialogFragment
import com.traydcorp.newdio.MainActivity
import com.traydcorp.newdio.R
import com.traydcorp.newdio.databinding.FragmentDialogBinding
import com.traydcorp.newdio.databinding.FragmentLoginPopUpBinding
import android.util.DisplayMetrics
import android.view.Display


class LoginPopUpFragment : DialogFragment() {

    private var viewBinding : FragmentLoginPopUpBinding? = null
    private val bind get() = viewBinding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        viewBinding = FragmentLoginPopUpBinding.inflate(inflater, container, false)
        // dialog 배경 투명하게
        dialog?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        // dialog 포지션
        val window: Window? = dialog?.window
        window?.setGravity(Gravity.BOTTOM)

        return bind.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        bind.loginPopupBtn.setOnClickListener {
            val intent = Intent(context, MainActivity::class.java)
            startActivity(intent)
            dismiss()
        }

        bind.deleteBtn.setOnClickListener {
            dismiss()
        }

    }

    override fun onResume() {
        super.onResume()

        val size = Point()
        var deviceWidth = 0

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val display = requireContext().display
            display!!.getSize(size)
            deviceWidth = size.x
        } else {
            val displayMetrics = DisplayMetrics()
            activity?.windowManager?.defaultDisplay?.getMetrics(displayMetrics)
            deviceWidth = displayMetrics.widthPixels
        }

        val params: ViewGroup.LayoutParams? = dialog?.window?.attributes

        params?.width = deviceWidth
        dialog?.window?.attributes = params as WindowManager.LayoutParams
    }

}