package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.AuthManager
import com.example.data.UserRole
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read string from context matches TS LuxeWear`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("TS LuxeWear", appName)
    }

    @Test
    fun `super admin whitelist verification`() {
        // Only Shakir and Stalha are super administrators
        assertTrue(AuthManager.isSuperAdminEmail("shakirsir2122@gmail.com"))
        assertTrue(AuthManager.isSuperAdminEmail("stalha2110@gmail.com"))
        
        // Any modification, trailing space, or casing works safely
        assertTrue(AuthManager.isSuperAdminEmail(" ShakirSir2122@gmail.com "))
        
        // Random emails never get super_admin access
        assertFalse(AuthManager.isSuperAdminEmail("hackadmin@gmail.com"))
        assertFalse(AuthManager.isSuperAdminEmail("stalha@gmail.com"))
        assertFalse(AuthManager.isSuperAdminEmail("customer@gmail.com"))
    }

    @Test
    fun `authenticating as super admin requires approved email`() {
        AuthManager.logout()
        assertNull(AuthManager.currentUserFlow.value)

        // Attempting to log in with an unauthorized email as Super Admin fails
        var callCount = 0
        val success = AuthManager.performGoogleLogin("fraud@gmail.com", UserRole.SUPER_ADMIN, onError = { error ->
            callCount++
            assertTrue(error.contains("Unauthorized Access"))
        })
        
        assertFalse(success)
        assertEquals(1, callCount)
        assertNull(AuthManager.currentUserFlow.value)

        // Attempting to login with approved email as Customer gets redirected to Customer role
        val loggedCustomer = AuthManager.performGoogleLogin("customer@gmail.com", UserRole.CUSTOMER, onError = {})
        assertTrue(loggedCustomer)
        assertEquals(UserRole.CUSTOMER, AuthManager.currentUserFlow.value?.role)

        // Attempting to login with approved email shakirsir2122@gmail.com automatically upgrades to Super Admin role
        val loggedAdmin = AuthManager.performGoogleLogin("shakirsir2122@gmail.com", UserRole.CUSTOMER, onError = {})
        assertTrue(loggedAdmin)
        assertEquals(UserRole.SUPER_ADMIN, AuthManager.currentUserFlow.value?.role)
    }

    @Test
    fun `continue as guest initializes guest state correctly`() {
        AuthManager.logout()
        AuthManager.continueAsGuest()
        
        val user = AuthManager.currentUserFlow.value
        assertNotNull(user)
        assertEquals("Guest User", user?.displayName)
        assertEquals(UserRole.GUEST, user?.role)
    }

    @Test
    fun `product sharing link navigates directly to specific product even for guests`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        com.example.data.TSLuxeWearRepository.initDatabase(context)
        
        // Register mock product since we start database empty
        val mockStore = com.example.model.Store(
            id = "store_priya",
            name = "Priya Boutique",
            ownerName = "Priya Sharma",
            ownerPhone = "+919876543210",
            ownerWhatsapp = "919876543210",
            logoUrl = "🌸",
            bannerColor = 0xFF8E244D,
            status = "Active",
            storeUrl = "http://tsluxewear.com/priya_boutique",
            addressMapLink = "https://maps.google.com/?q=Priya+Boutique+Mumbai",
            storeType = "Boutique Partywear",
            categories = listOf("Sarees", "Kurtis", "Dresses", "Ethnic Wear"),
            followersCount = 1420
        )
        val mockProduct = com.example.model.Product(
            id = "prod_priya_1",
            storeId = "store_priya",
            storeName = "Priya Boutique",
            name = "Georgette Zari Embroidered Saree",
            category = "Sarees",
            price = 2499.0,
            discountPrice = 1999.0,
            description = "Stunning Georgette saree",
            fabric = "Faux Georgette",
            sizes = listOf("Free Size"),
            colors = listOf("Cobalt Blue"),
            stockQuantity = 12,
            lowStockThreshold = 4,
            imageUrl = "👘"
        )
        com.example.data.TSLuxeWearRepository.addStore(mockStore)
        com.example.data.TSLuxeWearRepository.addProduct(mockProduct)

        // Logged out / guest / anonymous user
        AuthManager.logout()
        assertNull(AuthManager.currentUserFlow.value)
        
        // Product Link with query params containing store_id and product_id
        val testUrl = "https://myapp.com/product?storeId=store_priya&productId=prod_priya_1"
        
        var productCaptured: com.example.model.Product? = null
        var routeCaptured: String? = null
        
        handleUrlNavigationInput(
            urlInput = testUrl,
            currentUser = null,
            onBlocked = { _, _ -> },
            onGranted = { targetRoute ->
                routeCaptured = targetRoute
            },
            onProductShared = { prod ->
                productCaptured = prod
            }
        )
        
        assertEquals("customer_home", routeCaptured)
        assertNotNull(productCaptured)
        assertEquals("prod_priya_1", productCaptured?.id)
        assertEquals("store_priya", productCaptured?.storeId)
    }

    @Test
    fun `product sharing link works with alternative rest path segments`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        com.example.data.TSLuxeWearRepository.initDatabase(context)
        
        // Register mock product since we start database empty
        val mockStore = com.example.model.Store(
            id = "store_priya",
            name = "Priya Boutique",
            ownerName = "Priya Sharma",
            ownerPhone = "+919876543210",
            ownerWhatsapp = "919876543210",
            logoUrl = "🌸",
            bannerColor = 0xFF8E244D,
            status = "Active",
            storeUrl = "http://tsluxewear.com/priya_boutique",
            addressMapLink = "https://maps.google.com/?q=Priya+Boutique+Mumbai",
            storeType = "Boutique Partywear",
            categories = listOf("Sarees", "Kurtis", "Dresses", "Ethnic Wear"),
            followersCount = 1420
        )
        val mockProduct = com.example.model.Product(
            id = "prod_priya_1",
            storeId = "store_priya",
            storeName = "Priya Boutique",
            name = "Georgette Zari Embroidered Saree",
            category = "Sarees",
            price = 2499.0,
            discountPrice = 1999.0,
            description = "Stunning Georgette saree",
            fabric = "Faux Georgette",
            sizes = listOf("Free Size"),
            colors = listOf("Cobalt Blue"),
            stockQuantity = 12,
            lowStockThreshold = 4,
            imageUrl = "👘"
        )
        com.example.data.TSLuxeWearRepository.addStore(mockStore)
        com.example.data.TSLuxeWearRepository.addProduct(mockProduct)

        // Product Link with restful style path containing store_id and product_id
        val restUrl = "myapp.com/store/store_priya/product/prod_priya_1"
        
        var productCaptured: com.example.model.Product? = null
        var routeCaptured: String? = null
        
        handleUrlNavigationInput(
            urlInput = restUrl,
            currentUser = null,
            onBlocked = { _, _ -> },
            onGranted = { targetRoute ->
                routeCaptured = targetRoute
            },
            onProductShared = { prod ->
                productCaptured = prod
            }
        )
        
        assertEquals("customer_home", routeCaptured)
        assertNotNull(productCaptured)
        assertEquals("prod_priya_1", productCaptured?.id)
        assertEquals("store_priya", productCaptured?.storeId)
    }
}
