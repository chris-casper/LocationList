# LocationList

An Android app for saving, organizing, and viewing named locations — like a contact list, but for places. 

Each location can carry GPS coordinates, a description, notes, groups, tags, and photos, and the whole collection can be viewed on a map or imported/exported as KML/KMZ.

Built with Kotlin and Jetpack Compose. No API keys or accounts required — the map uses OpenStreetMap. OSM is the only cloud dependency. 

Only permissions are location based... because that's kinda the point of the app. It does still use Google Play Services, for ease of development. In some future version, I'll try to work around that or make it optional.

Should work for any Android higher than 7.

## Features

- **Save locations** with name, GPS coordinates (auto-filled from your current position or entered manually), description, notes, groups, tags, and photos from the camera or gallery.
- **Browse and search** all saved locations, filtering across names, descriptions, notes, groups, and tags.
- **Detail / edit screen** for changing any field, managing photos, and deleting.
- **Open in Maps** — hands a location's coordinates to your device's default map/navigation app.
- **Map view** showing every saved location as a pin, powered by OpenStreetMap (osmdroid).
- **Import** placemarks from KML or KMZ files; folders become groups, and you can tag the whole import with a group.
- **Export / share** your locations as a KML file via the Android share sheet or saved to a file of your choice.

## Tech stack

- **Language / UI:** Kotlin, Jetpack Compose (Material 3)
- **Database:** Room (with KSP)
- **Navigation:** Navigation Compose
- **Images:** Coil 3, with a FileProvider for camera capture
- **Location:** FusedLocationProviderClient (Google Play Services Location)
- **Maps:** osmdroid (OpenStreetMap)

## Requirements

- A recent version of Android Studio (this project uses Android Gradle Plugin 9).
- Android SDK with a minimum SDK of 24 (Android 7.0); target SDK 36.

The Gradle wrapper is included, so you don't need to install Gradle separately.

## Building and running

1. Clone the repository:
   ```
   git clone https://github.com/chris-casper/LocationList.git
   ```
2. Open the project in Android Studio and let it sync (this downloads dependencies the first time).
3. Create or select an emulator with Google Play Services, or connect a physical device with USB debugging enabled.
4. Press **Run**.

For a real-world test, a physical device is recommended — GPS, camera, and live map tiles all behave more naturally than on an emulator.

## Permissions

- `ACCESS_FINE_LOCATION` / `ACCESS_COARSE_LOCATION` — to fill in the current location when saving.
- `INTERNET` / `ACCESS_NETWORK_STATE` — to download map tiles. Should ONLY connect to OSM. Does not phone home.

The camera and gallery use the system pickers, so no camera or storage permission is required.

## Project structure

```
im.casper.locationlist
├── MainActivity.kt
├── data            # Room entity, DAO, database, type converters, KML import/export
├── navigation      # routes and the navigation graph
├── ui
│   ├── home        # home menu screen
│   ├── screens     # create, list, detail/edit, map, share, settings
│   └── theme       # generated Compose theme
└── util            # image storage helper (FileProvider + file copy)
```

## Roadmap / possible enhancements

- Read `<ExtendedData>` on import for lossless KML round-trips (notes and tags). I haven't done KML trips yet.
- Tap a map marker to open that location's detail screen. Just being lazy. 
- De-duplicate on import rather than adding copies. Also lazy.
- Import photos embedded in KMZ archives. Also lazy. 

## License

Released under the MIT License. See [LICENSE](LICENSE) for details.
