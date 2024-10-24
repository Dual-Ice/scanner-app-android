package com.example.m3zebrascan

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class ItemsAdapter(private val items: List<Item>) :
    RecyclerView.Adapter<ItemsAdapter.ItemViewHolder>() {

    class ItemViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val nameTextView: TextView = view.findViewById(R.id.itemNameTextView)
        val codeTextView: TextView = view.findViewById(R.id.itemCodeTextView)
        val quantityTextView: TextView = view.findViewById(R.id.itemQuantityTextView)
        val scannedTextView: TextView = view.findViewById(R.id.itemScannedTextView)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_row, parent, false)
        return ItemViewHolder(view)
    }

    override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
        val item = items[position]
        holder.nameTextView.text = item.name
        holder.codeTextView.text = item.code
        holder.quantityTextView.text = item.quantity.toString()
        holder.scannedTextView.text = item.scanned.toString()
    }

    override fun getItemCount() = items.size
}
