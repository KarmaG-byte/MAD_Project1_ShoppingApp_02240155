@file:OptIn(ExperimentalMaterial3Api::class)

package com.karmagbyte.tshongla

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val Wine = Color(0xFF8F3035)
private val WineDark = Color(0xFF5D1F25)
private val Gold = Color(0xFFE4A93B)
private val Cream = Color(0xFFFFF9F0)
private val Ink = Color(0xFF2C2523)
private val Sage = Color(0xFF59735C)

data class Product(
    val id: Int,
    val name: String,
    val category: String,
    val price: Int,
    val rating: Double,
    val seller: String,
    val location: String,
    val symbol: String,
    val colors: List<Color>,
    val description: String
)

private val products = listOf(
    Product(1, "Handwoven Yathra Bag", "Textiles", 1250, 4.9, "Pem's Handcraft", "Bumthang", "👜", listOf(Color(0xFFB95C4B), Color(0xFFE9A64A)), "A colourful handwoven shoulder bag made with traditional Bhutanese yathra patterns."),
    Product(2, "Natural Incense Set", "Wellness", 480, 4.8, "Druk Aromas", "Thimphu", "🌿", listOf(Color(0xFF557A5D), Color(0xFFA7C48C)), "A calming selection of locally prepared incense using aromatic Himalayan herbs."),
    Product(3, "Carved Wooden Bowl", "Home", 890, 4.7, "Zorig Woodworks", "Trashigang", "🥣", listOf(Color(0xFF8B5A3C), Color(0xFFD69B61)), "A smooth, food-safe wooden bowl individually carved and finished by a local artisan."),
    Product(4, "Bhutanese Woven Scarf", "Textiles", 1600, 4.9, "Weaves of Haa", "Haa", "🧣", listOf(Color(0xFF7A3F65), Color(0xFFD787A6)), "A soft statement scarf woven by hand with a contemporary interpretation of traditional motifs."),
    Product(5, "Clay Butter Lamp", "Crafts", 350, 4.6, "Deki Pottery", "Paro", "🪔", listOf(Color(0xFFAA603E), Color(0xFFF0B36D)), "A handmade clay butter lamp inspired by Bhutanese homes and sacred spaces."),
    Product(6, "Wildflower Honey", "Food", 650, 4.8, "Mountain Harvest", "Trongsa", "🍯", listOf(Color(0xFFE09A24), Color(0xFFF7D46B)), "Pure Bhutanese wildflower honey collected in small batches from mountain apiaries.")
)

enum class Screen { Home, Explore, Detail, Cart, Shop, AddProduct }

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { TshongLaTheme { TshongLaApp() } }
    }
}

@Composable
private fun TshongLaTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = Wine,
            onPrimary = Color.White,
            secondary = Gold,
            background = Cream,
            surface = Color.White,
            onSurface = Ink
        ),
        typography = Typography(
            headlineLarge = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold),
            headlineSmall = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
            titleLarge = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
        ),
        content = content
    )
}

@Composable
private fun TshongLaApp() {
    var screen by remember { mutableStateOf(Screen.Home) }
    var selected by remember { mutableStateOf(products.first()) }
    val cart = remember { mutableStateMapOf<Int, Int>() }

    fun openProduct(product: Product) { selected = product; screen = Screen.Detail }
    fun addToCart(product: Product) { cart[product.id] = (cart[product.id] ?: 0) + 1 }

    Scaffold(
        containerColor = Cream,
        bottomBar = {
            if (screen != Screen.Detail && screen != Screen.AddProduct) {
                AppBottomBar(screen, cart.values.sum()) { screen = it }
            }
        }
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            when (screen) {
                Screen.Home -> HomeScreen(::openProduct) { screen = Screen.Explore }
                Screen.Explore -> ExploreScreen(::openProduct)
                Screen.Detail -> ProductDetailScreen(selected, { screen = Screen.Home }) {
                    addToCart(selected); screen = Screen.Cart
                }
                Screen.Cart -> CartScreen(cart, { screen = Screen.Explore })
                Screen.Shop -> ShopScreen { screen = Screen.AddProduct }
                Screen.AddProduct -> AddProductScreen { screen = Screen.Shop }
            }
        }
    }
}

@Composable
private fun AppBottomBar(current: Screen, cartCount: Int, navigate: (Screen) -> Unit) {
    NavigationBar(containerColor = Color.White, tonalElevation = 8.dp) {
        val destinations = listOf(
            Triple(Screen.Home, Icons.Outlined.Home, "Home"),
            Triple(Screen.Explore, Icons.Outlined.Search, "Explore"),
            Triple(Screen.Cart, Icons.Outlined.ShoppingBag, "Cart"),
            Triple(Screen.Shop, Icons.Outlined.Storefront, "My Shop")
        )
        destinations.forEach { (screen, icon, label) ->
            NavigationBarItem(
                selected = current == screen,
                onClick = { navigate(screen) },
                icon = {
                    BadgedBox(badge = {
                        if (screen == Screen.Cart && cartCount > 0) Badge { Text(cartCount.toString()) }
                    }) { Icon(icon, label) }
                },
                label = { Text(label, fontSize = 11.sp) },
                colors = NavigationBarItemDefaults.colors(indicatorColor = Wine.copy(alpha = .12f))
            )
        }
    }
}

@Composable
private fun HomeScreen(openProduct: (Product) -> Unit, explore: () -> Unit) {
    LazyColumn(contentPadding = PaddingValues(bottom = 24.dp)) {
        item {
            Column(
                Modifier.fillMaxWidth().background(
                    Brush.verticalGradient(listOf(Color(0xFFFFF4E2), Cream))
                ).padding(20.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Kuzu zangpo!", color = Wine, fontWeight = FontWeight.SemiBold)
                        Text("Discover Bhutan,\nmade by hand.", fontSize = 30.sp, lineHeight = 34.sp, fontWeight = FontWeight.ExtraBold, color = Ink)
                    }
                    Surface(shape = CircleShape, color = Wine, modifier = Modifier.size(48.dp)) {
                        Box(contentAlignment = Alignment.Center) { Text("SD", color = Color.White, fontWeight = FontWeight.Bold) }
                    }
                }
                Spacer(Modifier.height(18.dp))
                SearchBar(readOnly = true, onClick = explore)
                Spacer(Modifier.height(18.dp))
                HeroBanner(explore)
            }
        }
        item { SectionHeader("Shop by category", "Explore all", explore) }
        item { CategoryRow() }
        item { SectionHeader("Featured for you", "See all", explore) }
        items(products.take(4).chunked(2)) { row ->
            Row(Modifier.padding(horizontal = 16.dp).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                row.forEach { product -> Box(Modifier.weight(1f)) { ProductCard(product) { openProduct(product) } } }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
            Spacer(Modifier.height(12.dp))
        }
    }
}

@Composable
private fun HeroBanner(onClick: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(24.dp), color = Wine
    ) {
        Row(Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("AUTHENTIC • LOCAL • UNIQUE", color = Gold, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                Text("Stories woven into\nevery creation", color = Color.White, fontSize = 21.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(12.dp))
                AssistChip(onClick = onClick, label = { Text("Explore collection") }, trailingIcon = { Icon(Icons.Default.ArrowForward, null, Modifier.size(16.dp)) })
            }
            Text("🧺", fontSize = 68.sp)
        }
    }
}

@Composable
private fun SectionHeader(title: String, action: String, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(start = 16.dp, end = 8.dp, top = 22.dp, bottom = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(title, style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
        TextButton(onClick = onClick) { Text(action) }
    }
}

@Composable
private fun CategoryRow() {
    val categories = listOf("🧵" to "Textiles", "🏺" to "Crafts", "🏠" to "Home", "🍯" to "Food", "🌿" to "Wellness")
    LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        items(categories) { (symbol, title) ->
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Surface(shape = RoundedCornerShape(20.dp), color = Color.White, shadowElevation = 2.dp, modifier = Modifier.size(68.dp)) {
                    Box(contentAlignment = Alignment.Center) { Text(symbol, fontSize = 30.sp) }
                }
                Spacer(Modifier.height(6.dp)); Text(title, fontSize = 12.sp, fontWeight = FontWeight.Medium)
            }
        }
    }
}

@Composable
private fun SearchBar(readOnly: Boolean = false, onClick: (() -> Unit)? = null, value: String = "", onValueChange: (String) -> Unit = {}) {
    OutlinedTextField(
        value = value, onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth().then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        enabled = !readOnly, readOnly = readOnly,
        placeholder = { Text("Search handmade products") },
        leadingIcon = { Icon(Icons.Default.Search, null) },
        trailingIcon = { Icon(Icons.Outlined.Tune, null) },
        shape = RoundedCornerShape(18.dp), colors = OutlinedTextFieldDefaults.colors(
            disabledContainerColor = Color.White, disabledBorderColor = Color.Transparent,
            disabledTextColor = Ink, disabledPlaceholderColor = Color.Gray
        )
    )
}

@Composable
private fun ProductCard(product: Product, onClick: () -> Unit) {
    Card(
        onClick = onClick, shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column {
            ProductArtwork(product, Modifier.fillMaxWidth().height(128.dp), 48)
            Column(Modifier.padding(12.dp)) {
                Text(product.name, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis, minLines = 2)
                Spacer(Modifier.height(5.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Star, null, tint = Gold, modifier = Modifier.size(15.dp))
                    Text(" ${product.rating}", fontSize = 12.sp, color = Color.Gray)
                }
                Spacer(Modifier.height(7.dp))
                Text("Nu. ${product.price}", color = Wine, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
            }
        }
    }
}

@Composable
private fun ProductArtwork(product: Product, modifier: Modifier, symbolSize: Int) {
    Box(modifier.clip(RoundedCornerShape(18.dp)).background(Brush.linearGradient(product.colors)), contentAlignment = Alignment.Center) {
        Text(product.symbol, fontSize = symbolSize.sp)
        Surface(Modifier.align(Alignment.TopEnd).padding(10.dp), shape = CircleShape, color = Color.White.copy(alpha = .9f)) {
            Icon(Icons.Outlined.FavoriteBorder, "Save", tint = Wine, modifier = Modifier.padding(7.dp).size(17.dp))
        }
    }
}

@Composable
private fun ExploreScreen(openProduct: (Product) -> Unit) {
    var query by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("All") }
    val filtered = products.filter { (selectedCategory == "All" || it.category == selectedCategory) && it.name.contains(query, true) }
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item { Text("Explore", style = MaterialTheme.typography.headlineLarge); Text("Find something made with meaning.", color = Color.Gray) }
        item { SearchBar(value = query, onValueChange = { query = it }) }
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(listOf("All", "Textiles", "Crafts", "Home", "Food", "Wellness")) { category ->
                    FilterChip(selected = selectedCategory == category, onClick = { selectedCategory = category }, label = { Text(category) })
                }
            }
        }
        item { Row(verticalAlignment = Alignment.CenterVertically) { Text("${filtered.size} products", fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f)); Icon(Icons.Default.SwapVert, "Sort") } }
        items(filtered.chunked(2)) { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                row.forEach { product -> Box(Modifier.weight(1f)) { ProductCard(product) { openProduct(product) } } }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun ProductDetailScreen(product: Product, back: () -> Unit, add: () -> Unit) {
    Column(Modifier.fillMaxSize()) {
        Box(Modifier.fillMaxWidth().height(330.dp)) {
            ProductArtwork(product, Modifier.fillMaxSize().clip(RoundedCornerShape(0.dp)), 100)
            SmallIconButton(Icons.Default.ArrowBack, "Back", Modifier.align(Alignment.TopStart).padding(16.dp), back)
            SmallIconButton(Icons.Outlined.Share, "Share", Modifier.align(Alignment.TopEnd).padding(16.dp)) {}
        }
        Column(Modifier.padding(20.dp).weight(1f)) {
            Text(product.category.uppercase(), color = Wine, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            Spacer(Modifier.height(6.dp)); Text(product.name, style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                repeat(5) { Icon(Icons.Default.Star, null, tint = Gold, modifier = Modifier.size(17.dp)) }
                Text("  ${product.rating}  •  24 reviews", color = Color.Gray, fontSize = 13.sp)
            }
            Spacer(Modifier.height(14.dp)); Text("Nu. ${product.price}", color = Wine, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold)
            HorizontalDivider(Modifier.padding(vertical = 18.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(shape = CircleShape, color = Wine.copy(alpha = .12f), modifier = Modifier.size(48.dp)) { Box(contentAlignment = Alignment.Center) { Icon(Icons.Default.Storefront, null, tint = Wine) } }
                Column(Modifier.padding(start = 12.dp).weight(1f)) { Text(product.seller, fontWeight = FontWeight.Bold); Text("📍 ${product.location}, Bhutan", color = Color.Gray, fontSize = 13.sp) }
                TextButton(onClick = {}) { Text("View shop") }
            }
            Spacer(Modifier.height(18.dp)); Text("About this piece", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Spacer(Modifier.height(8.dp)); Text(product.description, color = Color.DarkGray, lineHeight = 22.sp)
        }
        Row(Modifier.fillMaxWidth().background(Color.White).padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedIconButton(onClick = {}) { Icon(Icons.Outlined.FavoriteBorder, "Save") }
            Button(onClick = add, modifier = Modifier.weight(1f).height(52.dp), shape = RoundedCornerShape(16.dp)) { Icon(Icons.Default.ShoppingBag, null); Spacer(Modifier.width(8.dp)); Text("Add to cart") }
        }
    }
}

@Composable
private fun SmallIconButton(icon: ImageVector, description: String, modifier: Modifier, onClick: () -> Unit) {
    Surface(modifier = modifier, shape = CircleShape, color = Color.White.copy(alpha = .92f), shadowElevation = 2.dp) {
        IconButton(onClick = onClick) { Icon(icon, description) }
    }
}

@Composable
private fun CartScreen(cart: MutableMap<Int, Int>, explore: () -> Unit) {
    val cartProducts = products.filter { cart.containsKey(it.id) }
    val subtotal = cartProducts.sumOf { it.price * (cart[it.id] ?: 0) }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item { Text("Your cart", style = MaterialTheme.typography.headlineLarge); Text("${cart.values.sum()} items selected", color = Color.Gray) }
        if (cartProducts.isEmpty()) {
            item {
                Column(Modifier.fillParentMaxHeight(.65f).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                    Text("🧺", fontSize = 76.sp); Text("Your basket is empty", fontSize = 21.sp, fontWeight = FontWeight.Bold)
                    Text("Discover something special from a local maker.", color = Color.Gray)
                    Spacer(Modifier.height(20.dp)); Button(onClick = explore) { Text("Start exploring") }
                }
            }
        } else {
            items(cartProducts) { product ->
                Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(18.dp)) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        ProductArtwork(product, Modifier.size(88.dp), 34)
                        Column(Modifier.padding(start = 12.dp).weight(1f)) {
                            Text(product.name, fontWeight = FontWeight.Bold, maxLines = 2)
                            Text("Nu. ${product.price}", color = Wine, fontWeight = FontWeight.Bold)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(onClick = { val q = cart[product.id] ?: 1; if (q <= 1) cart.remove(product.id) else cart[product.id] = q - 1 }, modifier = Modifier.size(32.dp)) { Icon(Icons.Default.Remove, "Decrease") }
                                Text("${cart[product.id]}", Modifier.padding(horizontal = 10.dp), fontWeight = FontWeight.Bold)
                                IconButton(onClick = { cart[product.id] = (cart[product.id] ?: 0) + 1 }, modifier = Modifier.size(32.dp)) { Icon(Icons.Default.Add, "Increase") }
                            }
                        }
                    }
                }
            }
            item {
                Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(20.dp)) {
                    Column(Modifier.padding(18.dp)) {
                        Text("Order summary", fontWeight = FontWeight.Bold, fontSize = 19.sp)
                        SummaryRow("Subtotal", "Nu. $subtotal"); SummaryRow("Delivery", "Calculated next")
                        HorizontalDivider(Modifier.padding(vertical = 10.dp)); SummaryRow("Total", "Nu. $subtotal", true)
                        Spacer(Modifier.height(14.dp)); Button(onClick = {}, Modifier.fillMaxWidth().height(50.dp), shape = RoundedCornerShape(15.dp)) { Text("Continue to checkout") }
                    }
                }
            }
        }
    }
}

@Composable
private fun SummaryRow(label: String, value: String, bold: Boolean = false) {
    Row(Modifier.fillMaxWidth().padding(vertical = 5.dp)) { Text(label, Modifier.weight(1f), fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal); Text(value, fontWeight = if (bold) FontWeight.ExtraBold else FontWeight.Medium) }
}

@Composable
private fun ShopScreen(addProduct: () -> Unit) {
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) { Text("My Shop", style = MaterialTheme.typography.headlineLarge); Text("Pem's Handcraft", color = Wine, fontWeight = FontWeight.SemiBold) }
                Button(onClick = addProduct, shape = RoundedCornerShape(14.dp)) { Icon(Icons.Default.Add, null); Text(" Product") }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MetricCard("12", "Products", Icons.Outlined.Inventory2, Modifier.weight(1f))
                MetricCard("28", "Orders", Icons.Outlined.ReceiptLong, Modifier.weight(1f))
                MetricCard("4.9", "Rating", Icons.Outlined.Star, Modifier.weight(1f))
            }
        }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = Wine), shape = RoundedCornerShape(22.dp)) {
                Row(Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) { Text("This month's sales", color = Color.White.copy(.75f)); Text("Nu. 18,450", color = Color.White, fontSize = 27.sp, fontWeight = FontWeight.ExtraBold); Text("↑ 12% from last month", color = Gold, fontSize = 12.sp) }
                    Icon(Icons.Default.TrendingUp, null, tint = Gold, modifier = Modifier.size(42.dp))
                }
            }
        }
        item { Text("Your products", style = MaterialTheme.typography.titleLarge) }
        items(products.take(3)) { product ->
            Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(18.dp)) {
                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    ProductArtwork(product, Modifier.size(78.dp), 30)
                    Column(Modifier.padding(horizontal = 12.dp).weight(1f)) { Text(product.name, fontWeight = FontWeight.Bold); Text("Nu. ${product.price}  •  In stock", color = Sage, fontSize = 13.sp); Text("${product.rating} ★", color = Gold, fontWeight = FontWeight.Bold) }
                    IconButton(onClick = {}) { Icon(Icons.Default.MoreVert, "Options") }
                }
            }
        }
    }
}

@Composable
private fun MetricCard(value: String, label: String, icon: ImageVector, modifier: Modifier) {
    Card(modifier, colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(18.dp)) {
        Column(Modifier.padding(14.dp)) { Icon(icon, null, tint = Wine); Spacer(Modifier.height(8.dp)); Text(value, fontWeight = FontWeight.ExtraBold, fontSize = 21.sp); Text(label, color = Color.Gray, fontSize = 11.sp) }
    }
}

@Composable
private fun AddProductScreen(back: () -> Unit) {
    var name by remember { mutableStateOf("") }; var price by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }; var category by remember { mutableStateOf("Textiles") }
    var expanded by remember { mutableStateOf(false) }; var saved by remember { mutableStateOf(false) }
    LazyColumn(contentPadding = PaddingValues(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = back) { Icon(Icons.Default.ArrowBack, "Back") }
                Text("Add a new product", style = MaterialTheme.typography.headlineSmall)
            }
        }
        item {
            Card(onClick = {}, Modifier.fillMaxWidth().height(170.dp), shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = Wine.copy(alpha = .07f))) {
                Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                    Icon(Icons.Outlined.AddPhotoAlternate, null, tint = Wine, modifier = Modifier.size(42.dp)); Text("Add product photos", fontWeight = FontWeight.Bold); Text("Use clear photos from different angles", color = Color.Gray, fontSize = 12.sp)
                }
            }
        }
        item { FormField("Product name", name, { name = it }, "e.g., Handwoven Yathra Bag") }
        item {
            ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
                OutlinedTextField(category, {}, readOnly = true, label = { Text("Category") }, trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) }, modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth(), shape = RoundedCornerShape(15.dp))
                ExposedDropdownMenu(expanded, { expanded = false }) { listOf("Textiles", "Crafts", "Home", "Food", "Wellness").forEach { DropdownMenuItem({ Text(it) }, { category = it; expanded = false }) } }
            }
        }
        item { FormField("Price (Nu.)", price, { price = it }, "0", KeyboardType.Number) }
        item { FormField("Description", description, { description = it }, "Tell buyers what makes this product special", minLines = 4) }
        item {
            Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Outlined.VerifiedUser, null, tint = Sage); Spacer(Modifier.width(10.dp)); Text("Your verified seller profile will be shown with this listing.", color = Color.DarkGray, fontSize = 13.sp) }
        }
        item {
            Button(onClick = { saved = name.isNotBlank() && price.isNotBlank() }, enabled = name.isNotBlank() && price.isNotBlank(), modifier = Modifier.fillMaxWidth().height(54.dp), shape = RoundedCornerShape(16.dp)) { Text("Publish product") }
            if (saved) { Spacer(Modifier.height(8.dp)); Text("Product published successfully!", color = Sage, fontWeight = FontWeight.Bold) }
        }
    }
}

@Composable
private fun FormField(label: String, value: String, change: (String) -> Unit, placeholder: String, type: KeyboardType = KeyboardType.Text, minLines: Int = 1) {
    OutlinedTextField(value, change, Modifier.fillMaxWidth(), label = { Text(label) }, placeholder = { Text(placeholder) }, keyboardOptions = KeyboardOptions(keyboardType = type), minLines = minLines, shape = RoundedCornerShape(15.dp))
}
