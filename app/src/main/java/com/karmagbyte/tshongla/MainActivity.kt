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

data class Booking(
    val product: Product,
    val quantity: Int,
    val pickupCode: String,
    val status: String = "Reserved for pickup"
)

private val products = listOf(
    Product(1, "Handwoven Yathra Bag", "Textiles", 1250, 4.9, "Pem's Handcraft", "Bumthang", "👜", listOf(Color(0xFFB95C4B), Color(0xFFE9A64A)), "A colourful handwoven shoulder bag made with traditional Bhutanese yathra patterns."),
    Product(2, "Natural Incense Set", "Wellness", 480, 4.8, "Druk Aromas", "Thimphu", "🌿", listOf(Color(0xFF557A5D), Color(0xFFA7C48C)), "A calming selection of locally prepared incense using aromatic Himalayan herbs."),
    Product(3, "Carved Wooden Bowl", "Home", 890, 4.7, "Zorig Woodworks", "Trashigang", "🥣", listOf(Color(0xFF8B5A3C), Color(0xFFD69B61)), "A smooth, food-safe wooden bowl individually carved and finished by a local artisan."),
    Product(4, "Bhutanese Woven Scarf", "Textiles", 1600, 4.9, "Weaves of Haa", "Haa", "🧣", listOf(Color(0xFF7A3F65), Color(0xFFD787A6)), "A soft statement scarf woven by hand with a contemporary interpretation of traditional motifs."),
    Product(5, "Clay Butter Lamp", "Crafts", 350, 4.6, "Deki Pottery", "Paro", "🪔", listOf(Color(0xFFAA603E), Color(0xFFF0B36D)), "A handmade clay butter lamp inspired by Bhutanese homes and sacred spaces."),
    Product(6, "Wildflower Honey", "Food", 650, 4.8, "Mountain Harvest", "Trongsa", "🍯", listOf(Color(0xFFE09A24), Color(0xFFF7D46B)), "Pure Bhutanese wildflower honey collected in small batches from mountain apiaries.")
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
    AddProduct
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
    var selected by remember { mutableStateOf(products.first()) }
    val bookings = remember { mutableStateListOf<Booking>() }
    var latestBooking by remember { mutableStateOf<Booking?>(null) }

    fun openProduct(product: Product) {
        selected = product
        screen = Screen.Detail
    }

    Scaffold(
        containerColor = Cream,
        bottomBar = {
            if (screen in listOf(Screen.Home, Screen.Explore, Screen.Bookings)) {
                CustomerBottomBar(screen, bookings.size) { screen = it }
            }
            if (screen == Screen.Shop) {
                SellerBottomBar()
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
                    subtitle = "Manage your products, reservations and local customers.",
                    icon = Icons.Outlined.Storefront,
                    back = { screen = Screen.RoleSelect },
                    login = { screen = Screen.Shop }
                )
                Screen.Home -> HomeScreen(::openProduct) { screen = Screen.Explore }
                Screen.Explore -> ExploreScreen(::openProduct)
                Screen.Detail -> ProductDetailScreen(
                    product = selected,
                    back = { screen = Screen.Home },
                    book = { quantity ->
                        val booking = Booking(
                            product = selected,
                            quantity = quantity,
                            pickupCode = "TSH-${1000 + selected.id}${bookings.size + 1}"
                        )
                        bookings.add(booking)
                        latestBooking = booking
                        screen = Screen.BookingSuccess
                    }
                )
                Screen.Bookings -> BookingsScreen(bookings) { screen = Screen.Explore }
                Screen.BookingSuccess -> BookingSuccessScreen(
                    booking = latestBooking,
                    viewBookings = { screen = Screen.Bookings },
                    continueShopping = { screen = Screen.Home }
                )
                Screen.Shop -> ShopScreen { screen = Screen.AddProduct }
                Screen.AddProduct -> AddProductScreen { screen = Screen.Shop }
            }
        }
    }
}

@Composable
private fun RoleSelectionScreen(customer: () -> Unit, shopkeeper: () -> Unit) {
    Column(
        Modifier.fillMaxSize().background(
            Brush.verticalGradient(listOf(Color(0xFFFFF3E4), Cream, Color.White))
        ).padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(42.dp))
        Surface(shape = CircleShape, color = Wine, modifier = Modifier.size(76.dp), shadowElevation = 4.dp) {
            Box(contentAlignment = Alignment.Center) {
                Text("T", color = Color.White, fontSize = 34.sp, fontWeight = FontWeight.ExtraBold)
            }
        }
        Spacer(Modifier.height(18.dp))
        Text("TshongLa", fontSize = 34.sp, fontWeight = FontWeight.ExtraBold, color = Ink)
        Text("Handmade Marketplace", color = Wine, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(34.dp))
        Text("How would you like to continue?", fontSize = 22.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        Text("Choose your role to get the right experience.", color = Color.Gray, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 6.dp))
        Spacer(Modifier.height(26.dp))
        RoleCard(
            title = "I'm a Customer",
            text = "Browse handmade products, show buying intention and reserve items for personal pickup.",
            icon = Icons.Outlined.ShoppingBag,
            onClick = customer
        )
        Spacer(Modifier.height(16.dp))
        RoleCard(
            title = "I'm a Shopkeeper",
            text = "Display your products, manage your shop and handle customer reservations.",
            icon = Icons.Outlined.Storefront,
            onClick = shopkeeper
        )
        Spacer(Modifier.weight(1f))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.VerifiedUser, null, tint = Sage, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text("Secure local marketplace", color = Color.Gray, fontSize = 12.sp)
        }
    }
}

@Composable
private fun RoleCard(title: String, text: String, icon: ImageVector, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(3.dp)
    ) {
        Row(Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(shape = RoundedCornerShape(18.dp), color = SoftRose, modifier = Modifier.size(64.dp)) {
                Box(contentAlignment = Alignment.Center) { Icon(icon, null, tint = Wine, modifier = Modifier.size(32.dp)) }
            }
            Column(Modifier.weight(1f).padding(horizontal = 16.dp)) {
                Text(title, fontSize = 19.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(5.dp))
                Text(text, color = Color.Gray, fontSize = 13.sp, lineHeight = 18.sp)
            }
            Icon(Icons.Default.ChevronRight, null, tint = Wine)
        }
    }
}

@Composable
private fun LoginScreen(
    title: String,
    subtitle: String,
    icon: ImageVector,
    back: () -> Unit,
    login: () -> Unit
) {
    var phone by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var showError by remember { mutableStateOf(false) }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            IconButton(onClick = back) { Icon(Icons.Default.ArrowBack, "Back") }
            Spacer(Modifier.height(16.dp))
            Surface(shape = RoundedCornerShape(22.dp), color = SoftRose, modifier = Modifier.size(72.dp)) {
                Box(contentAlignment = Alignment.Center) { Icon(icon, null, tint = Wine, modifier = Modifier.size(36.dp)) }
            }
            Spacer(Modifier.height(18.dp))
            Text(title, style = MaterialTheme.typography.headlineLarge)
            Text(subtitle, color = Color.Gray, lineHeight = 21.sp)
        }
        item {
            OutlinedTextField(
                value = phone,
                onValueChange = { phone = it; showError = false },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Phone number") },
                placeholder = { Text("Enter your phone number") },
                leadingIcon = { Icon(Icons.Outlined.Phone, null) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                shape = RoundedCornerShape(16.dp),
                singleLine = true
            )
        }
        item {
            OutlinedTextField(
                value = password,
                onValueChange = { password = it; showError = false },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Password") },
                placeholder = { Text("Enter your password") },
                leadingIcon = { Icon(Icons.Outlined.Lock, null) },
                visualTransformation = PasswordVisualTransformation(),
                shape = RoundedCornerShape(16.dp),
                singleLine = true
            )
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = {}) { Text("Forgot password?") }
            }
            Button(
                onClick = {
                    if (phone.isNotBlank() && password.isNotBlank()) login() else showError = true
                },
                modifier = Modifier.fillMaxWidth().height(54.dp),
                shape = RoundedCornerShape(16.dp)
            ) { Text("Login", fontWeight = FontWeight.Bold) }
            if (showError) {
                Text("Please enter your phone number and password.", color = MaterialTheme.colorScheme.error, fontSize = 13.sp, modifier = Modifier.padding(top = 8.dp))
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                HorizontalDivider(Modifier.weight(1f))
                Text("  New to TshongLa?  ", color = Color.Gray, fontSize = 12.sp)
                HorizontalDivider(Modifier.weight(1f))
            }
            OutlinedButton(onClick = {}, modifier = Modifier.fillMaxWidth().padding(top = 14.dp), shape = RoundedCornerShape(16.dp)) {
                Text("Create an account")
            }
        }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = Sage.copy(alpha = .08f)), shape = RoundedCornerShape(18.dp)) {
                Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Security, null, tint = Sage)
                    Spacer(Modifier.width(10.dp))
                    Text("Identity and phone verification can be connected to the registration flow later.", color = Color.DarkGray, fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
private fun CustomerBottomBar(current: Screen, bookingCount: Int, navigate: (Screen) -> Unit) {
    NavigationBar(containerColor = Color.White, tonalElevation = 8.dp) {
        val destinations = listOf(
            Triple(Screen.Home, Icons.Outlined.Home, "Home"),
            Triple(Screen.Explore, Icons.Outlined.Search, "Explore"),
            Triple(Screen.Bookings, Icons.Outlined.ConfirmationNumber, "Bookings")
        )
        destinations.forEach { (screen, icon, label) ->
            NavigationBarItem(
                selected = current == screen,
                onClick = { navigate(screen) },
                icon = {
                    BadgedBox(badge = {
                        if (screen == Screen.Bookings && bookingCount > 0) Badge { Text(bookingCount.toString()) }
                    }) { Icon(icon, label) }
                },
                label = { Text(label, fontSize = 11.sp) },
                colors = NavigationBarItemDefaults.colors(indicatorColor = Wine.copy(alpha = .12f))
            )
        }
    }
}

@Composable
private fun SellerBottomBar() {
    NavigationBar(containerColor = Color.White, tonalElevation = 8.dp) {
        NavigationBarItem(selected = true, onClick = {}, icon = { Icon(Icons.Outlined.Storefront, "Shop") }, label = { Text("My Shop") })
        NavigationBarItem(selected = false, onClick = {}, icon = { Icon(Icons.Outlined.ReceiptLong, "Orders") }, label = { Text("Reservations") })
        NavigationBarItem(selected = false, onClick = {}, icon = { Icon(Icons.Outlined.Person, "Profile") }, label = { Text("Profile") })
    }
}

@Composable
private fun HomeScreen(openProduct: (Product) -> Unit, explore: () -> Unit) {
    LazyColumn(contentPadding = PaddingValues(bottom = 24.dp)) {
        item {
            Column(
                Modifier.fillMaxWidth().background(Brush.verticalGradient(listOf(Color(0xFFFFF4E2), Cream))).padding(20.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Kuzu zangpo!", color = Wine, fontWeight = FontWeight.SemiBold)
                        Text("Find something\nmade with meaning.", fontSize = 29.sp, lineHeight = 33.sp, fontWeight = FontWeight.ExtraBold, color = Ink)
                    }
                    Surface(shape = CircleShape, color = Wine, modifier = Modifier.size(48.dp)) {
                        Box(contentAlignment = Alignment.Center) { Icon(Icons.Outlined.Person, null, tint = Color.White) }
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
    Surface(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick), shape = RoundedCornerShape(24.dp), color = Wine) {
        Row(Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("LOCAL • HANDMADE • UNIQUE", color = Gold, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                Text("See it. Love it.\nReserve it locally.", color = Color.White, fontSize = 21.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(12.dp))
                AssistChip(onClick = onClick, label = { Text("Explore products") }, trailingIcon = { Icon(Icons.Default.ArrowForward, null, Modifier.size(16.dp)) })
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
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth().then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        enabled = !readOnly,
        readOnly = readOnly,
        placeholder = { Text("Search handmade products") },
        leadingIcon = { Icon(Icons.Default.Search, null) },
        trailingIcon = { Icon(Icons.Outlined.Tune, null) },
        shape = RoundedCornerShape(18.dp),
        colors = OutlinedTextFieldDefaults.colors(
            disabledContainerColor = Color.White,
            disabledBorderColor = Color.Transparent,
            disabledTextColor = Ink,
            disabledPlaceholderColor = Color.Gray
        )
    )
}

@Composable
private fun ProductCard(product: Product, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
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
                Text("📍 ${product.location}", color = Color.Gray, fontSize = 11.sp)
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
        item {
            Text("Explore products", style = MaterialTheme.typography.headlineLarge)
            Text("Choose an item and reserve it directly from a local shopkeeper.", color = Color.Gray)
        }
        item { SearchBar(value = query, onValueChange = { query = it }) }
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(listOf("All", "Textiles", "Crafts", "Home", "Food", "Wellness")) { category ->
                    FilterChip(selected = selectedCategory == category, onClick = { selectedCategory = category }, label = { Text(category) })
                }
            }
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
                Text(product.category.uppercase(), color = Wine, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                Spacer(Modifier.height(6.dp))
                Text(product.name, style = MaterialTheme.typography.headlineSmall)
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Star, null, tint = Gold, modifier = Modifier.size(18.dp))
                    Text(" ${product.rating}  •  Verified local seller", color = Color.Gray, fontSize = 13.sp)
                }
                Spacer(Modifier.height(14.dp))
                Text("Nu. ${product.price}", color = Wine, fontSize = 25.sp, fontWeight = FontWeight.ExtraBold)
                HorizontalDivider(Modifier.padding(vertical = 18.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(shape = CircleShape, color = Wine.copy(alpha = .12f), modifier = Modifier.size(48.dp)) {
                        Box(contentAlignment = Alignment.Center) { Icon(Icons.Default.Storefront, null, tint = Wine) }
                    }
                    Column(Modifier.padding(start = 12.dp).weight(1f)) {
                        Text(product.seller, fontWeight = FontWeight.Bold)
                        Text("📍 ${product.location}, Bhutan", color = Color.Gray, fontSize = 13.sp)
                    }
                    Icon(Icons.Outlined.Verified, "Verified", tint = Sage)
                }
                Spacer(Modifier.height(18.dp))
                Text("About this product", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Spacer(Modifier.height(8.dp))
                Text(product.description, color = Color.DarkGray, lineHeight = 22.sp)
                Spacer(Modifier.height(20.dp))
                Card(colors = CardDefaults.cardColors(containerColor = SoftRose), shape = RoundedCornerShape(18.dp)) {
                    Column(Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.LocationOn, null, tint = Wine)
                            Spacer(Modifier.width(8.dp))
                            Text("Pickup from seller", fontWeight = FontWeight.Bold)
                        }
                        Spacer(Modifier.height(6.dp))
                        Text("Reserve now to show your buying intention. The seller will keep the item for you to collect in person from ${product.location}.", color = Color.DarkGray, fontSize = 13.sp, lineHeight = 19.sp)
                    }
                }
                Spacer(Modifier.height(18.dp))
                Text("Quantity", fontWeight = FontWeight.Bold)
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp)) {
                    OutlinedIconButton(onClick = { if (quantity > 1) quantity-- }) { Icon(Icons.Default.Remove, "Decrease") }
                    Text(quantity.toString(), modifier = Modifier.padding(horizontal = 22.dp), fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    OutlinedIconButton(onClick = { quantity++ }) { Icon(Icons.Default.Add, "Increase") }
                }
                Spacer(Modifier.height(20.dp))
                Button(onClick = { book(quantity) }, modifier = Modifier.fillMaxWidth().height(56.dp), shape = RoundedCornerShape(17.dp)) {
                    Icon(Icons.Outlined.ConfirmationNumber, null)
                    Spacer(Modifier.width(9.dp))
                    Text("Book for pickup • Nu. ${product.price * quantity}", fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(8.dp))
                Text("No online payment in this prototype. Payment can be made when collecting the item.", color = Color.Gray, fontSize = 11.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
            }
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
private fun BookingSuccessScreen(booking: Booking?, viewBookings: () -> Unit, continueShopping: () -> Unit) {
    if (booking == null) return
    Column(
        Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(shape = CircleShape, color = Sage.copy(alpha = .12f), modifier = Modifier.size(92.dp)) {
            Box(contentAlignment = Alignment.Center) { Icon(Icons.Default.CheckCircle, null, tint = Sage, modifier = Modifier.size(56.dp)) }
        }
        Spacer(Modifier.height(22.dp))
        Text("Item reserved!", fontSize = 28.sp, fontWeight = FontWeight.ExtraBold)
        Text("Your buying intention has been recorded.", color = Color.Gray, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 6.dp))
        Spacer(Modifier.height(22.dp))
        Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(22.dp), elevation = CardDefaults.cardElevation(2.dp)) {
            Column(Modifier.padding(20.dp).fillMaxWidth()) {
                Text(booking.product.name, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Text("${booking.quantity} item(s) • Nu. ${booking.product.price * booking.quantity}", color = Wine, fontWeight = FontWeight.Bold)
                HorizontalDivider(Modifier.padding(vertical = 14.dp))
                Text("Pickup location", color = Color.Gray, fontSize = 12.sp)
                Text("${booking.product.seller}, ${booking.product.location}", fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(14.dp))
                Text("Pickup code", color = Color.Gray, fontSize = 12.sp)
                Text(booking.pickupCode, color = Wine, fontSize = 25.sp, fontWeight = FontWeight.ExtraBold)
                Text("Show this code to the shopkeeper when collecting your item.", color = Color.Gray, fontSize = 12.sp)
            }
        }
        Spacer(Modifier.height(22.dp))
        Button(onClick = viewBookings, modifier = Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(16.dp)) { Text("View my bookings") }
        OutlinedButton(onClick = continueShopping, modifier = Modifier.fillMaxWidth().padding(top = 10.dp), shape = RoundedCornerShape(16.dp)) { Text("Continue shopping") }
    }
}

@Composable
private fun BookingsScreen(bookings: List<Booking>, explore: () -> Unit) {
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item {
            Text("My bookings", style = MaterialTheme.typography.headlineLarge)
            Text("Items you have reserved for personal pickup.", color = Color.Gray)
        }
        if (bookings.isEmpty()) {
            item {
                Column(
                    Modifier.fillParentMaxHeight(.65f).fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text("🎟️", fontSize = 72.sp)
                    Text("No bookings yet", fontSize = 21.sp, fontWeight = FontWeight.Bold)
                    Text("Reserve an item and it will appear here.", color = Color.Gray)
                    Spacer(Modifier.height(18.dp))
                    Button(onClick = explore) { Text("Explore products") }
                }
            }
        } else {
            items(bookings) { booking ->
                Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(20.dp), elevation = CardDefaults.cardElevation(2.dp)) {
                    Column(Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            ProductArtwork(booking.product, Modifier.size(76.dp), 30)
                            Column(Modifier.padding(start = 12.dp).weight(1f)) {
                                Text(booking.product.name, fontWeight = FontWeight.Bold)
                                Text("Qty ${booking.quantity} • Nu. ${booking.product.price * booking.quantity}", color = Wine, fontWeight = FontWeight.SemiBold)
                                Text("📍 ${booking.product.location}", color = Color.Gray, fontSize = 12.sp)
                            }
                        }
                        HorizontalDivider(Modifier.padding(vertical = 12.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            AssistChip(onClick = {}, label = { Text(booking.status) }, leadingIcon = { Icon(Icons.Default.CheckCircle, null, Modifier.size(16.dp)) })
                            Spacer(Modifier.weight(1f))
                            Column(horizontalAlignment = Alignment.End) {
                                Text("Pickup code", color = Color.Gray, fontSize = 10.sp)
                                Text(booking.pickupCode, color = Wine, fontWeight = FontWeight.ExtraBold)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ShopScreen(addProduct: () -> Unit) {
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("My Shop", style = MaterialTheme.typography.headlineLarge)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Pem's Handcraft", color = Wine, fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.width(5.dp)); Icon(Icons.Outlined.Verified, null, tint = Sage, modifier = Modifier.size(17.dp))
                    }
                }
                Button(onClick = addProduct, shape = RoundedCornerShape(14.dp)) { Icon(Icons.Default.Add, null); Text(" Product") }
            }
        }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = SoftRose), shape = RoundedCornerShape(20.dp)) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.ConfirmationNumber, null, tint = Wine, modifier = Modifier.size(32.dp))
                    Column(Modifier.padding(start = 12.dp).weight(1f)) {
                        Text("3 new pickup reservations", fontWeight = FontWeight.Bold)
                        Text("Customers have shown buying intention for your items.", color = Color.Gray, fontSize = 12.sp)
                    }
                    TextButton(onClick = {}) { Text("View") }
                }
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
                    Column(Modifier.weight(1f)) {
                        Text("This month's sales", color = Color.White.copy(.75f))
                        Text("Nu. 18,450", color = Color.White, fontSize = 27.sp, fontWeight = FontWeight.ExtraBold)
                        Text("↑ 12% from last month", color = Gold, fontSize = 12.sp)
                    }
                    Icon(Icons.Default.TrendingUp, null, tint = Gold, modifier = Modifier.size(42.dp))
                }
            }
        }
        item { Text("Your products", style = MaterialTheme.typography.titleLarge) }
        items(products.take(3)) { product ->
            Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(18.dp)) {
                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    ProductArtwork(product, Modifier.size(78.dp), 30)
                    Column(Modifier.padding(horizontal = 12.dp).weight(1f)) {
                        Text(product.name, fontWeight = FontWeight.Bold)
                        Text("Nu. ${product.price}  •  In stock", color = Sage, fontSize = 13.sp)
                        Text("${product.rating} ★", color = Gold, fontWeight = FontWeight.Bold)
                    }
                    IconButton(onClick = {}) { Icon(Icons.Default.MoreVert, "Options") }
                }
            }
        }
    }
}

@Composable
private fun MetricCard(value: String, label: String, icon: ImageVector, modifier: Modifier) {
    Card(modifier, colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(18.dp)) {
        Column(Modifier.padding(14.dp)) {
            Icon(icon, null, tint = Wine)
            Spacer(Modifier.height(8.dp))
            Text(value, fontWeight = FontWeight.ExtraBold, fontSize = 21.sp)
            Text(label, color = Color.Gray, fontSize = 11.sp)
        }
    }
}

@Composable
private fun AddProductScreen(back: () -> Unit) {
    var name by remember { mutableStateOf("") }
    var price by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Textiles") }
    var expanded by remember { mutableStateOf(false) }
    var saved by remember { mutableStateOf(false) }

    LazyColumn(contentPadding = PaddingValues(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = back) { Icon(Icons.Default.ArrowBack, "Back") }
                Text("Add a new product", style = MaterialTheme.typography.headlineSmall)
            }
        }
        item {
            Card(onClick = {}, modifier = Modifier.fillMaxWidth().height(170.dp), shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = Wine.copy(alpha = .07f))) {
                Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                    Icon(Icons.Outlined.AddPhotoAlternate, null, tint = Wine, modifier = Modifier.size(42.dp))
                    Text("Add product photos", fontWeight = FontWeight.Bold)
                    Text("Use clear photos from different angles", color = Color.Gray, fontSize = 12.sp)
                }
            }
        }
        item { FormField("Product name", name, { name = it }, "e.g., Handwoven Yathra Bag") }
        item {
            ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
                OutlinedTextField(
                    value = category,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Category") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                    modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth(),
                    shape = RoundedCornerShape(15.dp)
                )
                ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    listOf("Textiles", "Crafts", "Home", "Food", "Wellness").forEach {
                        DropdownMenuItem(text = { Text(it) }, onClick = { category = it; expanded = false })
                    }
                }
            }
        }
        item { FormField("Price (Nu.)", price, { price = it }, "0", KeyboardType.Number) }
        item { FormField("Description", description, { description = it }, "Tell buyers what makes this product special", minLines = 4) }
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.VerifiedUser, null, tint = Sage)
                Spacer(Modifier.width(10.dp))
                Text("Your verified seller profile will be shown with this listing.", color = Color.DarkGray, fontSize = 13.sp)
            }
        }
        item {
            Button(
                onClick = { saved = name.isNotBlank() && price.isNotBlank() },
                enabled = name.isNotBlank() && price.isNotBlank(),
                modifier = Modifier.fillMaxWidth().height(54.dp),
                shape = RoundedCornerShape(16.dp)
            ) { Text("Publish product") }
            if (saved) {
                Spacer(Modifier.height(8.dp))
                Text("Product published successfully!", color = Sage, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun FormField(
    label: String,
    value: String,
    change: (String) -> Unit,
    placeholder: String,
    type: KeyboardType = KeyboardType.Text,
    minLines: Int = 1
) {
    OutlinedTextField(
        value = value,
        onValueChange = change,
        modifier = Modifier.fillMaxWidth(),
        label = { Text(label) },
        placeholder = { Text(placeholder) },
        keyboardOptions = KeyboardOptions(keyboardType = type),
        minLines = minLines,
        shape = RoundedCornerShape(15.dp)
    )
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun RoleSelectionPreview() {
    TshongLaTheme { RoleSelectionScreen({}, {}) }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun CustomerLoginPreview() {
    TshongLaTheme {
        LoginScreen(
            title = "Customer login",
            subtitle = "Discover local handmade products and reserve them for pickup.",
            icon = Icons.Outlined.Person,
            back = {},
            login = {}
        )
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun CustomerHomePreview() {
    TshongLaTheme { HomeScreen({}, {}) }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun ProductDetailPreview() {
    TshongLaTheme { ProductDetailScreen(products.first(), {}, {}) }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun ShopPreview() {
    TshongLaTheme { ShopScreen {} }
}
