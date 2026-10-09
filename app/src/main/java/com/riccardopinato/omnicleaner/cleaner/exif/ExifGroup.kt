package com.riccardopinato.omnicleaner.cleaner.exif

enum class ExifGroup(
    val title: String,
    val tags: Set<String>,
) {
    LOCATION(
        "Posizione",
        setOf(
            "GPSLatitude",
            "GPSLatitudeRef",
            "GPSLongitude",
            "GPSLongitudeRef",
            "GPSAltitude",
            "GPSAltitudeRef",
            "GPSTimeStamp",
            "GPSDateStamp",
            "GPSProcessingMethod",
            "GPSSpeed",
            "GPSSpeedRef",
            "GPSImgDirection",
            "GPSImgDirectionRef",
        ),
    ),
    DEVICE(
        "Dispositivo",
        setOf(
            "Make",
            "Model",
            "Software",
            "LensModel",
            "BodySerialNumber",
            "CameraOwnerName",
            "LensMake",
            "LensSerialNumber",
        ),
    ),
    DATES(
        "Date",
        setOf(
            "DateTime",
            "DateTimeOriginal",
            "DateTimeDigitized",
            "OffsetTime",
            "OffsetTimeOriginal",
            "OffsetTimeDigitized",
            "SubSecTime",
            "SubSecTimeOriginal",
            "SubSecTimeDigitized",
        ),
    ),
    COMMENTS(
        "Commenti",
        setOf(
            "UserComment",
            "ImageDescription",
            "Artist",
            "Copyright",
            "XPComment",
            "XPAuthor",
            "XPKeywords",
            "XPSubject",
            "XPTitle",
        ),
    );

    companion object {
        fun allTags(groups: Set<ExifGroup>): Set<String> = groups.flatMapTo(linkedSetOf()) { it.tags }
    }
}
