package com.createchance.simplecameraview;

import java.util.ArrayList;
import java.util.List;

/**
 * Camera v2设备信息
 */

class Camera2DeviceInfo {

    private List<Device> cameraList = new ArrayList<>();

    void addInfo(Device device) {
        if (device != null) {
            cameraList.add(device);
        }
    }

    Device getDeviceById(String id) {
        for (Device device : cameraList) {
            if (device.cameraId.equals(id)) {
                return device;
            }
        }

        return null;
    }

    Device getDeviceByFacing(int facing) {
        for (Device device : cameraList) {
            if (device.facing == facing) {
                return device;
            }
        }

        return null;
    }

    static class Device {
        private String cameraId;

        private int facing;

        private boolean isAFSupported = false;

        private boolean isAWBSupported = false;

        public Device(String id, int facing) {
            this.cameraId = id;
            this.facing = facing;
        }

        public String getCameraId() {
            return cameraId;
        }

        public void setCameraId(String cameraId) {
            this.cameraId = cameraId;
        }

        public int getFacing() {
            return facing;
        }

        public void setFacing(int facing) {
            this.facing = facing;
        }

        public boolean isAFSupported() {
            return isAFSupported;
        }

        public void setAFSupported(boolean AFSupported) {
            isAFSupported = AFSupported;
        }

        public boolean isAWBSupported() {
            return isAWBSupported;
        }

        public void setAWBSupported(boolean AWBSupported) {
            isAWBSupported = AWBSupported;
        }
    }
}
