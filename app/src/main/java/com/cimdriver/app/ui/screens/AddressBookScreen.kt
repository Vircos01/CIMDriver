package com.cimdriver.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.cimdriver.app.data.local.entity.SavedAddress
import com.cimdriver.app.ui.viewmodel.AddressBookViewModel
import kotlinx.coroutines.launch
import androidx.compose.ui.res.stringResource
import com.cimdriver.app.R
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts

@Composable
fun AddressBookScreen(
    onOpenDrawer: () -> Unit = {},
    onAddAddress: () -> Unit,
    onEditAddress: (Long) -> Unit,
    viewModel: AddressBookViewModel = hiltViewModel()
) {
    val addresses by viewModel.addresses.collectAsState()
    val context = androidx.compose.ui.platform.LocalContext.current
    var searchQuery by remember { mutableStateOf("") }
    
    var addressToDelete by remember { mutableStateOf<SavedAddress?>(null) }
    var deleteErrorMsg by remember { mutableStateOf<String?>(null) }
    var isSearchVisible by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    
    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            viewModel.importAddressesFromCsv(
                context = context,
                uri = it,
                onSuccess = { count ->
                    android.widget.Toast.makeText(context, context.getString(R.string.import_addresses_success, count), android.widget.Toast.LENGTH_SHORT).show()
                },
                onError = {
                    android.widget.Toast.makeText(context, context.getString(R.string.import_addresses_error), android.widget.Toast.LENGTH_SHORT).show()
                }
            )
        }
    }
    
    val filteredAddresses = addresses.filter { 
        it.label.contains(searchQuery, ignoreCase = true) || 
        it.address.contains(searchQuery, ignoreCase = true) 
    }

    Scaffold(
        topBar = {
            CimDriverTopAppBar(
                onOpenDrawer = onOpenDrawer,
                title = {
                    androidx.compose.animation.AnimatedVisibility(
                        visible = isSearchVisible,
                        enter = androidx.compose.animation.fadeIn() + androidx.compose.animation.expandHorizontally(),
                        exit = androidx.compose.animation.fadeOut() + androidx.compose.animation.shrinkHorizontally()
                    ) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(end = 8.dp),
                            placeholder = { Text(stringResource(R.string.search_ellipsis), color = com.cimdriver.app.ui.theme.CIMDriverWhite.copy(alpha = 0.7f)) },
                            singleLine = true,
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = androidx.compose.ui.graphics.Color.Transparent,
                                unfocusedContainerColor = androidx.compose.ui.graphics.Color.Transparent,
                                focusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
                                unfocusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
                                focusedTextColor = com.cimdriver.app.ui.theme.CIMDriverWhite,
                                unfocusedTextColor = com.cimdriver.app.ui.theme.CIMDriverWhite,
                                cursorColor = com.cimdriver.app.ui.theme.CIMDriverWhite
                            )
                        )
                    }
                    if (!isSearchVisible) {
                        Text(stringResource(R.string.address_book), color = com.cimdriver.app.ui.theme.CIMDriverWhite)
                    }
                },
                actions = {
                    if (!isSearchVisible) {
                        IconButton(onClick = { importLauncher.launch(arrayOf("text/csv", "text/comma-separated-values", "application/csv", "text/*")) }) {
                            Icon(Icons.Filled.FileUpload, contentDescription = stringResource(R.string.import_addresses), tint = com.cimdriver.app.ui.theme.CIMDriverWhite)
                        }
                        IconButton(onClick = { isSearchVisible = true }) {
                            Icon(Icons.Filled.Search, contentDescription = stringResource(R.string.search), tint = com.cimdriver.app.ui.theme.CIMDriverWhite)
                        }
                    } else {
                        IconButton(onClick = { 
                            isSearchVisible = false
                            searchQuery = ""
                        }) {
                            Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.cancel), tint = com.cimdriver.app.ui.theme.CIMDriverWhite)
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddAddress) {
                Icon(Icons.Filled.Add, stringResource(R.string.add_address))
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            if (filteredAddresses.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(if (addresses.isEmpty()) stringResource(R.string.no_addresses_saved) else stringResource(R.string.no_search_results))
                }
            } else {
                LazyColumn(contentPadding = PaddingValues(16.dp)) {
                    items(filteredAddresses, key = { it.id }) { address ->
                        com.cimdriver.app.ui.components.SwipeToDeleteContainer(
                            modifier = Modifier.animateItem(),
                            onDelete = {
                                coroutineScope.launch {
                                    val canDelete = viewModel.canDeleteAddress(address.address)
                                    if (canDelete) {
                                        addressToDelete = address
                                    } else {
                                        deleteErrorMsg = context.getString(R.string.delete_address_error)
                                    }
                                }
                            },
                            backgroundPadding = PaddingValues(bottom = 8.dp)
                        ) {
                            Card(
                                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                                onClick = { onEditAddress(address.id) }
                            ) {
                            Row(
                                modifier = Modifier.padding(16.dp).fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(address.label, style = MaterialTheme.typography.titleMedium)
                                    Text(address.address, style = MaterialTheme.typography.bodyMedium)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        // Adrestype badge
                                        val adresTypeTekst = when (address.addressType) {
                                            "THUIS" -> stringResource(R.string.home)
                                            "WERK" -> stringResource(R.string.work)
                                            "KLANT" -> stringResource(R.string.customer)
                                            else -> when {
                                                address.isHomeLocation -> stringResource(R.string.home)
                                                address.isWorkLocation -> stringResource(R.string.work)
                                                address.isCustomerLocation -> stringResource(R.string.customer)
                                                else -> null
                                            }
                                        }
                                        val adresTypeBadgeColor = when (address.addressType) {
                                            "THUIS" -> MaterialTheme.colorScheme.primary
                                            "WERK" -> MaterialTheme.colorScheme.tertiary
                                            "KLANT" -> MaterialTheme.colorScheme.secondary
                                            else -> when {
                                                address.isHomeLocation -> MaterialTheme.colorScheme.primary
                                                address.isWorkLocation -> MaterialTheme.colorScheme.tertiary
                                                address.isCustomerLocation -> MaterialTheme.colorScheme.secondary
                                                else -> null
                                            }
                                        }
                                        if (adresTypeTekst != null && adresTypeBadgeColor != null) {
                                            Badge(containerColor = adresTypeBadgeColor) {
                                                Text(adresTypeTekst, modifier = Modifier.padding(horizontal = 4.dp))
                                            }
                                        }
                                        // Rittype badge (indien ingesteld)
                                        if (!address.defaultTripType.isNullOrBlank()) {
                                            Badge(containerColor = MaterialTheme.colorScheme.surfaceVariant) {
                                                Text(address.defaultTripType, modifier = Modifier.padding(horizontal = 4.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            }
                                        }
                                    }
                                }
                                IconButton(onClick = {
                                    val uri = android.net.Uri.parse("geo:0,0?q=${android.net.Uri.encode(address.address)}")
                                    val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, uri)
                                    try {
                                        context.startActivity(intent)
                                    } catch (e: Exception) {
                                        android.widget.Toast.makeText(context, context.getString(R.string.nav_app_not_found), android.widget.Toast.LENGTH_SHORT).show()
                                    }
                                }) {
                                    Icon(Icons.Filled.Navigation, stringResource(R.string.navigate), tint = MaterialTheme.colorScheme.primary)
                                }
                            }
                        }
                    }
                }
                }
            }
        }
    }

    if (addressToDelete != null) {
        AlertDialog(
            onDismissRequest = { addressToDelete = null },
            title = { Text(stringResource(R.string.delete_address)) },
            text = { Text(stringResource(R.string.delete_address_confirm, addressToDelete?.label ?: "")) },
            confirmButton = {
                Button(onClick = {
                    addressToDelete?.let { viewModel.deleteAddress(it) }
                    addressToDelete = null
                }, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) {
                    Text(stringResource(R.string.delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { addressToDelete = null }) { Text(stringResource(R.string.cancel)) }
            }
        )
    }

    if (deleteErrorMsg != null) {
        AlertDialog(
            onDismissRequest = { deleteErrorMsg = null },
            title = { Text(stringResource(R.string.not_possible)) },
            text = { Text(deleteErrorMsg!!) },
            confirmButton = {
                Button(onClick = { deleteErrorMsg = null }) { Text(stringResource(R.string.ok_camel)) }
            }
        )
    }
}
