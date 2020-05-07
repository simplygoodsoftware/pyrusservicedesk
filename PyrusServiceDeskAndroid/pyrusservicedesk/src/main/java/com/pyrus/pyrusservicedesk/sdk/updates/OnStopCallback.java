package com.pyrus.pyrusservicedesk.sdk.updates;

/**
 * Defines the interface by which applications can receive notifications of stopping
 * PyrusServiceDesk.
 */
public interface OnStopCallback {
    void onServiceDeskStop();
}
