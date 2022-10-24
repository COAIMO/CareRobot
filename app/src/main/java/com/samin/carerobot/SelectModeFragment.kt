package com.samin.carerobot

import android.content.Context
import android.graphics.drawable.Animatable2
import android.graphics.drawable.AnimatedVectorDrawable
import android.graphics.drawable.AnimationDrawable
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Message
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.MutableLiveData
import com.samin.carerobot.Logics.SharedViewModel
import com.samin.carerobot.Nuri.PC_Protocol
import com.samin.carerobot.Nuri.SpeechMode
import com.samin.carerobot.databinding.FragmentSelectModeBinding
import java.util.*
import kotlin.concurrent.timer

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
    private val sendParser = PC_Protocol()
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
        uiTimer?.cancel()
        micAutoOffTimer?.cancel()
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

    val pcProtocol = PC_Protocol()
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
        ment.value = getString(R.string.main_ment)
        activity?.showHeartAnimation()

        animation.registerAnimationCallback(object : Animatable2.AnimationCallback() {
            override fun onAnimationEnd(drawable: Drawable?) {
                if (isAnimate) {
                    animation.start()
                }
            }
        })

        mBinding.ivMic.setOnClickListener {
            if (isAnimate) {
                setUITimer()
                micAutoOffTimer?.cancel()
                ment.value = getString(R.string.mic_off_ment)
                pcProtocol.setSpeech(SpeechMode.TOUCH_MIC_OFF.byte)
                val data = pcProtocol.Data!!.clone()
                activity?.sendProtocolToPC(data)
                isAnimate = false
                sharedViewModel.usingMic.value = false
                (mBinding.ivMic.drawable as AnimatedVectorDrawable).stop()
            } else {
                uiTimer?.purge()
                uiTimer?.cancel()
                ment.value = getString(R.string.mic_on_ment)
                pcProtocol.setSpeech(SpeechMode.TOUCH_MIC_ON.byte)
                val data = pcProtocol.Data!!.clone()
                activity?.sendProtocolToPC(data)
                isAnimate = true
                sharedViewModel.usingMic.value = true
                (mBinding.ivMic.drawable as AnimatedVectorDrawable).start()
            }
        }

        sharedViewModel.usingMic.observe(viewLifecycleOwner) {
            if (it)
                micAutoOffTimer = timer(period = 20000, initialDelay = 15000) {
                    pcProtocol.setSpeech(SpeechMode.TOUCH_MIC_OFF.byte)
                    val data = pcProtocol.Data!!.clone()
                    activity?.sendProtocolToPC(data)
                    isAnimate = false
                    setUITimer()
                    sharedViewModel.usingMic.postValue(false)
                    (mBinding.ivMic.drawable as AnimatedVectorDrawable).stop()
                    this.cancel()
                }
        }

        ment.observe(viewLifecycleOwner) {
            mBinding.tvMainment.text = it
        }
        setUITimer()
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
        mBinding.ivHeart1.setOnClickListener {
            pcProtocol.setSpeech(SpeechMode.TOUCH_BUTTON_1.byte)
            val data = pcProtocol.Data!!.clone()
            activity?.sendProtocolToPC(data)
        }
        mBinding.ivHeart2.setOnClickListener {
            pcProtocol.setSpeech(SpeechMode.TOUCH_BUTTON_2.byte)
            val data = pcProtocol.Data!!.clone()
            activity?.sendProtocolToPC(data)
        }
    }

    private fun onClick(view: View) {
        when (view) {
            mBinding.btnCarryMode -> {
                sharedViewModel.viewState.value = SharedViewModel.MODE_CARRY
                pcProtocol.setSpeech(SpeechMode.TOUCH_CARRYMODE.byte)
                val data = pcProtocol.Data!!.clone()
                activity?.sendProtocolToPC(data)
            }
            mBinding.btnBehaviorMode -> {
                sharedViewModel.viewState.value = SharedViewModel.MODE_BEHAVIOR
                pcProtocol.setSpeech(SpeechMode.TOUCH_BEHAVIORMODE.byte)
                val data = pcProtocol.Data!!.clone()
                activity?.sendProtocolToPC(data)
            }
            mBinding.btnChangeMode -> {
                sharedViewModel.viewState.value = SharedViewModel.MODE_CHANGE
                pcProtocol.setSpeech(SpeechMode.TOUCH_CHANGEMODE.byte)
                val data = pcProtocol.Data!!.clone()
                activity?.sendProtocolToPC(data)
            }
            mBinding.btnAllMode -> {
                sharedViewModel.viewState.value = SharedViewModel.MODE_ALL
                pcProtocol.setSpeech(SpeechMode.TOUCH_ALLMODE.byte)
                val data = pcProtocol.Data!!.clone()
                activity?.sendProtocolToPC(data)
            }
        }
        (parentFragment as MainFragment).childFragmentManager.beginTransaction().replace(
            R.id.mainFragment_container,
            (parentFragment as MainFragment).selectedModeFragment
        ).commit()

    }

    var uiTimer: Timer? = null
    var micAutoOffTimer: Timer? = null
    var mentIndex = 0

    private fun setUITimer() {
        uiTimer = timer(period = 4000, initialDelay = 1000) {
            mentIndex++
            if (mentIndex > 1) mentIndex = 0
            when (mentIndex) {
//                0 -> mBinding.tvMainment.text = getString(R.string.main_ment)
//                1 -> mBinding.tvMainment.text = getString(R.string.mic_off_ment)
                0 -> {
                    val msg = handler.obtainMessage().apply {
                        what = 0
                    }
                    handler.sendMessage(msg)
                }
                1 -> {
                    val msg = handler.obtainMessage().apply {
                        what = 1
                    }
                    handler.sendMessage(msg)
                }
            }
        }
    }

    val ment = MutableLiveData<String>()
    val handler = object : Handler(Looper.getMainLooper()) {
        override fun handleMessage(msg: Message) {
            try {
                when (msg.what) {
                    0 -> {
                        ment.value = getString(R.string.main_ment)
                    }
                    1 -> {
                        ment.value = getString(R.string.mic_off_ment)
                    }
                }

            } catch (e: Exception) {
                e.printStackTrace()
            }
            super.handleMessage(msg)
        }
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