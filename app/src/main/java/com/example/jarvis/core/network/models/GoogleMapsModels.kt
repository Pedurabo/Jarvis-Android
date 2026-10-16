package com.example.jarvis.core.network.models

import com.google.gson.annotations.SerializedName

data class DirectionsResponse(
    @SerializedName("routes") val routes: List<Route>,
    @SerializedName("status") val status: String,
    @SerializedName("error_message") val errorMessage: String?
)

data class Route(
    @SerializedName("legs") val legs: List<Leg>,
    @SerializedName("summary") val summary: String,
    @SerializedName("warnings") val warnings: List<String>
)

data class Leg(
    @SerializedName("duration") val duration: TextValue,
    @SerializedName("duration_in_traffic") val durationInTraffic: TextValue?,
    @SerializedName("distance") val distance: TextValue,
    @SerializedName("start_address") val startAddress: String,
    @SerializedName("end_address") val endAddress: String,
    @SerializedName("steps") val steps: List<Step>
)

data class Step(
    @SerializedName("html_instructions") val htmlInstructions: String,
    @SerializedName("distance") val distance: TextValue,
    @SerializedName("duration") val duration: TextValue
)

data class TextValue(
    @SerializedName("text") val text: String,
    @SerializedName("value") val value: Int // value in seconds or meters
)
