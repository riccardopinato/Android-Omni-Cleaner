package com.riccardopinato.omnicleaner.exif

enum class ExifGroup(
    val title: String,
    val tags: Set<String>,
) {
    LOCATION(
        title = "Posizione GPS",
        tags = setOf(
            "GPSVersionID",
            "GPSLatitudeRef",
            "GPSLatitude",
            "GPSLongitudeRef",
            "GPSLongitude",
            "GPSAltitudeRef",
            "GPSAltitude",
            "GPSTimeStamp",
            "GPSSatellites",
            "GPSStatus",
            "GPSMeasureMode",
            "GPSDOP",
            "GPSSpeedRef",
            "GPSSpeed",
            "GPSTrackRef",
            "GPSTrack",
            "GPSImgDirectionRef",
            "GPSImgDirection",
            "GPSMapDatum",
            "GPSDestLatitudeRef",
            "GPSDestLatitude",
            "GPSDestLongitudeRef",
            "GPSDestLongitude",
            "GPSDestBearingRef",
            "GPSDestBearing",
            "GPSDestDistanceRef",
            "GPSDestDistance",
            "GPSProcessingMethod",
            "GPSAreaInformation",
            "GPSDateStamp",
            "GPSDifferential",
            "GPSHPositioningError",
        ),
    ),
    DATES(
        title = "Date e orari EXIF",
        tags = setOf(
            "DateTime",
            "DateTimeOriginal",
            "DateTimeDigitized",
            "SubSecTime",
            "SubSecTimeOriginal",
            "SubSecTimeDigitized",
            "OffsetTime",
            "OffsetTimeOriginal",
            "OffsetTimeDigitized",
        ),
    ),
    DEVICE(
        title = "Dispositivo e autore",
        tags = setOf(
            "Make",
            "Model",
            "Software",
            "Artist",
            "Copyright",
            "CameraOwnerName",
            "BodySerialNumber",
            "LensMake",
            "LensModel",
            "LensSerialNumber",
            "ImageUniqueID",
        ),
    ),
    COMMENTS(
        title = "Commenti EXIF",
        tags = setOf(
            "UserComment",
            "ImageDescription",
        ),
    );

    companion object {
        fun selectedTags(groups: Set<ExifGroup>): Set<String> =
            groups.flatMapTo(LinkedHashSet()) { it.tags }
    }
}
