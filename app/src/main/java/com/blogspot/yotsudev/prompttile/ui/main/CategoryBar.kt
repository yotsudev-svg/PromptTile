package com.blogspot.yotsudev.prompttile.ui.main

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.blogspot.yotsudev.prompttile.data.entity.CategoryEntity
import com.blogspot.yotsudev.prompttile.data.entity.ParentCategoryEntity

/**
 * エリアB: カテゴリ選択バー（モバイル／1ペイン用）。
 * 2段構成: 親カテゴリ(Tab) + 子カテゴリ(Chip)
 */
@Composable
fun CategoryBar(
    parentCategories: List<ParentCategoryEntity>,
    selectedParentId: Long?,
    onParentSelected: (Long) -> Unit,
    childCategories: List<CategoryEntity>,
    selectedChildId: Long?,
    onChildSelected: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        // --- 1段目: 親カテゴリ ---
        if (parentCategories.isNotEmpty()) {
            val selectedTabIndex = parentCategories.indexOfFirst { it.id == selectedParentId }.coerceAtLeast(0)
            ScrollableTabRow(
                selectedTabIndex = selectedTabIndex,
                edgePadding = 12.dp,
                containerColor = Color.Transparent,
                divider = {},
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex])
                    )
                },
                modifier = Modifier.height(48.dp)
            ) {
                parentCategories.forEach { parent ->
                    Tab(
                        selected = parent.id == selectedParentId,
                        onClick = { onParentSelected(parent.id) },
                        text = {
                            Text(
                                text = parent.nameJa,
                                style = MaterialTheme.typography.labelLarge,
                                maxLines = 1
                            )
                        }
                    )
                }
            }
        }

        HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)

        // --- 2段目: 子カテゴリ ---
        AnimatedContent(
            targetState = selectedParentId to childCategories,
            transitionSpec = {
                (slideInHorizontally { -it / 5 } + fadeIn(tween(180))) togetherWith
                (slideOutHorizontally { it / 5 } + fadeOut(tween(120)))
            },
            label = "childCategoriesTransition"
        ) { (_, currentChildren) ->
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                contentPadding = PaddingValues(horizontal = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(currentChildren, key = { it.id }) { category ->
                    val interactionSource = remember { MutableInteractionSource() }
                    val isPressed by interactionSource.collectIsPressedAsState()
                    val scale by animateFloatAsState(
                        targetValue = if (isPressed) 0.92f else 1.0f,
                        animationSpec = spring(dampingRatio = 0.5f, stiffness = 300f),
                        label = "scale"
                    )

                    FilterChip(
                        selected = category.id == selectedChildId,
                        onClick = { onChildSelected(category.id) },
                        interactionSource = interactionSource,
                        label = {
                            Text(
                                text = category.nameJa,
                                style = MaterialTheme.typography.labelMedium,
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer,
                        ),
                        modifier = Modifier.graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                        }
                    )
                }
            }
        }
    }
}

/**
 * 3ペイン時の左側に表示する縦型のカテゴリ選択サイドバー。
 */
@Composable
fun CategorySidebar(
    parentCategories: List<ParentCategoryEntity>,
    selectedParentId: Long?,
    onParentSelected: (Long) -> Unit,
    childCategories: List<CategoryEntity>,
    selectedChildId: Long?,
    onChildSelected: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(modifier = modifier.fillMaxHeight()) {
        // 親カテゴリレール
        Column(
            modifier = Modifier
                .width(56.dp)
                .fillMaxHeight()
                .padding(vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            parentCategories.forEach { parent ->
                NavigationRailItem(
                    selected = parent.id == selectedParentId,
                    onClick = { onParentSelected(parent.id) },
                    icon = {
                        Text(
                            text = parent.nameJa,
                            style = MaterialTheme.typography.labelSmall,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    },
                    label = {},
                    alwaysShowLabel = false
                )
            }
        }

        androidx.compose.material3.VerticalDivider(thickness = 0.5.dp)

        // 子カテゴリリスト
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .padding(vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            contentPadding = PaddingValues(horizontal = 8.dp)
        ) {
            items(childCategories, key = { it.id }) { category ->
                NavigationRailItem(
                    selected = category.id == selectedChildId,
                    onClick = { onChildSelected(category.id) },
                    icon = {
                        Text(
                            text = category.nameJa,
                            style = MaterialTheme.typography.labelMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    label = {},
                    alwaysShowLabel = false,
                    colors = NavigationRailItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.onSecondaryContainer,
                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        indicatorColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                )
            }
        }
    }
}
