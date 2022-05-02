package com.samin.carerobot

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.samin.carerobot.Logics.SharedViewModel
import com.samin.carerobot.databinding.FragmentLoginBinding

private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

class LoginFragment : Fragment() {
    private var param1: String? = null
    private var param2: String? = null
    private lateinit var mBinding: FragmentLoginBinding
    private var activity: MainActivity? = null
    private lateinit var onBackPressedCallback: OnBackPressedCallback
    private val sharedViewModel by activityViewModels<SharedViewModel>()

    override fun onAttach(context: Context) {
        super.onAttach(context)
        activity = getActivity() as MainActivity
        onBackPressedCallback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {

            }
        }
        requireActivity().onBackPressedDispatcher.addCallback(this, onBackPressedCallback)
    }

    override fun onDetach() {
        super.onDetach()
        activity = null
        onBackPressedCallback.remove()
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            param1 = it.getString(ARG_PARAM1)
            param2 = it.getString(ARG_PARAM2)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        mBinding = FragmentLoginBinding.inflate(inflater, container, false)
        setButtonClickEvent()
        return mBinding.root
    }

    private fun setButtonClickEvent(){
        mBinding.btnJoin.setOnClickListener {
            onClick(mBinding.btnJoin)
        }
        mBinding.btnLogin.setOnClickListener {
            onClick(mBinding.btnLogin)
        }
        mBinding.tvFind.setOnClickListener {
            onClick(mBinding.tvFind)
        }
    }

    private fun onClick(view:View){
        when(view){
            mBinding.btnJoin -> activity?.onFragmentChange(SharedViewModel.NEWACCOUNTFRAGMENT)
            mBinding.btnLogin -> {
                if (!mBinding.etUserID.text.isNullOrEmpty()){
                    if (!mBinding.etUserPassword.text.isNullOrEmpty()){
                        if (activity?.sharedPreference?.checkUserID(mBinding.etUserID.text.toString())!!) {
                            if (activity?.sharedPreference?.checkUserPassword(
                                    mBinding.etUserID.text.toString(),
                                    mBinding.etUserPassword.text.toString()
                                )!!
                            ) {
                                activity?.onFragmentChange(SharedViewModel.MAINFRAGMENT)
                            } else {
                                Toast.makeText(
                                    requireContext(),
                                    "비밀번호가 틀립니다.",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        } else {
                            Toast.makeText(requireContext(), "존재하지 않는 아이디입니다.", Toast.LENGTH_SHORT)
                                .show()
                        }

                    }else{
                        Toast.makeText(requireContext(), "비밀번호를 입력해주세요.", Toast.LENGTH_SHORT).show()
                    }
                }else{
                    Toast.makeText(requireContext(), "아이디를 입력해주세요.", Toast.LENGTH_SHORT).show()
                }
            }
        }

    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment LoginFragment.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            LoginFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }
}