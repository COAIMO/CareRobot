package com.samin.carerobot.Nuri

enum class MovementMode(val byte: Byte) {
    GO_forward(0x01),
    GO_backward(0x02),
    TURN_Left(0x03),
    TURN_Right(0x04),
    UP_Lift(0x05),
    DOWN_Lift(0x06),
    LEFT_SHOULDER_UP(0x07),
    LEFT_SHOULDER_DOWN(0x08),
    RIGHT_SHOULDER_UP(0x09),
    RIGHT_SHOULDER_DOWN(0x0A),
}