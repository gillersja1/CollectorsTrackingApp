# Walkthrough - Collectible Images (Optional)

I have successfully added the ability to add pictures to your collectibles. Pictures are fully optional, so you can still save items with just text.

## Changes Made

### Data Layer
- Updated the [Collectible](file:///C:/Users/gille/OneDrive/Desktop/GitHub/CollectorsTrackingApp/app/src/main/java/com/example/collectorstrackingapp/model/Collectible.kt) entity to include an optional `imagePath` field.
- Incremented the database version to `3`.

### UI / Image Picker
- Added an **Image Picker** to the [Add/Edit screen](file:///C:/Users/gille/OneDrive/Desktop/GitHub/CollectorsTrackingApp/app/src/main/java/com/example/collectorstrackingapp/SecondFragment.kt). You can now tap "Choose Image" to pick a photo from your gallery.
- Added an **Image Preview** at the top of the form so you can see the selected photo before saving.
- Integrated the **Coil** library for high-performance image loading and caching.

### Display
- Both the **Main Collection List** and the **Category Galleries** now display the item's picture if one was provided.
- If no picture is chosen, the app displays a default gallery icon.

## How to Test
1. **Add an Item with Image**:
    - Click (+) on the home screen.
    - Click **Choose Image (Optional)** and select a photo.
    - Enter a name and category, then click **Save**.
    - See the photo appear in your collection list!
2. **Add an Item without Image**:
    - Click (+) and just enter text.
    - Save and see the default icon used instead.
3. **View in Gallery**:
    - Go to Settings -> Select a Category.
    - See your photos displayed in the 2-column grid.

> [!IMPORTANT]
> The app now copies selected images to its own private storage. This ensures the pictures remain visible even if you delete the original photo from your phone's gallery!
