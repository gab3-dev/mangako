package com.gabedev.mangako.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.gabedev.mangako.R
import com.gabedev.mangako.Screen
import com.gabedev.mangako.data.local.NavigationBarStyle

@Composable
fun AppNavigationBar(
    style: NavigationBarStyle,
    currentRoute: String?,
    items: List<Screen>,
    onNavigate: (Screen) -> Unit,
) {
    when (style) {
        NavigationBarStyle.CLASSIC -> ClassicNavigationBar(currentRoute, items, onNavigate)
        NavigationBarStyle.FLOATING -> FloatingNavigationBar(currentRoute, items, onNavigate)
    }
}

@Composable
private fun ClassicNavigationBar(
    currentRoute: String?,
    items: List<Screen>,
    onNavigate: (Screen) -> Unit,
) {
    NavigationBar {
        items.forEach { screen ->
            val selected = currentRoute == screen.route
            NavigationBarItem(
                selected = selected,
                onClick = { onNavigate(screen) },
                icon = { NavigationIcon(screen, selected) },
                label = { Text(stringResource(screen.titleRes)) },
            )
        }
    }
}

@Composable
private fun FloatingNavigationBar(
    currentRoute: String?,
    items: List<Screen>,
    onNavigate: (Screen) -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surfaceContainer,
            tonalElevation = 3.dp,
            shadowElevation = 6.dp,
        ) {
            Row(
                modifier = Modifier.padding(8.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                items.forEach { screen ->
                    FloatingNavigationItem(
                        screen = screen,
                        selected = currentRoute == screen.route,
                        onClick = { onNavigate(screen) },
                    )
                }
            }
        }
    }
}

@Composable
private fun FloatingNavigationItem(
    screen: Screen,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val itemWidth by animateDpAsState(
        targetValue = if (selected) 132.dp else 48.dp,
        animationSpec = spring(),
        label = "floatingNavigationItemWidth",
    )
    val containerColor by animateColorAsState(
        targetValue = if (selected) {
            MaterialTheme.colorScheme.secondaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceContainer
        },
        label = "floatingNavigationItemColor",
    )
    val contentColor by animateColorAsState(
        targetValue = if (selected) {
            MaterialTheme.colorScheme.onSecondaryContainer
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
        label = "floatingNavigationItemContentColor",
    )

    Surface(
        modifier = Modifier
            .width(itemWidth)
            .clip(MaterialTheme.shapes.extraLarge)
            .clickable(onClick = onClick)
            .semantics {
                role = Role.Tab
                this.selected = selected
            },
        shape = MaterialTheme.shapes.extraLarge,
        color = containerColor,
        contentColor = contentColor,
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            NavigationIcon(screen, selected)
            AnimatedVisibility(
                visible = selected,
                enter = fadeIn() + expandHorizontally(expandFrom = Alignment.Start),
                exit = fadeOut() + shrinkHorizontally(shrinkTowards = Alignment.Start),
            ) {
                Text(
                    text = stringResource(screen.titleRes),
                    modifier = Modifier
                        .padding(start = 8.dp)
                        .widthIn(max = 76.dp),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        }
    }
}

@Composable
private fun NavigationIcon(screen: Screen, selected: Boolean) {
    when (screen) {
        Screen.UserCollection -> AnimatedIcon(
            isSelected = selected,
            animatedIconRes = R.drawable.ic_library_selector,
        )
        Screen.Explore -> AnimatedIcon(
            isSelected = selected,
            animatedIconRes = R.drawable.ic_explore_selector,
        )
        Screen.Settings -> AnimatedIcon(
            isSelected = selected,
            animatedIconRes = R.drawable.ic_settings_selector,
        )
        else -> Icon(
            imageVector = screen.icon,
            contentDescription = stringResource(screen.titleRes),
            modifier = Modifier.size(24.dp),
        )
    }
}
