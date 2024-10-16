package com.bilibili;


import com.github.unidbg.AndroidEmulator;
import com.github.unidbg.LibraryResolver;
import com.github.unidbg.arm.backend.DynarmicFactory;

import com.github.unidbg.arm.backend.Unicorn2Factory;
import com.github.unidbg.linux.android.AndroidEmulatorBuilder;
import com.github.unidbg.linux.android.AndroidResolver;
import com.github.unidbg.linux.android.dvm.*;
import com.github.unidbg.linux.android.dvm.jni.ProxyDvmObject;
import com.github.unidbg.linux.android.dvm.DvmClass;
import com.github.unidbg.memory.Memory;
import com.github.unidbg.Module;
import com.sun.jna.Pointer;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;


public class xvideo extends AbstractJni {

    public static void main(String[] args) {
        long start = System.currentTimeMillis();
        xvideo xv = new xvideo();
        String result = xv.calls();
        System.out.println("call s result:"+result);
    }

    private String calls() {
        String arg1 = "aid=01A9qnZ7PZZHyaxsviZjmfkJSE31gEJ5t9NZ9plG8dx2KdJl4.&cfrom=28B5295010&cuid=0&noncestr=1cYUiYLI6N12bt9Kvwo13l45S9lr08&phone=13428844820&platform=ANDROID&timestamp=1729083642319&ua=RMX2117-RMX2117CN__oasis__3.5.8__Android__Android13&version=3.5.8&vid=2016013554340&wm=20004_90024";
        Boolean arg2 = false;
        String ret = NativeApi.newObject(null).callJniMethodObject(emulator, "s([BZ)Ljava/lang/String;", arg1.getBytes(StandardCharsets.UTF_8), arg2).getValue().toString();
        return ret;
    }

    private final AndroidEmulator emulator;
    private final VM vm;
    //private final Module module;
    private final DvmClass NativeApi;

    private xvideo() {
        emulator = AndroidEmulatorBuilder
                .for32Bit()
                .addBackendFactory(new DynarmicFactory(true))
                //.addBackendFactory(new Unicorn2Factory(true))
                .setProcessName("com.sina.oasis")
                .build();
        Memory memory = emulator.getMemory();
        LibraryResolver resolver = new AndroidResolver(23);
        memory.setLibraryResolver(resolver);

        vm = emulator.createDalvikVM(new File("unidbg-android/src/test/java/com/bilibili/lvzhou.apk"));

        vm.setJni(this);
        vm.setVerbose(true);
        DalvikModule dm = vm.loadLibrary(new File("unidbg-android/src/test/java/com/bilibili/liboasiscore.so"), true);
        NativeApi = vm.resolveClass("com/weibo/xvideo/NativeApi");
        dm.callJNI_OnLoad(emulator);
    }

}


