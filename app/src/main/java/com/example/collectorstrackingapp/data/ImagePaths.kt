package com.example.collectorstrackingapp.data

/**
 * Tells photos that live only on this phone apart from ones already in the cloud.
 */
object ImagePaths {
    /**
     * True for a photo that hasn't been uploaded yet: a content:// or file:// URI, or an
     * absolute path into the app's own storage (how the item screen saves picked photos).
     * Those don't survive a reinstall or reach other devices, so they need uploading.
     */
    fun isLocal(path: String?): Boolean =
        !path.isNullOrBlank() &&
            (path.startsWith("content://") || path.startsWith("file://") || path.startsWith("/"))
}
