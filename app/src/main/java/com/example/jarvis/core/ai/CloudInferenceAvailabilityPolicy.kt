package com.example.jarvis.core.ai

/**
 * Current Android release has no configured production cloud AI.
 * Route general AI generation to the on-device model.
 *
 * Does not alter directory access or device action authorization.
 */
internal object CloudInferenceAvailabilityPolicy {

    const val isEnabled: Boolean = false

    fun effectiveRoute(requested: ModelRouter.Route): ModelRouter.Route {
        return if (isEnabled) requested else ModelRouter.Route.LOCAL
    }
}
