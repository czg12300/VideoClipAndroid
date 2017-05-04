package com.jake.ffmpegandroid.record.videocodec;

/**
 * 音视频的编码的参数
 *
 * @author jake
 * @since 2017/5/2 下午5:47
 */

public class VideoCodecParameters {
    public static enum CodecType {
        H264, H265
    }

    public int frameRate;
    public int bitRate;
    public CodecType codecType;
    public int width;
    public int height;
    //关键帧间隔时间 单位s
    public int keyIFrameInterval = 1;

    public static class VideoCodecParametersBuilder {
        private VideoCodecParameters parameters;

        private VideoCodecParametersBuilder() {
            parameters = new VideoCodecParameters();
        }

        public VideoCodecParametersBuilder setHeight(int height) {
            parameters.height = height;
            return this;
        }

        public VideoCodecParametersBuilder setKeyIFrameInterval(int minils) {
            parameters.keyIFrameInterval = minils;
            return this;
        }

        public VideoCodecParametersBuilder setWidth(int width) {
            parameters.width = width;
            return this;
        }

        public VideoCodecParametersBuilder setFrameRate(int frameRate) {
            parameters.frameRate = frameRate;
            return this;
        }

        public VideoCodecParametersBuilder setBitRate(int bitRate) {
            parameters.bitRate = bitRate;
            return this;
        }

        public VideoCodecParametersBuilder setCodecType(CodecType codecType) {
            parameters.codecType = codecType;
            return this;
        }

        public VideoCodecParameters build() {
            return parameters;
        }

        public static VideoCodecParametersBuilder create() {
            return new VideoCodecParametersBuilder();
        }
    }
}
