package com.gov.employeedb

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class ZolvexApp : Application() {

    override fun onCreate() {
        // The three StAX properties MUST be set before any POI class loads,
        // otherwise poi-android resolves the desktop provider and crashes.
        System.setProperty(
            "org.apache.poi.javax.xml.stream.XMLInputFactory",
            "com.fasterxml.aalto.stax.InputFactoryImpl"
        )
        System.setProperty(
            "org.apache.poi.javax.xml.stream.XMLOutputFactory",
            "com.fasterxml.aalto.stax.OutputFactoryImpl"
        )
        System.setProperty(
            "org.apache.poi.javax.xml.stream.XMLEventFactory",
            "com.fasterxml.aalto.stax.EventFactoryImpl"
        )
        super.onCreate()
    }
}