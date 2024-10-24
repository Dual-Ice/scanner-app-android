package com.example.m3zebrascan.Inventory

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.m3zebrascan.R

class InventoryItemsAdapter (private val items: List<InventoryItem>) :
    RecyclerView.Adapter<InventoryItemsAdapter.ItemViewHolder>() {

    class ItemViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val codeTextView: TextView = view.findViewById(R.id.itemCodeTextView)
        val quantityTextView: TextView = view.findViewById(R.id.itemQuantityTextView)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.inventory_item_row, parent, false)
        return ItemViewHolder(view)
    }

    override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
        val item = items[position]
        holder.codeTextView.text = item.code
        holder.quantityTextView.text = item.quantity.toString()
    }

    override fun getItemCount() = items.size
}
