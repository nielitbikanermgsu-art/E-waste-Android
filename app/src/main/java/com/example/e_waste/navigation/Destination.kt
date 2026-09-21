package com.example.e_waste.navigation



import android.net.Uri

/**
 * Single source of truth for every route in the app. Screens never hardcode route strings —
 * they navigate via these functions/constants, so a route change only touches this file.
 */
object Destinations {
    const val HOME = "home"
    const val CAPTURE = "capture"

    // Result needs the captured photo's content URI to kick off the upload.
    const val RESULT_ARG_PHOTO_URI = "photoUri"
    const val RESULT_ROUTE = "result/{$RESULT_ARG_PHOTO_URI}"
    fun resultRoute(photoUri: String) = "result/${Uri.encode(photoUri)}"

    // Handover needs the detection session id produced by the Result screen, not the raw
    // detection payload — large objects don't belong in nav args.
    const val HANDOVER_ARG_SESSION_ID = "sessionId"
    const val HANDOVER_ROUTE = "handover/{$HANDOVER_ARG_SESSION_ID}"
    fun handoverRoute(sessionId: String) = "handover/$sessionId"

    const val HISTORY = "history"
}