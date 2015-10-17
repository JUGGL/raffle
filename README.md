# JUGGL Raffle [![Build Status](https://travis-ci.org/JUGGL/raffle.svg)](https://travis-ci.org/JUGGL/raffle) [![Codacy Badge](https://api.codacy.com/project/badge/3f24e372dc434caaa26dfe8a01408a80)](https://www.codacy.com/app/deven-phillips/Raffle)

## Overview

A simple application which handles people entering their given and family names. Once the names are entered, the admin
can select a winner at random from the entries.

## Current Status

5 - Production

## Prerequisites

* Maven
* Java 8

## Building The Application

```
git clone git@github.com:JUGGL/raffle.git
cd raffle
mvn clean package capsule:build
```

## Running The Application

```
java -jar target/juggl-raffle-<VERSION>-capsule-fat.jar
```
