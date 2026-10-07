package com.clickretina.brewkery

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.clickretina.brewkery.data.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Locale

private val Coffee = Color(0xFF382318)
private val Roast = Color(0xFFB95733)
private val Cream = Color(0xFFFFFAF5)
private val Pale = Color(0xFFF4EAE1)
private val Ink = Color(0xFF2D211B)
private val Muted = Color(0xFF82736A)

class MainActivity : ComponentActivity() {
    private val vm: BrewkeryViewModel by viewModels()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = android.graphics.Color.rgb(255, 250, 245)
        window.navigationBarColor = android.graphics.Color.rgb(255, 250, 245)
        @Suppress("DEPRECATION")
        window.decorView.systemUiVisibility = android.view.View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR or android.view.View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
        setContent { MaterialTheme(colorScheme = lightColorScheme(primary = Roast, onPrimary = Color.White, background = Cream, surface = Color.White)) { BrewkeryApp(vm) } }
    }
}

enum class Screen { MENU, DETAIL, CART, ORDER }
data class AppState(
    val loading: Boolean = true, val error: String? = null, val menu: MenuResponse? = null,
    val screen: Screen = Screen.MENU, val category: String = "ALL", val query: String = "",
    val selected: MenuItem? = null, val sizeIndex: Int = 0, val milkIndex: Int = 0, val sugarIndex: Int = 0,
    val cart: List<CartLine> = emptyList(), val orderId: String = ""
)

class BrewkeryViewModel : ViewModel() {
    private val repository = BrewkeryRepository()
    private val _state = MutableStateFlow(AppState())
    val state = _state.asStateFlow()
    init { loadMenu() }
    fun loadMenu() = viewModelScope.launch {
        _state.update { it.copy(loading = true, error = null) }
        runCatching { repository.menu() }
            .onSuccess { menu -> _state.update { it.copy(loading = false, menu = menu) } }
            .onFailure { _state.update { it.copy(loading = false, error = "Couldn't load the menu. Check your connection and retry.") } }
    }
    fun query(value: String) { _state.update { it.copy(query = value) } }
    fun category(value: String) { _state.update { it.copy(category = value) } }
    fun openItem(item: MenuItem) = viewModelScope.launch {
        _state.update { it.copy(loading = true, error = null, selected = item, screen = Screen.DETAIL, sizeIndex = 0, milkIndex = 0, sugarIndex = 0) }
        runCatching { repository.item(item.id) }
            .onSuccess { fetched -> _state.update { state -> state.copy(loading = false, selected = fetched) } }
            .onFailure { _state.update { it.copy(loading = false, error = "Showing menu details; live details couldn't be refreshed.") } }
    }
    fun customize(size: Int? = null, milk: Int? = null, sugar: Int? = null) {
        _state.update { s -> s.copy(sizeIndex = size ?: s.sizeIndex, milkIndex = milk ?: s.milkIndex, sugarIndex = sugar ?: s.sugarIndex) }
    }
    fun addToCart() {
        val s = _state.value; val item = s.selected ?: return; val c = item.customizations
        if (c.sizes.isEmpty() || c.milk_options.isEmpty() || c.sugar_levels.isEmpty()) return
        val line = CartLine(item, c.sizes[s.sizeIndex.coerceIn(c.sizes.indices)], c.sugar_levels[s.sugarIndex.coerceIn(c.sugar_levels.indices)], c.milk_options[s.milkIndex.coerceIn(c.milk_options.indices)])
        val cart = if (s.cart.any { it.key == line.key }) s.cart.map { if (it.key == line.key) it.copy(quantity = it.quantity + 1) else it } else s.cart + line
        _state.update { it.copy(cart = cart, screen = Screen.MENU, error = null) }
    }
    fun quantity(key: String, delta: Int) {
        _state.update { s -> s.copy(cart = s.cart.mapNotNull { line -> if (line.key != key) line else (line.quantity + delta).takeIf { it > 0 }?.let { line.copy(quantity = it) } }) }
    }
    fun clearCart() { _state.update { it.copy(cart = emptyList()) } }
    fun navigate(screen: Screen) { _state.update { it.copy(screen = screen, error = null) } }
    fun placeOrder() { _state.update { it.copy(screen = Screen.ORDER, orderId = "BK-${(10000..99999).random()}", cart = emptyList()) } }
}

@Composable
fun BrewkeryApp(vm: BrewkeryViewModel = viewModel()) {
    val s by vm.state.collectAsStateWithLifecycle()
    Scaffold(containerColor = Cream, bottomBar = {
        if (s.screen == Screen.MENU && s.cart.isNotEmpty()) Surface(shadowElevation = 12.dp, color = Coffee, shape = RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp)) {
            Row(Modifier.fillMaxWidth().clickable { vm.navigate(Screen.CART) }.padding(horizontal = 22.dp, vertical = 17.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("${s.cart.sumOf { it.quantity }}", color = Coffee, fontWeight = FontWeight.Bold, modifier = Modifier.background(Color.White, CircleShape).padding(horizontal = 9.dp, vertical = 5.dp))
                Spacer(Modifier.width(12.dp)); Column(Modifier.weight(1f)) { Text("View your cart", color = Color.White, fontWeight = FontWeight.SemiBold); Text("Proceed to checkout", color = Color(0xFFD5B9A5), fontSize = 12.sp) }
                Text(money(s.cart.sumOf { it.unitPrice * it.quantity }), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp); Spacer(Modifier.width(8.dp)); Box(Modifier.size(32.dp).clip(RoundedCornerShape(10.dp)).background(Color.White.copy(alpha = .14f)), contentAlignment = Alignment.Center) { Icon(Icons.AutoMirrored.Filled.ArrowForward, null, tint = Color.White) }
            }
        }
    }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            when (s.screen) {
                Screen.MENU -> MenuScreen(s, vm)
                Screen.DETAIL -> DetailScreen(s, vm)
                Screen.CART -> CartScreen(s, vm)
                Screen.ORDER -> OrderScreen(s, vm)
            }
        }
    }
}

@Composable
fun TopBar(title: String, back: (() -> Unit)? = null, action: (@Composable () -> Unit)? = null) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 22.dp, vertical = 16.dp), verticalAlignment = Alignment.CenterVertically) {
        if (back != null) { RoundedIconButton(Icons.AutoMirrored.Filled.ArrowBack, "Back", back); Spacer(Modifier.width(8.dp)) }
        Column(Modifier.weight(1f)) { Text("BK  FRESH ROAST & BAKES", color = Muted, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp); Text(title, color = Ink, fontSize = 21.sp, fontWeight = FontWeight.Bold) }
        action?.invoke()
    }
}

@Composable
fun RoundedIconButton(icon: ImageVector, description: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.size(40.dp).clickable(onClick = onClick),
        shape = RoundedCornerShape(13.dp),
        color = Color.White,
        border = androidx.compose.foundation.BorderStroke(1.dp, Pale)
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = description, tint = Ink)
        }
    }
}
@Composable
fun MenuScreen(s: AppState, vm: BrewkeryViewModel) {
    Column {
        TopBar("Brewkery Artisans", action = {
            val count = s.cart.sumOf { it.quantity }
            Box(Modifier.size(44.dp), contentAlignment = Alignment.Center) {
                Surface(
                    modifier = Modifier.size(40.dp).clickable { vm.navigate(Screen.CART) },
                    shape = CircleShape,
                    color = Color.White,
                    border = androidx.compose.foundation.BorderStroke(1.dp, Pale)
                ) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Icon(Icons.Filled.ShoppingBag, "Open cart: $count items", tint = Ink)
                    }
                }
                Text(
                    count.toString(), color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold,
                    modifier = Modifier.align(Alignment.TopEnd).offset(x = (-1).dp, y = 0.dp).sizeIn(minWidth = 20.dp, minHeight = 20.dp).clip(CircleShape).background(Roast).padding(horizontal = 5.dp, vertical = 2.dp)
                )
            }
        })
        if (s.loading && s.menu == null) { Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator(color = Roast) }; return }
        s.error?.let { ErrorBanner(it) { vm.loadMenu() } }
        LazyColumn(contentPadding = PaddingValues(bottom = 18.dp)) {
            item {
                if (s.orderId.isNotBlank()) ActiveOrderBanner(s.orderId) { vm.navigate(Screen.ORDER) }
                else StoreBanner(s.menu?.meta?.estimated_delivery_time ?: "20 - 30 mins")
            }
            item {
                Row(Modifier.padding(horizontal = 20.dp, vertical = 14.dp).fillMaxWidth().clip(RoundedCornerShape(15.dp)).background(Color.White).padding(horizontal = 14.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(32.dp).clip(RoundedCornerShape(10.dp)).background(Pale), contentAlignment = Alignment.Center) { Icon(Icons.Filled.Search, null, tint = Muted) }; Spacer(Modifier.width(8.dp)); BasicTextField(value = s.query, onValueChange = vm::query, singleLine = true, textStyle = androidx.compose.ui.text.TextStyle(color = Ink, fontSize = 14.sp), decorationBox = { inner -> if (s.query.isEmpty()) Text("Search roast, cold brew, pastry…", color = Muted, fontSize = 14.sp); inner() })
                }
            }
            item { Row(Modifier.horizontalScroll(rememberScrollState()).padding(start = 20.dp, bottom = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CategoryChip("All Items", "ALL", s.category, vm)
                s.menu?.categories.orEmpty().forEach { CategoryChip("${it.icon} ${it.name}", it.id, s.category, vm) }
            } }

            val filtered = s.menu?.items.orEmpty().filter { (s.category == "ALL" || it.category_id == s.category) && (s.query.isBlank() || it.name.contains(s.query, true) || it.tagline.contains(s.query, true)) }
            if (filtered.isEmpty()) item { Text("No items match your search.", Modifier.padding(24.dp), color = Muted) }
            items(filtered, key = { it.id }) { item -> MenuItemCard(item) { vm.openItem(item) } }
        }
    }
}

@Composable
fun ActiveOrderBanner(ticket: String, onTrack: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 4.dp).clip(RoundedCornerShape(18.dp)).background(Pale).padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(40.dp).clip(RoundedCornerShape(13.dp)).background(Color.White), contentAlignment = Alignment.Center) { Text("●", color = Color(0xFF39C98A), fontSize = 20.sp) }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text("ACTIVE ORDER  •", color = Color(0xFF00996E), fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = .6.sp)
            Text("Active Order #$ticket", color = Ink, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Text("Preparing (Arriving in 25 mins)", color = Muted, fontSize = 12.sp)
        }
        Button(onClick = onTrack, shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00996E)), contentPadding = PaddingValues(horizontal = 14.dp, vertical = 7.dp)) { Text("Track", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
    }
}
@Composable
fun StoreBanner(deliveryTime: String) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 4.dp).clip(RoundedCornerShape(18.dp)).background(Pale).padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
        Text("🛵", fontSize = 23.sp); Spacer(Modifier.width(13.dp)); Column(Modifier.weight(1f)) { Text("STORE INFO  •  OPEN", color = Roast, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = .6.sp); Text("Delivery in $deliveryTime", color = Ink, fontWeight = FontWeight.SemiBold); Text("$2.50 flat fee", color = Muted, fontSize = 12.sp) }
        Text("Open", color = Coffee, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.background(Color.White, RoundedCornerShape(20.dp)).padding(horizontal = 13.dp, vertical = 8.dp))
    }
}

@Composable
fun CategoryChip(label: String, id: String, selected: String, vm: BrewkeryViewModel) {
    val active = selected == id
    Text(label, modifier = Modifier.clip(RoundedCornerShape(22.dp)).background(if (active) Coffee else Color.White).clickable { vm.category(id) }.padding(horizontal = 15.dp, vertical = 10.dp), color = if (active) Color.White else Ink, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
}

@Composable
fun MenuItemCard(item: MenuItem, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 6.dp).clip(RoundedCornerShape(18.dp)).background(Color.White).clickable(onClick = onClick).padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
        AsyncImage(model = item.image_url, contentDescription = item.name, contentScale = ContentScale.Crop, modifier = Modifier.size(82.dp).clip(RoundedCornerShape(14.dp)))
        Spacer(Modifier.width(13.dp)); Column(Modifier.weight(1f)) {
            if (item.badge.isNotBlank()) Text(item.badge, color = Roast, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = .6.sp)
            Text(item.name, color = Ink, fontWeight = FontWeight.Bold, fontSize = 14.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text("⭐ ${item.rating} (${item.review_count})", color = Muted, fontSize = 11.sp, modifier = Modifier.padding(top = 4.dp))
            Row(Modifier.fillMaxWidth().padding(top = 5.dp), verticalAlignment = Alignment.CenterVertically) { Text(money(item.base_price), color = Roast, fontWeight = FontWeight.Bold); Spacer(Modifier.weight(1f)); Text("+ Customize", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.background(Roast, RoundedCornerShape(20.dp)).padding(horizontal = 11.dp, vertical = 7.dp)) }
        }
    }
}

@Composable
fun DetailScreen(s: AppState, vm: BrewkeryViewModel) {
    val item = s.selected ?: return
    Column(Modifier.fillMaxSize()) {
        TopBar("Customize your drink", { vm.navigate(Screen.MENU) }, { RoundedIconButton(Icons.Filled.ShoppingBag, "Cart", onClick = { vm.navigate(Screen.CART) }) })
        LazyColumn(Modifier.weight(1f)) {
            item { AsyncImage(model = item.image_url, contentDescription = item.name, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxWidth().height(230.dp).padding(horizontal = 20.dp).clip(RoundedCornerShape(22.dp))) }
            item { Column(Modifier.padding(horizontal = 22.dp, vertical = 16.dp)) { Text(item.badge, color = Roast, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp); Text(item.name, color = Ink, fontWeight = FontWeight.Bold, fontSize = 25.sp); Text("⭐ ${item.rating}  ·  ${item.review_count} reviews  ·  ${item.prep_time}", color = Muted, fontSize = 12.sp, modifier = Modifier.padding(top = 5.dp)); Text(item.description, color = Muted, fontSize = 14.sp, lineHeight = 21.sp, modifier = Modifier.padding(top = 12.dp)); Text("INGREDIENTS", color = Ink, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp, modifier = Modifier.padding(top = 15.dp)); Text(item.ingredients.joinToString("  ·  "), color = Muted, fontSize = 12.sp, modifier = Modifier.padding(top = 5.dp)) } }
            item { ChoiceSection("Choose size", item.customizations.sizes.map { it.label to it.extra_price }, s.sizeIndex) { vm.customize(size = it) } }
            item { ChoiceSection("Milk / topping", item.customizations.milk_options.map { it.name to it.extra_price }, s.milkIndex) { vm.customize(milk = it) } }
            item { ChoiceSection("Sweetness", item.customizations.sugar_levels.map { it to sugarPrice(it) }, s.sugarIndex) { vm.customize(sugar = it) } }
            item { Spacer(Modifier.height(20.dp)) }
        }
        val price = item.base_price + item.customizations.sizes.getOrNull(s.sizeIndex)?.extra_price.orZero() + item.customizations.milk_options.getOrNull(s.milkIndex)?.extra_price.orZero() + item.customizations.sugar_levels.getOrNull(s.sugarIndex)?.let(::sugarPrice).orZero()
        Button(onClick = { vm.addToCart() }, modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp).height(54.dp), shape = RoundedCornerShape(16.dp), colors = ButtonDefaults.buttonColors(containerColor = Coffee)) { Text("Add to cart  ·  ${money(price)}", fontWeight = FontWeight.Bold) }
    }
    if (s.loading) LinearProgressIndicator(Modifier.fillMaxWidth(), color = Roast)
    s.error?.let { ErrorBanner(it) { vm.openItem(item) } }
}

@Composable
fun ChoiceSection(title: String, options: List<Pair<String, Double>>, selected: Int, onSelect: (Int) -> Unit) {
    Column(Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
        Text(title, color = Ink, fontWeight = FontWeight.Bold, fontSize = 16.sp, modifier = Modifier.padding(bottom = 9.dp))
        options.forEachIndexed { index, (label, extra) ->
            val active = index == selected
            Row(Modifier.fillMaxWidth().padding(vertical = 4.dp).clip(RoundedCornerShape(13.dp)).background(if (active) Color(0xFFFFF2EA) else Color.White).clickable { onSelect(index) }.padding(horizontal = 14.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(if (active) "●" else "○", color = if (active) Roast else Muted); Text(label, Modifier.weight(1f).padding(start = 10.dp), color = Ink, fontSize = 13.sp); if (extra > 0) Text("+${money(extra)}", color = Roast, fontSize = 12.sp)
            }
        }
    }
}

@Composable
fun CartScreen(s: AppState, vm: BrewkeryViewModel) {
    val subtotal = s.cart.sumOf { it.unitPrice * it.quantity }
    val delivery = s.menu?.meta?.delivery_fee ?: 2.5
    val taxRate = s.menu?.meta?.tax_rate_percent ?: 8.0
    val tax = subtotal * taxRate / 100.0
    val total = if (s.cart.isEmpty()) 0.0 else subtotal + delivery + tax

    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
            RoundedIconButton(Icons.AutoMirrored.Filled.ArrowBack, "Back", onClick = { vm.navigate(Screen.MENU) })
            Box(Modifier.weight(1f), contentAlignment = Alignment.Center) { Text("YOUR CART", color = Ink, fontWeight = FontWeight.Bold, fontSize = 16.sp, letterSpacing = .7.sp) }
            TextButton(onClick = { vm.clearCart() }, contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp)) { Text("Clear Cart", color = Roast, fontSize = 13.sp, fontWeight = FontWeight.SemiBold) }
        }

        if (s.cart.isEmpty()) {
            Box(Modifier.weight(1f).fillMaxWidth().padding(horizontal = 20.dp, vertical = 4.dp), contentAlignment = Alignment.TopCenter) {
                Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(17.dp), color = Color.White, border = androidx.compose.foundation.BorderStroke(1.dp, Pale)) {
                    Column(Modifier.padding(vertical = 26.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(Modifier.size(44.dp).clip(RoundedCornerShape(13.dp)).background(Pale), contentAlignment = Alignment.Center) { Icon(Icons.Filled.ShoppingBag, "Empty cart", tint = Color(0xFFE8CFC1)) }
                        Text("Your cart is empty.", color = Muted, fontSize = 14.sp, modifier = Modifier.padding(top = 10.dp))
                    }
                }
            }
        } else {
            LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(bottom = 12.dp)) {
                items(s.cart, key = { it.key }) { line ->
                    Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 7.dp).clip(RoundedCornerShape(17.dp)).background(Color.White).padding(13.dp), verticalAlignment = Alignment.CenterVertically) {
                        AsyncImage(model = line.item.image_url, contentDescription = line.item.name, contentScale = ContentScale.Crop, modifier = Modifier.size(66.dp).clip(RoundedCornerShape(12.dp)))
                        Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                            Text(line.item.name, fontWeight = FontWeight.Bold, color = Ink, fontSize = 14.sp)
                            Text("${line.size.label} · ${line.milk.name}", color = Muted, fontSize = 10.sp, maxLines = 2)
                            Text(money(line.unitPrice * line.quantity), color = Roast, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 4.dp))
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RoundedIconButton(Icons.Filled.Remove, "Remove one", onClick = { vm.quantity(line.key, -1) }, modifier = Modifier.size(34.dp))
                            Text("${line.quantity}", Modifier.padding(horizontal = 9.dp), fontWeight = FontWeight.Bold)
                            RoundedIconButton(Icons.Filled.Add, "Add one", onClick = { vm.quantity(line.key, 1) }, modifier = Modifier.size(34.dp))
                        }
                    }
                }
            }
        }

        Surface(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 10.dp), shape = RoundedCornerShape(18.dp), color = Color.White, border = androidx.compose.foundation.BorderStroke(1.dp, Pale)) {
            Column(Modifier.padding(horizontal = 17.dp, vertical = 14.dp)) {
                PriceRow("Subtotal", subtotal)
                PriceRow("Delivery Fee", delivery)
                PriceRow("Est. Tax (${String.format(Locale.US, "%.1f", taxRate)}%)", tax)
                HorizontalDivider(Modifier.padding(vertical = 8.dp), color = Pale)
                PriceRow("Total Payable", total, true)
                Button(onClick = { vm.placeOrder() }, modifier = Modifier.fillMaxWidth().padding(top = 12.dp).height(54.dp), shape = RoundedCornerShape(15.dp), colors = ButtonDefaults.buttonColors(containerColor = Roast)) {
                    Icon(Icons.Filled.Lock, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Place Order Now  •  ${money(total)}", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
@Composable
fun PriceRow(label: String, amount: Double, strong: Boolean = false) { Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), horizontalArrangement = Arrangement.SpaceBetween) { Text(label, color = if (strong) Ink else Muted, fontWeight = if (strong) FontWeight.Bold else FontWeight.Normal); Text(money(amount), color = Ink, fontWeight = if (strong) FontWeight.Bold else FontWeight.Medium) } }

@Composable
fun OrderScreen(s: AppState, vm: BrewkeryViewModel) {
    Column(Modifier.fillMaxSize()) {
        TopBar("Order placed")
        Column(Modifier.fillMaxSize().padding(horizontal = 24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Text("✓", color = Color.White, fontSize = 42.sp, modifier = Modifier.background(Roast, CircleShape).padding(horizontal = 28.dp, vertical = 14.dp))
            Text("Order received!", color = Ink, fontSize = 27.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 22.dp))
            Text("Your order is being freshly prepared.", color = Muted, modifier = Modifier.padding(top = 7.dp))
            Column(Modifier.fillMaxWidth().padding(top = 28.dp).clip(RoundedCornerShape(20.dp)).background(Color.White).padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("TICKET ID", color = Muted, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp); Text(s.orderId, color = Ink, fontSize = 24.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 5.dp)); Spacer(Modifier.height(20.dp)); Text("●  PREPARING", color = Roast, fontWeight = FontWeight.Bold); Text("Estimated delivery: ${s.menu?.meta?.estimated_delivery_time ?: "20 - 30 mins"}", color = Muted, fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp))
            }
            Button(onClick = { vm.navigate(Screen.MENU) }, modifier = Modifier.fillMaxWidth().padding(top = 22.dp).height(52.dp), shape = RoundedCornerShape(16.dp), colors = ButtonDefaults.buttonColors(containerColor = Coffee)) { Text("Back to menu") }
        }
    }
}

@Composable
fun ErrorBanner(message: String, retry: () -> Unit) { Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 6.dp).clip(RoundedCornerShape(12.dp)).background(Color(0xFFFFE8E3)).clickable(onClick = retry).padding(12.dp), verticalAlignment = Alignment.CenterVertically) { Text(message, Modifier.weight(1f), color = Ink, fontSize = 12.sp); Text("Retry", color = Roast, fontWeight = FontWeight.Bold, fontSize = 12.sp) } }

private fun money(amount: Double) = String.format(Locale.US, "$%.2f", amount)
private fun Double?.orZero() = this ?: 0.0