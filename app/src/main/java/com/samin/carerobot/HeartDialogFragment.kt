package com.samin.carerobot

import android.content.Context
import android.content.DialogInterface
import android.graphics.Color
import android.graphics.drawable.*
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import com.samin.carerobot.Logics.HexDump
import com.samin.carerobot.Logics.SharedViewModel
import com.samin.carerobot.Nuri.PC_Protocol
import com.samin.carerobot.Nuri.SpeechMode
import com.samin.carerobot.databinding.FragmentHeartDialogBinding

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [HeartDialogFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class HeartDialogFragment : DialogFragment() {
    // TODO: Rename and change types of parameters
    private var param1: String? = null
    private var param2: String? = null
    private lateinit var mBinding: FragmentHeartDialogBinding
    private val sharedViewModel by activityViewModels<SharedViewModel>()
    private var activity: MainActivity? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)
        activity = getActivity() as MainActivity
    }

    override fun onDetach() {
        super.onDetach()
        activity = null
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            param1 = it.getString(ARG_PARAM1)
            param2 = it.getString(ARG_PARAM2)
        }
    }

    override fun onResume() {
        this.dialog?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        super.onResume()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        mBinding = FragmentHeartDialogBinding.inflate(inflater, container, false)
//        mBinding.ivHeart.setBackgroundResource(R.drawable.heart_animation)
        val animation = mBinding.ivHeart.drawable as AnimationDrawable
        animation.start()

        mBinding.ivHeart.setOnClickListener {
            this@HeartDialogFragment.dismiss()
        }
        return mBinding.root
    }
    val pcProtocol = PC_Protocol()

    override fun onDismiss(dialog: DialogInterface) {
        pcProtocol.setSpeech(SpeechMode.TOUCH_WAITTING_SCREEN.byte)
        val data = pcProtocol.Data!!.clone()
        sharedViewModel.pcsendProtocol.value = HexDump.toHexString(data)
        activity?.sendProtocolToPC(data)
        super.onDismiss(dialog)
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment HeartDialogFragment.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            HeartDialogFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }
}