/*
 * Copyright the original author or authors.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package de.schildbach.wallet.ui;

import org.mincoinj.core.Address;
import org.mincoinj.wallet.Wallet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.schildbach.wallet.Constants;
import de.schildbach.wallet.R;
import de.schildbach.wallet.WalletApplication;
import de.schildbach.wallet.data.AddressBookDao;
import de.schildbach.wallet.data.AddressBookEntry;
import de.schildbach.wallet.data.AppDatabase;
import de.schildbach.wallet.util.WalletUtils;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentManager;

/**
 * @author Andreas Schildbach
 */
public final class EditAddressBookEntryFragment extends DialogFragment {
    private static final String FRAGMENT_TAG = EditAddressBookEntryFragment.class.getName();

    private static final String KEY_ADDRESS = "address";
    private static final String KEY_SUGGESTED_ADDRESS_LABEL = "suggested_address_label";

    public static void edit(final FragmentManager fm, final Address address) {
        edit(fm, address, null);
    }

    private static void edit(final FragmentManager fm, final Address address,
            @Nullable final String suggestedAddressLabel) {
        final DialogFragment newFragment = EditAddressBookEntryFragment.instance(address, suggestedAddressLabel);
        newFragment.show(fm, FRAGMENT_TAG);
    }

    private static EditAddressBookEntryFragment instance(final Address address,
            @Nullable final String suggestedAddressLabel) {
        final EditAddressBookEntryFragment fragment = new EditAddressBookEntryFragment();

        final Bundle args = new Bundle();
        args.putString(KEY_ADDRESS, address.toString());
        args.putString(KEY_SUGGESTED_ADDRESS_LABEL, suggestedAddressLabel);
        fragment.setArguments(args);

        return fragment;
    }

    private AbstractWalletActivity activity;
    private AddressBookDao addressBookDao;
    private Wallet wallet;

    private static final Logger log = LoggerFactory.getLogger(EditAddressBookEntryFragment.class);

    @Override
    public void onAttach(final Context context) {
        super.onAttach(context);
        this.activity = (AbstractWalletActivity) context;
        final WalletApplication application = activity.getWalletApplication();
        this.addressBookDao = AppDatabase.getDatabase(context).addressBookDao();
        this.wallet = application.getWallet();
    }

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        log.info("opening dialog {}", getClass().getName());
    }

    @Override
    public Dialog onCreateDialog(final Bundle savedInstanceState) {
        final Bundle args = getArguments();
        final Address address = Address.fromString(Constants.NETWORK_PARAMETERS, args.getString(KEY_ADDRESS));
        final String suggestedAddressLabel = args.getString(KEY_SUGGESTED_ADDRESS_LABEL);

        final LayoutInflater inflater = LayoutInflater.from(activity);

        final String label = addressBookDao.resolveLabel(address.toString());

        final boolean isAdd = label == null;
        final boolean isOwn = wallet.isAddressMine(address);

        final DialogBuilder dialog = new DialogBuilder(activity);

        if (isOwn)
            dialog.setTitle(isAdd ? R.string.edit_address_book_entry_dialog_title_add_receive
                    : R.string.edit_address_book_entry_dialog_title_edit_receive);
        else
            dialog.setTitle(isAdd ? R.string.edit_address_book_entry_dialog_title_add
                    : R.string.edit_address_book_entry_dialog_title_edit);

        final View view = inflater.inflate(R.layout.edit_address_book_entry_dialog, null);

        final TextView viewAddress = view.findViewById(R.id.edit_address_book_entry_address);
        viewAddress.setText(WalletUtils.formatAddress(address, Constants.ADDRESS_FORMAT_GROUP_SIZE,
                Constants.ADDRESS_FORMAT_LINE_SIZE));

        final TextView viewLabel = view.findViewById(R.id.edit_address_book_entry_label);
        viewLabel.setText(label != null ? label : suggestedAddressLabel);

        dialog.setView(view);

        final DialogInterface.OnClickListener onClickListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(final DialogInterface dialog, final int which) {
                if (which == DialogInterface.BUTTON_POSITIVE) {
                    final String newLabel = viewLabel.getText().toString().trim();
                    if (!newLabel.isEmpty())
                        addressBookDao.insertOrUpdate(new AddressBookEntry(address.toString(), newLabel));
                    else if (!isAdd)
                        addressBookDao.delete(address.toString());
                } else if (which == DialogInterface.BUTTON_NEUTRAL) {
                    addressBookDao.delete(address.toString());
                }

                dismiss();
            }
        };

        dialog.setPositiveButton(isAdd ? R.string.button_add : R.string.edit_address_book_entry_dialog_button_edit,
                onClickListener);
        if (!isAdd)
            dialog.setNeutralButton(R.string.button_delete, onClickListener);
        dialog.setNegativeButton(R.string.button_cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(final DialogInterface dialog, final int which) {
                dismissAllowingStateLoss();
            }
        });

        return dialog.create();
    }
}
