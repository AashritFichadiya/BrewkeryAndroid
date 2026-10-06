# Brewkery Android

A native coffee and bakery ordering app built in Kotlin with Jetpack Compose for the Clickretina Android Developer assignment.

## Features

- **Home / menu:** store information, active-order tracking, category filters, search, ratings, menu badges, and product images.
- **Item details:** ingredients, preparation information, sizes, milk or toppings, sweetness choices, and live price recalculation.
- **Cart:** editable quantities, Clear Cart, subtotal, delivery fee, estimated tax, and total payable.
- **Order status:** generates a ticket ID, clears the cart, and shows the `PREPARING` status. The home screen then shows an active-order banner with a Track action.
- **Network states:** loading and error feedback for API requests. Product photos load from their supplied URLs with Coil.

The cart is in memory and resets when the app process closes. The provided API is read-only, so placing an order is simulated locally and does not send an order to a backend.

## Tech stack

- Kotlin, Jetpack Compose, Material 3
- Retrofit and Gson for the REST API
- Kotlin coroutines and `ViewModel` for asynchronous loading and screen state
- Coil for remote images
- JUnit for the cart pricing unit test

## API

- Menu, store details, categories, and products: `https://raw.githubusercontent.com/VivekShah138/Brewkery/main/data.json`
- Product details: `https://raw.githubusercontent.com/VivekShah138/Brewkery/main/api/items/{id}.json` (IDs 1–6)

## Build and run

Open this directory in Android Studio and run the `app` configuration. The project uses `minSdk 24` and `targetSdk 35`; use a JDK supported by the configured Android Gradle Plugin (JDK 17 or newer).

On Windows:

```powershell
.\gradlew.bat assembleDebug
```

Run the unit test:

```powershell
.\gradlew.bat testDebugUnitTest
```

The debug APK is generated at `app/build/outputs/apk/debug/app-debug.apk`.

## Unit test

`CartLineTest.customizationsAreIncludedInUnitPrice` checks that the base price, size surcharge, and milk surcharge are included in a cart line's unit price.

## AI use

**AI tool used:** OpenAI Codex.

**Prompts used in this task:**

1. “in the cart list screen i can not able to see the cart's above digit”
2. “when in home list screen then the label should show not when in cart screen”
3. “also add clear cart and all icon should have rounded card”

The assignment brief and prototype screenshots were also provided as context for implementing and refining the screens.

**One thing AI got right:** It mapped the supplied customization data into Kotlin models and built the menu-to-detail-to-cart flow around the provided API.

**One bug caught and fixed:** The first item-detail success handler used nested implicit `it` values. The inner state-update lambda shadowed the fetched item, so the wrong value was assigned to `selected`. Naming the values `fetched` and `state` made the state update use the API result correctly.
