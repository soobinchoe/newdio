package com.traydcorp.newdio.ui.setting

import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import com.traydcorp.newdio.R
import com.traydcorp.newdio.databinding.FragmentDeleteBinding


class DeleteFragment : Fragment() {

    private var viewBinding : FragmentDeleteBinding? = null
    private val bind get() = viewBinding!!

    private var bundle = Bundle()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        viewBinding = FragmentDeleteBinding.inflate(inflater, container, false)

        return bind.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        var isChecked = false

        // 동의 체크
        bind.check.setOnClickListener {
            if (!isChecked){
                bind.checkBox.setBackgroundResource(R.drawable.ic_general_check_on)
                bind.checkBox.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.light_green))
                bind.checkText.setTextColor(Color.parseColor("#69DB7C"))
                bind.deleteBtn.setBackgroundResource(R.drawable.custom_register_box)
                isChecked = true
            } else {
                bind.checkBox.setBackgroundResource(R.drawable.ic_general_check_off)
                bind.checkBox.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.white))
                bind.checkText.setTextColor(Color.parseColor("#919191"))
                bind.deleteBtn.setBackgroundResource(R.drawable.custom_register_box_grey)
                isChecked = false
            }

        }

        // 회원 탈퇴 버튼
        bind.deleteBtn.setOnClickListener {
            if (isChecked) {
                val dialog = DialogFragment()
                bundle.putString("key", "Delete")
                dialog.arguments = bundle
                dialog.show(parentFragmentManager, "dialog")
            }

        }

        // 뒤로가기
        bind.backBtn.setOnClickListener {
            if (requireActivity().supportFragmentManager.backStackEntryCount != 0){
                requireActivity().supportFragmentManager.popBackStack()
            }
        }



    }


}