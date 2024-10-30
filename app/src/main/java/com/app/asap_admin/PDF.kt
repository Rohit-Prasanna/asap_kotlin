package com.app.asap_admin

data class PDF(
    var userid: String? = null,
    var filePath: String? = null,
    var fileName: String? = null,
    var location: String? = null,
    var timestamp: Any? = null
) {
    // Default constructor required for calls to DataSnapshot.getValue(PDF::class.java)
}
