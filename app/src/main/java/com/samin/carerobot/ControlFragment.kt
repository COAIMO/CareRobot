package com.samin.carerobot

import android.content.Context
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.activityViewModels
import com.samin.carerobot.Logics.SharedViewModel
import com.samin.carerobot.R
import com.samin.carerobot.databinding.FragmentControlBinding
import com.samin.carerobot.databinding.FragmentSelectedModeBinding

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [ControlFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class ControlFragment : Fragment() {
    private var param1: String? = null
    private var param2: String? = null
    private lateinit var onBackPressedCallback: OnBackPressedCallback
    private lateinit var mBinding: FragmentControlBinding
    private var activity: MainActivity? = null
    private val sharedViewModel by activityViewModels<SharedViewModel>()

    override fun onAttach(context: Context) {
        super.onAttach(context)
        activity = getActivity() as MainActivity
        onBackPressedCallback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                parentFragmentManager.beginTransaction()
                    .replace(R.id.mainFragment_container, SelectModeFragment()).commit()
            }
        }
        requireActivity().onBackPressedDispatcher.addCallback(this, onBackPressedCallback)
    }

    private fun backButtonEvent(){
        when(sharedViewModel.viewState.value){
            SharedViewModel.MODE_CARRY_HEAVY -> {

            }
            SharedViewModel.MODE_CARRY_HEIGHT -> {

            }

            SharedViewModel.MODE_BEHAVIOR_STAND -> {

            }
            SharedViewModel.MODE_BEHAVIOR_WALKHAND -> {

            }
            SharedViewModel.MODE_BEHAVIOR_WALKHUG -> {

            }

            SharedViewModel.MODE_CHANGE_CHANGEHUG -> {

            }
            SharedViewModel.MODE_CHANGE_TRANSFERSTAND -> {

            }
            SharedViewModel.MODE_CHANGE_TRANSFERHARNESS -> {

            }

            SharedViewModel.MODE_ALL_POSITION -> {

            }
            SharedViewModel.MODE_ALL_CHANGESLING -> {

            }
            SharedViewModel.MODE_ALL_TRANSFERSLING -> {

            }
            SharedViewModel.MODE_ALL_TRANSFERBEDRIDDENSLING -> {

            }
            SharedViewModel.MODE_ALL_TRANSFERBEDRIDDENBOARD -> {

            }
            SharedViewModel.MODE_ALL_TRANSFERCHAIR -> {

            }
        }
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
        mBinding = FragmentControlBinding.inflate(inflater, container, false)
        return mBinding.root
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment ControlFragment.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            ControlFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }
}