package com.sina.weibo;

import com.bilibili.xvideo;
import com.github.unidbg.AndroidEmulator;
import com.github.unidbg.Emulator;
import com.github.unidbg.LibraryResolver;
import com.github.unidbg.arm.backend.DynarmicFactory;

import com.github.unidbg.arm.backend.Unicorn2Factory;
import com.github.unidbg.arm.context.Arm32RegisterContext;
import com.github.unidbg.debugger.BreakPointCallback;
import com.github.unidbg.linux.android.AndroidEmulatorBuilder;
import com.github.unidbg.linux.android.AndroidResolver;
import com.github.unidbg.linux.android.dvm.*;
import com.github.unidbg.linux.android.dvm.jni.ProxyDvmObject;
import com.github.unidbg.linux.android.dvm.DvmClass;
import com.github.unidbg.memory.Memory;
import com.github.unidbg.Module;
import com.sun.jna.Pointer;
import unicorn.ArmConst;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class Weibo extends AbstractJni{
    public static void main(String[] args) {
        Weibo wb = new Weibo();
        String result = wb.calls();
        System.out.println("call calculateS result:"+result);
    }

    private String calls() {
        DvmObject<?> context = vm.resolveClass("android/app/Application", vm.resolveClass("android/content/ContextWrapper", vm.resolveClass("android/content/Context"))).newObject(null);
        String arg2 = "hello world";
        String arg3 = "123456";
        String ret = NativeApi.newObject(null).callJniMethodObject(emulator, "calculateS", context, arg2, arg3).getValue().toString();
        return ret;
    }

    private final AndroidEmulator emulator;
    private final VM vm;
    //private final Module module;
    private final DvmClass NativeApi;

    private Weibo() {
        emulator = AndroidEmulatorBuilder
                .for32Bit()
                //.addBackendFactory(new DynarmicFactory(true))
                .addBackendFactory(new Unicorn2Factory(true))
                .setProcessName("com.weico.international")
                .build();
        Memory memory = emulator.getMemory();
        LibraryResolver resolver = new AndroidResolver(23);
        memory.setLibraryResolver(resolver);

        vm = emulator.createDalvikVM(new File("unidbg-android/src/test/java/com/sina/weibo/sinaInternational.apk"));

        vm.setJni(this);
        vm.setVerbose(true);
        //DalvikModule dm = vm.loadLibrary(new File("unidbg-android/src/test/java/com/sina/weibo/libutility.so"), true);
        DalvikModule dm = vm.loadLibrary("utility", true);

        //补环境 free函数
        emulator.attach().addBreakPoint(dm.getModule().findSymbolByName("free").getAddress(), new BreakPointCallback() {
            @Override
            public boolean onHit(Emulator<?> emulator, long address) {
                Arm32RegisterContext registerContext = emulator.getContext();
                emulator.getBackend().reg_write(ArmConst.UC_ARM_REG_R0, 0);
                emulator.getBackend().reg_write(ArmConst.UC_ARM_REG_PC, registerContext.getLR());
                return true;
            }
        });

        NativeApi = vm.resolveClass("com/sina/weibo/security/WeiboSecurityUtils");
        dm.callJNI_OnLoad(emulator);
    }

    //补环境
    //java.lang.UnsupportedOperationException: android/content/ContextWrapper->getPackageManager()Landroid/content/pm/PackageManager;
    //at com.github.unidbg.linux.android.dvm.AbstractJni.callObjectMethod(AbstractJni.java:933)

    @Override
    public DvmObject<?> callObjectMethod(BaseVM vm, DvmObject<?> dvmObject, String signature, VarArg varArg) {
        switch (signature){
            case "android/content/ContextWrapper->getPackageManager()Landroid/content/pm/PackageManager;":{
                return vm.resolveClass("android/content/pm/PackageManager").newObject(null);
            }
        }
        return super.callObjectMethod(vm, dvmObject, signature, varArg);
    }


}
