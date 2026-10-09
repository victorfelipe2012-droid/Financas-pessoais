package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.CategoryPreferences
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class CategoryPreferencesTest {

    private lateinit var categoryPreferences: CategoryPreferences

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        categoryPreferences = CategoryPreferences(context)
        categoryPreferences.resetToDefaults()
    }

    @Test
    fun testDefaultSubcategoriesLoaded() {
        val list = categoryPreferences.apartmentSubcategories.value
        assertTrue(list.contains("Condomínio"))
        assertTrue(list.contains("Energia Elétrica"))
    }

    @Test
    fun testAddSubcategory() {
        categoryPreferences.addApartmentSubcategory("Vaga de Garagem")
        val list = categoryPreferences.apartmentSubcategories.value
        assertTrue(list.contains("Vaga de Garagem"))
    }

    @Test
    fun testUpdateSubcategory() {
        categoryPreferences.updateApartmentSubcategory("Condomínio", "Taxa Condominial Mensal")
        val list = categoryPreferences.apartmentSubcategories.value
        assertFalse(list.contains("Condomínio"))
        assertTrue(list.contains("Taxa Condominial Mensal"))
    }

    @Test
    fun testDeleteSubcategory() {
        categoryPreferences.deleteApartmentSubcategory("Gás")
        val list = categoryPreferences.apartmentSubcategories.value
        assertFalse(list.contains("Gás"))
    }
}
