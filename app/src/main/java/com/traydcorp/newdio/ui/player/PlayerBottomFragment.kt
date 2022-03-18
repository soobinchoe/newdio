package com.traydcorp.newdio.ui.player

import android.content.res.Resources
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.Dimension
import androidx.coordinatorlayout.widget.CoordinatorLayout
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.traydcorp.newdio.R
import com.traydcorp.newdio.databinding.FragmentPlayerBottomBinding
import com.traydcorp.newdio.utils.SharedPreference


class PlayerBottomFragment : BottomSheetDialogFragment() {

    private var viewBinding : FragmentPlayerBottomBinding? = null
    private val bind get() = viewBinding!!

    private val sharedPreferences = SharedPreference()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setStyle(
            STYLE_NORMAL,
            R.style.TransparentBottomSheetDialogFragment
        )

    }

    // bottom sheet 화면크기
    override fun onStart() {
        super.onStart()

        if (dialog != null) {
            val bottomSheet: View = dialog!!.findViewById(R.id.design_bottom_sheet)
            bottomSheet.layoutParams.height = ViewGroup.LayoutParams.MATCH_PARENT
        }

        val view = view
        view!!.post{
            val parent = view.parent as View
            val params = parent.layoutParams as CoordinatorLayout.LayoutParams
            val behavior = params.behavior
            val bottomSheetBehavior = behavior as BottomSheetBehavior<*>?
            bottomSheetBehavior!!.peekHeight = (view.measuredHeight * 0.8).toInt() // 전체 화면의 80%
             parent.setBackgroundColor(Color.TRANSPARENT)
        }

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        viewBinding = FragmentPlayerBottomBinding.inflate(inflater, container, false)


        bind.playerBottomTitle.text = arguments?.getString("engTitle")
        bind.playerBottomContent.text = arguments?.getString("engContent")

        // 텍스트 크기 설정
        val textSize = sharedPreferences.getShared(requireContext(), "textSize")
        if (textSize != null) {
            when (textSize) {
                "small" ->  13F
                "original" ->  15F
                "large" ->  18F
                else -> null
            }?.let {
                Log.d("text size", it.toFloat().toString())
                bind.playerBottomContent.setTextSize(Dimension.SP, it)
            }
        }


        return bind.root
    }


}