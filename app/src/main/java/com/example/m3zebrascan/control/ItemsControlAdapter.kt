package com.example.m3zebrascan.control

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.m3zebrascan.Item
import com.example.m3zebrascan.R

class ItemsControlAdapter(
    private var items: List<Item>,
    private val onItemClicked: (Item) -> Unit
) : RecyclerView.Adapter<ItemsControlAdapter.ItemViewHolder>() {

    class ItemViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val nameTextView: TextView = view.findViewById(R.id.itemNameTextView)
        val codeTextView: TextView = view.findViewById(R.id.itemCodeTextView)
        val quantityTextView: TextView = view.findViewById(R.id.itemQuantityTextView)
        val scannedTextView: TextView = view.findViewById(R.id.itemScannedTextView)
        val commentIcon: ImageView = view.findViewById(R.id.commentIcon)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_row_control, parent, false)
        return ItemViewHolder(view)
    }

    override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
        val item = items[position]
        holder.nameTextView.text = item.name
        holder.codeTextView.text = item.code
        holder.quantityTextView.text = item.scanned.toString()
        holder.scannedTextView.text = item.control.toString()

        if (item.control == item.quantity) {
            holder.itemView.setBackgroundColor(Color.GREEN)
        } else {
            holder.itemView.setBackgroundColor(Color.TRANSPARENT)
        }

        if (item.comment.isNotBlank()) {
            holder.commentIcon.visibility = View.VISIBLE
        } else {
            holder.commentIcon.visibility = View.GONE
        }

        holder.itemView.setOnClickListener {
            onItemClicked(item)
        }
    }

    fun updateItems(newItems: List<Item>) {
        this.items = newItems.sortedByDescending { it.updatedAt }
        this.notifyDataSetChanged()
    }

    override fun getItemCount() = items.size
}
