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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val Wine = Color(0xFF8F3035)
private val WineDark = Color(0xFF5D1F25)
private val Gold = Color(0xFFE4A93B)
private val Cream = Color(0xFFFFF9F0)
private val Ink = Color(0xFF2C2523)
private val Sage = Color(0xFF59735C)
private val SoftRose = Color(0xFFF7E8E6)

private val dzongkhags = listOf(
    "Bumthang", "Chhukha", "Dagana", "Gasa", "Haa", "Lhuentse", "Mongar", "Paro", "Pemagatshel", "Punakha",
    "Samdrup Jongkhar", "Samtse", "Sarpang", "Thimphu", "Trashigang", "Trashiyangtse", "Trongsa", "Tsirang", "Wangdue Phodrang", "Zhemgang"
)

data class Product(
    val id: Int,
    val name: String,
    val category: String,
    val price: Int,
    val rating: Double,
    val seller: String,
    val dzongkhag: String,
    val address: String,
    val stock: Int,
    val discountPercent: Int,
    val symbol: String,
    val colors: List<Color>,
    val description: String
) {
    val discountedPrice: Int
        get() = if (discountPercent <= 0) price else (price * (100 - discountPercent)) / 100
}

data class Booking(
    val product: Product,
    val quantity: Int,
    val pickupCode: String,
    val status: String = "Reserved"
)

private val sampleProducts = listOf(
    Product(1, "Handwoven Yathra Bag", "Textiles", 1250, 4.9, "Pem's Handcraft", "Bumthang", "Chamkhar Town, near main market", 8, 10, "👜", listOf(Color(0xFFB95C4B), Color(0xFFE9A64A)), "A colourful handwoven shoulder bag made with traditional Bhutanese yathra patterns."),
    Product(2, "Natural Incense Set", "Wellness", 480, 4.8, "Druk Aromas", "Thimphu", "Norzin Lam, Thimphu", 15, 0, "🌿", listOf(Color(0xFF557A5D), Color(0xFFA7C48C)), "A calming selection of locally prepared incense using aromatic Himalayan herbs."),
    Product(3, "Carved Wooden Bowl", "Home", 890, 4.7, "Zorig Woodworks", "Trashigang", "Trashigang Town, upper market", 5, 5, "🥣", listOf(Color(0xFF8B5A3C), Color(0xFFD69B61)), "A smooth, food-safe wooden bowl individually carved and finished by a local artisan."),
    Product(4, "Bhutanese Woven Scarf", "Textiles", 1600, 4.9, "Weaves of Haa", "Haa", "Haa Town, near bus stand", 6, 0, "🧣", listOf(Color(0xFF7A3F65), Color(0xFFD787A6)), "A soft statement scarf woven by hand with a contemporary interpretation of traditional motifs."),
    Product(5, "Clay Butter Lamp", "Crafts", 350, 4.6, "Deki Pottery", "Paro", "Paro Town, Tshongdue", 20, 15, "🪔", listOf(Color(0xFFAA603E), Color(0xFFF0B36D)), "A handmade clay butter lamp inspired by Bhutanese homes and sacred spaces."),
    Product(6, "Wildflower Honey", "Food", 650, 4.8, "Mountain Harvest", "Trongsa", "Trongsa Town, below dzong road", 10, 0, "🍯", listOf(Color(0xFFE09A24), Color(0xFFF7D46B)), "Pure Bhutanese wildflower honey collected in small batches from mountain apiaries.")
)

enum class Screen {
    RoleSelect,
    CustomerLogin,
    ShopkeeperLogin,
    Home,
    Explore,
    Detail,
    Bookings,
    BookingSuccess,
    Shop,
    SellerReservations,
    AddProduct,
    EditProduct
}

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
    var screen by remember { mutableStateOf(Screen.RoleSelect) }
    val products = remember { mutableStateListOf<Product>().apply { addAll(sampleProducts) } }
    var selected by remember { mutableStateOf(products.first()) }
    val bookings = remember { mutableStateListOf<Booking>() }
    var latestBooking by remember { mutableStateOf<Booking?>(null) }

    fun openProduct(product: Product) {
        selected = product
        screen = Screen.Detail
    }

    fun replaceProduct(updated: Product) {
        val index = products.indexOfFirst { it.id == updated.id }
        if (index >= 0) products[index] = updated
    }

    fun updateBookingStatus(booking: Booking, newStatus: String) {
        val index = bookings.indexOfFirst { it.pickupCode == booking.pickupCode }
        if (index < 0 || booking.status == newStatus) return

        if (newStatus == "Cancelled" && booking.status != "Cancelled") {
            val currentProduct = products.firstOrNull { it.id == booking.product.id }
            if (currentProduct != null) {
                replaceProduct(currentProduct.copy(stock = currentProduct.stock + booking.quantity))
            }
        }
        bookings[index] = booking.copy(status = newStatus)
    }

    Scaffold(
        containerColor = Cream,
        bottomBar = {
            if (screen in listOf(Screen.Home, Screen.Explore, Screen.Bookings)) {
                CustomerBottomBar(screen, bookings.size) { screen = it }
            }
            if (screen in listOf(Screen.Shop, Screen.SellerReservations)) {
                SellerBottomBar(screen) { screen = it }
            }
        }
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            when (screen) {
                Screen.RoleSelect -> RoleSelectionScreen(
                    customer = { screen = Screen.CustomerLogin },
                    shopkeeper = { screen = Screen.ShopkeeperLogin }
                )
                Screen.CustomerLogin -> LoginScreen(
                    title = "Customer login",
                    subtitle = "Discover local handmade products and reserve them for pickup.",
                    icon = Icons.Outlined.Person,
                    back = { screen = Screen.RoleSelect },
                    login = { screen = Screen.Home }
                )
                Screen.ShopkeeperLogin -> LoginScreen(
                    title = "Shopkeeper login",
                    subtitle = "Manage your products, prices, discounts, stock and customer reservations.",
                    icon = Icons.Outlined.Storefront,
                    back = { screen = Screen.RoleSelect },
                    login = { screen = Screen.Shop }
                )
                Screen.Home -> HomeScreen(products, ::openProduct) { screen = Screen.Explore }
                Screen.Explore -> ExploreScreen(products, ::openProduct)
                Screen.Detail -> ProductDetailScreen(
                    product = selected,
                    back = { screen = Screen.Home },
                    book = { quantity ->
                        if (quantity <= selected.stock) {
                            val booking = Booking(
                                product = selected,
                                quantity = quantity,
                                pickupCode = "TSH-${1000 + selected.id}${bookings.size + 1}"
                            )
                            bookings.add(booking)
                            latestBooking = booking
                            replaceProduct(selected.copy(stock = selected.stock - quantity))
                            selected = selected.copy(stock = selected.stock - quantity)
                            screen = Screen.BookingSuccess
                        }
                    }
                )
                Screen.Bookings -> BookingsScreen(bookings) { screen = Screen.Explore }
                Screen.BookingSuccess -> BookingSuccessScreen(
                    booking = latestBooking,
                    viewBookings = { screen = Screen.Bookings },
                    continueShopping = { screen = Screen.Home }
                )
                Screen.Shop -> ShopScreen(
                    products = products,
                    bookings = bookings,
                    openReservations = { screen = Screen.SellerReservations },
                    addProduct = { screen = Screen.AddProduct },
                    editProduct = { product -> selected = product; screen = Screen.EditProduct },
                    deleteProduct = { product -> products.remove(product) }
                )
                Screen.SellerReservations -> SellerReservationsScreen(
                    bookings = bookings,
                    back = { screen = Screen.Shop },
                    updateStatus = ::updateBookingStatus
                )
                Screen.AddProduct -> AddProductScreen(
                    nextId = (products.maxOfOrNull { it.id } ?: 0) + 1,
                    save = { product -> products.add(product); screen = Screen.Shop },
                    back = { screen = Screen.Shop }
                )
                Screen.EditProduct -> EditProductScreen(
                    product = selected,
                    save = { updated -> replaceProduct(updated); selected = updated; screen = Screen.Shop },
                    back = { screen = Screen.Shop }
                )
            }
        }
    }
}

@Composable
private fun RoleSelectionScreen(customer: () -> Unit, shopkeeper: () -> Unit) {
    Column(
        Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFFFFF3E4), Cream, Color.White))).padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(42.dp))
        Surface(shape = CircleShape, color = Wine, modifier = Modifier.size(76.dp), shadowElevation = 4.dp) {
            Box(contentAlignment = Alignment.Center) { Text("T", color = Color.White, fontSize = 34.sp, fontWeight = FontWeight.ExtraBold) }
        }
        Spacer(Modifier.height(18.dp))
        Text("TshongLa", fontSize = 34.sp, fontWeight = FontWeight.ExtraBold, color = Ink)
        Text("Handmade Marketplace", color = Wine, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(34.dp))
        Text("How would you like to continue?", fontSize = 22.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        Text("Choose your role to get the right experience.", color = Color.Gray, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 6.dp))
        Spacer(Modifier.height(26.dp))
        RoleCard("I'm a Customer", "Browse handmade products, show buying intention and reserve items for personal pickup.", Icons.Outlined.ShoppingBag, customer)
        Spacer(Modifier.height(16.dp))
        RoleCard("I'm a Shopkeeper", "Register products, manage prices, stock, discounts and customer reservations.", Icons.Outlined.Storefront, shopkeeper)
        Spacer(Modifier.weight(1f))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.VerifiedUser, null, tint = Sage, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp)); Text("Secure local marketplace", color = Color.Gray, fontSize = 12.sp)
        }
    }
}

@Composable
private fun RoleCard(title: String, text: String, icon: ImageVector, onClick: () -> Unit) {
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(24.dp), elevation = CardDefaults.cardElevation(3.dp)) {
        Row(Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(shape = RoundedCornerShape(18.dp), color = SoftRose, modifier = Modifier.size(64.dp)) {
                Box(contentAlignment = Alignment.Center) { Icon(icon, null, tint = Wine, modifier = Modifier.size(32.dp)) }
            }
            Column(Modifier.weight(1f).padding(horizontal = 16.dp)) {
                Text(title, fontSize = 19.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(5.dp)); Text(text, color = Color.Gray, fontSize = 13.sp, lineHeight = 18.sp)
            }
            Icon(Icons.Default.ChevronRight, null, tint = Wine)
        }
    }
}

@Composable
private fun LoginScreen(title: String, subtitle: String, icon: ImageVector, back: () -> Unit, login: () -> Unit) {
    var phone by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var showError by remember { mutableStateOf(false) }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(24.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item {
            IconButton(onClick = back) { Icon(Icons.Default.ArrowBack, "Back") }
            Spacer(Modifier.height(16.dp))
            Surface(shape = RoundedCornerShape(22.dp), color = SoftRose, modifier = Modifier.size(72.dp)) {
                Box(contentAlignment = Alignment.Center) { Icon(icon, null, tint = Wine, modifier = Modifier.size(36.dp)) }
            }
            Spacer(Modifier.height(18.dp)); Text(title, style = MaterialTheme.typography.headlineLarge); Text(subtitle, color = Color.Gray, lineHeight = 21.sp)
        }
        item {
            OutlinedTextField(phone, { phone = it; showError = false }, Modifier.fillMaxWidth(), label = { Text("Phone number") }, placeholder = { Text("Enter your phone number") }, leadingIcon = { Icon(Icons.Outlined.Phone, null) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone), shape = RoundedCornerShape(16.dp), singleLine = true)
        }
        item {
            OutlinedTextField(password, { password = it; showError = false }, Modifier.fillMaxWidth(), label = { Text("Password") }, placeholder = { Text("Enter your password") }, leadingIcon = { Icon(Icons.Outlined.Lock, null) }, visualTransformation = PasswordVisualTransformation(), shape = RoundedCornerShape(16.dp), singleLine = true)
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) { TextButton(onClick = {}) { Text("Forgot password?") } }
            Button(onClick = { if (phone.isNotBlank() && password.isNotBlank()) login() else showError = true }, modifier = Modifier.fillMaxWidth().height(54.dp), shape = RoundedCornerShape(16.dp)) { Text("Login", fontWeight = FontWeight.Bold) }
            if (showError) Text("Please enter your phone number and password.", color = MaterialTheme.colorScheme.error, fontSize = 13.sp, modifier = Modifier.padding(top = 8.dp))
        }
        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) { HorizontalDivider(Modifier.weight(1f)); Text("  New to TshongLa?  ", color = Color.Gray, fontSize = 12.sp); HorizontalDivider(Modifier.weight(1f)) }
            OutlinedButton(onClick = {}, modifier = Modifier.fillMaxWidth().padding(top = 14.dp), shape = RoundedCornerShape(16.dp)) { Text("Create an account") }
        }
    }
}

@Composable
private fun CustomerBottomBar(current: Screen, bookingCount: Int, navigate: (Screen) -> Unit) {
    NavigationBar(containerColor = Color.White, tonalElevation = 8.dp) {
        val destinations = listOf(Triple(Screen.Home, Icons.Outlined.Home, "Home"), Triple(Screen.Explore, Icons.Outlined.Search, "Explore"), Triple(Screen.Bookings, Icons.Outlined.ConfirmationNumber, "Bookings"))
        destinations.forEach { (screen, icon, label) ->
            NavigationBarItem(selected = current == screen, onClick = { navigate(screen) }, icon = { BadgedBox(badge = { if (screen == Screen.Bookings && bookingCount > 0) Badge { Text(bookingCount.toString()) } }) { Icon(icon, label) } }, label = { Text(label, fontSize = 11.sp) }, colors = NavigationBarItemDefaults.colors(indicatorColor = Wine.copy(alpha = .12f)))
        }
    }
}

@Composable
private fun SellerBottomBar(current: Screen, navigate: (Screen) -> Unit) {
    NavigationBar(containerColor = Color.White, tonalElevation = 8.dp) {
        NavigationBarItem(selected = current == Screen.Shop, onClick = { navigate(Screen.Shop) }, icon = { Icon(Icons.Outlined.Storefront, "Shop") }, label = { Text("My Shop") })
        NavigationBarItem(selected = current == Screen.SellerReservations, onClick = { navigate(Screen.SellerReservations) }, icon = { Icon(Icons.Outlined.ReceiptLong, "Reservations") }, label = { Text("Reservations") })
        NavigationBarItem(selected = false, onClick = {}, icon = { Icon(Icons.Outlined.Person, "Profile") }, label = { Text("Profile") })
    }
}

@Composable
private fun HomeScreen(products: List<Product>, openProduct: (Product) -> Unit, explore: () -> Unit) {
    LazyColumn(contentPadding = PaddingValues(bottom = 24.dp)) {
        item {
            Column(Modifier.fillMaxWidth().background(Brush.verticalGradient(listOf(Color(0xFFFFF4E2), Cream))).padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) { Text("Kuzu zangpo!", color = Wine, fontWeight = FontWeight.SemiBold); Text("Find something\nmade with meaning.", fontSize = 29.sp, lineHeight = 33.sp, fontWeight = FontWeight.ExtraBold, color = Ink) }
                    Surface(shape = CircleShape, color = Wine, modifier = Modifier.size(48.dp)) { Box(contentAlignment = Alignment.Center) { Icon(Icons.Outlined.Person, null, tint = Color.White) } }
                }
                Spacer(Modifier.height(18.dp)); SearchBar(readOnly = true, onClick = explore); Spacer(Modifier.height(18.dp)); HeroBanner(explore)
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
    Surface(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick), shape = RoundedCornerShape(24.dp), color = Wine) {
        Row(Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("LOCAL • HANDMADE • UNIQUE", color = Gold, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp)); Text("See it. Love it.\nReserve it locally.", color = Color.White, fontSize = 21.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(12.dp)); AssistChip(onClick = onClick, label = { Text("Explore products") }, trailingIcon = { Icon(Icons.Default.ArrowForward, null, Modifier.size(16.dp)) })
            }
            Text("🧺", fontSize = 68.sp)
        }
    }
}

@Composable
private fun SectionHeader(title: String, action: String, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(start = 16.dp, end = 8.dp, top = 22.dp, bottom = 12.dp), verticalAlignment = Alignment.CenterVertically) { Text(title, style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f)); TextButton(onClick = onClick) { Text(action) } }
}

@Composable
private fun CategoryRow() {
    val categories = listOf("🧵" to "Textiles", "🏺" to "Crafts", "🏠" to "Home", "🍯" to "Food", "🌿" to "Wellness")
    LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        items(categories) { (symbol, title) ->
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Surface(shape = RoundedCornerShape(20.dp), color = Color.White, shadowElevation = 2.dp, modifier = Modifier.size(68.dp)) { Box(contentAlignment = Alignment.Center) { Text(symbol, fontSize = 30.sp) } }
                Spacer(Modifier.height(6.dp)); Text(title, fontSize = 12.sp, fontWeight = FontWeight.Medium)
            }
        }
    }
}

@Composable
private fun SearchBar(readOnly: Boolean = false, onClick: (() -> Unit)? = null, value: String = "", onValueChange: (String) -> Unit = {}) {
    OutlinedTextField(value, onValueChange, Modifier.fillMaxWidth().then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier), enabled = !readOnly, readOnly = readOnly, placeholder = { Text("Search handmade products") }, leadingIcon = { Icon(Icons.Default.Search, null) }, trailingIcon = { Icon(Icons.Outlined.Tune, null) }, shape = RoundedCornerShape(18.dp), colors = OutlinedTextFieldDefaults.colors(disabledContainerColor = Color.White, disabledBorderColor = Color.Transparent, disabledTextColor = Ink, disabledPlaceholderColor = Color.Gray))
}

@Composable
private fun ProductCard(product: Product, onClick: () -> Unit) {
    Card(onClick = onClick, shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)) {
        Column {
            ProductArtwork(product, Modifier.fillMaxWidth().height(128.dp), 48)
            Column(Modifier.padding(12.dp)) {
                if (product.discountPercent > 0) AssistChip(onClick = {}, label = { Text("${product.discountPercent}% OFF", fontSize = 10.sp) })
                Text(product.name, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis, minLines = 2)
                Spacer(Modifier.height(5.dp))
                Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.Star, null, tint = Gold, modifier = Modifier.size(15.dp)); Text(" ${product.rating}", fontSize = 12.sp, color = Color.Gray) }
                Spacer(Modifier.height(7.dp)); Text("Nu. ${product.discountedPrice}", color = Wine, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
                if (product.discountPercent > 0) Text("Regular Nu. ${product.price}", color = Color.Gray, fontSize = 11.sp)
                Text("📍 ${product.dzongkhag}", color = Color.Gray, fontSize = 11.sp)
                Text(if (product.stock > 0) "${product.stock} available" else "Out of stock", color = if (product.stock > 0) Sage else MaterialTheme.colorScheme.error, fontSize = 11.sp)
            }
        }
    }
}

@Composable
private fun ProductArtwork(product: Product, modifier: Modifier, symbolSize: Int) {
    Box(modifier.clip(RoundedCornerShape(18.dp)).background(Brush.linearGradient(product.colors)), contentAlignment = Alignment.Center) {
        Text(product.symbol, fontSize = symbolSize.sp)
        Surface(Modifier.align(Alignment.TopEnd).padding(10.dp), shape = CircleShape, color = Color.White.copy(alpha = .9f)) { Icon(Icons.Outlined.FavoriteBorder, "Save", tint = Wine, modifier = Modifier.padding(7.dp).size(17.dp)) }
    }
}

@Composable
private fun ExploreScreen(products: List<Product>, openProduct: (Product) -> Unit) {
    var query by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("All") }
    var selectedDzongkhag by remember { mutableStateOf("All Bhutan") }
    var locationExpanded by remember { mutableStateOf(false) }
    val filtered = products.filter {
        (selectedCategory == "All" || it.category == selectedCategory) &&
        (selectedDzongkhag == "All Bhutan" || it.dzongkhag == selectedDzongkhag) &&
        it.name.contains(query, true)
    }
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item { Text("Explore products", style = MaterialTheme.typography.headlineLarge); Text("Browse by category and Dzongkhag, then reserve from a local seller.", color = Color.Gray) }
        item { SearchBar(value = query, onValueChange = { query = it }) }
        item {
            ExposedDropdownMenuBox(expanded = locationExpanded, onExpandedChange = { locationExpanded = it }) {
                OutlinedTextField(value = selectedDzongkhag, onValueChange = {}, readOnly = true, label = { Text("Shop by Dzongkhag") }, leadingIcon = { Icon(Icons.Outlined.LocationOn, null) }, trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(locationExpanded) }, modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth(), shape = RoundedCornerShape(16.dp))
                ExposedDropdownMenu(expanded = locationExpanded, onDismissRequest = { locationExpanded = false }) {
                    (listOf("All Bhutan") + dzongkhags).forEach { place -> DropdownMenuItem(text = { Text(place) }, onClick = { selectedDzongkhag = place; locationExpanded = false }) }
                }
            }
        }
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) { items(listOf("All", "Textiles", "Crafts", "Home", "Food", "Wellness")) { category -> FilterChip(selected = selectedCategory == category, onClick = { selectedCategory = category }, label = { Text(category) }) } }
        }
        item { Text("${filtered.size} products available", fontWeight = FontWeight.Bold) }
        items(filtered.chunked(2)) { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                row.forEach { product -> Box(Modifier.weight(1f)) { ProductCard(product) { openProduct(product) } } }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun ProductDetailScreen(product: Product, back: () -> Unit, book: (Int) -> Unit) {
    var quantity by remember { mutableIntStateOf(1) }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 18.dp)) {
        item {
            Box(Modifier.fillMaxWidth().height(300.dp)) {
                ProductArtwork(product, Modifier.fillMaxSize().clip(RoundedCornerShape(0.dp)), 96)
                SmallIconButton(Icons.Default.ArrowBack, "Back", Modifier.align(Alignment.TopStart).padding(16.dp), back)
            }
        }
        item {
            Column(Modifier.padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(product.category.uppercase(), color = Wine, fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.weight(1f))
                    if (product.discountPercent > 0) Badge(containerColor = Wine) { Text("${product.discountPercent}% OFF", modifier = Modifier.padding(5.dp)) }
                }
                Spacer(Modifier.height(6.dp)); Text(product.name, style = MaterialTheme.typography.headlineSmall)
                Spacer(Modifier.height(8.dp)); Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.Star, null, tint = Gold, modifier = Modifier.size(18.dp)); Text(" ${product.rating}  •  Verified local seller", color = Color.Gray, fontSize = 13.sp) }
                Spacer(Modifier.height(14.dp)); Text("Nu. ${product.discountedPrice}", color = Wine, fontSize = 25.sp, fontWeight = FontWeight.ExtraBold)
                if (product.discountPercent > 0) Text("Regular price: Nu. ${product.price}", color = Color.Gray, fontSize = 13.sp)
                HorizontalDivider(Modifier.padding(vertical = 18.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(shape = CircleShape, color = Wine.copy(alpha = .12f), modifier = Modifier.size(48.dp)) { Box(contentAlignment = Alignment.Center) { Icon(Icons.Default.Storefront, null, tint = Wine) } }
                    Column(Modifier.padding(start = 12.dp).weight(1f)) { Text(product.seller, fontWeight = FontWeight.Bold); Text("📍 ${product.address}, ${product.dzongkhag}", color = Color.Gray, fontSize = 13.sp) }
                    Icon(Icons.Outlined.Verified, "Verified", tint = Sage)
                }
                Spacer(Modifier.height(14.dp)); Text(if (product.stock > 0) "${product.stock} item(s) currently available" else "Currently out of stock", color = if (product.stock > 0) Sage else MaterialTheme.colorScheme.error, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(18.dp)); Text("About this product", fontWeight = FontWeight.Bold, fontSize = 18.sp); Spacer(Modifier.height(8.dp)); Text(product.description, color = Color.DarkGray, lineHeight = 22.sp)
                Spacer(Modifier.height(20.dp))
                Card(colors = CardDefaults.cardColors(containerColor = SoftRose), shape = RoundedCornerShape(18.dp)) {
                    Column(Modifier.padding(16.dp)) { Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Outlined.LocationOn, null, tint = Wine); Spacer(Modifier.width(8.dp)); Text("Pickup from seller", fontWeight = FontWeight.Bold) }; Spacer(Modifier.height(6.dp)); Text("Reserve now to show your buying intention. Collect the item in person from ${product.address}, ${product.dzongkhag}.", color = Color.DarkGray, fontSize = 13.sp, lineHeight = 19.sp) }
                }
                Spacer(Modifier.height(18.dp)); Text("Quantity", fontWeight = FontWeight.Bold)
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp)) {
                    OutlinedIconButton(onClick = { if (quantity > 1) quantity-- }) { Icon(Icons.Default.Remove, "Decrease") }
                    Text(quantity.toString(), modifier = Modifier.padding(horizontal = 22.dp), fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    OutlinedIconButton(onClick = { if (quantity < product.stock) quantity++ }, enabled = product.stock > 0) { Icon(Icons.Default.Add, "Increase") }
                }
                Spacer(Modifier.height(20.dp))
                Button(onClick = { book(quantity) }, enabled = product.stock > 0 && quantity <= product.stock, modifier = Modifier.fillMaxWidth().height(56.dp), shape = RoundedCornerShape(17.dp)) {
                    Icon(Icons.Outlined.ConfirmationNumber, null); Spacer(Modifier.width(9.dp)); Text(if (product.stock > 0) "Book for pickup • Nu. ${product.discountedPrice * quantity}" else "Out of stock", fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(8.dp)); Text("Payment can be made when collecting the item.", color = Color.Gray, fontSize = 11.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
            }
        }
    }
}

@Composable
private fun SmallIconButton(icon: ImageVector, description: String, modifier: Modifier, onClick: () -> Unit) {
    Surface(modifier = modifier, shape = CircleShape, color = Color.White.copy(alpha = .92f), shadowElevation = 2.dp) { IconButton(onClick = onClick) { Icon(icon, description) } }
}

@Composable
private fun BookingSuccessScreen(booking: Booking?, viewBookings: () -> Unit, continueShopping: () -> Unit) {
    if (booking == null) return
    Column(Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Surface(shape = CircleShape, color = Sage.copy(alpha = .12f), modifier = Modifier.size(92.dp)) { Box(contentAlignment = Alignment.Center) { Icon(Icons.Default.CheckCircle, null, tint = Sage, modifier = Modifier.size(56.dp)) } }
        Spacer(Modifier.height(22.dp)); Text("Item reserved!", fontSize = 28.sp, fontWeight = FontWeight.ExtraBold); Text("Your buying intention has been recorded.", color = Color.Gray, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 6.dp))
        Spacer(Modifier.height(22.dp))
        Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(22.dp), elevation = CardDefaults.cardElevation(2.dp)) {
            Column(Modifier.padding(20.dp).fillMaxWidth()) {
                Text(booking.product.name, fontWeight = FontWeight.Bold, fontSize = 18.sp); Text("${booking.quantity} item(s) • Nu. ${booking.product.discountedPrice * booking.quantity}", color = Wine, fontWeight = FontWeight.Bold)
                HorizontalDivider(Modifier.padding(vertical = 14.dp)); Text("Pickup location", color = Color.Gray, fontSize = 12.sp); Text("${booking.product.address}, ${booking.product.dzongkhag}", fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(14.dp)); Text("Pickup code", color = Color.Gray, fontSize = 12.sp); Text(booking.pickupCode, color = Wine, fontSize = 25.sp, fontWeight = FontWeight.ExtraBold); Text("Show this code to the shopkeeper when collecting your item.", color = Color.Gray, fontSize = 12.sp)
            }
        }
        Spacer(Modifier.height(22.dp)); Button(onClick = viewBookings, modifier = Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(16.dp)) { Text("View my bookings") }; OutlinedButton(onClick = continueShopping, modifier = Modifier.fillMaxWidth().padding(top = 10.dp), shape = RoundedCornerShape(16.dp)) { Text("Continue shopping") }
    }
}

@Composable
private fun BookingsScreen(bookings: List<Booking>, explore: () -> Unit) {
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item { Text("My bookings", style = MaterialTheme.typography.headlineLarge); Text("Items you have reserved for personal pickup.", color = Color.Gray) }
        if (bookings.isEmpty()) {
            item {
                Column(Modifier.fillParentMaxHeight(.65f).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) { Text("🎟️", fontSize = 72.sp); Text("No bookings yet", fontSize = 21.sp, fontWeight = FontWeight.Bold); Text("Reserve an item and it will appear here.", color = Color.Gray); Spacer(Modifier.height(18.dp)); Button(onClick = explore) { Text("Explore products") } }
            }
        } else {
            items(bookings) { booking ->
                Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(20.dp), elevation = CardDefaults.cardElevation(2.dp)) {
                    Column(Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) { ProductArtwork(booking.product, Modifier.size(76.dp), 30); Column(Modifier.padding(start = 12.dp).weight(1f)) { Text(booking.product.name, fontWeight = FontWeight.Bold); Text("Qty ${booking.quantity} • Nu. ${booking.product.discountedPrice * booking.quantity}", color = Wine, fontWeight = FontWeight.SemiBold); Text("📍 ${booking.product.dzongkhag}", color = Color.Gray, fontSize = 12.sp) } }
                        HorizontalDivider(Modifier.padding(vertical = 12.dp)); Row(verticalAlignment = Alignment.CenterVertically) { BookingStatusChip(booking.status); Spacer(Modifier.weight(1f)); Column(horizontalAlignment = Alignment.End) { Text("Pickup code", color = Color.Gray, fontSize = 10.sp); Text(booking.pickupCode, color = Wine, fontWeight = FontWeight.ExtraBold) } }
                    }
                }
            }
        }
    }
}

@Composable
private fun BookingStatusChip(status: String) {
    val icon = when (status) {
        "Ready for pickup" -> Icons.Outlined.Inventory2
        "Collected" -> Icons.Outlined.CheckCircle
        "Cancelled" -> Icons.Outlined.Cancel
        else -> Icons.Outlined.Schedule
    }
    AssistChip(onClick = {}, label = { Text(status) }, leadingIcon = { Icon(icon, null, Modifier.size(16.dp)) })
}

@Composable
private fun ShopScreen(
    products: List<Product>,
    bookings: List<Booking>,
    openReservations: () -> Unit,
    addProduct: () -> Unit,
    editProduct: (Product) -> Unit,
    deleteProduct: (Product) -> Unit
) {
    var productToDelete by remember { mutableStateOf<Product?>(null) }
    val activeReservations = bookings.count { it.status == "Reserved" || it.status == "Ready for pickup" }

    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) { Text("My Shop", style = MaterialTheme.typography.headlineLarge); Row(verticalAlignment = Alignment.CenterVertically) { Text("Pem's Handcraft", color = Wine, fontWeight = FontWeight.SemiBold); Spacer(Modifier.width(5.dp)); Icon(Icons.Outlined.Verified, null, tint = Sage, modifier = Modifier.size(17.dp)) } }
                Button(onClick = addProduct, shape = RoundedCornerShape(14.dp)) { Icon(Icons.Default.Add, null); Text(" Register product") }
            }
        }
        item {
            Card(onClick = openReservations, colors = CardDefaults.cardColors(containerColor = SoftRose), shape = RoundedCornerShape(20.dp)) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    BadgedBox(badge = { if (activeReservations > 0) Badge { Text(activeReservations.toString()) } }) { Icon(Icons.Outlined.ConfirmationNumber, null, tint = Wine, modifier = Modifier.size(32.dp)) }
                    Column(Modifier.padding(start = 12.dp).weight(1f)) {
                        Text("Pickup reservations", fontWeight = FontWeight.Bold)
                        Text(if (activeReservations > 0) "$activeReservations reservation(s) need your attention." else "No active pickup reservations right now.", color = Color.Gray, fontSize = 12.sp)
                    }
                    TextButton(onClick = openReservations) { Text("View") }
                }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MetricCard(products.size.toString(), "Products", Icons.Outlined.Inventory2, Modifier.weight(1f))
                MetricCard(products.sumOf { it.stock }.toString(), "In stock", Icons.Outlined.Warehouse, Modifier.weight(1f))
                MetricCard(products.count { it.discountPercent > 0 }.toString(), "Discounts", Icons.Outlined.LocalOffer, Modifier.weight(1f))
            }
        }
        item { Text("Your products", style = MaterialTheme.typography.titleLarge) }
        items(products, key = { it.id }) { product ->
            SellerProductCard(product = product, edit = { editProduct(product) }, delete = { productToDelete = product })
        }
    }
    productToDelete?.let { product ->
        AlertDialog(onDismissRequest = { productToDelete = null }, icon = { Icon(Icons.Outlined.Delete, null) }, title = { Text("Delete product?") }, text = { Text("${product.name} will be removed from your shop and customers will no longer be able to find it.") }, confirmButton = { TextButton(onClick = { deleteProduct(product); productToDelete = null }) { Text("Delete", color = MaterialTheme.colorScheme.error) } }, dismissButton = { TextButton(onClick = { productToDelete = null }) { Text("Cancel") } })
    }
}

@Composable
private fun SellerReservationsScreen(
    bookings: List<Booking>,
    back: () -> Unit,
    updateStatus: (Booking, String) -> Unit
) {
    var selectedFilter by remember { mutableStateOf("Active") }
    val visible = when (selectedFilter) {
        "Active" -> bookings.filter { it.status == "Reserved" || it.status == "Ready for pickup" }
        "Completed" -> bookings.filter { it.status == "Collected" }
        "Cancelled" -> bookings.filter { it.status == "Cancelled" }
        else -> bookings
    }

    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = back) { Icon(Icons.Default.ArrowBack, "Back") }
                Column {
                    Text("Reservations", style = MaterialTheme.typography.headlineLarge)
                    Text("Manage customer pickup bookings.", color = Color.Gray)
                }
            }
        }
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(listOf("Active", "Completed", "Cancelled", "All")) { filter ->
                    FilterChip(selected = selectedFilter == filter, onClick = { selectedFilter = filter }, label = { Text(filter) })
                }
            }
        }
        if (visible.isEmpty()) {
            item {
                Column(Modifier.fillParentMaxHeight(.55f).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                    Icon(Icons.Outlined.ReceiptLong, null, tint = Wine.copy(alpha = .55f), modifier = Modifier.size(64.dp))
                    Spacer(Modifier.height(12.dp)); Text("No $selectedFilter reservations", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Text("New customer pickup bookings will appear here.", color = Color.Gray, textAlign = TextAlign.Center)
                }
            }
        } else {
            items(visible, key = { it.pickupCode }) { booking ->
                SellerReservationCard(booking = booking, updateStatus = { newStatus -> updateStatus(booking, newStatus) })
            }
        }
    }
}

@Composable
private fun SellerReservationCard(booking: Booking, updateStatus: (String) -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(20.dp), elevation = CardDefaults.cardElevation(2.dp)) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                ProductArtwork(booking.product, Modifier.size(76.dp), 30)
                Column(Modifier.padding(start = 12.dp).weight(1f)) {
                    Text(booking.product.name, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                    Text("Qty ${booking.quantity} • Nu. ${booking.product.discountedPrice * booking.quantity}", color = Wine, fontWeight = FontWeight.SemiBold)
                    Text("📍 ${booking.product.address}, ${booking.product.dzongkhag}", color = Color.Gray, fontSize = 12.sp, maxLines = 2)
                }
                BookingStatusChip(booking.status)
            }
            HorizontalDivider(Modifier.padding(vertical = 12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Pickup code", color = Color.Gray, fontSize = 11.sp)
                    Text(booking.pickupCode, color = Wine, fontWeight = FontWeight.ExtraBold, fontSize = 19.sp)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("Customer", color = Color.Gray, fontSize = 11.sp)
                    Text("Verified customer", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                }
            }
            if (booking.status == "Reserved") {
                Spacer(Modifier.height(14.dp))
                Button(onClick = { updateStatus("Ready for pickup") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp)) {
                    Icon(Icons.Outlined.Inventory2, null); Spacer(Modifier.width(7.dp)); Text("Mark ready for pickup")
                }
                TextButton(onClick = { updateStatus("Cancelled") }, modifier = Modifier.fillMaxWidth()) { Text("Cancel reservation", color = MaterialTheme.colorScheme.error) }
            } else if (booking.status == "Ready for pickup") {
                Spacer(Modifier.height(14.dp))
                Button(onClick = { updateStatus("Collected") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp), colors = ButtonDefaults.buttonColors(containerColor = Sage)) {
                    Icon(Icons.Outlined.CheckCircle, null); Spacer(Modifier.width(7.dp)); Text("Mark as collected")
                }
                TextButton(onClick = { updateStatus("Cancelled") }, modifier = Modifier.fillMaxWidth()) { Text("Cancel reservation", color = MaterialTheme.colorScheme.error) }
            }
        }
    }
}

@Composable
private fun SellerProductCard(product: Product, edit: () -> Unit, delete: () -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(20.dp), elevation = CardDefaults.cardElevation(2.dp)) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                ProductArtwork(product, Modifier.size(84.dp), 31)
                Column(Modifier.padding(horizontal = 12.dp).weight(1f)) {
                    Text(product.name, fontWeight = FontWeight.Bold)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Nu. ${product.discountedPrice}", color = Wine, fontWeight = FontWeight.ExtraBold)
                        if (product.discountPercent > 0) { Spacer(Modifier.width(8.dp)); Badge(containerColor = Wine) { Text("${product.discountPercent}% OFF") } }
                    }
                    Text("Stock: ${product.stock}", color = if (product.stock > 0) Sage else MaterialTheme.colorScheme.error, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    Text("📍 ${product.dzongkhag}", color = Color.Gray, fontSize = 12.sp)
                }
            }
            HorizontalDivider(Modifier.padding(vertical = 10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(onClick = edit, modifier = Modifier.weight(1f), shape = RoundedCornerShape(13.dp)) { Icon(Icons.Outlined.Edit, null, Modifier.size(17.dp)); Spacer(Modifier.width(6.dp)); Text("Edit") }
                OutlinedButton(onClick = delete, modifier = Modifier.weight(1f), shape = RoundedCornerShape(13.dp), colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)) { Icon(Icons.Outlined.Delete, null, Modifier.size(17.dp)); Spacer(Modifier.width(6.dp)); Text("Delete") }
            }
        }
    }
}

@Composable
private fun MetricCard(value: String, label: String, icon: ImageVector, modifier: Modifier) {
    Card(modifier, colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(18.dp)) { Column(Modifier.padding(14.dp)) { Icon(icon, null, tint = Wine); Spacer(Modifier.height(8.dp)); Text(value, fontWeight = FontWeight.ExtraBold, fontSize = 21.sp); Text(label, color = Color.Gray, fontSize = 11.sp) } }
}

@Composable
private fun AddProductScreen(nextId: Int, save: (Product) -> Unit, back: () -> Unit) {
    ProductFormScreen(title = "Register a product", initial = null, nextId = nextId, save = save, back = back)
}

@Composable
private fun EditProductScreen(product: Product, save: (Product) -> Unit, back: () -> Unit) {
    ProductFormScreen(title = "Edit product", initial = product, nextId = product.id, save = save, back = back)
}

@Composable
private fun ProductFormScreen(title: String, initial: Product?, nextId: Int, save: (Product) -> Unit, back: () -> Unit) {
    var name by remember(initial) { mutableStateOf(initial?.name ?: "") }
    var price by remember(initial) { mutableStateOf(initial?.price?.toString() ?: "") }
    var stock by remember(initial) { mutableStateOf(initial?.stock?.toString() ?: "") }
    var discount by remember(initial) { mutableStateOf(initial?.discountPercent?.toString() ?: "0") }
    var description by remember(initial) { mutableStateOf(initial?.description ?: "") }
    var category by remember(initial) { mutableStateOf(initial?.category ?: "Textiles") }
    var dzongkhag by remember(initial) { mutableStateOf(initial?.dzongkhag ?: "Thimphu") }
    var address by remember(initial) { mutableStateOf(initial?.address ?: "") }
    var categoryExpanded by remember { mutableStateOf(false) }
    var locationExpanded by remember { mutableStateOf(false) }
    var showError by remember { mutableStateOf(false) }

    val valid = name.isNotBlank() && price.toIntOrNull() != null && stock.toIntOrNull() != null && address.isNotBlank() && (discount.toIntOrNull() ?: -1) in 0..90

    LazyColumn(contentPadding = PaddingValues(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item { Row(verticalAlignment = Alignment.CenterVertically) { IconButton(onClick = back) { Icon(Icons.Default.ArrowBack, "Back") }; Text(title, style = MaterialTheme.typography.headlineSmall) } }
        item {
            Card(onClick = {}, modifier = Modifier.fillMaxWidth().height(150.dp), shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = Wine.copy(alpha = .07f))) {
                Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) { Icon(Icons.Outlined.AddPhotoAlternate, null, tint = Wine, modifier = Modifier.size(42.dp)); Text(if (initial == null) "Add product photos" else "Change product photos", fontWeight = FontWeight.Bold); Text("Use clear photos from different angles", color = Color.Gray, fontSize = 12.sp) }
            }
        }
        item { FormField("Product name", name, { name = it }, "e.g., Handwoven Yathra Bag") }
        item {
            ExposedDropdownMenuBox(expanded = categoryExpanded, onExpandedChange = { categoryExpanded = it }) {
                OutlinedTextField(category, {}, readOnly = true, label = { Text("Category") }, trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(categoryExpanded) }, modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth(), shape = RoundedCornerShape(15.dp))
                ExposedDropdownMenu(expanded = categoryExpanded, onDismissRequest = { categoryExpanded = false }) { listOf("Textiles", "Crafts", "Home", "Food", "Wellness").forEach { item -> DropdownMenuItem(text = { Text(item) }, onClick = { category = item; categoryExpanded = false }) } }
            }
        }
        item { FormField("Regular price (Nu.)", price, { price = it.filter(Char::isDigit) }, "0", KeyboardType.Number) }
        item { FormField("Available stock", stock, { stock = it.filter(Char::isDigit) }, "e.g., 10", KeyboardType.Number) }
        item { FormField("Discount (%)", discount, { discount = it.filter(Char::isDigit).take(2) }, "0", KeyboardType.Number) }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = SoftRose), shape = RoundedCornerShape(18.dp)) {
                Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Outlined.LocalOffer, null, tint = Wine); Spacer(Modifier.width(10.dp)); Column { Text("Discount preview", fontWeight = FontWeight.Bold); val p = price.toIntOrNull() ?: 0; val d = discount.toIntOrNull() ?: 0; Text(if (d in 1..90) "Customer price: Nu. ${(p * (100 - d)) / 100} ($d% off)" else "No discount applied", color = Color.DarkGray, fontSize = 13.sp) } }
            }
        }
        item { Text("Shop location", fontWeight = FontWeight.Bold, fontSize = 18.sp); Text("Choose the Dzongkhag and enter the pickup address. GPS is not required.", color = Color.Gray, fontSize = 12.sp) }
        item {
            ExposedDropdownMenuBox(expanded = locationExpanded, onExpandedChange = { locationExpanded = it }) {
                OutlinedTextField(dzongkhag, {}, readOnly = true, label = { Text("Dzongkhag") }, leadingIcon = { Icon(Icons.Outlined.LocationOn, null) }, trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(locationExpanded) }, modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth(), shape = RoundedCornerShape(15.dp))
                ExposedDropdownMenu(expanded = locationExpanded, onDismissRequest = { locationExpanded = false }) { dzongkhags.forEach { place -> DropdownMenuItem(text = { Text(place) }, onClick = { dzongkhag = place; locationExpanded = false }) } }
            }
        }
        item { FormField("Pickup address", address, { address = it }, "Town, street, landmark or shop address", minLines = 2) }
        item { FormField("Description", description, { description = it }, "Tell buyers what makes this product special", minLines = 4) }
        item {
            Button(onClick = {
                if (valid) {
                    save(Product(id = initial?.id ?: nextId, name = name.trim(), category = category, price = price.toInt(), rating = initial?.rating ?: 0.0, seller = initial?.seller ?: "Pem's Handcraft", dzongkhag = dzongkhag, address = address.trim(), stock = stock.toInt(), discountPercent = discount.toIntOrNull() ?: 0, symbol = initial?.symbol ?: "🎁", colors = initial?.colors ?: listOf(Color(0xFFB95C4B), Color(0xFFE9A64A)), description = description.trim()))
                } else showError = true
            }, modifier = Modifier.fillMaxWidth().height(54.dp), shape = RoundedCornerShape(16.dp)) { Text(if (initial == null) "Register product" else "Save changes", fontWeight = FontWeight.Bold) }
            if (showError && !valid) Text("Please complete the required fields. Discount must be between 0% and 90%.", color = MaterialTheme.colorScheme.error, fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp))
        }
    }
}

@Composable
private fun FormField(label: String, value: String, change: (String) -> Unit, placeholder: String, type: KeyboardType = KeyboardType.Text, minLines: Int = 1) {
    OutlinedTextField(value = value, onValueChange = change, modifier = Modifier.fillMaxWidth(), label = { Text(label) }, placeholder = { Text(placeholder) }, keyboardOptions = KeyboardOptions(keyboardType = type), minLines = minLines, shape = RoundedCornerShape(15.dp))
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun RoleSelectionPreview() { TshongLaTheme { RoleSelectionScreen({}, {}) } }

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun CustomerLoginPreview() { TshongLaTheme { LoginScreen("Customer login", "Discover local handmade products and reserve them for pickup.", Icons.Outlined.Person, {}, {}) } }

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun CustomerHomePreview() { TshongLaTheme { HomeScreen(sampleProducts, {}, {}) } }

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun ProductDetailPreview() { TshongLaTheme { ProductDetailScreen(sampleProducts.first(), {}, {}) } }

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun ShopPreview() { TshongLaTheme { ShopScreen(sampleProducts, emptyList(), {}, {}, {}, {}) } }

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun SellerReservationsPreview() {
    TshongLaTheme {
        SellerReservationsScreen(
            bookings = listOf(Booking(sampleProducts.first(), 2, "TSH-10011", "Ready for pickup")),
            back = {},
            updateStatus = { _, _ -> }
        )
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun AddProductPreview() { TshongLaTheme { AddProductScreen(7, {}, {}) } }
