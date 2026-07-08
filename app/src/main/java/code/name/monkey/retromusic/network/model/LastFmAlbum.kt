package code.name.monkey.retromusic.network.model

class LastFmAlbum {

    var album: Album? = null

    class Album {
        var listeners: String = ""
        var playcount: String = ""
        var image: List<Image> = ArrayList()
        var name: String? = null
        var tags: Tags? = null
        var wiki: Wiki? = null

        class Image {
            var text: String? = null
            var size: String? = null
        }

        class Tags {
            var tag: List<Tag>? = null
        }

        class Tag {
            var name: String? = null
            var url: String? = null
        }

        class Wiki {
            var content: String? = null
            var published: String? = null
        }
    }
}