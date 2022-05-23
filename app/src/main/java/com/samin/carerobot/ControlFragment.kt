package com.samin.carerobot

import android.content.Context
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.activityViewModels
import com.jeongmin.nurimotortester.Nuri.Direction
import com.jeongmin.nurimotortester.NurirobotMC
import com.samin.carerobot.Logics.CareRobotMC
import com.samin.carerobot.Logics.ControlMode
import com.samin.carerobot.Logics.SharedViewModel
import com.samin.carerobot.databinding.FragmentControlBinding

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
    var selectedShoulderMode = ControlMode.Both.byte
    var selectedElbowMode = ControlMode.Both.byte

    override fun onAttach(context: Context) {
        super.onAttach(context)
        activity = getActivity() as MainActivity
        onBackPressedCallback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                onBackPressedEvent()
            }
        }
        requireActivity().onBackPressedDispatcher.addCallback(this, onBackPressedCallback)
    }

    private fun onBackPressedEvent() {
        when (sharedViewModel.viewState.value) {
            SharedViewModel.MODE_CARRY_HEAVY and SharedViewModel.MODE_CARRY_HEIGHT -> {
                sharedViewModel.viewState.value = SharedViewModel.MODE_CARRY
                parentFragmentManager.beginTransaction().replace(
                    R.id.mainFragment_container,
                    (parentFragment as MainFragment).selectedModeFragment
                ).commit()
            }
            SharedViewModel.MODE_BEHAVIOR_STAND -> {
                sharedViewModel.viewState.value = SharedViewModel.MODE_BEHAVIOR
                parentFragmentManager.beginTransaction().replace(
                    R.id.mainFragment_container,
                    (parentFragment as MainFragment).selectedModeFragment
                ).commit()
            }
            SharedViewModel.MODE_BEHAVIOR_WALKHAND -> {
                sharedViewModel.viewState.value = SharedViewModel.MODE_BEHAVIOR
                parentFragmentManager.beginTransaction().replace(
                    R.id.mainFragment_container,
                    (parentFragment as MainFragment).selectedModeFragment
                ).commit()
            }
            SharedViewModel.MODE_BEHAVIOR_WALKHUG -> {
                sharedViewModel.viewState.value = SharedViewModel.MODE_BEHAVIOR
                parentFragmentManager.beginTransaction().replace(
                    R.id.mainFragment_container,
                    (parentFragment as MainFragment).selectedModeFragment
                ).commit()
            }
            SharedViewModel.MODE_CHANGE_CHANGEHUG -> {
                sharedViewModel.viewState.value = SharedViewModel.MODE_CHANGE
                parentFragmentManager.beginTransaction().replace(
                    R.id.mainFragment_container,
                    (parentFragment as MainFragment).selectedModeFragment
                ).commit()
            }
            SharedViewModel.MODE_CHANGE_TRANSFERSTAND -> {
                sharedViewModel.viewState.value = SharedViewModel.MODE_CHANGE
                parentFragmentManager.beginTransaction().replace(
                    R.id.mainFragment_container,
                    (parentFragment as MainFragment).selectedModeFragment
                ).commit()
            }
            SharedViewModel.MODE_CHANGE_TRANSFERHARNESS -> {
                sharedViewModel.viewState.value = SharedViewModel.MODE_CHANGE
                parentFragmentManager.beginTransaction().replace(
                    R.id.mainFragment_container,
                    (parentFragment as MainFragment).selectedModeFragment
                ).commit()
            }
            SharedViewModel.MODE_ALL_POSITION -> {
                sharedViewModel.viewState.value = SharedViewModel.MODE_ALL
                parentFragmentManager.beginTransaction().replace(
                    R.id.mainFragment_container,
                    (parentFragment as MainFragment).selectedModeFragment
                ).commit()
            }
            SharedViewModel.MODE_ALL_CHANGESLING -> {
                sharedViewModel.viewState.value = SharedViewModel.MODE_ALL
                parentFragmentManager.beginTransaction().replace(
                    R.id.mainFragment_container,
                    (parentFragment as MainFragment).selectedModeFragment
                ).commit()
            }
            SharedViewModel.MODE_ALL_TRANSFERSLING -> {
                sharedViewModel.viewState.value = SharedViewModel.MODE_ALL
                parentFragmentManager.beginTransaction().replace(
                    R.id.mainFragment_container,
                    (parentFragment as MainFragment).selectedModeFragment
                ).commit()
            }
            SharedViewModel.MODE_ALL_TRANSFERBEDRIDDENSLING -> {
                sharedViewModel.viewState.value = SharedViewModel.MODE_ALL
                parentFragmentManager.beginTransaction().replace(
                    R.id.mainFragment_container,
                    (parentFragment as MainFragment).selectedModeFragment
                ).commit()
            }
            SharedViewModel.MODE_ALL_TRANSFERBEDRIDDENBOARD -> {
                sharedViewModel.viewState.value = SharedViewModel.MODE_ALL
                parentFragmentManager.beginTransaction().replace(
                    R.id.mainFragment_container,
                    (parentFragment as MainFragment).selectedModeFragment
                ).commit()
            }
            SharedViewModel.MODE_ALL_TRANSFERCHAIR -> {
                sharedViewModel.viewState.value = SharedViewModel.MODE_ALL
                parentFragmentManager.beginTransaction().replace(
                    R.id.mainFragment_container,
                    (parentFragment as MainFragment).selectedModeFragment
                ).commit()
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
        setButtonClickEvent()
        initView()
        return mBinding.root
    }

    private fun initView() {
        setRadioButton()
    }

    private fun setRadioButton() {
        when (selectedShoulderMode) {
            ControlMode.Left.byte -> {
                mBinding.btnSelectedLeftShoulder.isChecked = true
            }
            ControlMode.Right.byte -> {
                mBinding.btnSelectedRightShoulder.isChecked = true
            }
            ControlMode.Both.byte -> {
                mBinding.btnSelectedBothShoulder.isChecked = true
            }
        }
        when (selectedElbowMode) {
            ControlMode.Left.byte -> {
                mBinding.btnSelectedLeftElbow.isChecked = true
            }
            ControlMode.Right.byte -> {
                mBinding.btnSelectedRightElbow.isChecked = true
            }
            ControlMode.Both.byte -> {
                mBinding.btnSelectedBothElbow.isChecked = true
            }
        }
    }

    private fun setButtonClickEvent() {
        mBinding.btnGoBack.setOnClickListener {
            onClick(mBinding.btnGoBack)
        }
        mBinding.btnGoForward.setOnClickListener {
            onClick(mBinding.btnGoForward)
        }
        mBinding.btnTurnLeft.setOnClickListener {
            onClick(mBinding.btnTurnLeft)
        }
        mBinding.btnTurnRight.setOnClickListener {
            onClick(mBinding.btnTurnRight)
        }
        mBinding.btnStop1.setOnClickListener {
            onClick(mBinding.btnStop1)
        }
        mBinding.shoulderRadioGroup.setOnCheckedChangeListener { group, checkedId ->
            when (checkedId) {
                R.id.btn_selected_LeftShoulder -> {
                    selectedShoulderMode = ControlMode.Left.byte
                }
                R.id.btn_selected_RightShoulder -> {
                    selectedShoulderMode = ControlMode.Right.byte
                }
                R.id.btn_selected_BothShoulder -> {
                    selectedShoulderMode = ControlMode.Both.byte
                }
            }
        }
        mBinding.btnRotateLeft.setOnClickListener {
            onClick(mBinding.btnRotateLeft)
        }
        mBinding.btnRotateRight.setOnClickListener {
            onClick(mBinding.btnRotateRight)
        }
        mBinding.btnStop2.setOnClickListener {
            onClick(mBinding.btnStop2)
        }
        mBinding.btnWaistUp.setOnClickListener {
            onClick(mBinding.btnWaistUp)
        }
        mBinding.btnWaistDown.setOnClickListener {
            onClick(mBinding.btnWaistDown)
        }
        mBinding.btnStop3.setOnClickListener {
            onClick(mBinding.btnStop3)
        }
        mBinding.elbowRadioGroup.setOnCheckedChangeListener { group, checkedId ->
            when (checkedId) {
                R.id.btn_selected_LeftElbow -> {
                    selectedElbowMode = ControlMode.Left.byte
                }
                R.id.btn_selected_RightElbow -> {
                    selectedElbowMode = ControlMode.Right.byte
                }
                R.id.btn_selected_BothElbow -> {
                    selectedElbowMode = ControlMode.Both.byte
                }
            }
        }
        mBinding.btnElbowUp.setOnClickListener {
            onClick(mBinding.btnElbowUp)
        }
        mBinding.btnElbowDown.setOnClickListener {
            onClick(mBinding.btnElbowDown)
        }
        mBinding.btnStop4.setOnClickListener {
            onClick(mBinding.btnStop4)
        }
    }

    private fun onClick(view: View) {
        val nuriMC = NurirobotMC()
        when (view) {
            mBinding.btnGoBack -> {
                val sedate = ByteArray(20)
                nuriMC.ControlAcceleratedSpeed(
                    CareRobotMC.Left_Wheel.byte,
                    Direction.CCW.direction,
                    5f,
                    0.1f
                )
                nuriMC.Data!!.copyInto(sedate, 0, 0, nuriMC.Data!!.size)
                nuriMC.ControlAcceleratedSpeed(
                    CareRobotMC.Left_Wheel.byte,
                    Direction.CW.direction,
                    5f,
                    0.1f
                )
                nuriMC.Data!!.copyInto(sedate, 10, 0, nuriMC.Data!!.size)
                activity?.serialService?.sendData(sedate)
            }
            mBinding.btnGoForward -> {
                val sedate = ByteArray(20)
                nuriMC.ControlAcceleratedSpeed(
                    CareRobotMC.Left_Wheel.byte,
                    Direction.CW.direction,
                    5f,
                    0.1f
                )
                nuriMC.Data!!.copyInto(sedate, 0, 0, nuriMC.Data!!.size)
                nuriMC.ControlAcceleratedSpeed(
                    CareRobotMC.Left_Wheel.byte,
                    Direction.CCW.direction,
                    5f,
                    0.1f
                )
                nuriMC.Data!!.copyInto(sedate, 10, 0, nuriMC.Data!!.size)
                activity?.serialService?.sendData(sedate)
            }
            mBinding.btnTurnLeft -> {
                val sedate = ByteArray(20)
                nuriMC.ControlAcceleratedSpeed(
                    CareRobotMC.Left_Wheel.byte,
                    Direction.CW.direction,
                    5f,
                    0.1f
                )
                nuriMC.Data!!.copyInto(sedate, 0, 0, nuriMC.Data!!.size)
                nuriMC.ControlAcceleratedSpeed(
                    CareRobotMC.Left_Wheel.byte,
                    Direction.CW.direction,
                    5f,
                    0.1f
                )
                nuriMC.Data!!.copyInto(sedate, 10, 0, nuriMC.Data!!.size)
                activity?.serialService?.sendData(sedate)
            }
            mBinding.btnTurnRight -> {
                val sedate = ByteArray(20)
                nuriMC.ControlAcceleratedSpeed(
                    CareRobotMC.Left_Wheel.byte,
                    Direction.CCW.direction,
                    5f,
                    0.1f
                )
                nuriMC.Data!!.copyInto(sedate, 0, 0, nuriMC.Data!!.size)
                nuriMC.ControlAcceleratedSpeed(
                    CareRobotMC.Left_Wheel.byte,
                    Direction.CCW.direction,
                    5f,
                    0.1f
                )
                nuriMC.Data!!.copyInto(sedate, 10, 0, nuriMC.Data!!.size)
                activity?.serialService?.sendData(sedate)
            }
            mBinding.btnStop1 -> {
                stopMotor(CareRobotMC.Left_Wheel.byte, CareRobotMC.Right_Wheel.byte)
            }
            mBinding.btnRotateLeft -> {
                when (selectedShoulderMode) {
                    ControlMode.Left.byte -> {
                        nuriMC.ControlAcceleratedSpeed(
                            CareRobotMC.Left_Shoulder.byte,
                            Direction.CW.direction,
                            1f,
                            0.1f
                        )
                        sharedViewModel.controlDirection = Direction.CW
                        activity?.serialService?.sendData(nuriMC.Data!!)
                    }
                    ControlMode.Right.byte -> {
                        nuriMC.ControlAcceleratedSpeed(
                            CareRobotMC.Right_Shoulder.byte,
                            Direction.CW.direction,
                            1f,
                            0.1f
                        )
                        sharedViewModel.controlDirection = Direction.CW
                        activity?.serialService?.sendData(nuriMC.Data!!)
                    }
                    ControlMode.Both.byte -> {
                        val sedate = ByteArray(20)
                        nuriMC.ControlAcceleratedSpeed(
                            CareRobotMC.Left_Shoulder.byte,
                            Direction.CW.direction,
                            1f,
                            0.1f
                        )
                        nuriMC.Data!!.copyInto(sedate, 0, 0, nuriMC.Data!!.size)
                        nuriMC.ControlAcceleratedSpeed(
                            CareRobotMC.Right_Shoulder.byte,
                            Direction.CW.direction,
                            1f,
                            0.1f
                        )
                        nuriMC.Data!!.copyInto(sedate, 10, 0, nuriMC.Data!!.size)
                        sharedViewModel.controlDirection = Direction.CW
                        activity?.serialService?.sendData(sedate)
                    }
                }
            }
            mBinding.btnRotateRight -> {
                when (selectedShoulderMode) {
                    ControlMode.Left.byte -> {
                        nuriMC.ControlAcceleratedSpeed(
                            CareRobotMC.Left_Shoulder.byte,
                            Direction.CCW.direction,
                            1f,
                            0.1f
                        )
                        sharedViewModel.controlDirection = Direction.CCW
                        activity?.serialService?.sendData(nuriMC.Data!!)
                    }
                    ControlMode.Right.byte -> {
                        nuriMC.ControlAcceleratedSpeed(
                            CareRobotMC.Right_Shoulder.byte,
                            Direction.CCW.direction,
                            1f,
                            0.1f
                        )
                        sharedViewModel.controlDirection = Direction.CCW
                        activity?.serialService?.sendData(nuriMC.Data!!)
                    }
                    ControlMode.Both.byte -> {
                        val sedate = ByteArray(20)
                        nuriMC.ControlAcceleratedSpeed(
                            CareRobotMC.Left_Shoulder.byte,
                            Direction.CCW.direction,
                            1f,
                            0.1f
                        )
                        nuriMC.Data!!.copyInto(sedate, 0, 0, nuriMC.Data!!.size)
                        nuriMC.ControlAcceleratedSpeed(
                            CareRobotMC.Right_Shoulder.byte,
                            Direction.CCW.direction,
                            1f,
                            0.1f
                        )
                        nuriMC.Data!!.copyInto(sedate, 10, 0, nuriMC.Data!!.size)
                        sharedViewModel.controlDirection = Direction.CCW
                        activity?.serialService?.sendData(sedate)
                    }
                }

            }
            mBinding.btnStop2 -> {
                stopMotor(CareRobotMC.Left_Shoulder.byte, CareRobotMC.Right_Shoulder.byte)
            }
            mBinding.btnWaistUp -> {
//                nuriMC.ControlPosSpeed(CareRobotMC.Waist.byte, Direction.CW.direction, 360f, 0.1f)
                nuriMC.ControlAcceleratedSpeed(
                    CareRobotMC.Waist.byte,
                    Direction.CCW.direction,
                    200f,
                    0.1f
                )
                activity?.serialService?.sendData(nuriMC.Data!!.clone())
            }
            mBinding.btnWaistDown -> {
//                nuriMC.ControlPosSpeed(CareRobotMC.Waist.byte, Direction.CCW.direction, 360f, 0.1f)
                nuriMC.ControlAcceleratedSpeed(
                    CareRobotMC.Waist.byte,
                    Direction.CW.direction,
                    200f,
                    0.1f
                )
                activity?.serialService?.sendData(nuriMC.Data!!.clone())
            }
            mBinding.btnStop3 -> {
                stopMotor(CareRobotMC.Waist.byte)
            }
            mBinding.btnElbowUp -> {
                when (selectedElbowMode) {
                    ControlMode.Left.byte -> {
                        nuriMC.ControlAcceleratedSpeed(
                            CareRobotMC.Left_Elbow.byte,
                            Direction.CW.direction,
                            1f,
                            0.1f
                        )
                        activity?.serialService?.sendData(nuriMC.Data!!)
                    }
                    ControlMode.Right.byte -> {
                        nuriMC.ControlAcceleratedSpeed(
                            CareRobotMC.Right_Elbow.byte,
                            Direction.CCW.direction,
                            1f,
                            0.1f
                        )
                        activity?.serialService?.sendData(nuriMC.Data!!)
                    }
                    ControlMode.Both.byte -> {
                        val sedate = ByteArray(20)
                        nuriMC.ControlAcceleratedSpeed(
                            CareRobotMC.Left_Elbow.byte,
                            Direction.CW.direction,
                            1f,
                            0.1f
                        )
                        nuriMC.Data!!.copyInto(sedate, 0, 0, nuriMC.Data!!.size)
                        nuriMC.ControlAcceleratedSpeed(
                            CareRobotMC.Right_Elbow.byte,
                            Direction.CCW.direction,
                            1f,
                            0.1f
                        )
                        nuriMC.Data!!.copyInto(sedate, 10, 0, nuriMC.Data!!.size)
                        activity?.serialService?.sendData(sedate)
                    }
                }
            }
            mBinding.btnElbowDown -> {
                when (selectedElbowMode) {
                    ControlMode.Left.byte -> {
                        nuriMC.ControlAcceleratedSpeed(
                            CareRobotMC.Left_Elbow.byte,
                            Direction.CCW.direction,
                            1f,
                            0.1f
                        )
                        activity?.serialService?.sendData(nuriMC.Data!!)
                    }
                    ControlMode.Right.byte -> {
                        nuriMC.ControlAcceleratedSpeed(
                            CareRobotMC.Right_Elbow.byte,
                            Direction.CW.direction,
                            1f,
                            0.1f
                        )
                        activity?.serialService?.sendData(nuriMC.Data!!)
                    }
                    ControlMode.Both.byte -> {
                        val sedate = ByteArray(20)
                        nuriMC.ControlAcceleratedSpeed(
                            CareRobotMC.Left_Elbow.byte,
                            Direction.CCW.direction,
                            1f,
                            0.1f
                        )
                        nuriMC.Data!!.copyInto(sedate, 0, 0, nuriMC.Data!!.size)
                        nuriMC.ControlAcceleratedSpeed(
                            CareRobotMC.Right_Elbow.byte,
                            Direction.CW.direction,
                            1f,
                            0.1f
                        )
                        nuriMC.Data!!.copyInto(sedate, 10, 0, nuriMC.Data!!.size)
                        activity?.serialService?.sendData(sedate)
                    }
                }
            }
            mBinding.btnStop4 -> {
                stopMotor(CareRobotMC.Left_Elbow.byte, CareRobotMC.Right_Elbow.byte)
            }
        }

    }

    private fun stopMotor(id_1: Byte, id_2: Byte? = null) {
        val nuriMC = NurirobotMC()
        if (id_2 == null) {
            nuriMC.ControlAcceleratedSpeed(id_1, Direction.CCW.direction, 0f, 0.1f)
            activity?.serialService?.sendData(nuriMC.Data!!.clone())
        } else {
            val sedate = ByteArray(20)
            nuriMC.ControlAcceleratedSpeed(id_1, Direction.CCW.direction, 0f, 0.1f)
            nuriMC.Data!!.clone().copyInto(sedate, 0, 0, nuriMC.Data!!.size)
            nuriMC.ControlAcceleratedSpeed(id_2, Direction.CCW.direction, 0f, 0.1f)
            nuriMC.Data!!.clone().copyInto(sedate, 10, 0, nuriMC.Data!!.size)
            activity?.serialService?.sendData(sedate)
        }
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