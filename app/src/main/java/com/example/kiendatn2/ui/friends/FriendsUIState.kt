package com.example.kiendatn2.ui.friends

data class FriendsUIState(
    val friendsState: FriendsState = FriendsState.Loading,
    val pendingRequestsState: FriendsState = FriendsState.Loading,
    val sentRequestsState: FriendsState = FriendsState.Loading,
    val searchState: SearchState = SearchState.Initial,
    val acceptingIds: Set<String> = emptySet(),
    val decliningIds: Set<String> = emptySet(),
    val cancelingIds: Set<String> = emptySet()
)