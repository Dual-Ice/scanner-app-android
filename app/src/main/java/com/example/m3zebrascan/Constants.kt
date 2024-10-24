package com.example.m3zebrascan

object Actions {
    const val ACTION_TYPE_RECEIVING = "receiving"
    const val ACTION_TYPE_PICKING = "picking"
    const val ACTION_TYPE_CHECKING = "checking"
    const val ACTION_TYPE_INVENTORY = "inventory"

    fun getActionName(actionType: String?): String {
        return when (actionType) {
            ACTION_TYPE_RECEIVING -> "Приёмка"
            ACTION_TYPE_PICKING -> "Отбор"
            ACTION_TYPE_CHECKING -> "Контроль"
            ACTION_TYPE_INVENTORY -> "Инвентаризация"
            else -> "Действие" // Значение по умолчанию
        }
    }
}