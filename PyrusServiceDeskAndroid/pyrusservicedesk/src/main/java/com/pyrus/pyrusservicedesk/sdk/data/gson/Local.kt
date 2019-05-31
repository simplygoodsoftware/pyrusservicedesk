package com.pyrus.pyrusservicedesk.sdk.data.gson

/**
 * All local fields of entities that are sent to the server but can also be persisted for offline purposes must
 * be annotated by this.
 * This is checked by [RemoteGsonExclusionStrategy] for skipping writing of the entity's field.
 */
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FIELD)
annotation class Local