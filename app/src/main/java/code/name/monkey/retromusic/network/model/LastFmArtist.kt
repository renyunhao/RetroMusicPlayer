package code.name.monkey.retromusic.network.model

class LastFmArtist {

    var artist: Artist? = null

    class Artist {
        var stats: Stats = Stats()
        var bio: Bio? = null
        var image: List<Image> = ArrayList()

        class Image {
            var text: String? = null
            var size: String? = null
        }

        class Stats {
            var listeners: String = ""
            var playcount: String = ""
        }

        class Bio {
            var content: String? = null
        }
    }
}