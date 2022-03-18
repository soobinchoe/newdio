package com.traydcorp.newdio.ui.register


import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.appcompat.widget.AppCompatButton
import androidx.constraintlayout.widget.ConstraintLayout
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.traydcorp.newdio.MainActivity
import com.traydcorp.newdio.R
import com.traydcorp.newdio.databinding.FragmentRegister2Binding


class Register2Fragment : Fragment() {

    private var viewBinding : FragmentRegister2Binding? = null
    private val bind get() = viewBinding!!


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)



    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        viewBinding = FragmentRegister2Binding.inflate(inflater, container, false)

        // 뒤로가기
        val backBtn : ImageView? = requireActivity().findViewById(R.id.backBtn)
        backBtn?.setOnClickListener {
            if (requireActivity().supportFragmentManager.backStackEntryCount != 0){
                requireActivity().supportFragmentManager.popBackStack()
            } else {
                val intent = Intent(context, MainActivity::class.java)
                startActivity(intent)
            }
        }

        // 이전 페이지에서 받아온 데이터
        val accessToken = arguments?.getString("accessToken")
        val socialName = arguments?.getString("socialName")
        val birthday = arguments?.getString("birthday")

        bind.genderSelectBottomSheet.visibility = View.GONE

        bind.genderTextBox.setOnClickListener {
            // 성별 선택 dialog
            val dialog = BottomSheetDialog(requireContext())
            val view = layoutInflater.inflate(R.layout.fragment_register2_bottom, null)
            dialog.setContentView(view)
            dialog.show()

            // dialog 버튼
            val male = view.findViewById<ConstraintLayout>(R.id.male)
            val female = view.findViewById<ConstraintLayout>(R.id.female)
            val notSelect = view.findViewById<ConstraintLayout>(R.id.notSelect)
            val cancelBtn = view.findViewById<AppCompatButton>(R.id.cancelBtn)

            // on click listener
            male.setOnClickListener {
                bind.genderText.text = getString(R.string.social_login_male)
                bind.genderText.setTextColor(Color.parseColor("#FFFFFF"))
                bind.nextBtn.setBackgroundResource(R.drawable.custom_register_box)
                dialog.dismiss()
            }
            female.setOnClickListener {
                bind.genderText.text = getString(R.string.social_login_female)
                bind.genderText.setTextColor(Color.parseColor("#FFFFFF"))
                bind.nextBtn.setBackgroundResource(R.drawable.custom_register_box)
                dialog.dismiss()
            }
            notSelect.setOnClickListener {
                bind.genderText.text = getString(R.string.social_login_genderless)
                bind.genderText.setTextColor(Color.parseColor("#FFFFFF"))
                bind.nextBtn.setBackgroundResource(R.drawable.custom_register_box)
                dialog.dismiss()
            }
            cancelBtn.setOnClickListener {
                dialog.dismiss()
            }

            // 데이터 담아서 다음페이지로
            if (bind.genderText.text != null){
                bind.nextBtn.setOnClickListener {
                    val bundle: Bundle = arguments!!
                    var gender = ""
                    if (bind.genderText.text.equals(getString(R.string.social_login_male))){
                        gender = "M"
                    } else if (bind.genderText.text.equals(getString(R.string.social_login_female))){
                        gender = "F"
                    } else {
                        gender = "N"
                    }
                    bundle.putString("gender", gender)

                    Log.d("bundle", bundle.toString())

                    val register3Fragment = Register3Fragment()
                    register3Fragment.arguments = bundle
                    requireActivity().supportFragmentManager.beginTransaction().addToBackStack("register")
                        .replace(R.id.fragmentViewLayout, register3Fragment, "register3").commit()
                }
            } else {
                // 성별 선택 경고창?
            }
        }

        return bind.root
    }

}