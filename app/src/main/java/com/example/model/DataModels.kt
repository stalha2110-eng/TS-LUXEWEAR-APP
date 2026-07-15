package com.example.model

data class Store(
    val id: String,
    val name: String,
    val ownerName: String,
    val ownerPhone: String,
    val ownerWhatsapp: String,
    val logoUrl: String, // String icon code or name
    val bannerColor: Long, // Color tint
    val status: String, // "Active", "Suspended"
    val storeUrl: String,
    val addressMapLink: String,
    val storeType: String,
    val categories: List<String>,
    val followersCount: Int,
    val createdAt: Long = System.currentTimeMillis()
)

data class Product(
    val id: String,
    val storeId: String,
    val storeName: String,
    val name: String,
    val category: String,
    val price: Double,
    val discountPrice: Double? = null,
    val description: String,
    val fabric: String,
    val sizes: List<String>,
    val colors: List<String>,
    val stockQuantity: Int,
    val lowStockThreshold: Int = 5,
    val imageUrl: String // Placeholder descriptor / icon
)

data class Order(
    val orderId: String,
    val customerName: String,
    val customerPhone: String,
    val customerAddress: String,
    val productId: String,
    val productName: String,
    val productPrice: Double,
    val productSize: String,
    val productColor: String,
    val productImageUrl: String,
    val storeId: String,
    val storeName: String,
    val timestamp: Long = System.currentTimeMillis(),
    val orderStatus: String, // "Pending", "Confirmed", "Packed", "Shipped", "Delivered", "Cancelled"
    val deliveryCharge: Double = 0.0,
    val isCod: Boolean = true,
    val invoiceId: String? = null,
    val statusHistory: List<String> = listOf("Pending:${System.currentTimeMillis()}")
)

data class Inquiry(
    val id: String,
    val customerId: String,
    val customerName: String,
    val productId: String,
    val productName: String,
    val storeId: String,
    val storeName: String,
    val question: String,
    val answer: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val status: String, // "New", "Replied", "Resolved"
    val chatHistory: List<String> = emptyList() // Format: "SENDER:text:timestamp"
)

data class StoreFollower(
    val email: String,
    val storeId: String
)

data class Offer(
    val id: String,
    val storeId: String,
    val title: String,
    val description: String,
    val discountPercent: Int,
    val code: String
)

data class Complaint(
    val id: String,
    val fromUser: String,
    val userRole: String,
    val subject: String,
    val detail: String,
    val timestamp: Long = System.currentTimeMillis(),
    val status: String = "Pending"
)

data class ProductReview(
    val id: String,
    val productId: String,
    val reviewerName: String,
    val rating: Int,
    val feedback: String,
    val timestamp: Long = System.currentTimeMillis()
)

data class StockHistory(
    val id: String,
    val storeId: String,
    val productId: String,
    val productName: String,
    val previousStock: Int,
    val newStock: Int,
    val description: String,
    val timestamp: Long = System.currentTimeMillis()
)

data class ClientRequest(
    val id: String,
    val storeId: String,
    val customerName: String,
    val requestType: String, // "Category" or "Product" or "Out of Stock Product"
    val details: String,
    val timestamp: Long = System.currentTimeMillis()
)

data class StoreOrderSettings(
    val storeId: String = "",
    val acceptOrders: Boolean = true,
    val deliveryChargeOn: Boolean = false,
    val deliveryCharge: Double = 50.0,
    val codAvailable: Boolean = true,
    val returnPolicyEnabled: Boolean = true,
    val invoicePrefixChoice: String = "STORE_PREFIX", // "STORE_PREFIX-YEAR-MONTH-NUMBER"
    val resetInvoiceYearly: Boolean = false
)

data class ChatMessage(
    val id: String,
    val storeId: String,
    val customerName: String,
    val sender: String, // "Customer" or "StoreOwner"
    val messageText: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isPinned: Boolean = false,
    val isArchived: Boolean = false,
    val isUnread: Boolean = false,
    val productLink: String? = null,
    val imageUrl: String? = null
)

data class ScalabilityMetric(
    val storesCount: Int,
    val customersCount: Int,
    val productsCount: Int,
    val ordersCount: Int,
    val inquiriesCount: Int,
    val notificationsSentCount: Int,
    val queriesExecuted: Int,
    val msQueryTimeNoIndex: Double,
    val msQueryTimeIndexed: Double,
    val speedupFactor: Double
)


