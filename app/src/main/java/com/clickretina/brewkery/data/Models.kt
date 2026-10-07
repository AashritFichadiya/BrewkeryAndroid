package com.clickretina.brewkery.data

data class MenuResponse(val meta: StoreMeta = StoreMeta(), val categories: List<Category> = emptyList(), val items: List<MenuItem> = emptyList())
data class StoreMeta(val delivery_fee: Double = 2.5, val tax_rate_percent: Double = 8.0, val estimated_delivery_time: String = "20 - 30 mins")
data class Category(val id: String, val name: String, val icon: String = "")
data class MenuItem(
    val id: Int,
    val category_id: String = "",
    val name: String,
    val tagline: String = "",
    val description: String = "",
    val base_price: Double,
    val rating: Double = 0.0,
    val review_count: Int = 0,
    val prep_time: String = "",
    val calories: Int = 0,
    val image_url: String = "",
    val badge: String = "",
    val ingredients: List<String> = emptyList(),
    val customizations: Customizations = Customizations()
)
data class Customizations(val sizes: List<SizeOption> = emptyList(), val sugar_levels: List<String> = emptyList(), val milk_options: List<MilkOption> = emptyList())
data class SizeOption(val id: String, val label: String, val extra_price: Double = 0.0)
data class MilkOption(val id: String, val name: String, val extra_price: Double = 0.0)
data class CartLine(val item: MenuItem, val size: SizeOption, val sugar: String, val milk: MilkOption, val quantity: Int = 1) {
    val unitPrice: Double get() = item.base_price + size.extra_price + milk.extra_price + sugarPrice(sugar)
    val key: String get() = "${item.id}|${size.id}|${sugar}|${milk.id}"
}
fun sugarPrice(value: String): Double = if ("(+0.40)" in value) 0.40 else 0.0
