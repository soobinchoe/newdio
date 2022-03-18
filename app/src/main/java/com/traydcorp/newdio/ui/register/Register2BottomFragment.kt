package com.traydcorp.newdio.ui.register


import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.traydcorp.newdio.R
import com.traydcorp.newdio.databinding.FragmentRegister2BottomBinding


class Register2BottomFragment : BottomSheetDialogFragment() {

    private var viewBinding : FragmentRegister2BottomBinding? = null
    private val bind get() = viewBinding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        viewBinding = FragmentRegister2BottomBinding.inflate(inflater, container, false)


        return bind.root
    }



}