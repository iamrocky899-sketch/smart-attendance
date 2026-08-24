package com.attendancehalim.smartattendance.data.remote.dto

import com.google.gson.annotations.SerializedName

data class ApiResponseDto<T>(
    @SerializedName("success")
    val success: Boolean? = null,
    @SerializedName("isSuccess")
    val isSuccessVal: Boolean? = null,
    @SerializedName("status")
    val status: String? = null,
    @SerializedName("message")
    val message: String? = null,
    @SerializedName("errorCode")
    val errorCode: String? = null,
    @SerializedName("data")
    val data: T? = null
) {
    val isSuccess: Boolean
        get() = success == true || isSuccessVal == true || status.equals("success", ignoreCase = true)
}
