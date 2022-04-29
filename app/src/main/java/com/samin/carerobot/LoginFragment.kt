package com.samin.carerobot

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import com.samin.carerobot.Logics.SharedViewModel
import com.samin.carerobot.databinding.FragmentLoginBinding

private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

class LoginFragment : Fragment() {
    private var param1: String? = null
    private var param2: String? = null
    private lateinit var mBinding: FragmentLoginBinding
    private var activity: MainActivity? = null
    private lateinit var onBackPressed: OnBackPressedCallback

    override fun onAttach(context: Context) {
        super.onAttach(context)
        activity = getActivity() as MainActivity

    }

    override fun onDetach() {
        super.onDetach()
        activity = null
//        onBackPressed.remove()
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
            mBinding.btnLogin -> activity?.onFragmentChange(SharedViewModel.MAINFRAGMENT)
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