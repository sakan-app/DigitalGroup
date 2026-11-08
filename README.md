# DigitalGroup

## App structure
- `app/src/main/java/ai/digitalgroup/app/MainActivity.kt`: Compose UI, brand switching, department catalogue, product configuration, summary, shared local cart, QR generation and allowlisted store WebView.
- `app/src/main/AndroidManifest.xml`: launcher activity and network permission.
- `app/build.gradle.kts`: Android, Kotlin, Compose and ZXing dependencies.

The native product cards are deliberately templates: live product records and authoritative prices are not fabricated. Selecting a store opens that store's official domain; payment and inventory remain on the merchant site. The local cart is a device-side selection basket and is not yet synchronized with the stores' server-side carts.
