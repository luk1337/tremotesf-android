// SPDX-FileCopyrightText: 2017-2022 Alexey Rochev <equeim@gmail.com>
//
// SPDX-License-Identifier: GPL-3.0-or-later

package org.equeim.tremotesf.ui.torrentslistfragment

import android.os.Bundle
import androidx.navigation.NavOptions
import kotlinx.coroutines.flow.combine
import org.equeim.libtremotesf.RpcConnectionState
import org.equeim.tremotesf.R
import org.equeim.tremotesf.databinding.TransmissionSettingsDialogFragmentBinding
import org.equeim.tremotesf.rpc.GlobalRpc
import org.equeim.tremotesf.rpc.GlobalServers
import org.equeim.tremotesf.ui.NavigationBottomSheetDialogFragment
import org.equeim.tremotesf.ui.utils.launchAndCollectWhenStarted

class TransmissionSettingsDialogFragment :
    NavigationBottomSheetDialogFragment(R.layout.transmission_settings_dialog_fragment) {

    override fun onViewStateRestored(savedInstanceState: Bundle?) {
        super.onViewStateRestored(savedInstanceState)

        val binding = TransmissionSettingsDialogFragmentBinding.bind(requireView())

        with(binding) {
            connectButton.setOnClickListener {
                if (GlobalRpc.connectionState.value == RpcConnectionState.Disconnected) {
                    GlobalRpc.nativeInstance.connect()
                } else {
                    GlobalRpc.nativeInstance.disconnect()
                }
            }
            combine(GlobalServers.hasServers, GlobalRpc.connectionState, ::Pair).launchAndCollectWhenStarted(
                viewLifecycleOwner
            ) { (hasServers, connectionState) ->
                connectButton.apply {
                    isEnabled = hasServers
                    text = when (connectionState) {
                        RpcConnectionState.Disconnected -> getString(R.string.connect)
                        RpcConnectionState.Connecting,
                        RpcConnectionState.Connected -> getString(R.string.disconnect)
                        else -> ""
                    }
                }
            }

            val serversViewAdapter = ServersViewAdapter(serversView)
            serversView.apply {
                setAdapter(serversViewAdapter)
                setOnItemClickListener { _, _, position, _ ->
                    serversViewAdapter.servers[position].let {
                        GlobalServers.setCurrentServer(it.name)
                    }
                }
            }
            GlobalServers.serversState.launchAndCollectWhenStarted(viewLifecycleOwner) {
                serversView.isEnabled = it.servers.isNotEmpty()
                serversViewAdapter.update()
            }

            connectionSettings.setOnClickListener {
                navigate(TransmissionSettingsDialogFragmentDirections.toConnectionSettingsFragment())
            }

            serverSettings.setOnClickListener {
                navigate(TransmissionSettingsDialogFragmentDirections.toServerSettingsFragment())
            }

            alternativeLimitsCheckBox.isChecked =
                GlobalRpc.serverSettings.alternativeSpeedLimitsEnabled
            alternativeLimitsClickable.setOnClickListener {
                alternativeLimitsCheckBox.apply {
                    isChecked = !isChecked
                    GlobalRpc.serverSettings.alternativeSpeedLimitsEnabled = isChecked
                }
            }

            serverStats.setOnClickListener {
                navigate(
                    TransmissionSettingsDialogFragmentDirections.toServerStatsDialog(),
                    NavOptions.Builder().setPopUpTo(
                        checkNotNull(navController.previousBackStackEntry).destination.id, false
                    ).build()
                )
            }

            GlobalRpc.isConnected.launchAndCollectWhenStarted(viewLifecycleOwner) { connected ->
                listOf(
                    serverSettings,
                    alternativeLimitsClickable,
                    alternativeLimitsCheckBox,
                    serverStats
                ).forEach {
                    it.isEnabled = connected
                }
            }
        }
    }
}