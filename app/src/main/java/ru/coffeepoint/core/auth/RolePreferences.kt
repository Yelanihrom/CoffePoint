package ru.coffeepoint.core.auth

import android.content.Context

class RolePreferences(
    context: Context
) {

    private val preferences = context.getSharedPreferences(
        "role_preferences",
        Context.MODE_PRIVATE
    )

    fun getSelectedRole(): UserRole? {

        val value = preferences.getString(KEY_SELECTED_ROLE, null)
            ?: return null

        return runCatching {
            UserRole.valueOf(value)
        }.getOrNull()

    }

    fun saveSelectedRole(role: UserRole) {

        preferences.edit()
            .putString(KEY_SELECTED_ROLE, role.name)
            .apply()

    }

    fun clearSelectedRole() {

        preferences.edit()
            .remove(KEY_SELECTED_ROLE)
            .apply()

    }

    private companion object {

        const val KEY_SELECTED_ROLE = "selected_role"

    }

}
