#!/bin/bash

./gradlew

./gradlew setupHbase

./gradlew setupNutch

./gradlew buildPlugin

