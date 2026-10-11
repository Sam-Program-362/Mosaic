package com.samprogram.mosaic.core.model

/**
 * Why reading a selected folder failed.
 *
 * Reasons are stored instead of messages so the data layer stays free of
 * user-facing text and the UI can resolve each reason to a localized string.
 */
enum class ScanFailure {
    /** No persisted read grant is held; the user must select the folder again. */
    ACCESS_NOT_GRANTED,

    /** The provider denied the query despite a recorded grant. */
    ACCESS_DENIED,

    /** The folder document no longer exists at the provider. */
    MISSING_DOCUMENT,

    /** The provider returned no usable result. */
    PROVIDER_ERROR,

    /** An unexpected I/O failure occurred while enumerating. */
    IO_ERROR,
}
