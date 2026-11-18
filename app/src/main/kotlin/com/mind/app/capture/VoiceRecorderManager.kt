package com.mind.app.capture

import android.content.Context
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Build
import java.io.File

/**
 * Lightweight audio recorder saving `.m4a` voice memories locally.
 */
class VoiceRecorderHelper(private val context: Context) {
    private var recorder: MediaRecorder? = null
    private var outputFile: File? = null
    private var startedAtMs: Long = 0L

    fun startRecording(targetFile: File): Result<Unit> = runCatching {
        stopSilently()
        targetFile.parentFile?.mkdirs()
        val instance = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(context)
        } else {
            @Suppress("DEPRECATION")
            MediaRecorder()
        }

        instance.apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setAudioEncodingBitRate(96_000)
            setAudioSamplingRate(44_100)
            setOutputFile(targetFile.absolutePath)
            prepare()
            start()
        }
        recorder = instance
        outputFile = targetFile
        startedAtMs = System.currentTimeMillis()
    }

    fun stopAndFinalize(): Pair<File, Int>? {
        val file = outputFile ?: return null
        val durationSec = ((System.currentTimeMillis() - startedAtMs) / 1000L).toInt().coerceAtLeast(1)
        val success = runCatching {
            recorder?.stop()
        }.isSuccess
        runCatching { recorder?.release() }
        recorder = null
        outputFile = null
        return if (success && file.exists() && file.length() > 0L) {
            file to durationSec
        } else {
            runCatching { file.delete() }
            null
        }
    }

    fun stopSilently() {
        runCatching { recorder?.stop() }
        runCatching { recorder?.release() }
        recorder = null
    }
}

/**
 * Simple local `.m4a` playback controller used on MemoryDetailScreen.
 */
class VoicePlaybackHelper {
    private var player: MediaPlayer? = null

    fun play(
        filePath: String,
        onCompleted: () -> Unit,
        onError: () -> Unit,
    ): Boolean {
        stop()
        return runCatching {
            val mp = MediaPlayer().apply {
                setDataSource(filePath)
                setOnCompletionListener {
                    stop()
                    onCompleted()
                }
                setOnErrorListener { _, _, _ ->
                    stop()
                    onError()
                    true
                }
                prepare()
                start()
            }
            player = mp
        }.isSuccess
    }

    fun stop() {
        runCatching { player?.stop() }
        runCatching { player?.release() }
        player = null
    }
}
