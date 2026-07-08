package com.privacyshield.monitor.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.privacyshield.monitor.R
import com.privacyshield.monitor.ui.components.SectionCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.about_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
            )
        },
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Spacer(Modifier.size(2.dp))
            SectionCard(title = stringResource(R.string.app_name)) {
                Text(stringResource(R.string.about_intro), style = MaterialTheme.typography.bodyMedium)
            }
            SectionCard(title = stringResource(R.string.about_privacy_title)) {
                Text(stringResource(R.string.about_privacy_body), style = MaterialTheme.typography.bodyMedium)
            }
            SectionCard(title = stringResource(R.string.about_limits_title)) {
                Text(stringResource(R.string.about_limits_body), style = MaterialTheme.typography.bodyMedium)
            }
            SectionCard(title = stringResource(R.string.about_how_title)) {
                Text(stringResource(R.string.about_how_body), style = MaterialTheme.typography.bodyMedium)
            }
            Text(
                stringResource(R.string.about_version),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Medium,
            )
            Spacer(Modifier.size(24.dp))
        }
    }
}
