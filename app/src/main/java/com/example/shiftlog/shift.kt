package com.example.shiftlog

data class Shift(
    val startTime: String = "",
    val endTime: String = "",
    val duration: Double = 0.0,
    val salary: Double = 0.0
)
