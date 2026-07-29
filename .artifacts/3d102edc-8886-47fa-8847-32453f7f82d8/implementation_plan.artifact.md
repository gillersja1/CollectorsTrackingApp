# Implementation Plan - Search and Filter Collectibles

This plan adds a dedicated search page where users can search for collectibles by name and filter them by category.

## Proposed Changes

### 1. Data Layer (Search & Filter)

#### [MODIFY] [CollectibleDao.kt](file:///C:/Users/gille/OneDrive/Desktop/GitHub/CollectorsTrackingApp/app/src/main/java/com/example/collectorstrackingapp/data/CollectibleDao.kt)
- Add a search query method:
  ```kotlin
  @Query("SELECT * FROM collectibles WHERE (:query IS NULL OR name LIKE '%' || :query || '%') AND (:category IS NULL OR :category = 'All' OR category = :category) ORDER BY timestamp DESC")
  fun searchCollectibles(query: String?, category: String?): Flow<List<Collectible>>
  ```

#### [MODIFY] [CollectibleRepository.kt](file:///C:/Users/gille/OneDrive/Desktop/GitHub/CollectorsTrackingApp/app/src/main/java/com/example/collectorstrackingapp/data/CollectibleRepository.kt)
- Expose `searchCollectibles(query, category)` from the DAO.

#### [MODIFY] [CollectibleViewModel.kt](file:///C:/Users/gille/OneDrive/Desktop/GitHub/CollectorsTrackingApp/app/src/main/java/com/example/collectorstrackingapp/viewmodel/CollectibleViewModel.kt)
- Add a method to perform searches: `searchCollectibles(query: String?, category: String?): LiveData<List<Collectible>>`.

---

### 2. User Interface (Search Screen)

#### [NEW] [fragment_search.xml](file:///C:/Users/gille/OneDrive/Desktop/GitHub/CollectorsTrackingApp/app/src/main/res/layout/fragment_search.xml)
- Layout featuring:
    - A `SearchView` or `EditText` for the name search.
    - A `Spinner` or `AutoCompleteTextView` (dropdown) for category filtering (including an "All" option).
    - A `RecyclerView` to display results (using existing `item_collectible.xml`).

#### [NEW] [SearchFragment.kt](file:///C:/Users/gille/OneDrive/Desktop/GitHub/CollectorsTrackingApp/app/src/main/java/com/example/collectorstrackingapp/SearchFragment.kt)
- Logic to:
    - Initialize the category dropdown with "All" + existing categories.
    - Observe search parameters and update the results list.
    - Handle clicks on items to navigate to the Edit screen (reusing `collectibleId` bundle logic).

---

### 3. Navigation & Menu

#### [MODIFY] [nav_graph.xml](file:///C:/Users/gille/OneDrive/Desktop/GitHub/CollectorsTrackingApp/app/src/main/res/navigation/nav_graph.xml)
- Add `SearchFragment` as a destination.
- Add an action from `FirstFragment` to `SearchFragment`.

#### [MODIFY] [menu_main.xml](file:///C:/Users/gille/OneDrive/Desktop/GitHub/CollectorsTrackingApp/app/src/main/res/menu/menu_main.xml)
- Add a search icon item:
  ```xml
  <item
      android:id="@+id/action_search"
      android:icon="@android:drawable/ic_menu_search"
      android:title="Search"
      app:showAsAction="ifRoom" />
  ```

#### [MODIFY] [MainActivity.kt](file:///C:/Users/gille/OneDrive/Desktop/GitHub/CollectorsTrackingApp/app/src/main/java/com/example/collectorstrackingapp/MainActivity.kt)
- Handle the `action_search` click to navigate to the `SearchFragment`.

## Verification Plan

### Manual Verification
1. **Open Search**: Tap the search icon in the toolbar.
2. **Search by Name**: Type a partial name (e.g., "Coin") and verify only matching items appear.
3. **Filter by Category**: Select a category from the dropdown and verify only items in that category appear.
4. **Combine Both**: Search by name AND filter by category simultaneously.
5. **Clear Filters**: Select "All" and clear text to see the full list again.
6. **Navigate from Search**: Click a search result and verify it opens the Edit screen.
