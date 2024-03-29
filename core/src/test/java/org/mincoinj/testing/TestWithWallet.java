/*
 * Copyright 2013 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.mincoinj.testing;

import org.mincoinj.core.AbstractBlockChain;
import org.mincoinj.core.Address;
import org.mincoinj.core.Block;
import org.mincoinj.core.BlockChain;
import org.mincoinj.core.Coin;
import org.mincoinj.core.Context;
import org.mincoinj.core.ECKey;
import org.mincoinj.core.LegacyAddress;
import org.mincoinj.core.NetworkParameters;
import org.mincoinj.core.Transaction;
import org.mincoinj.core.VerificationException;
import org.mincoinj.params.MainNetParams;
import org.mincoinj.params.UnitTestParams;
import org.mincoinj.script.Script;
import org.mincoinj.store.BlockStore;
import org.mincoinj.store.MemoryBlockStore;
import org.mincoinj.utils.BriefLogFormatter;
import org.mincoinj.wallet.Wallet;

import javax.annotation.Nullable;

import static org.mincoinj.testing.FakeTxBuilder.createFakeBlock;
import static org.mincoinj.testing.FakeTxBuilder.createFakeTx;

// TODO: This needs to be somewhat rewritten - the "sendMoneyToWallet" methods aren't sending via the block chain object

/**
 * A utility class that you can derive from in your unit tests. TestWithWallet sets up an empty wallet,
 * an in-memory block store and a block chain object. It also provides helper methods for filling the wallet
 * with money in whatever ways you wish. Note that for simplicity with amounts, this class sets the default
 * fee per kilobyte to zero in setUp.
 */
public class TestWithWallet {
    protected static final NetworkParameters UNITTEST = UnitTestParams.get();
    protected static final NetworkParameters MAINNET = MainNetParams.get();

    protected ECKey myKey;
    protected Address myAddress;
    protected Wallet wallet;
    protected BlockChain chain;
    protected BlockStore blockStore;

    public void setUp() throws Exception {
        BriefLogFormatter.init();
        Context.propagate(new Context(UNITTEST, 100, Coin.ZERO, false));
        wallet = Wallet.createDeterministic(UNITTEST, Script.ScriptType.P2PKH);
        myKey = wallet.currentReceiveKey();
        myAddress = LegacyAddress.fromKey(UNITTEST, myKey);
        blockStore = new MemoryBlockStore(UNITTEST);
        chain = new BlockChain(UNITTEST, wallet, blockStore);
    }

    public void tearDown() throws Exception {
    }

    @Nullable
    protected Transaction sendMoneyToWallet(Wallet wallet, AbstractBlockChain.NewBlockType type, Transaction... transactions)
            throws VerificationException {
        if (type == null) {
            // Pending transaction
            for (Transaction tx : transactions)
                if (wallet.isPendingTransactionRelevant(tx))
                    wallet.receivePending(tx, null);
        } else {
            FakeTxBuilder.BlockPair bp = createFakeBlock(blockStore, Block.BLOCK_HEIGHT_GENESIS, transactions);
            for (Transaction tx : transactions)
                wallet.receiveFromBlock(tx, bp.storedBlock, type, 0);
            if (type == AbstractBlockChain.NewBlockType.BEST_CHAIN)
                wallet.notifyNewBestBlock(bp.storedBlock);
        }
        if (transactions.length == 1)
            return wallet.getTransaction(transactions[0].getTxId());  // Can be null if tx is a double spend that's otherwise irrelevant.
        else
            return null;
    }

    @Nullable
    protected Transaction sendMoneyToWallet(Wallet wallet, AbstractBlockChain.NewBlockType type, Coin value, Address toAddress) throws VerificationException {
        return sendMoneyToWallet(wallet, type, createFakeTx(UNITTEST, value, toAddress));
    }

    @Nullable
    protected Transaction sendMoneyToWallet(Wallet wallet, AbstractBlockChain.NewBlockType type, Coin value, ECKey toPubKey) throws VerificationException {
        return sendMoneyToWallet(wallet, type, createFakeTx(UNITTEST, value, toPubKey));
    }

    @Nullable
    protected Transaction sendMoneyToWallet(AbstractBlockChain.NewBlockType type, Transaction... transactions) throws VerificationException {
        return sendMoneyToWallet(this.wallet, type, transactions);
    }

    @Nullable
    protected Transaction sendMoneyToWallet(AbstractBlockChain.NewBlockType type, Coin value) throws VerificationException {
        return sendMoneyToWallet(this.wallet, type, value, myAddress);
    }

    @Nullable
    protected Transaction sendMoneyToWallet(AbstractBlockChain.NewBlockType type, Coin value, Address toAddress) throws VerificationException {
        return sendMoneyToWallet(this.wallet, type, value, toAddress);
    }

    @Nullable
    protected Transaction sendMoneyToWallet(AbstractBlockChain.NewBlockType type, Coin value, ECKey toPubKey) throws VerificationException {
        return sendMoneyToWallet(this.wallet, type, value, toPubKey);
    }
}
