package com.traydcorp.newdio.ui.live

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.Log
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import com.traydcorp.newdio.databinding.FragmentInfoDialogBinding
import android.view.Gravity
import android.view.Window
import android.view.WindowManager

// discover와 live의 help&info dialog
class InfoDialogFragment : DialogFragment() {

    private var viewBinding : FragmentInfoDialogBinding? = null
    private val bind get() = viewBinding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        viewBinding = FragmentInfoDialogBinding.inflate(inflater, container, false)

        // dialog 배경 투명하게
        dialog?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        val from = arguments?.getString("from")

        // dialog 포지션

        val window: Window? = dialog?.window
        window?.attributes?.x = dpToPx(requireContext(), 28f).toInt()
        window?.attributes?.y = dpToPx(requireContext(), 52f).toInt()

        if (from == "live") {
            bind.liveExtra.visibility = View.VISIBLE
        }

        bind.liveInfoText.text = arguments?.getString("key")
        window?.setGravity(Gravity.TOP or Gravity.END)
        dialog?.requestWindowFeature(Window.FEATURE_NO_TITLE)

        return bind.root
    }

    fun dpToPx(context: Context, dp: Float): Float {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, context.resources.displayMetrics)
    }



}