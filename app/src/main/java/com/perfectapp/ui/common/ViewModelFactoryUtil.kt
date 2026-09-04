package com.perfectapp.ui.common

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

/** Small helper to build a one-off ViewModelProvider.Factory from a lambda, avoiding
 *  a dedicated Factory class per screen when no Hilt/DI framework is used. */
fun <T : ViewModel> viewModelFactory(build: () -> T): ViewModelProvider.Factory =
    object : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <VM : ViewModel> create(modelClass: Class<VM>): VM = build() as VM
    }
