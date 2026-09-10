package com.veloce.domain.model

sealed interface TrackingFailure {
    data object StorageFull : TrackingFailure
    data object StorageUnavailable : TrackingFailure
    data object StorageCorrupt : TrackingFailure
    data object StorageBusy : TrackingFailure
    data object InvalidData : TrackingFailure
    data object PermissionDenied : TrackingFailure
    data object LocationUnavailable : TrackingFailure
    data object CaptureOverflow : TrackingFailure
    data object Unexpected : TrackingFailure
}

sealed interface Outcome<out T> {
    data class Success<T>(val value: T) : Outcome<T>
    data class Failure(val error: TrackingFailure) : Outcome<Nothing>
}
