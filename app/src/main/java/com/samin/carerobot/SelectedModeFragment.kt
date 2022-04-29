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
import com.samin.carerobot.databinding.FragmentSelectedModeBinding

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [SelectedModeFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class SelectedModeFragment : Fragment() {
    // TODO: Rename and change types of parameters
    private var param1: String? = null
    private var param2: String? = null
    private lateinit var onBackPressedCallback: OnBackPressedCallback
    private lateinit var mBinding: FragmentSelectedModeBinding
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
        mBinding = FragmentSelectedModeBinding.inflate(inflater, container, false)
        initView()
        setButtonClickEvent()
        return mBinding.root
    }

    private fun initSettingView() {
        mBinding.carryModeLayout.visibility = View.GONE
        mBinding.behaviorModeLayout.visibility = View.GONE
        mBinding.changeModeLayout.visibility = View.GONE
        mBinding.allModeLayout.visibility = View.GONE
    }

    private fun initView() {
        sharedViewModel.viewState.observe(viewLifecycleOwner) {
            initSettingView()
            when (it) {
                SharedViewModel.MODE_CARRY -> {
                    mBinding.carryModeLayout.visibility = View.VISIBLE
                }
                SharedViewModel.MODE_BEHAVIOR -> {
                    mBinding.behaviorModeLayout.visibility = View.VISIBLE
                }
                SharedViewModel.MODE_CHANGE -> {
                    mBinding.changeModeLayout.visibility = View.VISIBLE
                }
                SharedViewModel.MODE_ALL -> {
                    mBinding.allModeLayout.visibility = View.VISIBLE
                }
            }
        }
    }

    private fun setButtonClickEvent(){
        mBinding.btnHeavy.setOnClickListener {
            onClick(mBinding.btnHeavy)
        }
        mBinding.btnHeight.setOnClickListener {
            onClick(mBinding.btnHeight)
        }
        mBinding.btnStand.setOnClickListener {
            onClick(mBinding.btnStand)
        }
        mBinding.btnWalkHand.setOnClickListener {
            onClick(mBinding.btnWalkHand)
        }
        mBinding.btnWalkHug.setOnClickListener {
            onClick(mBinding.btnWalkHug)
        }
        mBinding.btnChangeHug.setOnClickListener {
            onClick(mBinding.btnChangeHug)
        }
        mBinding.btnTransferStand.setOnClickListener {
            onClick(mBinding.btnTransferStand)
        }
        mBinding.btnTransferHarness.setOnClickListener {
            onClick(mBinding.btnTransferHarness)
        }
        mBinding.btnPosition.setOnClickListener {
            onClick(mBinding.btnPosition)
        }
        mBinding.btnChangeSling.setOnClickListener {
            onClick(mBinding.btnChangeSling)
        }
        mBinding.btnTransferSling.setOnClickListener {
            onClick(mBinding.btnTransferSling)
        }
        mBinding.btnTransferBedriddenSling.setOnClickListener {
            onClick(mBinding.btnTransferBedriddenSling)
        }
        mBinding.btnTransferBedriddenBoard.setOnClickListener {
            onClick(mBinding.btnTransferBedriddenBoard)
        }
        mBinding.btnTransferChair.setOnClickListener {
            onClick(mBinding.btnTransferChair)
        }
    }

    private fun onClick(view:View){
        when(view){
            mBinding.btnHeavy->{
                sharedViewModel.viewState.value = SharedViewModel.MODE_CARRY_HEAVY
            }
            mBinding.btnHeight->{
                sharedViewModel.viewState.value = SharedViewModel.MODE_CARRY_HEAVY
            }
            mBinding.btnStand ->{
                sharedViewModel.viewState.value = SharedViewModel.MODE_BEHAVIOR_STAND
            }
            mBinding.btnWalkHand ->{
                sharedViewModel.viewState.value = SharedViewModel.MODE_BEHAVIOR_WALKHAND
            }
            mBinding.btnWalkHug ->{
                sharedViewModel.viewState.value = SharedViewModel.MODE_BEHAVIOR_WALKHUG
            }
            mBinding.btnChangeHug ->{
                sharedViewModel.viewState.value = SharedViewModel.MODE_CHANGE_CHANGEHUG
            }
            mBinding.btnTransferStand ->{
                sharedViewModel.viewState.value = SharedViewModel.MODE_CHANGE_TRANSFERSTAND
            }
            mBinding.btnTransferHarness->{
                sharedViewModel.viewState.value = SharedViewModel.MODE_CHANGE_TRANSFERHARNESS
            }
            mBinding.btnPosition->{
                sharedViewModel.viewState.value = SharedViewModel.MODE_ALL_POSITION
            }
            mBinding.btnChangeSling->{
                sharedViewModel.viewState.value = SharedViewModel.MODE_ALL_CHANGESLING
            }
            mBinding.btnTransferSling->{
                sharedViewModel.viewState.value = SharedViewModel.MODE_ALL_TRANSFERSLING
            }
            mBinding.btnTransferBedriddenSling->{
                sharedViewModel.viewState.value = SharedViewModel.MODE_ALL_TRANSFERBEDRIDDENSLING
            }
            mBinding.btnTransferBedriddenBoard->{
                sharedViewModel.viewState.value = SharedViewModel.MODE_ALL_TRANSFERBEDRIDDENBOARD
            }
            mBinding.btnTransferChair->{
                sharedViewModel.viewState.value = SharedViewModel.MODE_ALL_TRANSFERCHAIR
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
         * @return A new instance of fragment SelectedModeFragment.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            SelectedModeFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }


}