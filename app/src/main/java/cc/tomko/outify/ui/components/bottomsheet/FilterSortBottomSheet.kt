package cc.tomko.outify.ui.components.bottomsheet

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilterAlt
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import cc.tomko.outify.ui.viewmodel.library.ExplicitFilter
import cc.tomko.outify.ui.viewmodel.library.SortBy

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterSortBottomSheet(
    onDismissRequest: () -> Unit,
    explicitFilter: ExplicitFilter,
    onExplicitFilterChange: (ExplicitFilter) -> Unit,
    artistNameFilter: String,
    onArtistNameFilterChange: (String) -> Unit,
    trackNameFilter: String,
    onTrackNameFilterChange: (String) -> Unit,
    sortBy: SortBy,
    onSortByChange: (SortBy) -> Unit,
    sortAscending: Boolean,
    onSortAscendingChange: (Boolean) -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.FilterAlt,
                    contentDescription = "Filter and Sort",
                    modifier = Modifier.padding(end = 8.dp)
                )
                Text(
                    text = "Filter & Sort",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }

            // Filters Section
            Text(
                text = "FILTERS",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            // Explicit Filter
            Column(
                modifier = Modifier.padding(bottom = 24.dp)
            ) {
                Text(
                    text = "Explicit Content",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                val explicitOptions = listOf(
                    ExplicitFilter.BOTH to "Show All",
                    ExplicitFilter.EXPLICIT_ONLY to "Explicit Only",
                    ExplicitFilter.NON_EXPLICIT_ONLY to "Non-Explicit Only"
                )
                explicitOptions.forEach { (option, label) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = explicitFilter == option,
                            onClick = { onExplicitFilterChange(option) }
                        )
                        Text(
                            text = label,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                }
            }

            // Artist Name Filter
            TextField(
                value = artistNameFilter,
                onValueChange = onArtistNameFilterChange,
                label = { Text("Filter by Artist Name") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                singleLine = true
            )

            // Track Name Filter
            TextField(
                value = trackNameFilter,
                onValueChange = onTrackNameFilterChange,
                label = { Text("Filter by Track Name") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
                singleLine = true
            )

            // Sort Section
            Text(
                text = "SORT BY",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            // Sort Options
            val sortOptions = listOf(
                SortBy.POSITION to "Added (Default Order)",
                SortBy.ARTIST_NAME to "Artist Name",
                SortBy.TRACK_NAME to "Track Name",
                SortBy.DURATION to "Duration"
            )

            Column(
                modifier = Modifier.padding(bottom = 20.dp)
            ) {
                sortOptions.forEach { (option, label) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = sortBy == option,
                            onClick = { onSortByChange(option) }
                        )
                        Text(
                            text = label,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                }
            }

            // Sort Direction
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = if (sortAscending) "Ascending" else "Descending",
                    style = MaterialTheme.typography.bodyMedium
                )
                Switch(
                    checked = sortAscending,
                    onCheckedChange = onSortAscendingChange
                )
            }

            // Bottom spacing
            androidx.compose.foundation.layout.Spacer(modifier = Modifier.padding(bottom = 8.dp))
        }
    }
}
