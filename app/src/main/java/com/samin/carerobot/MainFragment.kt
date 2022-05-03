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
import com.samin.carerobot.databinding.FragmentMainBinding

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [MainFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class MainFragment : Fragment() {
    private var param1: String? = null
    private var param2: String? = null
    private lateinit var mBinding: FragmentMainBinding
    private var activity: MainActivity? = null
    lateinit var selectModeFragment: SelectModeFragment
    lateinit var selectedModeFragment: SelectedModeFragment
    lateinit var controlFragment: ControlFragment
    private val sharedViewModel by activityViewModels<SharedViewModel>()
    private lateinit var onBackPressedCallback: OnBackPressedCallback

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
        mBinding = FragmentMainBinding.inflate(inflater, container, false)
        initView()
        setChlidFragment()
        setButtonClickEvent()

        return mBinding.root
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment MainFragment.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            MainFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }

    private fun initView() {
        sharedViewModel.viewState.observe(viewLifecycleOwner) {
            initTopNaviButton()
            setTopNavibar(it)
            setSubNavibarTitle(it)
        }
        mBinding.topNavi.btnUserInfo.text = activity?.sharedPreference!!.loadUserID()
    }

    private fun setSubNavibarTitle(viewState: Int){
        when (viewState) {
            SharedViewModel.MODE_CARRY -> {
                mBinding.subtopNavibar.tvModeTitle.setText(R.string.carry_mode_title)
                mBinding.subtopNavibar.tvModeTitleSub.text = ""
                mBinding.subtopNavibar.tvModeTitleContent.text = ""
            }
            SharedViewModel.MODE_CARRY_HEAVY -> {
                mBinding.subtopNavibar.tvModeTitle.setText(R.string.carry_mode_title)
                mBinding.subtopNavibar.ivForward.visibility = View.VISIBLE
                mBinding.subtopNavibar.tvModeTitleContent.setText(R.string.carry_mode_title_heavy)

            }
            SharedViewModel.MODE_CARRY_HEIGHT -> {
                mBinding.subtopNavibar.tvModeTitle.setText(R.string.carry_mode_title)
                mBinding.subtopNavibar.ivForward.visibility = View.VISIBLE
                mBinding.subtopNavibar.tvModeTitleContent.setText(R.string.carry_mode_title_height)
            }
            SharedViewModel.MODE_BEHAVIOR -> {
                mBinding.subtopNavibar.tvModeTitleContent.text = ""
                mBinding.subtopNavibar.tvModeTitle.setText(R.string.behavior_mode_title)
                mBinding.subtopNavibar.tvModeTitleSub.setText(R.string.behavior_mode_title_sub)
            }
            SharedViewModel.MODE_BEHAVIOR_STAND -> {
                mBinding.subtopNavibar.tvModeTitle.setText(R.string.behavior_mode_title)
                mBinding.subtopNavibar.tvModeTitleSub.setText(R.string.behavior_mode_title_sub)
                mBinding.subtopNavibar.ivForward.visibility = View.VISIBLE
                mBinding.subtopNavibar.tvModeTitleContent.setText(R.string.behavior_mode_title_stand)
            }
            SharedViewModel.MODE_BEHAVIOR_WALKHAND -> {
                mBinding.subtopNavibar.tvModeTitle.setText(R.string.behavior_mode_title)
                mBinding.subtopNavibar.tvModeTitleSub.setText(R.string.behavior_mode_title_sub)
                mBinding.subtopNavibar.ivForward.visibility = View.VISIBLE
                mBinding.subtopNavibar.tvModeTitleContent.setText(R.string.behavior_mode_title_walk_hand)
            }
            SharedViewModel.MODE_BEHAVIOR_WALKHUG -> {
                mBinding.subtopNavibar.tvModeTitle.setText(R.string.behavior_mode_title)
                mBinding.subtopNavibar.tvModeTitleSub.setText(R.string.behavior_mode_title_sub)
                mBinding.subtopNavibar.ivForward.visibility = View.VISIBLE
                mBinding.subtopNavibar.tvModeTitleContent.setText(R.string.behavior_mode_title_walk_waist)
            }
            SharedViewModel.MODE_CHANGE -> {
                mBinding.subtopNavibar.tvModeTitleContent.text = ""
                mBinding.subtopNavibar.tvModeTitle.setText(R.string.change_mode_title)
                mBinding.subtopNavibar.tvModeTitleSub.setText(R.string.change_mode_title_sub)
            }
            SharedViewModel.MODE_CHANGE_CHANGEHUG -> {
                mBinding.subtopNavibar.tvModeTitle.setText(R.string.change_mode_title)
                mBinding.subtopNavibar.tvModeTitleSub.setText(R.string.change_mode_title_sub)
                mBinding.subtopNavibar.ivForward.visibility = View.VISIBLE
                mBinding.subtopNavibar.tvModeTitleContent.setText(R.string.change_mode_title_change_hug)
            }
            SharedViewModel.MODE_CHANGE_TRANSFERSTAND -> {
                mBinding.subtopNavibar.tvModeTitle.setText(R.string.change_mode_title)
                mBinding.subtopNavibar.tvModeTitleSub.setText(R.string.change_mode_title_sub)
                mBinding.subtopNavibar.ivForward.visibility = View.VISIBLE
                mBinding.subtopNavibar.tvModeTitleContent.setText(R.string.change_mode_title_transfer_stand)
            }
            SharedViewModel.MODE_CHANGE_TRANSFERHARNESS -> {
                mBinding.subtopNavibar.tvModeTitle.setText(R.string.change_mode_title)
                mBinding.subtopNavibar.tvModeTitleSub.setText(R.string.change_mode_title_sub)
                mBinding.subtopNavibar.ivForward.visibility = View.VISIBLE
                mBinding.subtopNavibar.tvModeTitleContent.setText(R.string.change_mode_title_transfer_harness)
            }
            SharedViewModel.MODE_ALL -> {
                mBinding.subtopNavibar.tvModeTitleContent.text = ""
                mBinding.subtopNavibar.tvModeTitle.setText(R.string.all_mode_title)
                mBinding.subtopNavibar.tvModeTitleSub.setText(R.string.all_mode_title_sub)
            }
            SharedViewModel.MODE_ALL_POSITION -> {
                mBinding.subtopNavibar.tvModeTitle.setText(R.string.all_mode_title)
                mBinding.subtopNavibar.tvModeTitleSub.setText(R.string.all_mode_title_sub)
                mBinding.subtopNavibar.ivForward.visibility = View.VISIBLE
                mBinding.subtopNavibar.tvModeTitleContent.setText(R.string.all_mode_title_position)
            }
            SharedViewModel.MODE_ALL_CHANGESLING -> {
                mBinding.subtopNavibar.tvModeTitle.setText(R.string.all_mode_title)
                mBinding.subtopNavibar.tvModeTitleSub.setText(R.string.all_mode_title_sub)
                mBinding.subtopNavibar.ivForward.visibility = View.VISIBLE
                mBinding.subtopNavibar.tvModeTitleContent.setText(R.string.all_mode_title_change_sling)
            }
            SharedViewModel.MODE_ALL_TRANSFERSLING -> {
                mBinding.subtopNavibar.tvModeTitle.setText(R.string.all_mode_title)
                mBinding.subtopNavibar.tvModeTitleSub.setText(R.string.all_mode_title_sub)
                mBinding.subtopNavibar.ivForward.visibility = View.VISIBLE
                mBinding.subtopNavibar.tvModeTitleContent.setText(R.string.all_mode_title_transfer_sling)
            }
            SharedViewModel.MODE_ALL_TRANSFERBEDRIDDENSLING -> {
                mBinding.subtopNavibar.tvModeTitle.setText(R.string.all_mode_title)
                mBinding.subtopNavibar.tvModeTitleSub.setText(R.string.all_mode_title_sub)
                mBinding.subtopNavibar.ivForward.visibility = View.VISIBLE
                mBinding.subtopNavibar.tvModeTitleContent.setText(R.string.all_mode_title_transfer_bedridden_sling)
            }
            SharedViewModel.MODE_ALL_TRANSFERBEDRIDDENBOARD -> {
                mBinding.subtopNavibar.tvModeTitle.setText(R.string.all_mode_title)
                mBinding.subtopNavibar.tvModeTitleSub.setText(R.string.all_mode_title_sub)
                mBinding.subtopNavibar.ivForward.visibility = View.VISIBLE
                mBinding.subtopNavibar.tvModeTitleContent.setText(R.string.all_mode_title_transfer_bedridden_board)
            }
            SharedViewModel.MODE_ALL_TRANSFERCHAIR -> {
                mBinding.subtopNavibar.tvModeTitle.setText(R.string.all_mode_title)
                mBinding.subtopNavibar.tvModeTitleSub.setText(R.string.all_mode_title_sub)
                mBinding.subtopNavibar.ivForward.visibility = View.VISIBLE
                mBinding.subtopNavibar.tvModeTitleContent.setText(R.string.all_mode_title_transfer_chair)
            }
        }

    }

    private fun setTopNavibar(viewState: Int) {
        mBinding.subtopNavibar.ivForward.visibility = View.GONE
        when (viewState) {
            SharedViewModel.MODE_CARRY -> {
                mBinding.topNavi.btnModeCarry.setBackgroundResource(R.drawable.mode1_selected_button)
                mBinding.subtopLayout.visibility = View.VISIBLE
                childFragmentManager.beginTransaction().replace(
                    R.id.mainFragment_container,
                    selectedModeFragment
                ).commit()
            }
            SharedViewModel.MODE_CARRY_HEAVY -> {
                mBinding.topNavi.btnModeCarry.setBackgroundResource(R.drawable.mode1_selected_button)
                mBinding.subtopLayout.visibility = View.VISIBLE
                childFragmentManager.beginTransaction().replace(
                    R.id.mainFragment_container,
                    controlFragment
                ).commit()
            }
            SharedViewModel.MODE_CARRY_HEIGHT -> {
                mBinding.topNavi.btnModeCarry.setBackgroundResource(R.drawable.mode1_selected_button)
                mBinding.subtopLayout.visibility = View.VISIBLE
                childFragmentManager.beginTransaction().replace(
                    R.id.mainFragment_container,
                    controlFragment
                ).commit()
            }

            SharedViewModel.MODE_BEHAVIOR -> {
                mBinding.topNavi.btnModeBehavior.setBackgroundResource(R.drawable.mode2_selected_button)
                mBinding.subtopLayout.visibility = View.VISIBLE
                childFragmentManager.beginTransaction().replace(
                    R.id.mainFragment_container,
                    selectedModeFragment
                ).commit()
            }
            SharedViewModel.MODE_BEHAVIOR_STAND -> {
                mBinding.topNavi.btnModeBehavior.setBackgroundResource(R.drawable.mode2_selected_button)
                mBinding.subtopLayout.visibility = View.VISIBLE
                childFragmentManager.beginTransaction().replace(
                    R.id.mainFragment_container,
                    controlFragment
                ).commit()
            }
            SharedViewModel.MODE_BEHAVIOR_WALKHAND -> {
                mBinding.topNavi.btnModeBehavior.setBackgroundResource(R.drawable.mode2_selected_button)
                mBinding.subtopLayout.visibility = View.VISIBLE
                childFragmentManager.beginTransaction().replace(
                    R.id.mainFragment_container,
                    controlFragment
                ).commit()
            }
            SharedViewModel.MODE_BEHAVIOR_WALKHUG -> {
                mBinding.topNavi.btnModeBehavior.setBackgroundResource(R.drawable.mode2_selected_button)
                mBinding.subtopLayout.visibility = View.VISIBLE
                childFragmentManager.beginTransaction().replace(
                    R.id.mainFragment_container,
                    controlFragment
                ).commit()
            }

            SharedViewModel.MODE_CHANGE -> {
                mBinding.topNavi.btnModeChange.setBackgroundResource(R.drawable.mode3_selected_button)
                mBinding.subtopLayout.visibility = View.VISIBLE
                childFragmentManager.beginTransaction().replace(
                    R.id.mainFragment_container,
                    selectedModeFragment
                ).commit()
            }
            SharedViewModel.MODE_CHANGE_CHANGEHUG -> {
                mBinding.topNavi.btnModeChange.setBackgroundResource(R.drawable.mode3_selected_button)
                mBinding.subtopLayout.visibility = View.VISIBLE
                childFragmentManager.beginTransaction().replace(
                    R.id.mainFragment_container,
                    controlFragment
                ).commit()
            }
            SharedViewModel.MODE_CHANGE_TRANSFERSTAND -> {
                mBinding.topNavi.btnModeChange.setBackgroundResource(R.drawable.mode3_selected_button)
                mBinding.subtopLayout.visibility = View.VISIBLE
                childFragmentManager.beginTransaction().replace(
                    R.id.mainFragment_container,
                    controlFragment
                ).commit()
            }
            SharedViewModel.MODE_CHANGE_TRANSFERHARNESS -> {
                mBinding.topNavi.btnModeChange.setBackgroundResource(R.drawable.mode3_selected_button)
                mBinding.subtopLayout.visibility = View.VISIBLE
                childFragmentManager.beginTransaction().replace(
                    R.id.mainFragment_container,
                    controlFragment
                ).commit()
            }

            SharedViewModel.MODE_ALL -> {
                mBinding.topNavi.btnModeAll.setBackgroundResource(R.drawable.mode4_selected_button)
                mBinding.subtopLayout.visibility = View.VISIBLE
                childFragmentManager.beginTransaction().replace(
                    R.id.mainFragment_container,
                    selectedModeFragment
                ).commit()
            }
            SharedViewModel.MODE_ALL_POSITION -> {
                mBinding.topNavi.btnModeAll.setBackgroundResource(R.drawable.mode4_selected_button)
                mBinding.subtopLayout.visibility = View.VISIBLE
                childFragmentManager.beginTransaction().replace(
                    R.id.mainFragment_container,
                    controlFragment
                ).commit()
            }
            SharedViewModel.MODE_ALL_CHANGESLING -> {
                mBinding.topNavi.btnModeAll.setBackgroundResource(R.drawable.mode4_selected_button)
                mBinding.subtopLayout.visibility = View.VISIBLE
                childFragmentManager.beginTransaction().replace(
                    R.id.mainFragment_container,
                    controlFragment
                ).commit()
            }
            SharedViewModel.MODE_ALL_TRANSFERSLING -> {
                mBinding.topNavi.btnModeAll.setBackgroundResource(R.drawable.mode4_selected_button)
                mBinding.subtopLayout.visibility = View.VISIBLE
                childFragmentManager.beginTransaction().replace(
                    R.id.mainFragment_container,
                    controlFragment
                ).commit()
            }
            SharedViewModel.MODE_ALL_TRANSFERBEDRIDDENSLING -> {
                mBinding.topNavi.btnModeAll.setBackgroundResource(R.drawable.mode4_selected_button)
                mBinding.subtopLayout.visibility = View.VISIBLE
                childFragmentManager.beginTransaction().replace(
                    R.id.mainFragment_container,
                    controlFragment
                ).commit()
            }
            SharedViewModel.MODE_ALL_TRANSFERBEDRIDDENBOARD -> {
                mBinding.topNavi.btnModeAll.setBackgroundResource(R.drawable.mode4_selected_button)
                mBinding.subtopLayout.visibility = View.VISIBLE
                childFragmentManager.beginTransaction().replace(
                    R.id.mainFragment_container,
                    controlFragment
                ).commit()
            }
            SharedViewModel.MODE_ALL_TRANSFERCHAIR -> {
                mBinding.topNavi.btnModeAll.setBackgroundResource(R.drawable.mode4_selected_button)
                mBinding.subtopLayout.visibility = View.VISIBLE
                childFragmentManager.beginTransaction().replace(
                    R.id.mainFragment_container,
                    controlFragment
                ).commit()
            }
        }

    }

    private fun setButtonClickEvent() {
        mBinding.topNavi.btnModeCarry.setOnClickListener {
            onClick(mBinding.topNavi.btnModeCarry)
        }
        mBinding.topNavi.btnModeBehavior.setOnClickListener {
            onClick(mBinding.topNavi.btnModeBehavior)
        }
        mBinding.topNavi.btnModeChange.setOnClickListener {
            onClick(mBinding.topNavi.btnModeChange)
        }
        mBinding.topNavi.btnModeAll.setOnClickListener {
            onClick(mBinding.topNavi.btnModeAll)
        }
        mBinding.topNavi.btnLogout.setOnClickListener {
            onClick(mBinding.topNavi.btnLogout)
        }
        mBinding.subtopNavibar.btnBack.setOnClickListener {
            onClick(mBinding.subtopNavibar.btnBack)
        }
        mBinding.subtopNavibar.btnHome.setOnClickListener {
            onClick(mBinding.subtopNavibar.btnHome)
        }
    }

    private fun onClick(view: View) {
        initTopNaviButton()
        when (view) {
            mBinding.topNavi.btnModeCarry -> {
                sharedViewModel.viewState.value = SharedViewModel.MODE_CARRY
            }
            mBinding.topNavi.btnModeBehavior -> {
                sharedViewModel.viewState.value = SharedViewModel.MODE_BEHAVIOR
            }
            mBinding.topNavi.btnModeChange -> {
                sharedViewModel.viewState.value = SharedViewModel.MODE_CHANGE
            }
            mBinding.topNavi.btnModeAll -> {
                sharedViewModel.viewState.value = SharedViewModel.MODE_ALL
            }
            mBinding.topNavi.btnLogout -> {
                activity?.sharedPreference!!.loginState(false)
                activity?.onFragmentChange(SharedViewModel.LOGINFRAGMENT)
            }
            mBinding.subtopNavibar.btnBack -> {
                subNavibarBackButtonEvent()
            }
            mBinding.subtopNavibar.btnHome -> {
                childFragmentManager.beginTransaction().replace(
                    R.id.mainFragment_container,
                    selectModeFragment
                ).commit()
            }
        }
    }

    private fun subNavibarBackButtonEvent(){
        when(sharedViewModel.viewState.value){
            SharedViewModel.MODE_CARRY ->{
                sharedViewModel.viewState.value = SharedViewModel.MODE_CARRY
                childFragmentManager.beginTransaction().replace(
                    R.id.mainFragment_container,
                    selectModeFragment
                ).commit()
            }
            SharedViewModel.MODE_CARRY_HEAVY and SharedViewModel.MODE_CARRY_HEIGHT ->{
                sharedViewModel.viewState.value = SharedViewModel.MODE_CARRY
                childFragmentManager.beginTransaction().replace(
                    R.id.mainFragment_container,
                    selectedModeFragment
                ).commit()
            }
            SharedViewModel.MODE_BEHAVIOR ->{
                sharedViewModel.viewState.value = SharedViewModel.MODE_BEHAVIOR
                childFragmentManager.beginTransaction().replace(
                    R.id.mainFragment_container,
                    selectModeFragment
                ).commit()
            }
            SharedViewModel.MODE_BEHAVIOR_STAND ->{
                sharedViewModel.viewState.value = SharedViewModel.MODE_BEHAVIOR
                childFragmentManager.beginTransaction().replace(
                    R.id.mainFragment_container,
                    selectedModeFragment
                ).commit()
            }
            SharedViewModel.MODE_BEHAVIOR_WALKHAND ->{
                sharedViewModel.viewState.value = SharedViewModel.MODE_BEHAVIOR
                childFragmentManager.beginTransaction().replace(
                    R.id.mainFragment_container,
                    selectedModeFragment
                ).commit()
            }
            SharedViewModel.MODE_BEHAVIOR_WALKHUG ->{
                sharedViewModel.viewState.value = SharedViewModel.MODE_BEHAVIOR
                childFragmentManager.beginTransaction().replace(
                    R.id.mainFragment_container,
                    selectedModeFragment
                ).commit()
            }
            SharedViewModel.MODE_CHANGE ->{
                sharedViewModel.viewState.value = SharedViewModel.MODE_CHANGE
                childFragmentManager.beginTransaction().replace(
                    R.id.mainFragment_container,
                    selectModeFragment
                ).commit()
            }
            SharedViewModel.MODE_CHANGE_CHANGEHUG ->{
                sharedViewModel.viewState.value = SharedViewModel.MODE_CHANGE
                childFragmentManager.beginTransaction().replace(
                    R.id.mainFragment_container,
                    selectedModeFragment
                ).commit()
            }
            SharedViewModel.MODE_CHANGE_TRANSFERSTAND ->{
                sharedViewModel.viewState.value = SharedViewModel.MODE_CHANGE
                childFragmentManager.beginTransaction().replace(
                    R.id.mainFragment_container,
                    selectedModeFragment
                ).commit()
            }
            SharedViewModel.MODE_CHANGE_TRANSFERHARNESS ->{
                sharedViewModel.viewState.value = SharedViewModel.MODE_CHANGE
                childFragmentManager.beginTransaction().replace(
                    R.id.mainFragment_container,
                    selectedModeFragment
                ).commit()
            }
            SharedViewModel.MODE_ALL ->{
                sharedViewModel.viewState.value = SharedViewModel.MODE_ALL
                childFragmentManager.beginTransaction().replace(
                    R.id.mainFragment_container,
                    selectModeFragment
                ).commit()
            }
            SharedViewModel.MODE_ALL_POSITION ->{
                sharedViewModel.viewState.value = SharedViewModel.MODE_ALL
                childFragmentManager.beginTransaction().replace(
                    R.id.mainFragment_container,
                    selectedModeFragment
                ).commit()
            }
            SharedViewModel.MODE_ALL_CHANGESLING ->{
                sharedViewModel.viewState.value = SharedViewModel.MODE_ALL
                childFragmentManager.beginTransaction().replace(
                    R.id.mainFragment_container,
                    selectedModeFragment
                ).commit()
            }
            SharedViewModel.MODE_ALL_TRANSFERSLING ->{
                sharedViewModel.viewState.value = SharedViewModel.MODE_ALL
                childFragmentManager.beginTransaction().replace(
                    R.id.mainFragment_container,
                    selectedModeFragment
                ).commit()
            }
            SharedViewModel.MODE_ALL_TRANSFERBEDRIDDENSLING ->{
                sharedViewModel.viewState.value = SharedViewModel.MODE_ALL
                childFragmentManager.beginTransaction().replace(
                    R.id.mainFragment_container,
                    selectedModeFragment
                ).commit()
            }
            SharedViewModel.MODE_ALL_TRANSFERBEDRIDDENBOARD ->{
                sharedViewModel.viewState.value = SharedViewModel.MODE_ALL
                childFragmentManager.beginTransaction().replace(
                    R.id.mainFragment_container,
                    selectedModeFragment
                ).commit()
            }
            SharedViewModel.MODE_ALL_TRANSFERCHAIR ->{
                sharedViewModel.viewState.value = SharedViewModel.MODE_ALL
                childFragmentManager.beginTransaction().replace(
                    R.id.mainFragment_container,
                    selectedModeFragment
                ).commit()
            }
        }
    }

    private fun setChlidFragment() {
        selectModeFragment = SelectModeFragment()
        childFragmentManager.beginTransaction()
            .replace(R.id.mainFragment_container, selectModeFragment).commit()
        selectedModeFragment = SelectedModeFragment()
        controlFragment = ControlFragment()
    }

    fun initTopNaviButton() {
        mBinding.topNavi.btnModeCarry.setBackgroundResource(R.drawable.mode_nonselected_button)
        mBinding.topNavi.btnModeBehavior.setBackgroundResource(R.drawable.mode_nonselected_button)
        mBinding.topNavi.btnModeChange.setBackgroundResource(R.drawable.mode_nonselected_button)
        mBinding.topNavi.btnModeAll.setBackgroundResource(R.drawable.mode_nonselected_button)
        mBinding.subtopLayout.visibility = View.GONE
    }
}