package com.daljoalab.memorycapsule.core.voice

import android.content.Context
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Build
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.webrtc.AudioSource
import org.webrtc.DataChannel
import org.webrtc.IceCandidate
import org.webrtc.MediaConstraints
import org.webrtc.MediaStream
import org.webrtc.PeerConnection
import org.webrtc.PeerConnectionFactory
import org.webrtc.RtpReceiver
import org.webrtc.SdpObserver
import org.webrtc.SessionDescription
import org.webrtc.audio.JavaAudioDeviceModule
import java.nio.ByteBuffer
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.time.Duration.Companion.milliseconds

@Singleton
class LiveVoiceClient @Inject constructor(
    @ApplicationContext private val context: Context,
    private val json: Json,
) {

    private val factory: PeerConnectionFactory by lazy {
        PeerConnectionFactory.initialize(
            PeerConnectionFactory.InitializationOptions.builder(context).createInitializationOptions(),
        )
        val audioDeviceModule = JavaAudioDeviceModule.builder(context)
            .setUseHardwareAcousticEchoCanceler(true)
            .setUseHardwareNoiseSuppressor(true)
            .createAudioDeviceModule()
        PeerConnectionFactory.builder()
            .setAudioDeviceModule(audioDeviceModule)
            .createPeerConnectionFactory()
    }
    private val audioManager = context.getSystemService(AudioManager::class.java)

    private var peerConnection: PeerConnection? = null
    private var dataChannel: DataChannel? = null
    private var audioSource: AudioSource? = null

    private val _events = MutableSharedFlow<LiveEvent>(extraBufferCapacity = 256)
    val events: SharedFlow<LiveEvent> = _events.asSharedFlow()

    suspend fun createOffer(): String {
        close()
        routeAudioForCall()

        val iceGathered = CompletableDeferred<Unit>()
        val config = PeerConnection.RTCConfiguration(emptyList()).apply {
            sdpSemantics = PeerConnection.SdpSemantics.UNIFIED_PLAN
        }
        val connection = factory.createPeerConnection(config, PeerConnectionObserver(iceGathered))
            ?: throw LiveVoiceException("음성 연결을 준비하지 못했어요.")
        peerConnection = connection

        val source = factory.createAudioSource(audioConstraints()).also { audioSource = it }
        connection.addTrack(factory.createAudioTrack(MIC_TRACK_ID, source), listOf(STREAM_ID))

        dataChannel = connection.createDataChannel(EVENTS_CHANNEL, DataChannel.Init())
            .also { it.registerObserver(DataChannelObserver(it)) }

        val offer = connection.awaitCreateOffer()
        connection.awaitSetLocalDescription(offer)
        withTimeoutOrNull(ICE_GATHERING_TIMEOUT_MS.milliseconds) { iceGathered.await() }

        return connection.localDescription?.description
            ?: throw LiveVoiceException("마이크 연결 준비 시간이 초과됐어요.")
    }

    suspend fun applyAnswer(sdp: String) {
        val connection = peerConnection ?: throw LiveVoiceException("음성 연결이 끊겼어요.")
        connection.awaitSetRemoteDescription(SessionDescription(SessionDescription.Type.ANSWER, sdp))
    }

    fun requestClose() {
        peerConnection?.senders?.forEach { it.track()?.setEnabled(false) }
        send(buildJsonObject { put("type", "session.close") }.toString())
    }

    fun close() {
        dataChannel?.run {
            unregisterObserver()
            close()
            dispose()
        }
        dataChannel = null
        peerConnection?.dispose()
        peerConnection = null
        audioSource?.dispose()
        audioSource = null
        restoreAudioRouting()
    }

    private fun send(text: String) {
        val channel = dataChannel?.takeIf { it.state() == DataChannel.State.OPEN } ?: return
        channel.send(DataChannel.Buffer(ByteBuffer.wrap(text.toByteArray()), false))
    }

    private fun audioConstraints() = MediaConstraints().apply {
        listOf("googEchoCancellation", "googNoiseSuppression", "googAutoGainControl", "googHighpassFilter")
            .forEach { mandatory.add(MediaConstraints.KeyValuePair(it, "true")) }
    }

    private fun routeAudioForCall() {
        audioManager.mode = AudioManager.MODE_IN_COMMUNICATION
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            audioManager.availableCommunicationDevices
                .firstOrNull { it.type == AudioDeviceInfo.TYPE_BUILTIN_SPEAKER }
                ?.let(audioManager::setCommunicationDevice)
        } else {
            @Suppress("DEPRECATION")
            audioManager.isSpeakerphoneOn = true
        }
    }

    private fun restoreAudioRouting() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            audioManager.clearCommunicationDevice()
        } else {
            @Suppress("DEPRECATION")
            audioManager.isSpeakerphoneOn = false
        }
        audioManager.mode = AudioManager.MODE_NORMAL
    }

    private inner class PeerConnectionObserver(
        private val iceGathered: CompletableDeferred<Unit>,
    ) : PeerConnection.Observer {

        override fun onIceGatheringChange(state: PeerConnection.IceGatheringState) {
            if (state == PeerConnection.IceGatheringState.COMPLETE) iceGathered.complete(Unit)
        }

        override fun onConnectionChange(state: PeerConnection.PeerConnectionState) {
            if (state == PeerConnection.PeerConnectionState.FAILED) _events.tryEmit(LiveEvent.Disconnected)
        }

        override fun onSignalingChange(state: PeerConnection.SignalingState) = Unit
        override fun onIceConnectionChange(state: PeerConnection.IceConnectionState) = Unit
        override fun onIceConnectionReceivingChange(receiving: Boolean) = Unit
        override fun onIceCandidate(candidate: IceCandidate) = Unit
        override fun onIceCandidatesRemoved(candidates: Array<out IceCandidate>) = Unit
        override fun onAddStream(stream: MediaStream) = Unit
        override fun onRemoveStream(stream: MediaStream) = Unit
        override fun onDataChannel(channel: DataChannel) = Unit
        override fun onRenegotiationNeeded() = Unit
        override fun onAddTrack(receiver: RtpReceiver, streams: Array<out MediaStream>) = Unit
    }

    private inner class DataChannelObserver(
        private val channel: DataChannel,
    ) : DataChannel.Observer {

        override fun onMessage(buffer: DataChannel.Buffer) {
            if (buffer.binary) return
            val bytes = ByteArray(buffer.data.remaining()).also(buffer.data::get)
            json.parseLiveEvent(bytes.decodeToString())?.let(_events::tryEmit)
        }

        override fun onStateChange() {
            if (channel.state() == DataChannel.State.CLOSED) _events.tryEmit(LiveEvent.Disconnected)
        }

        override fun onBufferedAmountChange(previousAmount: Long) = Unit
    }

    private companion object {
        const val EVENTS_CHANNEL = "oai-events"
        const val MIC_TRACK_ID = "mic"
        const val STREAM_ID = "mc"
        const val ICE_GATHERING_TIMEOUT_MS = 10_000L
    }
}

class LiveVoiceException(message: String) : Exception(message)

private suspend fun PeerConnection.awaitCreateOffer(): SessionDescription =
    suspendCancellableCoroutine { continuation ->
        createOffer(
            object : SdpObserverAdapter() {
                override fun onCreateSuccess(description: SessionDescription) = continuation.resume(description)
                override fun onCreateFailure(error: String?) =
                    continuation.resumeWithException(LiveVoiceException("음성 연결을 준비하지 못했어요."))
            },
            MediaConstraints(),
        )
    }

private suspend fun PeerConnection.awaitSetLocalDescription(description: SessionDescription) =
    suspendCancellableCoroutine { continuation ->
        setLocalDescription(continuation.setObserver(), description)
    }

private suspend fun PeerConnection.awaitSetRemoteDescription(description: SessionDescription) =
    suspendCancellableCoroutine { continuation ->
        setRemoteDescription(continuation.setObserver(), description)
    }

private fun kotlinx.coroutines.CancellableContinuation<Unit>.setObserver() = object : SdpObserverAdapter() {
    override fun onSetSuccess() = resume(Unit)
    override fun onSetFailure(error: String?) =
        resumeWithException(LiveVoiceException("음성 연결을 준비하지 못했어요."))
}

private abstract class SdpObserverAdapter : SdpObserver {
    override fun onCreateSuccess(description: SessionDescription) = Unit
    override fun onSetSuccess() = Unit
    override fun onCreateFailure(error: String?) = Unit
    override fun onSetFailure(error: String?) = Unit
}
