package com.traydcorp.newdio.ui.home


import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.traydcorp.newdio.R
import com.traydcorp.newdio.databinding.FragmentErrorBinding


class ErrorFragment : Fragment() {

    companion object {
        fun newInstance() = ErrorFragment()
    }

    private var viewBinding : FragmentErrorBinding? = null
    private val bind get() = viewBinding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d("error fragment", "create called")

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        viewBinding = FragmentErrorBinding.inflate(inflater, container, false)

        val error = arguments?.getString("key")
        if (error == "serverError"){
            bind.errorText.text = getString(R.string.error_server_title)
            bind.errorText2.text = getString(R.string.error_server_subtitle)
        }

        return bind.root
    }


}