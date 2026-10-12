package com.riccardopinato.omnicleaner.exif

enum class ExifGroup(
    val tags: Set<String>,
) {
    LOCATION(
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
