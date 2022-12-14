package com.samin.carerobot.Logics

import android.util.Log
import android.view.InputDevice
import android.view.InputEvent
import android.view.KeyEvent
import android.view.MotionEvent
import com.samin.carerobot.MainActivity

class ControllerPad(viewModel: SharedViewModel) {
    private var directionPressed = -1
    val sharedViewModel: SharedViewModel = viewModel
    var ex_X = 0f
    var ex_Y = 0f
    var isUsable = false

    fun getDirectionPressed(event: InputEvent): Int {
        if (!isDpadDevice(event)) {
            return -1
        }

        (event as? MotionEvent)?.apply {

            val xaxis: Float = event.getAxisValue(MotionEvent.AXIS_HAT_X)
            val yaxis: Float = event.getAxisValue(MotionEvent.AXIS_HAT_Y)

            directionPressed = when {
                xaxis.compareTo(-1.0f) == 0 -> LEFT
                xaxis.compareTo(1.0f) == 0 -> RIGHT
                yaxis.compareTo(-1.0f) == 0 -> UP
                yaxis.compareTo(1.0f) == 0 -> DOWN
                else -> directionPressed
            }
        }
        (event as? KeyEvent)?.apply {

            directionPressed = when (event.keyCode) {
                KeyEvent.KEYCODE_DPAD_LEFT -> LEFT
                KeyEvent.KEYCODE_DPAD_RIGHT -> RIGHT
                KeyEvent.KEYCODE_DPAD_UP -> UP
                KeyEvent.KEYCODE_DPAD_DOWN -> DOWN
                KeyEvent.KEYCODE_DPAD_CENTER -> CENTER
                else -> directionPressed
            }
        }
        return directionPressed
    }

    fun getCenteredAxis(
        event: MotionEvent,
        device: InputDevice,
        axis: Int,
        historyPos: Int
    ): Float {
        val range: InputDevice.MotionRange? = device.getMotionRange(axis, event.source)

        // A joystick at rest does not always report an absolute position of
        // (0,0). Use the getFlat() method to determine the range of values
        // bounding the joystick axis center.
        range?.apply {
            val value: Float = if (historyPos < 0) {
                event.getAxisValue(axis)
            } else {
                event.getHistoricalAxisValue(axis, historyPos)
            }

            // Ignore axis values that are within the 'flat' region of the
            // joystick axis center.
            if (Math.abs(value) > flat) {
                return value
            }
        }
        return 0f
    }

    fun processJoystickInput(event: MotionEvent, historyPos: Int) {
        val inputDevice = event.device

        //왼쪽 조이스틱
        var x: Float = getCenteredAxis(event, inputDevice, MotionEvent.AXIS_X, historyPos)
        var y: Float = getCenteredAxis(event, inputDevice, MotionEvent.AXIS_Y, historyPos)
//        if (!ex_X.equals(x) || !ex_Y.equals(y)) {
//            ex_X = x
//            ex_Y = y
            sharedViewModel.left_Joystick.postValue(JoystickCoordinate(x, y))

//        }
//        if  (ex_X == 0f || ex_Y == 0f){
            sharedViewModel.left_Joystick.postValue(JoystickCoordinate(x, y))
//        }


        //방향키값 들어옴
        if (x == 0f) {
            x = getCenteredAxis(event, inputDevice, MotionEvent.AXIS_HAT_X, historyPos)
//            Log.d(MainActivity.TAG, " AXIS_HAT_X : $x")
        }
        if (y == 0f) {
            y = getCenteredAxis(event, inputDevice, MotionEvent.AXIS_HAT_Y, historyPos)
//            Log.d(MainActivity.TAG, " AXIS_HAT_Y : $y")
        }

        //오른쪽 조이스틱
        if (x == 0f && y == 0f) {
            x = getCenteredAxis(event, inputDevice, MotionEvent.AXIS_Z, historyPos)
            y = getCenteredAxis(event, inputDevice, MotionEvent.AXIS_RZ, historyPos)
            sharedViewModel.right_Joystick.postValue(JoystickCoordinate(x, y))
        }
//        if (y == 0f) {
//            y = getCenteredAxis(event, inputDevice, MotionEvent.AXIS_RZ, historyPos)
//        }
    }

    companion object {
        internal const val UP = 0
        internal const val LEFT = 1
        internal const val RIGHT = 2
        internal const val DOWN = 3
        internal const val CENTER = 4

        fun isDpadDevice(event: InputEvent): Boolean =
            // Check that input comes from a device with directional pads.
            event.source and InputDevice.SOURCE_DPAD != InputDevice.SOURCE_DPAD

        fun isJoyStick(event: MotionEvent): Boolean =
            event.source and InputDevice.SOURCE_JOYSTICK == InputDevice.SOURCE_JOYSTICK
                    && event.action == MotionEvent.ACTION_MOVE

        fun isGamePad(event: KeyEvent): Boolean =
            event.source and InputDevice.SOURCE_GAMEPAD == InputDevice.SOURCE_GAMEPAD
    }
}