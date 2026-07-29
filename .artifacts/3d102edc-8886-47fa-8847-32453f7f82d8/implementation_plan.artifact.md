# Implementation Plan - Collectible Images

This plan adds the ability to attach a picture to each collectible in your collection.

## Proposed Changes

### 1. Data Layer (Persistence)

#### [MODIFY] [Collectible.kt](file:///C:/Users/gille/OneDrive/Desktop/GitHub/CollectorsTrackingApp/app/src/main/java/com/example/collectorstrackingapp/model/Collectible.kt)
- Add an optional `imagePath: String?` field to store the local URI or file path of the image.

#### [MODIFY] [AppDatabase.kt](file:///C:/Users/gille/OneDrive/Desktop/GitHub/CollectorsTrackingApp/app/src/main/java/com/example/collectorstrackingapp/data/AppDatabase.kt)
- Increment the database version to `3`.
- The existing `fallbackToDestructiveMigration()` will handle the schema change for now (note: this will wipe existing data during this update).

---

### 2. UI - Image Selection (Add/Edit Screen)

#### [MODIFY] [fragment_second.xml](file:///C:/Users/gille/OneDrive/Desktop/GitHub/CollectorsTrackingApp/app/src/main/res/layout/fragment_second.xml)
- Add an `ImageView` at the top to preview the selected picture.
- Add a "Choose Image" `Button` or click listener on the `ImageView`.

#### [MODIFY] [SecondFragment.kt](file:///C:/Users/gille/OneDrive/Desktop/GitHub/CollectorsTrackingApp/app/src/main/java/com/example/collectorstrackingapp/SecondFragment.kt)
- Implement `ActivityResultLauncher` using `GetContent()` to pick an image from the gallery.
- Save the selected image URI to the `imagePath` variable.
- Update `saveCollectible()` to include the image path.
- Handle displaying the image if editing an existing collectible.

---

### 3. UI - Displaying Images (List & Tiles)

#### [MODIFY] [item_collectible.xml](file:///C:/Users/gille/OneDrive/Desktop/GitHub/CollectorsTrackingApp/app/src/main/res/layout/item_collectible.xml)
- Add an `ImageView` to the card layout (positioned at the top or side).

#### [MODIFY] [CollectibleAdapter.kt](file:///C:/Users/gille/OneDrive/Desktop/GitHub/CollectorsTrackingApp/app/src/main/java/com/example/collectorstrackingapp/CollectibleAdapter.kt)
- Update the `bind` logic to load the image from the `imagePath` if it exists.
- I will use the **Coil** library for efficient image loading.

#### [MODIFY] [build.gradle.kts](file:///C:/Users/gille/OneDrive/Desktop/GitHub/CollectorsTrackingApp/app/build.gradle.kts)
- Add the Coil dependency: `implementation("io.coil-kt:coil:2.4.0")`.

## Verification Plan

### Manual Verification
1. **Add Item**: Click (+), choose an image from the gallery, fill in details, and save.
2. **Verify List**: Ensure the image appears in the main collection list.
3. **Verify Gallery**: Go to Settings -> Category and ensure the image appears in the 2-column grid.
4. **Edit Item**: Click an item with an image, verify the preview loads, change the image, and save.
5. **Delete Item**: Ensure deleting an item works correctly with the image.

> [!CAUTION]
> Because I am updating the database schema, your existing test items will be cleared during this update.
