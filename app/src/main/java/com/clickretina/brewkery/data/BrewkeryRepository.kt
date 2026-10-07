package com.clickretina.brewkery.data

class BrewkeryRepository(private val api: BrewkeryApi = BrewkeryApi.service) {
    suspend fun menu() = api.menu()
    suspend fun item(id: Int) = api.item("${BrewkeryApi.BASE}api/items/$id.json")
}
