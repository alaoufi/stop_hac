package com.privacyshield.monitor.ui.permissions

import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.privacyshield.monitor.core.model.SpecialAccessApp
import com.privacyshield.monitor.core.model.TrackedPermission
import com.privacyshield.monitor.di.AppContainer
import com.privacyshield.monitor.monitor.SpecialAccessScanner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Apps holding a given sensitive permission, grouped for the UI. */
data class PermissionGroup(
    val permission: TrackedPermission,
    val apps: List<PermittedApp>,
)

data class PermittedApp(
    val packageName: String,
    val label: String,
    val isSystem: Boolean,
)

/**
 * Enumerates which installed apps currently hold each sensitive permission we
 * track. Read-only inspection through PackageManager; nothing is transmitted.
 */
class PermissionsViewModel(private val container: AppContainer) : ViewModel() {

    private val pm: PackageManager = container.appContext.packageManager

    private val _groups = MutableStateFlow<List<PermissionGroup>>(emptyList())
    val groups: StateFlow<List<PermissionGroup>> = _groups.asStateFlow()

    private val _specialAccess = MutableStateFlow<List<SpecialAccessApp>>(emptyList())
    val specialAccess: StateFlow<List<SpecialAccessApp>> = _specialAccess.asStateFlow()

    private val _loading = MutableStateFlow(true)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    fun refresh(includeSystem: Boolean) {
        viewModelScope.launch {
            _loading.value = true
            val special = withContext(Dispatchers.IO) {
                SpecialAccessScanner(container.appContext, container.appRepository).scan()
            }
            _specialAccess.value = special
            _groups.value = withContext(Dispatchers.IO) { buildGroups(includeSystem) }
            _loading.value = false
        }
    }

    private fun buildGroups(includeSystem: Boolean): List<PermissionGroup> {
        val packages = runCatching {
            pm.getInstalledPackages(PackageManager.GET_PERMISSIONS)
        }.getOrDefault(emptyList())

        val tracked = TrackedPermission.entries.filter { it.manifestPermission != null }
        val map = tracked.associateWith { mutableListOf<PermittedApp>() }

        for (pkg in packages) {
            if (pkg.applicationInfo == null) continue
            val isSystem = container.appRepository.isSystemApp(pkg.packageName)
            if (isSystem && !includeSystem) continue
            val granted = grantedPermissions(pkg)
            tracked.forEach { tp ->
                if (tp.manifestPermission in granted) {
                    map.getValue(tp) += PermittedApp(
                        pkg.packageName,
                        container.appRepository.info(pkg.packageName).label,
                        isSystem,
                    )
                }
            }
        }
        return tracked
            .map { PermissionGroup(it, map.getValue(it).sortedBy { a -> a.label.lowercase() }) }
            .filter { it.apps.isNotEmpty() }
            .sortedByDescending { it.apps.size }
    }

    private fun grantedPermissions(pkg: PackageInfo): Set<String> {
        val requested = pkg.requestedPermissions ?: return emptySet()
        val flags = pkg.requestedPermissionsFlags ?: return emptySet()
        val out = HashSet<String>()
        requested.forEachIndexed { i, perm ->
            val granted = i < flags.size &&
                (flags[i] and PackageInfo.REQUESTED_PERMISSION_GRANTED) != 0
            if (granted) out += perm
        }
        return out
    }
}
