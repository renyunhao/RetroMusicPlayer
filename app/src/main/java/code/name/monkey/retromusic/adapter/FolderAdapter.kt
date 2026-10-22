package code.name.monkey.retromusic.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import code.name.monkey.retromusic.R
import code.name.monkey.retromusic.databinding.ItemFolderBinding
import code.name.monkey.retromusic.model.Folder

class FolderAdapter(
    private var folders: List<Folder>,
    private val onFolderClickListener: (Folder) -> Unit
) : RecyclerView.Adapter<FolderAdapter.FolderViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FolderViewHolder {
        val binding = ItemFolderBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return FolderViewHolder(binding)
    }

    override fun onBindViewHolder(holder: FolderViewHolder, position: Int) {
        holder.bind(folders[position])
    }

    override fun getItemCount(): Int = folders.size

    fun swapData(newFolders: List<Folder>) {
        folders = newFolders
        notifyDataSetChanged()
    }

    inner class FolderViewHolder(
        private val binding: ItemFolderBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        init {
            binding.root.setOnClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onFolderClickListener(folders[position])
                }
            }
        }

        fun bind(folder: Folder) {
            binding.folderName.text = folder.name.ifBlank {
                binding.root.context.getString(R.string.folders)
            }
            binding.folderPath.text = folder.path
            binding.songCount.text = binding.root.context.resources.getQuantityString(
                R.plurals.song_plural, folder.songCount, folder.songCount
            )
        }
    }
}