# Walkthrough - Search and Filter Functionality

I have added a dedicated search and filter screen to help you find collectibles in your collection more easily.

## Changes Made

### Data Layer
- Added `searchCollectibles(query, category)` to the [DAO](file:///C:/Users/gille/OneDrive/Desktop/GitHub/CollectorsTrackingApp/app/src/main/java/com/example/collectorstrackingapp/data/CollectibleDao.kt) and exposed it through the [Repository](file:///C:/Users/gille/OneDrive/Desktop/GitHub/CollectorsTrackingApp/app/src/main/java/com/example/collectorstrackingapp/data/CollectibleRepository.kt) and [ViewModel](file:///C:/Users/gille/OneDrive/Desktop/GitHub/CollectorsTrackingApp/app/src/main/java/com/example/collectorstrackingapp/viewmodel/CollectibleViewModel.kt).
- This allows real-time filtering as you type or change the category.

### Navigation & UI
- Created [SearchFragment](file:///C:/Users/gille/OneDrive/Desktop/GitHub/CollectorsTrackingApp/app/src/main/java/com/example/collectorstrackingapp/SearchFragment.kt) and its [layout](file:///C:/Users/gille/OneDrive/Desktop/GitHub/CollectorsTrackingApp/app/src/main/res/layout/fragment_search.xml).
- Added a **Search Icon** to the top toolbar of the home screen.
- Updated [MainActivity](file:///C:/Users/gille/OneDrive/Desktop/GitHub/CollectorsTrackingApp/app/src/main/java/com/example/collectorstrackingapp/MainActivity.kt) to handle navigation to the search screen.

### Search Features
- **Live Search**: The results list updates instantly as you type in the search bar.
- **Category Filter**: A dropdown menu allows you to filter results by a specific category or view "All" (default).
- **Interactive Results**: Just like the main list, you can click any search result to go directly to its "Edit" screen.

## How to Test
1. **Open the app** to your main collection list.
2. **Tap the Search icon** (magnifying glass) in the top right corner.
3. **Search by Name**: Start typing the name of an item.
4. **Filter by Category**: Tap the category dropdown to select a specific category.
5. **Combined Filter**: Try searching for a specific name *within* a specific category.
6. **Go to Edit**: Click on a result to verify it opens the edit page for that specific item.

> [!TIP]
> The search is case-insensitive, so "COIN" will find "coin" or "Coin".
