package com.example.data

import android.content.Context
import android.content.SharedPreferences
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class CategoryPreferences(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("privafin_categories_prefs", Context.MODE_PRIVATE)
    private val moshi = Moshi.Builder().build()
    private val listType = Types.newParameterizedType(List::class.java, String::class.java)
    private val adapter = moshi.adapter<List<String>>(listType)

    private val KEY_APARTMENT_SUBCATEGORIES = "key_apartment_subcategories"

    private val defaultApartmentSubcategories = listOf(
        "Aluguel / Financiamento",
        "Condomínio",
        "Energia Elétrica",
        "Água e Esgoto",
        "Internet / Wi-Fi",
        "Gás",
        "IPTU",
        "Manutenção e Reformas",
        "Móveis e Eletros",
        "Limpeza e Casa"
    )

    private val _apartmentSubcategories = MutableStateFlow<List<String>>(loadApartmentSubcategories())
    val apartmentSubcategories: StateFlow<List<String>> = _apartmentSubcategories.asStateFlow()

    private fun loadApartmentSubcategories(): List<String> {
        val json = prefs.getString(KEY_APARTMENT_SUBCATEGORIES, null) ?: return defaultApartmentSubcategories
        return try {
            adapter.fromJson(json) ?: defaultApartmentSubcategories
        } catch (e: Exception) {
            defaultApartmentSubcategories
        }
    }

    private fun saveApartmentSubcategories(list: List<String>) {
        val cleanList = list.map { it.trim() }.filter { it.isNotBlank() }.distinct()
        try {
            val json = adapter.toJson(cleanList)
            prefs.edit().putString(KEY_APARTMENT_SUBCATEGORIES, json).apply()
            _apartmentSubcategories.value = cleanList
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun addApartmentSubcategory(name: String) {
        val current = _apartmentSubcategories.value.toMutableList()
        val trimmed = name.trim()
        if (trimmed.isNotBlank() && !current.any { it.equals(trimmed, ignoreCase = true) }) {
            current.add(trimmed)
            saveApartmentSubcategories(current)
        }
    }

    fun updateApartmentSubcategory(oldName: String, newName: String) {
        val current = _apartmentSubcategories.value.toMutableList()
        val trimmedNew = newName.trim()
        if (trimmedNew.isNotBlank()) {
            val index = current.indexOfFirst { it.equals(oldName, ignoreCase = true) }
            if (index != -1) {
                current[index] = trimmedNew
                saveApartmentSubcategories(current)
            }
        }
    }

    fun deleteApartmentSubcategory(name: String) {
        val current = _apartmentSubcategories.value.toMutableList()
        current.removeAll { it.equals(name, ignoreCase = true) }
        saveApartmentSubcategories(current)
    }

    fun resetToDefaults() {
        saveApartmentSubcategories(defaultApartmentSubcategories)
    }
}
