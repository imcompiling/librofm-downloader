package com.vishnurajeevan.libroabs

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.NoOpCliktCommand
import com.github.ajalt.clikt.core.main
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.types.choice
import com.github.ajalt.clikt.parameters.types.int
import com.vishnurajeevan.libroabs.libro.LibraryMetadata
import com.vishnurajeevan.libroabs.libro.LibroApiHandler
import io.github.kevincianfarini.cardiologist.intervalPulse
import io.ktor.client.HttpClient
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.response.respondText
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.Clock
import kotlinx.serialization.json.Json
import java.io.File
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import com.vishnurajeevan.libroabs.Config

fun main(args: Array<String>) {
  NoOpCliktCommand(name = "librofm-abs")
    .subcommands(Run())
    .main(args)
}

class Run : CliktCommand("run") {
  val config = ConfigLoaderBuilder.default()
    .addResourceSource("/config.yaml")
    .build()
    .loadConfigOrThrow<Config>()

    config.println()

  private val lfdLogger: (String) -> Unit = {
    if (verbose) {
      println(it)
    }
  }

  private val libroFmApi by lazy {
    LibroApiHandler(
      client = HttpClient { },
      dataDir = dataDir,
      dryRun = dryRun,
      verbose = verbose,
      lfdLogger = lfdLogger
    )
  }

  private val serverScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

  override fun run() {
    println(
      """
        Starting up!
        internal port: $port
        syncInterval: $syncInterval
        dryRun: $dryRun
        renameChapters: $renameChapters
        writeTitleTag: $writeTitleTag
        verbose: $verbose
        libroFmUsername: $libroFmUsername
        libroFmPassword: ${libroFmPassword.map { "*" }.joinToString("")}
        directoryTemplate: $directoryTemplate
      """.trimIndent()
    )

    runBlocking {
      val dataDir = File(dataDir).apply {
        if (!exists()) {
          mkdirs()
        }
      }
      val tokenFile = File("$dataDir/token.txt")
      if (!tokenFile.exists()) {
        lfdLogger("Token file not found, logging in")
        libroFmApi.fetchLoginData(libroFmUsername, libroFmPassword)
      }

      libroFmApi.fetchLibrary()
      processLibrary()

      launch {
        lfdLogger("Sync Interval: $syncInterval")
        val syncIntervalTimeUnit = when (syncInterval) {
          "h" -> 1.hours
          "d" -> 1.days
          "w" -> 7.days
          else -> error("Unhandled sync interval")
        }

        Clock.System.intervalPulse(syncIntervalTimeUnit)
          .beat { scheduled, occurred ->
            lfdLogger("Checking library on pulse!")
            libroFmApi.fetchLibrary()
            processLibrary()
          }
      }

      serverScope.launch {
        embeddedServer(
          factory = Netty,
          port = port,
          host = "0.0.0.0",
          module = {
            routing {
              post("/update") {
                call.respondText("Updating!")
                libroFmApi.fetchLibrary()
                processLibrary()
              }
            }
          }
        ).start(wait = true)
      }
    }
  }

  private suspend fun processLibrary() {
    val localLibrary = Json.decodeFromString<LibraryMetadata>(
      File("$dataDir/library.json").readText()
    )

    localLibrary.audiobooks
      .let {
        if (devMode) {
          it.take(1)
        }
        else {
          it
        }
      }
      .forEach { book ->
        val targetDir = File("$mediaDir/${book.authors.first()}/${book.title}")

        if (!targetDir.exists()) {
          lfdLogger("downloading ${book.title}")
          targetDir.mkdirs()
          val downloadData = libroFmApi.fetchDownloadMetadata(book.isbn)
          libroFmApi.fetchAudioBook(
            data = downloadData.parts,
            targetDirectory = targetDir
          )

          if (renameChapters) {
            libroFmApi.renameChapters(
              title = book.title,
              tracks = downloadData.tracks,
              targetDirectory = targetDir,
              writeTitleTag = writeTitleTag
            )
          }
        }
        else {
          lfdLogger("skipping ${book.title} as it exists on the filesystem!")
        }
      }
  }
}

