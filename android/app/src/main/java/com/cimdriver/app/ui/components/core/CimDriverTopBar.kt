package com.cimdriver.app.ui.components.core

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.RowScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import com.cimdriver.app.R
import com.cimdriver.app.ui.theme.CIMDriverNavy
import com.cimdriver.app.ui.theme.CIMDriverWhite

enum class TopBarNavType {
    MENU,
    BACK,
    CLOSE,
    NONE
}

/**
 * Standardized TopAppBar for CIMDriver screens.
 * Provides unified branding, title alignment, and navigation behaviors across both main and detail screens.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CimDriverTopBar(
    title: String = "",
    titleContent: (@Composable () -> Unit)? = null,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    navType: TopBarNavType = TopBarNavType.BACK,
    onNavClick: () -> Unit = {},
    containerColor: Color = CIMDriverNavy,
    contentColor: Color = CIMDriverWhite,
    actions: @Composable RowScope.() -> Unit = {}
) {
    CenterAlignedTopAppBar(
        modifier = modifier,
        title = {
            if (titleContent != null) {
                titleContent()
            } else {
                Column(horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = contentColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (!subtitle.isNullOrBlank()) {
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.labelSmall,
                            color = contentColor.copy(alpha = 0.8f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        },
        navigationIcon = {
            when (navType) {
                TopBarNavType.MENU -> {
                    IconButton(onClick = onNavClick) {
                        Icon(
                            imageVector = Icons.Filled.Menu,
                            contentDescription = stringResource(R.string.open_menu),
                            tint = contentColor
                        )
                    }
                }
                TopBarNavType.BACK -> {
                    IconButton(onClick = onNavClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back),
                            tint = contentColor
                        )
                    }
                }
                TopBarNavType.CLOSE -> {
                    IconButton(onClick = onNavClick) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = stringResource(R.string.close),
                            tint = contentColor
                        )
                    }
                }
                TopBarNavType.NONE -> {
                    // No navigation icon
                }
            }
        },
        actions = actions,
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = containerColor,
            titleContentColor = contentColor,
            navigationIconContentColor = contentColor,
            actionIconContentColor = contentColor
        )
    )
}
