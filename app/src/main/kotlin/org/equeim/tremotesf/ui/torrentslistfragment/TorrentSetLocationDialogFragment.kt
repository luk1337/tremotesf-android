/*
 * Copyright (C) 2017-2022 Alexey Rochev <equeim@gmail.com>
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

package org.equeim.tremotesf.ui.torrentslistfragment

import android.app.Dialog
import android.os.Bundle
import android.text.InputType
import androidx.navigation.fragment.navArgs
import org.equeim.libtremotesf.IntVector
import org.equeim.tremotesf.R
import org.equeim.tremotesf.databinding.SetLocationDialogBinding
import org.equeim.tremotesf.rpc.GlobalRpc
import org.equeim.tremotesf.ui.NavigationDialogFragment
import org.equeim.tremotesf.ui.addtorrent.AddTorrentDirectoriesAdapter
import org.equeim.tremotesf.ui.utils.createTextFieldDialog
import org.equeim.tremotesf.ui.utils.normalizePath
import org.equeim.tremotesf.ui.utils.toNativeSeparators

class TorrentSetLocationDialogFragment : NavigationDialogFragment() {
    private val args: TorrentSetLocationDialogFragmentArgs by navArgs()
    private var directoriesAdapter: AddTorrentDirectoriesAdapter? = null

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return createTextFieldDialog(requireContext(),
            title = null,
            viewBindingFactory = SetLocationDialogBinding::inflate,
            textFieldId = R.id.download_directory_edit,
            textFieldLayoutId = R.id.download_directory_layout,
            hint = getString(R.string.location),
            inputType = InputType.TYPE_TEXT_VARIATION_URI,
            defaultText = args.location.toNativeSeparators(),
            onInflatedView = {
                it.downloadDirectoryLayout.downloadDirectoryEdit.let { edit ->
                    directoriesAdapter = AddTorrentDirectoriesAdapter(edit, savedInstanceState)
                    edit.setAdapter(directoriesAdapter)
                }
            },
            onAccepted = {
                GlobalRpc.nativeInstance.setTorrentsLocation(
                    IntVector(args.torrentIds),
                    it.downloadDirectoryLayout.downloadDirectoryEdit.text.toString().normalizePath(),
                    it.moveFilesCheckBox.isChecked
                )
                directoriesAdapter?.save()
            })
    }

    override fun onSaveInstanceState(outState: Bundle) {
        directoriesAdapter?.saveInstanceState(outState)
    }
}