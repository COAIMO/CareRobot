package com.samin.carerobot

import android.content.Context
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.activityViewModels
import com.samin.carerobot.Logics.SharedPreference
import com.samin.carerobot.Logics.SharedViewModel
import com.samin.carerobot.databinding.FragmentNewAccountBinding
import com.samin.carerobot.databinding.FragmentSelectModeBinding

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [NewAccountFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class NewAccountFragment : Fragment() {
    private var param1: String? = null
    private var param2: String? = null
    private lateinit var mBinding: FragmentNewAccountBinding
    private lateinit var onBackPressedCallback: OnBackPressedCallback
    private var activity: MainActivity? = null
    private lateinit var selectedModeFragment: SelectedModeFragment
    private val sharedViewModel by activityViewModels<SharedViewModel>()

    override fun onAttach(context: Context) {
        super.onAttach(context)
        activity = getActivity() as MainActivity
        onBackPressedCallback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                activity?.onFragmentChange(SharedViewModel.LOGINFRAGMENT)
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
        mBinding = FragmentNewAccountBinding.inflate(inflater, container, false)
        mBinding.btnLogin.setOnClickListener {
            if (!mBinding.etUserID.text.isNullOrBlank()) {
                if (!mBinding.etUserPassword.text.isNullOrBlank() && !mBinding.etUserPasswordConfirm.text.isNullOrBlank()) {
                    if (!mBinding.etUserPhoneNumber.text.isNullOrBlank()) {
                        if (mBinding.etUserPassword.text.toString() == mBinding.etUserPasswordConfirm.text.toString()) {
                            activity?.sharedPreference?.saveUserInfo(SharedPreference.USER_NAME, mBinding.etUserID.text.toString())
                            activity?.sharedPreference?.saveUserInfo(mBinding.etUserID.text.toString(), mBinding.etUserPassword.text.toString())
                            activity?.sharedPreference?.saveUserInfo(mBinding.etUserPhoneNumber.text.toString(), mBinding.etUserID.text.toString())
                            Toast.makeText(requireContext(), "계정이 생성되었습니다.", Toast.LENGTH_SHORT).show()
                            activity?.onFragmentChange(SharedViewModel.LOGINFRAGMENT)
                        }else{
                            Toast.makeText(requireContext(), "비밀번호가 일치하지 않습니다.", Toast.LENGTH_SHORT).show()
                        }
                    }else{
                        Toast.makeText(requireContext(), "휴대폰 번호를 입력해주세요.", Toast.LENGTH_SHORT).show()
                    }
                }else{
                    Toast.makeText(requireContext(), "비밀번호을 입력해주세요.", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(requireContext(), "이름을 입력해주세요.", Toast.LENGTH_SHORT).show()
            }
        }
        mBinding.btnBack.setOnClickListener {
            activity?.onFragmentChange(SharedViewModel.LOGINFRAGMENT)
        }


        return mBinding.root
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment NewAccountFragment.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            NewAccountFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }
}