package com.gabedev.mangako.ui

object TestTags {
    const val ExploreNavigation = "navigation-explore"
    const val LibraryNavigation = "navigation-library"
    const val SettingsNavigation = "navigation-settings"
    const val ExploreSearch = "explore-search"
    const val CollectionSearch = "collection-search"
    const val AddToLibrary = "add-to-library"

    fun mangaSearchResult(id: String) = "manga-search-result-$id"
    fun mangaCard(id: String) = "manga-card-$id"
    fun volumeCard(id: String) = "volume-card-$id"
}
