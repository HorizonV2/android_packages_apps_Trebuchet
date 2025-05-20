/*
 * Copyright (C) 2025 AxionAOSP Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.android.launcher3.util;

import android.os.IBinder;
import android.os.Process;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.util.Log;

import com.android.internal.os.IBoostFramework;

public class L3BoostFramework {

    private static final String TAG = "L3BoostFramework";

    public static int REQUEST_ANIMATION_BOOST_TYPE_LAUNCHER_ANIMATION_BOOST = 1 << 1;
    public static int REQUEST_ANIMATION_BOOST_TYPE_LAUNCHER_DISMISS_BOOST = 1 << 2;

    private static final int STATUS_BIND_BIG_CORE = 0;
    private static final int STATUS_BIND_SMALL_CORE = 1;
    private static final int STATUS_UNBIND = 2;

    private static final long ANIMATION_BOOST_ON = 0L;
    private static final long ANIMATION_BOOST_OFF = -1L;

    private int mAnimationBoostType = 0;
    private int mBindStatus = STATUS_UNBIND;
    private long mAnimationBoost = ANIMATION_BOOST_OFF;
    
    private static IBoostFramework sService;
    
    private static L3BoostFramework instance = null;

    private L3BoostFramework() {}

    public static synchronized L3BoostFramework INSTANCE() {
        if (instance == null) {
            instance = new L3BoostFramework();
        }
        return instance;
    }

    private static IBoostFramework getService() {
        if (sService == null) {
            IBinder binder = ServiceManager.getService("boost_framework");
            sService = IBoostFramework.Stub.asInterface(binder);
        }
        return sService;
    }

    public void bindBigCore() {
        if (mBindStatus != STATUS_BIND_BIG_CORE) {
            mBindStatus = STATUS_BIND_BIG_CORE;
            executeSetThreadAffinity(STATUS_BIND_BIG_CORE);
        }
    }

    public void bindSmallCore() {
        if (mBindStatus != STATUS_BIND_SMALL_CORE) {
            mBindStatus = STATUS_BIND_SMALL_CORE;
            executeSetThreadAffinity(STATUS_BIND_SMALL_CORE);
        }
    }

    public void unbind() {
        if (mBindStatus != STATUS_UNBIND) {
            mBindStatus = STATUS_UNBIND;
            executeSetThreadAffinity(STATUS_UNBIND);
        }
    }

    public void animationBoostOn(int type) {
        mAnimationBoostType |= type;
        if (mAnimationBoost != ANIMATION_BOOST_ON) {
            bindBigCore();
            mAnimationBoost = ANIMATION_BOOST_ON;
            executeSetAnimationBoost(ANIMATION_BOOST_ON);
        }
    }

    public void animationBoostOff(int type) {
        mAnimationBoostType &= ~type;
        if (mAnimationBoostType <= 0 && mAnimationBoost != ANIMATION_BOOST_OFF) {
            unbind();
            mAnimationBoost = ANIMATION_BOOST_OFF;
            executeSetAnimationBoost(ANIMATION_BOOST_OFF);
        }
    }

    private void executeSetAnimationBoost(long boost) {
        try {
            animationBoost(boost);
        } catch (Exception e) {
            Log.w(TAG, "executeSetAnimationBoost() Exception: ", e);
        }
    }

    private void executeSetThreadAffinity(int affinity) {
        try {
            setProcThreadAffinity(affinity);
        } catch (Exception e) {
            Log.w(TAG, "executeSetThreadAffinity() Exception: ", e);
        }
    }
    
    public static void setProcThreadAffinity(int affinity) {
        try {
            int tid = Process.myPid();
            IBoostFramework service = getService();
            if (service != null) {
                service.setProcThreadAffinity(tid, affinity);
            }
        } catch (RemoteException e) {
            Log.e(TAG, "Failed to call setProcThreadAffinity", e);
        }
    }

    public static void animationBoost(long boost) {
        try {
            int tid = Process.myPid();
            IBoostFramework service = getService();
            if (service != null) {
                service.animationBoost(tid, boost);
                releaseMemory();
            }
        } catch (RemoteException e) {
            Log.e(TAG, "Failed to call animationBoost", e);
        }
    }
    
    private static void releaseMemory() {
        try {
            android.app.ActivityManager.getService().releaseMemory(900, 20, false, false);
        } catch (RemoteException e) {
        }
    }
}
