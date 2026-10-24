# Master Sahib Location Changer

A native Android app that keeps the phone's mock GPS location fixed at coordinates entered by the user.

The app also includes a MapLibre vector map. Move the map under the center pin to select a location visually, or enter coordinates and tap **Center map on coordinates**.

## Open and build

1. Install Android Studio with Android SDK 36 and JDK 17.
2. Open this project folder in Android Studio.
3. Let Gradle sync, then run the `app` configuration on an Android 8.0+ phone.

## Phone setup (new users)

1. Phone par **Settings → About phone** kholen.
2. **Build number** par 7 dafa tap karen. PIN mangay to enter karen. Is se Developer Options on ho jayega.
3. App kholen aur **Open Developer Options** dabayen.
4. Developer Options mein **Select mock location app** (ya **Mock location app**) dhoonden.
5. List se **Master Sahib Location Changer** select karen.
6. App mein wapas aakar latitude aur longitude enter karen.
7. **Fix Location** dabayen aur permissions allow karen. Location ko active rakhne ke liye notification ko chalne dein.
8. Asli GPS par wapas jane ke liye **Stop Location** dabayen.

Samsung, Xiaomi, Oppo aur doosre phones par setting ka naam ya menu position thori mukhtalif ho sakti hai. Settings search mein `mock location` likhna asaan tareeqa hai.

Use **Stop Location** or the notification action to restore normal GPS operation.

This app uses Android's official developer mock-location facility. It does not conceal mock-location status or bypass another app's anti-spoofing checks.
