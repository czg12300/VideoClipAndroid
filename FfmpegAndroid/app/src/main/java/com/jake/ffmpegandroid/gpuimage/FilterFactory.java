package com.jake.ffmpegandroid.gpuimage;

import android.content.Context;

import com.jake.ffmpegandroid.gpuimage.filter.GPUImageBeautyFilter;
import com.jake.ffmpegandroid.gpuimage.filter.GPUImageFilter;
import com.jake.ffmpegandroid.gpuimage.filter.MagicBlackCatFilter;


/**
 * 滤镜工厂
 */
public class FilterFactory {

    /**
     * 根据滤镜定义创建滤镜
     *
     * @param type
     * @param context
     * @return
     */
    public static GPUImageFilter getFilter(FilterType type, Context context) {
        switch (type) {

            case BEAUTY:
                return new GPUImageBeautyFilter(context);
            case BLACK_CAT:
                return new MagicBlackCatFilter(context);

            default:
                return new GPUImageFilter();
        }
    }

    /**
     * 根据滤镜获取滤镜的类型
     *
     * @param filter
     * @return
     */
    public static FilterType getFilterType(GPUImageFilter filter) {
        FilterType type = FilterType.NO_FILTER;
        if (filter instanceof GPUImageBeautyFilter) {
            type = FilterType.BEAUTY;
        }else if(filter instanceof MagicBlackCatFilter){
            type = FilterType.BLACK_CAT;

        }
        return type;
    }


}
