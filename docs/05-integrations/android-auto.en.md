# Android Auto and Automotive

CIMDriver includes explicit and native support for Android Auto and Android Automotive OS. The manifest contains the required metadata for this, a minimum car API level declaration, and an exported `CIMDriverCarAppService`. This enables the user to perform basic actions regarding their trip registration via the car's display.

## Car components

The codebase implements a separate, specific UI layer for in the car, driven by the `androidx.car.app` library. The flow starts with the `CIMDriverCarAppService` and `CIMDriverSession`, which control the underlying screens:
- `MainCarScreen`: The main menu and overview.
- `StartTripScreen` / `ReviewTripScreen`: Screens for starting or manually approving (classifying) a newly finished trip.
- `RecentTripsCarScreen`: A list of the most recently taken trips.
- `AddressBookCarScreen`: Viewing known, linked addresses (like clients or home locations) and the ability to start navigation directly to these points via the car's built-in navigation apps.

## Integration level

The combination of manifest metadata for projected Android Auto and built-in Android Automotive OS shows that the app is not limited to a single car mode. Thus, car integration is a fully-fledged product track and not an experimental side path. The logic of the Car app relies heavily on the same underlying classification utilities (Dynamics trip types) and Room database queries as the main app, ensuring a consistent experience between the smartphone and car interface.
