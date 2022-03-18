package com.traydcorp.newdio.ui.register

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.KeyEvent
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import com.traydcorp.newdio.MainActivity
import com.traydcorp.newdio.R
import com.traydcorp.newdio.databinding.FragmentRegister1Binding
import java.text.DecimalFormat


class Register1Fragment : Fragment() {

    private var viewBinding : FragmentRegister1Binding? = null
    private val bind get() = viewBinding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        viewBinding = FragmentRegister1Binding.inflate(inflater, container, false)

        // 뒤로가기
        val backBtn : ImageView? = requireActivity().findViewById(R.id.backBtn)
        backBtn?.setOnClickListener {
            if (requireActivity().supportFragmentManager.backStackEntryCount != 0){
                requireActivity().supportFragmentManager.popBackStack()
            } else {
                val intent = Intent(context, MainActivity::class.java)
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(intent)
            }
        }

        // 생일 패턴 체크
        val birthPattern =
            "^(19[0-9][0-9]|20[0-9][0-9])-(0[0-9]|1[0-2])-(0[1-9]|[1-2][0-9]|3[0-1])$".toRegex()
        var isDeleting = false

        // 숫자 사이 - 자동 생성
        bind.birthday.addTextChangedListener(object : TextWatcher {

            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
            }

            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
            }

            override fun afterTextChanged(p0: Editable?) {
                val editText = bind.birthday
                if ((editText.text.length == 4 || editText.text.length == 7) && !isDeleting) {
                    Log.d("DEBUG", "- 생성")
                    editText.text.append("-")
                }
                isDeleting = false
                if(birthPattern.matches(bind.birthday.text)){ // 올바른 날짜형식
                    bind.subtext3.text = getString(R.string.social_login_correct_date_format)
                    bind.subtext3.setTextColor(Color.parseColor("#00FF00"))
                    bind.nextBtn.setBackgroundResource(R.drawable.custom_register_box)
                } else {
                    bind.subtext3.text = getString(R.string.social_login_incorrect_date_format) // 잘못된 날짜형식
                    bind.subtext3.setTextColor(Color.parseColor("#FF0000"))
                    bind.nextBtn.setBackgroundResource(R.drawable.custom_register_btn)
                }
            }
        })

        bind.birthday.setOnKeyListener { v, keyCode, event ->
            when (keyCode) {
                KeyEvent.KEYCODE_DEL -> {
                    val editText = bind.birthday
                    isDeleting = true

                    if (editText.text.length == 4 || editText.text.length == 7) {
                        val str = editText.text.dropLast(1)
                        editText.setText(str)
                        editText.setSelection(editText.text.length)
                        isDeleting = false
                    }
                }
            }
            false
        }

        // 데이터 담아서 다음페이지로
        bind.nextBtn.setOnClickListener {
            if (birthPattern.matches(bind.birthday.text)){
                val bundle: Bundle = arguments!!
                bundle.putString("birthday", bind.birthday.text.toString())

                val register2Fragment = Register2Fragment()
                register2Fragment.arguments = bundle
                requireActivity().supportFragmentManager.beginTransaction().addToBackStack("register")
                    .replace(R.id.fragmentViewLayout, register2Fragment).commit()
            }
        }

        return bind.root
    }


}