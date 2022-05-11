package com.akuleshov7.certification.schemas

import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.Serializable

@Serializable
data class ArrayCertification(
    val ints: List<Int>,
    val floats: List<Float>,
    val dates: List<LocalDateTime>,
    val comments: List<Int>,
    val strings: List<String>
)