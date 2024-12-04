package com.example.lib


import com.example.thirdparty.MarvelApiClient
import org.koin.dsl.module

val appModule = module {
    single { MarvelApiClient() }
}
