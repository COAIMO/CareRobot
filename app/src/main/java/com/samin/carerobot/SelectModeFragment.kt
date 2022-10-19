package com.samin.carerobot

import android.content.Context
import android.graphics.drawable.Animatable2
import android.graphics.drawable.AnimatedVectorDrawable
import android.graphics.drawable.AnimationDrawable
import android.graphics.drawable.Drawable
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.activityViewModels
import com.samin.carerobot.Logics.SharedViewModel
import com.samin.carerobot.databinding.FragmentSelectModeBinding

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [SelectModeFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class SelectModeFragment : Fragment() {
    private var param1: String? = null
    private var param2: String? = null
    private lateinit var mBinding: FragmentSelectModeBinding
    private lateinit var onBackPressedCallback: OnBackPressedCallback
    private var activity: MainActivity? = null
    private lateinit var selectedModeFragment: SelectedModeFragment
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

    var isAnimate: Boolean = false
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        mBinding = FragmentSelectModeBinding.inflate(inflater, container, false)
        setFragment()
        setButtonClickEvent()
        (parentFragment as MainFragment).initTopNaviButton()
        val animation = mBinding.ivMic.drawable as AnimatedVectorDrawable
//        animation.start()
//
        animation.registerAnimationCallback(object : Animatable2.AnimationCallback() {
            override fun onAnimationEnd(drawable: Drawable?) {
                if (isAnimate) {
                    animation.start()
                }
            }
        })

        mBinding.ivMic.setOnClickListener {
            if (isAnimate) {
                isAnimate = false
                (mBinding.ivMic.drawable as AnimatedVectorDrawable).stop()
            }else{
                (mBinding.ivMic.drawable as AnimatedVectorDrawable).start()
                isAnimate = true
            }
        }
        return mBinding.root
    }

    private fun setFragment() {
        selectedModeFragment = SelectedModeFragment()
    }

    private fun setButtonClickEvent() {
        mBinding.btnCarryMode.setOnClickListener {
            onClick(mBinding.btnCarryMode)
        }
        mBinding.btnBehaviorMode.setOnClickListener {
            onClick(mBinding.btnBehaviorMode)
        }
        mBinding.btnChangeMode.setOnClickListener {
            onClick(mBinding.btnChangeMode)
        }
        mBinding.btnAllMode.setOnClickListener {
            onClick(mBinding.btnAllMode)
        }
    }

    private fun onClick(view: View) {
        when (view) {
            mBinding.btnCarryMode -> {
                sharedViewModel.viewState.value = SharedViewModel.MODE_CARRY
            }
            mBinding.btnBehaviorMode -> {
                sharedViewModel.viewState.value = SharedViewModel.MODE_BEHAVIOR
            }
            mBinding.btnChangeMode -> {
                sharedViewModel.viewState.value = SharedViewModel.MODE_CHANGE
            }
            mBinding.btnAllMode -> {
                sharedViewModel.viewState.value = SharedViewModel.MODE_ALL
            }
        }
        (parentFragment as MainFragment).childFragmentManager.beginTransaction().replace(
            R.id.mainFragment_container,
            (parentFragment as MainFragment).selectedModeFragment
        ).commit()

    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment SelectModeFragment.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            SelectModeFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }

}