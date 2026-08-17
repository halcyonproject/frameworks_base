package com.android.server.halcyon;

import android.content.Context;

import com.android.server.SystemService;

public final class AttestationService extends SystemService {

    public AttestationService(Context context) {
        super(context);
    }

    @Override
    public void onStart() {}

    @Override
    public void onBootPhase(int phase) {}
}

