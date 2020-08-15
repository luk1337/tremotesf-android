/*
 * Copyright (C) 2017-2020 Alexey Rochev <equeim@gmail.com>
 *
 * This file is part of Tremotesf.
 *
 * Tremotesf is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Tremotesf is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.equeim.tremotesf

import java.util.concurrent.TimeUnit

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.os.Handler
import android.os.Looper

import androidx.concurrent.futures.CallbackToFutureAdapter
import androidx.core.app.NotificationCompat
import androidx.core.content.getSystemService
import androidx.core.os.bundleOf
import androidx.navigation.NavDeepLinkBuilder

import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ListenableWorker
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequest
import androidx.work.WorkManager
import androidx.work.WorkerParameters

import com.google.common.util.concurrent.ListenableFuture

import org.qtproject.qt5.android.QtNative

import org.equeim.libtremotesf.IntVector
import org.equeim.libtremotesf.JniRpc
import org.equeim.libtremotesf.JniServerSettingsData
import org.equeim.libtremotesf.Peer
import org.equeim.libtremotesf.SessionStats
import org.equeim.libtremotesf.TorrentData
import org.equeim.libtremotesf.TorrentDataVector
import org.equeim.libtremotesf.TorrentFile
import org.equeim.libtremotesf.TorrentFilesVector
import org.equeim.libtremotesf.TorrentPeersVector
import org.equeim.tremotesf.torrentpropertiesfragment.TorrentPropertiesFragment
import org.equeim.tremotesf.utils.LiveEvent
import org.equeim.tremotesf.utils.Logger
import org.equeim.tremotesf.utils.NonNullMutableLiveData
import org.equeim.tremotesf.utils.emit


typealias RpcStatus = org.equeim.libtremotesf.Rpc.Status
typealias RpcError = org.equeim.libtremotesf.Rpc.Error

data class ServerStats(var downloadSpeed: Long,
                       var uploadSpeed: Long,
                       var currentSession: SessionStats,
                       var total: SessionStats)

object Rpc : Logger {
    private const val FINISHED_NOTIFICATION_CHANNEL_ID = "finished"
    private const val ADDED_NOTIFICATION_CHANNEL_ID = "added"

    private val context = Application.instance

    private val handler = Handler(Looper.getMainLooper())

    init {
        System.loadLibrary("c++_shared")
        QtNative.setClassLoader(context.classLoader)

        @Suppress("ConstantConditionIf")
        if (BuildConfig.QT_HAS_ABI_SUFFIX) {
            val suffix = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
                @Suppress("DEPRECATION")
                Build.CPU_ABI
            } else {
                Build.SUPPORTED_ABIS.first()
            }
            System.loadLibrary("Qt5Core_$suffix")
            System.loadLibrary("Qt5Network_$suffix")
            System.loadLibrary("tremotesf_$suffix")
        } else {
            System.loadLibrary("Qt5Core")
            System.loadLibrary("Qt5Network")
            System.loadLibrary("tremotesf")
        }
    }

    val nativeInstance: JniRpc = object : JniRpc() {
        override fun onStatusChanged(status: Int) {
            handler.post {
                Rpc.onStatusChanged(status)
            }
        }

        override fun onErrorChanged(error: Int, errorMessage: String) {
            handler.post {
                Rpc.onErrorChanged(error, errorMessage)
            }
        }

        override fun onServerSettingsChanged(data: JniServerSettingsData) {
            handler.post {
                Rpc.onServerSettingsChanged(data)
            }
        }

        override fun onTorrentsUpdated(removed: IntVector, changed: TorrentDataVector, added: TorrentDataVector) {
            val r = removed.toList()
            val c = changed.toList()
            val a = added.toList()
            handler.post {
                Rpc.onTorrentsUpdated(r, c, a)
            }
        }

        override fun onServerStatsUpdated(downloadSpeed: Long, uploadSpeed: Long, currentSession: SessionStats, total: SessionStats) {
            handler.post {
                Rpc.onServerStatsUpdated(downloadSpeed, uploadSpeed, currentSession, total)
            }
        }

        override fun onTorrentAdded(id: Int, hashString: String, name: String) {
            handler.post {
                Rpc.onTorrentAdded(id, hashString, name)
            }
        }

        override fun onTorrentFinished(id: Int, hashString: String, name: String) {
            handler.post {
                Rpc.onTorrentFinished(id, hashString, name)
            }
        }

        override fun onTorrentAddDuplicate() {
            handler.post {
                Rpc.onTorrentAddDuplicate()
            }
        }

        override fun onTorrentAddError() {
            handler.post {
                Rpc.onTorrentAddError()
            }
        }

        override fun onTorrentFilesUpdated(torrentId: Int, files: TorrentFilesVector) {
            val list = files.toList()
            handler.post {
                Rpc.onTorrentFilesUpdated(torrentId, list)
            }
        }

        override fun onTorrentPeersUpdated(torrentId: Int, removed: IntVector, changed: TorrentPeersVector, added: TorrentPeersVector) {
            val r = removed.toList()
            val c = changed.toList()
            val a = added.toList()
            handler.post {
                Rpc.onTorrentPeersUpdated(torrentId, r, c, a)
            }
        }

        override fun onTorrentFileRenamed(torrentId: Int, filePath: String, newName: String) {
            handler.post {
                Rpc.onTorrentFileRenamed(torrentId, filePath, newName)
            }
        }

        override fun onGotDownloadDirFreeSpace(bytes: Long) {
            handler.post {
                Rpc.onGotDownloadDirFreeSpace(bytes)
            }
        }

        override fun onGotFreeSpaceForPath(path: String, success: Boolean, bytes: Long) {
            handler.post {
                Rpc.onGotFreeSpaceForPath(path, success, bytes)
            }
        }

        override fun onAboutToDisconnect() {
            handler.post {
                Rpc.onAboutToDisconnect()
            }
        }
    }

    private val notificationManager: NotificationManager = context.getSystemService()!!

    private var updateWorkerCompleter: CallbackToFutureAdapter.Completer<ListenableWorker.Result>? = null

    var serverSettings: JniServerSettingsData = nativeInstance.serverSettingsData()
        private set
    val serverStats = NonNullMutableLiveData(ServerStats(0, 0, SessionStats(), SessionStats()))

    val status = NonNullMutableLiveData(RpcStatus.Disconnected)

    val isConnected: Boolean
        get() = (status.value == RpcStatus.Connected)

    val statusString: String
        get() {
            return when (status.value) {
                RpcStatus.Disconnected -> when (error.value) {
                    RpcError.NoError -> context.getString(R.string.disconnected)
                    RpcError.TimedOut -> context.getString(R.string.timed_out)
                    RpcError.ConnectionError -> context.getString(R.string.connection_error)
                    RpcError.AuthenticationError -> context.getString(R.string.authentication_error)
                    RpcError.ParseError -> context.getString(R.string.parsing_error)
                    RpcError.ServerIsTooNew -> context.getString(R.string.server_is_too_new)
                    RpcError.ServerIsTooOld -> context.getString(R.string.server_is_too_old)
                    else -> context.getString(R.string.disconnected)
                }
                RpcStatus.Connecting -> context.getString(R.string.connecting)
                RpcStatus.Connected -> context.getString(R.string.connected)
                else -> context.getString(R.string.disconnected)
            }
        }

    val error = NonNullMutableLiveData(RpcError.NoError)
    var errorMessage: String = ""
        private set

    val torrentAddDuplicateEvent = LiveEvent<Unit>()
    val torrentAddErrorEvent = LiveEvent<Unit>()

    data class TorrentFilesUpdatedData(val torrentId: Int, val changedFiles: List<TorrentFile>)
    val torrentFilesUpdatedEvent = LiveEvent<TorrentFilesUpdatedData>()
    data class TorrentPeersUpdatedData(val torrentId: Int, val removed: List<Int>, val changed: List<Peer>, val added: List<Peer>)
    val torrentPeersUpdatedEvent = LiveEvent<TorrentPeersUpdatedData>()

    data class TorrentFileRenamedData(val torrentId: Int, val filePath: String, val newName: String)
    val torrentFileRenamedEvent = LiveEvent<TorrentFileRenamedData>()

    val gotDownloadDirFreeSpaceEvent = LiveEvent<Long>()
    data class GotFreeSpaceForPathData(val path: String, val success: Boolean, val bytes: Long)
    val gotFreeSpaceForPathEvent = LiveEvent<GotFreeSpaceForPathData>()

    val torrents = NonNullMutableLiveData<List<Torrent>>(emptyList())

    private var disconnectingAfterCurrentServerChanged = false

    private var connectedOnce = false

    init {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notificationManager.createNotificationChannels(listOf(NotificationChannel(FINISHED_NOTIFICATION_CHANNEL_ID,
                    context.getString(R.string.finished_torrents_channel_name),
                    NotificationManager.IMPORTANCE_DEFAULT),
                    NotificationChannel(ADDED_NOTIFICATION_CHANNEL_ID,
                            context.getString(R.string.added_torrents_channel_name),
                            NotificationManager.IMPORTANCE_DEFAULT)))
        }

        var gotFirst = false
        Servers.currentServer.observeForever { server ->
            if (!gotFirst) {
                gotFirst = true
                return@observeForever
            }

            if (isConnected) {
                disconnectingAfterCurrentServerChanged = true
            }

            if (server != null) {
                setServer(server)
                nativeInstance.connect()
            } else {
                nativeInstance.resetServer()
            }
        }

        Servers.currentServer.value?.let(::setServer)
    }

    private fun setServer(server: Server) {
        val s = org.equeim.libtremotesf.Server()
        with(server) {
            s.name = name
            s.address = address
            s.port = port
            s.apiPath = apiPath

            s.proxyType = nativeProxyType()
            s.proxyHostname = proxyHostname
            s.proxyPort = proxyPort
            s.proxyUser = proxyUser
            s.proxyPassword = proxyPassword

            s.https = httpsEnabled
            s.selfSignedCertificateEnabled = selfSignedCertificateEnabled
            s.selfSignedCertificate = selfSignedCertificate.toByteArray()
            s.clientCertificateEnabled = clientCertificateEnabled
            s.clientCertificate = clientCertificate.toByteArray()

            s.authentication = authentication
            s.username = username
            s.password = password

            s.updateInterval = updateInterval
            s.timeout = timeout
        }
        nativeInstance.setServer(s)
    }

    fun connectOnce() {
        if (!connectedOnce) {
            nativeInstance.connect()
            connectedOnce = true
        }
    }

    fun disconnectOnShutdown() {
        nativeInstance.disconnect()
        connectedOnce = false
    }

    private fun onStatusChanged(newStatus: Int) {
        status.value = newStatus
        when (newStatus) {
            RpcStatus.Connected -> {
                showNotificationsSinceLastConnection()
                handleWorkerCompleter()
            }
            RpcStatus.Disconnected -> handleWorkerCompleter()
        }
    }

    private fun onErrorChanged(newError: Int, newErrorMessage: String) {
        errorMessage = newErrorMessage
        error.value = newError
    }

    private fun onServerSettingsChanged(data: JniServerSettingsData) {
        serverSettings.delete()
        serverSettings = data
    }

    private fun onTorrentsUpdated(removed: List<Int>, changed: List<TorrentData>, added: List<TorrentData>) {
        val newTorrents = torrents.value.toMutableList()

        val deleteNativeObjects = ArrayList<TorrentData>(removed.size + changed.size)

        for (index in removed) {
            deleteNativeObjects.add(newTorrents.removeAt(index).data)
        }

        if (changed.isNotEmpty()) {
            val changedIter = changed.iterator()
            var changedTorrentData = changedIter.next()
            var changedId = changedTorrentData.id
            val torrentsIter = newTorrents.listIterator()
            while (torrentsIter.hasNext()) {
                val torrent = torrentsIter.next()
                if (torrent.id == changedId) {
                    deleteNativeObjects.add(torrent.data)
                    torrentsIter.set(Torrent(changedTorrentData, context, torrent))
                    if (changedIter.hasNext()) {
                        changedTorrentData = changedIter.next()
                        changedId = changedTorrentData.id
                    } else {
                        changedId = -1
                    }
                } else {
                    torrent.isChanged = false
                }
            }
        }

        for (torrentData in added) {
            newTorrents.add(Torrent(torrentData, context))
        }

        torrents.value = newTorrents
        deleteNativeObjects.forEach(TorrentData::delete)

        if (isConnected) {
            handleWorkerCompleter()
        }
    }

    private fun showNotificationsSinceLastConnection() {
        val notifyOnFinished: Boolean
        val notifyOnAdded: Boolean
        if (updateWorkerCompleter == null) {
            notifyOnFinished = Settings.notifyOnFinishedSinceLastConnection
            notifyOnAdded = Settings.notifyOnAddedSinceLastConnection
        } else {
            notifyOnFinished = Settings.notifyOnFinished
            notifyOnAdded = Settings.notifyOnAdded
        }

        if (notifyOnFinished || notifyOnAdded) {
            val server = Servers.currentServer.value
            if (server != null) {
                val lastTorrents = server.lastTorrents
                if (lastTorrents.saved) {
                    for (torrent in torrents.value) {
                        val hashString: String = torrent.hashString
                        val oldTorrent = lastTorrents.torrents.find { it.hashString == hashString }
                        if (oldTorrent == null) {
                            if (notifyOnAdded) {
                                showAddedNotification(torrent.id,
                                                      hashString,
                                                      torrent.name)
                            }
                        } else {
                            if (!oldTorrent.finished && (torrent.isFinished) && notifyOnFinished) {
                                showFinishedNotification(torrent.id,
                                                         hashString,
                                                         torrent.name)
                            }
                        }
                    }
                }
            }
        }
    }

    private fun onServerStatsUpdated(downloadSpeed: Long, uploadSpeed: Long, currentSession: SessionStats, total: SessionStats) {
        val stats = serverStats.value
        val oldCurrent = stats.currentSession
        val oldTotal = stats.total
        serverStats.value = stats.apply {
            this.downloadSpeed = downloadSpeed
            this.uploadSpeed = uploadSpeed
            this.currentSession = currentSession
            this.total = total
        }
        oldCurrent.delete()
        oldTotal.delete()
    }

    private fun onTorrentAdded(id: Int, hashString: String, name: String) {
        if (Settings.notifyOnAdded) {
            showAddedNotification(id, hashString, name)
        }
    }

    private fun onTorrentFinished(id: Int, hashString: String, name: String) {
        if (Settings.notifyOnFinished) {
            showFinishedNotification(id, hashString, name)
        }
    }

    private fun onTorrentAddDuplicate() {
        torrentAddDuplicateEvent.emit()
    }

    private fun onTorrentAddError() {
        torrentAddErrorEvent.emit()
    }

    private fun onTorrentFilesUpdated(torrentId: Int, files: List<TorrentFile>) {
        torrents.value.find { it.id == torrentId }?.let { torrent ->
            if (torrent.filesEnabled) {
                torrentFilesUpdatedEvent.emit(TorrentFilesUpdatedData(torrentId, files))
            }
        }
    }

    private fun onTorrentPeersUpdated(torrentId: Int, removed: List<Int>, changed: List<Peer>, added: List<Peer>) {
        torrents.value.find { it.id == torrentId }?.let { torrent ->
            if (torrent.peersEnabled) {
                torrentPeersUpdatedEvent.emit(TorrentPeersUpdatedData(torrentId, removed, changed, added))
            }
        }
    }

    private fun onTorrentFileRenamed(torrentId: Int, filePath: String, newName: String) {
        torrentFileRenamedEvent.emit(TorrentFileRenamedData(torrentId, filePath, newName))
    }

    private fun onGotDownloadDirFreeSpace(bytes: Long) {
        gotDownloadDirFreeSpaceEvent.emit(bytes)
    }

    private fun onGotFreeSpaceForPath(path: String, success: Boolean, bytes: Long) {
        gotFreeSpaceForPathEvent.emit(GotFreeSpaceForPathData(path, success, bytes))
    }

    private fun onAboutToDisconnect() {
        if (disconnectingAfterCurrentServerChanged) {
            disconnectingAfterCurrentServerChanged = false
        } else {
            Servers.save()
        }
    }

    private fun showTorrentNotification(torrentId: Int,
                                        hashString: String,
                                        name: String,
                                        notificationChannel: String,
                                        notificationTitle: String) {
        notificationManager.notify(
                torrentId,
                NotificationCompat.Builder(context, notificationChannel)
                        .setSmallIcon(R.drawable.notification_icon)
                        .setContentTitle(notificationTitle)
                        .setContentText(name)
                        .setContentIntent(NavDeepLinkBuilder(context)
                                                  .setGraph(R.navigation.nav_main)
                                                  .setDestination(R.id.torrentPropertiesFragment)
                                                  .setArguments(bundleOf(TorrentPropertiesFragment.HASH to hashString,
                                                                         TorrentPropertiesFragment.NAME to name))
                                                  .createPendingIntent())
                        .setAutoCancel(true)
                        .setDefaults(Notification.DEFAULT_ALL)
                        .build())
    }

    private fun showFinishedNotification(id: Int, hashString: String, name: String) {
        showTorrentNotification(id,
                hashString,
                name,
                FINISHED_NOTIFICATION_CHANNEL_ID,
                context.getString(R.string.torrent_finished))
    }

    private fun showAddedNotification(id: Int, hashString: String, name: String) {
        showTorrentNotification(id,
                hashString,
                name,
                ADDED_NOTIFICATION_CHANNEL_ID,
                context.getString(R.string.torrent_added))
    }

    fun enqueueUpdateWorker() {
        val interval = Settings.backgroundUpdateInterval
        if (interval > 0 && (Settings.notifyOnFinished || Settings.notifyOnAdded)) {
            info("Rpc.enqueueUpdateWorker(), interval=$interval")
            val constraints = Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()
            val request = PeriodicWorkRequest.Builder(UpdateWorker::class.java, interval, TimeUnit.MINUTES)
                    .setInitialDelay(interval, TimeUnit.MINUTES)
                    .setConstraints(constraints)
                    .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(UpdateWorker.UNIQUE_WORK_NAME, ExistingPeriodicWorkPolicy.KEEP, request)
        }
    }

    fun cancelUpdateWorker() {
        info("Rpc.cancelUpdateWorker()")
        WorkManager.getInstance(context).cancelUniqueWork(UpdateWorker.UNIQUE_WORK_NAME)
    }

    private fun handleWorkerCompleter() {
        updateWorkerCompleter?.let { completer ->
            info("Rpc.handleWorkerCompleter()")
            if (isConnected) {
                Servers.save()
            }
            completer.set(ListenableWorker.Result.success())
            updateWorkerCompleter = null
        }
    }

    class UpdateWorker(context: Context, workerParameters: WorkerParameters) : ListenableWorker(context, workerParameters), Logger {
        companion object {
            const val UNIQUE_WORK_NAME = "RpcUpdateWorker"
        }

        override fun startWork(): ListenableFuture<Result> {
            info("Rpc.UpdateWorker.startWork()")

            if (NavigationActivity.activeActivity != null) {
                warn("Rpc.UpdateWorker.startWork(), activity is not null, return")
                return CallbackToFutureAdapter.getFuture { it.set(Result.success()) }
            }

            if (!Settings.notifyOnFinished && !Settings.notifyOnAdded) {
                warn("Rpc.UpdateWorker.startWork(), notifications are disabled, return")
                return CallbackToFutureAdapter.getFuture { it.set(Result.success()) }
            }

            return CallbackToFutureAdapter.getFuture { completer ->
                updateWorkerCompleter = completer
                if (status.value == RpcStatus.Disconnected) {
                    nativeInstance.connect()
                } else {
                    nativeInstance.updateData()
                }
                javaClass.simpleName
            }
        }
    }
}
