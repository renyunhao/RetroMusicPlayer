package code.name.monkey.retromusic.fragments.home

import code.name.monkey.retromusic.databinding.FragmentHomeBinding

class HomeBinding(
    homeBinding: FragmentHomeBinding
) {
    val root = homeBinding.root
    val container = homeBinding.container
    val contentContainer = homeBinding.contentContainer
    val appBarLayout = homeBinding.appBarLayout
    val toolbar = homeBinding.appBarLayout.toolbar
    val lastAdded = homeBinding.homeContent.absPlaylists.lastAdded
    val topPlayed = homeBinding.homeContent.absPlaylists.topPlayed
    val actionShuffle = homeBinding.homeContent.absPlaylists.actionShuffle
    val actionScan = homeBinding.homeContent.absPlaylists.actionScan
    val history = homeBinding.homeContent.absPlaylists.history
    val recyclerView = homeBinding.homeContent.recyclerView
    val suggestions = homeBinding.homeContent.suggestions
    val scanProgressCard = homeBinding.homeContent.scanProgressCard
    val scanProgressBar = homeBinding.homeContent.scanProgressBar
    val scanProgressText = homeBinding.homeContent.scanProgressText

}