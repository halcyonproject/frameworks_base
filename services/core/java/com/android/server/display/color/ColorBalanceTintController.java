/*
 * Copyright (C) 2019 The Android Open Source Project
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

package com.android.server.display.color;

import static com.android.server.display.color.DisplayTransformManager.LEVEL_COLOR_MATRIX_COLOR_BALANCE;

import android.content.Context;
import android.graphics.Color;
import android.hardware.display.ColorDisplayManager;
import android.opengl.Matrix;
import android.provider.Settings;

import java.util.Arrays;

/** Control the color transform for global color balance. */
final class ColorBalanceTintController extends TintController {

    private final float[] mMatrix = new float[16];

    @Override
    public void setUp(Context context, boolean needsLinear) {
    }

    @Override
    public float[] getMatrix() {
        return Arrays.copyOf(mMatrix, mMatrix.length);
    }

    @Override
    public void setMatrix(int rgb) {
        setMatrix(Color.red(rgb), Color.green(rgb), Color.blue(rgb), 180, 100, 100);
    }

    public void setMatrix(int red, int green, int blue, int hue, int saturation, int contrast) {
        float[] rgbMatrix = new float[16];
        Matrix.setIdentityM(rgbMatrix, 0);
        rgbMatrix[0] = ((float) red) / 255.0f;
        rgbMatrix[5] = ((float) green) / 255.0f;
        rgbMatrix[10] = ((float) blue) / 255.0f;

        float[] hueMatrix = new float[16];
        Matrix.setIdentityM(hueMatrix, 0);
        float angleDegrees = hue - 180.0f;
        if (angleDegrees != 0.0f) {
            float rad = (float) Math.toRadians(angleDegrees);
            float cos = (float) Math.cos(rad);
            float sin = (float) Math.sin(rad);

            hueMatrix[0] = 0.213f + cos * 0.787f - sin * 0.213f;
            hueMatrix[4] = 0.715f - cos * 0.715f - sin * 0.715f;
            hueMatrix[8] = 0.072f - cos * 0.072f + sin * 0.928f;

            hueMatrix[1] = 0.213f - cos * 0.213f + sin * 0.143f;
            hueMatrix[5] = 0.715f + cos * 0.285f + sin * 0.140f;
            hueMatrix[9] = 0.072f - cos * 0.072f - sin * 0.283f;

            hueMatrix[2] = 0.213f - cos * 0.213f - sin * 0.787f;
            hueMatrix[6] = 0.715f - cos * 0.715f + sin * 0.715f;
            hueMatrix[10] = 0.072f + cos * 0.928f + sin * 0.072f;
        }

        float[] satMatrix = new float[16];
        Matrix.setIdentityM(satMatrix, 0);
        float s = saturation / 100.0f;
        if (s != 1.0f) {
            float d = 1.0f - s;
            float lr = 0.213f * d;
            float lg = 0.715f * d;
            float lb = 0.072f * d;

            satMatrix[0] = lr + s;
            satMatrix[4] = lg;
            satMatrix[8] = lb;

            satMatrix[1] = lr;
            satMatrix[5] = lg + s;
            satMatrix[9] = lb;

            satMatrix[2] = lr;
            satMatrix[6] = lg;
            satMatrix[10] = lb + s;
        }

        float[] contrastMatrix = new float[16];
        Matrix.setIdentityM(contrastMatrix, 0);
        float c = contrast / 100.0f;
        if (c != 1.0f) {
            float t = 0.5f * (1.0f - c);

            contrastMatrix[0] = c;
            contrastMatrix[5] = c;
            contrastMatrix[10] = c;

            contrastMatrix[12] = t;
            contrastMatrix[13] = t;
            contrastMatrix[14] = t;
        }

        float[] temp1 = new float[16];
        float[] temp2 = new float[16];
        Matrix.multiplyMM(temp1, 0, satMatrix, 0, hueMatrix, 0);
        Matrix.multiplyMM(temp2, 0, contrastMatrix, 0, temp1, 0);
        Matrix.multiplyMM(mMatrix, 0, rgbMatrix, 0, temp2, 0);
    }

    @Override
    public int getLevel() {
        return LEVEL_COLOR_MATRIX_COLOR_BALANCE;
    }

    @Override
    public boolean isAvailable(Context context) {
        return ColorDisplayManager.isColorTransformAccelerated(context);
    }

    public void updateBalance(Context context, int userId) {
        int red = Settings.Secure.getIntForUser(context.getContentResolver(), channelToKey(0),
                getDefaultValueForChannel(0), userId);
        int green = Settings.Secure.getIntForUser(context.getContentResolver(), channelToKey(1),
                getDefaultValueForChannel(1), userId);
        int blue = Settings.Secure.getIntForUser(context.getContentResolver(), channelToKey(2),
                getDefaultValueForChannel(2), userId);
        int hue = Settings.Secure.getIntForUser(context.getContentResolver(), channelToKey(3),
                getDefaultValueForChannel(3), userId);
        int saturation = Settings.Secure.getIntForUser(context.getContentResolver(), channelToKey(4),
                getDefaultValueForChannel(4), userId);
        int contrast = Settings.Secure.getIntForUser(context.getContentResolver(), channelToKey(5),
                getDefaultValueForChannel(5), userId);

        setMatrix(red, green, blue, hue, saturation, contrast);
    }

    public static int getDefaultValueForChannel(int channel) {
        switch (channel) {
            case 0:
            case 1:
            case 2:
                return 255;
            case 3:
                return 180;
            case 4:
            case 5:
                return 100;
            default:
                throw new IllegalArgumentException("Unknown channel: " + channel);
        }
    }

    public static String channelToKey(int channel) {
        switch (channel) {
            case 0:
                return Settings.Secure.DISPLAY_COLOR_BALANCE_RED;
            case 1:
                return Settings.Secure.DISPLAY_COLOR_BALANCE_GREEN;
            case 2:
                return Settings.Secure.DISPLAY_COLOR_BALANCE_BLUE;
            case 3:
                return Settings.Secure.DISPLAY_COLOR_BALANCE_HUE;
            case 4:
                return Settings.Secure.DISPLAY_COLOR_BALANCE_SATURATION;
            case 5:
                return Settings.Secure.DISPLAY_COLOR_BALANCE_CONTRAST;
            default:
                throw new IllegalArgumentException("Unknown channel: " + channel);
        }
    }
}
