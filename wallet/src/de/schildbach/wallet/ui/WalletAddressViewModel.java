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
import org.mincoinj.core.Coin;
import org.mincoinj.core.Transaction;
import org.mincoinj.uri.BitcoinURI;
import org.mincoinj.utils.Threading;
import org.mincoinj.wallet.Wallet;
import org.mincoinj.wallet.listeners.WalletChangeEventListener;
import org.mincoinj.wallet.listeners.WalletCoinsReceivedEventListener;
import org.mincoinj.wallet.listeners.WalletCoinsSentEventListener;
import org.mincoinj.wallet.listeners.WalletReorganizeEventListener;

import de.schildbach.wallet.Constants;
import de.schildbach.wallet.WalletApplication;
import de.schildbach.wallet.data.AbstractWalletLiveData;
import de.schildbach.wallet.data.ConfigOwnNameLiveData;
import de.schildbach.wallet.util.Qr;

import android.app.Application;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;

/**
 * @author Andreas Schildbach
 */
public class WalletAddressViewModel extends AndroidViewModel {
    private final WalletApplication application;
    public final CurrentAddressLiveData currentAddress;
    public final ConfigOwnNameLiveData ownName;
    public final MediatorLiveData<Bitmap> qrCode = new MediatorLiveData<>();
    public final MediatorLiveData<Uri> bitcoinUri = new MediatorLiveData<>();
    public final MutableLiveData<Event<Void>> showWalletAddressDialog = new MutableLiveData<>();

    public WalletAddressViewModel(final Application application) {
        super(application);
        this.application = (WalletApplication) application;
        this.currentAddress = new CurrentAddressLiveData(this.application);
        this.ownName = new ConfigOwnNameLiveData(this.application);
        this.qrCode.addSource(currentAddress, new Observer<Address>() {
            @Override
            public void onChanged(final Address currentAddress) {
                maybeGenerateQrCode();
            }
        });
        this.qrCode.addSource(ownName, new Observer<String>() {
            @Override
            public void onChanged(final String label) {
                maybeGenerateQrCode();
            }
        });
        this.bitcoinUri.addSource(currentAddress, new Observer<Address>() {
            @Override
            public void onChanged(final Address currentAddress) {
                maybeGenerateBitcoinUri();
            }
        });
        this.bitcoinUri.addSource(ownName, new Observer<String>() {
            @Override
            public void onChanged(final String label) {
                maybeGenerateBitcoinUri();
            }
        });
    }

    private void maybeGenerateQrCode() {
        final Address address = currentAddress.getValue();
        if (address != null) {
            AsyncTask.execute(new Runnable() {
                @Override
                public void run() {
                    qrCode.postValue(Qr.bitmap(uri(address, ownName.getValue())));
                }
            });
        }
    }

    private void maybeGenerateBitcoinUri() {
        final Address address = currentAddress.getValue();
        if (address != null) {
            bitcoinUri.setValue(Uri.parse(uri(address, ownName.getValue())));
        }
    }

    private String uri(final Address address, final String label) {
        return BitcoinURI.convertToBitcoinURI(address, null, label, null);
    }

    public static class CurrentAddressLiveData extends AbstractWalletLiveData<Address> {
        public CurrentAddressLiveData(final WalletApplication application) {
            super(application);
        }

        @Override
        protected void onWalletActive(final Wallet wallet) {
            addWalletListener(wallet);
            load();
        }

        @Override
        protected void onWalletInactive(final Wallet wallet) {
            removeWalletListener(wallet);
        }

        private void addWalletListener(final Wallet wallet) {
            wallet.addCoinsReceivedEventListener(Threading.SAME_THREAD, walletListener);
            wallet.addCoinsSentEventListener(Threading.SAME_THREAD, walletListener);
            wallet.addReorganizeEventListener(Threading.SAME_THREAD, walletListener);
            wallet.addChangeEventListener(Threading.SAME_THREAD, walletListener);
        }

        private void removeWalletListener(final Wallet wallet) {
            wallet.removeChangeEventListener(walletListener);
            wallet.removeReorganizeEventListener(walletListener);
            wallet.removeCoinsSentEventListener(walletListener);
            wallet.removeCoinsReceivedEventListener(walletListener);
        }

        @Override
        protected void load() {
            final Wallet wallet = getWallet();
            AsyncTask.execute(new Runnable() {
                @Override
                public void run() {
                    org.mincoinj.core.Context.propagate(Constants.CONTEXT);
                    postValue(wallet.currentReceiveAddress());
                }
            });
        }

        private final WalletListener walletListener = new WalletListener();

        private class WalletListener implements WalletCoinsReceivedEventListener, WalletCoinsSentEventListener,
                WalletReorganizeEventListener, WalletChangeEventListener {
            @Override
            public void onCoinsReceived(final Wallet wallet, final Transaction tx, final Coin prevBalance,
                    final Coin newBalance) {
                triggerLoad();
            }

            @Override
            public void onCoinsSent(final Wallet wallet, final Transaction tx, final Coin prevBalance,
                    final Coin newBalance) {
                triggerLoad();
            }

            @Override
            public void onReorganize(final Wallet wallet) {
                triggerLoad();
            }

            @Override
            public void onWalletChanged(final Wallet wallet) {
                triggerLoad();
            }
        }
    }
}
