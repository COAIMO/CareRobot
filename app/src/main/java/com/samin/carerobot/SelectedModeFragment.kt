package com.samin.carerobot

import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.activityViewModels
import com.jeongmin.nurimotortester.Nuri.Direction
import com.jeongmin.nurimotortester.Nuri.ProtocolMode
import com.jeongmin.nurimotortester.NurirobotMC
import com.samin.carerobot.LoadingPage.LoadingDialog
import com.samin.carerobot.Logics.CareRobotMC
import com.samin.carerobot.Logics.HexDump
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
    private lateinit var changeRobotPositionThread_1: Thread
    private lateinit var changeRobotPositionThread_2: Thread

    private lateinit var laodingView: LoadingDialog
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

    private fun setButtonClickEvent() {
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

    var step_1 = true
    var step_2 = true
    var right_ShoulderSet = false
    var left_ShoulderSet = false
    var right_ElbowSet = false
    var left_ElbowSet = false
    private fun onClick(view: View) {
        laodingView = LoadingDialog(requireContext())
        when (view) {
            mBinding.btnHeavy -> {
                laodingView.show()
                step_1 = true
                step_2 = true
                right_ShoulderSet = false
                left_ShoulderSet = false
                right_ElbowSet = false
                left_ElbowSet = false
                changeRobotPositionThread_1 = Thread {
                    val nuriMC = NurirobotMC()

                    while (step_1) {
                        val rightShoulder_tmp =
                            sharedViewModel.motorInfo[CareRobotMC.Right_Shoulder_Encoder.byte]
                        val leftShoulder_tmp =
                            sharedViewModel.motorInfo[CareRobotMC.Left_Shoulder_Encoder.byte]
                        if (rightShoulder_tmp?.position!! > 1 && rightShoulder_tmp.position!! <= rightShoulder_tmp.min_Range!! + 10) {
                            right_ShoulderSet = false
                            if (rightShoulder_tmp?.position!! < 3) {
                                nuriMC.ControlAcceleratedSpeed(
                                    CareRobotMC.Right_Shoulder.byte,
                                    Direction.CW.direction,
                                    0.3f,
                                    0.1f
                                )
                            } else {
                                nuriMC.ControlAcceleratedSpeed(
                                    CareRobotMC.Right_Shoulder.byte,
                                    Direction.CW.direction,
                                    1f,
                                    0.1f
                                )
                            }
                            activity?.serialService?.sendData(nuriMC.Data!!.clone())
                            Thread.sleep(20)
                        } else if (rightShoulder_tmp.min_Range!! < rightShoulder_tmp.position!! && rightShoulder_tmp.position!! > rightShoulder_tmp.max_Range!! - 10) {
                            right_ShoulderSet = false
                            if (rightShoulder_tmp.position!! > 362) {
                                nuriMC.ControlAcceleratedSpeed(
                                    CareRobotMC.Right_Shoulder.byte,
                                    Direction.CCW.direction,
                                    0.3f,
                                    0.1f
                                )
                            } else {
                                nuriMC.ControlAcceleratedSpeed(
                                    CareRobotMC.Right_Shoulder.byte,
                                    Direction.CCW.direction,
                                    1f,
                                    0.1f
                                )
                            }
                            activity?.serialService?.sendData(nuriMC.Data!!.clone())
                            Thread.sleep(20)
                        } else if (rightShoulder_tmp.position!! > 0 && rightShoulder_tmp.position!! < 1) {
                            if (!right_ShoulderSet) {
                                activity?.stopMotor(CareRobotMC.Right_Shoulder.byte)
                            }
                            right_ShoulderSet = true
                        }

                        if (leftShoulder_tmp?.position!! > 1 && leftShoulder_tmp.position!! <= leftShoulder_tmp.min_Range!! + 10) {
                            left_ShoulderSet = false
                            if (leftShoulder_tmp.position!! < 3) {
                                nuriMC.ControlAcceleratedSpeed(
                                    CareRobotMC.Left_Shoulder.byte,
                                    Direction.CW.direction,
                                    0.3f,
                                    0.1f
                                )
                            } else {
                                nuriMC.ControlAcceleratedSpeed(
                                    CareRobotMC.Left_Shoulder.byte,
                                    Direction.CW.direction,
                                    1f,
                                    0.1f
                                )
                            }
                            activity?.serialService?.sendData(nuriMC.Data!!.clone())
                            Thread.sleep(20)
                        } else if (leftShoulder_tmp.position!! > leftShoulder_tmp.min_Range!! && leftShoulder_tmp.position!! > leftShoulder_tmp.max_Range!! - 10) {
                            left_ShoulderSet = false
                            if (leftShoulder_tmp.position!! > 362) {
                                nuriMC.ControlAcceleratedSpeed(
                                    CareRobotMC.Left_Shoulder.byte,
                                    Direction.CCW.direction,
                                    0.3f,
                                    0.1f
                                )
                            } else {
                                nuriMC.ControlAcceleratedSpeed(
                                    CareRobotMC.Left_Shoulder.byte,
                                    Direction.CCW.direction,
                                    1f,
                                    0.1f
                                )
                            }
                            activity?.serialService?.sendData(nuriMC.Data!!.clone())
                            Thread.sleep(20)
                        } else if (leftShoulder_tmp.position!! > 0 && leftShoulder_tmp.position!! < 1) {
                            if (!left_ShoulderSet) {
                                activity?.stopMotor(CareRobotMC.Left_Shoulder.byte)
                            }
                            left_ShoulderSet = true
                        }

                        if (right_ShoulderSet && left_ShoulderSet) {
                            step_1 = false
                        }
                    }

                    while (step_2) {
                        val rightElbow_tmp =
                            sharedViewModel.motorInfo[CareRobotMC.Right_Elbow_Encoder.byte]
                        val leftElbow_tmp =
                            sharedViewModel.motorInfo[CareRobotMC.Left_Elbow_Encoder.byte]

                        if (rightElbow_tmp?.position!! > rightElbow_tmp.min_Range!! - 10 && rightElbow_tmp.position!! <= 94) {
                            right_ElbowSet = false
                            if (rightElbow_tmp.position!! > 92) {
                                nuriMC.ControlAcceleratedSpeed(
                                    CareRobotMC.Right_Elbow.byte,
                                    Direction.CCW.direction,
                                    0.3f,
                                    0.1f
                                )
                            } else {
                                nuriMC.ControlAcceleratedSpeed(
                                    CareRobotMC.Right_Elbow.byte,
                                    Direction.CCW.direction,
                                    1f,
                                    0.1f
                                )
                            }
                            activity?.serialService?.sendData(nuriMC.Data!!)
                            Thread.sleep(20)
                        } else if (95 < rightElbow_tmp.position!! && rightElbow_tmp.position!! < rightElbow_tmp.max_Range!! + 10) {
                            right_ElbowSet = false
                            if (rightElbow_tmp.position!! < 98) {
                                nuriMC.ControlAcceleratedSpeed(
                                    CareRobotMC.Right_Elbow.byte,
                                    Direction.CW.direction,
                                    0.3f,
                                    0.1f
                                )
                            } else {
                                nuriMC.ControlAcceleratedSpeed(
                                    CareRobotMC.Right_Elbow.byte,
                                    Direction.CW.direction,
                                    1f,
                                    0.1f
                                )
                            }
                            activity?.serialService?.sendData(nuriMC.Data!!)
                            Thread.sleep(20)
                        } else if (rightElbow_tmp.position!! > 94 && rightElbow_tmp.position!! < 95) {
                            if (!right_ElbowSet) {
                                activity?.stopMotor(CareRobotMC.Right_Elbow.byte)
                            }
                            right_ElbowSet = true
                        }

                        if (leftElbow_tmp?.position!! > leftElbow_tmp.min_Range!! - 10 && leftElbow_tmp.position!! <= 264) {
                            left_ElbowSet = false
                            if (leftElbow_tmp.position!! > 262) {
                                nuriMC.ControlAcceleratedSpeed(
                                    CareRobotMC.Left_Elbow.byte,
                                    Direction.CCW.direction,
                                    0.3f,
                                    0.1f
                                )
                            } else {
                                nuriMC.ControlAcceleratedSpeed(
                                    CareRobotMC.Left_Elbow.byte,
                                    Direction.CCW.direction,
                                    1f,
                                    0.1f
                                )
                            }
                            activity?.serialService?.sendData(nuriMC.Data!!)
                            Thread.sleep(20)
                        } else if (265 < leftElbow_tmp.position!! && leftElbow_tmp.position!! < leftElbow_tmp.max_Range!! + 10) {
                            left_ElbowSet = false
                            if (leftElbow_tmp.position!! < 268) {
                                nuriMC.ControlAcceleratedSpeed(
                                    CareRobotMC.Left_Elbow.byte,
                                    Direction.CW.direction,
                                    0.3f,
                                    0.1f
                                )
                            } else {
                                nuriMC.ControlAcceleratedSpeed(
                                    CareRobotMC.Left_Elbow.byte,
                                    Direction.CW.direction,
                                    1f,
                                    0.1f
                                )
                            }
                            activity?.serialService?.sendData(nuriMC.Data!!)
                            Thread.sleep(20)
                        } else if (leftElbow_tmp.position!! > 264 && leftElbow_tmp.position!! < 265) {
                            if (!left_ElbowSet) {
                                activity?.stopMotor(CareRobotMC.Left_Elbow.byte)
                            }
                            left_ElbowSet = true
                        }

                        if (right_ElbowSet && left_ElbowSet) {
                            step_2 = false
                        }
                    }

                    activity?.runOnUiThread {
                        sharedViewModel.viewState.value = SharedViewModel.MODE_CARRY_HEAVY
                        laodingView.dismiss()
                    }
                }
                changeRobotPositionThread_1.start()
            }
            mBinding.btnHeight -> {
                sharedViewModel.viewState.value = SharedViewModel.MODE_CARRY_HEIGHT
                laodingView.show()
                step_1 = true
                step_2 = true
                right_ShoulderSet = false
                left_ShoulderSet = false
                right_ElbowSet = false
                left_ElbowSet = false
                changeRobotPositionThread_2 = Thread {
                    val nuriMC = NurirobotMC()

                    while (step_1) {
                        val rightShoulder_tmp =
                            sharedViewModel.motorInfo[CareRobotMC.Right_Shoulder_Encoder.byte]
                        val leftShoulder_tmp =
                            sharedViewModel.motorInfo[CareRobotMC.Left_Shoulder_Encoder.byte]
//                        if (rightShoulder_tmp?.position!! > 180 && rightShoulder_tmp.position!! <= rightShoulder_tmp.min_Range!! + 10) {
//                            right_ShoulderSet = false
//                            if (rightShoulder_tmp.position!! < 183) {
//                                nuriMC.ControlAcceleratedSpeed(
//                                    CareRobotMC.Right_Shoulder.byte,
//                                    Direction.CW.direction,
//                                    0.3f,
//                                    0.1f
//                                )
//                            } else {
//                                nuriMC.ControlAcceleratedSpeed(
//                                    CareRobotMC.Right_Shoulder.byte,
//                                    Direction.CW.direction,
//                                    1f,
//                                    0.1f
//                                )
//                            }
//                            activity?.serialService?.sendData(nuriMC.Data!!.clone())
//                            Thread.sleep(20)
//                        } else if (rightShoulder_tmp.position!! > rightShoulder_tmp.min_Range!! && rightShoulder_tmp.position!! > rightShoulder_tmp.max_Range!! - 10) {
//                            right_ShoulderSet = false
//                            nuriMC.ControlAcceleratedSpeed(
//                                CareRobotMC.Right_Shoulder.byte,
//                                Direction.CCW.direction,
//                                1f,
//                                0.1f
//                            )
//                            activity?.serialService?.sendData(nuriMC.Data!!.clone())
//                            Thread.sleep(20)
//                        } else if (rightShoulder_tmp.position!! < 180) {
//                            right_ShoulderSet = false
//                            if (rightShoulder_tmp.position!! > 177) {
//                                nuriMC.ControlAcceleratedSpeed(
//                                    CareRobotMC.Right_Shoulder.byte,
//                                    Direction.CCW.direction,
//                                    0.3f,
//                                    0.1f
//                                )
//                            } else {
//                                nuriMC.ControlAcceleratedSpeed(
//                                    CareRobotMC.Right_Shoulder.byte,
//                                    Direction.CCW.direction,
//                                    1f,
//                                    0.1f
//                                )
//                            }
//                            activity?.serialService?.sendData(nuriMC.Data!!.clone())
//                            Thread.sleep(20)
//                        } else if (rightShoulder_tmp.position!! > 179 && rightShoulder_tmp.position!! <= 180) {
//                            if (right_ShoulderSet) {
//                                activity?.stopMotor(CareRobotMC.Right_Shoulder.byte)
//                            }
//                            right_ShoulderSet = true
//                        }

                        if (leftShoulder_tmp?.position!! < 180 && leftShoulder_tmp.position!! >= leftShoulder_tmp.max_Range!! - 10) {
                            left_ShoulderSet = false
                            if (leftShoulder_tmp.position!! > 175) {
                                nuriMC.ControlAcceleratedSpeed(
                                    CareRobotMC.Left_Shoulder.byte,
                                    Direction.CCW.direction,
                                    0.3f,
                                    0.1f
                                )

                            } else {
                                nuriMC.ControlAcceleratedSpeed(
                                    CareRobotMC.Left_Shoulder.byte,
                                    Direction.CCW.direction,
                                    1f,
                                    0.1f
                                )
                            }
                            activity?.serialService?.sendData(nuriMC.Data!!)
                            Thread.sleep(20)
                        }
//                        else if (leftShoulder_tmp.position!! < leftShoulder_tmp.min_Range!! + 10) {
//                            left_ShoulderSet = false
//                            nuriMC.ControlAcceleratedSpeed(
//                                CareRobotMC.Left_Shoulder.byte,
//                                Direction.CW.direction,
//                                2f,
//                                0.1f
//                            )
//                            activity?.serialService?.sendData(nuriMC.Data!!.clone())
//                            Thread.sleep(20)
//                        }
//                        else if (leftShoulder_tmp.position!! > 181) {
//                            left_ShoulderSet = false
//                            if (leftShoulder_tmp.position!! < 183) {
//                                nuriMC.ControlAcceleratedSpeed(
//                                    CareRobotMC.Left_Shoulder.byte,
//                                    Direction.CW.direction,
//                                    0.3f,
//                                    0.1f
//                                )
//                            } else {
//                                nuriMC.ControlAcceleratedSpeed(
//                                    CareRobotMC.Left_Shoulder.byte,
//                                    Direction.CW.direction,
//                                    1f,
//                                    0.1f
//                                )
//                            }
//                            activity?.serialService?.sendData(nuriMC.Data!!.clone())
//                            Thread.sleep(20)
//                        }
                        else if (leftShoulder_tmp.position!! > 180 && leftShoulder_tmp.position!! < 181) {
                            val msg = datahandler.obtainMessage(0, CareRobotMC.Left_Shoulder.byte)
                            datahandler.handleMessage(msg)

                        }

                        if (right_ShoulderSet && left_ShoulderSet) {
                            step_1 = false
                        }

                    }

//                    while (step_2) {
//                        val rightElbow_tmp =
//                            sharedViewModel.motorInfo[CareRobotMC.Right_Elbow_Encoder.byte]
//                        val leftElbow_tmp =
//                            sharedViewModel.motorInfo[CareRobotMC.Left_Elbow_Encoder.byte]
//
//                        if (rightElbow_tmp?.position!! > rightElbow_tmp.min_Range!! - 10 && rightElbow_tmp.position!! <= 94) {
//                            right_ElbowSet = false
//                            if (rightElbow_tmp.position!! > 92) {
//                                nuriMC.ControlAcceleratedSpeed(
//                                    CareRobotMC.Right_Elbow.byte,
//                                    Direction.CCW.direction,
//                                    0.3f,
//                                    0.1f
//                                )
//                            } else {
//                                nuriMC.ControlAcceleratedSpeed(
//                                    CareRobotMC.Right_Elbow.byte,
//                                    Direction.CCW.direction,
//                                    1f,
//                                    0.1f
//                                )
//                            }
//                            activity?.serialService?.sendData(nuriMC.Data!!)
//                            Thread.sleep(20)
//                        } else if (95 < rightElbow_tmp.position!! && rightElbow_tmp.position!! < rightElbow_tmp.max_Range!! + 10) {
//                            right_ElbowSet = false
//                            if (rightElbow_tmp.position!! < 98) {
//                                nuriMC.ControlAcceleratedSpeed(
//                                    CareRobotMC.Right_Elbow.byte,
//                                    Direction.CW.direction,
//                                    0.3f,
//                                    0.1f
//                                )
//                            } else {
//                                nuriMC.ControlAcceleratedSpeed(
//                                    CareRobotMC.Right_Elbow.byte,
//                                    Direction.CW.direction,
//                                    1f,
//                                    0.1f
//                                )
//                            }
//                            activity?.serialService?.sendData(nuriMC.Data!!)
//                            Thread.sleep(20)
//                        } else if (rightElbow_tmp.position!! > 94 && rightElbow_tmp.position!! < 95) {
//                            if (!right_ElbowSet) {
//                                activity?.stopMotor(CareRobotMC.Right_Elbow.byte)
//                            }
//                            right_ElbowSet = true
//                        }
//
//                        if (leftElbow_tmp?.position!! > leftElbow_tmp.min_Range!! - 10 && leftElbow_tmp.position!! <= 264) {
//                            left_ElbowSet = false
//                            if (leftElbow_tmp.position!! > 262) {
//                                nuriMC.ControlAcceleratedSpeed(
//                                    CareRobotMC.Left_Elbow.byte,
//                                    Direction.CCW.direction,
//                                    0.3f,
//                                    0.1f
//                                )
//                            } else {
//                                nuriMC.ControlAcceleratedSpeed(
//                                    CareRobotMC.Left_Elbow.byte,
//                                    Direction.CCW.direction,
//                                    1f,
//                                    0.1f
//                                )
//                            }
//                            activity?.serialService?.sendData(nuriMC.Data!!)
//                            Thread.sleep(20)
//                        } else if (265 < leftElbow_tmp.position!! && leftElbow_tmp.position!! < leftElbow_tmp.max_Range!! + 10) {
//                            left_ElbowSet = false
//                            if (leftElbow_tmp.position!! < 268) {
//                                nuriMC.ControlAcceleratedSpeed(
//                                    CareRobotMC.Left_Elbow.byte,
//                                    Direction.CW.direction,
//                                    0.3f,
//                                    0.1f
//                                )
//                            } else {
//                                nuriMC.ControlAcceleratedSpeed(
//                                    CareRobotMC.Left_Elbow.byte,
//                                    Direction.CW.direction,
//                                    1f,
//                                    0.1f
//                                )
//                            }
//                            activity?.serialService?.sendData(nuriMC.Data!!)
//                            Thread.sleep(20)
//                        } else if (leftElbow_tmp.position!! > 264 && leftElbow_tmp.position!! < 265) {
//                            if (!left_ElbowSet) {
//                                activity?.stopMotor(CareRobotMC.Left_Elbow.byte)
//                            }
//                            left_ElbowSet = true
//                        }
//
//                        if (right_ElbowSet && left_ElbowSet) {
//                            step_2 = false
//                        }
//                    }

                    activity?.runOnUiThread {
                        sharedViewModel.viewState.value = SharedViewModel.MODE_CARRY_HEIGHT
                        laodingView.dismiss()
                    }
                }
                changeRobotPositionThread_2.start()
            }
            mBinding.btnStand -> {
                sharedViewModel.viewState.value = SharedViewModel.MODE_BEHAVIOR_STAND
            }
            mBinding.btnWalkHand -> {
                sharedViewModel.viewState.value = SharedViewModel.MODE_BEHAVIOR_WALKHAND
            }
            mBinding.btnWalkHug -> {
                sharedViewModel.viewState.value = SharedViewModel.MODE_BEHAVIOR_WALKHUG
            }
            mBinding.btnChangeHug -> {
                sharedViewModel.viewState.value = SharedViewModel.MODE_CHANGE_CHANGEHUG
            }
            mBinding.btnTransferStand -> {
                sharedViewModel.viewState.value = SharedViewModel.MODE_CHANGE_TRANSFERSTAND
            }
            mBinding.btnTransferHarness -> {
                sharedViewModel.viewState.value = SharedViewModel.MODE_CHANGE_TRANSFERHARNESS
            }
            mBinding.btnPosition -> {
                sharedViewModel.viewState.value = SharedViewModel.MODE_ALL_POSITION
            }
            mBinding.btnChangeSling -> {
                sharedViewModel.viewState.value = SharedViewModel.MODE_ALL_CHANGESLING
            }
            mBinding.btnTransferSling -> {
                sharedViewModel.viewState.value = SharedViewModel.MODE_ALL_TRANSFERSLING
            }
            mBinding.btnTransferBedriddenSling -> {
                sharedViewModel.viewState.value = SharedViewModel.MODE_ALL_TRANSFERBEDRIDDENSLING
            }
            mBinding.btnTransferBedriddenBoard -> {
                sharedViewModel.viewState.value = SharedViewModel.MODE_ALL_TRANSFERBEDRIDDENBOARD
            }
            mBinding.btnTransferChair -> {
                sharedViewModel.viewState.value = SharedViewModel.MODE_ALL_TRANSFERCHAIR
            }
        }

    }

    fun aasdfa() {
        val nuriMC = NurirobotMC()

        while (step_1) {
            val rightShoulder_tmp =
                sharedViewModel.motorInfo[CareRobotMC.Right_Shoulder_Encoder.byte]
            val leftShoulder_tmp =
                sharedViewModel.motorInfo[CareRobotMC.Left_Shoulder_Encoder.byte]
            val sedate = ByteArray(20)

            //mode 1
            if (rightShoulder_tmp?.position!! > 1 && rightShoulder_tmp.position!! <= rightShoulder_tmp.min_Range!! + 10) {
                nuriMC.ControlAcceleratedSpeed(
                    CareRobotMC.Right_Shoulder.byte,
                    Direction.CW.direction,
                    2f,
                    0.1f
                )
                nuriMC.Data!!.copyInto(sedate, 0, 0, nuriMC.Data!!.size)
            } else if (rightShoulder_tmp.min_Range!! < rightShoulder_tmp.position!! && rightShoulder_tmp.position!! > rightShoulder_tmp.max_Range!! - 10) {
                nuriMC.ControlAcceleratedSpeed(
                    CareRobotMC.Right_Shoulder.byte,
                    Direction.CCW.direction,
                    2f,
                    0.1f
                )
                nuriMC.Data!!.copyInto(sedate, 0, 0, nuriMC.Data!!.size)
            } else if (rightShoulder_tmp.position!! > 0 && rightShoulder_tmp.position!! < 1) {
                activity?.stopMotor(CareRobotMC.Right_Shoulder.byte)
            }

            if (leftShoulder_tmp?.position!! > 1 && leftShoulder_tmp.position!! <= leftShoulder_tmp.min_Range!! + 10) {
                nuriMC.ControlAcceleratedSpeed(
                    CareRobotMC.Left_Shoulder.byte,
                    Direction.CW.direction,
                    2f,
                    0.1f
                )
                nuriMC.Data!!.copyInto(sedate, 10, 0, nuriMC.Data!!.size)
            } else if (leftShoulder_tmp.position!! > leftShoulder_tmp.min_Range!! && leftShoulder_tmp.position!! > leftShoulder_tmp.max_Range!! - 10) {
                nuriMC.ControlAcceleratedSpeed(
                    CareRobotMC.Left_Shoulder.byte,
                    Direction.CCW.direction,
                    2f,
                    0.1f
                )
                nuriMC.Data!!.copyInto(sedate, 10, 0, nuriMC.Data!!.size)
            } else if (leftShoulder_tmp.position!! > 0 && leftShoulder_tmp.position!! < 1) {
                activity?.stopMotor(CareRobotMC.Left_Shoulder.byte)
                step_1 = false
            }
            activity?.serialService?.sendData(sedate)


            //mode 2
            if (rightShoulder_tmp?.position!! > 180 && rightShoulder_tmp.position!! <= rightShoulder_tmp.min_Range!! + 10) {
                nuriMC.ControlAcceleratedSpeed(
                    CareRobotMC.Right_Shoulder.byte,
                    Direction.CW.direction,
                    2f,
                    0.1f
                )
                nuriMC.Data!!.copyInto(sedate, 0, 0, nuriMC.Data!!.size)
            } else if (rightShoulder_tmp.position!! > rightShoulder_tmp.min_Range!! && rightShoulder_tmp.position!! > rightShoulder_tmp.max_Range!!) {
                nuriMC.ControlAcceleratedSpeed(
                    CareRobotMC.Right_Shoulder.byte,
                    Direction.CCW.direction,
                    2f,
                    0.1f
                )
                nuriMC.Data!!.copyInto(sedate, 0, 0, nuriMC.Data!!.size)
            } else if (rightShoulder_tmp.position!! < 179) {
                nuriMC.ControlAcceleratedSpeed(
                    CareRobotMC.Right_Shoulder.byte,
                    Direction.CCW.direction,
                    2f,
                    0.1f
                )
                nuriMC.Data!!.copyInto(sedate, 0, 0, nuriMC.Data!!.size)
            } else if (rightShoulder_tmp.position!! > 179 && rightShoulder_tmp.position!! < 180) {
                activity?.stopMotor(CareRobotMC.Right_Shoulder.byte)
            }

            if (leftShoulder_tmp?.position!! < 180 && leftShoulder_tmp.position!! >= leftShoulder_tmp.max_Range!! - 10) {
                nuriMC.ControlAcceleratedSpeed(
                    CareRobotMC.Left_Shoulder.byte,
                    Direction.CCW.direction,
                    2f,
                    0.1f
                )
                nuriMC.Data!!.copyInto(sedate, 10, 0, nuriMC.Data!!.size)
            } else if (leftShoulder_tmp.position!! < leftShoulder_tmp.min_Range!! + 10 && leftShoulder_tmp.position!! >= 0) {
                nuriMC.ControlAcceleratedSpeed(
                    CareRobotMC.Left_Shoulder.byte,
                    Direction.CW.direction,
                    2f,
                    0.1f
                )
                nuriMC.Data!!.copyInto(sedate, 10, 0, nuriMC.Data!!.size)
            } else if (leftShoulder_tmp.position!! > 181) {
                nuriMC.ControlAcceleratedSpeed(
                    CareRobotMC.Left_Shoulder.byte,
                    Direction.CW.direction,
                    2f,
                    0.1f
                )
                nuriMC.Data!!.copyInto(sedate, 10, 0, nuriMC.Data!!.size)
            } else if (leftShoulder_tmp.position!! > 180 && leftShoulder_tmp.position!! < 181) {
                activity?.stopMotor(CareRobotMC.Left_Shoulder.byte)
                step_1 = false
            }
        }

        while (step_2) {
            val rightElbow_tmp = sharedViewModel.motorInfo[CareRobotMC.Right_Elbow.byte]
            val leftElbow_tmp = sharedViewModel.motorInfo[CareRobotMC.Left_Elbow.byte]

            if (rightElbow_tmp?.position!! > rightElbow_tmp.min_Range!! - 10 && rightElbow_tmp.position!! <= 94) {
                nuriMC.ControlAcceleratedSpeed(
                    CareRobotMC.Right_Elbow.byte,
                    Direction.CCW.direction,
                    3f,
                    0.1f
                )
                activity?.serialService?.sendData(nuriMC.Data!!)
                Thread.sleep(20)
            } else if (95 < rightElbow_tmp.position!! && rightElbow_tmp.position!! < rightElbow_tmp.max_Range!! + 10) {
                nuriMC.ControlAcceleratedSpeed(
                    CareRobotMC.Right_Elbow.byte,
                    Direction.CW.direction,
                    3f,
                    0.1f
                )
                activity?.serialService?.sendData(nuriMC.Data!!)
                Thread.sleep(20)
            } else if (rightElbow_tmp.position!! > 94 && rightElbow_tmp.position!! < 95) {
                activity?.stopMotor(CareRobotMC.Right_Shoulder.byte)
                step_2 = false
            }

            if (leftElbow_tmp?.position!! > leftElbow_tmp.min_Range!! - 10 && leftElbow_tmp.position!! <= 264) {
                nuriMC.ControlAcceleratedSpeed(
                    CareRobotMC.Left_Elbow.byte,
                    Direction.CCW.direction,
                    3f,
                    0.1f
                )
                activity?.serialService?.sendData(nuriMC.Data!!)
                Thread.sleep(20)
            } else if (265 < leftElbow_tmp.position!! && leftElbow_tmp.position!! < leftElbow_tmp.max_Range!! + 10) {
                nuriMC.ControlAcceleratedSpeed(
                    CareRobotMC.Left_Elbow.byte,
                    Direction.CW.direction,
                    3f,
                    0.1f
                )
                activity?.serialService?.sendData(nuriMC.Data!!)
                Thread.sleep(20)
            } else if (leftElbow_tmp.position!! > 264 && leftElbow_tmp.position!! < 265) {
                activity?.stopMotor(CareRobotMC.Left_Elbow.byte)
                step_2 = false
            }


        }
    }

    val datahandler = object : Handler(Looper.getMainLooper()) {
        override fun handleMessage(msg: Message) {
            when (msg.what) {
                0 -> {
                    stopMotor(CareRobotMC.Left_Shoulder.byte)
                    Log.d("망","asdfasdfasdf")

                }


            }
        }
    }

    private fun stopMotor(id_1: Byte, id_2: Byte? = null) {
        val nuriMC = NurirobotMC()
        for (count in 0..2){
            if (id_2 == null) {
                nuriMC.ControlAcceleratedSpeed(id_1, Direction.CCW.direction, 0f, 0.1f)
                activity?.serialService?.sendData(nuriMC.Data!!.clone())
                Log.d("망","${HexDump.toHexString(nuriMC.Data!!.clone())}")

            } else {
                val sedate = ByteArray(22)
                nuriMC.ControlPosSpeed(id_1, Direction.CCW.direction, 0f, 0f)
                nuriMC.Data!!.clone().copyInto(sedate, 0, 0, nuriMC.Data!!.size)
                nuriMC.ControlPosSpeed(id_2, Direction.CCW.direction, 0f, 0f)
                nuriMC.Data!!.clone().copyInto(sedate, 11, 0, nuriMC.Data!!.size)
                activity?.serialService?.sendData(sedate)
            }
            Thread.sleep(10)
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