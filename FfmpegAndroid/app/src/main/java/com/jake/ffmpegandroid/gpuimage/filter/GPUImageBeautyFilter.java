/*
 * Copyright (C) 2012 CyberAgent
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.jake.ffmpegandroid.gpuimage.filter;

import android.content.Context;
import android.opengl.GLES20;

import com.jake.ffmpegandroid.R;
import com.jake.ffmpegandroid.gpuimage.OpenGlUtils;


public class GPUImageBeautyFilter extends GPUImageFilter {
    private int mSingleStepOffsetLocation;
    private int mParamsLocation;

    public GPUImageBeautyFilter(Context context) {
        super(NO_FILTER_VERTEX_SHADER,
                OpenGlUtils.readShaderFromRawResource(context, R.raw.beautify_fragment));
    }

    protected void onInit() {
        super.onInit();
        mSingleStepOffsetLocation = GLES20.glGetUniformLocation(getProgram(), "singleStepOffset");
        mParamsLocation = GLES20.glGetUniformLocation(getProgram(), "params");
        setBeautyLevel(3);
    }

    protected void onDestroy() {
        super.onDestroy();
    }

    private void setTexelSize(final float w, final float h) {
        setFloatVec2(mSingleStepOffsetLocation, new float[]{2.0f / w, 2.0f / h});
    }

    @Override
    public void onInputSizeChanged(final int width, final int height) {
        super.onInputSizeChanged(width, height);
        setTexelSize(width, height);
    }

    public void setBeautyLevel(int level) {
        switch (level) {
            case 1:
                setFloatVec4(mParamsLocation, new float[]{1.0f, 1.0f, 0.15f, 0.15f});
                break;
            case 2:
                setFloatVec4(mParamsLocation, new float[]{0.8f, 0.9f, 0.2f, 0.2f});
                break;
            case 3:
                setFloatVec4(mParamsLocation, new float[]{0.6f, 0.8f, 0.25f, 0.25f});
                break;
            case 4:
                setFloatVec4(mParamsLocation, new float[]{0.4f, 0.7f, 0.38f, 0.3f});
                break;
            case 5:
                setFloatVec4(mParamsLocation, new float[]{0.33f, 0.63f, 0.4f, 0.35f});
                break;
            default:
                break;
        }
    }
}
