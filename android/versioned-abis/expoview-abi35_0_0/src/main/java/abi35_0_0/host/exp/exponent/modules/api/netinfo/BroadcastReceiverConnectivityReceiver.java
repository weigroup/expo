/**
 * Copyright (c) Facebook, Inc. and its affiliates.
 * <p>
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */
package abi35_0_0.host.exp.exponent.modules.api.netinfo;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import abi35_0_0.com.facebook.react.bridge.ReactApplicationContext;

/**
 * This gets the connectivity status using a BroadcastReceiver. This method was deprecated on
 * Android from API level 24 (N) but we use this method still for any devices running below level
 * 24.
 *
 * <p>It has a few differences from the new NetworkCallback method: - Changes to the cellular
 * network effective type (eg. from 2g to 3g) will not trigger a callback
 */
@SuppressWarnings("deprecation")
public class BroadcastReceiverConnectivityReceiver extends ConnectivityReceiver {
  private final ConnectivityBroadcastReceiver mConnectivityBroadcastReceiver;

  public BroadcastReceiverConnectivityReceiver(ReactApplicationContext reactContext) {
    super(reactContext);
    mConnectivityBroadcastReceiver = new ConnectivityBroadcastReceiver();
  }

  @Override
  public void register() {
    IntentFilter filter = new IntentFilter();
    filter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
    getReactContext().registerReceiver(mConnectivityBroadcastReceiver, filter);
    mConnectivityBroadcastReceiver.setRegistered(true);
    updateAndSendConnectionType();
  }

  @Override
  public void unregister() {
    if (mConnectivityBroadcastReceiver.isRegistered()) {
      getReactContext().unregisterReceiver(mConnectivityBroadcastReceiver);
      mConnectivityBroadcastReceiver.setRegistered(false);
    }
  }

  @SuppressLint("MissingPermission")
  private void updateAndSendConnectionType() {
    String connectionType = CONNECTION_TYPE_OTHER;
    String cellularGeneration = null;

    try {
      NetworkInfo networkInfo = getConnectivityManager().getActiveNetworkInfo();
      if (networkInfo == null || !networkInfo.isConnected()) {
        connectionType = CONNECTION_TYPE_NONE;
      } else {
        int networkType = networkInfo.getType();
        switch (networkType) {
          case ConnectivityManager.TYPE_BLUETOOTH:
            connectionType = CONNECTION_TYPE_BLUETOOTH;
            break;
          case ConnectivityManager.TYPE_ETHERNET:
            connectionType = CONNECTION_TYPE_ETHERNET;
            break;
          case ConnectivityManager.TYPE_MOBILE:
          case ConnectivityManager.TYPE_MOBILE_DUN:
            connectionType = CONNECTION_TYPE_CELLULAR;
            cellularGeneration = getEffectiveConnectionType(networkInfo);
            break;
          case ConnectivityManager.TYPE_WIFI:
            connectionType = CONNECTION_TYPE_WIFI;
            break;
          case ConnectivityManager.TYPE_WIMAX:
            connectionType = CONNECTION_TYPE_WIMAX;
            break;
          case ConnectivityManager.TYPE_VPN:
            connectionType = CONNECTION_TYPE_VPN;
        }
      }
    } catch (SecurityException e) {
      setNoNetworkPermission();
      connectionType = CONNECTION_TYPE_UNKNOWN;
    }

    updateConnectivity(connectionType, cellularGeneration);
  }

  /**
   * Class that receives intents whenever the connection type changes. NB: It is possible on some
   * devices to receive certain connection type changes multiple times.
   */
  private class ConnectivityBroadcastReceiver extends BroadcastReceiver {

    // TODO: Remove registered check when source of crash is found. t9846865
    private boolean isRegistered = false;

    public boolean isRegistered() {
      return isRegistered;
    }

    public void setRegistered(boolean registered) {
      isRegistered = registered;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
      String action = intent.getAction();
      if (action != null && action.equals(ConnectivityManager.CONNECTIVITY_ACTION)) {
        updateAndSendConnectionType();
      }
    }
  }
}
