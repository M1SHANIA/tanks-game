package com.example.hra_tanks.models

import android.view.View
import com.example.hra_tanks.enums.Direction

data class Bullet(
    val view: View,
    val direction: Direction,
    val tank: Tank,
    var canMoveFurther: Boolean = true
)
