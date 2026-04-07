package com.pranav.punecityguide.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.pranav.punecityguide.data.model.Expense
import com.pranav.punecityguide.ui.theme.*

@Composable
fun TotalBudgetCard(total: Double, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        shadowElevation = 12.dp,
        border = BorderStroke(1.dp, CostPilotCyan.copy(alpha = 0.2f))
    ) {
        Box(
            modifier = Modifier
                .background(Brush.linearGradient(GradientPrimary))
                .padding(28.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    "TRAVEL BUDGET",
                    color = Color.White.copy(alpha = 0.7f),
                    style = MaterialTheme.typography.labelMedium,
                    letterSpacing = 2.sp,
                    fontWeight = FontWeight.Bold
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("$", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Light, color = Color.White)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "%,.2f".format(total),
                        style = MaterialTheme.typography.displayMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                        letterSpacing = (-1.5).sp
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .background(Color.White.copy(alpha = 0.15f), CircleShape)
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Box(modifier = Modifier.size(6.dp).background(CostPilotSuccess, CircleShape))
                    Spacer(Modifier.width(6.dp))
                    Text("Budget Tracking Active", style = MaterialTheme.typography.labelSmall, color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun ExpenseItemCard(expense: Expense, onDelete: () -> Unit, modifier: Modifier = Modifier) {
    val icon = when(expense.category.lowercase()) {
        "food" -> Icons.Default.Restaurant
        "travel" -> Icons.Default.LocalTaxi
        "entry fee" -> Icons.Default.ConfirmationNumber
        "shopping" -> Icons.Default.LocalMall
        "accommodation" -> Icons.Default.Hotel
        "transport" -> Icons.Default.DirectionsBus
        "entertainment" -> Icons.Default.TheaterComedy
        "drinks" -> Icons.Default.LocalCafe
        else -> Icons.Default.Payments
    }
    val catColor = when(expense.category.lowercase()) {
        "food" -> CatFood
        "travel", "transport" -> CatTransport
        "entry fee" -> CatAccommodation
        "shopping" -> CatShopping
        "accommodation" -> CatAccommodation
        "entertainment" -> CatEntertainment
        "drinks" -> CatDrinks
        else -> CatOthers
    }

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.08f)),
        shadowElevation = 2.dp,
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = catColor.copy(alpha = 0.1f),
                modifier = Modifier.size(48.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = null, tint = catColor, modifier = Modifier.size(22.dp))
                }
            }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(expense.title, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyLarge, maxLines = 1)
                Text(
                    expense.category.uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    letterSpacing = 1.sp
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("$${"%,.0f".format(expense.amount)}", fontWeight = FontWeight.Black, style = MaterialTheme.typography.titleMedium)
                IconButton(onClick = onDelete, modifier = Modifier.size(24.dp).offset(y = 4.dp)) {
                    Icon(Icons.Default.RemoveCircleOutline, contentDescription = "Delete", modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.error.copy(alpha = 0.4f))
                }
            }
        }
    }
}

@Composable
fun EmptyExpenseState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth().padding(48.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            shape = CircleShape,
            color = CostPilotCyan.copy(alpha = 0.08f),
            modifier = Modifier.size(80.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    Icons.Default.Payments,
                    null,
                    modifier = Modifier.size(36.dp),
                    tint = CostPilotCyan.copy(alpha = 0.3f)
                )
            }
        }
        Spacer(Modifier.height(24.dp))
        Text("No expenses yet", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold)
        Text(
            "Your travel expenses will appear here.",
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun AddExpenseDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, Double, String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var selectedCat by remember { mutableStateOf("Food") }
    val categories = listOf("Food", "Transport", "Accommodation", "Shopping", "Entertainment", "Drinks", "Others")

    val isValidAmount = amount.toDoubleOrNull()?.let { it > 0 } ?: false
    val canSubmit = title.isNotBlank() && isValidAmount

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp,
            modifier = Modifier.fillMaxWidth().padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(28.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                Text("New Expense", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold)

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("DESCRIPTION", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = CostPilotCyan)
                    TextField(
                        value = title,
                        onValueChange = { title = it },
                        placeholder = { Text("e.g. Hotel stay in Bangkok") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            disabledContainerColor = Color.Transparent
                        )
                    )
                }

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "AMOUNT ($)",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (amount.isNotEmpty() && !isValidAmount) MaterialTheme.colorScheme.error else CostPilotCyan
                    )
                    TextField(
                        value = amount,
                        onValueChange = { input ->
                            if (input.all { it.isDigit() || it == '.' }) amount = input
                        },
                        placeholder = { Text("0.00") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            disabledContainerColor = Color.Transparent
                        ),
                        isError = amount.isNotEmpty() && !isValidAmount
                    )
                }

                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("CATEGORY", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = CostPilotCyan)
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        categories.forEach { cat ->
                            FilterChip(
                                selected = selectedCat == cat,
                                onClick = { selectedCat = cat },
                                label = { Text(cat) },
                                shape = RoundedCornerShape(12.dp),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = CostPilotCyan,
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(16.dp)
                    ) {
                        Text("CANCEL", fontWeight = FontWeight.Bold)
                    }
                    Button(
                        onClick = {
                            val amt = amount.toDoubleOrNull() ?: 0.0
                            if (canSubmit) onConfirm(title, amt, selectedCat)
                        },
                        enabled = canSubmit,
                        modifier = Modifier.weight(1.5f),
                        shape = RoundedCornerShape(16.dp),
                        contentPadding = PaddingValues(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = CostPilotCyan,
                            disabledContainerColor = CostPilotCyan.copy(alpha = 0.3f)
                        )
                    ) {
                        Text("LOG EXPENSE", fontWeight = FontWeight.ExtraBold)
                    }
                }
            }
        }
    }
}
