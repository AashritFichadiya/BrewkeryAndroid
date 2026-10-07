package com.clickretina.brewkery

import com.clickretina.brewkery.data.*
import org.junit.Assert.assertEquals
import org.junit.Test

class CartLineTest {
    @Test fun customizationsAreIncludedInUnitPrice() {
        val item = MenuItem(id = 1, name = "Coffee", base_price = 4.85)
        val line = CartLine(item, SizeOption("large", "Large", 1.25), "Light", MilkOption("almond", "Almond", .50))
        assertEquals(6.60, line.unitPrice, .001)
    }
}
