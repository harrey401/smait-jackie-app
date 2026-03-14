package com.gow.smaitrobot

import android.app.Application

/**
 * Application subclass for SMAIT Jackie robot app.
 *
 * Will later hold singleton instances for WebSocketRepository, ThemeRepository,
 * and other app-scoped dependencies. Currently a minimal stub to register
 * the application class in AndroidManifest.xml.
 */
class JackieApplication : Application()
