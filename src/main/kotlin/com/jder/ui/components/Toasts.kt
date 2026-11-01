package com.jder.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Snackbar
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.jder.ui.strings.UiStrings
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.receiveAsFlow

private const val MAX_TOASTS = 4
private const val ENTER_MILLIS = 300
private const val EXIT_MILLIS = 300

private fun <T> enterSpec() = tween<T>(ENTER_MILLIS, easing = LinearOutSlowInEasing)

private fun <T> exitSpec() = tween<T>(EXIT_MILLIS, easing = FastOutLinearInEasing)

private val toastEnter = fadeIn(enterSpec()) + expandVertically(enterSpec())
private val toastExit = fadeOut(exitSpec()) + shrinkVertically(exitSpec())
private val toastRise = slideInVertically(enterSpec()) { it }
private val toastSink = slideOutVertically(exitSpec()) { it }

enum class ToastType(val icon: ImageVector, val duration: Duration) {
    SUCCESS(Icons.Default.CheckCircle, 4.seconds),
    WARNING(Icons.Default.Warning, 8.seconds),
    ERROR(Icons.Default.Error, 12.seconds),
}

class Toast(val message: String, val type: ToastType)

object Toasts {
    private val pending = Channel<Toast>(Channel.UNLIMITED)
    internal val incoming = pending.receiveAsFlow()

    fun success(message: String) = show(message, ToastType.SUCCESS)

    fun warning(message: String) {
        log("Attenzione", message)
        show(message, ToastType.WARNING)
    }

    fun warning(action: String, cause: Throwable) = warning("$action: ${cause.describe()}")

    fun error(message: String) {
        log("Errore", message)
        show(message, ToastType.ERROR)
    }

    fun error(action: String, cause: Throwable) = error("$action: ${cause.describe()}")

    private fun show(message: String, type: ToastType) {
        pending.trySend(Toast(message, type))
    }
}

fun logError(action: String, cause: Throwable) = log("Errore", "$action: ${cause.describe()}")

fun Throwable.describe(): String =
    message?.lineSequence()?.firstOrNull { it.isNotBlank() }?.trim() ?: javaClass.simpleName

private fun log(level: String, message: String) = System.err.println("$level: $message")

private class ShownToast(val toast: Toast) {
    val visibility = MutableTransitionState(false).apply { targetState = true }
    var timerRestarts by mutableStateOf(0)
    val isLeaving: Boolean
        get() = !visibility.targetState

    val isGone: Boolean
        get() = visibility.isIdle && !visibility.currentState

    fun dismiss() {
        visibility.targetState = false
    }
}

@Composable
fun ToastHost(isDarkTheme: Boolean) {
    val toasts = remember { mutableStateListOf<ShownToast>() }
    LaunchedEffect(Unit) {
        Toasts.incoming.collect { toast ->
            val active = toasts.filterNot { it.isLeaving }
            val same =
                active.find { it.toast.type == toast.type && it.toast.message == toast.message }
            if (same != null) {
                same.timerRestarts++
            } else {
                if (toast.type == ToastType.SUCCESS) {
                    active.filter { it.toast.type == ToastType.SUCCESS }.forEach { it.dismiss() }
                }
                toasts += ShownToast(toast)
                toasts.filterNot { it.isLeaving }.dropLast(MAX_TOASTS).forEach { it.dismiss() }
            }
        }
    }
    if (toasts.isEmpty()) return
    Popup(alignment = Alignment.BottomCenter, properties = PopupProperties(focusable = false)) {
        Column(
            modifier =
                Modifier.padding(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 16.dp)
                    .widthIn(max = 600.dp)
        ) {
            toasts.forEach { shown ->
                key(shown) {
                    ToastCard(shown, isDarkTheme)
                    if (shown.isGone) LaunchedEffect(Unit) { toasts.remove(shown) }
                }
            }
        }
    }
}

private class ToastColors(val container: Color, val content: Color, val icon: Color)

@Composable
private fun ToastType.colors(isDarkTheme: Boolean): ToastColors =
    when (this) {
        ToastType.SUCCESS ->
            if (isDarkTheme) ToastColors(Color(0xFF1E3A26), Color(0xFFD3F2DA), Color(0xFF7DD99A))
            else ToastColors(Color(0xFFE2F4E6), Color(0xFF103A1D), Color(0xFF2E7D32))
        ToastType.WARNING ->
            if (isDarkTheme) ToastColors(Color(0xFF3D3011), Color(0xFFFFE6AD), Color(0xFFFFC04D))
            else ToastColors(Color(0xFFFFF2D1), Color(0xFF4A3300), Color(0xFFB26A00))
        ToastType.ERROR ->
            with(MaterialTheme.colorScheme) {
                ToastColors(
                    errorContainer,
                    onErrorContainer,
                    if (isDarkTheme) onErrorContainer else error,
                )
            }
    }

@OptIn(ExperimentalAnimationApi::class)
@Composable
private fun ToastCard(shown: ShownToast, isDarkTheme: Boolean) {
    val toast = shown.toast
    LaunchedEffect(shown, shown.timerRestarts) {
        delay(toast.type.duration)
        shown.dismiss()
    }
    val colors = toast.type.colors(isDarkTheme)
    AnimatedVisibility(visibleState = shown.visibility, enter = toastEnter, exit = toastExit) {
        Snackbar(
            modifier =
                Modifier.padding(top = 8.dp)
                    .animateEnterExit(enter = toastRise, exit = toastSink)
                    .semantics { liveRegion = LiveRegionMode.Polite },
            dismissAction = {
                IconButton(onClick = shown::dismiss) {
                    Icon(Icons.Default.Close, contentDescription = UiStrings.CLOSE)
                }
            },
            shape = MaterialTheme.shapes.medium,
            containerColor = colors.container,
            contentColor = colors.content,
            dismissActionContentColor = colors.content,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Icon(toast.type.icon, contentDescription = null, tint = colors.icon)
                Text(toast.message)
            }
        }
    }
}
